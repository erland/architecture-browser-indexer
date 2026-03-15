package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
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

class TypeScriptArchitectureFixtureRegressionTest {

    @Test
    void layeredReactFixtureProducesArchitectFacingTypeScriptViews() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            tsFile(
                "src/api/contracts/OrderDto.ts",
                "typescript",
                "react",
                """
                export interface OrderDto {
                  id: string;
                  total: number;
                }
                """,
                List.of(),
                List.of(
                    interfaceDeclaration("""
                        export interface OrderDto {
                          id: string;
                          total: number;
                        }
                        """, "OrderDto", List.of(), List.of(
                        propertySignature("id: string;", "id"),
                        propertySignature("total: number;", "total")
                    ), List.of())
                )
            ),
            tsFile(
                "src/api/client/OrderApi.ts",
                "typescript",
                "react",
                """
                import type { OrderDto } from '../contracts/OrderDto';

                export interface OrderApi {
                  fetchOrder(id: string): OrderDto;
                }
                """,
                List.of("import type { OrderDto } from '../contracts/OrderDto';"),
                List.of(
                    interfaceDeclaration("""
                        export interface OrderApi {
                          fetchOrder(id: string): OrderDto;
                        }
                        """, "OrderApi", List.of(), List.of(), List.of(
                        methodSignature("fetchOrder(id: string): OrderDto;", "fetchOrder", "OrderDto")
                    ))
                )
            ),
            tsFile(
                "src/services/core/OrderService.ts",
                "typescript",
                "react",
                """
                import type { OrderDto } from '../../api/contracts/OrderDto';
                import type { OrderApi } from '../../api/client/OrderApi';

                export class OrderService {
                  currentOrder: OrderDto;
                  constructor(api: OrderApi) {}
                  loadOrder(id: string): OrderDto { return this.currentOrder; }
                }
                """,
                List.of(
                    "import type { OrderDto } from '../../api/contracts/OrderDto';",
                    "import type { OrderApi } from '../../api/client/OrderApi';"
                ),
                List.of(
                    classDeclaration("""
                        export class OrderService {
                          currentOrder: OrderDto;
                          constructor(api: OrderApi) {}
                          loadOrder(id: string): OrderDto { return this.currentOrder; }
                        }
                        """, "OrderService", List.of(), null, List.of(), List.of(
                        publicField("currentOrder: OrderDto;", "currentOrder"),
                        methodDefinition("constructor(api: OrderApi) {}", "constructor", null),
                        methodDefinition("loadOrder(id: string): OrderDto { return this.currentOrder; }", "loadOrder", "OrderDto")
                    ))
                )
            ),
            tsFile(
                "src/state/orders/OrdersStore.ts",
                "typescript",
                "react",
                """
                import type { OrderDto } from '../../api/contracts/OrderDto';
                import { OrderService } from '../../services/core/OrderService';

                export class OrdersStore {
                  service: OrderService;
                  current(): OrderDto { return this.service.loadOrder('1'); }
                }
                """,
                List.of(
                    "import type { OrderDto } from '../../api/contracts/OrderDto';",
                    "import { OrderService } from '../../services/core/OrderService';"
                ),
                List.of(
                    classDeclaration("""
                        export class OrdersStore {
                          service: OrderService;
                          current(): OrderDto { return this.service.loadOrder('1'); }
                        }
                        """, "OrdersStore", List.of(), null, List.of(), List.of(
                        publicField("service: OrderService;", "service"),
                        methodDefinition("current(): OrderDto { return this.service.loadOrder('1'); }", "current", "OrderDto")
                    ))
                )
            ),
            tsFile(
                "src/app/pages/OrdersPage.tsx",
                "typescript",
                "react",
                """
                import React from 'react';
                import { OrdersStore } from '../../state/orders/OrdersStore';

                export function OrdersPage(store: OrdersStore) { return <main />; }
                """,
                List.of(
                    "import React from 'react';",
                    "import { OrdersStore } from '../../state/orders/OrdersStore';"
                ),
                List.of(
                    functionDeclaration("export function OrdersPage(store: OrdersStore) { return <main />; }", "OrdersPage")
                )
            )
        ));

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> packageDependencies = dependencyViewList(document, "packageDependencies");
        List<Map<String, Object>> moduleDependencies = dependencyViewList(document, "moduleDependencies");
        assertTrue(
            !packageDependencies.isEmpty() || !moduleDependencies.isEmpty(),
            () -> "Expected package or module dependency views to be present. packageDependencies="
                + packageDependencies + ", moduleDependencies=" + moduleDependencies
                + ", typeDependencies=" + dependencyViewList(document, "typeDependencies")
        );

        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertTrue(typeDependencies.stream().anyMatch(dep ->
            "OrdersStore".equals(dep.get("sourceTypeName"))
                && "OrderService".equals(dep.get("targetTypeName"))
                && ((List<?>) dep.get("dependencySources")).contains("field")
                && Boolean.TRUE.equals(dep.get("internalTarget"))
                && "internal".equals(dep.get("targetBoundary"))
        ));

        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertTrue(
            evidenceDependencies.stream().anyMatch(dep ->
                "src/app/pages/OrdersPage.tsx".equals(dep.get("sourceName"))
                    && "react".equals(dep.get("targetName"))
                    && Boolean.TRUE.equals(dep.get("externalTarget"))
                    && ((List<?>) dep.get("dependencySources")).contains("import")
            ) || document.entities().stream().anyMatch(entity ->
                "react".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED
            ),
            () -> "Expected react to appear as external evidence or inferred external entity. evidenceDependencies="
                + evidenceDependencies + ", entities=" + document.entities()
        );

        ArchitectureEntity orderDto = entity(document, EntityKind.INTERFACE, "OrderDto");
        ArchitectureEntity orderService = entity(document, EntityKind.CLASS, "OrderService");
        assertEquals("interface", orderDto.metadata().get("declarationKind"));
        assertEquals("class", orderService.metadata().get("declarationKind"));
        assertTrue(document.entities().stream().anyMatch(entity ->
            entity.kind() == EntityKind.UI_MODULE
                && "OrdersPage".equals(entity.name())
                && ("page-or-router".equals(entity.metadata().get("uiProfile"))
                    || "react-function-component".equals(entity.metadata().get("uiProfile"))
                    || entity.metadata().get("uiProfile") == null)
        ), () -> "Expected OrdersPage to be interpreted as a UI module. entities=" + document.entities());
    }

    @Test
    void angularFixturePreservesRolesDeclarationKindsAndBoundarySignals() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            tsFile(
                "src/app/orders/order.dto.ts",
                "typescript",
                "angular",
                """
                export interface OrderDto {
                  id: string;
                }
                """,
                List.of(),
                List.of(
                    interfaceDeclaration("""
                        export interface OrderDto {
                          id: string;
                        }
                        """, "OrderDto", List.of(), List.of(
                        propertySignature("id: string;", "id")
                    ), List.of())
                )
            ),
            tsFile(
                "src/app/orders/order.service.ts",
                "typescript",
                "angular",
                """
                import { Injectable } from '@angular/core';
                import type { OrderDto } from './order.dto';

                @Injectable()
                export class OrderService {
                  current: OrderDto;
                }
                """,
                List.of(
                    "import { Injectable } from '@angular/core';",
                    "import type { OrderDto } from './order.dto';"
                ),
                List.of(
                    classDeclaration("""
                        @Injectable()
                        export class OrderService {
                          current: OrderDto;
                        }
                        """, "OrderService", List.of("@Injectable()"), null, List.of(), List.of(
                        publicField("current: OrderDto;", "current")
                    ))
                )
            ),
            tsFile(
                "src/app/orders/order-list.component.ts",
                "typescript",
                "angular",
                """
                import { Component } from '@angular/core';
                import { OrderService } from './order.service';

                @Component({ selector: 'app-order-list' })
                export class OrderListComponent {
                  service: OrderService;
                }
                """,
                List.of(
                    "import { Component } from '@angular/core';",
                    "import { OrderService } from './order.service';"
                ),
                List.of(
                    classDeclaration("""
                        @Component({ selector: 'app-order-list' })
                        export class OrderListComponent {
                          service: OrderService;
                        }
                        """, "OrderListComponent", List.of("@Component({ selector: 'app-order-list' })"), null, List.of(), List.of(
                        publicField("service: OrderService;", "service")
                    ))
                )
            ),
            tsFile(
                "src/app/orders/orders.module.ts",
                "typescript",
                "angular",
                """
                import { NgModule } from '@angular/core';
                import { OrderListComponent } from './order-list.component';

                @NgModule({ declarations: [OrderListComponent] })
                export class OrdersModule {}
                """,
                List.of(
                    "import { NgModule } from '@angular/core';",
                    "import { OrderListComponent } from './order-list.component';"
                ),
                List.of(
                    classDeclaration("""
                        @NgModule({ declarations: [OrderListComponent] })
                        export class OrdersModule {}
                        """, "OrdersModule", List.of("@NgModule({ declarations: [OrderListComponent] })"), null, List.of(), List.of())
                )
            )
        ));

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertTrue(typeDependencies.stream().anyMatch(dep ->
            "OrderListComponent".equals(dep.get("sourceTypeName"))
                && "OrderService".equals(dep.get("targetTypeName"))
                && ((List<?>) dep.get("dependencySources")).contains("field")
                && "internal".equals(dep.get("targetBoundary"))
        ));

        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertTrue(
            evidenceDependencies.stream().anyMatch(dep -> "@angular/core".equals(dep.get("targetName")))
                || document.entities().stream().anyMatch(entity -> "@angular/core".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED),
            () -> "Expected @angular/core to appear as external evidence or inferred external entity. evidenceDependencies="
                + evidenceDependencies + ", entities=" + document.entities()
        );

        assertTrue(document.entities().stream().anyMatch(entity ->
            entity.kind() == EntityKind.UI_MODULE
                && "OrderListComponent".equals(entity.name())
                && "angular-component".equals(entity.metadata().get("uiProfile"))
        ));
        assertTrue(document.entities().stream().anyMatch(entity ->
            entity.kind() == EntityKind.UI_MODULE
                && "OrdersModule".equals(entity.name())
                && "angular-module".equals(entity.metadata().get("uiProfile"))
        ));
        assertTrue(document.entities().stream().anyMatch(entity ->
            entity.kind() == EntityKind.SERVICE
                && "OrderService".equals(entity.name())
        ));

        ArchitectureEntity dto = entity(document, EntityKind.INTERFACE, "OrderDto");
        ArchitectureEntity service = entity(document, EntityKind.CLASS, "OrderService");
        assertEquals("interface", dto.metadata().get("declarationKind"));
        assertTrue(
            service.metadata().get("declarationKind") == null || "class".equals(service.metadata().get("declarationKind")),
            () -> "Expected OrderService declarationKind to be null or class. service=" + service
        );
    }

    @Test
    void frontendFrameworkRelationshipsFlowIntoTypeAndModuleRollups() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            tsFile(
                "src/app/context/AuthContext.tsx",
                "typescript",
                "react",
                """
                import React, { createContext } from 'react';
                export const AuthContext = createContext(null);
                export function AuthProvider() { return <AuthContext.Provider value={{}}><section /></AuthContext.Provider>; }
                """,
                List.of("import React, { createContext } from 'react';"),
                List.of(
                    functionDeclaration("export function AuthProvider() { return <AuthContext.Provider value={{}}><section /></AuthContext.Provider>; }", "AuthProvider")
                )
            ),
            tsFile(
                "src/app/hooks/useOrdersQuery.ts",
                "typescript",
                "react",
                """
                export function useOrdersQuery() { return null; }
                """,
                List.of(),
                List.of(functionDeclaration("export function useOrdersQuery() { return null; }", "useOrdersQuery"))
            ),
            tsFile(
                "src/app/pages/OrdersPage.tsx",
                "typescript",
                "react",
                """
                import React, { useContext } from 'react';
                import { Route } from 'react-router-dom';
                import { AuthContext } from '../context/AuthContext';
                import { useOrdersQuery } from '../hooks/useOrdersQuery';

                export function OrdersPage() {
                  const auth = useContext(AuthContext);
                  useOrdersQuery();
                  return <main><Route path="orders" element={<OrdersPage />} /></main>;
                }
                """,
                List.of(
                    "import React, { useContext } from 'react';",
                    "import { Route } from 'react-router-dom';",
                    "import { AuthContext } from '../context/AuthContext';",
                    "import { useOrdersQuery } from '../hooks/useOrdersQuery';"
                ),
                List.of(functionDeclaration("""
                    export function OrdersPage() {
                      const auth = useContext(AuthContext);
                      useOrdersQuery();
                      return <main><Route path="orders" element={<OrdersPage />} /></main>;
                    }
                    """, "OrdersPage"))
            ),
            tsFile(
                "src/app/orders/orders.component.ts",
                "typescript",
                "angular",
                """
                import { Component, Inject } from '@angular/core';
                import { ORDER_API } from './orders.tokens';

                @Component({ selector: 'app-orders' })
                export class OrdersComponent {
                  constructor(@Inject(ORDER_API) api: OrdersApi) {}
                }
                """,
                List.of(
                    "import { Component, Inject } from '@angular/core';",
                    "import { ORDER_API } from './orders.tokens';"
                ),
                List.of(classDeclaration("""
                    @Component({ selector: 'app-orders' })
                    export class OrdersComponent {
                      constructor(@Inject(ORDER_API) api: OrdersApi) {}
                    }
                    """, "OrdersComponent", List.of("@Component({ selector: 'app-orders' })"), null, List.of(), List.of(
                    methodDefinition("constructor(@Inject(ORDER_API) api: OrdersApi) {}", "constructor", null)
                )))
            ),
            tsFile(
                "src/app/orders/orders.module.ts",
                "typescript",
                "angular",
                """
                import { NgModule } from '@angular/core';
                import { OrdersComponent } from './orders.component';
                import { OrdersApi } from './orders.api';
                import { ORDER_API } from './orders.tokens';

                @NgModule({ declarations: [OrdersComponent], providers: [{ provide: ORDER_API, useClass: OrdersApi }] })
                export class OrdersModule {}
                """,
                List.of(
                    "import { NgModule } from '@angular/core';",
                    "import { OrdersComponent } from './orders.component';",
                    "import { OrdersApi } from './orders.api';",
                    "import { ORDER_API } from './orders.tokens';"
                ),
                List.of(classDeclaration("""
                    @NgModule({ declarations: [OrdersComponent], providers: [{ provide: ORDER_API, useClass: OrdersApi }] })
                    export class OrdersModule {}
                    """, "OrdersModule", List.of("@NgModule({ declarations: [OrdersComponent], providers: [{ provide: ORDER_API, useClass: OrdersApi }] })"), null, List.of(), List.of()))
            )
        ));

        Map<String, Object> dependencyViews = dependencyViews(document);
        List<Map<String, Object>> frameworkTypeDependencies = dependencyViewList(document, "frameworkTypeDependencies");
        List<Map<String, Object>> frameworkModuleDependencies = dependencyViewList(document, "frameworkModuleDependencies");
        List<Map<String, Object>> compositionTypeDependencies = dependencyViewList(document, "compositionTypeDependencies");
        List<Map<String, Object>> routeTypeDependencies = dependencyViewList(document, "routeTypeDependencies");
        List<Map<String, Object>> providerTypeDependencies = dependencyViewList(document, "providerTypeDependencies");
        List<Map<String, Object>> hookTypeDependencies = dependencyViewList(document, "hookTypeDependencies");

        assertTrue(!frameworkTypeDependencies.isEmpty(), () -> "Expected framework-aware type dependencies. dependencyViews=" + dependencyViews);
        assertTrue(!frameworkModuleDependencies.isEmpty(), () -> "Expected framework-aware module dependencies. dependencyViews=" + dependencyViews);
        assertTrue(compositionTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("renders")));
        assertTrue(routeTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("targets")));
        assertTrue(providerTypeDependencies.stream().anyMatch(dep -> {
            List<?> relationships = (List<?>) dep.get("frameworkRelationships");
            return relationships.contains("providesContext") || relationships.contains("consumesContext") || relationships.contains("injects") || relationships.contains("providedBy");
        }), () -> "Expected provider/DI dependencies. providerTypeDependencies=" + providerTypeDependencies);
        assertTrue(hookTypeDependencies.stream().anyMatch(dep -> ((List<?>) dep.get("frameworkRelationships")).contains("usesHook")));
        assertTrue(frameworkModuleDependencies.stream().anyMatch(dep -> {
            List<?> viewKinds = (List<?>) dep.get("architectureViewKinds");
            List<?> frameworks = (List<?>) dep.get("frameworks");
            return viewKinds.contains("framework") && (frameworks.contains("react") || frameworks.contains("angular"));
        }), () -> "Expected framework metadata on module rollups. frameworkModuleDependencies=" + frameworkModuleDependencies);

        @SuppressWarnings("unchecked")
        Map<String, Object> frontendArchitectureViews = (Map<String, Object>) dependencyViews.get("frontendArchitectureViews");
        assertEquals(List.of("frameworkTypeDependencies", "frameworkModuleDependencies"), frontendArchitectureViews.get("frameworkAware"));
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

    private static ArchitectureEntity entity(ArchitectureIndexDocument document, EntityKind kind, String name) {
        return document.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .sorted((left, right) -> {
                int originCompare = Boolean.compare(left.origin() == EntityOrigin.OBSERVED, right.origin() == EntityOrigin.OBSERVED);
                if (originCompare != 0) {
                    return -originCompare;
                }
                boolean leftHasDeclarationKind = left.metadata() != null && left.metadata().get("declarationKind") != null;
                boolean rightHasDeclarationKind = right.metadata() != null && right.metadata().get("declarationKind") != null;
                return -Boolean.compare(leftHasDeclarationKind, rightHasDeclarationKind);
            })
            .findFirst()
            .orElseThrow();
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

    private static SyntaxNode methodSignature(String snippet, String name, String returnType) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "property_identifier", name));
        children.add(localNode(normalized, "formal_parameters", between(normalized, '(', ')'), List.of()));
        children.add(localLeaf(normalized, "type_identifier", returnType));
        return localNode(normalized, "method_signature", normalized, children);
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
