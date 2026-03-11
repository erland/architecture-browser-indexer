package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
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
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrFactoryStructuralExtractionTest {
    @Test
    void includesStructuralExtractionEntitiesRelationshipsAndSummaryInDocument() {
        FileInventory inventory = new FileInventory(
            List.of(new FileInventoryEntry("src/app.ts", 10, "ts", "source", "typescript", false, List.of("typescript"))),
            1, 1, 0, Set.of("typescript"), Set.of("typescript")
        );
        String source = "import { x } from './lib';\nexport function run() { return x; }\n";

        SyntaxNode root = new SyntaxNode("program", true, 0, source.length(), 0, 0, 1, 35, false, false, source, List.of(
            new SyntaxNode("import_statement", true, 0, 26, 0, 0, 0, 26, false, false, "import { x } from './lib';", List.of()),
            new SyntaxNode("function_declaration", true, 27, source.length(), 1, 0, 1, 35, false, false,
                "export function run() { return x; }", List.of(
                    new SyntaxNode("identifier", true, 43, 46, 1, 16, 1, 19, false, false, "run", List.of())
                ))
        ));

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            List.of(new SourceParseResult(
                new SourceParseRequest(Path.of("src/app.ts"), "src/app.ts", ParseLanguage.TYPESCRIPT, source),
                ParseStatus.SUCCESS,
                new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", root, false, root.nodeCount()),
                List.of(),
                Map.of("parserBackend", "tree-sitter-jtreesitter"))),
            Map.of(ParseLanguage.TYPESCRIPT, 1),
            Map.of(ParseStatus.SUCCESS, 1)
        );
        StructuralExtractionResult extractionResult = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
            .extract(parseBatchResult);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-10T12:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extractionResult
        );

        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.FUNCTION && "run".equals(entity.name())));
        assertTrue(document.relationships().stream().anyMatch(relationship -> "./lib".equals(relationship.label())));
        assertEquals("SUCCESS", document.runMetadata().outcome().name());
        assertTrue(document.metadata().containsKey("extractionSummary"));
    }
}
