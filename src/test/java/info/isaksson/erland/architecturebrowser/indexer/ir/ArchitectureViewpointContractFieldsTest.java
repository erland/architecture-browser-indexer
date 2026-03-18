package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureViewpointContractFieldsTest {

    @Test
    void viewpointOptionalCollectionsAreCanonicalizedForDeterministicOutput() {
        ArchitectureViewpoint viewpoint = new ArchitectureViewpoint(
            " api-surface ",
            "API surface",
            " Externally exposed request entry points. ",
            " available ",
            0.95,
            List.of("entity:resource", " entity:resource "),
            List.of("api-entrypoint", " api-entrypoint "),
            List.of("serves-request", " serves-request "),
            List.of("java:type-dependencies", " java:type-dependencies "),
            List.of("java-normalization", " java-normalization ")
        );

        assertEquals("api-surface", viewpoint.id());
        assertEquals("API surface", viewpoint.title());
        assertEquals("Externally exposed request entry points.", viewpoint.description());
        assertEquals("available", viewpoint.availability());
        assertEquals(List.of("entity:resource"), viewpoint.seedEntityIds());
        assertEquals(List.of("api-entrypoint"), viewpoint.seedRoleIds());
        assertEquals(List.of("serves-request"), viewpoint.expandViaSemantics());
        assertEquals(List.of("java:type-dependencies"), viewpoint.preferredDependencyViews());
        assertEquals(List.of("java-normalization"), viewpoint.evidenceSources());
    }

    @Test
    void legacyDocumentConstructorKeepsViewpointsOptional() {
        ArchitectureIndexDocument document = new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            "0.1.0-SNAPSHOT",
            TestArchitectureDocuments.runMetadata(),
            TestArchitectureDocuments.repositorySource(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            TestArchitectureDocuments.completeness(),
            Map.of()
        );

        assertNull(document.viewpoints());
    }

    @Test
    void validatorAcceptsAbsentOrEmptyViewpointsAndValidReferencedSemantics() {
        ArchitectureEntity resource = new ArchitectureEntity(
            "entity:resource",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderResource",
            "OrderResource",
            "scope:repo",
            List.of(),
            Map.of(),
            List.of("api-entrypoint"),
            null
        );
        ArchitectureEntity service = new ArchitectureEntity(
            "entity:service",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of(),
            List.of("application-service"),
            null
        );
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:resource:service",
            RelationshipKind.CALLS,
            resource.id(),
            service.id(),
            "calls",
            List.of(),
            Map.of(),
            List.of("serves-request")
        );
        ArchitectureViewpoint viewpoint = new ArchitectureViewpoint(
            "api-surface",
            "API surface",
            "Shows API entrypoints and first-hop request handling edges.",
            "available",
            0.92,
            List.of(resource.id()),
            List.of("api-entrypoint"),
            List.of("serves-request", " serves-request "),
            List.of("java:type-dependencies"),
            List.of("java-normalization")
        );

        var result = ArchitectureIrValidator.validate(new ArchitectureIndexDocument(
            ArchitectureIrVersions.CURRENT_SCHEMA_VERSION,
            "0.1.0-SNAPSHOT",
            TestArchitectureDocuments.runMetadata(),
            TestArchitectureDocuments.repositorySource(),
            List.of(),
            List.of(resource, service),
            List.of(relationship),
            List.of(viewpoint),
            List.of(),
            TestArchitectureDocuments.completeness(),
            Map.of()
        ));

        assertTrue(result.isValid(), () -> "canonical constructor should strip duplicate viewpoint values before validation: " + result.messages());
    }
}
