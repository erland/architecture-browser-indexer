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
import static org.junit.jupiter.api.Assertions.assertNull;

class JavaArchitectureRelationshipNormalizationRuleTest {

    private final ArchitectureRelationshipNormalizationService service = ArchitectureRelationshipNormalizationService.defaultService();

    @Test
    void mapsJavaRequestAndPersistenceFlowRelationshipsToCanonicalSemantics() {
        ArchitectureEntity resource = entity(
            "entity:class:order-resource",
            EntityKind.CLASS,
            Map.of("language", "java"),
            List.of("api-entrypoint")
        );
        ArchitectureEntity endpoint = entity(
            "entity:endpoint:create-order",
            EntityKind.ENDPOINT,
            Map.of("sourceLanguage", "java"),
            List.of("api-entrypoint")
        );
        ArchitectureEntity serviceEntity = entity(
            "entity:class:order-service",
            EntityKind.SERVICE,
            Map.of("language", "java"),
            List.of("application-service")
        );
        ArchitectureEntity repository = entity(
            "entity:class:order-repository",
            EntityKind.PERSISTENCE_ADAPTER,
            Map.of("language", "java"),
            List.of("persistence-access")
        );

        Map<String, ArchitectureEntity> entitiesById = Map.of(
            resource.id(), resource,
            endpoint.id(), endpoint,
            serviceEntity.id(), serviceEntity,
            repository.id(), repository
        );

        ArchitectureRelationship servesRequest = new ArchitectureRelationship(
            "rel:resource:endpoint",
            RelationshipKind.EXPOSES,
            resource.id(),
            endpoint.id(),
            "POST /orders",
            List.of(),
            Map.of("sourceLanguage", "java")
        );
        ArchitectureRelationship invokesUseCase = new ArchitectureRelationship(
            "rel:resource:service",
            RelationshipKind.USES,
            resource.id(),
            serviceEntity.id(),
            "orderService",
            List.of(),
            Map.of("sourceLanguage", "java", "dependencySource", "field")
        );
        ArchitectureRelationship accessesPersistence = new ArchitectureRelationship(
            "rel:service:repository",
            RelationshipKind.USES,
            serviceEntity.id(),
            repository.id(),
            "orderRepository",
            List.of(),
            Map.of("sourceLanguage", "java", "dependencySource", "field")
        );

        assertEquals(List.of("serves-request"), service.normalizeRelationship(servesRequest, entitiesById, Map.of()).architecturalSemantics());
        assertEquals(List.of("invokes-use-case"), service.normalizeRelationship(invokesUseCase, entitiesById, Map.of()).architecturalSemantics());
        assertEquals(List.of("accesses-persistence"), service.normalizeRelationship(accessesPersistence, entitiesById, Map.of()).architecturalSemantics());
    }

    @Test
    void preservesExistingSemanticsAndStaysConservativeForNonJavaRelationships() {
        ArchitectureEntity frontend = entity(
            "entity:ui:orders",
            EntityKind.UI_MODULE,
            Map.of("language", "typescript"),
            List.of("api-entrypoint")
        );
        ArchitectureEntity serviceEntity = entity(
            "entity:service:orders",
            EntityKind.SERVICE,
            Map.of("language", "typescript"),
            List.of("application-service")
        );
        ArchitectureRelationship nonJava = new ArchitectureRelationship(
            "rel:frontend:service",
            RelationshipKind.USES,
            frontend.id(),
            serviceEntity.id(),
            "ordersApi",
            List.of(),
            Map.of("sourceLanguage", "typescript"),
            List.of("custom-semantic")
        );

        ArchitectureRelationship normalized = service.normalizeRelationship(
            nonJava,
            Map.of(frontend.id(), frontend, serviceEntity.id(), serviceEntity),
            Map.of(nonJava.id(), nonJava)
        );

        assertEquals(List.of("custom-semantic"), normalized.architecturalSemantics());

        ArchitectureRelationship empty = new ArchitectureRelationship(
            "rel:none",
            RelationshipKind.USES,
            frontend.id(),
            serviceEntity.id(),
            "ordersApi",
            List.of(),
            Map.of()
        );
        assertNull(service.normalizeRelationship(empty, Map.of(frontend.id(), frontend, serviceEntity.id(), serviceEntity), Map.of()).architecturalSemantics());
    }

    private static ArchitectureEntity entity(String id, EntityKind kind, Map<String, Object> metadata, List<String> roles) {
        return new ArchitectureEntity(
            id,
            kind,
            EntityOrigin.OBSERVED,
            id,
            id,
            "scope:repo",
            List.of(),
            metadata,
            roles,
            null
        );
    }
}
