package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.CompletenessStatus;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseIssue;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrFactoryPartialResultTest {

    @Test
    void marksRunAsPartialAndEmitsDiagnosticSummaryWhenSomeFilesAreDegraded() {
        FileInventory inventory = new FileInventory(
            List.of(
                new FileInventoryEntry("src/main/java/com/example/DemoController.java", 100, "java", "source", "java", false, List.of("spring")),
                new FileInventoryEntry("src/main/ts/app/main.ts", 80, "ts", "source", "typescript", false, List.of("react")),
                new FileInventoryEntry("node_modules/lib/index.js", 50, "js", "source", "javascript", true, List.of())
            ),
            2, 3, 1, Set.of("java", "typescript"), Set.of("spring", "react")
        );

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            List.of(
                new SourceParseResult(
                    new SourceParseRequest(Path.of("src/main/java/com/example/DemoController.java"), "src/main/java/com/example/DemoController.java", ParseLanguage.JAVA, "class DemoController {}"),
                    ParseStatus.SUCCESS,
                    null,
                    List.of(),
                    Map.of()
                ),
                new SourceParseResult(
                    new SourceParseRequest(Path.of("src/main/ts/app/main.ts"), "src/main/ts/app/main.ts", ParseLanguage.TYPESCRIPT, "export const x = ;"),
                    ParseStatus.PARSE_ERROR,
                    null,
                    List.of(new ParseIssue("parse.error", "Unexpected token", 1, 1, false)),
                    Map.of()
                )
            ),
            Map.of(ParseLanguage.JAVA, 1, ParseLanguage.TYPESCRIPT, 1),
            Map.of(ParseStatus.SUCCESS, 1, ParseStatus.PARSE_ERROR, 1)
        );

        List<Diagnostic> acquisitionDiagnostics = List.of(
            new Diagnostic(
                "diag:test:warning",
                DiagnosticSeverity.WARNING,
                DiagnosticPhase.ACQUISITION,
                "test.warning",
                "Non-fatal acquisition warning",
                false,
                "src/main/ts/app/main.ts",
                null,
                null,
                List.of(),
                Map.of()
            )
        );

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            acquisitionDiagnostics,
            parseBatchResult,
            new StructuralExtractionResult(List.of(), List.of(), List.of(), List.of(), new ExtractionSummary(0, 0, Map.of(), Map.of(), 0, 0))
        );

        assertEquals(CompletenessStatus.PARTIAL, document.completeness().status());
        assertEquals(1, document.completeness().degradedFileCount());
        assertEquals("PARTIAL", document.runMetadata().outcome().name());

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnosticSummary = (Map<String, Object>) document.metadata().get("diagnosticSummary");
        assertEquals(2, diagnosticSummary.get("totalDiagnostics"));

        @SuppressWarnings("unchecked")
        Map<String, Object> partialResult = (Map<String, Object>) document.metadata().get("partialResult");
        assertEquals(Boolean.TRUE, partialResult.get("partial"));
        assertTrue(String.valueOf(partialResult.get("degradedPaths")).contains("src/main/ts/app/main.ts"));
        assertTrue(document.completeness().notes().stream().anyMatch(note -> note.contains("parse errors")));
    }

    @Test
    void marksRunAsFailedWhenFatalDiagnosticsArePresent() {
        FileInventory inventory = new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of());
        List<Diagnostic> fatalDiagnostics = List.of(
            new Diagnostic(
                "diag:test:fatal",
                DiagnosticSeverity.ERROR,
                DiagnosticPhase.ACQUISITION,
                "test.fatal",
                "Fatal acquisition problem",
                true,
                null,
                null,
                null,
                List.of(),
                Map.of()
            )
        );

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            fatalDiagnostics
        );

        assertEquals("FAILED", document.runMetadata().outcome().name());
        assertEquals(CompletenessStatus.PARTIAL, document.completeness().status());

        @SuppressWarnings("unchecked")
        Map<String, Object> diagnosticSummary = (Map<String, Object>) document.metadata().get("diagnosticSummary");
        assertEquals(1, diagnosticSummary.get("fatalCount"));
    }
}
