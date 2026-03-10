package info.isaksson.erland.architecturebrowser.indexer.parse;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParseDiagnosticsTest {
    @Test
    void convertsParseIssuesIntoIrDiagnostics() {
        SourceParseRequest request = new SourceParseRequest(Path.of("src/Demo.java"), "src/Demo.java", ParseLanguage.JAVA, "class Demo {}\n");
        SourceParseResult result = new SourceParseResult(
            request,
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(new ParseIssue("parse.backend.unavailable", "Runtime missing", null, null, false)),
            Map.of("runtimeAvailable", false)
        );

        Diagnostic diagnostic = ParseDiagnostics.toDiagnostics(new ParseBatchResult(
            List.of(result),
            Map.of(ParseLanguage.JAVA, 1),
            Map.of(ParseStatus.BACKEND_UNAVAILABLE, 1)
        )).get(0);

        assertEquals(DiagnosticSeverity.WARNING, diagnostic.severity());
        assertEquals("parse.backend.unavailable", diagnostic.code());
        assertEquals("src/Demo.java", diagnostic.filePath());
        assertEquals("java", diagnostic.sourceRefs().get(0).metadata().get("language"));
    }
}
