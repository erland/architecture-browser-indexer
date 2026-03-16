package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunMetadata;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;

import java.time.Instant;
import java.util.Map;

final class ArchitectureIrRunMetadataBuilder {
    private ArchitectureIrRunMetadataBuilder() {
    }

    static RunMetadata build(
        Instant generatedAt,
        FileInventory inventory,
        StructuralExtractionResult extractionResult,
        InterpretationResult interpretationResult,
        TopologyResult topologyResult,
        RunAssessment assessment
    ) {
        return new RunMetadata(
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
    }
}
