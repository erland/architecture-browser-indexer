package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

final class JavaJpaAssociationMetadataSupport {
    static final Pattern FIELD_NAME_PATTERN = Pattern.compile("(?:private|protected|public)?\\s*(?:static\\s+)?(?:final\\s+)?[\\w<>\\[\\], ?]+\\s+(\\w+)\\s*(?:=|;)");

    private JavaJpaAssociationMetadataSupport() {}

    static boolean isJpaAssociation(ArchitectureRelationship relationship) {
        return relationship != null
            && "jpa".equalsIgnoreCase(stringValue(relationship.metadata().get("framework")))
            && "hasAssociation".equalsIgnoreCase(stringValue(relationship.metadata().get("relationshipType")))
            && associationCardinality(relationship) != null;
    }

    static boolean hasMappedBy(ArchitectureRelationship relationship) {
        return normalizedString(valueAtPath(relationship.metadata(), "jpaAssociationEvidence", "mappedBy")) != null
            || normalizedString(relationship.metadata().get("mappedBy")) != null;
    }


    static String inverseSideReference(ArchitectureRelationship relationship) {
        String mappedBy = normalizedString(valueAtPath(relationship.metadata(), "jpaAssociationEvidence", "mappedBy"));
        if (mappedBy != null) {
            return mappedBy;
        }
        return normalizedString(relationship.metadata().get("mappedBy"));
    }

    static String associationCardinality(ArchitectureRelationship relationship) {
        String value = normalizedString(relationship.metadata().get("associationCardinality"));
        if (value != null) {
            return value;
        }
        return normalizedString(valueAtPath(relationship.metadata(), "jpaAssociationEvidence", "associationKind"));
    }

    static Boolean booleanEvidenceOrNull(ArchitectureRelationship relationship, String key) {
        Object nested = valueAtPath(relationship.metadata(), "jpaAssociationEvidence", key);
        if (nested instanceof Boolean value) {
            return value;
        }
        Object flat = relationship.metadata().get(key);
        if (flat instanceof Boolean value) {
            return value;
        }
        if (nested != null) {
            return Boolean.valueOf(String.valueOf(nested));
        }
        if (flat != null) {
            return Boolean.valueOf(String.valueOf(flat));
        }
        return null;
    }

    static boolean booleanEvidence(ArchitectureRelationship relationship, String key) {
        return Boolean.TRUE.equals(booleanEvidenceOrNull(relationship, key));
    }

    static Object valueAtPath(Map<String, Object> metadata, String nestedKey, String key) {
        if (metadata == null) {
            return null;
        }
        Object nested = metadata.get(nestedKey);
        if (nested instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    static String normalizedString(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isBlank() ? null : string.toLowerCase(Locale.ROOT);
    }

    static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isBlank() ? null : string;
    }

    static String propertyNameForRelationship(ArchitectureRelationship relationship) {
        if (relationship == null) {
            return null;
        }
        String propertyName = stringValue(relationship.metadata().get("ownerPropertyName"));
        if (propertyName != null) {
            return propertyName;
        }
        String memberName = stringValue(relationship.metadata().get("ownerMemberName"));
        if (memberName != null) {
            return memberName;
        }
        if (relationship.sourceRefs() != null) {
            for (var ref : relationship.sourceRefs()) {
                String inferred = inferPropertyNameFromSnippet(ref == null ? null : ref.snippet());
                if (inferred != null) {
                    return inferred;
                }
            }
        }
        return null;
    }

    static String inferPropertyNameFromSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = FIELD_NAME_PATTERN.matcher(snippet.replace("\n", " ").replace("\r", " "));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    static boolean mappedByMatches(ArchitectureRelationship inverseSide, ArchitectureRelationship owningSide) {
        String mappedBy = normalizedString(valueAtPath(inverseSide.metadata(), "jpaAssociationEvidence", "mappedBy"));
        if (mappedBy == null) {
            mappedBy = normalizedString(inverseSide.metadata().get("mappedBy"));
        }
        if (mappedBy == null) {
            return false;
        }
        String propertyName = normalizedString(propertyNameForRelationship(owningSide));
        if (Objects.equals(mappedBy, propertyName)) {
            return true;
        }
        String memberName = normalizedString(owningSide.metadata().get("ownerMemberName"));
        return Objects.equals(mappedBy, memberName);
    }
}
