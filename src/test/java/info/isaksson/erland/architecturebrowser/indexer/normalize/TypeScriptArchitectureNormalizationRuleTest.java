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

class TypeScriptArchitectureNormalizationRuleTest {

    private final ArchitectureEntityNormalizationService entityService = ArchitectureEntityNormalizationService.defaultService();
    private final ArchitectureRelationshipNormalizationService relationshipService = ArchitectureRelationshipNormalizationService.defaultService();

    @Test
    void mapsTypeScriptUiServiceAdapterAndProviderToCanonicalRoles() {
        ArchitectureEntity routeSource = entity("entity:ts:orders-page-source", EntityKind.CLASS, Map.of(
            "language", "typescript",
            "path", "src/pages/orders/OrdersPage.tsx",
            "sourceSnippet", "export function OrdersPage() { return <div/>; }"
        ));
        ArchitectureEntity uiModule = new ArchitectureEntity(
            "entity:ts:orders-page-ui",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "OrdersPage",
            "OrdersPage ui module",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "typescript",
                "sourceEntityId", routeSource.id(),
                "uiProfile", "page-or-router"
            )
        );
        ArchitectureEntity serviceSource = entity("entity:ts:orders-store-source", EntityKind.CLASS, Map.of(
            "language", "typescript",
            "path", "src/store/ordersSlice.ts",
            "sourceSnippet", "createSlice({ name: 'orders' })"
        ));
        ArchitectureEntity appService = new ArchitectureEntity(
            "entity:ts:orders-store-service",
            EntityKind.SERVICE,
            EntityOrigin.INFERRED,
            "ordersSlice",
            "ordersSlice service",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "typescript",
                "sourceEntityId", serviceSource.id(),
                "serviceProfile", "state-module"
            )
        );
        ArchitectureEntity adapterSource = entity("entity:ts:orders-api-source", EntityKind.CLASS, Map.of(
            "language", "typescript",
            "path", "src/api/ordersApi.ts",
            "sourceSnippet", "export async function fetchOrders() { return fetch('/api/orders'); }"
        ));
        ArchitectureEntity adapter = new ArchitectureEntity(
            "entity:ts:orders-api-service",
            EntityKind.SERVICE,
            EntityOrigin.INFERRED,
            "ordersApi",
            "ordersApi service",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "typescript",
                "sourceEntityId", adapterSource.id(),
                "serviceProfile", "api-client-or-service"
            )
        );
        ArchitectureEntity providerSource = entity("entity:ts:session-provider-source", EntityKind.CLASS, Map.of(
            "language", "typescript",
            "path", "src/context/SessionProvider.tsx",
            "sourceSnippet", "const SessionContext = createContext(null); export function SessionProvider() {}"
        ));
        ArchitectureEntity provider = new ArchitectureEntity(
            "entity:ts:session-provider-ui",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "SessionProvider",
            "SessionProvider ui module",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "typescript",
                "sourceEntityId", providerSource.id(),
                "uiProfile", "react-context"
            )
        );

        Map<String, ArchitectureEntity> entities = Map.of(
            routeSource.id(), routeSource,
            uiModule.id(), uiModule,
            serviceSource.id(), serviceSource,
            appService.id(), appService,
            adapterSource.id(), adapterSource,
            adapter.id(), adapter,
            providerSource.id(), providerSource,
            provider.id(), provider
        );

        assertEquals(List.of("api-entrypoint", "ui-page"), entityService.normalizeEntity(uiModule, entities).architecturalRoles());
        assertEquals(List.of("externally-exposed", "user-facing"), entityService.normalizeEntity(uiModule, entities).architecturalTraits());
        assertEquals(List.of("application-service"), entityService.normalizeEntity(appService, entities).architecturalRoles());
        assertEquals(List.of("integration-adapter"), entityService.normalizeEntity(adapter, entities).architecturalRoles());
        assertEquals(List.of("configuration-provider"), entityService.normalizeEntity(provider, entities).architecturalRoles());
        assertEquals(List.of("configuration-driven"), entityService.normalizeEntity(provider, entities).architecturalTraits());
    }


    @Test
    void mapsFrontendRouteEntitiesToUiPageAndLayoutRoles() {
        ArchitectureEntity ordersRoute = new ArchitectureEntity(
            "entity:react-route:/orders",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "react-route:/orders",
            "Orders route",
            "scope:repo",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "declared-route",
                "routeDeclarationKind", "route-object",
                "routeFullPath", "/orders",
                "routePath", "orders",
                "routeSnippet", "{ path: 'orders', element: <OrdersPage /> }"
            )
        );
        ArchitectureEntity shellRoute = new ArchitectureEntity(
            "entity:react-route:/app",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "AppLayout",
            "App layout route",
            "scope:repo",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "declared-route",
                "routeDeclarationKind", "route-object",
                "routeFullPath", "/app",
                "routePath", "",
                "routeSnippet", "{ path: '', element: <AppLayout />, children: [{ path: 'orders' }] }"
            )
        );

        ArchitectureEntity normalizedPage = entityService.normalizeEntity(ordersRoute, Map.of(ordersRoute.id(), ordersRoute));
        ArchitectureEntity normalizedLayout = entityService.normalizeEntity(shellRoute, Map.of(shellRoute.id(), shellRoute));

        assertEquals(List.of("ui-page"), normalizedPage.architecturalRoles());
        assertEquals(List.of("route-declared", "user-facing"), normalizedPage.architecturalTraits());
        assertEquals(List.of("ui-layout"), normalizedLayout.architecturalRoles());
        assertEquals(List.of("route-declared", "user-facing"), normalizedLayout.architecturalTraits());
    }

    @Test
    void mapsGroundedNavigationStructureToUiNavigationNode() {
        ArchitectureEntity navComponent = entity(
            "entity:ts:orders-sidebar",
            EntityKind.UI_MODULE,
            Map.of(
                "language", "typescript",
                "path", "src/navigation/OrdersSidebar.tsx",
                "sourceSnippet", "export function OrdersSidebar() { return <nav><NavLink to='/orders'>Orders</NavLink></nav>; }"
            )
        );

        ArchitectureEntity normalized = entityService.normalizeEntity(navComponent, Map.of(navComponent.id(), navComponent));

        assertEquals(List.of("ui-navigation-node"), normalized.architecturalRoles());
        assertEquals(List.of("user-facing"), normalized.architecturalTraits());
    }

    @Test
    void mapsTypeScriptRequestAndIntegrationRelationshipsConservatively() {
        ArchitectureEntity entrypoint = entityWithRoles("entity:ui", EntityKind.UI_MODULE, Map.of("language", "typescript"), List.of("api-entrypoint"));
        ArchitectureEntity service = entityWithRoles("entity:svc", EntityKind.SERVICE, Map.of("language", "typescript"), List.of("application-service"));
        ArchitectureEntity adapter = entityWithRoles("entity:adapter", EntityKind.SERVICE, Map.of("language", "typescript"), List.of("integration-adapter"));
        ArchitectureEntity external = entityWithRoles("entity:external", EntityKind.EXTERNAL_SYSTEM, Map.of("language", "yaml", "external", true), List.of("external-dependency"));

        Map<String, ArchitectureEntity> entities = Map.of(
            entrypoint.id(), entrypoint,
            service.id(), service,
            adapter.id(), adapter,
            external.id(), external
        );

        ArchitectureRelationship uiToService = relationship("rel:ui:svc", RelationshipKind.USES, entrypoint.id(), service.id(), Map.of("sourceLanguage", "typescript"));
        ArchitectureRelationship adapterToExternal = relationship("rel:adapter:ext", RelationshipKind.CALLS, adapter.id(), external.id(), Map.of("sourceLanguage", "typescript"));

        assertEquals(List.of("invokes-use-case"), relationshipService.normalizeRelationship(uiToService, entities, Map.of()).architecturalSemantics());
        assertEquals(List.of("calls-external-system"), relationshipService.normalizeRelationship(adapterToExternal, entities, Map.of()).architecturalSemantics());
    }

    private static ArchitectureEntity entity(String id, EntityKind kind, Map<String, Object> metadata) {
        return new ArchitectureEntity(id, kind, EntityOrigin.OBSERVED, id, id, "scope:repo", List.of(), metadata);
    }

    private static ArchitectureEntity entityWithRoles(String id, EntityKind kind, Map<String, Object> metadata, List<String> roles) {
        return new ArchitectureEntity(id, kind, EntityOrigin.OBSERVED, id, id, "scope:repo", List.of(), metadata, roles, null);
    }

    private static ArchitectureRelationship relationship(String id, RelationshipKind kind, String fromId, String toId, Map<String, Object> metadata) {
        return new ArchitectureRelationship(id, kind, fromId, toId, id, List.of(), metadata);
    }
}
