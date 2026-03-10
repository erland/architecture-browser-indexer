package info.isaksson.erland.architecturebrowser.indexer.parse;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.Diagnostic;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticPhase;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.DiagnosticSeverity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ParseDiagnostics {
    private ParseDiagnostics() {
    }

    public static List<Diagnostic> toDiagnostics(ParseBatchResult batchResult) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (SourceParseResult result : batchResult.results()) {
            for (int i = 0; i < result.issues().size(); i++) {
                ParseIssue issue = result.issues().get(i);
                diagnostics.add(toDiagnostic(result, issue, i));
            }
        }
        return diagnostics;
    }

    private static Diagnostic toDiagnostic(SourceParseResult result, ParseIssue issue, int issueIndex) {
        DiagnosticSeverity severity = switch (result.status()) {
            case SUCCESS, SKIPPED -> DiagnosticSeverity.INFO;
            case UNSUPPORTED_LANGUAGE, BACKEND_UNAVAILABLE -> DiagnosticSeverity.WARNING;
            case PARSE_ERROR -> DiagnosticSeverity.ERROR;
        };

        Map<String, Object> metadata = new LinkedHashMap<>(result.metadata());
        metadata.put("parseStatus", result.status().name());
        metadata.put("parseLanguage", result.request().language().inventoryKey());
        metadata.put("parserBackend", result.syntaxTree() == null ? null : result.syntaxTree().parserBackend());

        SourceReference sourceRef = new SourceReference(
            result.request().relativePath(),
            issue.startLine(),
            issue.endLine(),
            null,
            Map.of("language", result.request().language().inventoryKey())
        );

        return new Diagnostic(
            "diag:parse:" + sanitize(result.request().relativePath()) + ":" + issueIndex,
            severity,
            DiagnosticPhase.EXTRACTION,
            issue.code(),
            issue.message(),
            issue.fatal(),
            result.request().relativePath(),
            null,
            null,
            List.of(sourceRef),
            metadata
        );
    }

    private static String sanitize(String input) {
        return input.replace('\\', ':').replace('/', ':').replace(' ', '_');
    }
}
