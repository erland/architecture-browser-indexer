package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InverseRelationshipCanonicalSideSelectionPolicyTest {

    @Test
    void prefersOwningSideWhenCardinalityDoesNotAlreadyDetermineCanonicalBase() {
        InverseRelationshipMergeInput owning = mergeInput(
            relationship("rel:owner", "entity:group", "entity:user"),
            InverseRelationshipSideRole.OWNING,
            "many-to-many"
        );
        InverseRelationshipMergeInput inverse = mergeInput(
            relationship("rel:inverse", "entity:user", "entity:group"),
            InverseRelationshipSideRole.INVERSE,
            "many-to-many"
        );

        InverseRelationshipMergeInput canonical = JavaJpaNormalizedAssociationAssembler.chooseCanonicalBase(owning, inverse);

        assertEquals("rel:owner", canonical.id());
    }

    @Test
    void prefersExplicitOwningSideOverUnspecifiedSideWhenRolesAreAsymmetric() {
        InverseRelationshipMergeInput owning = mergeInput(
            relationship("rel:owner", "entity:order", "entity:tag"),
            InverseRelationshipSideRole.OWNING,
            "many-to-many"
        );
        InverseRelationshipMergeInput unspecified = mergeInput(
            relationship("rel:other", "entity:tag", "entity:order"),
            InverseRelationshipSideRole.UNSPECIFIED,
            "many-to-many"
        );

        InverseRelationshipMergeInput canonical = JavaJpaNormalizedAssociationAssembler.chooseCanonicalBase(owning, unspecified);

        assertEquals("rel:owner", canonical.id());
    }

    private static InverseRelationshipMergeInput mergeInput(
        ArchitectureRelationship relationship,
        InverseRelationshipSideRole sideRole,
        String cardinality
    ) {
        return new InverseRelationshipMergeInput(
            relationship,
            "framework-x",
            "hasassociation",
            cardinality,
            "association",
            sideRole,
            null,
            relationship.metadata() == null ? null : (String) relationship.metadata().get("ownerPropertyName"),
            RelationshipMultiplicityBoundsSupport.boundsForRelationship(relationship)
        );
    }

    private static ArchitectureRelationship relationship(String id, String from, String to) {
        return new ArchitectureRelationship(
            id,
            RelationshipKind.USES,
            from,
            to,
            null,
            List.of(),
            Map.of("ownerPropertyName", id + "Property"),
            null,
            null
        );
    }
}
