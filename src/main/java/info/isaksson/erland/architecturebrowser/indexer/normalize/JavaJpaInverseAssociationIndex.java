package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class JavaJpaInverseAssociationIndex {
    private final Map<String, InverseRelationshipMergeInput> byRelationshipId;
    private final Map<String, List<InverseRelationshipMergeInput>> byDirectedPair;

    private JavaJpaInverseAssociationIndex(
        Map<String, InverseRelationshipMergeInput> byRelationshipId,
        Map<String, List<InverseRelationshipMergeInput>> byDirectedPair
    ) {
        this.byRelationshipId = byRelationshipId;
        this.byDirectedPair = byDirectedPair;
    }

    static JavaJpaInverseAssociationIndex build(List<ArchitectureRelationship> relationships) {
        Map<String, InverseRelationshipMergeInput> byRelationshipId = new LinkedHashMap<>();
        Map<String, List<InverseRelationshipMergeInput>> byDirectedPair = new LinkedHashMap<>();
        if (relationships == null || relationships.isEmpty()) {
            return new JavaJpaInverseAssociationIndex(Map.of(), Map.of());
        }
        for (ArchitectureRelationship relationship : relationships) {
            if (relationship == null || relationship.id() == null) {
                continue;
            }
            InverseRelationshipMergeInput input = JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(relationship);
            byRelationshipId.put(relationship.id(), input);
            if (!isJpaAssociation(input)) {
                continue;
            }
            byDirectedPair.computeIfAbsent(pairKey(input.fromEntityId(), input.toEntityId()), ignored -> new ArrayList<>())
                .add(input);
        }
        return new JavaJpaInverseAssociationIndex(Map.copyOf(byRelationshipId), freezeLists(byDirectedPair));
    }

    InverseRelationshipMergeInput inputFor(ArchitectureRelationship relationship) {
        if (relationship == null || relationship.id() == null) {
            return null;
        }
        return byRelationshipId.get(relationship.id());
    }

    List<InverseRelationshipMergeInput> inverseCandidatesFor(InverseRelationshipMergeInput relationshipInput) {
        if (relationshipInput == null) {
            return List.of();
        }
        return byDirectedPair.getOrDefault(pairKey(relationshipInput.toEntityId(), relationshipInput.fromEntityId()), List.of());
    }

    boolean hasSwappedJpaAssociation(ArchitectureRelationship relationship) {
        if (relationship == null || relationship.id() == null) {
            return false;
        }
        for (InverseRelationshipMergeInput candidate : byDirectedPair.getOrDefault(pairKey(relationship.toEntityId(), relationship.fromEntityId()), List.of())) {
            if (candidate == null || Objects.equals(candidate.id(), relationship.id())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static boolean isJpaAssociation(InverseRelationshipMergeInput input) {
        return input != null
            && "jpa".equals(input.framework())
            && "hasassociation".equals(input.relationshipType())
            && input.associationCardinality() != null;
    }

    private static String pairKey(String fromEntityId, String toEntityId) {
        return Objects.toString(fromEntityId, "") + "\u0000" + Objects.toString(toEntityId, "");
    }

    private static Map<String, List<InverseRelationshipMergeInput>> freezeLists(Map<String, List<InverseRelationshipMergeInput>> source) {
        Map<String, List<InverseRelationshipMergeInput>> frozen = new LinkedHashMap<>();
        for (Map.Entry<String, List<InverseRelationshipMergeInput>> entry : source.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(frozen);
    }
}
