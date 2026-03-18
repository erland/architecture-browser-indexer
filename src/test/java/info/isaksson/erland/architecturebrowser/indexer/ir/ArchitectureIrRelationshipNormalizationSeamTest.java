package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedRelationshipFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitectureEntityNormalizationService;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitectureRelationshipNormalizationService;
import info.isaksson.erland.architecturebrowser.indexer.normalize.NormalizedArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureIrRelationshipNormalizationSeamTest {

    @Test
    void assemblyRoutesRelationshipsThroughNormalizationServiceBeforeComposition() {
        ExtractedEntityFact source = new ExtractedEntityFact(
            "entity:resource",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderResource",
            "OrderResource",
            null,
            List.of(),
            Map.of("language", "java")
        );
        ExtractedEntityFact target = new ExtractedEntityFact(
            "entity:service",
            EntityKind.SERVICE,
            EntityOrigin.INFERRED,
            "OrderService",
            "OrderService",
            null,
            List.of(),
            Map.of("sourceLanguage", "java")
        );
        ExtractedRelationshipFact relationship = new ExtractedRelationshipFact(
            "rel:resource-service",
            RelationshipKind.USES,
            source.id(),
            target.id(),
            "orderService",
            List.of(),
            Map.of("sourceLanguage", "java")
        );

        ArchitectureIrAssemblyInputs inputs = new ArchitectureIrAssemblyInputs(
            RepositorySource.localPath("repo", "/tmp/repo", Instant.parse("2026-03-18T00:00:00Z")),
            new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of()),
            List.of(),
            null,
            new StructuralExtractionResult(List.of(), List.of(source, target), List.of(relationship), List.of(), null),
            null,
            null
        );

        ArchitectureIrAssemblyState state = ArchitectureIrAssemblyStateBuilder.build(
            inputs,
            ArchitectureEntityNormalizationService.of(List.of()),
            ArchitectureRelationshipNormalizationService.of(List.of(
                context -> new NormalizedArchitectureRelationship(List.of("invokes-use-case"))
            ))
        );
        ArchitectureRelationship normalizedRelationship = state.relationships().stream()
            .filter(rel -> rel.id().equals("rel:resource-service"))
            .findFirst()
            .orElseThrow();

        assertEquals(List.of("invokes-use-case"), normalizedRelationship.architecturalSemantics());
    }
}
