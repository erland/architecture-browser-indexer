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
import java.util.List;
import java.util.Map;

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

        List<LogicalScope> scopes = new ArrayList<>();
        scopes.add(repositoryScope);
        if (extractionResult != null) {
            scopes.addAll(extractionResult.scopes());
        }
        if (topologyResult != null) {
            scopes.addAll(topologyResult.scopes());
        }

        List<ArchitectureEntity> entities = new ArrayList<>();
        entities.add(inventoryEntity);
        if (extractionResult != null) {
            for (ExtractedEntityFact entity : extractionResult.entities()) {
                entities.add(new ArchitectureEntity(
                    entity.id(), entity.kind(), entity.origin(), entity.name(), entity.displayName(), entity.scopeId(), entity.sourceRefs(), entity.metadata()
                ));
            }
        }
        if (interpretationResult != null) {
            for (InterpretedEntityFact entity : interpretationResult.entities()) {
                entities.add(new ArchitectureEntity(
                    entity.id(), entity.kind(), entity.origin(), entity.name(), entity.displayName(), entity.scopeId(), entity.sourceRefs(), entity.metadata()
                ));
            }
        }
        if (topologyResult != null) {
            entities.addAll(topologyResult.entities());
        }

        List<ArchitectureRelationship> relationships = new ArrayList<>();
        if (extractionResult != null) {
            for (ExtractedRelationshipFact relationship : extractionResult.relationships()) {
                relationships.add(new ArchitectureRelationship(
                    relationship.id(), relationship.kind(), relationship.fromEntityId(), relationship.toEntityId(), relationship.label(), relationship.sourceRefs(), relationship.metadata()
                ));
            }
        }
        if (interpretationResult != null) {
            for (InterpretedRelationshipFact relationship : interpretationResult.relationships()) {
                relationships.add(new ArchitectureRelationship(
                    relationship.id(), relationship.kind(), relationship.fromEntityId(), relationship.toEntityId(), relationship.label(), relationship.sourceRefs(), relationship.metadata()
                ));
            }
        }
        if (topologyResult != null) {
            relationships.addAll(topologyResult.relationships());
        }

        int degradedFileCount = parseBatchResult == null ? 0 : (int) parseBatchResult.results().stream().filter(result -> !result.successful()).count();
        CompletenessStatus completenessStatus = degradedFileCount > 0 ? CompletenessStatus.PARTIAL : CompletenessStatus.COMPLETE;
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

        CompletenessMetadata completeness = new CompletenessMetadata(
            completenessStatus,
            inventory.indexedFiles(),
            inventory.totalFiles(),
            degradedFileCount,
            inventory.entries().stream().filter(FileInventoryEntry::ignored).map(FileInventoryEntry::relativePath).toList(),
            List.copyOf(completenessNotes)
        );

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

        RunMetadata runMetadata = new RunMetadata(
            generatedAt,
            generatedAt,
            degradedFileCount > 0 ? RunOutcome.PARTIAL : RunOutcome.SUCCESS,
            inventory.detectedTechnologyMarkers().stream().sorted().toList(),
            Map.of(
                "mode", topologyResult != null ? "cli-topology" : (interpretationResult != null ? "cli-interpretation" : (extractionResult == null ? "cli-inventory" : "cli-structural-extraction")),
                "inventoryOnly", extractionResult == null,
                "structuralExtraction", extractionResult != null,
                "interpretation", interpretationResult != null,
                "topology", topologyResult != null
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

    public static ArchitectureIndexDocument createPlaceholderDocument(RepositorySource source, String indexerVersion) {
        return createInventoryDocument(
            source,
            indexerVersion,
            new FileInventory(List.of(), 0, 0, 0, java.util.Set.of(), java.util.Set.of()),
            List.of()
        );
    }
}
