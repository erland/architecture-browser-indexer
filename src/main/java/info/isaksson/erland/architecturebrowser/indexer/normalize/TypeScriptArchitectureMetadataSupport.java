package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TypeScriptArchitectureMetadataSupport {
    private TypeScriptArchitectureMetadataSupport() {
    }

    static boolean isTypeScriptBacked(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity == null) {
            return false;
        }
        if (metadataEquals(entity.metadata(), "language", "typescript")
            || metadataEquals(entity.metadata(), "sourceLanguage", "typescript")
            || metadataEquals(entity.metadata(), "language", "tsx")
            || metadataEquals(entity.metadata(), "sourceLanguage", "tsx")
            || metadataEquals(entity.metadata(), "framework", "react")
            || metadataEquals(entity.metadata(), "framework", "angular")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity.metadata(), "sourceEntityId");
        if (blank(sourceEntityId)) {
            return false;
        }
        ArchitectureEntity source = entitiesById.get(sourceEntityId);
        return source != null && source != entity && isTypeScriptBacked(source, entitiesById);
    }

    static boolean isTypeScriptBacked(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (relationship == null) {
            return false;
        }
        if (metadataContains(relationship.metadata(), "sourceLanguage", "typescript")
            || metadataContains(relationship.metadata(), "sourceLanguage", "tsx")
            || metadataContains(relationship.metadata(), "uiProfile", "page-or-router")
            || metadataContains(relationship.metadata(), "serviceProfile", "api-client-or-service")
            || metadataContains(relationship.metadata(), "framework", "react")
            || metadataContains(relationship.metadata(), "framework", "angular")
            || context.relationshipRouteEvidenceOptional().isPresent()
            || context.frontendNavigationEvidenceOptional().isPresent()) {
            return true;
        }
        return isTypeScriptBacked(source, context.entitiesById()) || isTypeScriptBacked(target, context.entitiesById());
    }

    static boolean metadataEquals(Map<String, Object> metadata, String key, String expected) {
        Object value = metadata == null ? null : metadata.get(key);
        return value != null && String.valueOf(value).equalsIgnoreCase(expected);
    }

    static boolean metadataContains(Map<String, Object> metadata, String key, String expected) {
        Object value = metadata == null ? null : metadata.get(key);
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    static boolean metadataListContains(Map<String, Object> metadata, String key, String expectedSuffixOrValue) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                .map(String::valueOf)
                .map(TypeScriptArchitectureMetadataSupport::lower)
                .anyMatch(item -> item.equals(lower(expectedSuffixOrValue)) || item.endsWith(lower(expectedSuffixOrValue)));
        }
        return value != null && lower(String.valueOf(value)).contains(lower(expectedSuffixOrValue));
    }

    static String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!blank(value)) {
                return value;
            }
        }
        return null;
    }

    static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
