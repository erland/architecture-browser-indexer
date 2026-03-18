package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArchitectureIrViewpointDerivationServiceTest {

    @Test
    void derivesCoreViewpointsFromNormalizedRolesAndSemantics() {
        ArchitectureEntity resource = new ArchitectureEntity(
            "entity:resource",
            EntityKind.SERVICE,
            EntityOrigin.INFERRED,
            "com.example.orders.api.OrderResource",
            "OrderResource",
            "scope:repo",
            List.of(),
            Map.of("backendProfile", "jax-rs-resource", "frameworks", List.of("jax-rs")),
            List.of("api-entrypoint"),
            List.of("externally-exposed")
        );
        ArchitectureEntity service = new ArchitectureEntity(
            "entity:service",
            EntityKind.SERVICE,
            EntityOrigin.INFERRED,
            "com.example.orders.service.OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of("backendProfile", "application-service"),
            List.of("application-service"),
            null
        );
        ArchitectureEntity repository = new ArchitectureEntity(
            "entity:repo",
            EntityKind.PERSISTENCE_ADAPTER,
            EntityOrigin.INFERRED,
            "com.example.orders.repo.OrderRepository",
            "OrderRepository",
            "scope:repo",
            List.of(),
            Map.of("entityRole", "repository", "frameworks", List.of("jpa")),
            List.of("persistence-access"),
            null
        );
        ArchitectureEntity orderEntity = new ArchitectureEntity(
            "entity:order",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "com.example.orders.domain.OrderEntity",
            "OrderEntity",
            "scope:repo",
            List.of(),
            Map.of("annotations", List.of("@Entity"), "frameworks", List.of("jpa")),
            List.of("persistent-entity"),
            List.of("persistent")
        );

        List<ArchitectureRelationship> relationships = List.of(
            new ArchitectureRelationship(
                "rel:resource:service",
                RelationshipKind.CALLS,
                resource.id(),
                service.id(),
                "calls",
                List.of(),
                Map.of("frameworks", List.of("jax-rs")),
                List.of("serves-request", "invokes-use-case")
            ),
            new ArchitectureRelationship(
                "rel:service:repo",
                RelationshipKind.DEPENDS_ON,
                service.id(),
                repository.id(),
                "depends-on",
                List.of(),
                Map.of(),
                List.of("accesses-persistence")
            )
        );

        List<ArchitectureViewpoint> viewpoints = ArchitectureIrViewpointDerivationService.derive(
            List.of(resource, service, repository, orderEntity),
            relationships,
            Map.of("moduleDependencies", List.of(Map.of("from", "module:a", "to", "module:b")))
        );

        ArchitectureViewpoint apiSurface = viewpointById(viewpoints, "api-surface");
        assertEquals("available", apiSurface.availability());
        assertEquals(List.of("entity:resource"), apiSurface.seedEntityIds());
        assertEquals(List.of("api-entrypoint"), apiSurface.seedRoleIds());
        assertEquals(List.of("serves-request"), apiSurface.expandViaSemantics());
        assertNotNull(apiSurface.evidenceSources());

        ArchitectureViewpoint requestHandling = viewpointById(viewpoints, "request-handling");
        assertEquals("available", requestHandling.availability());
        assertEquals(List.of("accesses-persistence", "invokes-use-case", "serves-request"), requestHandling.expandViaSemantics());
        assertEquals(List.of("api-entrypoint", "application-service"), requestHandling.seedRoleIds());

        ArchitectureViewpoint persistenceModel = viewpointById(viewpoints, "persistence-model");
        assertEquals("available", persistenceModel.availability());
        assertEquals(List.of("entity:order", "entity:repo"), persistenceModel.seedEntityIds());
        assertEquals(List.of("persistent-entity", "persistence-access"), persistenceModel.seedRoleIds());
        assertEquals(List.of("accesses-persistence"), persistenceModel.expandViaSemantics());

        ArchitectureViewpoint moduleDependencies = viewpointById(viewpoints, "module-dependencies");
        assertEquals("available", moduleDependencies.availability());
        assertEquals(List.of("dependency-views"), moduleDependencies.evidenceSources());
    }

    @Test
    void degradesToUnavailableOrPartialWhenEvidenceIsMissing() {
        ArchitectureEntity module = new ArchitectureEntity(
            "entity:module:a",
            EntityKind.MODULE,
            EntityOrigin.INFERRED,
            "module-a",
            "module-a",
            "scope:repo",
            List.of(),
            Map.of(),
            null,
            null
        );
        ArchitectureEntity external = new ArchitectureEntity(
            "entity:external:erp",
            EntityKind.EXTERNAL_SYSTEM,
            EntityOrigin.INFERRED,
            "ERP",
            "ERP",
            "scope:repo",
            List.of(),
            Map.of(),
            null,
            null
        );

        List<ArchitectureViewpoint> viewpoints = ArchitectureIrViewpointDerivationService.derive(
            List.of(module, external),
            List.of(),
            Map.of()
        );

        assertEquals("unavailable", viewpointById(viewpoints, "api-surface").availability());
        assertEquals("unavailable", viewpointById(viewpoints, "request-handling").availability());
        assertEquals("unavailable", viewpointById(viewpoints, "persistence-model").availability());
        assertEquals("partial", viewpointById(viewpoints, "integration-map").availability());
        assertEquals("unavailable", viewpointById(viewpoints, "module-dependencies").availability());
    }

    private static ArchitectureViewpoint viewpointById(List<ArchitectureViewpoint> viewpoints, String id) {
        return viewpoints.stream()
            .filter(viewpoint -> id.equals(viewpoint.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing viewpoint " + id));
    }
}
