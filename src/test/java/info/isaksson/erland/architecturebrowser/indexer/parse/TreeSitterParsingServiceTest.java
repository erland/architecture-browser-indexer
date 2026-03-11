package info.isaksson.erland.architecturebrowser.indexer.parse;

import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryScanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeSitterParsingServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesSupportedFilesViaRegistryAndExposesSyntaxTree() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/Demo.java"), "class Demo {}\n");
        Files.writeString(tempDir.resolve("src/app.ts"), "export const app = 1;\n");

        FileInventory inventory = new FileInventoryScanner().scan(tempDir);
        ParserRegistry registry = new ParserRegistry(List.of(
            new FakeSuccessParser(ParseLanguage.JAVA),
            new FakeSuccessParser(ParseLanguage.TYPESCRIPT)
        ));

        ParseBatchResult batchResult = new TreeSitterParsingService(registry).parseInventory(tempDir, inventory);

        assertEquals(2, batchResult.results().size());
        assertEquals(1, batchResult.attemptedByLanguage().get(ParseLanguage.JAVA));
        assertEquals(1, batchResult.attemptedByLanguage().get(ParseLanguage.TYPESCRIPT));
        assertEquals(2, batchResult.countsByStatus().get(ParseStatus.SUCCESS));
        assertTrue(batchResult.results().stream().allMatch(SourceParseResult::successful));
        assertTrue(batchResult.results().stream().allMatch(result -> result.syntaxTree().root().nodeCount() >= 2));
    }

    @Test
    void parseErrorsBecomeResultIssuesInsteadOfCrashes() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/Broken.java"), "class Broken {\n");

        FileInventory inventory = new FileInventoryScanner().scan(tempDir);
        ParserRegistry registry = new ParserRegistry(List.of(new FakeErrorParser(ParseLanguage.JAVA)));

        ParseBatchResult batchResult = new TreeSitterParsingService(registry).parseInventory(tempDir, inventory);

        assertEquals(1, batchResult.results().size());
        SourceParseResult result = batchResult.results().get(0);
        assertEquals(ParseStatus.PARSE_ERROR, result.status());
        assertFalse(result.successful());
        assertEquals("parse.syntax.error", result.issues().get(0).code());
    }

    @Test
    void defaultRegistryGracefullyReportsBackendUnavailableWhenDisabledByConfiguration() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.writeString(tempDir.resolve("src/Demo.java"), "class Demo {}\n");

        String previous = System.getProperty("archbrowser.treesitter.enabled");
        System.setProperty("archbrowser.treesitter.enabled", "false");
        try {
            FileInventory inventory = new FileInventoryScanner().scan(tempDir);
            ParseBatchResult batchResult = new TreeSitterParsingService(
                TreeSitterParserRegistryFactory.createDefaultRegistry(TreeSitterConfiguration.fromEnvironment())).parseInventory(tempDir, inventory);

            assertEquals(1, batchResult.results().size());
            SourceParseResult result = batchResult.results().get(0);
            assertEquals(ParseStatus.BACKEND_UNAVAILABLE, result.status());
            assertTrue(result.issues().get(0).message().contains("disabled by configuration"));
        } finally {
            if (previous == null) {
                System.clearProperty("archbrowser.treesitter.enabled");
            } else {
                System.setProperty("archbrowser.treesitter.enabled", previous);
            }
        }
    }

    private static final class FakeSuccessParser implements SourceParser {
        private final ParseLanguage language;

        private FakeSuccessParser(ParseLanguage language) {
            this.language = language;
        }

        @Override
        public ParseLanguage language() {
            return language;
        }

        @Override
        public SourceParseResult parse(SourceParseRequest request) {
            SyntaxNode leaf = new SyntaxNode("identifier", true, 0, 4, 0, 0, 0, 4, false, false, "Demo", List.of());
            SyntaxNode root = new SyntaxNode("program", true, 0, request.sourceText().length(), 0, 0, 0, request.sourceText().length(), false, false, null, List.of(leaf));
            return new SourceParseResult(
                request,
                ParseStatus.SUCCESS,
                new SyntaxTree(language, "fake-tree-sitter", root, false, root.nodeCount()),
                List.of(),
                Map.of("parserBackend", "fake-tree-sitter")
            );
        }
    }

    private static final class FakeErrorParser implements SourceParser {
        private final ParseLanguage language;

        private FakeErrorParser(ParseLanguage language) {
            this.language = language;
        }

        @Override
        public ParseLanguage language() {
            return language;
        }

        @Override
        public SourceParseResult parse(SourceParseRequest request) {
            return new SourceParseResult(
                request,
                ParseStatus.PARSE_ERROR,
                null,
                List.of(new ParseIssue("parse.syntax.error", "Encountered syntax error", 1, 1, false)),
                Map.of()
            );
        }
    }
}
