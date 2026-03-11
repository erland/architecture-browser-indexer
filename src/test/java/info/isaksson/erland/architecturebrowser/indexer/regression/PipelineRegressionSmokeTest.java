package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.incremental.FileFingerprintService;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalDiffService;
import info.isaksson.erland.architecturebrowser.indexer.incremental.IncrementalPlanner;
import info.isaksson.erland.architecturebrowser.indexer.incremental.model.IncrementalSnapshot;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationRegistry;
import info.isaksson.erland.architecturebrowser.indexer.interpret.InterpretationService;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrFactory;
import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
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
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineRegressionSmokeTest {

    @Test
    void endToEndDocumentStillContainsExpectedSignalsAcrossStages() {
        String javaPath = "src/main/java/com/example/demo/DemoController.java";
        String tsPath = "src/main/ts/app/main.ts";

        FileInventory inventory = new FileInventory(
            List.of(
                new FileInventoryEntry(javaPath, 120, "java", "source", "java", false, List.of("spring")),
                new FileInventoryEntry(tsPath, 80, "ts", "source", "typescript", false, List.of("react"))
            ),
            2, 2, 0, Set.of("java", "typescript"), Set.of("spring", "react")
        );

        String javaSource = """
            package com.example.demo;
            import com.example.shared.CustomerRepository;
            @RestController
            public class DemoController {
              @GetMapping(\"/orders\")
              public String getOrders() { return \"ok\"; }
            }
            """;
        SyntaxNode javaRoot = new SyntaxNode("program", true, 0, javaSource.length(), 0, 0, 5, 0, false, false, javaSource, List.of(
            new SyntaxNode("package_declaration", true, 0, 25, 0, 0, 0, 25, false, false, "package com.example.demo;", List.of(
                new SyntaxNode("scoped_identifier", true, 8, 24, 0, 8, 0, 24, false, false, "com.example.demo", List.of())
            )),
            new SyntaxNode("import_declaration", true, 26, 70, 1, 0, 1, 44, false, false, "import com.example.shared.CustomerRepository;", List.of()),
            new SyntaxNode("class_declaration", true, 71, javaSource.length(), 2, 0, 5, 0, false, false, "@RestController public class DemoController {}", List.of(
                new SyntaxNode("marker_annotation", true, 71, 86, 2, 0, 2, 15, false, false, "@RestController", List.of()),
                new SyntaxNode("identifier", true, 100, 114, 3, 13, 3, 27, false, false, "DemoController", List.of()),
                new SyntaxNode("method_declaration", true, 118, javaSource.length()-2, 4, 2, 5, 0, false, false, "@GetMapping(\"/orders\") public String getOrders() { return \"ok\"; }", List.of(
                    new SyntaxNode("marker_annotation", true, 118, 140, 4, 2, 4, 24, false, false, "@GetMapping(\"/orders\")", List.of()),
                    new SyntaxNode("identifier", true, 155, 164, 5, 16, 5, 25, false, false, "getOrders", List.of()),
                    new SyntaxNode("formal_parameters", true, 164, 166, 5, 25, 5, 27, false, false, "()", List.of())
                ))
            ))
        ));

        String tsSource = """
            @Component
            export class AppComponent {}
            export function bootstrapApplication() { return 1; }
            """;
        SyntaxNode tsRoot = new SyntaxNode("program", true, 0, tsSource.length(), 0, 0, 2, 0, false, false, tsSource, List.of(
            new SyntaxNode("class_declaration", true, 0, 37, 0, 0, 1, 0, false, false, "@Component export class AppComponent {}", List.of(
                new SyntaxNode("decorator", true, 0, 10, 0, 0, 0, 10, false, false, "@Component", List.of()),
                new SyntaxNode("type_identifier", true, 24, 36, 1, 0, 1, 12, false, false, "AppComponent", List.of())
            )),
            new SyntaxNode("function_declaration", true, 38, tsSource.length(), 2, 0, 2, 56, false, false, "export function bootstrapApplication() { return 1; }", List.of(
                new SyntaxNode("identifier", true, 54, 74, 2, 16, 2, 36, false, false, "bootstrapApplication", List.of())
            ))
        ));

        ParseBatchResult parseBatchResult = new ParseBatchResult(
            List.of(
                new SourceParseResult(
                    new SourceParseRequest(Path.of(javaPath), javaPath, ParseLanguage.JAVA, javaSource),
                    ParseStatus.SUCCESS,
                    new SyntaxTree(ParseLanguage.JAVA, "tree-sitter-jtreesitter", javaRoot, false, javaRoot.nodeCount()),
                    List.of(),
                    Map.of("parserBackend", "tree-sitter-jtreesitter")
                ),
                new SourceParseResult(
                    new SourceParseRequest(Path.of(tsPath), tsPath, ParseLanguage.TYPESCRIPT, tsSource),
                    ParseStatus.SUCCESS,
                    new SyntaxTree(ParseLanguage.TYPESCRIPT, "tree-sitter-jtreesitter", tsRoot, false, tsRoot.nodeCount()),
                    List.of(),
                    Map.of("parserBackend", "tree-sitter-jtreesitter")
                )
            ),
            Map.of(ParseLanguage.JAVA, 1, ParseLanguage.TYPESCRIPT, 1),
            Map.of(ParseStatus.SUCCESS, 2)
        );

        var extraction = new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(parseBatchResult);
        var interpretation = new InterpretationService(InterpretationRegistry.defaultRegistry()).interpret(extraction);
        var topology = new TopologyService().infer(inventory, extraction, interpretation);

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("demo", "/tmp/demo", Instant.parse("2026-03-11T14:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            parseBatchResult,
            extraction,
            interpretation,
            topology
        );

        var validation = ArchitectureIrValidator.validate(document);
        assertTrue(validation.isValid());
        assertTrue(document.entities().stream().anyMatch(e -> e.kind() == EntityKind.ENDPOINT));
        assertTrue(document.entities().stream().anyMatch(e -> e.kind() == EntityKind.UI_MODULE));
        assertTrue(document.relationships().stream().anyMatch(r -> r.kind() == RelationshipKind.EXPOSES));
        assertTrue(document.metadata().containsKey("topologySummary"));

        FileFingerprintService fingerprintService = new FileFingerprintService();
        IncrementalSnapshot snapshot = fingerprintService.createSnapshot(inventory);
        var diff = new IncrementalDiffService().diff(snapshot, snapshot);
        var plan = new IncrementalPlanner().plan(diff);
        assertEquals(false, plan.incremental());
    }
}
