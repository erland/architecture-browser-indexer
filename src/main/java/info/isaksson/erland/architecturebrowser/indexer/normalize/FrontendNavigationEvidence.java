package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Internal normalized-access helper for frontend navigation/link evidence.
 */
public record FrontendNavigationEvidence(
    String framework,
    String routeSourceKind,
    String navigationTargetLiteral,
    String emittedRelationshipKind
) {
    public static Optional<FrontendNavigationEvidence> fromRelationship(ArchitectureRelationship relationship) {
        if (relationship == null) {
            return Optional.empty();
        }
        Map<String, Object> metadata = relationship.metadata();
        if (!hasAnyNavigationSignal(metadata)) {
            return Optional.empty();
        }
        return Optional.of(new FrontendNavigationEvidence(
            stringMetadata(metadata, "framework"),
            stringMetadata(metadata, "routeSourceKind"),
            stringMetadata(metadata, "navigationTargetLiteral"),
            stringMetadata(metadata, "emittedRelationshipKind")
        ));
    }

    public boolean staticLink() {
        return equalsIgnoreCase(routeSourceKind, "link") || equalsIgnoreCase(emittedRelationshipKind, "linksToRoute");
    }

    public boolean imperativeNavigation() {
        return equalsIgnoreCase(routeSourceKind, "navigate") || equalsIgnoreCase(emittedRelationshipKind, "navigatesToRoute");
    }

    public boolean frameworkMatches(String expectedFramework) {
        return equalsIgnoreCase(framework, expectedFramework);
    }

    private static boolean hasAnyNavigationSignal(Map<String, Object> metadata) {
        return !blank(stringMetadata(metadata, "navigationTargetLiteral"))
            || !blank(stringMetadata(metadata, "emittedRelationshipKind"))
            || equalsIgnoreCase(stringMetadata(metadata, "routeSourceKind"), "link")
            || equalsIgnoreCase(stringMetadata(metadata, "routeSourceKind"), "navigate");
    }

    private static String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.toLowerCase(Locale.ROOT).equals(right.toLowerCase(Locale.ROOT));
    }
}
