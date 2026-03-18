package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Stable exported entity contract.
 *
 * Step 1 baseline note: normalized architecture-facing fields such as architectural roles,
 * traits, and optional evidence are planned to live directly on this record rather than being
 * hidden only inside metadata once the contract change is approved.
 */
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
