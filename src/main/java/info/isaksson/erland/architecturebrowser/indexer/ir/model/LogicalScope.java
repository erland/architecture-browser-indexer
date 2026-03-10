package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LogicalScope(
    String id,
    ScopeKind kind,
    String name,
    String displayName,
    String parentScopeId,
    List<SourceReference> sourceRefs,
    Map<String, Object> metadata
) {
}
