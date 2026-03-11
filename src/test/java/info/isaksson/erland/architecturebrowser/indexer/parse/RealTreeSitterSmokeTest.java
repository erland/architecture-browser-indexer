package info.isaksson.erland.architecturebrowser.indexer.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventoryScanner;
import info.isaksson.erland.architecturebrowser.indexer.scan.InventoryScanOptions;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RealTreeSitterSmokeTest {

    @Test
    void parsesJavaAndTypeScriptWithRealTreeSitterAndReportsExtractionMode() throws Exception {
        Path repoRoot = Files.createTempDirectory("arch-browser-real-treesitter");
        Path javaFile = repoRoot.resolve("src/main/java/com/example/demo/DemoController.java");
        Path tsFile = repoRoot.resolve("src/main/ts/demo.ts");

        Files.createDirectories(javaFile.getParent());
        Files.createDirectories(tsFile.getParent());

        Files.writeString(javaFile, """
                package com.example.demo;

                import java.util.List;

                public class DemoController {
                    public String hello() {
                        return "hi";
                    }
                }
                """);

        Files.writeString(tsFile, """
                import { httpGet } from "./http";

                export class DemoApi {
                  hello(): string {
                    return "hi";
                  }
                }

                export function greet(name: string): string {
                  return `Hello ${name}`;
                }
                """);

        FileInventory inventory = new FileInventoryScanner().scan(repoRoot, InventoryScanOptions.defaults());

        TreeSitterConfiguration configuration = TreeSitterConfiguration.fromEnvironment();
        ParserRegistry registry = TreeSitterParserRegistryFactory.createDefaultRegistry(configuration);
        ParseBatchResult parseBatch = new TreeSitterParsingService(registry).parseInventory(repoRoot, inventory);

        SourceParseResult javaResult = parseBatch.results().stream()
                .filter(result -> result.request().relativePath().endsWith("DemoController.java"))
                .findFirst()
                .orElseThrow();

        SourceParseResult tsResult = parseBatch.results().stream()
                .filter(result -> result.request().relativePath().endsWith("demo.ts"))
                .findFirst()
                .orElseThrow();

        assertRealTreeSitterSuccess(javaResult);
        assertRealTreeSitterSuccess(tsResult);

        StructuralExtractionResult extractionResult =
                new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry())
                        .extract(parseBatch);

        assertFalse(extractionResult.entities().isEmpty(), "Expected extracted entities from smoke test inputs.");

        Set<String> entityNames = extractionResult.entities().stream()
                .map(entity -> entity.name())
                .collect(Collectors.toSet());

        assertTrue(entityNames.contains("DemoController"));
        assertTrue(entityNames.contains("DemoApi"));

        assertNotNull(extractionResult.summary());
        assertFalse(
                extractionResult.summary().extractedByMode().isEmpty(),
                "Expected at least one reported extraction mode.");

        Set<String> extractionModes = extractionResult.entities().stream()
                .map(entity -> entity.metadata().get("extractionMode"))
                .filter(value -> value != null)
                .map(String::valueOf)
                .collect(Collectors.toSet());

        assertFalse(extractionModes.isEmpty(), "Expected extractionMode metadata on extracted entities.");
        assertTrue(
                extractionModes.contains(ExtractionMode.SOURCE_TEXT_FALLBACK.name())
                        || extractionModes.contains(ExtractionMode.SYNTAX_TREE.name()),
                "Expected extractionMode to be SOURCE_TEXT_FALLBACK or SYNTAX_TREE but was: " + extractionModes);
    }

    private static void assertRealTreeSitterSuccess(SourceParseResult result) {
        assertEquals(
                ParseStatus.SUCCESS,
                result.status(),
                "Expected real Tree-sitter parse success for "
                        + result.request().relativePath()
                        + " but got "
                        + result.status()
                        + " with metadata "
                        + result.metadata()
                        + " and issues "
                        + result.issues()
                        + ". If the native grammar libraries are missing, run scripts/setup-treesitter-macos-aarch64.sh.");

        assertNotNull(result.syntaxTree(), "Expected syntax tree for " + result.request().relativePath());

        Map<String, Object> metadata = result.metadata();
        assertEquals(
                "tree-sitter-jtreesitter",
                String.valueOf(metadata.get("parserBackend")),
                "Expected parserBackend metadata to identify real Tree-sitter backend. Metadata: " + metadata);
    }
}
