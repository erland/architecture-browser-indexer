package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Diagnostic(
    String id,
    DiagnosticSeverity severity,
    DiagnosticPhase phase,
    String code,
    String message,
    boolean fatal,
    String filePath,
    String scopeId,
    String entityId,
    List<SourceReference> sourceRefs,
    Map<String, Object> metadata
) {
}
