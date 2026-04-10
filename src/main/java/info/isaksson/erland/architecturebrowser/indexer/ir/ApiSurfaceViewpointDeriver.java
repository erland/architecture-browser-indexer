package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class ApiSurfaceViewpointDeriver implements ArchitectureViewpointDeriver {
    @Override
    public ArchitectureViewpoint derive(ViewpointEvidence evidence) {
        List<String> entrypoints = preferredEntrypointSeeds(evidence);
        boolean hasEntryPoints = !entrypoints.isEmpty();
        String availability = hasEntryPoints ? "available" : "unavailable";
        double confidence = hasEntryPoints
            ? ArchitectureViewpointDerivationSupport.clamp(0.72 + Math.min(0.24, entrypoints.size() * 0.08))
            : 0.0;
        return new ArchitectureViewpoint(
            "api-surface",
            "API surface",
            "Highlights externally exposed API entrypoints and the first service hop behind them when available.",
            availability,
            confidence,
            hasEntryPoints ? entrypoints : null,
            hasEntryPoints ? java.util.List.of(ArchitecturalRole.API_ENTRYPOINT.id()) : null,
            evidence.hasSemantic(ArchitecturalRelationshipSemantic.SERVES_REQUEST.id())
                ? java.util.List.of(ArchitecturalRelationshipSemantic.SERVES_REQUEST.id())
                : null,
            null,
            ArchitectureViewpointDerivationSupport.evidenceSources(evidence, hasEntryPoints, false, false, false)
        );
    }

    private static List<String> preferredEntrypointSeeds(ViewpointEvidence evidence) {
        Set<String> apiEntrypointIds = evidence.entityIdsForRole(ArchitecturalRole.API_ENTRYPOINT.id()).stream()
            .collect(Collectors.toSet());
        List<String> endpointIds = evidence.entities().stream()
            .filter(ApiSurfaceViewpointDeriver::isNonTestEntrypoint)
            .filter(ApiSurfaceViewpointDeriver::isJavaEndpoint)
            .map(ArchitectureEntity::id)
            .sorted()
            .toList();
        if (!endpointIds.isEmpty()) {
            return endpointIds;
        }
        List<String> resourceIds = evidence.entities().stream()
            .filter(entity -> apiEntrypointIds.contains(entity.id()))
            .filter(ApiSurfaceViewpointDeriver::isNonTestEntrypoint)
            .filter(ApiSurfaceViewpointDeriver::isConcreteResourceEntrypoint)
            .map(ArchitectureEntity::id)
            .sorted()
            .toList();
        if (!resourceIds.isEmpty()) {
            return resourceIds;
        }
        return java.util.List.of();
    }


    private static boolean isJavaEndpoint(ArchitectureEntity entity) {
        if (entity == null || entity.kind() != EntityKind.ENDPOINT) {
            return false;
        }
        return metadataEquals(entity, "sourceLanguage", "java")
            || metadataEquals(entity, "language", "java")
            || metadataEquals(entity, "ownerLanguage", "java")
            || containsMetadata(entity, "framework", "jax-rs")
            || containsMetadata(entity, "frameworks", "jax-rs")
            || stringMetadata(entity, "path") != null
            || stringMetadata(entity, "httpMethod") != null;
    }

    private static boolean isNonTestEntrypoint(ArchitectureEntity entity) {
        return entity != null && !isTestPath(stringMetadata(entity, "relativePath"))
            && !isTestPath(stringMetadata(entity, "sourcePath"))
            && !isTestPath(stringMetadata(entity, "filePath"))
            && entity.sourceRefs().stream().noneMatch(ref -> isTestPath(ref.path()));
    }

    private static boolean isConcreteResourceEntrypoint(ArchitectureEntity entity) {
        if (entity == null || entity.kind() != EntityKind.SERVICE) {
            return false;
        }
        String entityRole = stringMetadata(entity, "entityRole");
        String backendProfile = stringMetadata(entity, "backendProfile");
        return "resource".equalsIgnoreCase(entityRole)
            || (backendProfile != null && backendProfile.toLowerCase(Locale.ROOT).contains("resource"));
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


    private static boolean metadataEquals(ArchitectureEntity entity, String key, String expected) {
        return expected.equalsIgnoreCase(String.valueOf(entity.metadata().getOrDefault(key, "")));
    }

    private static boolean containsMetadata(ArchitectureEntity entity, String key, String expected) {
        Object value = entity.metadata().get(key);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).anyMatch(item -> expected.equalsIgnoreCase(item));
        }
        return expected.equalsIgnoreCase(String.valueOf(value));
    }

    private static String stringMetadata(ArchitectureEntity entity, String key) {
        Object value = entity.metadata().get(key);
        return value == null ? null : String.valueOf(value);
    }
}
