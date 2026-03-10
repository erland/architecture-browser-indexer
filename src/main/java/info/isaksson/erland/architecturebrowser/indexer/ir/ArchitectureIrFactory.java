package info.isaksson.erland.architecturebrowser.indexer.ir;

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
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;

import java.time.Instant;
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

        ArchitectureRelationship containsRelationship = new ArchitectureRelationship(
            "rel:repo:contains:inventory",
            RelationshipKind.CONTAINS,
            inventoryEntity.id(),
            inventoryEntity.id(),
            "placeholder relationship retained until structural extraction exists",
            inventoryEntity.sourceRefs(),
            Map.of("placeholder", true)
        );

        List<Diagnostic> diagnostics = acquisitionDiagnostics == null || acquisitionDiagnostics.isEmpty()
            ? List.of(new Diagnostic(
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
            ))
            : List.copyOf(acquisitionDiagnostics);

        CompletenessMetadata completeness = new CompletenessMetadata(
            CompletenessStatus.COMPLETE,
            inventory.indexedFiles(),
            inventory.totalFiles(),
            0,
            inventory.entries().stream().filter(FileInventoryEntry::ignored).map(FileInventoryEntry::relativePath).toList(),
            List.of("Inventory-only payload produced before structural extraction is implemented")
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

        RunMetadata runMetadata = new RunMetadata(
            generatedAt,
            generatedAt,
            RunOutcome.SUCCESS,
            inventory.detectedTechnologyMarkers().stream().sorted().toList(),
            Map.of("mode", "cli-inventory", "inventoryOnly", true)
        );

        return new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            indexerVersion,
            runMetadata,
            source,
            List.of(repositoryScope),
            List.of(inventoryEntity),
            List.of(),
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
