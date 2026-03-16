package info.isaksson.erland.architecturebrowser.indexer.topology;

import info.isaksson.erland.architecturebrowser.indexer.extract.IdUtils;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class TopologyRelationshipRollupService {
    private final TopologyRelationshipResolver relationshipResolver;

    TopologyRelationshipRollupService(TopologyRelationshipResolver relationshipResolver) {
        this.relationshipResolver = relationshipResolver;
    }

    void inferRelationshipRollups(StructuralExtractionResult extractionResult, TopologyScopeInferenceContext scopeContext, TopologyInferenceState state) {
        Map<String, ExtractedEntityFact> extractedEntitiesById = extractionResult.entities().stream()
            .collect(Collectors.toMap(ExtractedEntityFact::id, entity -> entity, (left, right) -> left, LinkedHashMap::new));
        Map<String, ExtractedEntityFact> structuralTypesByQualifiedName = extractionResult.entities().stream()
            .filter(TopologyRelationshipRollupService::isStructuralTypeEntity)
            .filter(entity -> entity.metadata().get("qualifiedName") != null)
            .collect(Collectors.toMap(entity -> String.valueOf(entity.metadata().get("qualifiedName")), entity -> entity, (left, right) -> left, LinkedHashMap::new));
        Map<String, ExtractedEntityFact> fileModulesByPath = TopologyScopeInferenceService.fileModuleEntities(extractionResult.entities()).stream()
            .collect(Collectors.toMap(ExtractedEntityFact::name, entity -> entity, (left, right) -> left, LinkedHashMap::new));

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
            state.inferredRelationships().putIfAbsent(directRelationship.id(), directRelationship);

            if (evidenceImport) {
                continue;
            }

            String fromPackageScopeId = packageScopeIdForEntity(fromEntity, scopeContext.packageScopesById().values());
            String toPackageScopeId = packageScopeIdForEntity(targetEntity, scopeContext.packageScopesById().values());
            String fromPackageEntityId = fromPackageScopeId == null ? null : scopeContext.packageScopeToEntityId().get(fromPackageScopeId);
            String toPackageEntityId = toPackageScopeId == null ? null : scopeContext.packageScopeToEntityId().get(toPackageScopeId);
            if (fromPackageEntityId != null && toPackageEntityId != null && !fromPackageEntityId.equals(toPackageEntityId)) {
                String key = relationship.kind().name() + ":" + fromPackageEntityId + "->" + toPackageEntityId + "|" + rollupDependencySignature(relationship);
                if (seenPackageUses.add(key)) {
                    Map<String, Object> packageMetadata = topologyMetadata(relationship, Map.of("rollup", "package-package"));
                    ArchitectureRelationship pkgRelationship = switch (relationship.kind()) {
                        case EXTENDS -> TopologySupport.typedRelationship(RelationshipKind.EXTENDS, fromPackageEntityId, toPackageEntityId, relationship.label(), relationship.sourceRefs(), packageMetadata);
                        case IMPLEMENTS -> TopologySupport.typedRelationship(RelationshipKind.IMPLEMENTS, fromPackageEntityId, toPackageEntityId, relationship.label(), relationship.sourceRefs(), packageMetadata);
                        default -> TopologySupport.uses(fromPackageEntityId, toPackageEntityId, relationship.label(), relationship.sourceRefs(), packageMetadata);
                    };
                    state.inferredRelationships().putIfAbsent(pkgRelationship.id(), pkgRelationship);
                }
            }

            String fromModuleEntityId = TopologyPaths.sourceRootEntityId(fromPath);
            String toModuleEntityId = TopologyPaths.sourceRootEntityId(TopologySupport.primaryPath(targetEntity));
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
                    state.inferredRelationships().putIfAbsent(moduleRelationship.id(), moduleRelationship);
                }
            }
        }
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

    static String packageScopeIdForEntity(ExtractedEntityFact entity, Collection<LogicalScope> scopes) {
        if (entity == null) {
            return null;
        }
        String language = String.valueOf(entity.metadata().getOrDefault("language", "java"));
        if ((entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE) && !"typescript".equalsIgnoreCase(language)) {
            return entity.scopeId();
        }
        String ownerQualifiedName = String.valueOf(entity.metadata().getOrDefault("ownerQualifiedName", ""));
        if (!ownerQualifiedName.isBlank()) {
            String ownerPackage = TopologyPaths.parentQualifiedName(ownerQualifiedName);
            if (ownerPackage != null && !ownerPackage.isBlank()) {
                String expectedScopeId = IdUtils.scopeId(language + "-package", ownerPackage);
                if (scopes.stream().anyMatch(scope -> expectedScopeId.equals(scope.id()))) {
                    return expectedScopeId;
                }
            }
        }
        return TopologyScopeInferenceService.packageScopeIdForFile(entity, scopes);
    }

    private static boolean isJavaStructuralEntity(EntityKind kind) {
        return kind == EntityKind.CLASS || kind == EntityKind.INTERFACE || kind == EntityKind.FIELD || kind == EntityKind.FUNCTION;
    }

    private static boolean isTopologyRelevantRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.DEPENDS_ON
            || kind == RelationshipKind.EXTENDS
            || kind == RelationshipKind.IMPLEMENTS;
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
        addRollupSignaturePart(parts, "relationshipType", relationship.metadata().get("relationshipType"));
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
}
