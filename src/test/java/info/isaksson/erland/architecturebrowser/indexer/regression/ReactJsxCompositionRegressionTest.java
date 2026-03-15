package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReactJsxCompositionRegressionTest {

    @Test
    void reactTsxCompositionAddsExplicitRenderRelationships() {
        String source = """
            export function OrdersPage() {
              return <AppShell><Header /><OrdersTable /><SummaryPanel /></AppShell>;
            }

            export function AppShell() {
              return <section><Toolbar /></section>;
            }

            export function Header() { return <header />; }
            export function OrdersTable() { return <table />; }
            export class SummaryPanel {}
            export function Toolbar() { return <div />; }
            """;

        SyntaxNode ordersPage = functionDeclaration(
            """
            export function OrdersPage() {
              return <AppShell><Header /><OrdersTable /><SummaryPanel /></AppShell>;
            }
            """.strip(),
            "OrdersPage"
        );
        SyntaxNode appShell = functionDeclaration(
            """
            export function AppShell() {
              return <section><Toolbar /></section>;
            }
            """.strip(),
            "AppShell"
        );
        SyntaxNode header = functionDeclaration("export function Header() { return <header />; }", "Header");
        SyntaxNode ordersTable = functionDeclaration("export function OrdersTable() { return <table />; }", "OrdersTable");
        SyntaxNode summaryPanel = classDeclaration("export class SummaryPanel {}", "SummaryPanel");
        SyntaxNode toolbar = functionDeclaration("export function Toolbar() { return <div />; }", "Toolbar");

        StructuralExtractionResult result = extract(
            "src/pages/OrdersPage.tsx",
            source,
            program(source, ordersPage, appShell, header, ordersTable, summaryPanel, toolbar)
        );

        assertReactRelationship(result, "OrdersPage", EntityKind.FUNCTION, "AppShell", EntityKind.FUNCTION, true);
        assertReactRelationship(result, "OrdersPage", EntityKind.FUNCTION, "Header", EntityKind.FUNCTION, true);
        assertReactRelationship(result, "OrdersPage", EntityKind.FUNCTION, "OrdersTable", EntityKind.FUNCTION, true);
        assertReactRelationship(result, "OrdersPage", EntityKind.FUNCTION, "SummaryPanel", EntityKind.CLASS, true);
        assertReactRelationship(result, "AppShell", EntityKind.FUNCTION, "Toolbar", EntityKind.FUNCTION, true);

        assertFalse(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && "OrdersPage".equals(rel.label())
            && "OrdersPage".equals(rel.label())));
    }

    private static void assertReactRelationship(
        StructuralExtractionResult result,
        String fromName,
        EntityKind fromKind,
        String toName,
        EntityKind toKind,
        boolean resolved
    ) {
        var fromEntity = entity(result, fromKind, fromName);
        var toEntity = entity(result, toKind, toName);
        var relationship = result.relationships().stream()
            .filter(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
                && fromEntity.id().equals(rel.fromEntityId())
                && toEntity.id().equals(rel.toEntityId())
                && toName.equals(rel.label())
                && "react".equals(rel.metadata().get("framework"))
                && "renders".equals(rel.metadata().get("frameworkRelationship")))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship);
        assertEquals("react:jsx-renders", relationship.metadata().get("dependencySource"));
        assertEquals(resolved, relationship.metadata().get("resolvedFromJsxComposition"));
    }

    private static info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact entity(
        StructuralExtractionResult result,
        EntityKind kind,
        String name
    ) {
        return result.entities().stream()
            .filter(entity -> entity.kind() == kind && name.equals(entity.name()))
            .findFirst()
            .orElseThrow();
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

    private static SyntaxNode functionDeclaration(String snippet, String name) {
        int nameIndex = Math.max(0, snippet.indexOf(name));
        return new SyntaxNode("function_declaration", true, 0, snippet.length(), 0, 0, 0, snippet.length(), false, false,
            snippet, List.of(
                new SyntaxNode("identifier", true, nameIndex, nameIndex + name.length(), 0, nameIndex, 0, nameIndex + name.length(), false, false, name, List.of())
            ));
    }

    private static SyntaxNode classDeclaration(String snippet, String name) {
        int nameIndex = Math.max(0, snippet.indexOf(name));
        return new SyntaxNode("class_declaration", true, 0, snippet.length(), 0, 0, 0, snippet.length(), false, false,
            snippet, List.of(
                new SyntaxNode("type_identifier", true, nameIndex, nameIndex + name.length(), 0, nameIndex, 0, nameIndex + name.length(), false, false, name, List.of())
            ));
    }
}
