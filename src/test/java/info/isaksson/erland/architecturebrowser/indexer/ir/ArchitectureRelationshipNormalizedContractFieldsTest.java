package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.NormalizedAssociation;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureRelationshipNormalizedContractFieldsTest {

    @Test
    void architecturalSemanticsAreCanonicalizedForDeterministicOutput() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:order-resource:service",
            RelationshipKind.CALLS,
            "entity:resource",
            "entity:service",
            "calls",
            List.of(),
            Map.of(),
            List.of("invokes-use-case", " invokes-use-case ", "serves-request")
        );

        assertEquals(List.of("invokes-use-case", "serves-request"), relationship.architecturalSemantics());
    }

    @Test
    void legacyConstructorKeepsRelationshipSemanticsOptional() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:order-resource:service",
            RelationshipKind.CALLS,
            "entity:resource",
            "entity:service",
            "calls",
            List.of(),
            Map.of()
        );

        assertNull(relationship.architecturalSemantics());
    }

    @Test
    void validatorAcceptsCanonicalizedRelationshipSemanticsWhenPresent() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:order-resource:service",
            RelationshipKind.CALLS,
            "entity:resource",
            "entity:service",
            "calls",
            List.of(),
            Map.of(),
            List.of("invokes-use-case", "invokes-use-case", " ")
        );

        var result = ArchitectureIrValidator.validate(new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            "0.1.0-SNAPSHOT",
            TestArchitectureDocuments.runMetadata(),
            TestArchitectureDocuments.repositorySource(),
            List.of(),
            List.of(
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity(
                    "entity:resource",
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind.CLASS,
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED,
                    "OrderResource",
                    "OrderResource",
                    "scope:repo",
                    List.of(),
                    Map.of()
                ),
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity(
                    "entity:service",
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind.CLASS,
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED,
                    "OrderService",
                    "OrderService",
                    "scope:repo",
                    List.of(),
                    Map.of()
                )
            ),
            List.of(relationship),
            List.of(),
            TestArchitectureDocuments.completeness(),
            Map.of()
        ));

        assertTrue(result.isValid(), () -> "canonical constructor should strip blank/duplicate normalized relationship semantics before validation: " + result.messages());
    }


    @Test
    void normalizedAssociationEvidenceIdsAreCanonicalizedForDeterministicOutput() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:project:tasks",
            RelationshipKind.USES,
            "entity:project",
            "entity:task",
            "contains tasks",
            List.of(),
            Map.of(),
            null,
            new NormalizedAssociation(
                "containment",
                "one-to-many",
                "1",
                "1",
                "0",
                "*",
                true,
                List.of(" rel:task:project ", "rel:project:tasks", "rel:task:project"),
                "entity:project",
                "field:project:tasks",
                "entity:task",
                "field:task:project"
            )
        );

        assertNotNull(relationship.normalizedAssociation());
        assertEquals(List.of("rel:project:tasks", "rel:task:project"), relationship.normalizedAssociation().evidenceRelationshipIds());
    }

    @Test
    void validatorAcceptsNormalizedAssociationWhenOptionalContractExtensionIsPresent() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:project:tasks",
            RelationshipKind.USES,
            "entity:project",
            "entity:task",
            "contains tasks",
            List.of(),
            Map.of(),
            null,
            new NormalizedAssociation(
                "containment",
                "one-to-many",
                "1",
                "1",
                "0",
                "*",
                true,
                List.of("rel:project:tasks", "rel:task:project"),
                "entity:project",
                "field:project:tasks",
                "entity:task",
                "field:task:project"
            )
        );

        var result = ArchitectureIrValidator.validate(new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            "0.1.0-SNAPSHOT",
            TestArchitectureDocuments.runMetadata(),
            TestArchitectureDocuments.repositorySource(),
            List.of(),
            List.of(
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity(
                    "entity:project",
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind.CLASS,
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED,
                    "Project",
                    "Project",
                    "scope:repo",
                    List.of(),
                    Map.of()
                ),
                new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity(
                    "entity:task",
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind.CLASS,
                    info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin.OBSERVED,
                    "Task",
                    "Task",
                    "scope:repo",
                    List.of(),
                    Map.of()
                )
            ),
            List.of(relationship),
            List.of(),
            TestArchitectureDocuments.completeness(),
            Map.of()
        ));

        assertTrue(result.isValid(), () -> "optional normalizedAssociation object should validate when present: " + result.messages());
    }

}
