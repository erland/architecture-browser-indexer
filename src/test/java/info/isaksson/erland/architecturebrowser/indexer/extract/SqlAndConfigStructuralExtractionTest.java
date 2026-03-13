package info.isaksson.erland.architecturebrowser.indexer.extract;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlAndConfigStructuralExtractionTest {
    @Test
    void extractsSqlTablesAndDependenciesFromSyntaxTree() {
        String source = """
            CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT);
            SELECT * FROM orders JOIN customers ON customers.id = orders.customer_id;
            """;

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 1, 80, false, false, source, List.of(
            new SyntaxNode("create_table_statement", true, 0, 58, 0, 0, 0, 58, false, false,
                "CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT);", List.of()),
            new SyntaxNode("select_statement", true, 59, source.length(), 1, 0, 1, 80, false, false,
                "SELECT * FROM orders JOIN customers ON customers.id = orders.customer_id;", List.of())
        ));

        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("db/schema.sql"), "db/schema.sql", ParseLanguage.SQL, source),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.SQL, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.SQL, 1), Map.of(ParseStatus.SUCCESS, 1)));

        assertEquals(1, result.summary().extractedByLanguage().get("sql"));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.DATASTORE && "orders".equals(entity.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && "customers".equals(rel.label())));
    }

    @Test
    void extractsConfigEntriesAndExternalTargetsFromJsonAndPropertiesSyntaxTrees() {
        String jsonSource = """
            { "baseUrl": "https://api.example.org/v1", "name": "demo" }
            """;
        SyntaxNode jsonRoot = new SyntaxNode("document", true, 0, jsonSource.length(), 0, 0, 0, 58, false, false, jsonSource, List.of(
            new SyntaxNode("pair", true, 2, 40, 0, 2, 0, 40, false, false, "\"baseUrl\": \"https://api.example.org/v1\"", List.of()),
            new SyntaxNode("pair", true, 42, 56, 0, 42, 0, 56, false, false, "\"name\": \"demo\"", List.of())
        ));
        SourceParseResult jsonResult = new SourceParseResult(
            new SourceParseRequest(Path.of("config/app.json"), "config/app.json", ParseLanguage.JSON, jsonSource),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.JSON, "tree-sitter-jtreesitter", jsonRoot, false, jsonRoot.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        String propertiesSource = """
            spring.datasource.url=jdbc:postgresql://localhost:5432/app
            server.port=8080
            """;
        SyntaxNode propertiesRoot = new SyntaxNode("document", true, 0, propertiesSource.length(), 0, 0, 1, 16, false, false, propertiesSource, List.of(
            new SyntaxNode("property", true, 0, 57, 0, 0, 0, 57, false, false, "spring.datasource.url=jdbc:postgresql://localhost:5432/app", List.of()),
            new SyntaxNode("property", true, 58, propertiesSource.length(), 1, 0, 1, 16, false, false, "server.port=8080", List.of())
        ));
        SourceParseResult propertiesResult = new SourceParseResult(
            new SourceParseRequest(Path.of("config/application.properties"), "config/application.properties", ParseLanguage.PROPERTIES, propertiesSource),
            ParseStatus.SUCCESS,
            new SyntaxTree(ParseLanguage.PROPERTIES, "tree-sitter-jtreesitter", propertiesRoot, false, propertiesRoot.nodeCount()),
            List.of(),
            Map.of("parserBackend", "tree-sitter-jtreesitter")
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(
                List.of(jsonResult, propertiesResult),
                Map.of(ParseLanguage.JSON, 1, ParseLanguage.PROPERTIES, 1),
                Map.of(ParseStatus.SUCCESS, 2)
            ));

        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.CONFIG_ARTIFACT && "baseUrl".equals(entity.name()) && "baseUrl".equals(entity.displayName())));
        assertTrue(result.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.DATASTORE && "spring.datasource.url".equals(entity.name())));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && rel.label().contains("https://api.example.org")));
        assertTrue(result.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON && rel.label().contains("jdbc:postgresql://localhost:5432/app")));
    }

    @Test
    void emitsDiagnosticForConfigLanguageWhenSyntaxTreeUnavailable() {
        String source = "service.url: https://example.org";
        SourceParseResult parseResult = new SourceParseResult(
            new SourceParseRequest(Path.of("config/app.yaml"), "config/app.yaml", ParseLanguage.YAML, source),
            ParseStatus.BACKEND_UNAVAILABLE,
            null,
            List.of(),
            Map.of()
        );

        StructuralExtractionResult result = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(new ParseBatchResult(List.of(parseResult), Map.of(ParseLanguage.YAML, 1), Map.of(ParseStatus.BACKEND_UNAVAILABLE, 1)));

        assertEquals(0, result.summary().extractedByLanguage().getOrDefault("yaml", 0));
        assertTrue(result.diagnostics().stream().anyMatch(d -> "extract.yaml.syntax-tree-required".equals(d.code())));
    }
}
