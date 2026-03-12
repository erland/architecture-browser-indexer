package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessMetadata;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RunOutcome;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class RunAssessment {
    private final RunOutcome outcome;
    private final CompletenessMetadata completeness;
    private final Map<String, Object> diagnosticSummary;
    private final Map<String, Object> partialResult;
    private final List<String> degradedPaths;

    private RunAssessment(
        RunOutcome outcome,
        CompletenessMetadata completeness,
        Map<String, Object> diagnosticSummary,
        Map<String, Object> partialResult,
        List<String> degradedPaths
    ) {
        this.outcome = outcome;
        this.completeness = completeness;
        this.diagnosticSummary = diagnosticSummary;
        this.partialResult = partialResult;
        this.degradedPaths = degradedPaths;
    }

    static RunAssessment assess(FileInventory inventory, ParseBatchResult parseBatchResult, List<Diagnostic> diagnostics, List<String> completenessNotes) {
        List<Diagnostic> safeDiagnostics = diagnostics == null ? List.of() : diagnostics;
        Map<String, Integer> bySeverity = new LinkedHashMap<>();
        Map<String, Integer> byPhase = new LinkedHashMap<>();
        int fatalCount = 0;
        LinkedHashSet<String> degradedPaths = new LinkedHashSet<>();
        for (Diagnostic diagnostic : safeDiagnostics) {
            bySeverity.merge(diagnostic.severity().name(), 1, Integer::sum);
            byPhase.merge(diagnostic.phase().name(), 1, Integer::sum);
            if (diagnostic.fatal()) {
                fatalCount++;
            }
            if (diagnostic.filePath() != null && (diagnostic.severity() != DiagnosticSeverity.INFO || diagnostic.fatal())) {
                degradedPaths.add(diagnostic.filePath());
            }
        }

        int parseSuccessCount = 0;
        int parseSkippedCount = 0;
        int parseBackendUnavailableCount = 0;
        int parseUnsupportedCount = 0;
        int parseErrorCount = 0;
        int recoverableParseErrorCount = 0;
        if (parseBatchResult != null) {
            for (var result : parseBatchResult.results()) {
                switch (result.status()) {
                    case SUCCESS -> parseSuccessCount++;
                    case SKIPPED -> parseSkippedCount++;
                    case BACKEND_UNAVAILABLE -> {
                        parseBackendUnavailableCount++;
                        degradedPaths.add(result.request().relativePath());
                    }
                    case UNSUPPORTED_LANGUAGE -> {
                        parseUnsupportedCount++;
                        degradedPaths.add(result.request().relativePath());
                    }
                    case PARSE_ERROR -> {
                        parseErrorCount++;
                        if (result.syntaxTree() == null) {
                            degradedPaths.add(result.request().relativePath());
                        } else {
                            recoverableParseErrorCount++;
                        }
                    }
                }
            }
        }

        int indexedFileCount = inventory == null ? 0 : inventory.indexedFiles();
        int totalFileCount = inventory == null ? 0 : inventory.totalFiles();
        int ignoredFileCount = inventory == null ? 0 : inventory.ignoredFiles();
        List<String> omittedPaths = inventory == null ? List.of() : inventory.entries().stream()
            .filter(FileInventoryEntry::ignored)
            .map(FileInventoryEntry::relativePath)
            .toList();

        int degradedFileCount = degradedPaths.size();

        RunOutcome outcome;
        if (fatalCount > 0) {
            outcome = RunOutcome.FAILED;
        } else if (degradedFileCount > 0) {
            outcome = RunOutcome.PARTIAL;
        } else {
            outcome = RunOutcome.SUCCESS;
        }

        CompletenessStatus status;
        if (outcome == RunOutcome.FAILED) {
            status = CompletenessStatus.PARTIAL;
        } else if (degradedFileCount > 0) {
            status = CompletenessStatus.PARTIAL;
        } else {
            status = CompletenessStatus.COMPLETE;
        }

        List<String> notes = new ArrayList<>(completenessNotes == null ? List.of() : completenessNotes);
        if (parseErrorCount > 0) {
            if (recoverableParseErrorCount > 0) {
                notes.add(recoverableParseErrorCount + " file(s) had recoverable parse errors but still produced syntax trees");
            }
            int nonRecoverableParseErrorCount = parseErrorCount - recoverableParseErrorCount;
            if (nonRecoverableParseErrorCount > 0) {
                notes.add(nonRecoverableParseErrorCount + " file(s) had non-recoverable parse errors");
            }
        }
        if (parseBackendUnavailableCount > 0) {
            notes.add(parseBackendUnavailableCount + " file(s) could not be parsed because the backend was unavailable");
        }
        if (parseUnsupportedCount > 0) {
            notes.add(parseUnsupportedCount + " file(s) used unsupported languages");
        }
        if (ignoredFileCount > 0) {
            notes.add(ignoredFileCount + " file(s) were intentionally omitted by ignore rules");
        }
        if (fatalCount > 0) {
            notes.add("Fatal diagnostics were emitted");
        }

        CompletenessMetadata completeness = new CompletenessMetadata(
            status,
            indexedFileCount,
            totalFileCount,
            degradedFileCount,
            omittedPaths,
            List.copyOf(notes)
        );

        Map<String, Object> parseCounts = new LinkedHashMap<>();
        parseCounts.put("success", parseSuccessCount);
        parseCounts.put("skipped", parseSkippedCount);
        parseCounts.put("backendUnavailable", parseBackendUnavailableCount);
        parseCounts.put("unsupportedLanguage", parseUnsupportedCount);
        parseCounts.put("parseError", parseErrorCount);
        parseCounts.put("recoverableParseError", recoverableParseErrorCount);
        parseCounts.put("nonRecoverableParseError", parseErrorCount - recoverableParseErrorCount);

        Map<String, Object> diagnosticSummary = new LinkedHashMap<>();
        diagnosticSummary.put("totalDiagnostics", safeDiagnostics.size());
        diagnosticSummary.put("fatalCount", fatalCount);
        diagnosticSummary.put("bySeverity", Map.copyOf(bySeverity));
        diagnosticSummary.put("byPhase", Map.copyOf(byPhase));
        diagnosticSummary.put("parseCounts", Map.copyOf(parseCounts));

        Map<String, Object> partialResult = new LinkedHashMap<>();
        partialResult.put("partial", outcome != RunOutcome.SUCCESS);
        partialResult.put("degradedPaths", List.copyOf(degradedPaths));
        partialResult.put("omittedPaths", omittedPaths);
        partialResult.put("fatalCount", fatalCount);
        partialResult.put("degradedFileCount", degradedFileCount);
        partialResult.put("ignoredFileCount", ignoredFileCount);

        return new RunAssessment(
            outcome,
            completeness,
            Map.copyOf(diagnosticSummary),
            Map.copyOf(partialResult),
            List.copyOf(degradedPaths)
        );
    }

    RunOutcome outcome() {
        return outcome;
    }

    CompletenessMetadata completeness() {
        return completeness;
    }

    Map<String, Object> diagnosticSummary() {
        return diagnosticSummary;
    }

    Map<String, Object> partialResult() {
        return partialResult;
    }

    List<String> degradedPaths() {
        return degradedPaths;
    }
}
