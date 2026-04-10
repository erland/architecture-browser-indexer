package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InverseRelationshipBoundMergePolicyTest {

    @Test
    void mergesOneToManyAndManyToOneIntoCanonicalOrientation() {
        ArchitectureRelationship oneToMany = relationship(
            "rel:project-tasks",
            "entity:project",
            "entity:task",
            Map.of(
                "associationCardinality", "one-to-many",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1",
                "targetLowerBound", "0",
                "targetUpperBound", "*"
            )
        );
        ArchitectureRelationship manyToOne = relationship(
            "rel:task-project",
            "entity:task",
            "entity:project",
            Map.of(
                "associationCardinality", "many-to-one",
                "sourceLowerBound", "0",
                "sourceUpperBound", "*",
                "targetLowerBound", "1",
                "targetUpperBound", "1"
            )
        );

        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds merged = InverseRelationshipBoundMergePolicy.mergeBounds(
            oneToMany,
            manyToOne,
            "one-to-many",
            "many-to-one"
        );

        assertEquals("1", merged.sourceLowerBound());
        assertEquals("1", merged.sourceUpperBound());
        assertEquals("0", merged.targetLowerBound());
        assertEquals("*", merged.targetUpperBound());
    }

    @Test
    void conservativelyCombinesBoundsByCanonicalEntityForSymmetricRelationships() {
        ArchitectureRelationship left = relationship(
            "rel:left",
            "entity:user",
            "entity:group",
            Map.of(
                "associationCardinality", "many-to-many",
                "sourceLowerBound", "1",
                "sourceUpperBound", "3",
                "targetLowerBound", "0",
                "targetUpperBound", "*"
            )
        );
        ArchitectureRelationship right = relationship(
            "rel:right",
            "entity:group",
            "entity:user",
            Map.of(
                "associationCardinality", "many-to-many",
                "sourceLowerBound", "0",
                "sourceUpperBound", "10",
                "targetLowerBound", "2",
                "targetUpperBound", "4"
            )
        );

        RelationshipMultiplicityBoundsSupport.RelationshipEndBounds merged = InverseRelationshipBoundMergePolicy.mergeBounds(
            left,
            right,
            "many-to-many",
            "many-to-many"
        );

        assertEquals("1", merged.sourceLowerBound());
        assertEquals("4", merged.sourceUpperBound());
        assertEquals("0", merged.targetLowerBound());
        assertEquals("*", merged.targetUpperBound());
    }

    private static ArchitectureRelationship relationship(String id, String from, String to, Map<String, Object> metadata) {
        return new ArchitectureRelationship(
            id,
            RelationshipKind.USES,
            from,
            to,
            null,
            List.of(),
            metadata,
            null,
            null
        );
    }
}
