package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitectureRelationshipNormalizationServiceTest {

    @Test
    void normalizationServiceMergesRuleOutputsThroughCanonicalContractField() {
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:resource-service",
            RelationshipKind.USES,
            "entity:resource",
            "entity:service",
            "orderService",
            List.of(),
            Map.of(),
            List.of(" accesses-persistence ")
        );

        ArchitectureRelationshipNormalizationService service = ArchitectureRelationshipNormalizationService.of(List.of(
            context -> new NormalizedArchitectureRelationship(List.of(
                ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id(),
                ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id()
            )),
            context -> new NormalizedArchitectureRelationship(List.of(
                " invokes-use-case ",
                ArchitecturalRelationshipSemantic.SERVES_REQUEST.id()
            ))
        ));

        ArchitectureRelationship normalized = service.normalizeRelationship(
            relationship,
            Map.of(
                "entity:resource", entity("entity:resource", List.of("api-entrypoint")),
                "entity:service", entity("entity:service", List.of("application-service"))
            ),
            Map.of(relationship.id(), relationship)
        );

        assertEquals(List.of("accesses-persistence", "invokes-use-case", "serves-request"), normalized.architecturalSemantics());
    }

    private static ArchitectureEntity entity(String id, List<String> roles) {
        return new ArchitectureEntity(
            id,
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            id,
            id,
            "scope:repo",
            List.of(),
            Map.of(),
            roles,
            null
        );
    }
}
