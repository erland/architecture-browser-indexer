package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Conservative SQL/config normalization mapping.
 */
final class SqlConfigArchitectureEntityNormalizationRule implements ArchitectureEntityNormalizationRule {
    @Override
    public NormalizedArchitectureEntity normalize(ArchitectureEntityNormalizationContext context) {
        ArchitectureEntity entity = context.entity();
        if (entity == null || !isSqlOrConfigBacked(entity, context.entitiesById())) {
            return null;
        }

        List<String> roles = new ArrayList<>();
        List<String> traits = new ArrayList<>();

        if (isSqlPersistentEntity(entity)) {
            roles.add(ArchitecturalRole.PERSISTENT_ENTITY.id());
            traits.add(ArchitecturalTrait.PERSISTENT.id());
        }
        if (isExternalDependency(entity)) {
            roles.add(ArchitecturalRole.EXTERNAL_DEPENDENCY.id());
        }
        if (isConfigurationProvider(entity)) {
            roles.add(ArchitecturalRole.CONFIGURATION_PROVIDER.id());
            traits.add(ArchitecturalTrait.CONFIGURATION_DRIVEN.id());
        }
        if (isModuleBoundary(entity)) {
            roles.add(ArchitecturalRole.MODULE_BOUNDARY.id());
        }

        if (roles.isEmpty() && traits.isEmpty()) {
            return null;
        }
        return new NormalizedArchitectureEntity(
            roles.isEmpty() ? null : roles,
            traits.isEmpty() ? null : traits
        );
    }

    private static boolean isSqlOrConfigBacked(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (metadataEquals(entity, "language", "sql")
            || metadataEquals(entity, "language", "json")
            || metadataEquals(entity, "language", "yaml")
            || metadataEquals(entity, "language", "properties")
            || metadataEquals(entity, "language", "xml")
            || metadataEquals(entity, "sourceLanguage", "sql")
            || metadataEquals(entity, "sourceLanguage", "json")
            || metadataEquals(entity, "sourceLanguage", "yaml")
            || metadataEquals(entity, "sourceLanguage", "properties")
            || metadataEquals(entity, "sourceLanguage", "xml")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        if (sourceEntityId == null || sourceEntityId.isBlank()) {
            return false;
        }
        ArchitectureEntity source = entitiesById.get(sourceEntityId);
        return source != null && isSqlOrConfigBacked(source, Map.of());
    }

    private static boolean isSqlPersistentEntity(ArchitectureEntity entity) {
        return entity.kind() == EntityKind.DATASTORE && metadataPresent(entity, "tableName") && metadataEquals(entity, "language", "sql");
    }

    private static boolean isExternalDependency(ArchitectureEntity entity) {
        if (entity.kind() == EntityKind.EXTERNAL_SYSTEM) {
            return true;
        }
        if (entity.kind() != EntityKind.DATASTORE) {
            return false;
        }
        return metadataEquals(entity, "external", "true") || metadataPresent(entity, "sourceConfigKey");
    }

    private static boolean isConfigurationProvider(ArchitectureEntity entity) {
        if (entity.kind() != EntityKind.CONFIG_ARTIFACT) {
            return false;
        }
        return metadataPresent(entity, "configKey") || metadataEquals(entity, "kind", "config-entry");
    }

    private static boolean isModuleBoundary(ArchitectureEntity entity) {
        if (entity.kind() != EntityKind.MODULE) {
            return false;
        }
        String language = lower(stringMetadata(entity, "language"));
        return language.equals("sql")
            || language.equals("json")
            || language.equals("yaml")
            || language.equals("properties")
            || language.equals("xml");
    }

    private static boolean metadataEquals(ArchitectureEntity entity, String key, String expected) {
        Object value = entity.metadata().get(key);
        return value != null && String.valueOf(value).equalsIgnoreCase(expected);
    }

    private static boolean metadataPresent(ArchitectureEntity entity, String key) {
        Object value = entity.metadata().get(key);
        return value != null && !String.valueOf(value).isBlank();
    }

    private static String stringMetadata(ArchitectureEntity entity, String key) {
        Object value = entity.metadata().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
