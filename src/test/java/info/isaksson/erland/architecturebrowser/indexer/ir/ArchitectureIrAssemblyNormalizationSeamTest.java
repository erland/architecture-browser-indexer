package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalTrait;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitectureEntityNormalizationService;
import info.isaksson.erland.architecturebrowser.indexer.normalize.NormalizedArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RepositorySource;
import info.isaksson.erland.architecturebrowser.indexer.scan.FileInventory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureIrAssemblyNormalizationSeamTest {

    @Test
    void assemblyRoutesEntitiesThroughNormalizationServiceBeforeComposition() {
        ExtractedEntityFact extracted = new ExtractedEntityFact(
            "entity:order-service",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            null,
            List.of(),
            Map.of()
        );

        ArchitectureIrAssemblyInputs inputs = new ArchitectureIrAssemblyInputs(
            RepositorySource.localPath("repo", "/tmp/repo", Instant.parse("2026-03-18T00:00:00Z")),
            new FileInventory(List.of(), 0, 0, 0, Set.of(), Set.of()),
            List.of(),
            null,
            new StructuralExtractionResult(List.of(), List.of(extracted), List.of(), List.of(), null),
            null,
            null
        );

        ArchitectureEntityNormalizationService normalizationService = ArchitectureEntityNormalizationService.of(List.of(
            context -> context.entity().id().equals("entity:order-service")
                ? new NormalizedArchitectureEntity(
                    List.of(ArchitecturalRole.APPLICATION_SERVICE.id()),
                    List.of(ArchitecturalTrait.TRANSACTIONAL.id())
                )
                : NormalizedArchitectureEntity.EMPTY
        ));

        ArchitectureIrAssemblyState state = ArchitectureIrAssemblyStateBuilder.build(inputs, normalizationService);
        ArchitectureEntity entity = state.entitiesById().get("entity:order-service");

        assertEquals(List.of("application-service"), entity.architecturalRoles());
        assertEquals(List.of("transactional"), entity.architecturalTraits());
    }
}
