package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Optional stable contract extension for one canonical normalized association.
 *
 * <p>This record defines the exported shape only. Population/derivation work is
 * intentionally deferred to later steps.</p>
 */
public record NormalizedAssociation(
    String associationKind,
    String associationCardinality,
    String sourceLowerBound,
    String sourceUpperBound,
    String targetLowerBound,
    String targetUpperBound,
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean bidirectional,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> evidenceRelationshipIds,
    @JsonInclude(JsonInclude.Include.NON_NULL) String owningSideEntityId,
    @JsonInclude(JsonInclude.Include.NON_NULL) String owningSideMemberId,
    @JsonInclude(JsonInclude.Include.NON_NULL) String inverseSideEntityId,
    @JsonInclude(JsonInclude.Include.NON_NULL) String inverseSideMemberId
) {
    public NormalizedAssociation {
        evidenceRelationshipIds = canonicalizeRelationshipIds(evidenceRelationshipIds);
    }

    private static List<String> canonicalizeRelationshipIds(List<String> values) {
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
