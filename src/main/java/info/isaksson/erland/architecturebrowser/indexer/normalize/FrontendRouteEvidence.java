package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Internal normalized-access helper for frontend route/page evidence.
 *
 * <p>This type is intentionally not exported. It gives normalization rules a single seam for
 * reading route-related metadata that was emitted during frontend extraction without requiring the
 * rules to hardcode many raw metadata keys.</p>
 */
public record FrontendRouteEvidence(
    String framework,
    String routeSourceKind,
    String routeDeclarationKind,
    String routeFullPath,
    String routePath,
    String redirectTargetLiteral,
    String guardReference
) {
    public static Optional<FrontendRouteEvidence> fromEntity(ArchitectureEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        Map<String, Object> metadata = entity.metadata();
        if (!hasAnyRouteSignal(metadata)) {
            return Optional.empty();
        }
        return Optional.of(new FrontendRouteEvidence(
            stringMetadata(metadata, "framework"),
            stringMetadata(metadata, "routeSourceKind"),
            stringMetadata(metadata, "routeDeclarationKind"),
            firstNonBlank(
                stringMetadata(metadata, "routeFullPath"),
                stringMetadata(metadata, "navigationTargetLiteral")
            ),
            stringMetadata(metadata, "routePath"),
            stringMetadata(metadata, "redirectTargetLiteral"),
            stringMetadata(metadata, "guardReference")
        ));
    }

    public static Optional<FrontendRouteEvidence> fromRelationship(ArchitectureRelationship relationship) {
        if (relationship == null) {
            return Optional.empty();
        }
        Map<String, Object> metadata = relationship.metadata();
        if (!hasAnyRouteSignal(metadata)) {
            return Optional.empty();
        }
        return Optional.of(new FrontendRouteEvidence(
            stringMetadata(metadata, "framework"),
            stringMetadata(metadata, "routeSourceKind"),
            stringMetadata(metadata, "routeDeclarationKind"),
            stringMetadata(metadata, "navigationTargetLiteral"),
            stringMetadata(metadata, "routePath"),
            stringMetadata(metadata, "redirectTargetLiteral"),
            stringMetadata(metadata, "guardReference")
        ));
    }

    public boolean declaredRoute() {
        return equalsIgnoreCase(routeSourceKind, "declared-route") || routeDeclarationKind != null;
    }

    public boolean redirect() {
        return equalsIgnoreCase(routeSourceKind, "redirect") || !blank(redirectTargetLiteral);
    }

    public boolean guarded() {
        return !blank(guardReference);
    }

    public boolean frameworkMatches(String expectedFramework) {
        return equalsIgnoreCase(framework, expectedFramework);
    }

    private static boolean hasAnyRouteSignal(Map<String, Object> metadata) {
        return !blank(stringMetadata(metadata, "routeSourceKind"))
            || !blank(stringMetadata(metadata, "routeDeclarationKind"))
            || !blank(stringMetadata(metadata, "routeFullPath"))
            || !blank(stringMetadata(metadata, "routePath"))
            || !blank(stringMetadata(metadata, "redirectTargetLiteral"))
            || !blank(stringMetadata(metadata, "guardReference"));
    }

    private static String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
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

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }
}
