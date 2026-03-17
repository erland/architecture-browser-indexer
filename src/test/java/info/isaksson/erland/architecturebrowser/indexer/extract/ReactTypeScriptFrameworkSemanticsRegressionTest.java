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

class ReactTypeScriptFrameworkSemanticsRegressionTest extends AbstractTypeScriptExtractionTestSupport {


    @Test
    void extractsReactJsxCompositionRelationshipsFromCommonTsxPatterns() {
        String source = """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /><OrderSummary /></PageLayout>;
            }

            export const PageLayout = () => <section><Toolbar /></section>;

            export function OrdersTable() { return <table />; }
            export class OrderSummary {}
            export function Toolbar() { return <header />; }
            """;

        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 101, 0, 0, 2, 1, false, false,
            """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /><OrderSummary /></PageLayout>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 16, 26, 0, 16, 0, 26, false, false, "OrdersPage", List.of())
            ));
        SyntaxNode pageLayout = new SyntaxNode("variable_declarator", true, 103, 166, 4, 13, 4, 76, false, false,
            "PageLayout = () => <section><Toolbar /></section>", List.of(
                new SyntaxNode("identifier", true, 103, 113, 4, 13, 4, 23, false, false, "PageLayout", List.of()),
                new SyntaxNode("arrow_function", true, 116, 166, 4, 26, 4, 76, false, false,
                    "() => <section><Toolbar /></section>", List.of())
            ));
        SyntaxNode ordersTable = new SyntaxNode("function_declaration", true, 168, 216, 6, 0, 6, 48, false, false,
            "export function OrdersTable() { return <table />; }", List.of(
                new SyntaxNode("identifier", true, 184, 195, 6, 16, 6, 27, false, false, "OrdersTable", List.of())
            ));
        SyntaxNode orderSummary = new SyntaxNode("class_declaration", true, 217, 246, 7, 0, 7, 29, false, false,
            "export class OrderSummary {}", List.of(
                new SyntaxNode("type_identifier", true, 230, 242, 7, 13, 7, 25, false, false, "OrderSummary", List.of())
            ));
        SyntaxNode toolbar = new SyntaxNode("function_declaration", true, 247, source.length(), 8, 0, 8, 45, false, false,
            "export function Toolbar() { return <header />; }", List.of(
                new SyntaxNode("identifier", true, 263, 270, 8, 16, 8, 23, false, false, "Toolbar", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders/OrdersPage.tsx", source,
            program(source, ordersPage, pageLayout, ordersTable, orderSummary, toolbar));

        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");
        var pageLayoutEntity = entity(result, EntityKind.FUNCTION, "PageLayout");
        var ordersTableEntity = entity(result, EntityKind.FUNCTION, "OrdersTable");
        var orderSummaryEntity = entity(result, EntityKind.CLASS, "OrderSummary");
        var toolbarEntity = entity(result, EntityKind.FUNCTION, "Toolbar");

        assertReactFrameworkRelationship(result, ordersPageEntity.id(), pageLayoutEntity.id(), "PageLayout", true);
        assertReactFrameworkRelationship(result, ordersPageEntity.id(), ordersTableEntity.id(), "OrdersTable", true);
        assertReactFrameworkRelationship(result, ordersPageEntity.id(), orderSummaryEntity.id(), "OrderSummary", true);
        assertReactFrameworkRelationship(result, pageLayoutEntity.id(), toolbarEntity.id(), "Toolbar", true);
        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && ordersPageEntity.id().equals(rel.fromEntityId())
            && "main".equals(rel.label())));
    }



    @Test
    void infersReactJsxCompositionTargetsWhenRenderedComponentIsNotDeclaredInSameFile() {
        String source = """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /></PageLayout>;
            }
            """;

        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, source.length(), 0, 0, 2, 1, false, false,
            """
            export function OrdersPage() {
              return <PageLayout><OrdersTable /></PageLayout>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 16, 26, 0, 16, 0, 26, false, false, "OrdersPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/app/orders/OrdersPage.tsx", source, program(source, ordersPage));

        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");
        var pageLayoutEntity = entity(result, EntityKind.UI_MODULE, "PageLayout");
        var ordersTableEntity = entity(result, EntityKind.UI_MODULE, "OrdersTable");

        assertReactFrameworkRelationship(result, ordersPageEntity.id(), pageLayoutEntity.id(), "PageLayout", false);
        assertReactFrameworkRelationship(result, ordersPageEntity.id(), ordersTableEntity.id(), "OrdersTable", false);
        assertEquals("react-component-target", pageLayoutEntity.metadata().get("targetClassification"));
        assertEquals(Boolean.FALSE, pageLayoutEntity.metadata().get("external"));
    }




    @Test
    void extractsReactCustomHookClassificationAndUsageRelationships() {
        String source = """
            import { useContext } from 'react';
            import { useQuery } from '@tanstack/react-query';

            export const AuthContext = createContext(null);

            export function useAuth() {
              return useContext(AuthContext);
            }

            export function useOrdersQuery() {
              return useQuery({ queryKey: ['orders'], queryFn: async () => [] });
            }

            export function useOrdersScreenState() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return { auth, orders };
            }

            export function OrdersPage() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return <section>{auth?.user}-{orders.data?.length}</section>;
            }
            """;

        SyntaxNode useAuth = new SyntaxNode("function_declaration", true, 0, 0, 5, 0, 7, 1, false, false,
            """
            export function useAuth() {
              return useContext(AuthContext);
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 5, 16, 5, 23, false, false, "useAuth", List.of())
            ));
        SyntaxNode useOrdersQuery = new SyntaxNode("function_declaration", true, 0, 0, 9, 0, 11, 1, false, false,
            """
            export function useOrdersQuery() {
              return useQuery({ queryKey: ['orders'], queryFn: async () => [] });
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 9, 16, 9, 30, false, false, "useOrdersQuery", List.of())
            ));
        SyntaxNode useOrdersScreenState = new SyntaxNode("function_declaration", true, 0, 0, 13, 0, 17, 1, false, false,
            """
            export function useOrdersScreenState() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return { auth, orders };
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 13, 16, 13, 36, false, false, "useOrdersScreenState", List.of())
            ));
        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 0, 19, 0, 23, 1, false, false,
            """
            export function OrdersPage() {
              const auth = useAuth();
              const orders = useOrdersQuery();
              return <section>{auth?.user}-{orders.data?.length}</section>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 19, 16, 19, 26, false, false, "OrdersPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/hooks/useOrders.tsx", source,
            program(source, useAuth, useOrdersQuery, useOrdersScreenState, ordersPage));

        var useAuthEntity = entity(result, EntityKind.FUNCTION, "useAuth");
        var useOrdersQueryEntity = entity(result, EntityKind.FUNCTION, "useOrdersQuery");
        var useOrdersScreenStateEntity = entity(result, EntityKind.FUNCTION, "useOrdersScreenState");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");

        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("reactHook"));
        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("customHook"));
        assertEquals("context", useAuthEntity.metadata().get("hookClassification"));
        assertEquals(Boolean.TRUE, useAuthEntity.metadata().get("declaredReactHook"));

        assertEquals(Boolean.TRUE, useOrdersQueryEntity.metadata().get("reactHook"));
        assertEquals("data-fetch", useOrdersQueryEntity.metadata().get("hookClassification"));

        assertReactHookRelationship(result, useOrdersScreenStateEntity.id(), useAuthEntity.id(), "useAuth", "hook", "context", true);
        assertReactHookRelationship(result, useOrdersScreenStateEntity.id(), useOrdersQueryEntity.id(), "useOrdersQuery", "hook", "data-fetch", true);
        assertReactHookRelationship(result, ordersPageEntity.id(), useAuthEntity.id(), "useAuth", "component", "context", true);
        assertReactHookRelationship(result, ordersPageEntity.id(), useOrdersQueryEntity.id(), "useOrdersQuery", "component", "data-fetch", true);
    }



    @Test
    void extractsReactContextProviderAndConsumerRelationships() {
        String source = """
            import React, { createContext, useContext } from 'react';

            export const AuthContext = createContext(null);

            export function AuthProvider({ children }) {
              return <AuthContext.Provider value={{ user: 'alice' }}>{children}</AuthContext.Provider>;
            }

            export function useAuth() {
              return useContext(AuthContext);
            }

            export function OrdersPage() {
              const auth = useContext(AuthContext);
              return <section>{auth?.user}</section>;
            }
            """;

        SyntaxNode authProvider = new SyntaxNode("function_declaration", true, 0, 0, 4, 0, 6, 1, false, false,
            """
            export function AuthProvider({ children }) {
              return <AuthContext.Provider value={{ user: 'alice' }}>{children}</AuthContext.Provider>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 4, 16, 4, 28, false, false, "AuthProvider", List.of())
            ));
        SyntaxNode useAuth = new SyntaxNode("function_declaration", true, 0, 0, 8, 0, 10, 1, false, false,
            """
            export function useAuth() {
              return useContext(AuthContext);
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 8, 16, 8, 23, false, false, "useAuth", List.of())
            ));
        SyntaxNode ordersPage = new SyntaxNode("function_declaration", true, 0, 0, 12, 0, 15, 1, false, false,
            """
            export function OrdersPage() {
              const auth = useContext(AuthContext);
              return <section>{auth?.user}</section>;
            }
            """.strip(), List.of(
                new SyntaxNode("identifier", true, 0, 0, 12, 16, 12, 26, false, false, "OrdersPage", List.of())
            ));

        StructuralExtractionResult result = extract("src/context/AuthProvider.tsx", source,
            program(source, authProvider, useAuth, ordersPage));

        var authContext = entity(result, EntityKind.UI_MODULE, "AuthContext");
        var authProviderEntity = entity(result, EntityKind.FUNCTION, "AuthProvider");
        var useAuthEntity = entity(result, EntityKind.FUNCTION, "useAuth");
        var ordersPageEntity = entity(result, EntityKind.FUNCTION, "OrdersPage");

        assertEquals(Boolean.TRUE, authContext.metadata().get("reactContext"));
        assertEquals(Boolean.TRUE, authContext.metadata().get("declaredReactContext"));
        assertEquals(Boolean.FALSE, authContext.metadata().get("external"));
        assertReactContextRelationship(result, authProviderEntity.id(), authContext.id(), "AuthContext", "providesContext", true);
        assertReactContextRelationship(result, useAuthEntity.id(), authContext.id(), "AuthContext", "consumesContext", true);
        assertReactContextRelationship(result, ordersPageEntity.id(), authContext.id(), "AuthContext", "consumesContext", true);
    }

}
