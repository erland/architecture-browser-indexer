package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Java-first normalization mapping for relationship semantics.
 *
 * <p>Step 6 keeps the mapping intentionally conservative and additive. It reuses the normalized
 * entity roles introduced in Step 5 together with current Java interpretation/dependency evidence
 * so downstream consumers can follow request and persistence flows without inspecting raw Java
 * metadata.</p>
 */
final class JavaArchitectureRelationshipNormalizationRule implements ArchitectureRelationshipNormalizationRule {
    @Override
    public NormalizedArchitectureRelationship normalize(ArchitectureRelationshipNormalizationContext context) {
        ArchitectureRelationship relationship = context.relationship();
        if (relationship == null) {
            return null;
        }
        ArchitectureEntity source = context.entitiesById().get(relationship.fromEntityId());
        ArchitectureEntity target = context.entitiesById().get(relationship.toEntityId());
        if (!isJavaBacked(relationship, source, target, context)) {
            return null;
        }

        List<String> semantics = new ArrayList<>();
        if (servesRequest(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.SERVES_REQUEST.id());
        }
        if (invokesUseCase(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id());
        }
        if (accessesPersistence(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id());
        }
        if (storedIn(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.STORED_IN.id());
        }
        if (callsExternalSystem(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id());
        }
        return semantics.isEmpty() ? null : new NormalizedArchitectureRelationship(semantics);
    }

    private static boolean servesRequest(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return relationship.kind() == RelationshipKind.EXPOSES
            && (hasRole(target, ArchitecturalRole.API_ENTRYPOINT.id()) || hasRole(source, ArchitecturalRole.API_ENTRYPOINT.id()));
    }

    private static boolean invokesUseCase(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && hasRole(source, ArchitecturalRole.API_ENTRYPOINT.id())
            && hasRole(target, ArchitecturalRole.APPLICATION_SERVICE.id());
    }

    private static boolean accessesPersistence(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id())
            && hasRole(target, ArchitecturalRole.PERSISTENCE_ACCESS.id());
    }

    private static boolean storedIn(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && hasRole(source, ArchitecturalRole.PERSISTENT_ENTITY.id())
            && target != null
            && target.kind() == EntityKind.DATASTORE;
    }

    private static boolean callsExternalSystem(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && target != null
            && target.kind() == EntityKind.EXTERNAL_SYSTEM
            && (hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id())
                || hasRole(source, ArchitecturalRole.PERSISTENCE_ACCESS.id())
                || hasRole(source, ArchitecturalRole.API_ENTRYPOINT.id()));
    }

    private static boolean isInvocationRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.USES || kind == RelationshipKind.DEPENDS_ON;
    }

    private static boolean hasRole(ArchitectureEntity entity, String expectedRole) {
        return entity != null
            && entity.architecturalRoles() != null
            && entity.architecturalRoles().stream().anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }

    private static boolean isJavaBacked(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (metadataContains(relationship.metadata(), "sourceLanguage", "java")
            || metadataContains(relationship.metadata(), "framework", "jax-rs")
            || metadataListContains(relationship.metadata(), "frameworks", "cdi")
            || metadataListContains(relationship.metadata(), "frameworks", "jpa")
            || metadataListContains(relationship.metadata(), "frameworkRelationships", "exposesEndpoint")
            || metadataListContains(relationship.metadata(), "frameworkRelationships", "publishesEvent")) {
            return true;
        }
        return isJavaEntity(source, context) || isJavaEntity(target, context);
    }

    private static boolean isJavaEntity(ArchitectureEntity entity, ArchitectureRelationshipNormalizationContext context) {
        if (entity == null) {
            return false;
        }
        if (metadataContains(entity.metadata(), "language", "java")
            || metadataContains(entity.metadata(), "sourceLanguage", "java")
            || metadataListContains(entity.metadata(), "frameworks", "jax-rs")
            || metadataListContains(entity.metadata(), "frameworks", "jpa")
            || metadataContains(entity.metadata(), "framework", "jax-rs")
            || metadataContains(entity.metadata(), "framework", "jpa")
            || Boolean.TRUE.equals(entity.metadata().get("jaxRsResource"))
            || Boolean.TRUE.equals(entity.metadata().get("jpaEntity"))) {
            return true;
        }
        Object sourceEntityId = entity.metadata().get("sourceEntityId");
        if (sourceEntityId == null) {
            return false;
        }
        ArchitectureEntity sourceEntity = context.entitiesById().get(String.valueOf(sourceEntityId));
        return sourceEntity != null && sourceEntity != entity && isJavaEntity(sourceEntity, context);
    }

    private static boolean metadataContains(java.util.Map<String, Object> metadata, String key, String expected) {
        Object value = metadata == null ? null : metadata.get(key);
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private static boolean metadataListContains(java.util.Map<String, Object> metadata, String key, String expected) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).anyMatch(item -> item.equalsIgnoreCase(expected));
        }
        return value != null && String.valueOf(value).equalsIgnoreCase(expected);
    }
}
