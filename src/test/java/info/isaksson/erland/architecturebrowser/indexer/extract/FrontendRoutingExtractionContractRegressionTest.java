package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRoutingExtractionContractRegressionTest extends AbstractTypeScriptExtractionTestSupport {


    @Test
    void extractsAngularFrontendRoutesIncludingNestedLazyGuardsAndResolvers() {
        String source = """
            import { Routes } from '@angular/router';

            export const routes: Routes = [
              {
                path: 'orders',
                component: OrdersPageComponent,
                canActivate: [AuthGuard],
                resolve: { initial: OrdersResolver },
                children: [
                  {
                    path: 'details',
                    loadChildren: () => import('./order-details.module').then(m => m.OrderDetailsModule)
                  }
                ]
              }
            ];

            export class OrdersPageComponent {}
            export class AuthGuard {}
            export class OrdersResolver {}
            export class OrderDetailsModule {}
            """;

        SyntaxNode ordersPage = classDeclaration(0, 0, 15, "OrdersPageComponent", List.of());
        SyntaxNode authGuard = classDeclaration(0, 0, 16, "AuthGuard", List.of());
        SyntaxNode ordersResolver = classDeclaration(0, 0, 17, "OrdersResolver", List.of());
        SyntaxNode detailsModule = classDeclaration(0, 0, 18, "OrderDetailsModule", List.of());

        StructuralExtractionResult result = extract("src/app/app.routes.ts", source,
            program(source, ordersPage, authGuard, ordersResolver, detailsModule));

        var ordersRoute = entity(result, EntityKind.UI_MODULE, "angular-route:/orders");
        var detailsRoute = entity(result, EntityKind.UI_MODULE, "angular-route:/orders/details");
        var ordersPageEntity = entity(result, EntityKind.CLASS, "OrdersPageComponent");
        var authGuardEntity = entity(result, EntityKind.CLASS, "AuthGuard");
        var ordersResolverEntity = entity(result, EntityKind.CLASS, "OrdersResolver");
        var detailsModuleEntity = entity(result, EntityKind.CLASS, "OrderDetailsModule");

        assertFrontendRouteRelationship(result, ordersRoute.id(), ordersPageEntity.id(), "OrdersPageComponent", "angular", "targets", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), authGuardEntity.id(), "AuthGuard", "angular", "guards", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), ordersResolverEntity.id(), "OrdersResolver", "angular", "resolves", true);
        assertFrontendRouteRelationship(result, detailsRoute.id(), detailsModuleEntity.id(), "OrderDetailsModule", "angular", "lazyLoads", true);
        assertFrontendRouteRelationship(result, detailsRoute.id(), ordersRoute.id(), "details", "angular", "childOf", true);
        assertEquals("/orders", ordersRoute.metadata().get("routeFullPath"));
        assertEquals("/orders/details", detailsRoute.metadata().get("routeFullPath"));
    }



    @Test
    void extractsReactFrontendRoutesFromObjectAndJsxRouteDeclarations() {
        String source = """
            import { createBrowserRouter, Route, Routes } from 'react-router-dom';

            export const router = createBrowserRouter([
              {
                path: '/',
                element: <AppShell />,
                children: [
                  {
                    path: 'orders',
                    element: <OrdersPage />
                  }
                ]
              }
            ]);

            export function AppRoutes() {
              return <Routes><Route path="reports" element={<ReportsPage />} /></Routes>;
            }

            export function AppShell() { return <main />; }
            export function OrdersPage() { return <section />; }
            export function ReportsPage() { return <article />; }
            """;

        SyntaxNode appRoutes = new SyntaxNode("function_declaration", true, 0, 0, 14, 0, 16, 1, false, false,
            "export function AppRoutes() { return <Routes><Route path=\"reports\" element={<ReportsPage />} /></Routes>; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 14, 16, 14, 25, false, false, "AppRoutes", List.of())
            ));
        SyntaxNode appShell = new SyntaxNode("function_declaration", true, 0, 0, 18, 0, 18, 47, false, false,
            "export function AppShell() { return <main />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 18, 16, 18, 24, false, false, "AppShell", List.of())
            ));
        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 0, 19, 0, 19, 50, false, false,
            "export function OrdersPage() { return <section />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 19, 16, 19, 26, false, false, "OrdersPage", List.of())
            ));
        SyntaxNode reportsPage = new SyntaxNode("function_declaration", true, 0, 0, 20, 0, 20, 51, false, false,
            "export function ReportsPage() { return <article />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 20, 16, 20, 27, false, false, "ReportsPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/router.tsx", source,
            program(source, appRoutes, appShell, ordersPage, reportsPage));

        var rootRoute = entity(result, EntityKind.UI_MODULE, "react-route:/");
        var ordersRoute = entity(result, EntityKind.UI_MODULE, "react-route:/orders");
        var reportsRoute = entity(result, EntityKind.UI_MODULE, "react-route:/reports");
        var appShellEntity = entity(result, EntityKind.FUNCTION, "AppShell");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");
        var reportsPageEntity = entity(result, EntityKind.FUNCTION, "ReportsPage");

        assertFrontendRouteRelationship(result, rootRoute.id(), appShellEntity.id(), "AppShell", "react", "targets", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), ordersPageEntity.id(), "OrdersPage", "react", "targets", true);
        assertFrontendRouteRelationship(result, ordersRoute.id(), rootRoute.id(), "orders", "react", "childOf", true);
        assertFrontendRouteRelationship(result, reportsRoute.id(), reportsPageEntity.id(), "ReportsPage", "react", "targets", true);
    }

    @Test
    void extractsRedirectsAndStaticNavigationEvidenceForLaterUiNavigationNormalization() {
        String source = """
            import { Link, createBrowserRouter } from 'react-router-dom';
            import { Router } from '@angular/router';

            export const router = createBrowserRouter([
              {
                path: '/',
                element: <AppShell />,
                children: [
                  { path: 'orders', element: <OrdersPage /> },
                  { path: 'legacy', redirectTo: '/orders' }
                ]
              }
            ]);

            export function OrdersPage() {
              return <div><Link to="/reports">Reports</Link></div>;
            }

            export function LegacyEntry() {
              navigate('/orders');
              router.navigate(['/reports']);
              return <section />;
            }

            export function AppShell() { return <main />; }
            export function ReportsPage() { return <article />; }
            """;

        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 0, 14, 0, 16, 1, false, false,
            "export function OrdersPage() { return <div><Link to=\"/reports\">Reports</Link></div>; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 14, 16, 14, 26, false, false, "OrdersPage", List.of())
            ));
        SyntaxNode legacyEntry = new SyntaxNode("function_declaration", true, 0, 0, 18, 0, 21, 1, false, false,
            "export function LegacyEntry() { navigate('/orders'); router.navigate(['/reports']); return <section />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 18, 16, 18, 27, false, false, "LegacyEntry", List.of())
            ));
        SyntaxNode appShell = new SyntaxNode("function_declaration", true, 0, 0, 23, 0, 23, 47, false, false,
            "export function AppShell() { return <main />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 23, 16, 23, 24, false, false, "AppShell", List.of())
            ));
        SyntaxNode reportsPage = new SyntaxNode("function_declaration", true, 0, 0, 24, 0, 24, 51, false, false,
            "export function ReportsPage() { return <article />; }", List.of(
                new SyntaxNode("identifier", true, 0, 0, 24, 16, 24, 27, false, false, "ReportsPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/router.tsx", source,
            program(source, ordersPage, legacyEntry, appShell, reportsPage));

        var legacyRoute = entity(result, EntityKind.UI_MODULE, "react-route:/legacy");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");
        var legacyEntryEntity = entity(result, EntityKind.FUNCTION, "LegacyEntry");
        var ordersRouteTarget = entity(result, EntityKind.UI_MODULE, "react-route:/orders");
        var reportsRouteTarget = entity(result, EntityKind.UI_MODULE, "react-route:/reports");

        assertEquals("route-object", legacyRoute.metadata().get("routeDeclarationKind"));
        assertEquals("/orders", legacyRoute.metadata().get("redirectTargetLiteral"));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && legacyRoute.id().equals(rel.fromEntityId())
            && ordersRouteTarget.id().equals(rel.toEntityId())
            && "redirects".equals(rel.metadata().get("frameworkRelationship"))
            && "/orders".equals(rel.metadata().get("redirectTargetLiteral"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && ordersPageEntity.id().equals(rel.fromEntityId())
            && reportsRouteTarget.id().equals(rel.toEntityId())
            && "linksToRoute".equals(rel.metadata().get("frameworkRelationship"))
            && "link".equals(rel.metadata().get("routeSourceKind"))
            && "/reports".equals(rel.metadata().get("navigationTargetLiteral"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && legacyEntryEntity.id().equals(rel.fromEntityId())
            && ordersRouteTarget.id().equals(rel.toEntityId())
            && "navigatesToRoute".equals(rel.metadata().get("frameworkRelationship"))
            && "navigate-call".equals(rel.metadata().get("routeSourceKind"))
            && "/orders".equals(rel.metadata().get("navigationTargetLiteral"))));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && legacyEntryEntity.id().equals(rel.fromEntityId())
            && reportsRouteTarget.id().equals(rel.toEntityId())
            && "navigatesToRoute".equals(rel.metadata().get("frameworkRelationship"))
            && "navigate-call".equals(rel.metadata().get("routeSourceKind"))
            && "/reports".equals(rel.metadata().get("navigationTargetLiteral"))));
    }

}
