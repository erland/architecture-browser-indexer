package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
