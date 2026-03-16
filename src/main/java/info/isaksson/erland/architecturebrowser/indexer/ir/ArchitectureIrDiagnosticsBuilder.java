package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseDiagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ArchitectureIrDiagnosticsBuilder {
    private ArchitectureIrDiagnosticsBuilder() {
    }

    static List<Diagnostic> build(ArchitectureIrAssemblyInputs inputs, LogicalScope repositoryScope, ArchitectureEntity inventoryEntity) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (inputs.acquisitionDiagnostics() == null || inputs.acquisitionDiagnostics().isEmpty()) {
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
                Map.of("totalFiles", inputs.inventory().totalFiles(), "ignoredFiles", inputs.inventory().ignoredFiles())
            ));
        } else {
            diagnostics.addAll(inputs.acquisitionDiagnostics());
        }
        if (inputs.parseBatchResult() != null) {
            diagnostics.addAll(ParseDiagnostics.toDiagnostics(inputs.parseBatchResult()));
        }
        if (inputs.extractionResult() != null) {
            diagnostics.addAll(inputs.extractionResult().diagnostics());
        }
        if (inputs.interpretationResult() != null) {
            diagnostics.addAll(inputs.interpretationResult().diagnostics());
        }
        if (inputs.topologyResult() != null) {
            diagnostics.addAll(inputs.topologyResult().diagnostics());
        }
        return List.copyOf(diagnostics);
    }
}
