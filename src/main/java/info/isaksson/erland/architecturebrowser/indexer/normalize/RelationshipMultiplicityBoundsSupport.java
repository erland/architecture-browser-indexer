package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Objects;

/**
 * Generic support for reading and conservatively combining multiplicity bounds on relationships.
 * <p>
 * This is intentionally framework-agnostic so the same logic can be reused by other normalization
 * passes that merge inverse relationships emitted from non-JPA frameworks.
 */
final class RelationshipMultiplicityBoundsSupport {
    private RelationshipMultiplicityBoundsSupport() {}

    static RelationshipEndBounds boundsForRelationship(ArchitectureRelationship relationship) {
        if (relationship == null || relationship.metadata() == null) {
            return new RelationshipEndBounds(null, null, null, null);
        }
        return new RelationshipEndBounds(
            stringValue(relationship.metadata().get("sourceLowerBound")),
            stringValue(relationship.metadata().get("sourceUpperBound")),
            stringValue(relationship.metadata().get("targetLowerBound")),
            stringValue(relationship.metadata().get("targetUpperBound"))
        );
    }

    static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isBlank() ? null : string;
    }

    static String normalizeBound(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    static String multiplicityLowerForEntity(ArchitectureRelationship relationship, String entityId, RelationshipEndBounds bounds) {
        if (relationship == null || entityId == null || bounds == null) {
            return null;
        }
        if (entityId.equals(relationship.fromEntityId())) {
            return bounds.sourceLowerBound();
        }
        if (entityId.equals(relationship.toEntityId())) {
            return bounds.targetLowerBound();
        }
        return null;
    }

    static String multiplicityUpperForEntity(ArchitectureRelationship relationship, String entityId, RelationshipEndBounds bounds) {
        if (relationship == null || entityId == null || bounds == null) {
            return null;
        }
        if (entityId.equals(relationship.fromEntityId())) {
            return bounds.sourceUpperBound();
        }
        if (entityId.equals(relationship.toEntityId())) {
            return bounds.targetUpperBound();
        }
        return null;
    }

    static String combineLowerBound(String first, String second) {
        return chooseConservativeBound(first, second, true);
    }

    static String combineUpperBound(String first, String second) {
        return chooseConservativeBound(first, second, false);
    }

    private static String chooseConservativeBound(String first, String second, boolean lowerBound) {
        if (first == null || first.isBlank()) {
            return normalizeBound(second);
        }
        if (second == null || second.isBlank()) {
            return normalizeBound(first);
        }
        String normalizedFirst = normalizeBound(first);
        String normalizedSecond = normalizeBound(second);
        if (Objects.equals(normalizedFirst, normalizedSecond)) {
            return normalizedFirst;
        }
        if (lowerBound) {
            Integer firstRank = lowerBoundRank(normalizedFirst);
            Integer secondRank = lowerBoundRank(normalizedSecond);
            if (firstRank != null && secondRank != null) {
                return firstRank <= secondRank ? normalizedFirst : normalizedSecond;
            }
            return "0";
        }
        Integer firstRank = upperBoundRank(normalizedFirst);
        Integer secondRank = upperBoundRank(normalizedSecond);
        if (firstRank != null && secondRank != null) {
            return firstRank >= secondRank ? normalizedFirst : normalizedSecond;
        }
        return "*";
    }

    private static Integer lowerBoundRank(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "0" -> 0;
            case "1" -> 1;
            default -> parseIntegerOrNull(value);
        };
    }

    private static Integer upperBoundRank(String value) {
        if (value == null) {
            return null;
        }
        if ("*".equals(value)) {
            return Integer.MAX_VALUE;
        }
        return parseIntegerOrNull(value);
    }

    private static Integer parseIntegerOrNull(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    record RelationshipEndBounds(String sourceLowerBound, String sourceUpperBound, String targetLowerBound, String targetUpperBound) {}
}
