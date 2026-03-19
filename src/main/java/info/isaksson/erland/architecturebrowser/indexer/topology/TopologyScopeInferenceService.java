package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class TopologyScopeInferenceService {

    TopologyScopeInferenceContext infer(FileInventory inventory, StructuralExtractionResult extractionResult, TopologyInferenceState state) {
        Map<String, String> fileDirectoryScopeIds = new LinkedHashMap<>();
        Map<String, String> fileModuleScopeIds = new LinkedHashMap<>();

        for (FileInventoryEntry entry : inventory.entries()) {
            if (entry.ignored()) {
                continue;
            }
            TopologyPaths.buildDirectoryHierarchy(entry.relativePath(), state.inferredScopes(), fileDirectoryScopeIds);
            String modulePath = TopologyPaths.moduleRoot(entry.relativePath());
            if (modulePath != null) {
                String parentDirectory = TopologyPaths.parentPath(modulePath);
                String parentScopeId = parentDirectory == null ? "scope:repo" : IdUtils.scopeId("directory", parentDirectory);
                LogicalScope moduleScope = TopologySupport.moduleScope(modulePath, parentScopeId, entry.detectedLanguage());
                state.inferredScopes().putIfAbsent(moduleScope.id(), moduleScope);
                fileModuleScopeIds.put(entry.relativePath(), moduleScope.id());
                state.inferredEntities().putIfAbsent(
                    IdUtils.externalEntityId("logical-module", modulePath),
                    TopologySupport.moduleEntity(modulePath, moduleScope.id(), entry.detectedLanguage(), "source-root")
                );
            }
        }

        Map<String, LogicalScope> packageScopesById = expandPackageScopeHierarchy(extractionResult.scopes());
        Map<String, String> packageScopeToEntityId = new LinkedHashMap<>();
        for (LogicalScope scope : packageScopesById.values()) {
            state.inferredScopes().putIfAbsent(scope.id(), scope);
            ArchitectureEntity packageEntity = TopologySupport.packageEntity(scope);
            state.inferredEntities().putIfAbsent(packageEntity.id(), packageEntity);
            packageScopeToEntityId.put(scope.id(), packageEntity.id());
        }

        inferTypeScriptPackageScopes(inventory, state.inferredScopes(), packageScopesById, packageScopeToEntityId, state.inferredEntities());
        createContainmentRelationships(extractionResult, state, packageScopesById, packageScopeToEntityId);

        return new TopologyScopeInferenceContext(packageScopesById, packageScopeToEntityId, fileDirectoryScopeIds, fileModuleScopeIds);
    }

    private static void createContainmentRelationships(
        StructuralExtractionResult extractionResult,
        TopologyInferenceState state,
        Map<String, LogicalScope> packageScopesById,
        Map<String, String> packageScopeToEntityId
    ) {
        for (LogicalScope packageScope : packageScopesById.values()) {
            LogicalScope parentPackageScope = packageScopesById.get(packageScope.parentScopeId());
            if (parentPackageScope == null) {
                continue;
            }
            String parentEntityId = packageScopeToEntityId.get(parentPackageScope.id());
            String childEntityId = packageScopeToEntityId.get(packageScope.id());
            if (parentEntityId != null && childEntityId != null) {
                state.inferredRelationships().putIfAbsent(
                    IdUtils.relationshipId("topology-contains", parentEntityId, childEntityId, packageScope.name()),
                    TopologySupport.contains(parentEntityId, childEntityId, packageScope.name(), packageScope.sourceRefs(), Map.of("rollup", "package-subpackage"))
                );
            }
        }

        for (ArchitectureEntity entity : state.inferredEntities().values()) {
            Object logicalRole = entity.metadata().get("logicalRole");
            if (!"source-root".equals(logicalRole)) {
                continue;
            }
            String modulePath = entity.name();
            for (ExtractedEntityFact fileEntity : fileModuleEntities(extractionResult.entities())) {
                String filePath = TopologySupport.primaryPath(fileEntity);
                if (filePath != null && modulePath.equals(TopologyPaths.moduleRoot(filePath))) {
                    state.inferredRelationships().putIfAbsent(
                        IdUtils.relationshipId("topology-contains", entity.id(), fileEntity.id(), filePath),
                        TopologySupport.contains(entity.id(), fileEntity.id(), filePath, fileEntity.sourceRefs(), Map.of("rollup", "module-file"))
                    );
                }
            }
            for (LogicalScope scope : packageScopesById.values()) {
                if (scope.sourceRefs().stream().anyMatch(ref -> {
                    String p = ref.path();
                    return p != null && modulePath.equals(TopologyPaths.moduleRoot(p));
                })) {
                    LogicalScope parentPackageScope = packageScopesById.get(scope.parentScopeId());
                    boolean topLevelInModule = parentPackageScope == null || parentPackageScope.sourceRefs().stream().noneMatch(ref -> {
                        String p = ref.path();
                        return p != null && modulePath.equals(TopologyPaths.moduleRoot(p));
                    });
                    if (topLevelInModule) {
                        String packageEntityId = packageScopeToEntityId.get(scope.id());
                        if (packageEntityId != null) {
                            state.inferredRelationships().putIfAbsent(
                                IdUtils.relationshipId("topology-contains", entity.id(), packageEntityId, scope.name()),
                                TopologySupport.contains(entity.id(), packageEntityId, scope.name(), scope.sourceRefs(), Map.of("rollup", "module-package"))
                            );
                        }
                    }
                }
            }
        }

        for (ExtractedEntityFact fileEntity : fileModuleEntities(extractionResult.entities())) {
            String packageScopeId = packageScopeIdForFile(fileEntity, packageScopesById.values());
            String packageEntityId = packageScopeId == null ? null : packageScopeToEntityId.get(packageScopeId);
            if (packageEntityId != null) {
                state.inferredRelationships().putIfAbsent(
                    IdUtils.relationshipId("topology-contains", packageEntityId, fileEntity.id(), fileEntity.name()),
                    TopologySupport.contains(packageEntityId, fileEntity.id(), fileEntity.name(), fileEntity.sourceRefs(), Map.of("rollup", "package-file"))
                );
            }
        }
    }

    static List<ExtractedEntityFact> fileModuleEntities(List<ExtractedEntityFact> entities) {
        return entities.stream().filter(entity -> entity.kind() == EntityKind.MODULE).toList();
    }

    static String packageScopeIdForFile(ExtractedEntityFact fileEntity, Collection<LogicalScope> scopes) {
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

    static Map<String, LogicalScope> expandPackageScopeHierarchy(List<LogicalScope> scopes) {
        Map<String, LogicalScope> packageScopes = scopes.stream()
            .filter(scope -> scope.kind() == ScopeKind.PACKAGE)
            .collect(Collectors.toMap(LogicalScope::id, scope -> scope, TopologyScopeInferenceService::mergePackageScopes, LinkedHashMap::new));

        boolean changed;
        do {
            changed = false;
            List<LogicalScope> snapshot = new ArrayList<>(packageScopes.values());
            for (LogicalScope scope : snapshot) {
                String language = String.valueOf(scope.metadata().getOrDefault("language", "unknown"));
                String parentPackageName = TopologyPaths.parentPackageName(scope.name());
                if (parentPackageName == null) {
                    continue;
                }
                String parentScopeId = IdUtils.scopeId(language + "-package", parentPackageName);
                if (!packageScopes.containsKey(parentScopeId)) {
                    packageScopes.put(parentScopeId, TopologySupport.packageScope(
                        parentPackageName,
                        TopologyPaths.parentPackageName(parentPackageName) == null ? "scope:repo" : IdUtils.scopeId(language + "-package", TopologyPaths.parentPackageName(parentPackageName)),
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

    static LogicalScope mergePackageScopes(LogicalScope left, LogicalScope right) {
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
            String moduleRoot = TopologyPaths.moduleRoot(relativePath);
            String directoryPath = TopologyPaths.parentPath(relativePath);
            if (directoryPath == null || moduleRoot == null || directoryPath.equals(moduleRoot)) {
                continue;
            }
            List<String> packagePaths = new ArrayList<>();
            String current = directoryPath;
            while (current != null && !current.isBlank() && !current.equals(moduleRoot)) {
                packagePaths.add(0, current);
                current = TopologyPaths.parentPath(current);
            }
            SourceReference fileRef = new SourceReference(relativePath, null, null, null, Map.of("language", "typescript", "scopeKind", "package"));
            for (String packagePath : packagePaths) {
                String parentPackagePath = TopologyPaths.parentPath(packagePath);
                String parentScopeId = parentPackagePath == null || parentPackagePath.isBlank() || moduleRoot.equals(parentPackagePath)
                    ? IdUtils.scopeId("module", moduleRoot)
                    : IdUtils.scopeId("typescript-package", parentPackagePath);
                LogicalScope scope = TopologySupport.packageScope(packagePath, parentScopeId, "typescript", List.of(fileRef));
                packageScopesById.merge(scope.id(), scope, TopologyScopeInferenceService::mergePackageScopes);
                inferredScopes.putIfAbsent(scope.id(), scope);
                ArchitectureEntity packageEntity = TopologySupport.packageEntity(scope);
                inferredEntities.putIfAbsent(packageEntity.id(), packageEntity);
                packageScopeToEntityId.put(scope.id(), packageEntity.id());
            }
        }
    }
}
