package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;

import java.time.Instant;
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
        ArchitectureIrAssemblyInputs inputs = new ArchitectureIrAssemblyInputs(
            source,
            inventory,
            acquisitionDiagnostics,
            parseBatchResult,
            extractionResult,
            interpretationResult,
            topologyResult
        );
        ArchitectureIrAssemblyState assembly = ArchitectureIrAssemblyStateBuilder.build(inputs);
        List<String> completenessNotes = ArchitectureIrCompletenessNotesBuilder.build(extractionResult, interpretationResult, topologyResult);
        RunAssessment assessment = RunAssessment.assess(inventory, parseBatchResult, assembly.diagnostics(), completenessNotes);
        CompletenessMetadata completeness = assessment.completeness();
        Map<String, Object> documentMetadata = ArchitectureIrDocumentMetadataBuilder.build(inputs, assembly, assessment);
        RunMetadata runMetadata = ArchitectureIrRunMetadataBuilder.build(
            generatedAt,
            inventory,
            extractionResult,
            interpretationResult,
            topologyResult,
            assessment
        );

        return new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            indexerVersion,
            runMetadata,
            source,
            assembly.scopes(),
            assembly.entities(),
            assembly.relationships(),
            ArchitectureIrViewpointDerivationService.derive(
                assembly.entities(),
                assembly.relationships(),
                assembly.dependencyViews()
            ),
            assembly.diagnostics(),
            completeness,
            documentMetadata
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
