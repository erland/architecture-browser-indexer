package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaJpaInverseRelationshipMergeInputFactoryTest {

    @Test
    void projectsJpaRelationshipIntoFrameworkNeutralMergeInput() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:project-tasks",
            RelationshipKind.USES,
            "entity:project",
            "entity:task",
            null,
            List.of(),
            Map.of(
                "framework", "jpa",
                "relationshipType", "hasAssociation",
                "associationCardinality", "one-to-many",
                "mappedBy", "project",
                "ownerPropertyName", "tasks",
                "sourceLowerBound", "0",
                "sourceUpperBound", "1",
                "targetLowerBound", "0",
                "targetUpperBound", "*"
            ),
            null,
            null
        );

        InverseRelationshipMergeInput input = JavaJpaInverseRelationshipMergeInputFactory.fromRelationship(relationship);

        assertEquals("jpa", input.framework());
        assertEquals("hasassociation", input.relationshipType());
        assertEquals("one-to-many", input.associationCardinality());
        assertEquals(InverseRelationshipSideRole.INVERSE, input.sideRole());
        assertEquals("project", input.inverseSideReference());
        assertEquals("tasks", input.propertyName());
        assertEquals("0", input.bounds().sourceLowerBound());
        assertEquals("*", input.bounds().targetUpperBound());
    }
}
