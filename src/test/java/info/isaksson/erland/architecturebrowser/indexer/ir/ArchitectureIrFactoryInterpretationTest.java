package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationSummary;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrFactoryInterpretationTest {

    @Test
    void includesInterpretationEntitiesRelationshipsAndSummaryInDocument() {
        FileInventory inventory = new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of());
        StructuralExtractionResult extraction = new StructuralExtractionResult(List.of(), List.of(), List.of(), List.of(), new info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary(0, 0, Map.of(), Map.of(), 0, 0));
        SourceReference ref = new SourceReference("src/main/java/com/example/DemoController.java", 12, 12, "@GetMapping(\"/orders\")", Map.of());

        InterpretationResult interpretation = new InterpretationResult(
            List.of(new InterpretedEntityFact(
                "entity:interpret:endpoint",
                EntityKind.ENDPOINT,
                EntityOrigin.INFERRED,
                "GET /orders",
                "DemoController endpoint GET /orders",
                "scope:file",
                List.of(ref),
                Map.of("httpMethod", "GET", "path", "/orders")
            )),
            List.of(new InterpretedRelationshipFact(
                "rel:interpret:exposes",
                RelationshipKind.EXPOSES,
                "entity:java:controller",
                "entity:interpret:endpoint",
                "GET /orders",
                List.of(ref),
                Map.of("ruleId", "java-backend-high-value")
            )),
            List.of(),
            new InterpretationSummary(Map.of("ENDPOINT", 1), Map.of("EXPOSES", 1), Map.of("java-backend-high-value", 2))
        );

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T10:00:00Z")),
            "0.1.0-SNAPSHOT",
            inventory,
            List.of(),
            new ParseBatchResult(List.of(), Map.of(), Map.of()),
            extraction,
            interpretation
        );

        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.ENDPOINT));
        assertTrue(document.relationships().stream().anyMatch(relationship -> relationship.kind() == RelationshipKind.EXPOSES));
        assertTrue(document.metadata().containsKey("interpretationSummary"));
    }
}
