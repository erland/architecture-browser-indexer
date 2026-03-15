package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseDiagnostics;
import info.isaksson.erland.architecturebrowser.indexer.parse.TreeSitterParsingService;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ArchitectureIrFactory {
    private ArchitectureIrFactory() {
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, parseBatchResult, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult,
        StructuralExtractionResult extractionResult
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, parseBatchResult, extractionResult, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult,
        StructuralExtractionResult extractionResult,
        InterpretationResult interpretationResult
    ) {
        return createInventoryDocument(source, indexerVersion, inventory, acquisitionDiagnostics, parseBatchResult, extractionResult, interpretationResult, null);
    }

    public static ArchitectureIndexDocument createInventoryDocument(
        RepositorySource source,
        String indexerVersion,
        FileInventory inventory,
        List<Diagnostic> acquisitionDiagnostics,
        ParseBatchResult parseBatchResult,
        StructuralExtractionResult extractionResult,
        InterpretationResult interpretationResult,
        TopologyResult topologyResult
    ) {
        Instant generatedAt = Instant.now();

        LogicalScope repositoryScope = new LogicalScope(
            "scope:repo",
            ScopeKind.REPOSITORY,
            source.repositoryId(),
            source.repositoryId(),
            null,
            List.of(),
            Map.of("acquisitionType", source.acquisitionType())
        );

        SourceReference firstSource = inventory.entries().stream()
            .filter(entry -> !entry.ignored())
            .findFirst()
            .map(entry -> new SourceReference(entry.relativePath(), null, null, null, Map.of("type", entry.type())))
            .orElse(null);

        ArchitectureEntity inventoryEntity = new ArchitectureEntity(
            "entity:inventory:root",
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            "Repository inventory",
            source.repositoryId() + ":inventory",
            repositoryScope.id(),
            firstSource == null ? List.of() : List.of(firstSource),
            Map.of(
                "indexedFileCount", inventory.indexedFiles(),
                "totalFileCount", inventory.totalFiles(),
                "detectedLanguages", inventory.detectedLanguages(),
                "detectedTechnologyMarkers", inventory.detectedTechnologyMarkers()
            )
        );

        List<Diagnostic> diagnostics = new ArrayList<>();
        if (acquisitionDiagnostics == null || acquisitionDiagnostics.isEmpty()) {
            diagnostics.add(new Diagnostic(
                "diag:inventory:scan-complete",
                DiagnosticSeverity.INFO,
                DiagnosticPhase.ACQUISITION,
                "inventory.scan.complete",
                "Acquisition and file inventory completed",
                false,
                null,
                repositoryScope.id(),
                inventoryEntity.id(),
                inventoryEntity.sourceRefs(),
                Map.of("totalFiles", inventory.totalFiles(), "ignoredFiles", inventory.ignoredFiles())
            ));
        } else {
            diagnostics.addAll(acquisitionDiagnostics);
        }
        if (parseBatchResult != null) {
            diagnostics.addAll(ParseDiagnostics.toDiagnostics(parseBatchResult));
        }
        if (extractionResult != null) {
            diagnostics.addAll(extractionResult.diagnostics());
        }
        if (interpretationResult != null) {
            diagnostics.addAll(interpretationResult.diagnostics());
        }
        if (topologyResult != null) {
            diagnostics.addAll(topologyResult.diagnostics());
        }

        Map<String, LogicalScope> scopesById = new LinkedHashMap<>();
        scopesById.put(repositoryScope.id(), repositoryScope);
        if (extractionResult != null) {
            for (LogicalScope scope : extractionResult.scopes()) {
                scopesById.put(scope.id(), scope);
            }
        }
        if (topologyResult != null) {
            for (LogicalScope scope : topologyResult.scopes()) {
                scopesById.put(scope.id(), scope);
            }
        }
        List<LogicalScope> scopes = List.copyOf(scopesById.values());

        Map<String, ArchitectureEntity> entitiesById = new LinkedHashMap<>();
        entitiesById.put(inventoryEntity.id(), inventoryEntity);
        if (extractionResult != null) {
            for (ExtractedEntityFact entity : extractionResult.entities()) {
                entitiesById.put(entity.id(), new ArchitectureEntity(
                    entity.id(), entity.kind(), entity.origin(), entity.name(), entity.displayName(), normalizeScopeId(entity.scopeId(), repositoryScope.id()), entity.sourceRefs(), entity.metadata()
                ));
            }
        }
        if (interpretationResult != null) {
            for (InterpretedEntityFact entity : interpretationResult.entities()) {
                entitiesById.put(entity.id(), new ArchitectureEntity(
                    entity.id(), entity.kind(), entity.origin(), entity.name(), entity.displayName(), normalizeScopeId(entity.scopeId(), repositoryScope.id()), entity.sourceRefs(), entity.metadata()
                ));
            }
        }
        if (topologyResult != null) {
            for (ArchitectureEntity entity : topologyResult.entities()) {
                entitiesById.put(entity.id(), entity);
            }
        }
        List<ArchitectureEntity> entities = List.copyOf(entitiesById.values());

        Map<String, ArchitectureRelationship> relationshipsById = new LinkedHashMap<>();
        if (extractionResult != null) {
            for (ExtractedRelationshipFact relationship : extractionResult.relationships()) {
                ArchitectureRelationship architectureRelationship = new ArchitectureRelationship(
                    relationship.id(), relationship.kind(), relationship.fromEntityId(), relationship.toEntityId(), relationship.label(), relationship.sourceRefs(), relationship.metadata()
                );
                relationshipsById.put(architectureRelationship.id(), architectureRelationship);
            }
        }
        if (interpretationResult != null) {
            for (InterpretedRelationshipFact relationship : interpretationResult.relationships()) {
                ArchitectureRelationship architectureRelationship = new ArchitectureRelationship(
                    relationship.id(), relationship.kind(), relationship.fromEntityId(), relationship.toEntityId(), relationship.label(), relationship.sourceRefs(), relationship.metadata()
                );
                relationshipsById.put(architectureRelationship.id(), architectureRelationship);
            }
        }
        if (topologyResult != null) {
            for (ArchitectureRelationship relationship : topologyResult.relationships()) {
                relationshipsById.put(relationship.id(), relationship);
            }
        }
        List<ArchitectureRelationship> relationships = enrichDependencyRelationshipMetadata(List.copyOf(relationshipsById.values()), entitiesById);

        List<String> completenessNotes = new ArrayList<>();
        if (extractionResult == null) {
            completenessNotes.add("Inventory-only payload produced before structural extraction is implemented");
        } else if (interpretationResult == null) {
            completenessNotes.add("Structural extraction included syntax-tree-based extraction without interpretation");
        } else if (topologyResult == null) {
            completenessNotes.add("Structural extraction and first-pass interpretation rules were included");
        } else {
            completenessNotes.add("Structural extraction, interpretation, logical scoping, and relationship inference were included");
        }

        RunAssessment assessment = RunAssessment.assess(inventory, parseBatchResult, diagnostics, completenessNotes);
        CompletenessMetadata completeness = assessment.completeness();

        Map<String, Object> documentMetadata = new LinkedHashMap<>();
        documentMetadata.put("inventoryEntries", inventory.entries());
        documentMetadata.put("inventorySummary", Map.of(
            "totalFiles", inventory.totalFiles(),
            "indexedFiles", inventory.indexedFiles(),
            "ignoredFiles", inventory.ignoredFiles(),
            "detectedLanguages", inventory.detectedLanguages(),
            "detectedTechnologyMarkers", inventory.detectedTechnologyMarkers()
        ));
        if (parseBatchResult != null) {
            documentMetadata.put("parseSummary", TreeSitterParsingService.summarize(parseBatchResult));
        }
        if (extractionResult != null) {
            documentMetadata.put("extractionSummary", extractionResult.summary());
        }
        if (interpretationResult != null) {
            documentMetadata.put("interpretationSummary", interpretationResult.summary());
        }
        if (topologyResult != null) {
            documentMetadata.put("topologySummary", topologyResult.summary());
        }
        documentMetadata.put("dependencyViews", buildDependencyViews(relationships, entitiesById));
        documentMetadata.put("diagnosticSummary", assessment.diagnosticSummary());
        documentMetadata.put("partialResult", assessment.partialResult());

        RunMetadata runMetadata = new RunMetadata(
            generatedAt,
            generatedAt,
            assessment.outcome(),
            inventory.detectedTechnologyMarkers().stream().sorted().toList(),
            Map.of(
                "mode", topologyResult != null ? "cli-topology" : (interpretationResult != null ? "cli-interpretation" : (extractionResult == null ? "cli-inventory" : "cli-structural-extraction")),
                "inventoryOnly", extractionResult == null,
                "structuralExtraction", extractionResult != null,
                "interpretation", interpretationResult != null,
                "topology", topologyResult != null,
                "degradedPaths", assessment.degradedPaths()
            )
        );

        return new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            indexerVersion,
            runMetadata,
            source,
            List.copyOf(scopes),
            List.copyOf(entities),
            List.copyOf(relationships),
            diagnostics,
            completeness,
            Map.copyOf(documentMetadata)
        );
    }


    private static List<ArchitectureRelationship> enrichDependencyRelationshipMetadata(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        List<ArchitectureRelationship> enriched = new ArrayList<>(relationships.size());
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = entitiesById.get(relationship.fromEntityId());
            ArchitectureEntity target = entitiesById.get(relationship.toEntityId());
            if (!isDependencyRelationship(relationship.kind())) {
                enriched.add(relationship);
                continue;
            }
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (relationship.metadata() != null) {
                metadata.putAll(relationship.metadata());
            }
            if (isTypeDependencyRelationship(relationship, source, target)) {
                metadata.put("dependencyView", "type");
                metadata.put("dependencySourceTypeId", relationship.fromEntityId());
                metadata.put("dependencyTargetTypeId", relationship.toEntityId());
                metadata.put("dependencyTargetInternal", isInternalEntity(target));
                metadata.put("dependencyTargetExternal", isExternalEntity(target));
            } else if (isImportEvidenceRelationship(relationship, source, target)) {
                metadata.putIfAbsent("dependencyView", "evidence");
                metadata.put("dependencyTargetInternal", isInternalEntity(target));
                metadata.put("dependencyTargetExternal", isExternalEntity(target));
            }
            enriched.add(new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                Map.copyOf(metadata)
            ));
        }
        return List.copyOf(enriched);
    }

    private static Map<String, Object> buildDependencyViews(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        Map<String, NormalizedTypeDependency> byKey = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            ArchitectureEntity source = entitiesById.get(relationship.fromEntityId());
            ArchitectureEntity target = entitiesById.get(relationship.toEntityId());
            if (!isTypeDependencyRelationship(relationship, source, target)) {
                continue;
            }
            String key = relationship.kind().name() + "|" + relationship.fromEntityId() + "|" + relationship.toEntityId();
            byKey.computeIfAbsent(key, ignored -> new NormalizedTypeDependency(
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.kind(),
                source == null ? null : source.name(),
                target == null ? null : target.name(),
                isInternalEntity(target),
                isExternalEntity(target)
            )).addEvidence(relationship);
        }
        List<Map<String, Object>> typeDependencies = new ArrayList<>();
        for (NormalizedTypeDependency dependency : byKey.values()) {
            typeDependencies.add(dependency.toMetadataMap());
        }
        return Map.of("typeDependencies", List.copyOf(typeDependencies));
    }

    private static boolean isDependencyRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.DEPENDS_ON || kind == RelationshipKind.EXTENDS || kind == RelationshipKind.IMPLEMENTS;
    }

    private static boolean isTypeDependencyRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        return isDependencyRelationship(relationship.kind()) && isTypeEntity(source) && isTypeEntity(target);
    }

    private static boolean isImportEvidenceRelationship(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target
    ) {
        if (relationship.kind() != RelationshipKind.DEPENDS_ON || source == null || target == null) {
            return false;
        }
        Object dependencySource = relationship.metadata() == null ? null : relationship.metadata().get("dependencySource");
        return source.kind() == EntityKind.MODULE && target.kind() == EntityKind.MODULE && Objects.equals("import", dependencySource);
    }

    private static boolean isTypeEntity(ArchitectureEntity entity) {
        return entity != null && (entity.kind() == EntityKind.CLASS || entity.kind() == EntityKind.INTERFACE);
    }

    private static boolean isInternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return !Boolean.TRUE.equals(external) && entity.origin() == EntityOrigin.OBSERVED;
    }

    private static boolean isExternalEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        Object external = entity.metadata() == null ? null : entity.metadata().get("external");
        return Boolean.TRUE.equals(external) || entity.origin() == EntityOrigin.INFERRED;
    }

    private static final class NormalizedTypeDependency {
        private final String sourceTypeId;
        private final String targetTypeId;
        private final RelationshipKind relationshipKind;
        private final String sourceTypeName;
        private final String targetTypeName;
        private final boolean internalTarget;
        private final boolean externalTarget;
        private final Set<String> dependencySources = new LinkedHashSet<>();
        private final Set<String> dependencyCategories = new LinkedHashSet<>();
        private final Set<String> evidenceRelationshipIds = new LinkedHashSet<>();
        private final Set<String> evidenceLabels = new LinkedHashSet<>();

        private NormalizedTypeDependency(
            String sourceTypeId,
            String targetTypeId,
            RelationshipKind relationshipKind,
            String sourceTypeName,
            String targetTypeName,
            boolean internalTarget,
            boolean externalTarget
        ) {
            this.sourceTypeId = sourceTypeId;
            this.targetTypeId = targetTypeId;
            this.relationshipKind = relationshipKind;
            this.sourceTypeName = sourceTypeName;
            this.targetTypeName = targetTypeName;
            this.internalTarget = internalTarget;
            this.externalTarget = externalTarget;
        }

        private void addEvidence(ArchitectureRelationship relationship) {
            evidenceRelationshipIds.add(relationship.id());
            if (relationship.label() != null && !relationship.label().isBlank()) {
                evidenceLabels.add(relationship.label());
            }
            if (relationship.metadata() != null) {
                addIfPresent(dependencySources, relationship.metadata().get("dependencySource"));
                addIfPresent(dependencyCategories, relationship.metadata().get("dependencyCategory"));
            }
        }

        private Map<String, Object> toMetadataMap() {
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("sourceTypeId", sourceTypeId);
            metadata.put("targetTypeId", targetTypeId);
            metadata.put("relationshipKind", relationshipKind.name());
            if (sourceTypeName != null) {
                metadata.put("sourceTypeName", sourceTypeName);
            }
            if (targetTypeName != null) {
                metadata.put("targetTypeName", targetTypeName);
            }
            metadata.put("dependencySources", List.copyOf(dependencySources));
            metadata.put("dependencyCategories", List.copyOf(dependencyCategories));
            metadata.put("internalTarget", internalTarget);
            metadata.put("externalTarget", externalTarget);
            metadata.put("evidenceRelationshipCount", evidenceRelationshipIds.size());
            metadata.put("evidenceRelationshipIds", List.copyOf(evidenceRelationshipIds));
            metadata.put("evidenceLabels", List.copyOf(evidenceLabels));
            return Map.copyOf(metadata);
        }

        private static void addIfPresent(Set<String> sink, Object value) {
            if (value == null) {
                return;
            }
            String asString = Objects.toString(value, "").trim();
            if (!asString.isEmpty()) {
                sink.add(asString);
            }
        }
    }

    public static ArchitectureIndexDocument createPlaceholderDocument(RepositorySource source, String indexerVersion) {
        return createInventoryDocument(
            source,
            indexerVersion,
            new FileInventory(List.of(), 0, 0, 0, java.util.Set.of(), java.util.Set.of()),
            List.of()
        );
    }

    private static String normalizeScopeId(String scopeId, String repositoryScopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return repositoryScopeId;
        }
        return scopeId;
    }
}
