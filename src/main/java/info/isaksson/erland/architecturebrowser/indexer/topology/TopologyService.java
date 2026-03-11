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
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
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

        // Create package logical module entities for package scopes and contain files.
        Map<String, String> packageScopeToEntityId = new LinkedHashMap<>();
        for (LogicalScope scope : extractionResult.scopes()) {
            if (scope.kind() == ScopeKind.PACKAGE) {
                ArchitectureEntity packageEntity = TopologySupport.packageEntity(scope);
                inferredEntities.putIfAbsent(packageEntity.id(), packageEntity);
                packageScopeToEntityId.put(scope.id(), packageEntity.id());
            }
        }

        // Build contains relations module->package/file and package->file.
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
                for (LogicalScope scope : extractionResult.scopes()) {
                    if (scope.kind() == ScopeKind.PACKAGE && scope.sourceRefs().stream().anyMatch(ref -> {
                        String p = ref.path();
                        return p != null && modulePath.equals(moduleRoot(p));
                    })) {
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

        for (ExtractedEntityFact fileEntity : fileModuleEntities(extractionResult.entities())) {
            String packageScopeId = packageScopeIdForFile(fileEntity, extractionResult.scopes());
            String packageEntityId = packageScopeId == null ? null : packageScopeToEntityId.get(packageScopeId);
            if (packageEntityId != null) {
                inferredRelationships.putIfAbsent(
                    IdUtils.relationshipId("topology-contains", packageEntityId, fileEntity.id(), fileEntity.name()),
                    TopologySupport.contains(packageEntityId, fileEntity.id(), fileEntity.name(), fileEntity.sourceRefs(), Map.of("rollup", "package-file"))
                );
            }
        }

        // Resolve internal Java dependencies from qualified names.
        Map<String, ExtractedEntityFact> javaTypesByQualifiedName = extractionResult.entities().stream()
            .filter(entity -> {
                String lang = String.valueOf(entity.metadata().getOrDefault("language", ""));
                return "java".equalsIgnoreCase(lang) && (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE);
            })
            .filter(entity -> entity.metadata().get("qualifiedName") != null)
            .collect(Collectors.toMap(entity -> String.valueOf(entity.metadata().get("qualifiedName")), entity -> entity, (left, right) -> left, LinkedHashMap::new));

        // Resolve TypeScript relative imports to file modules.
        Map<String, ExtractedEntityFact> fileModulesByPath = fileModuleEntities(extractionResult.entities()).stream()
            .collect(Collectors.toMap(entity -> entity.name(), entity -> entity, (left, right) -> left, LinkedHashMap::new));

        // Roll-up package/package and module/module relationships.
        Set<String> seenPackageUses = new LinkedHashSet<>();
        Set<String> seenModuleUses = new LinkedHashSet<>();

        for (ExtractedRelationshipFact relationship : extractionResult.relationships()) {
            if (relationship.kind() != info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind.DEPENDS_ON) {
                continue;
            }
            ExtractedEntityFact fromEntity = extractedEntitiesById.get(relationship.fromEntityId());
            if (fromEntity == null || fromEntity.kind() != EntityKind.MODULE) {
                continue;
            }
            String fromPath = TopologySupport.primaryPath(fromEntity);
            if (fromPath == null) {
                continue;
            }

            Optional<ExtractedEntityFact> resolvedTarget = resolveInternalTarget(relationship.label(), fromPath, javaTypesByQualifiedName, fileModulesByPath);
            if (resolvedTarget.isEmpty()) {
                continue;
            }

            ExtractedEntityFact targetEntity = resolvedTarget.get();
            inferredRelationships.putIfAbsent(
                IdUtils.relationshipId("topology-uses", fromEntity.id(), targetEntity.id(), relationship.label()),
                TopologySupport.uses(fromEntity.id(), targetEntity.id(), relationship.label(), relationship.sourceRefs(), Map.of("rollup", "file-internal", "sourceRelationshipId", relationship.id()))
            );

            String fromPackageScopeId = packageScopeIdForFile(fromEntity, extractionResult.scopes());
            String toPackageScopeId = packageScopeIdForFile(targetEntity, extractionResult.scopes());
            String fromPackageEntityId = fromPackageScopeId == null ? null : packageScopeToEntityId.get(fromPackageScopeId);
            String toPackageEntityId = toPackageScopeId == null ? null : packageScopeToEntityId.get(toPackageScopeId);
            if (fromPackageEntityId != null && toPackageEntityId != null && !fromPackageEntityId.equals(toPackageEntityId)) {
                String key = fromPackageEntityId + "->" + toPackageEntityId;
                if (seenPackageUses.add(key)) {
                    inferredRelationships.putIfAbsent(
                        IdUtils.relationshipId("topology-uses", fromPackageEntityId, toPackageEntityId, relationship.label()),
                        TopologySupport.uses(fromPackageEntityId, toPackageEntityId, relationship.label(), relationship.sourceRefs(), Map.of("rollup", "package-package"))
                    );
                }
            }

            String fromModuleEntityId = sourceRootEntityId(fromPath);
            String toModuleEntityId = sourceRootEntityId(TopologySupport.primaryPath(targetEntity));
            if (fromModuleEntityId != null && toModuleEntityId != null && !fromModuleEntityId.equals(toModuleEntityId)) {
                String key = fromModuleEntityId + "->" + toModuleEntityId;
                if (seenModuleUses.add(key)) {
                    inferredRelationships.putIfAbsent(
                        IdUtils.relationshipId("topology-uses", fromModuleEntityId, toModuleEntityId, relationship.label()),
                        TopologySupport.uses(fromModuleEntityId, toModuleEntityId, relationship.label(), relationship.sourceRefs(), Map.of("rollup", "module-module"))
                    );
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

    private static Optional<ExtractedEntityFact> resolveInternalTarget(
        String label,
        String fromPath,
        Map<String, ExtractedEntityFact> javaTypesByQualifiedName,
        Map<String, ExtractedEntityFact> fileModulesByPath
    ) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        if (javaTypesByQualifiedName.containsKey(label)) {
            return Optional.of(javaTypesByQualifiedName.get(label));
        }
        if (label.startsWith("./") || label.startsWith("../")) {
            String resolved = resolveRelativePath(fromPath, label);
            if (resolved != null) {
                for (String candidate : List.of(resolved, resolved + ".ts", resolved + ".tsx", resolved + "/index.ts", resolved + "/index.tsx")) {
                    if (fileModulesByPath.containsKey(candidate)) {
                        return Optional.of(fileModulesByPath.get(candidate));
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static List<ExtractedEntityFact> fileModuleEntities(List<ExtractedEntityFact> entities) {
        return entities.stream().filter(entity -> entity.kind() == EntityKind.MODULE).toList();
    }

    private static String packageScopeIdForFile(ExtractedEntityFact fileEntity, List<LogicalScope> scopes) {
        String filePath = TopologySupport.primaryPath(fileEntity);
        if (filePath == null) {
            return null;
        }
        return scopes.stream()
            .filter(scope -> scope.kind() == ScopeKind.PACKAGE)
            .filter(scope -> scope.sourceRefs().stream().anyMatch(ref -> filePath.equals(ref.path())))
            .map(LogicalScope::id)
            .findFirst()
            .orElse(null);
    }

    private static String sourceRootEntityId(String filePath) {
        String moduleRoot = moduleRoot(filePath);
        return moduleRoot == null ? null : IdUtils.externalEntityId("logical-module", moduleRoot);
    }

    private static String moduleRoot(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String[] parts = relativePath.split("/");
        if (parts.length >= 3 && "src".equals(parts[0]) && ("main".equals(parts[1]) || "test".equals(parts[1]))) {
            return parts[0] + "/" + parts[1] + "/" + parts[2];
        }
        return parts.length > 0 ? parts[0] : null;
    }

    private static String resolveRelativePath(String fromPath, String importLabel) {
        try {
            Path base = Path.of(fromPath).getParent();
            if (base == null) {
                return null;
            }
            return base.resolve(importLabel).normalize().toString().replace("\\", "/");
        } catch (Exception ex) {
            return null;
        }
    }

    private static String parentPath(String path) {
        if (path == null || path.isBlank() || !path.contains("/")) {
            return null;
        }
        return path.substring(0, path.lastIndexOf('/'));
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
