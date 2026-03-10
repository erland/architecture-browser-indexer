package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitectureEntity(
    String id,
    EntityKind kind,
    EntityOrigin origin,
    String name,
    String displayName,
    String scopeId,
    List<SourceReference> sourceRefs,
    Map<String, Object> metadata
) {
}
