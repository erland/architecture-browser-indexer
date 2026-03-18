package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArchitectureEntityNormalizationServiceTest {

    @Test
    void normalizationServiceMergesRuleOutputsThroughCanonicalContractFields() {
        ArchitectureEntity entity = new ArchitectureEntity(
            "entity:order-service",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of(),
            List.of("application-service"),
            null
        );

        ArchitectureEntityNormalizationService service = ArchitectureEntityNormalizationService.of(List.of(
            context -> new NormalizedArchitectureEntity(
                List.of(ArchitecturalRole.API_ENTRYPOINT.id(), ArchitecturalRole.APPLICATION_SERVICE.id()),
                List.of(ArchitecturalTrait.EXTERNALLY_EXPOSED.id())
            ),
            context -> new NormalizedArchitectureEntity(
                List.of(" api-entrypoint "),
                List.of(ArchitecturalTrait.TRANSACTIONAL.id(), ArchitecturalTrait.EXTERNALLY_EXPOSED.id())
            )
        ));

        ArchitectureEntity normalized = service.normalizeEntity(entity, Map.of(entity.id(), entity));

        assertEquals(List.of("api-entrypoint", "application-service"), normalized.architecturalRoles());
        assertEquals(List.of("externally-exposed", "transactional"), normalized.architecturalTraits());
    }

    @Test
    void defaultNormalizationServicePreservesCurrentBehaviorUntilRulesAreAdded() {
        ArchitectureEntity entity = new ArchitectureEntity(
            "entity:order-service",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "OrderService",
            "OrderService",
            "scope:repo",
            List.of(),
            Map.of()
        );

        ArchitectureEntity normalized = ArchitectureEntityNormalizationService.defaultService()
            .normalizeEntity(entity, Map.of(entity.id(), entity));

        assertNull(normalized.architecturalRoles());
        assertNull(normalized.architecturalTraits());
    }
}
