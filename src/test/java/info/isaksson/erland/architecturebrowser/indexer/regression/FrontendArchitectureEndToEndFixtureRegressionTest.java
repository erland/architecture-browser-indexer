package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseStatus;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseRequest;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxTree;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryEntry;
import info.isaksson.erland.architecturebrowser.indexer.topology.TopologyService;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendArchitectureEndToEndFixtureRegressionTest {

    @Test
    void angularEndToEndFixtureProducesFrameworkAwareViews() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            tsFile(
                "src/app/orders/order.dto.ts",
                "typescript",
                "angular",
                """
                export interface OrderDto {
                  id: string;
                  status: string;
                }
                """,
                List.of(),
                List.of(interfaceDeclaration("""
                    export interface OrderDto {
                      id: string;
                      status: string;
                    }
                    """, "OrderDto", List.of(), List.of(
                    propertySignature("id: string;", "id"),
                    propertySignature("status: string;", "status")
                ), List.of()))
            ),
            tsFile(
                "src/app/orders/orders.tokens.ts",
                "typescript",
                "angular",
                """
                export const ORDER_API = 'ORDER_API';
                export const ORDERS_CONFIG = 'ORDERS_CONFIG';
                """,
                List.of(),
                List.of()
            ),
            tsFile(
                "src/app/orders/orders.api.ts",
                "typescript",
                "angular",
                """
                import type { OrderDto } from './order.dto';

                export class OrdersApi {
                  fetchOrders(): OrderDto { throw new Error('noop'); }
                }
                """,
                List.of("import type { OrderDto } from './order.dto';"),
                List.of(classDeclaration("""
                    export class OrdersApi {
                      fetchOrders(): OrderDto { throw new Error('noop'); }
                    }
                    """, "OrdersApi", List.of(), null, List.of(), List.of(
                    methodDefinition("fetchOrders(): OrderDto { throw new Error('noop'); }", "fetchOrders", "OrderDto")
                )))
            ),
            tsFile(
                "src/app/shared/shared-card.component.ts",
                "typescript",
                "angular",
                """
                import { Component } from '@angular/core';

                @Component({
                  selector: 'shared-card',
                  standalone: true,
                  template: `<section class="card"><ng-content /></section>`
                })
                export class SharedCardComponent {}
                """,
                List.of("import { Component } from '@angular/core';"),
                List.of(classDeclaration("""
                    @Component({
                      selector: 'shared-card',
                      standalone: true,
                      template: `<section class="card"><ng-content /></section>`
                    })
                    export class SharedCardComponent {}
                    """, "SharedCardComponent", List.of("""
                        @Component({
                          selector: 'shared-card',
                          standalone: true,
                          template: `<section class="card"><ng-content /></section>`
                        })
                        """.strip()), null, List.of(), List.of()))
            ),
            tsFile(
                "src/app/orders/order-status.pipe.ts",
                "typescript",
                "angular",
                """
                import { Pipe } from '@angular/core';

                @Pipe({ name: 'orderStatus' })
                export class OrderStatusPipe {}
                """,
                List.of("import { Pipe } from '@angular/core';"),
                List.of(classDeclaration("""
                    @Pipe({ name: 'orderStatus' })
                    export class OrderStatusPipe {}
                    """, "OrderStatusPipe", List.of("@Pipe({ name: 'orderStatus' })"), null, List.of(), List.of()))
            ),
            tsFile(
                "src/app/orders/track-click.directive.ts",
                "typescript",
                "angular",
                """
                import { Directive } from '@angular/core';

                @Directive({ selector: '[appTrackClick]' })
                export class TrackClickDirective {}
                """,
                List.of("import { Directive } from '@angular/core';"),
                List.of(classDeclaration("""
                    @Directive({ selector: '[appTrackClick]' })
                    export class TrackClickDirective {}
                    """, "TrackClickDirective", List.of("@Directive({ selector: '[appTrackClick]' })"), null, List.of(), List.of()))
            ),
            tsFile(
                "src/app/orders/order-list.component.ts",
                "typescript",
                "angular",
                """
                import { Component, Inject } from '@angular/core';
                import { SharedCardComponent } from '../shared/shared-card.component';
                import { ORDER_API } from './orders.tokens';
                import { OrdersApi } from './orders.api';
                import { OrderStatusPipe } from './order-status.pipe';
                import { TrackClickDirective } from './track-click.directive';

                @Component({
                  selector: 'app-order-list',
                  template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
                })
                export class OrderListComponent {
                  status: string;
                  constructor(@Inject(ORDER_API) private api: OrdersApi) {}
                }
                """,
                List.of(
                    "import { Component, Inject } from '@angular/core';",
                    "import { SharedCardComponent } from '../shared/shared-card.component';",
                    "import { ORDER_API } from './orders.tokens';",
                    "import { OrdersApi } from './orders.api';",
                    "import { OrderStatusPipe } from './order-status.pipe';",
                    "import { TrackClickDirective } from './track-click.directive';"
                ),
                List.of(classDeclaration("""
                    @Component({
                      selector: 'app-order-list',
                      template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
                    })
                    export class OrderListComponent {
                      status: string;
                      constructor(@Inject(ORDER_API) private api: OrdersApi) {}
                    }
                    """, "OrderListComponent", List.of("""
                        @Component({
                          selector: 'app-order-list',
                          template: `<shared-card appTrackClick>{{ status | orderStatus }}</shared-card>`
                        })
                        """.strip()), null, List.of(), List.of(
                    publicField("status: string;", "status"),
                    methodDefinition("constructor(@Inject(ORDER_API) private api: OrdersApi) {}", "constructor", null)
                )))
            ),
            tsFile(
                "src/app/orders/orders.routes.ts",
                "typescript",
                "angular",
                """
                import { OrderListComponent } from './order-list.component';

                export const ORDERS_ROUTES = [{ path: 'orders', component: OrderListComponent }];
                """,
                List.of("import { OrderListComponent } from './order-list.component';"),
                List.of()
            ),
            tsFile(
                "src/app/orders/orders.module.ts",
                "typescript",
                "angular",
                """
                import { NgModule } from '@angular/core';
                import { OrderListComponent } from './order-list.component';
                import { OrdersApi } from './orders.api';
                import { ORDER_API } from './orders.tokens';
                import { SharedCardComponent } from '../shared/shared-card.component';
                import { OrderStatusPipe } from './order-status.pipe';
                import { TrackClickDirective } from './track-click.directive';

                @NgModule({
                  declarations: [OrderListComponent, OrderStatusPipe, TrackClickDirective],
                  imports: [SharedCardComponent],
                  providers: [{ provide: ORDER_API, useClass: OrdersApi }],
                  exports: [OrderListComponent]
                })
                export class OrdersModule {}
                """,
                List.of(
                    "import { NgModule } from '@angular/core';",
                    "import { OrderListComponent } from './order-list.component';",
                    "import { OrdersApi } from './orders.api';",
                    "import { ORDER_API } from './orders.tokens';",
                    "import { SharedCardComponent } from '../shared/shared-card.component';",
                    "import { OrderStatusPipe } from './order-status.pipe';",
                    "import { TrackClickDirective } from './track-click.directive';"
                ),
                List.of(classDeclaration("""
                    @NgModule({
                      declarations: [OrderListComponent, OrderStatusPipe, TrackClickDirective],
                      imports: [SharedCardComponent],
                      providers: [{ provide: ORDER_API, useClass: OrdersApi }],
                      exports: [OrderListComponent]
                    })
                    export class OrdersModule {}
                    """, "OrdersModule", List.of("""
                        @NgModule({
                          declarations: [OrderListComponent, OrderStatusPipe, TrackClickDirective],
                          imports: [SharedCardComponent],
                          providers: [{ provide: ORDER_API, useClass: OrdersApi }],
                          exports: [OrderListComponent]
                        })
                        """.strip()), null, List.of(), List.of()))
            )
        ));

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> compositionTypeDependencies = dependencyViewList(document, "compositionTypeDependencies");
        List<Map<String, Object>> routeTypeDependencies = dependencyViewList(document, "routeTypeDependencies");
        List<Map<String, Object>> providerTypeDependencies = dependencyViewList(document, "providerTypeDependencies");
        List<Map<String, Object>> frameworkModuleDependencies = dependencyViewList(document, "frameworkModuleDependencies");

        assertTrue(compositionTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("declares")));
        assertTrue(compositionTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("templateRenders")));
        assertTrue(compositionTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("usesDirective")));
        assertTrue(compositionTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("usesPipe")));
        assertTrue(routeTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("targets")));
        assertTrue(providerTypeDependencies.stream().anyMatch(dep -> {
            List<?> relationships = (List<?>) dep.get("frameworkRelationships");
            return relationships.contains("injects") || relationships.contains("providedBy") || relationships.contains("resolvesTo");
        }), () -> "Expected Angular DI/provider relationships. providerTypeDependencies=" + providerTypeDependencies);
        assertTrue(frameworkModuleDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworks")).contains("angular")));

        @SuppressWarnings("unchecked")
        Map<String, Object> frontendBrowserViews = (Map<String, Object>) dependencyViews(document).get("frontendBrowserViews");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> views = (List<Map<String, Object>>) frontendBrowserViews.get("views");
        assertBrowserView(views, "angularModuleGraph", "angular", "compositionTypeDependencies", "compositionModuleDependencies", "declares");
        assertBrowserView(views, "angularProviderGraph", "angular", "providerTypeDependencies", "providerModuleDependencies", "injects");
        assertBrowserView(views, "routeGraph", "frontend", "routeTypeDependencies", "routeModuleDependencies", "targets");
    }

    @Test
    void reactEndToEndFixtureProducesFrameworkAwareViews() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            tsFile(
                "src/domain/OrderDto.ts",
                "typescript",
                "react",
                """
                export interface OrderDto {
                  id: string;
                  total: number;
                }
                """,
                List.of(),
                List.of(interfaceDeclaration("""
                    export interface OrderDto {
                      id: string;
                      total: number;
                    }
                    """, "OrderDto", List.of(), List.of(
                    propertySignature("id: string;", "id"),
                    propertySignature("total: number;", "total")
                ), List.of()))
            ),
            tsFile(
                "src/hooks/useOrdersQuery.ts",
                "typescript",
                "react",
                """
                import { useEffect, useState } from 'react';
                import type { OrderDto } from '../domain/OrderDto';

                export function useOrdersQuery(): OrderDto[] {
                  const [orders, setOrders] = useState<OrderDto[]>([]);
                  useEffect(() => { setOrders([]); }, []);
                  return orders;
                }
                """,
                List.of(
                    "import { useEffect, useState } from 'react';",
                    "import type { OrderDto } from '../domain/OrderDto';"
                ),
                List.of(functionDeclaration("""
                    export function useOrdersQuery(): OrderDto[] {
                      const [orders, setOrders] = useState<OrderDto[]>([]);
                      useEffect(() => { setOrders([]); }, []);
                      return orders;
                    }
                    """, "useOrdersQuery"))
            ),
            tsFile(
                "src/context/OrdersContext.tsx",
                "typescript",
                "react",
                """
                import React, { createContext } from 'react';
                import type { OrderDto } from '../domain/OrderDto';

                export const OrdersContext = createContext<OrderDto[] | null>(null);
                """,
                List.of(
                    "import React, { createContext } from 'react';",
                    "import type { OrderDto } from '../domain/OrderDto';"
                ),
                List.of()
            ),
            tsFile(
                "src/components/OrdersTable.tsx",
                "typescript",
                "react",
                """
                import React from 'react';
                import type { OrderDto } from '../domain/OrderDto';

                export function OrdersTable(props: { orders: OrderDto[] }) { return <table />; }
                """,
                List.of(
                    "import React from 'react';",
                    "import type { OrderDto } from '../domain/OrderDto';"
                ),
                List.of(functionDeclaration("export function OrdersTable(props: { orders: OrderDto[] }) { return <table />; }", "OrdersTable"))
            ),
            tsFile(
                "src/context/OrdersProvider.tsx",
                "typescript",
                "react",
                """
                import React from 'react';
                import { OrdersContext } from './OrdersContext';
                import { useOrdersQuery } from '../hooks/useOrdersQuery';

                export function OrdersProvider() {
                  const orders = useOrdersQuery();
                  return <OrdersContext.Provider value={orders}><section /></OrdersContext.Provider>;
                }
                """,
                List.of(
                    "import React from 'react';",
                    "import { OrdersContext } from './OrdersContext';",
                    "import { useOrdersQuery } from '../hooks/useOrdersQuery';"
                ),
                List.of(functionDeclaration("""
                    export function OrdersProvider() {
                      const orders = useOrdersQuery();
                      return <OrdersContext.Provider value={orders}><section /></OrdersContext.Provider>;
                    }
                    """, "OrdersProvider"))
            ),
            tsFile(
                "src/pages/OrdersPage.tsx",
                "typescript",
                "react",
                """
                import React, { useContext } from 'react';
                import { OrdersContext } from '../context/OrdersContext';
                import { OrdersTable } from '../components/OrdersTable';

                export function OrdersPage() {
                  const orders = useContext(OrdersContext) ?? [];
                  return <main><OrdersTable orders={orders} /></main>;
                }
                """,
                List.of(
                    "import React, { useContext } from 'react';",
                    "import { OrdersContext } from '../context/OrdersContext';",
                    "import { OrdersTable } from '../components/OrdersTable';"
                ),
                List.of(functionDeclaration("""
                    export function OrdersPage() {
                      const orders = useContext(OrdersContext) ?? [];
                      return <main><OrdersTable orders={orders} /></main>;
                    }
                    """, "OrdersPage"))
            ),
            tsFile(
                "src/routes/AppRoutes.tsx",
                "typescript",
                "react",
                """
                import React from 'react';
                import { Routes, Route } from 'react-router-dom';
                import { OrdersPage } from '../pages/OrdersPage';

                export function AppRoutes() {
                  return <Routes><Route path="orders" element={<OrdersPage />} /></Routes>;
                }
                """,
                List.of(
                    "import React from 'react';",
                    "import { Routes, Route } from 'react-router-dom';",
                    "import { OrdersPage } from '../pages/OrdersPage';"
                ),
                List.of(functionDeclaration("""
                    export function AppRoutes() {
                      return <Routes><Route path="orders" element={<OrdersPage />} /></Routes>;
                    }
                    """, "AppRoutes"))
            )
        ));

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> compositionTypeDependencies = dependencyViewList(document, "compositionTypeDependencies");
        List<Map<String, Object>> routeTypeDependencies = dependencyViewList(document, "routeTypeDependencies");
        List<Map<String, Object>> providerTypeDependencies = dependencyViewList(document, "providerTypeDependencies");
        List<Map<String, Object>> hookTypeDependencies = dependencyViewList(document, "hookTypeDependencies");
        List<Map<String, Object>> frameworkTypeDependencies = dependencyViewList(document, "frameworkTypeDependencies");

        assertTrue(compositionTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("renders")));
        assertTrue(routeTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("targets")));
        assertTrue(providerTypeDependencies.stream().anyMatch(dep -> {
            List<?> relationships = (List<?>) dep.get("frameworkRelationships");
            return relationships.contains("providesContext") || relationships.contains("consumesContext");
        }), () -> "Expected React context dependencies. providerTypeDependencies=" + providerTypeDependencies);
        assertTrue(hookTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("usesHook")));
        assertTrue(frameworkTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworks")).contains("react")));

        @SuppressWarnings("unchecked")
        Map<String, Object> frontendBrowserViews = (Map<String, Object>) dependencyViews(document).get("frontendBrowserViews");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> views = (List<Map<String, Object>>) frontendBrowserViews.get("views");
        assertBrowserView(views, "routeGraph", "frontend", "routeTypeDependencies", "routeModuleDependencies", "targets");
        assertBrowserView(views, "reactComponentCompositionGraph", "react", "compositionTypeDependencies", "compositionModuleDependencies", "renders");
        assertBrowserView(views, "reactContextGraph", "react", "providerTypeDependencies", "providerModuleDependencies", "providesContext");
        assertBrowserView(views, "reactHookGraph", "react", "hookTypeDependencies", "hookModuleDependencies", "usesHook");
    }

    private static void assertBrowserView(
        List<Map<String, Object>> views,
        String id,
        String framework,
        String typeDependencyView,
        String moduleDependencyView,
        String frameworkRelationship
    ) {
        Map<String, Object> view = views.stream()
            .filter(candidate -> id.equals(candidate.get("id")))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing browser view " + id + ". views=" + views));
        assertEquals(framework, view.get("framework"));
        assertEquals(typeDependencyView, view.get("typeDependencyView"));
        assertEquals(moduleDependencyView, view.get("moduleDependencyView"));
        assertEquals(Boolean.TRUE, view.get("available"));
        assertTrue(((List<?>) view.get("frameworkRelationships")).contains(frameworkRelationship),
            () -> "Expected framework relationship " + frameworkRelationship + " in view=" + view);
    }

    private static ArchitectureIndexDocument buildDocument(List<TsFixtureFile> files) {
        Set<String> technologies = files.stream()
            .flatMap(file -> file.technologies().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));

        FileInventory inventory = new FileInventory(
            files.stream()
                .map(file -> new FileInventoryEntry(file.path(), file.source().length(), extension(file.path()), "source", file.language(), false, List.copyOf(file.technologies())))
                .toList(),
            files.size(),
            files.size(),
            0,
            Set.of(ParseLanguage.TYPESCRIPT.name().toLowerCase()),
            technologies
        );

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            files.stream()
                .map(file -> new SourceParseResult(
                    new SourceParseRequest(Path.of(file.path()), file.path(), ParseLanguage.TYPESCRIPT, file.source()),
                    ParseStatus.SUCCESS,
                    new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", file.root(), false, file.root().nodeCount()),
                    List.of(),
                    Map.of("parserBackend", "tree-sitter-jtreesitter")
                ))
                .toList(),
            Map.of(ParseLanguage.TYPESCRIPT, files.size()),
            Map.of(ParseStatus.SUCCESS, files.size())
        );

        StructuralExtractionResult extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatchResult);
        InterpretationResult interpretation = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
        TopologyResult topology = new TopologyService().infer(inventory, extraction, interpretation);

        return ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("fixture", "/tmp/fixture", Instant.parse("2026-03-15T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extraction,
            interpretation,
            topology
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dependencyViews(ArchitectureIndexDocument document) {
        return (Map<String, Object>) document.metadata().get("dependencyViews");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> dependencyViewList(ArchitectureIndexDocument document, String key) {
        return (List<Map<String, Object>>) dependencyViews(document).get(key);
    }

    private static TsFixtureFile tsFile(String path, String language, String technology, String body, List<String> imports, List<SyntaxNode> declarations) {
        String source = normalize(body);
        List<SyntaxNode> children = new ArrayList<>();
        for (String importSnippet : imports) {
            children.add(importStatement(source, importSnippet));
        }
        children.addAll(declarations);
        return new TsFixtureFile(path, source, program(source, children.toArray(SyntaxNode[]::new)), language, Set.of(technology));
    }

    private static SyntaxNode program(String source, SyntaxNode... children) {
        int[] end = lineAndColumn(source, source.length());
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, end[0], end[1], false, false, source, List.of(children));
    }

    private static SyntaxNode importStatement(String source, String snippet) {
        return node("import_statement", source, snippet, List.of());
    }

    private static SyntaxNode interfaceDeclaration(String snippet, String name, List<String> extendsTypes, List<SyntaxNode> properties, List<SyntaxNode> methods) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "type_identifier", name));
        if (!extendsTypes.isEmpty()) {
            List<SyntaxNode> extChildren = extendsTypes.stream().map(type -> localLeaf(normalized, "type_identifier", type)).toList();
            children.add(localNode(normalized, "extends_clause", "extends " + String.join(", ", extendsTypes), extChildren));
        }
        children.addAll(properties);
        children.addAll(methods);
        return localNode(normalized, "interface_declaration", normalized, children);
    }

    private static SyntaxNode classDeclaration(String snippet, String name, List<String> decorators, String extendsType, List<String> implementsTypes, List<SyntaxNode> members) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        decorators.forEach(decorator -> children.add(localNode(normalized, "decorator", decorator, List.of())));
        children.add(localLeaf(normalized, "type_identifier", name));
        if (extendsType != null) {
            children.add(localNode(normalized, "extends_clause", "extends " + extendsType, List.of(localLeaf(normalized, "type_identifier", extendsType))));
        }
        if (!implementsTypes.isEmpty()) {
            List<SyntaxNode> implChildren = implementsTypes.stream().map(type -> localLeaf(normalized, "type_identifier", type)).toList();
            children.add(localNode(normalized, "implements_clause", "implements " + String.join(", ", implementsTypes), implChildren));
        }
        children.addAll(members);
        return localNode(normalized, "class_declaration", normalized, children);
    }

    private static SyntaxNode publicField(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = List.of(
            localLeaf(normalized, "property_identifier", name),
            localLeaf(normalized, "type_identifier", declaredType(normalized))
        );
        return localNode(normalized, "public_field_definition", normalized, children);
    }

    private static SyntaxNode propertySignature(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = List.of(
            localLeaf(normalized, "property_identifier", name),
            localLeaf(normalized, "type_identifier", declaredType(normalized))
        );
        return localNode(normalized, "property_signature", normalized, children);
    }

    private static SyntaxNode methodDefinition(String snippet, String name, String returnType) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "property_identifier", name));
        children.add(localNode(normalized, "formal_parameters", between(normalized, '(', ')'), List.of()));
        if (returnType != null) {
            children.add(localLeaf(normalized, "type_identifier", returnType));
        }
        return localNode(normalized, "method_definition", normalized, children);
    }

    private static SyntaxNode functionDeclaration(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "identifier", name));
        children.add(localNode(normalized, "formal_parameters", between(normalized, '(', ')'), List.of()));
        return localNode(normalized, "function_declaration", normalized, children);
    }

    private static SyntaxNode node(String type, String source, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) {
            throw new IllegalArgumentException("Snippet not found: " + snippet);
        }
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        return new SyntaxNode(type, true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, children);
    }

    private static SyntaxNode localNode(String source, String type, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) {
            throw new IllegalArgumentException("Snippet not found in local source: " + snippet);
        }
        int end = start + snippet.length();
        int[] from = lineAndColumn(source, start);
        int[] to = lineAndColumn(source, end);
        return new SyntaxNode(type, true, start, end, from[0], from[1], to[0], to[1], false, false, snippet, children);
    }

    private static SyntaxNode localLeaf(String source, String type, String text) {
        return localNode(source, type, text, List.of());
    }

    private static int[] lineAndColumn(String source, int offset) {
        int line = 0;
        int column = 0;
        for (int i = 0; i < offset && i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }

    private static String between(String text, char start, char end) {
        int from = text.indexOf(start);
        int to = text.indexOf(end, from + 1);
        if (from < 0 || to < 0) {
            return "()";
        }
        return text.substring(from, to + 1);
    }

    private static String declaredType(String snippet) {
        int colon = snippet.indexOf(':');
        if (colon < 0) {
            return "";
        }
        String tail = snippet.substring(colon + 1).trim();
        if (tail.endsWith(";")) {
            tail = tail.substring(0, tail.length() - 1).trim();
        }
        return tail;
    }

    private static String normalize(String text) {
        return text.stripIndent().strip() + "\n";
    }

    private static String extension(String path) {
        int idx = path.lastIndexOf('.');
        return idx < 0 ? "" : path.substring(idx + 1);
    }

    private record TsFixtureFile(String path, String source, SyntaxNode root, String language, Set<String> technologies) {}
}
