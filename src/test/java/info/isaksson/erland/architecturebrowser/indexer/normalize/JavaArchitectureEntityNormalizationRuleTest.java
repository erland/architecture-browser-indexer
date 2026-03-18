package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JavaArchitectureEntityNormalizationRuleTest {

    private final ArchitectureEntityNormalizationService service = ArchitectureEntityNormalizationService.defaultService();

    @Test
    void mapsObservedJavaResourceServiceJpaEntityAndRepositoryToCanonicalRoles() {
        ArchitectureEntity resource = entity(
            "entity:class:order-resource",
            EntityKind.CLASS,
            "OrderResource",
            Map.of(
                "language", "java",
                "packageName", "com.example.orders.api",
                "jaxRsResource", true,
                "annotations", List.of("Path")
            )
        );
        ArchitectureEntity serviceEntity = entity(
            "entity:class:order-service",
            EntityKind.CLASS,
            "OrderService",
            Map.of(
                "language", "java",
                "packageName", "com.example.orders.service",
                "annotations", List.of("ApplicationScoped")
            )
        );
        ArchitectureEntity orderEntity = entity(
            "entity:class:order-entity",
            EntityKind.CLASS,
            "OrderEntity",
            Map.of(
                "language", "java",
                "jpaEntity", true,
                "jpaKind", "entity"
            )
        );
        ArchitectureEntity repository = entity(
            "entity:class:order-repository",
            EntityKind.CLASS,
            "OrderRepository",
            Map.of(
                "language", "java",
                "packageName", "com.example.orders.repo",
                "annotations", List.of("ApplicationScoped")
            )
        );

        Map<String, ArchitectureEntity> entitiesById = Map.of(
            resource.id(), resource,
            serviceEntity.id(), serviceEntity,
            orderEntity.id(), orderEntity,
            repository.id(), repository
        );

        assertEquals(List.of("api-entrypoint"), service.normalizeEntity(resource, entitiesById).architecturalRoles());
        assertEquals(List.of("externally-exposed"), service.normalizeEntity(resource, entitiesById).architecturalTraits());

        assertEquals(List.of("application-service"), service.normalizeEntity(serviceEntity, entitiesById).architecturalRoles());
        assertNull(service.normalizeEntity(serviceEntity, entitiesById).architecturalTraits());

        assertEquals(List.of("persistent-entity"), service.normalizeEntity(orderEntity, entitiesById).architecturalRoles());
        assertEquals(List.of("persistent"), service.normalizeEntity(orderEntity, entitiesById).architecturalTraits());

        assertEquals(List.of("persistence-access"), service.normalizeEntity(repository, entitiesById).architecturalRoles());
    }

    @Test
    void mapsInterpretedJavaRoleEntitiesAndEndpointsWithoutDestroyingExistingValues() {
        ArchitectureEntity sourceResource = entity(
            "entity:class:order-resource",
            EntityKind.CLASS,
            "OrderResource",
            Map.of(
                "language", "java",
                "jaxRsResource", true,
                "annotations", List.of("Path")
            )
        );
        ArchitectureEntity interpretedResourceRole = new ArchitectureEntity(
            "entity:interpret-service:order-resource",
            EntityKind.SERVICE,
            EntityOrigin.INFERRED,
            "OrderResource",
            "OrderResource service",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "java",
                "sourceEntityId", sourceResource.id(),
                "entityRole", "resource",
                "backendProfile", "jax-rs-resource"
            ),
            null,
            List.of("framework-managed")
        );
        ArchitectureEntity endpoint = new ArchitectureEntity(
            "entity:interpret-endpoint:post-orders",
            EntityKind.ENDPOINT,
            EntityOrigin.INFERRED,
            "POST /orders",
            "OrderResource endpoint POST /orders",
            "scope:repo",
            List.of(),
            Map.of(
                "sourceLanguage", "java",
                "sourceEntityId", sourceResource.id(),
                "httpMethod", "POST",
                "path", "/orders"
            )
        );

        Map<String, ArchitectureEntity> entitiesById = Map.of(
            sourceResource.id(), sourceResource,
            interpretedResourceRole.id(), interpretedResourceRole,
            endpoint.id(), endpoint
        );

        ArchitectureEntity normalizedRole = service.normalizeEntity(interpretedResourceRole, entitiesById);
        assertEquals(List.of("api-entrypoint"), normalizedRole.architecturalRoles());
        assertEquals(List.of("externally-exposed", "framework-managed"), normalizedRole.architecturalTraits());

        ArchitectureEntity normalizedEndpoint = service.normalizeEntity(endpoint, entitiesById);
        assertEquals(List.of("api-entrypoint"), normalizedEndpoint.architecturalRoles());
        assertEquals(List.of("externally-exposed"), normalizedEndpoint.architecturalTraits());
    }

    private static ArchitectureEntity entity(String id, EntityKind kind, String name, Map<String, Object> metadata) {
        return new ArchitectureEntity(
            id,
            kind,
            EntityOrigin.OBSERVED,
            name,
            name,
            "scope:repo",
            List.of(),
            metadata
        );
    }
}
