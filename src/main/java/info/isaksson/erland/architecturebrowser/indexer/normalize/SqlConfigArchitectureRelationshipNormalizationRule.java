package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative SQL/config relationship normalization mapping.
 */
final class SqlConfigArchitectureRelationshipNormalizationRule implements ArchitectureRelationshipNormalizationRule {
    @Override
    public NormalizedArchitectureRelationship normalize(ArchitectureRelationshipNormalizationContext context) {
        ArchitectureRelationship relationship = context.relationship();
        if (relationship == null) {
            return null;
        }
        ArchitectureEntity source = context.entitiesById().get(relationship.fromEntityId());
        ArchitectureEntity target = context.entitiesById().get(relationship.toEntityId());
        if (!isSqlOrConfigBacked(relationship, source, target, context)) {
            return null;
        }

        List<String> semantics = new ArrayList<>();
        if (configurationCallsExternal(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id());
        }
        if (configurationAccessesPersistence(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id());
        }
        return semantics.isEmpty() ? null : new NormalizedArchitectureRelationship(semantics);
    }

    private static boolean configurationCallsExternal(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isRelevantRelationship(relationship.kind())
            && hasRole(source, ArchitecturalRole.CONFIGURATION_PROVIDER.id())
            && target != null
            && (target.kind() == EntityKind.EXTERNAL_SYSTEM || hasRole(target, ArchitecturalRole.EXTERNAL_DEPENDENCY.id()));
    }

    private static boolean configurationAccessesPersistence(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isRelevantRelationship(relationship.kind())
            && (hasRole(source, ArchitecturalRole.CONFIGURATION_PROVIDER.id()) || hasRole(source, ArchitecturalRole.MODULE_BOUNDARY.id()))
            && target != null
            && (target.kind() == EntityKind.DATASTORE || hasRole(target, ArchitecturalRole.PERSISTENT_ENTITY.id()));
    }

    private static boolean isRelevantRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.USES || kind == RelationshipKind.DEPENDS_ON || kind == RelationshipKind.READS || kind == RelationshipKind.WRITES || kind == RelationshipKind.CALLS;
    }

    private static boolean hasRole(ArchitectureEntity entity, String expectedRole) {
        return entity != null
            && entity.architecturalRoles() != null
            && entity.architecturalRoles().stream().anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }

    private static boolean isSqlOrConfigBacked(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (metadataContains(relationship.metadata(), "sourceLanguage", "sql")
            || metadataContains(relationship.metadata(), "sourceLanguage", "json")
            || metadataContains(relationship.metadata(), "sourceLanguage", "yaml")
            || metadataContains(relationship.metadata(), "sourceLanguage", "properties")
            || metadataContains(relationship.metadata(), "sourceLanguage", "xml")) {
            return true;
        }
        return isSqlOrConfigEntity(source, context) || isSqlOrConfigEntity(target, context);
    }

    private static boolean isSqlOrConfigEntity(ArchitectureEntity entity, ArchitectureRelationshipNormalizationContext context) {
        if (entity == null) {
            return false;
        }
        if (metadataContains(entity.metadata(), "language", "sql")
            || metadataContains(entity.metadata(), "language", "json")
            || metadataContains(entity.metadata(), "language", "yaml")
            || metadataContains(entity.metadata(), "language", "properties")
            || metadataContains(entity.metadata(), "language", "xml")
            || metadataContains(entity.metadata(), "sourceLanguage", "sql")
            || metadataContains(entity.metadata(), "sourceLanguage", "json")
            || metadataContains(entity.metadata(), "sourceLanguage", "yaml")
            || metadataContains(entity.metadata(), "sourceLanguage", "properties")
            || metadataContains(entity.metadata(), "sourceLanguage", "xml")) {
            return true;
        }
        Object sourceEntityId = entity.metadata().get("sourceEntityId");
        if (sourceEntityId == null) {
            return false;
        }
        ArchitectureEntity sourceEntity = context.entitiesById().get(String.valueOf(sourceEntityId));
        return sourceEntity != null && sourceEntity != entity && isSqlOrConfigEntity(sourceEntity, context);
    }

    private static boolean metadataContains(java.util.Map<String, Object> metadata, String key, String expected) {
        Object value = metadata == null ? null : metadata.get(key);
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }
}
