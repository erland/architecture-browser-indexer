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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        ArchitectureEntity endpoint = new ArchitectureEntity(
            "entity:endpoint:get-orders",
            EntityKind.ENDPOINT,
            EntityOrigin.INFERRED,
            "GET /orders",
            "GET /orders",
            "scope:repo",
            List.of(),
            Map.of("sourceLanguage", "java", "relativePath", "src/main/java/com/example/orders/api/OrderResource.java"),
            List.of("api-entrypoint"),
            List.of("externally-exposed")
        );
        ArchitectureEntity testEndpoint = new ArchitectureEntity(
            "entity:endpoint:test-orders",
            EntityKind.ENDPOINT,
            EntityOrigin.INFERRED,
            "GET /test/orders",
            "GET /test/orders",
            "scope:repo",
            List.of(),
            Map.of("sourceLanguage", "java", "relativePath", "src/test/java/com/example/orders/api/OrderResourceTest.java"),
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
            List.of(resource, endpoint, testEndpoint, service, repository, orderEntity),
            relationships,
            Map.of(
                "moduleDependencies", List.of(Map.of("from", "module:a", "to", "module:b")),
                "endpointTypeDependencies", List.of(Map.of("from", resource.id(), "to", service.id())),
                "endpointModuleDependencies", List.of(Map.of("from", "module:api", "to", "module:service")),
                "entityModelTypeDependencies", List.of(Map.of("from", orderEntity.id(), "to", "entity:line")),
                "entityModelModuleDependencies", List.of(),
                "observerTypeDependencies", List.of(Map.of("from", "entity:publisher", "to", "entity:event")),
                "observerModuleDependencies", List.of(Map.of("from", "module:events", "to", "module:listeners")),
                "writePathTypeDependencies", List.of(Map.of("from", service.id(), "to", repository.id())),
                "writePathModuleDependencies", List.of(Map.of("from", "module:service", "to", "module:persistence")),
                "javaBrowserViews", Map.of(
                    "views", List.of(
                        Map.of(
                            "id", "javaEndpointGraph",
                            "available", true,
                            "preferredDependencyView", "endpointTypeDependencies",
                            "typeDependencyView", "endpointTypeDependencies",
                            "moduleDependencyView", "endpointModuleDependencies",
                            "typeDependencyCount", 1,
                            "moduleDependencyCount", 1
                        ),
                        Map.of(
                            "id", "javaEntityModelGraph",
                            "available", true,
                            "preferredDependencyView", "entityModelTypeDependencies",
                            "typeDependencyView", "entityModelTypeDependencies",
                            "moduleDependencyView", "entityModelModuleDependencies",
                            "typeDependencyCount", 1,
                            "moduleDependencyCount", 0
                        ),
                        Map.of(
                            "id", "javaEventFlowGraph",
                            "available", true,
                            "preferredDependencyView", "observerTypeDependencies",
                            "typeDependencyView", "observerTypeDependencies",
                            "moduleDependencyView", "observerModuleDependencies",
                            "typeDependencyCount", 1,
                            "moduleDependencyCount", 1
                        ),
                        Map.of(
                            "id", "javaWritePathGraph",
                            "available", true,
                            "preferredDependencyView", "writePathTypeDependencies",
                            "typeDependencyView", "writePathTypeDependencies",
                            "moduleDependencyView", "writePathModuleDependencies",
                            "typeDependencyCount", 1,
                            "moduleDependencyCount", 1
                        )
                    )
                )
            )
        );

        ArchitectureViewpoint apiSurface = viewpointById(viewpoints, "api-surface");
        assertEquals("available", apiSurface.availability());
        assertEquals(List.of("entity:endpoint:get-orders"), apiSurface.seedEntityIds());
        assertEquals(List.of("api-entrypoint"), apiSurface.seedRoleIds());
        assertEquals(List.of("serves-request"), apiSurface.expandViaSemantics());
        assertEquals(List.of("endpointModuleDependencies", "endpointTypeDependencies"), apiSurface.preferredDependencyViews());
        assertNotNull(apiSurface.evidenceSources());
        assertTrue(apiSurface.evidenceSources().contains("java-browser-views"));

        ArchitectureViewpoint requestHandling = viewpointById(viewpoints, "request-handling");
        assertEquals("available", requestHandling.availability());
        assertEquals(List.of("accesses-persistence", "invokes-use-case", "serves-request"), requestHandling.expandViaSemantics());
        assertEquals(List.of("api-entrypoint", "application-service"), requestHandling.seedRoleIds());
        assertEquals(List.of("writePathModuleDependencies", "writePathTypeDependencies"), requestHandling.preferredDependencyViews());

        ArchitectureViewpoint persistenceModel = viewpointById(viewpoints, "persistence-model");
        assertEquals("available", persistenceModel.availability());
        assertEquals(List.of("entity:order", "entity:repo"), persistenceModel.seedEntityIds());
        assertEquals(List.of("persistence-access", "persistent-entity"), persistenceModel.seedRoleIds());
        assertEquals(List.of("accesses-persistence"), persistenceModel.expandViaSemantics());
        assertEquals(List.of("entityModelModuleDependencies", "entityModelTypeDependencies"), persistenceModel.preferredDependencyViews());

        ArchitectureViewpoint eventFlow = viewpointById(viewpoints, "event-flow");
        assertEquals("available", eventFlow.availability());
        assertEquals(List.of("observerModuleDependencies", "observerTypeDependencies"), eventFlow.preferredDependencyViews());

        ArchitectureViewpoint moduleDependencies = viewpointById(viewpoints, "module-dependencies");
        assertEquals("available", moduleDependencies.availability());
        assertEquals(List.of("dependency-views"), moduleDependencies.evidenceSources());
    }


    @Test
    void derivesUiNavigationViewpointFromCanonicalUiRolesAndSemantics() {
        ArchitectureEntity shell = new ArchitectureEntity(
            "entity:ui:shell",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "src/app/AppShell.tsx",
            "AppShell",
            "scope:repo",
            List.of(),
            Map.of("framework", "react", "routePath", "/"),
            List.of("ui-layout"),
            List.of("route-declared", "user-facing")
        );
        ArchitectureEntity home = new ArchitectureEntity(
            "entity:ui:home",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "src/app/HomePage.tsx",
            "HomePage",
            "scope:repo",
            List.of(),
            Map.of("framework", "react", "routePath", "/home"),
            List.of("ui-page"),
            List.of("route-declared", "user-facing")
        );
        ArchitectureEntity reports = new ArchitectureEntity(
            "entity:ui:reports",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "src/app/ReportsPage.tsx",
            "ReportsPage",
            "scope:repo",
            List.of(),
            Map.of("framework", "react", "routePath", "/reports"),
            List.of("ui-page"),
            List.of("route-declared", "user-facing")
        );
        ArchitectureEntity sidebar = new ArchitectureEntity(
            "entity:ui:sidebar",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "src/app/SidebarNav.tsx",
            "SidebarNav",
            "scope:repo",
            List.of(),
            Map.of("framework", "react", "navigationTargetLiteral", "/reports"),
            List.of("ui-navigation-node"),
            List.of("user-facing")
        );

        List<ArchitectureRelationship> relationships = List.of(
            new ArchitectureRelationship(
                "rel:ui:shell:home",
                RelationshipKind.CONTAINS,
                shell.id(),
                home.id(),
                "contains",
                List.of(),
                Map.of("framework", "react", "parentRoutePath", "/"),
                List.of("contains-route")
            ),
            new ArchitectureRelationship(
                "rel:ui:sidebar:reports",
                RelationshipKind.USES,
                sidebar.id(),
                reports.id(),
                "navigates",
                List.of(),
                Map.of("framework", "react", "navigationTargetLiteral", "/reports"),
                List.of("navigates-to")
            ),
            new ArchitectureRelationship(
                "rel:ui:home:reports",
                RelationshipKind.USES,
                home.id(),
                reports.id(),
                "redirects",
                List.of(),
                Map.of("framework", "react", "redirectTargetLiteral", "/reports"),
                List.of("redirects-to")
            )
        );

        List<ArchitectureViewpoint> viewpoints = ArchitectureIrViewpointDerivationService.derive(
            List.of(shell, home, reports, sidebar),
            relationships,
            Map.of()
        );

        ArchitectureViewpoint uiNavigation = viewpointById(viewpoints, "ui-navigation");
        assertEquals("available", uiNavigation.availability());
        assertEquals(List.of("entity:ui:home", "entity:ui:reports", "entity:ui:shell", "entity:ui:sidebar"), uiNavigation.seedEntityIds());
        assertEquals(List.of("ui-layout", "ui-navigation-node", "ui-page"), uiNavigation.seedRoleIds());
        assertEquals(List.of("contains-route", "navigates-to", "redirects-to"), uiNavigation.expandViaSemantics());
        assertEquals(List.of("frontend-routing", "normalized-roles", "normalized-semantics"), uiNavigation.evidenceSources());
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
        assertEquals("unavailable", viewpointById(viewpoints, "ui-navigation").availability());
    }

    private static ArchitectureViewpoint viewpointById(List<ArchitectureViewpoint> viewpoints, String id) {
        return viewpoints.stream()
            .filter(viewpoint -> id.equals(viewpoint.id()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing viewpoint " + id));
    }
}
