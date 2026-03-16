package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;

import java.util.ArrayList;
import java.util.List;

final class ArchitectureIrCompletenessNotesBuilder {
    private ArchitectureIrCompletenessNotesBuilder() {
    }

    static List<String> build(
        StructuralExtractionResult extractionResult,
        InterpretationResult interpretationResult,
        TopologyResult topologyResult
    ) {
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
        return List.copyOf(completenessNotes);
    }
}
