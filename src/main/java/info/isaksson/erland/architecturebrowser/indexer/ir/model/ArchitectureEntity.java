package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

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
    Map<String, Object> metadata,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> architecturalRoles,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> architecturalTraits
) {
    public ArchitectureEntity(
        String id,
        EntityKind kind,
        EntityOrigin origin,
        String name,
        String displayName,
        String scopeId,
        List<SourceReference> sourceRefs,
        Map<String, Object> metadata
    ) {
        this(id, kind, origin, name, displayName, scopeId, sourceRefs, metadata, null, null);
    }

    public ArchitectureEntity {
        sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        architecturalRoles = canonicalizeArchitecturalStrings(architecturalRoles);
        architecturalTraits = canonicalizeArchitecturalStrings(architecturalTraits);
    }

    private static List<String> canonicalizeArchitecturalStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .distinct()
            .sorted()
            .toList();
    }
}
