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
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptArchitectureNormalizationModularizationTest {

    private final ArchitectureEntityNormalizationService entityService = ArchitectureEntityNormalizationService.defaultService();
    private final ArchitectureRelationshipNormalizationService relationshipService = ArchitectureRelationshipNormalizationService.defaultService();

    @Test
    void entityAndRelationshipRulesStillNormalizeUsingSharedTypeScriptSemanticsHelpers() {
        ArchitectureEntity pageSource = new ArchitectureEntity(
            "entity:ts:page-source",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrdersPage",
            "OrdersPage",
            "scope:repo",
            List.of(),
            Map.of(
                "language", "typescript",
                "path", "src/pages/orders/OrdersPage.tsx",
                "sourceSnippet", "export function OrdersPage() { return <Link to='/orders'>Orders</Link>; }"
            )
        );
        ArchitectureEntity page = new ArchitectureEntity(
            "entity:ts:page-ui",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "OrdersPage",
            "OrdersPage",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "typescript",
                "sourceEntityId", pageSource.id(),
                "uiProfile", "page-or-router"
            )
        );
        ArchitectureEntity route = new ArchitectureEntity(
            "entity:react-route:/orders",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "OrdersRoute",
            "OrdersRoute",
            "scope:repo",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "declared-route",
                "routeDeclarationKind", "route-object",
                "routeFullPath", "/orders",
                "routePath", "orders",
                "routeSnippet", "{ path: 'orders', element: <OrdersPage /> }"
            ),
            List.of("ui-page"),
            List.of("route-declared", "user-facing")
        );
        ArchitectureRelationship navigation = new ArchitectureRelationship(
            "rel:page:route",
            RelationshipKind.DEPENDS_ON,
            page.id(),
            route.id(),
            "page to route",
            List.of(),
            Map.of(
                "framework", "react",
                "frameworkRelationship", "linksToRoute",
                "routeSourceKind", "link",
                "navigationTargetLiteral", "/orders",
                "emittedRelationshipKind", "linksToRoute"
            )
        );

        Map<String, ArchitectureEntity> entities = Map.of(
            pageSource.id(), pageSource,
            page.id(), page,
            route.id(), route
        );

        ArchitectureEntity normalizedPage = entityService.normalizeEntity(page, entities);
        ArchitectureRelationship normalizedNavigation = relationshipService.normalizeRelationship(
            navigation,
            entities,
            Map.of(navigation.id(), navigation)
        );

        assertTrue(normalizedPage.architecturalRoles().contains("api-entrypoint"));
        assertTrue(normalizedPage.architecturalRoles().contains("ui-page"));
        assertEquals(List.of("navigates-to"), normalizedNavigation.architecturalSemantics());
    }
}
