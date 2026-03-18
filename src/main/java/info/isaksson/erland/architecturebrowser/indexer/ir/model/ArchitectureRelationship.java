package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

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
    Map<String, Object> metadata,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> architecturalSemantics
) {
    public ArchitectureRelationship(
        String id,
        RelationshipKind kind,
        String fromEntityId,
        String toEntityId,
        String label,
        List<SourceReference> sourceRefs,
        Map<String, Object> metadata
    ) {
        this(id, kind, fromEntityId, toEntityId, label, sourceRefs, metadata, null);
    }

    public ArchitectureRelationship {
        sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        architecturalSemantics = canonicalizeArchitecturalStrings(architecturalSemantics);
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
