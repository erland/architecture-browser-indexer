package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative TypeScript relationship normalization mapping.
 */
final class TypeScriptArchitectureRelationshipNormalizationRule implements ArchitectureRelationshipNormalizationRule {
    @Override
    public NormalizedArchitectureRelationship normalize(ArchitectureRelationshipNormalizationContext context) {
        ArchitectureRelationship relationship = context.relationship();
        if (relationship == null) {
            return null;
        }
        ArchitectureEntity source = context.entitiesById().get(relationship.fromEntityId());
        ArchitectureEntity target = context.entitiesById().get(relationship.toEntityId());
        if (!isTypeScriptBacked(relationship, source, target, context)) {
            return null;
        }

        List<String> semantics = new ArrayList<>();
        if (invokesUseCase(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id());
        }
        if (callsExternalSystem(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id());
        }
        if (accessesPersistence(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id());
        }
        return semantics.isEmpty() ? null : new NormalizedArchitectureRelationship(semantics);
    }

    private static boolean invokesUseCase(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && hasRole(source, ArchitecturalRole.API_ENTRYPOINT.id())
            && hasRole(target, ArchitecturalRole.APPLICATION_SERVICE.id());
    }

    private static boolean callsExternalSystem(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && (hasRole(source, ArchitecturalRole.INTEGRATION_ADAPTER.id()) || hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id()))
            && (target != null && (target.kind() == EntityKind.EXTERNAL_SYSTEM || hasRole(target, ArchitecturalRole.EXTERNAL_DEPENDENCY.id())));
    }

    private static boolean accessesPersistence(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && (hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id()) || hasRole(source, ArchitecturalRole.INTEGRATION_ADAPTER.id()))
            && (target != null && (target.kind() == EntityKind.DATASTORE || hasRole(target, ArchitecturalRole.PERSISTENT_ENTITY.id())));
    }

    private static boolean isInvocationRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.USES || kind == RelationshipKind.DEPENDS_ON || kind == RelationshipKind.CALLS || kind == RelationshipKind.READS || kind == RelationshipKind.WRITES;
    }

    private static boolean hasRole(ArchitectureEntity entity, String expectedRole) {
        return entity != null
            && entity.architecturalRoles() != null
            && entity.architecturalRoles().stream().anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }

    private static boolean isTypeScriptBacked(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (metadataContains(relationship.metadata(), "sourceLanguage", "typescript")
            || metadataContains(relationship.metadata(), "sourceLanguage", "tsx")
            || metadataContains(relationship.metadata(), "uiProfile", "page-or-router")
            || metadataContains(relationship.metadata(), "serviceProfile", "api-client-or-service")) {
            return true;
        }
        return isTypeScriptEntity(source, context) || isTypeScriptEntity(target, context);
    }

    private static boolean isTypeScriptEntity(ArchitectureEntity entity, ArchitectureRelationshipNormalizationContext context) {
        if (entity == null) {
            return false;
        }
        if (metadataContains(entity.metadata(), "language", "typescript")
            || metadataContains(entity.metadata(), "language", "tsx")
            || metadataContains(entity.metadata(), "sourceLanguage", "typescript")
            || metadataContains(entity.metadata(), "sourceLanguage", "tsx")) {
            return true;
        }
        Object sourceEntityId = entity.metadata().get("sourceEntityId");
        if (sourceEntityId == null) {
            return false;
        }
        ArchitectureEntity sourceEntity = context.entitiesById().get(String.valueOf(sourceEntityId));
        return sourceEntity != null && sourceEntity != entity && isTypeScriptEntity(sourceEntity, context);
    }

    private static boolean metadataContains(java.util.Map<String, Object> metadata, String key, String expected) {
        Object value = metadata == null ? null : metadata.get(key);
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }
}
