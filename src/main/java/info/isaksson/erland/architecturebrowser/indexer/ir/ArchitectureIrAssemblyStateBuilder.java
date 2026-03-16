package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ArchitectureIrAssemblyStateBuilder {
    private ArchitectureIrAssemblyStateBuilder() {
    }

    static ArchitectureIrAssemblyState build(ArchitectureIrAssemblyInputs inputs) {
        LogicalScope repositoryScope = createRepositoryScope(inputs.source());
        ArchitectureEntity inventoryEntity = createInventoryEntity(inputs, repositoryScope);
        List<info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic> diagnostics =
            ArchitectureIrDiagnosticsBuilder.build(inputs, repositoryScope, inventoryEntity);
        List<LogicalScope> scopes = assembleScopes(inputs, repositoryScope);
        Map<String, ArchitectureEntity> entitiesById = assembleEntitiesById(inputs, repositoryScope, inventoryEntity);
        Map<String, ArchitectureEntity> observedTypesByQualifiedName = ArchitectureIrAssemblyCompositionSupport.observedTypesByQualifiedName(entitiesById);
        List<ArchitectureRelationship> relationships = assembleRelationships(inputs, entitiesById, observedTypesByQualifiedName);
        Map<String, Object> dependencyViews = ArchitectureIrAssemblyCompositionSupport.buildDependencyViews(
            new ArchitectureIrDependencyViewAssemblyInputs(
                relationships,
                entitiesById,
                observedTypesByQualifiedName
            )
        );
        Map<String, ArchitectureEntity> enrichedEntitiesById = ArchitectureIrPackageEntityEnrichmentSupport.enrichPackageEntities(entitiesById, dependencyViews);
        return new ArchitectureIrAssemblyState(
            repositoryScope,
            inventoryEntity,
            scopes,
            List.copyOf(enrichedEntitiesById.values()),
            relationships,
            diagnostics,
            Map.copyOf(enrichedEntitiesById),
            observedTypesByQualifiedName,
            dependencyViews
        );
    }

    private static LogicalScope createRepositoryScope(info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource source) {
        return new LogicalScope(
            "scope:repo",
            ScopeKind.REPOSITORY,
            source.repositoryId(),
            source.repositoryId(),
            null,
            List.of(),
            Map.of("acquisitionType", source.acquisitionType())
        );
    }

    private static ArchitectureEntity createInventoryEntity(ArchitectureIrAssemblyInputs inputs, LogicalScope repositoryScope) {
        SourceReference firstSource = inputs.inventory().entries().stream()
            .filter(entry -> !entry.ignored())
            .findFirst()
            .map(entry -> new SourceReference(entry.relativePath(), null, null, null, Map.of("type", entry.type())))
            .orElse(null);
        return new ArchitectureEntity(
            "entity:inventory:root",
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            "Repository inventory",
            inputs.source().repositoryId() + ":inventory",
            repositoryScope.id(),
            firstSource == null ? List.of() : List.of(firstSource),
            Map.of(
                "indexedFileCount", inputs.inventory().indexedFiles(),
                "totalFileCount", inputs.inventory().totalFiles(),
                "detectedLanguages", inputs.inventory().detectedLanguages(),
                "detectedTechnologyMarkers", inputs.inventory().detectedTechnologyMarkers()
            )
        );
    }

    private static List<LogicalScope> assembleScopes(ArchitectureIrAssemblyInputs inputs, LogicalScope repositoryScope) {
        Map<String, LogicalScope> scopesById = new LinkedHashMap<>();
        scopesById.put(repositoryScope.id(), repositoryScope);
        if (inputs.extractionResult() != null) {
            for (LogicalScope scope : inputs.extractionResult().scopes()) {
                scopesById.put(scope.id(), scope);
            }
        }
        if (inputs.topologyResult() != null) {
            for (LogicalScope scope : inputs.topologyResult().scopes()) {
                scopesById.put(scope.id(), scope);
            }
        }
        return List.copyOf(scopesById.values());
    }

    private static Map<String, ArchitectureEntity> assembleEntitiesById(
        ArchitectureIrAssemblyInputs inputs,
        LogicalScope repositoryScope,
        ArchitectureEntity inventoryEntity
    ) {
        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(inventoryEntity.id(), inventoryEntity);
        if (inputs.extractionResult() != null) {
            for (ExtractedEntityFact entity : inputs.extractionResult().entities()) {
                entitiesById.put(entity.id(), new ArchitectureEntity(
                    entity.id(),
                    entity.kind(),
                    entity.origin(),
                    entity.name(),
                    entity.displayName(),
                    ArchitectureIrScopeNormalizationSupport.normalizeScopeId(entity.scopeId(), repositoryScope.id()),
                    entity.sourceRefs(),
                    entity.metadata()
                ));
            }
        }
        if (inputs.interpretationResult() != null) {
            for (InterpretedEntityFact entity : inputs.interpretationResult().entities()) {
                entitiesById.put(entity.id(), new ArchitectureEntity(
                    entity.id(),
                    entity.kind(),
                    entity.origin(),
                    entity.name(),
                    entity.displayName(),
                    ArchitectureIrScopeNormalizationSupport.normalizeScopeId(entity.scopeId(), repositoryScope.id()),
                    entity.sourceRefs(),
                    entity.metadata()
                ));
            }
        }
        if (inputs.topologyResult() != null) {
            for (ArchitectureEntity entity : inputs.topologyResult().entities()) {
                entitiesById.put(entity.id(), entity);
            }
        }
        return entitiesById;
    }

    private static List<ArchitectureRelationship> assembleRelationships(
        ArchitectureIrAssemblyInputs inputs,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureEntity> observedTypesByQualifiedName
    ) {
        Map<String, ArchitectureRelationship> relationshipsById = new LinkedHashMap<>();
        if (inputs.extractionResult() != null) {
            for (ExtractedRelationshipFact relationship : inputs.extractionResult().relationships()) {
                ArchitectureRelationship architectureRelationship = new ArchitectureRelationship(
                    relationship.id(),
                    relationship.kind(),
                    relationship.fromEntityId(),
                    relationship.toEntityId(),
                    relationship.label(),
                    relationship.sourceRefs(),
                    relationship.metadata()
                );
                relationshipsById.put(architectureRelationship.id(), architectureRelationship);
            }
        }
        if (inputs.interpretationResult() != null) {
            for (InterpretedRelationshipFact relationship : inputs.interpretationResult().relationships()) {
                ArchitectureRelationship architectureRelationship = new ArchitectureRelationship(
                    relationship.id(),
                    relationship.kind(),
                    relationship.fromEntityId(),
                    relationship.toEntityId(),
                    relationship.label(),
                    relationship.sourceRefs(),
                    relationship.metadata()
                );
                relationshipsById.put(architectureRelationship.id(), architectureRelationship);
            }
        }
        if (inputs.topologyResult() != null) {
            for (ArchitectureRelationship relationship : inputs.topologyResult().relationships()) {
                relationshipsById.put(relationship.id(), relationship);
            }
        }
        List<ArchitectureRelationship> relationships = ArchitectureIrDependencyRelationshipEnricher.enrichDependencyRelationshipMetadata(
            List.copyOf(relationshipsById.values()),
            entitiesById,
            observedTypesByQualifiedName
        );
        return ArchitectureIrAssemblyCompositionSupport.ensurePackageDependencyRelationships(relationships, entitiesById, observedTypesByQualifiedName);
    }
}
