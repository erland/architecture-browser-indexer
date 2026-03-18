package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Stable exported relationship contract.
 *
 * Step 1 baseline note: normalized architecture-facing relationship semantics are planned to
 * live directly on this record, alongside the existing generic relationship kind, once the
 * contract change is approved.
 */
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
