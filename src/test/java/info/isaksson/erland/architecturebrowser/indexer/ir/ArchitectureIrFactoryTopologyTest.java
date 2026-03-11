package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationResult;
import info.isaksson.erland.architecturebrowser.indexer.interpret.model.InterpretationSummary;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ScopeKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologyResult;
import info.isaksson.erland.architecturebrowser.indexer.topology.model.TopologySummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrFactoryTopologyTest {

    @Test
    void includesTopologyScopesRelationshipsAndSummary() {
        TopologyResult topology = new TopologyResult(
            List.of(new LogicalScope("scope:dir:a", ScopeKind.DIRECTORY, "src/main/java/com/example", "src/main/java/com/example", "scope:repo", List.of(), Map.of())),
            List.of(new info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity(
                "entity:logical:module",
                EntityKind.MODULE,
                EntityOrigin.INFERRED,
                "src/main/java",
                "src/main/java",
                "scope:module:a",
                List.of(new SourceReference("src/main/java", null, null, null, Map.of())),
                Map.of("logicalRole", "source-root")
            )),
            List.of(new ArchitectureRelationship(
                "rel:logical",
                RelationshipKind.USES,
                "entity:file:a",
                "entity:file:b",
                "com.example.shared.CustomerRepository",
                List.of(),
                Map.of("rollup", "file-internal")
            )),
            List.of(),
            new TopologySummary(1, 1, 1, Map.of("DIRECTORY", 1), Map.of("MODULE", 1), Map.of("USES", 1))
        );

        ArchitectureIndexDocument document = ArchitectureIrFactory.createInventoryDocument(
            RepositorySource.localPath("sample", "/tmp/sample", Instant.parse("2026-03-11T11:00:00Z")),
            "0.1.0-SNAPSHOT",
            new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of()),
            List.of(),
            new ParseBatchResult(List.of(), Map.of(), Map.of()),
            new StructuralExtractionResult(List.of(), List.of(), List.of(), List.of(), new ExtractionSummary(0, 0, Map.of(), Map.of(), 0, 0)),
            new InterpretationResult(List.of(), List.of(), List.of(), new InterpretationSummary(Map.of(), Map.of(), Map.of())),
            topology
        );

        assertTrue(document.scopes().stream().anyMatch(scope -> scope.kind() == ScopeKind.DIRECTORY));
        assertTrue(document.entities().stream().anyMatch(entity -> entity.kind() == EntityKind.MODULE && "src/main/java".equals(entity.name())));
        assertTrue(document.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.USES));
        assertTrue(document.metadata().containsKey("topologySummary"));
    }
}
