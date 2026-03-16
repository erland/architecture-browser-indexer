package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;

import java.util.List;

record ArchitectureIrAssemblyInputs(
    RepositorySource source,
    FileInventory inventory,
    List<Diagnostic> acquisitionDiagnostics,
    ParseBatchResult parseBatchResult,
    StructuralExtractionResult extractionResult,
    InterpretationResult interpretationResult,
    TopologyResult topologyResult
) {
}
