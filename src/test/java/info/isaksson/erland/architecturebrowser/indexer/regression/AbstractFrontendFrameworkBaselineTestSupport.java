package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
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

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractFrontendFrameworkBaselineTestSupport {
    protected static ArchitectureIndexDocument buildRoleInterpretationDocument() {
        return buildDocument(List.of(
            tsFile(
                "src/app/orders/order.service.ts",
                "typescript",
                "angular",
                """
                import { Injectable } from '@angular/core';

                @Injectable()
                export class OrderService {}
                """,
                List.of("import { Injectable } from '@angular/core';"),
                List.of(classDeclaration("""
                        @Injectable()
                        export class OrderService {}
                        """, "OrderService", List.of("@Injectable()"), List.of()))
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
                List.of("import { Component } from '@angular/core';", "import { OrderService } from './order.service';"),
                List.of(classDeclaration("""
                        @Component({ selector: 'app-order-list' })
                        export class OrderListComponent {
                          service: OrderService;
                        }
                        """, "OrderListComponent", List.of("@Component({ selector: 'app-order-list' })"), List.of(publicField("service: OrderService;", "service"))))
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
                List.of("import { NgModule } from '@angular/core';", "import { OrderListComponent } from './order-list.component';"),
                List.of(classDeclaration("""
                        @NgModule({ declarations: [OrderListComponent] })
                        export class OrdersModule {}
                        """, "OrdersModule", List.of("@NgModule({ declarations: [OrderListComponent] })"), List.of()))
            ),
            tsFile("src/components/UserCard.tsx", "typescript", "react", """
                import React from 'react';

                export function UserCard() { return <div />; }
                """, List.of("import React from 'react';"), List.of(functionDeclaration("export function UserCard() { return <div />; }", "UserCard"))),
            tsFile("src/context/AuthProvider.tsx", "typescript", "react", """
                import React from 'react';

                export function AuthProvider() {
                  const value = React.createContext(null);
                  return <AuthContext.Provider value={value} />;
                }
                """, List.of("import React from 'react';"), List.of(functionDeclaration("""
                    export function AuthProvider() {
                      const value = React.createContext(null);
                      return <AuthContext.Provider value={value} />;
                    }
                    """, "AuthProvider"))),
            tsFile("src/pages/OrdersPage.tsx", "typescript", "react", """
                import React from 'react';

                export function OrdersPage() { return <main />; }
                """, List.of("import React from 'react';"), List.of(functionDeclaration("export function OrdersPage() { return <main />; }", "OrdersPage")))
        ));
    }

    protected static ArchitectureIndexDocument buildDependencyViewsDocument() {
        return buildDocument(List.of(
            tsFile("src/services/core/OrderService.ts", "typescript", "react", """
                export class OrderService {
                  loadOrder() { return '1'; }
                }
                """, List.of(), List.of(classDeclaration("""
                    export class OrderService {
                      loadOrder() { return '1'; }
                    }
                    """, "OrderService", List.of(), List.of()))),
            tsFile("src/state/orders/OrdersStore.ts", "typescript", "react", """
                import { OrderService } from '../../services/core/OrderService';

                export class OrdersStore {
                  service: OrderService;
                }
                """, List.of("import { OrderService } from '../../services/core/OrderService';"), List.of(classDeclaration("""
                    export class OrdersStore {
                      service: OrderService;
                    }
                    """, "OrdersStore", List.of(), List.of(publicField("service: OrderService;", "service"))))),
            tsFile("src/pages/OrdersPage.tsx", "typescript", "react", """
                import React from 'react';
                import { OrdersStore } from '../state/orders/OrdersStore';

                export function OrdersPage(store: OrdersStore) { return <main />; }
                """, List.of("import React from 'react';", "import { OrdersStore } from '../state/orders/OrdersStore';"), List.of(functionDeclaration("export function OrdersPage(store: OrdersStore) { return <main />; }", "OrdersPage"))),
            tsFile("src/app/orders/order-list.component.ts", "typescript", "angular", """
                import { Component } from '@angular/core';
                import { OrderService } from '../../services/core/OrderService';

                @Component({ selector: 'app-order-list' })
                export class OrderListComponent {
                  service: OrderService;
                }
                """, List.of("import { Component } from '@angular/core';", "import { OrderService } from '../../services/core/OrderService';"), List.of(classDeclaration("""
                    @Component({ selector: 'app-order-list' })
                    export class OrderListComponent {
                      service: OrderService;
                    }
                    """, "OrderListComponent", List.of("@Component({ selector: 'app-order-list' })"), List.of(publicField("service: OrderService;", "service")))))
        ));
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
    protected static List<Map<String, Object>> dependencyViewList(ArchitectureIndexDocument document, String key) {
        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        return (List<Map<String, Object>>) dependencyViews.get(key);
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

    protected static SyntaxNode program(String source, SyntaxNode... children) {
        int[] end = lineAndColumn(source, source.length());
        return new SyntaxNode("program", true, 0, source.length(), 0, 0, end[0], end[1], false, false, source, List.of(children));
    }

    private static SyntaxNode importStatement(String source, String snippet) {
        return node("import_statement", source, snippet, List.of());
    }

    protected static SyntaxNode classDeclaration(String snippet, String name, List<String> decorators, List<SyntaxNode> members) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        decorators.forEach(decorator -> children.add(localNode(normalized, "decorator", decorator, List.of())));
        children.add(localLeaf(normalized, "type_identifier", name));
        children.addAll(members);
        return localNode(normalized, "class_declaration", normalized, children);
    }

    private static SyntaxNode functionDeclaration(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = new ArrayList<>();
        children.add(localLeaf(normalized, "identifier", name));
        children.add(localNode(normalized, "formal_parameters", between(normalized, '(', ')'), List.of()));
        return localNode(normalized, "function_declaration", normalized, children);
    }

    private static SyntaxNode publicField(String snippet, String name) {
        String normalized = normalize(snippet);
        List<SyntaxNode> children = List.of(
            localLeaf(normalized, "property_identifier", name),
            localLeaf(normalized, "type_identifier", declaredType(normalized))
        );
        return localNode(normalized, "public_field_definition", normalized, children);
    }

    private static SyntaxNode node(String type, String source, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) {
            throw new IllegalArgumentException("Snippet not found: " + snippet);
        }
        int end = start + snippet.length();
        int[] startPos = lineAndColumn(source, start);
        int[] endPos = lineAndColumn(source, end);
        return new SyntaxNode(type, true, start, end, startPos[0], startPos[1], endPos[0], endPos[1], false, false, snippet, children);
    }

    private static SyntaxNode localNode(String source, String type, String snippet, List<SyntaxNode> children) {
        int start = source.indexOf(snippet);
        if (start < 0) {
            throw new IllegalArgumentException("Snippet not found in local source: " + snippet);
        }
        int end = start + snippet.length();
        int[] startPos = lineAndColumn(source, start);
        int[] endPos = lineAndColumn(source, end);
        return new SyntaxNode(type, true, start, end, startPos[0], startPos[1], endPos[0], endPos[1], false, false, snippet, children);
    }

    private static SyntaxNode localLeaf(String source, String type, String snippet) {
        return localNode(source, type, snippet, List.of());
    }

    private static String declaredType(String snippet) {
        int colon = snippet.indexOf(':');
        if (colon < 0) {
            return "unknown";
        }
        int end = snippet.indexOf(';', colon);
        if (end < 0) {
            end = snippet.length();
        }
        return snippet.substring(colon + 1, end).trim();
    }

    private static String between(String value, char start, char end) {
        int startIndex = value.indexOf(start);
        int endIndex = value.lastIndexOf(end);
        if (startIndex < 0 || endIndex < 0 || endIndex < startIndex) {
            return "()";
        }
        return value.substring(startIndex, endIndex + 1);
    }

    private static int[] lineAndColumn(String text, int offset) {
        int line = 0;
        int column = 0;
        for (int index = 0; index < offset && index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }

    private static String normalize(String source) {
        return source.stripIndent().strip();
    }

    private static String extension(String path) {
        int index = path.lastIndexOf('.');
        return index >= 0 ? path.substring(index + 1) : "";
    }

    private record TsFixtureFile(String path, String source, SyntaxNode root, String language, Set<String> technologies) {
    }

}
