package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FrontendRoutingRegressionTest {

    @Test
    void frontendRoutingExtractionRemainsStableForAngularAndReactRepresentativePatterns() {
        String angularSource = """
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
        StructuralExtractionResult angular = extract("src/app/app.routes.ts", angularSource,
            program(angularSource,
                classDeclaration("OrdersPageComponent", 15),
                classDeclaration("AuthGuard", 16),
                classDeclaration("OrdersResolver", 17),
                classDeclaration("OrderDetailsModule", 18)));

        var angularOrdersRoute = entity(angular, EntityKind.UI_MODULE, "angular-route:/orders");
        var angularDetailsRoute = entity(angular, EntityKind.UI_MODULE, "angular-route:/orders/details");
        assertRouteRelationship(angular, angularOrdersRoute.id(), entity(angular, EntityKind.CLASS, "OrdersPageComponent").id(), "OrdersPageComponent", "angular", "targets");
        assertRouteRelationship(angular, angularOrdersRoute.id(), entity(angular, EntityKind.CLASS, "AuthGuard").id(), "AuthGuard", "angular", "guards");
        assertRouteRelationship(angular, angularOrdersRoute.id(), entity(angular, EntityKind.CLASS, "OrdersResolver").id(), "OrdersResolver", "angular", "resolves");
        assertRouteRelationship(angular, angularDetailsRoute.id(), entity(angular, EntityKind.CLASS, "OrderDetailsModule").id(), "OrderDetailsModule", "angular", "lazyLoads");
        assertRouteRelationship(angular, angularDetailsRoute.id(), angularOrdersRoute.id(), "details", "angular", "childOf");
        assertEquals("/orders", angularOrdersRoute.metadata().get("routeFullPath"));
        assertEquals("/orders/details", angularDetailsRoute.metadata().get("routeFullPath"));

        String reactSource = """
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
              return <Routes><Route path=\"reports\" element={<ReportsPage />} /></Routes>;
            }

            export function AppShell() { return <main />; }
            export function OrdersPage() { return <section />; }
            export function ReportsPage() { return <article />; }
            """;
        StructuralExtractionResult react = extract("src/app/router.tsx", reactSource,
            program(reactSource,
                functionDeclaration("AppRoutes", 14),
                functionDeclaration("AppShell", 18),
                functionDeclaration("OrdersPage", 19),
                functionDeclaration("ReportsPage", 20)));

        var reactRootRoute = entity(react, EntityKind.UI_MODULE, "react-route:/");
        var reactOrdersRoute = entity(react, EntityKind.UI_MODULE, "react-route:/orders");
        var reactReportsRoute = entity(react, EntityKind.UI_MODULE, "react-route:/reports");
        assertRouteRelationship(react, reactRootRoute.id(), entity(react, EntityKind.FUNCTION, "AppShell").id(), "AppShell", "react", "targets");
        assertRouteRelationship(react, reactOrdersRoute.id(), entity(react, EntityKind.FUNCTION, "OrdersPage").id(), "OrdersPage", "react", "targets");
        assertRouteRelationship(react, reactOrdersRoute.id(), reactRootRoute.id(), "orders", "react", "childOf");
        assertRouteRelationship(react, reactReportsRoute.id(), entity(react, EntityKind.FUNCTION, "ReportsPage").id(), "ReportsPage", "react", "targets");
        assertEquals("/reports", reactReportsRoute.metadata().get("routeFullPath"));
    }

    private static void assertRouteRelationship(
        StructuralExtractionResult result,
        String fromId,
        String toId,
        String label,
        String framework,
        String frameworkRelationship
    ) {
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromId.equals(rel.fromEntityId())
                && toId.equals(rel.toEntityId())
                && label.equals(rel.label())
                && framework.equals(rel.metadata().get("framework"))
                && frameworkRelationship.equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals(framework + ":route-" + frameworkRelationship, relationship.metadata().get("dependencySource"));
    }

    private static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of(relativePath), relativePath, ParseLanguage.TYPESCRIPT, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.TYPESCRIPT, 1), Map.of(ParseStatus.SUCCESS, 1)));
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        int endLine = Math.max(0, source.split("\\R", -1).length - 1);
        int endColumn = source.isEmpty() ? 0 : source.length() - source.lastIndexOf('\n') - 1;
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, endLine, endColumn, false, false, source, List.of(children));
    }

    private static SyntaxNode classDeclaration(String name, int line) {
        return new SyntaxNode("class_declaration", true, 0, 0, line, 0, line, name.length(), false, false,
            "export class " + name + " {}", List.of(
                new SyntaxNode("type_identifier", true, 0, 0, line, 13, line, 13 + name.length(), false, false, name, List.of())
            ));
    }

    private static SyntaxNode functionDeclaration(String name, int line) {
        return new SyntaxNode("function_declaration", true, 0, 0, line, 0, line, name.length(), false, false,
            "export function " + name + "() {}", List.of(
                new SyntaxNode("identifier", true, 0, 0, line, 16, line, 16 + name.length(), false, false, name, List.of())
            ));
    }

    private static ExtractedEntityFact entity(StructuralExtractionResult result, EntityKind kind, String name) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .findFirst()
            .orElseThrow();
    }
}
