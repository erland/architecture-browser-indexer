package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeScriptArchitectureRelationshipNormalizationRuleTest {

    private final ArchitectureRelationshipNormalizationService relationshipService = ArchitectureRelationshipNormalizationService.defaultService();

    @Test
    void mapsRouteContainmentToContainsRoute() {
        ArchitectureEntity parent = routeEntity("entity:react-route:/app", "AppLayout", "/app", Map.of(), List.of("ui-layout"));
        ArchitectureEntity child = routeEntity("entity:react-route:/app/orders", "OrdersPage", "/app/orders", Map.of(), List.of("ui-page"));
        ArchitectureRelationship relationship = relationship(
            "rel:orders:app",
            child.id(),
            parent.id(),
            Map.of(
                "framework", "react",
                "frameworkRelationship", "childOf",
                "routeSourceKind", "declared-route"
            )
        );

        ArchitectureRelationship normalized = relationshipService.normalizeRelationship(
            relationship,
            Map.of(parent.id(), parent, child.id(), child),
            Map.of(relationship.id(), relationship)
        );

        assertEquals(List.of("contains-route"), normalized.architecturalSemantics());
    }

    @Test
    void mapsRedirectRelationshipsToRedirectsTo() {
        ArchitectureEntity legacyRoute = routeEntity(
            "entity:react-route:/legacy",
            "LegacyRoute",
            "/legacy",
            Map.of("redirectTargetLiteral", "/orders"),
            List.of("ui-page")
        );
        ArchitectureEntity ordersRoute = routeEntity("entity:react-route:/orders", "OrdersPage", "/orders", Map.of(), List.of("ui-page"));
        ArchitectureRelationship relationship = relationship(
            "rel:legacy:orders",
            legacyRoute.id(),
            ordersRoute.id(),
            Map.of(
                "framework", "react",
                "frameworkRelationship", "redirects",
                "routeSourceKind", "redirect",
                "redirectTargetLiteral", "/orders"
            )
        );

        ArchitectureRelationship normalized = relationshipService.normalizeRelationship(
            relationship,
            Map.of(legacyRoute.id(), legacyRoute, ordersRoute.id(), ordersRoute),
            Map.of(relationship.id(), relationship)
        );

        assertEquals(List.of("redirects-to"), normalized.architecturalSemantics());
    }

    @Test
    void mapsStaticAndImperativeNavigationToNavigatesTo() {
        ArchitectureEntity dashboardPage = new ArchitectureEntity(
            "entity:page:dashboard",
            EntityKind.UI_MODULE,
            EntityOrigin.OBSERVED,
            "DashboardPage",
            "DashboardPage",
            "scope:repo",
            List.of(),
            Map.of(
                "language", "typescript",
                "path", "src/pages/DashboardPage.tsx",
                "sourceSnippet", "return <Link to='/reports'>Reports</Link>;"
            ),
            List.of("ui-page"),
            List.of("user-facing")
        );
        ArchitectureEntity reportsRoute = routeEntity("entity:react-route:/reports", "ReportsPage", "/reports", Map.of(), List.of("ui-page"));

        ArchitectureRelationship staticLink = relationship(
            "rel:dashboard:reports:link",
            dashboardPage.id(),
            reportsRoute.id(),
            Map.of(
                "framework", "react",
                "frameworkRelationship", "linksToRoute",
                "routeSourceKind", "link",
                "navigationTargetLiteral", "/reports",
                "emittedRelationshipKind", "linksToRoute"
            )
        );
        ArchitectureRelationship imperative = relationship(
            "rel:dashboard:reports:navigate",
            dashboardPage.id(),
            reportsRoute.id(),
            Map.of(
                "framework", "react",
                "frameworkRelationship", "navigatesToRoute",
                "routeSourceKind", "navigate",
                "navigationTargetLiteral", "/reports",
                "emittedRelationshipKind", "navigatesToRoute"
            )
        );

        Map<String, ArchitectureEntity> entities = Map.of(dashboardPage.id(), dashboardPage, reportsRoute.id(), reportsRoute);

        assertEquals(
            List.of("navigates-to"),
            relationshipService.normalizeRelationship(staticLink, entities, Map.of(staticLink.id(), staticLink)).architecturalSemantics()
        );
        assertEquals(
            List.of("navigates-to"),
            relationshipService.normalizeRelationship(imperative, entities, Map.of(imperative.id(), imperative)).architecturalSemantics()
        );
    }

    @Test
    void mapsGuardRelationshipsToGuardsRouteAndPreservesExistingSemantics() {
        ArchitectureEntity ordersRoute = routeEntity(
            "entity:angular-route:/orders",
            "OrdersRoute",
            "/orders",
            Map.of("guardReference", "AuthGuard"),
            List.of("ui-page")
        );
        ArchitectureEntity authGuard = new ArchitectureEntity(
            "entity:class:AuthGuard",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "AuthGuard",
            "AuthGuard",
            "scope:repo",
            List.of(),
            Map.of("language", "typescript")
        );
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:orders:guard",
            RelationshipKind.DEPENDS_ON,
            ordersRoute.id(),
            authGuard.id(),
            "AuthGuard",
            List.of(),
            Map.of(
                "framework", "angular",
                "frameworkRelationship", "guards",
                "guardReference", "AuthGuard",
                "routeSourceKind", "declared-route"
            ),
            List.of("depends-on-module")
        );

        ArchitectureRelationship normalized = relationshipService.normalizeRelationship(
            relationship,
            Map.of(ordersRoute.id(), ordersRoute, authGuard.id(), authGuard),
            Map.of(relationship.id(), relationship)
        );

        assertEquals(List.of("depends-on-module", "guards-route"), normalized.architecturalSemantics());
    }

    private static ArchitectureEntity routeEntity(String id, String name, String fullPath, Map<String, Object> extraMetadata, List<String> roles) {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("framework", id.contains(":angular-route:") ? "angular" : "react");
        metadata.put("routeSourceKind", "declared-route");
        metadata.put("routeDeclarationKind", "route-object");
        metadata.put("routeFullPath", fullPath);
        metadata.putAll(extraMetadata);
        return new ArchitectureEntity(
            id,
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            name,
            name,
            "scope:repo",
            List.of(),
            metadata,
            roles,
            List.of("route-declared", "user-facing")
        );
    }

    private static ArchitectureRelationship relationship(String id, String fromId, String toId, Map<String, Object> metadata) {
        return new ArchitectureRelationship(id, RelationshipKind.DEPENDS_ON, fromId, toId, id, List.of(), metadata);
    }
}
