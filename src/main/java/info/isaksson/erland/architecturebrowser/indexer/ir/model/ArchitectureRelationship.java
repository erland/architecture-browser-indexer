package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitectureRelationship(
    String id,
    RelationshipKind kind,
    String fromEntityId,
    String toEntityId,
    String label,
    List<SourceReference> sourceRefs,
    Map<String, Object> metadata
) {
}
