package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologySummary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TopologyService {
    private final TopologyRelationshipResolver relationshipResolver;

    public TopologyService() {
        this(new DefaultTopologyRelationshipResolver());
    }

    public TopologyService(TopologyRelationshipResolver relationshipResolver) {
        this.relationshipResolver = relationshipResolver;
    }

    public TopologyResult infer(FileInventory inventory, StructuralExtractionResult extractionResult, InterpretationResult interpretationResult) {
        Map<String, LogicalScope> inferredScopes = new LinkedHashMap<>();
        Map<String, ArchitectureEntity> inferredEntities = new LinkedHashMap<>();
        Map<String, ArchitectureRelationship> inferredRelationships = new LinkedHashMap<>();
        List<Diagnostic> diagnostics = new ArrayList<>();

        Map<String, LogicalScope> extractionScopesById = extractionResult.scopes().stream()
            .collect(Collectors.toMap(LogicalScope::id, scope -> scope, (left, right) -> left, LinkedHashMap::new));
        Map<String, ExtractedEntityFact> extractedEntitiesById = extractionResult.entities().stream()
            .collect(Collectors.toMap(ExtractedEntityFact::id, entity -> entity, (left, right) -> left, LinkedHashMap::new));

        // Build directory and source-root module scopes from inventory.
        Map<String, String> fileDirectoryScopeIds = new LinkedHashMap<>();
        Map<String, String> fileModuleScopeIds = new LinkedHashMap<>();
        for (FileInventoryEntry entry : inventory.entries()) {
            if (entry.ignored()) {
                continue;
            }
            buildDirectoryHierarchy(entry.relativePath(), inferredScopes, fileDirectoryScopeIds);
            String modulePath = moduleRoot(entry.relativePath());
            if (modulePath != null) {
                String parentDirectory = parentPath(modulePath);
                String parentScopeId = parentDirectory == null ? "scope:repo" : IdUtils.scopeId("directory", parentDirectory);
                LogicalScope moduleScope = TopologySupport.moduleScope(modulePath, parentScopeId, entry.detectedLanguage());
                inferredScopes.putIfAbsent(moduleScope.id(), moduleScope);
                fileModuleScopeIds.put(entry.relativePath(), moduleScope.id());
                inferredEntities.putIfAbsent(
                    IdUtils.externalEntityId("logical-module", modulePath),
                    TopologySupport.moduleEntity(modulePath, moduleScope.id(), entry.detectedLanguage(), "source-root")
                );
            }
        }

        // Create package logical module entities for package scopes and keep package hierarchy mappings.
        Map<String, LogicalScope> packageScopesById = expandPackageScopeHierarchy(extractionResult.scopes());
        Map<String, String> packageScopeToEntityId = new LinkedHashMap<>();
        for (LogicalScope scope : packageScopesById.values()) {
            inferredScopes.putIfAbsent(scope.id(), scope);
            ArchitectureEntity packageEntity = TopologySupport.packageEntity(scope);
            inferredEntities.putIfAbsent(packageEntity.id(), packageEntity);
            packageScopeToEntityId.put(scope.id(), packageEntity.id());
        }

        inferTypeScriptPackageScopes(inventory, inferredScopes, packageScopesById, packageScopeToEntityId, inferredEntities);

        // Build contains relations module->top-level package/file, package->subpackage and package->file.
        for (LogicalScope packageScope : packageScopesById.values()) {
            LogicalScope parentPackageScope = packageScopesById.get(packageScope.parentScopeId());
            if (parentPackageScope == null) {
                continue;
            }
            String parentEntityId = packageScopeToEntityId.get(parentPackageScope.id());
            String childEntityId = packageScopeToEntityId.get(packageScope.id());
            if (parentEntityId != null && childEntityId != null) {
                inferredRelationships.putIfAbsent(
                    IdUtils.relationshipId("topology-contains", parentEntityId, childEntityId, packageScope.name()),
                    TopologySupport.contains(parentEntityId, childEntityId, packageScope.name(), packageScope.sourceRefs(), Map.of("rollup", "package-subpackage"))
                );
            }
        }

        for (ArchitectureEntity entity : inferredEntities.values()) {
            Object logicalRole = entity.metadata().get("logicalRole");
            if ("source-root".equals(logicalRole)) {
                String modulePath = entity.name();
                for (ExtractedEntityFact fileEntity : fileModuleEntities(extractionResult.entities())) {
                    String filePath = TopologySupport.primaryPath(fileEntity);
                    if (filePath != null && modulePath.equals(moduleRoot(filePath))) {
                        inferredRelationships.putIfAbsent(
                            IdUtils.relationshipId("topology-contains", entity.id(), fileEntity.id(), filePath),
                            TopologySupport.contains(entity.id(), fileEntity.id(), filePath, fileEntity.sourceRefs(), Map.of("rollup", "module-file"))
                        );
                    }
                }
                for (LogicalScope scope : packageScopesById.values()) {
                    if (scope.sourceRefs().stream().anyMatch(ref -> {
                        String p = ref.path();
                        return p != null && modulePath.equals(moduleRoot(p));
                    })) {
                        LogicalScope parentPackageScope = packageScopesById.get(scope.parentScopeId());
                        boolean topLevelInModule = parentPackageScope == null || parentPackageScope.sourceRefs().stream().noneMatch(ref -> {
                            String p = ref.path();
                            return p != null && modulePath.equals(moduleRoot(p));
                        });
                        if (topLevelInModule) {
                            String packageEntityId = packageScopeToEntityId.get(scope.id());
                            if (packageEntityId != null) {
                                inferredRelationships.putIfAbsent(
                                    IdUtils.relationshipId("topology-contains", entity.id(), packageEntityId, scope.name()),
                                    TopologySupport.contains(entity.id(), packageEntityId, scope.name(), scope.sourceRefs(), Map.of("rollup", "module-package"))
                                );
                            }
                        }
                    }
                }
            }
        }

        for (ExtractedEntityFact fileEntity : fileModuleEntities(extractionResult.entities())) {
            String packageScopeId = packageScopeIdForFile(fileEntity, packageScopesById.values());
            String packageEntityId = packageScopeId == null ? null : packageScopeToEntityId.get(packageScopeId);
            if (packageEntityId != null) {
                inferredRelationships.putIfAbsent(
                    IdUtils.relationshipId("topology-contains", packageEntityId, fileEntity.id(), fileEntity.name()),
                    TopologySupport.contains(packageEntityId, fileEntity.id(), fileEntity.name(), fileEntity.sourceRefs(), Map.of("rollup", "package-file"))
                );
            }
        }

        // Resolve internal Java/TypeScript entities from qualified names.
        Map<String, ExtractedEntityFact> structuralTypesByQualifiedName = extractionResult.entities().stream()
            .filter(entity -> isStructuralTypeEntity(entity))
            .filter(entity -> entity.metadata().get("qualifiedName") != null)
            .collect(Collectors.toMap(entity -> String.valueOf(entity.metadata().get("qualifiedName")), entity -> entity, (left, right) -> left, LinkedHashMap::new));

        // Resolve TypeScript relative imports to file modules.
        Map<String, ExtractedEntityFact> fileModulesByPath = fileModuleEntities(extractionResult.entities()).stream()
            .collect(Collectors.toMap(ExtractedEntityFact::name, entity -> entity, (left, right) -> left, LinkedHashMap::new));

        // Roll-up internal relationships across files, types, and members.
        Set<String> seenPackageUses = new LinkedHashSet<>();
        Set<String> seenModuleUses = new LinkedHashSet<>();

        for (ExtractedRelationshipFact relationship : extractionResult.relationships()) {
            if (!isTopologyRelevantRelationship(relationship.kind())) {
                continue;
            }
            ExtractedEntityFact fromEntity = extractedEntitiesById.get(relationship.fromEntityId());
            if (fromEntity == null) {
                continue;
            }
            String fromPath = TopologySupport.primaryPath(fromEntity);
            if (fromPath == null) {
                continue;
            }

            Optional<ExtractedEntityFact> resolvedTarget = resolveInternalTarget(relationship, fromPath, extractedEntitiesById, structuralTypesByQualifiedName, fileModulesByPath);
            if (resolvedTarget.isEmpty()) {
                continue;
            }

            ExtractedEntityFact targetEntity = resolvedTarget.get();
            boolean evidenceImport = isTypeScriptImportEvidenceRelationship(relationship);
            String rollup = evidenceImport
                ? (fromEntity.kind() == EntityKind.MODULE ? "file-evidence" : "entity-evidence")
                : (fromEntity.kind() == EntityKind.MODULE ? "file-internal" : "entity-internal");
            Map<String, Object> directMetadata = new LinkedHashMap<>(topologyMetadata(relationship, Map.of("rollup", rollup, "sourceRelationshipId", relationship.id())));
            if (evidenceImport) {
                directMetadata.put("dependencyPriority", "evidence");
            }
            ArchitectureRelationship directRelationship = switch (relationship.kind()) {
                case EXTENDS -> TopologySupport.typedRelationship(RelationshipKind.EXTENDS, fromEntity.id(), targetEntity.id(), relationship.label(), relationship.sourceRefs(), directMetadata);
                case IMPLEMENTS -> TopologySupport.typedRelationship(RelationshipKind.IMPLEMENTS, fromEntity.id(), targetEntity.id(), relationship.label(), relationship.sourceRefs(), directMetadata);
                default -> TopologySupport.uses(fromEntity.id(), targetEntity.id(), relationship.label(), relationship.sourceRefs(), directMetadata);
            };
            inferredRelationships.putIfAbsent(directRelationship.id(), directRelationship);

            if (evidenceImport) {
                continue;
            }

            String fromPackageScopeId = packageScopeIdForEntity(fromEntity, packageScopesById.values());
            String toPackageScopeId = packageScopeIdForEntity(targetEntity, packageScopesById.values());
            String fromPackageEntityId = fromPackageScopeId == null ? null : packageScopeToEntityId.get(fromPackageScopeId);
            String toPackageEntityId = toPackageScopeId == null ? null : packageScopeToEntityId.get(toPackageScopeId);
            if (fromPackageEntityId != null && toPackageEntityId != null && !fromPackageEntityId.equals(toPackageEntityId)) {
                String key = relationship.kind().name() + ":" + fromPackageEntityId + "->" + toPackageEntityId + "|" + rollupDependencySignature(relationship);
                if (seenPackageUses.add(key)) {
                    Map<String, Object> packageMetadata = topologyMetadata(relationship, Map.of("rollup", "package-package"));
                    ArchitectureRelationship pkgRelationship = switch (relationship.kind()) {
                        case EXTENDS -> TopologySupport.typedRelationship(RelationshipKind.EXTENDS, fromPackageEntityId, toPackageEntityId, relationship.label(), relationship.sourceRefs(), packageMetadata);
                        case IMPLEMENTS -> TopologySupport.typedRelationship(RelationshipKind.IMPLEMENTS, fromPackageEntityId, toPackageEntityId, relationship.label(), relationship.sourceRefs(), packageMetadata);
                        default -> TopologySupport.uses(fromPackageEntityId, toPackageEntityId, relationship.label(), relationship.sourceRefs(), packageMetadata);
                    };
                    inferredRelationships.putIfAbsent(pkgRelationship.id(), pkgRelationship);
                }
            }

            String fromModuleEntityId = sourceRootEntityId(fromPath);
            String toModuleEntityId = sourceRootEntityId(TopologySupport.primaryPath(targetEntity));
            boolean sameModule = fromModuleEntityId != null && fromModuleEntityId.equals(toModuleEntityId);
            boolean allowModuleRollup = fromModuleEntityId != null
                && toModuleEntityId != null
                && (!sameModule || relationship.kind() == RelationshipKind.DEPENDS_ON);
            if (allowModuleRollup) {
                String key = relationship.kind().name() + ":" + fromModuleEntityId + "->" + toModuleEntityId + "|" + rollupDependencySignature(relationship) + "|same=" + sameModule;
                if (seenModuleUses.add(key)) {
                    Map<String, Object> moduleAdditions = new LinkedHashMap<>();
                    moduleAdditions.put("rollup", "module-module");
                    if (sameModule) {
                        moduleAdditions.put("sameModule", true);
                    }
                    Map<String, Object> moduleMetadata = topologyMetadata(relationship, moduleAdditions);
                    ArchitectureRelationship moduleRelationship = switch (relationship.kind()) {
                        case EXTENDS -> TopologySupport.typedRelationship(RelationshipKind.EXTENDS, fromModuleEntityId, toModuleEntityId, relationship.label(), relationship.sourceRefs(), moduleMetadata);
                        case IMPLEMENTS -> TopologySupport.typedRelationship(RelationshipKind.IMPLEMENTS, fromModuleEntityId, toModuleEntityId, relationship.label(), relationship.sourceRefs(), moduleMetadata);
                        default -> TopologySupport.uses(fromModuleEntityId, toModuleEntityId, relationship.label(), relationship.sourceRefs(), moduleMetadata);
                    };
                    inferredRelationships.putIfAbsent(moduleRelationship.id(), moduleRelationship);
                }
            }
        }

        TopologySummary summary = new TopologySummary(
            inferredScopes.size(),
            inferredEntities.size(),
            inferredRelationships.size(),
            countsByKind(inferredScopes.values(), LogicalScope::kind),
            countsByKind(inferredEntities.values(), ArchitectureEntity::kind),
            countsByKind(inferredRelationships.values(), ArchitectureRelationship::kind)
        );

        return new TopologyResult(
            inferredScopes.values().stream().sorted(Comparator.comparing(LogicalScope::displayName)).toList(),
            List.copyOf(inferredEntities.values()),
            List.copyOf(inferredRelationships.values()),
            List.copyOf(diagnostics),
            summary
        );
    }

    private static void buildDirectoryHierarchy(String relativePath, Map<String, LogicalScope> inferredScopes, Map<String, String> fileDirectoryScopeIds) {
        Path path = Path.of(relativePath);
        Path parent = path.getParent();
        String previousScopeId = "scope:repo";
        String current = "";
        while (parent != null) {
            current = current.isEmpty() ? parent.getName(0).toString() : current;
            break;
        }
        String normalized = "";
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            normalized = normalized.isEmpty() ? path.getName(i).toString() : normalized + "/" + path.getName(i);
            String parentPath = parentPath(normalized);
            String parentScopeId = parentPath == null ? "scope:repo" : IdUtils.scopeId("directory", parentPath);
            LogicalScope scope = TopologySupport.directoryScope(normalized, parentScopeId);
            inferredScopes.putIfAbsent(scope.id(), scope);
            previousScopeId = scope.id();
        }
        if (!previousScopeId.equals("scope:repo")) {
            fileDirectoryScopeIds.put(relativePath, previousScopeId);
        }
    }

    private static String parentPath(String path) {
        if (path == null || path.isBlank() || !path.contains("/")) {
            return null;
        }
        return path.substring(0, path.lastIndexOf('/'));
    }


private static java.util.List<ExtractedEntityFact> fileModuleEntities(java.util.List<ExtractedEntityFact> entities) {
    return entities.stream().filter(entity -> entity.kind() == EntityKind.MODULE).toList();
}

private static String packageScopeIdForFile(ExtractedEntityFact fileEntity, java.util.Collection<LogicalScope> scopes) {
    String filePath = TopologySupport.primaryPath(fileEntity);
    if (filePath == null) {
        return null;
    }
    return scopes.stream()
        .filter(scope -> scope.kind() == ScopeKind.PACKAGE)
        .filter(scope -> scope.sourceRefs().stream().anyMatch(ref -> filePath.equals(ref.path())))
        .sorted(Comparator.comparingInt((LogicalScope scope) -> scope.name() == null ? 0 : scope.name().length()).reversed())
        .map(LogicalScope::id)
        .findFirst()
        .orElse(null);
}

private static boolean isJavaStructuralEntity(EntityKind kind) {
    return kind == EntityKind.CLASS || kind == EntityKind.INTERFACE || kind == EntityKind.FIELD || kind == EntityKind.FUNCTION;
}

private static boolean isTopologyRelevantRelationship(info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind kind) {
    return kind == info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON
        || kind == info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.EXTENDS
        || kind == info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.IMPLEMENTS;
}

private Optional<ExtractedEntityFact> resolveInternalTarget(
    ExtractedRelationshipFact relationship,
    String fromPath,
    Map<String, ExtractedEntityFact> extractedEntitiesById,
    Map<String, ExtractedEntityFact> structuralTypesByQualifiedName,
    Map<String, ExtractedEntityFact> fileModulesByPath
) {
    ExtractedEntityFact direct = extractedEntitiesById.get(relationship.toEntityId());
    if (direct != null) {
        return Optional.of(direct);
    }
    return relationshipResolver.resolveInternalTarget(relationship, fromPath, structuralTypesByQualifiedName, fileModulesByPath);
}

private static String packageScopeIdForEntity(ExtractedEntityFact entity, java.util.Collection<LogicalScope> scopes) {
    if (entity == null) {
        return null;
    }
    String language = String.valueOf(entity.metadata().getOrDefault("language", "java"));
    if ((entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE) && !"typescript".equalsIgnoreCase(language)) {
        return entity.scopeId();
    }
    String ownerQualifiedName = String.valueOf(entity.metadata().getOrDefault("ownerQualifiedName", ""));
    if (!ownerQualifiedName.isBlank()) {
        String ownerPackage = parentQualifiedName(ownerQualifiedName);
        if (ownerPackage != null && !ownerPackage.isBlank()) {
            String expectedScopeId = IdUtils.scopeId(language + "-package", ownerPackage);
            if (scopes.stream().anyMatch(scope -> expectedScopeId.equals(scope.id()))) {
                return expectedScopeId;
            }
        }
    }
    return packageScopeIdForFile(entity, scopes);
}

private static String parentQualifiedName(String qualifiedName) {
    if (qualifiedName == null || qualifiedName.isBlank() || !qualifiedName.contains(".")) {
        return null;
    }
    return qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
}

private static String sourceRootEntityId(String filePath) {
    String root = moduleRoot(filePath);
    return root == null ? null : IdUtils.externalEntityId("logical-module", root);
}

private static String moduleRoot(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
        return null;
    }
    String[] parts = relativePath.split("/");
    if (parts.length >= 3 && "src".equals(parts[0]) && ("main".equals(parts[1]) || "test".equals(parts[1]))) {
        return parts[0] + "/" + parts[1] + "/" + parts[2];
    }
    if (parts.length >= 2 && "src".equals(parts[0]) && !("main".equals(parts[1]) || "test".equals(parts[1]))) {
        return parts[0] + "/" + parts[1];
    }
    return parts.length > 0 ? parts[0] : null;
}

    private static Map<String, LogicalScope> expandPackageScopeHierarchy(List<LogicalScope> scopes) {
        Map<String, LogicalScope> packageScopes = scopes.stream()
            .filter(scope -> scope.kind() == ScopeKind.PACKAGE)
            .collect(Collectors.toMap(LogicalScope::id, scope -> scope, (left, right) -> mergePackageScopes(left, right), LinkedHashMap::new));

        boolean changed;
        do {
            changed = false;
            List<LogicalScope> snapshot = new ArrayList<>(packageScopes.values());
            for (LogicalScope scope : snapshot) {
                String language = String.valueOf(scope.metadata().getOrDefault("language", "unknown"));
                String parentPackageName = parentPackageName(scope.name());
                if (parentPackageName == null) {
                    continue;
                }
                String parentScopeId = IdUtils.scopeId(language + "-package", parentPackageName);
                if (!packageScopes.containsKey(parentScopeId)) {
                    packageScopes.put(parentScopeId, TopologySupport.packageScope(
                        parentPackageName,
                        parentPackageName(parentPackageName) == null ? "scope:repo" : IdUtils.scopeId(language + "-package", parentPackageName(parentPackageName)),
                        language,
                        scope.sourceRefs()
                    ));
                    changed = true;
                }
                if (!parentScopeId.equals(scope.parentScopeId())) {
                    packageScopes.put(scope.id(), new LogicalScope(
                        scope.id(),
                        scope.kind(),
                        scope.name(),
                        scope.displayName(),
                        parentScopeId,
                        scope.sourceRefs(),
                        scope.metadata()
                    ));
                    changed = true;
                }
            }
        } while (changed);

        return packageScopes;
    }

    private static LogicalScope mergePackageScopes(LogicalScope left, LogicalScope right) {
        if (left.sourceRefs().size() >= right.sourceRefs().size()) {
            return left;
        }
        return new LogicalScope(
            left.id(),
            left.kind(),
            left.name(),
            left.displayName(),
            left.parentScopeId(),
            right.sourceRefs(),
            left.metadata().isEmpty() ? right.metadata() : left.metadata()
        );
    }

    private static String parentPackageName(String packageName) {
        if (packageName == null || packageName.isBlank()) {
            return null;
        }
        if (packageName.contains("/")) {
            return packageName.contains("/") ? parentPath(packageName) : null;
        }
        if (!packageName.contains(".")) {
            return null;
        }
        return packageName.substring(0, packageName.lastIndexOf('.'));
    }

    private static boolean isStructuralTypeEntity(ExtractedEntityFact entity) {
        if (entity == null) {
            return false;
        }
        String lang = String.valueOf(entity.metadata().getOrDefault("language", ""));
        return ("java".equalsIgnoreCase(lang) && isJavaStructuralEntity(entity.kind()))
            || ("typescript".equalsIgnoreCase(lang) && (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE));
    }

    private static boolean isTypeScriptImportEvidenceRelationship(ExtractedRelationshipFact relationship) {
        if (relationship == null) {
            return false;
        }
        String language = String.valueOf(relationship.metadata().getOrDefault("language", ""));
        String dependencySource = String.valueOf(relationship.metadata().getOrDefault("dependencySource", ""));
        return "typescript".equalsIgnoreCase(language) && "import".equalsIgnoreCase(dependencySource);
    }


    private static String rollupDependencySignature(ExtractedRelationshipFact relationship) {
        if (relationship == null || relationship.metadata() == null || relationship.metadata().isEmpty()) {
            return "generic";
        }
        List<String> parts = new ArrayList<>();
        addRollupSignaturePart(parts, "dependencySource", relationship.metadata().get("dependencySource"));
        addRollupSignaturePart(parts, "framework", relationship.metadata().get("framework"));
        addRollupSignaturePart(parts, "frameworkRelationship", relationship.metadata().get("frameworkRelationship"));
        addRollupSignaturePart(parts, "hookClassification", relationship.metadata().get("hookClassification"));
        addRollupSignaturePart(parts, "dependencyCategory", relationship.metadata().get("dependencyCategory"));
        return parts.isEmpty() ? "generic" : String.join("|", parts);
    }

    private static void addRollupSignaturePart(List<String> parts, String key, Object value) {
        if (value == null) {
            return;
        }
        String normalized = String.valueOf(value).trim();
        if (!normalized.isEmpty()) {
            parts.add(key + "=" + normalized);
        }
    }

    private static Map<String, Object> topologyMetadata(ExtractedRelationshipFact relationship, Map<String, Object> additions) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (relationship != null && relationship.metadata() != null) {
            metadata.putAll(relationship.metadata());
        }
        if (additions != null) {
            metadata.putAll(additions);
        }
        return Map.copyOf(metadata);
    }

    private static void inferTypeScriptPackageScopes(
        FileInventory inventory,
        Map<String, LogicalScope> inferredScopes,
        Map<String, LogicalScope> packageScopesById,
        Map<String, String> packageScopeToEntityId,
        Map<String, ArchitectureEntity> inferredEntities
    ) {
        for (FileInventoryEntry entry : inventory.entries()) {
            if (entry.ignored()) {
                continue;
            }
            String language = entry.detectedLanguage();
            if (!"typescript".equalsIgnoreCase(language)) {
                continue;
            }
            String relativePath = entry.relativePath();
            String moduleRoot = moduleRoot(relativePath);
            String directoryPath = parentPath(relativePath);
            if (directoryPath == null || moduleRoot == null || directoryPath.equals(moduleRoot)) {
                continue;
            }
            List<String> packagePaths = new ArrayList<>();
            String current = directoryPath;
            while (current != null && !current.isBlank() && !current.equals(moduleRoot)) {
                packagePaths.add(0, current);
                current = parentPath(current);
            }
            SourceReference fileRef = new SourceReference(relativePath, null, null, null, Map.of("language", "typescript", "scopeKind", "package"));
            for (String packagePath : packagePaths) {
                String parentScopeId = moduleRoot.equals(parentPath(packagePath))
                    ? IdUtils.scopeId("module", moduleRoot)
                    : IdUtils.scopeId("typescript-package", parentPath(packagePath));
                LogicalScope scope = TopologySupport.packageScope(packagePath, parentScopeId, "typescript", List.of(fileRef));
                packageScopesById.merge(scope.id(), scope, TopologyService::mergePackageScopes);
                inferredScopes.putIfAbsent(scope.id(), scope);
                ArchitectureEntity packageEntity = TopologySupport.packageEntity(scope);
                inferredEntities.putIfAbsent(packageEntity.id(), packageEntity);
                packageScopeToEntityId.put(scope.id(), packageEntity.id());
            }
        }
    }

    private static <T, K extends Enum<K>> Map<String, Integer> countsByKind(Collection<T> values, java.util.function.Function<T, K> classifier) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (T value : values) {
            String key = classifier.apply(value).name();
            counts.merge(key, 1, Integer::sum);
        }
        return Map.copyOf(counts);
    }
}
