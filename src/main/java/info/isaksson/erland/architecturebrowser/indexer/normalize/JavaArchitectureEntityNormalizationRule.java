package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Java-first normalization mapping.
 *
 * <p>Step 5 keeps the mapping intentionally conservative and additive. It reuses already available
 * Java/JAX-RS/JPA/repository evidence and maps it into stable architecture-facing roles instead of
 * asking downstream consumers to understand Java framework metadata directly.</p>
 */
final class JavaArchitectureEntityNormalizationRule implements ArchitectureEntityNormalizationRule {
    @Override
    public NormalizedArchitectureEntity normalize(ArchitectureEntityNormalizationContext context) {
        ArchitectureEntity entity = context.entity();
        if (entity == null || !isJavaBacked(entity, context.entitiesById()) || isTestBacked(entity, context.entitiesById())) {
            return null;
        }

        List<String> roles = new ArrayList<>();
        List<String> traits = new ArrayList<>();

        if (isApiEntrypoint(entity, context.entitiesById())) {
            roles.add(ArchitecturalRole.API_ENTRYPOINT.id());
            traits.add(ArchitecturalTrait.EXTERNALLY_EXPOSED.id());
        }
        if (isApplicationService(entity, context.entitiesById())) {
            roles.add(ArchitecturalRole.APPLICATION_SERVICE.id());
        }
        if (isPersistentEntity(entity, context.entitiesById())) {
            roles.add(ArchitecturalRole.PERSISTENT_ENTITY.id());
            traits.add(ArchitecturalTrait.PERSISTENT.id());
        }
        if (isPersistenceAccess(entity, context.entitiesById())) {
            roles.add(ArchitecturalRole.PERSISTENCE_ACCESS.id());
        }

        if (roles.isEmpty() && traits.isEmpty()) {
            return null;
        }
        return new NormalizedArchitectureEntity(
            roles.isEmpty() ? null : roles,
            traits.isEmpty() ? null : traits
        );
    }

    private static boolean isJavaBacked(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (metadataEquals(entity, "language", "java") || hasFramework(entity, "jax-rs") || hasFramework(entity, "persistence") || hasFramework(entity, "jpa")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        if (sourceEntityId == null || sourceEntityId.isBlank()) {
            return false;
        }
        ArchitectureEntity source = entitiesById.get(sourceEntityId);
        return source != null && isJavaBacked(source, Map.of());
    }

    private static boolean isApiEntrypoint(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (isTestBacked(entity, entitiesById)) {
            return false;
        }
        if (entity.kind() == EntityKind.ENDPOINT) {
            return metadataEquals(entity, "sourceLanguage", "java") || metadataEquals(entity, "language", "java") || referencesJavaSource(entity, entitiesById);
        }
        if (Boolean.TRUE.equals(entity.metadata().get("jaxRsResource"))) {
            return true;
        }
        if (metadataEquals(entity, "entityRole", "resource") || containsMetadata(entity, "backendProfile", "resource")) {
            return true;
        }
        return false;
    }

    private static boolean isApplicationService(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (isTestBacked(entity, entitiesById)) {
            return false;
        }
        if (metadataEquals(entity, "entityRole", "service") || containsMetadata(entity, "backendProfile", "application-service")) {
            return true;
        }
        if (entity.kind() == EntityKind.SERVICE && referencesJavaSource(entity, entitiesById) && !isApiEntrypoint(entity, entitiesById)) {
            return true;
        }
        String lowerName = entity.name() == null ? "" : entity.name().toLowerCase(Locale.ROOT);
        String packageName = stringMetadata(entity, "packageName");
        List<String> annotations = stringListMetadata(entity, "annotations");
        return matchesAny(annotations, "service")
            || lowerName.endsWith("service")
            || lowerName.endsWith("facade")
            || lowerName.endsWith("manager")
            || containsIgnoreCase(packageName, ".service")
            || containsIgnoreCase(packageName, ".application")
            || containsIgnoreCase(packageName, ".usecase");
    }

    private static boolean isPersistentEntity(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (isTestBacked(entity, entitiesById)) {
            return false;
        }
        if (Boolean.TRUE.equals(entity.metadata().get("jpaEntity")) || metadataEquals(entity, "jpaKind", "entity")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        if (sourceEntityId == null || sourceEntityId.isBlank()) {
            return false;
        }
        ArchitectureEntity source = entitiesById.get(sourceEntityId);
        return source != null && isPersistentEntity(source, Map.of());
    }

    private static boolean isPersistenceAccess(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (isTestBacked(entity, entitiesById)) {
            return false;
        }
        if (entity.kind() == EntityKind.PERSISTENCE_ADAPTER) {
            return true;
        }
        if (metadataEquals(entity, "entityRole", "repository") || metadataEquals(entity, "entityRole", "dao") || metadataEquals(entity, "entityRole", "mapper")) {
            return true;
        }
        if (metadataEquals(entity, "backendProfile", "repository") || metadataEquals(entity, "backendProfile", "mapping-adapter")) {
            return true;
        }
        String lowerName = entity.name() == null ? "" : entity.name().toLowerCase(Locale.ROOT);
        String packageName = stringMetadata(entity, "packageName");
        List<String> annotations = stringListMetadata(entity, "annotations");
        if (matchesAny(annotations, "repository", "mapper")
            || lowerName.endsWith("repository")
            || lowerName.endsWith("dao")
            || lowerName.endsWith("mapper")
            || containsIgnoreCase(packageName, ".repo")
            || containsIgnoreCase(packageName, ".repository")
            || containsIgnoreCase(packageName, ".dao")
            || containsIgnoreCase(packageName, ".mapper")
            || containsIgnoreCase(packageName, ".persistence")) {
            return true;
        }
        if (!referencesJavaSource(entity, entitiesById)) {
            return false;
        }
        return false;
    }

    private static boolean referencesJavaSource(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        if (sourceEntityId == null || sourceEntityId.isBlank()) {
            return false;
        }
        ArchitectureEntity source = entitiesById.get(sourceEntityId);
        return source != null && isJavaBacked(source, Map.of());
    }

    private static boolean isTestBacked(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity == null) {
            return false;
        }
        if (entity.sourceRefs().stream().anyMatch(ref -> isTestPath(ref.path()))) {
            return true;
        }
        if (isTestPath(stringMetadata(entity, "relativePath"))
            || isTestPath(stringMetadata(entity, "sourcePath"))
            || isTestPath(stringMetadata(entity, "filePath"))
            || isTestPath(stringMetadata(entity, "path"))) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        if (sourceEntityId == null || sourceEntityId.isBlank()) {
            return false;
        }
        ArchitectureEntity source = entitiesById.get(sourceEntityId);
        return source != null && isTestBacked(source, entitiesById);
    }

    private static boolean isTestPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("/src/test/")
            || normalized.startsWith("src/test/")
            || normalized.contains("/test/")
            || normalized.startsWith("test/");
    }

    private static boolean hasFramework(ArchitectureEntity entity, String expected) {
        if (metadataEquals(entity, "framework", expected)) {
            return true;
        }
        Object frameworks = entity.metadata().get("frameworks");
        if (frameworks instanceof List<?> list) {
            return list.stream().anyMatch(value -> expected.equalsIgnoreCase(String.valueOf(value)));
        }
        return containsIgnoreCase(String.valueOf(frameworks), expected);
    }

    private static boolean matchesAny(List<String> annotations, String... suffixes) {
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }
        for (String annotation : annotations) {
            String normalized = annotation == null ? "" : annotation.toLowerCase(Locale.ROOT);
            for (String suffix : suffixes) {
                if (normalized.endsWith(suffix.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean metadataEquals(ArchitectureEntity entity, String key, String expected) {
        return expected.equalsIgnoreCase(String.valueOf(entity.metadata().getOrDefault(key, "")));
    }

    private static boolean containsMetadata(ArchitectureEntity entity, String key, String expected) {
        return containsIgnoreCase(String.valueOf(entity.metadata().getOrDefault(key, "")), expected);
    }

    private static boolean containsIgnoreCase(String value, String expectedFragment) {
        return value != null && expectedFragment != null && value.toLowerCase(Locale.ROOT).contains(expectedFragment.toLowerCase(Locale.ROOT));
    }

    private static String stringMetadata(ArchitectureEntity entity, String key) {
        Object value = entity.metadata().get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private static List<String> stringListMetadata(ArchitectureEntity entity, String key) {
        Object value = entity.metadata().get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
