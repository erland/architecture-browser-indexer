package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class JavaJpaAssociationEvidenceSupport {
    record JpaAssociationEvidence(
        String annotationSimpleName,
        String associationKind,
        String declaredType,
        String targetType,
        boolean collectionValued,
        boolean propertyAccess,
        String propertyName,
        String mappedBy,
        String joinColumn,
        String joinTable,
        Boolean associationOptional,
        Boolean joinColumnNullable,
        boolean orphanRemoval,
        boolean cascadeAll,
        boolean cascadeRemove,
        boolean mapsId,
        boolean primaryKeyJoinColumn,
        boolean elementCollection,
        boolean embedded,
        boolean embeddedId,
        boolean valueLikeTarget,
        boolean peerEntityAssociation,
        boolean inverseMergeEligible,
        String handlingCategory
    ) {
        Map<String, Object> toMetadata() {
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            put(metadata, "annotationSimpleName", annotationSimpleName);
            put(metadata, "associationKind", associationKind);
            put(metadata, "declaredType", declaredType);
            put(metadata, "targetType", targetType);
            metadata.put("collectionValued", collectionValued);
            metadata.put("propertyAccess", propertyAccess);
            put(metadata, "propertyName", propertyName);
            put(metadata, "mappedBy", mappedBy);
            put(metadata, "joinColumn", joinColumn);
            put(metadata, "joinTable", joinTable);
            put(metadata, "associationOptional", associationOptional);
            put(metadata, "joinColumnNullable", joinColumnNullable);
            metadata.put("orphanRemoval", orphanRemoval);
            metadata.put("cascadeAll", cascadeAll);
            metadata.put("cascadeRemove", cascadeRemove);
            metadata.put("mapsId", mapsId);
            metadata.put("primaryKeyJoinColumn", primaryKeyJoinColumn);
            metadata.put("elementCollection", elementCollection);
            metadata.put("embedded", embedded);
            metadata.put("embeddedId", embeddedId);
            metadata.put("valueLikeTarget", valueLikeTarget);
            metadata.put("peerEntityAssociation", peerEntityAssociation);
            metadata.put("inverseMergeEligible", inverseMergeEligible);
            put(metadata, "handlingCategory", handlingCategory);
            return Map.copyOf(metadata);
        }

        private static void put(LinkedHashMap<String, Object> metadata, String key, Object value) {
            if (value != null) {
                metadata.put(key, value);
            }
        }
    }

    private JavaJpaAssociationEvidenceSupport() {}

    static Optional<JpaAssociationEvidence> extractFieldAssociationEvidence(List<String> annotations, String declaredType, String snippet) {
        return extractAssociationEvidence(annotations, declaredType, snippet, false, null);
    }

    static Optional<JpaAssociationEvidence> extractMethodAssociationEvidence(List<String> annotations, String declaredType, String snippet, String propertyName) {
        return extractAssociationEvidence(annotations, declaredType, snippet, true, propertyName);
    }

    private static Optional<JpaAssociationEvidence> extractAssociationEvidence(List<String> annotations, String declaredType, String snippet, boolean propertyAccess, String propertyName) {
        String safeSnippet = snippet == null ? "" : snippet;
        boolean embedded = JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "Embedded") || JavaJpaDomainSemanticsSupport.containsAnnotationSnippet(safeSnippet, "Embedded");
        boolean embeddedId = JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "EmbeddedId") || JavaJpaDomainSemanticsSupport.containsAnnotationSnippet(safeSnippet, "EmbeddedId");
        boolean elementCollection = JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "ElementCollection") || JavaJpaDomainSemanticsSupport.containsAnnotationSnippet(safeSnippet, "ElementCollection");
        Optional<String> associationKind = JavaJpaDomainSemanticsSupport.detectJpaAssociation(annotations, safeSnippet);
        if (associationKind.isEmpty() && !(embedded || embeddedId || elementCollection)) {
            return Optional.empty();
        }
        String normalizedDeclaredType = declaredType == null ? "" : declaredType;
        String targetType = JavaRelationshipEvidenceEmitter.extractReferencedTypes(normalizedDeclaredType).stream().reduce((first, second) -> second).orElse("");
        boolean collectionValued = isCollectionValued(normalizedDeclaredType, associationKind.orElse(null), elementCollection);
        String annotationSimpleName = detectPrimaryAnnotationSimpleName(annotations, safeSnippet, associationKind.orElse(null), elementCollection, embedded, embeddedId);
        boolean cascadeAll = JavaJpaDomainSemanticsSupport.hasCascadeValue(safeSnippet, "ALL");
        boolean cascadeRemove = cascadeAll || JavaJpaDomainSemanticsSupport.hasCascadeValue(safeSnippet, "REMOVE");
        boolean valueLikeTarget = embedded || embeddedId || elementCollection;
        boolean peerEntityAssociation = !valueLikeTarget && associationKind.isPresent();
        boolean inverseMergeEligible = peerEntityAssociation && (
            "one-to-many".equals(associationKind.orElse(null))
                || "many-to-one".equals(associationKind.orElse(null))
                || "one-to-one".equals(associationKind.orElse(null))
                || "many-to-many".equals(associationKind.orElse(null))
        );
        String handlingCategory = valueLikeTarget
            ? (elementCollection ? "value-collection" : embeddedId ? "embedded-identifier" : "embedded-value")
            : peerEntityAssociation ? "peer-entity-association" : "other";
        return Optional.of(new JpaAssociationEvidence(
            annotationSimpleName,
            associationKind.orElse(elementCollection ? "element-collection" : embeddedId ? "embedded-id" : "embedded"),
            normalizedDeclaredType,
            targetType.isBlank() ? null : targetType,
            collectionValued,
            propertyAccess,
            propertyName,
            JavaJpaDomainSemanticsSupport.extractJpaMappedBy(safeSnippet).orElse(null),
            JavaJpaDomainSemanticsSupport.extractJpaJoinColumn(safeSnippet).orElse(null),
            JavaJpaDomainSemanticsSupport.extractJpaJoinTable(safeSnippet).orElse(null),
            JavaJpaDomainSemanticsSupport.extractJpaAssociationOptional(safeSnippet).orElse(null),
            JavaJpaDomainSemanticsSupport.extractJpaJoinColumnNullable(safeSnippet).orElse(null),
            JavaJpaDomainSemanticsSupport.extractJpaOrphanRemoval(safeSnippet).orElse(false),
            cascadeAll,
            cascadeRemove,
            JavaJpaDomainSemanticsSupport.containsAnnotationSnippet(safeSnippet, "MapsId") || JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "MapsId"),
            JavaJpaDomainSemanticsSupport.containsAnnotationSnippet(safeSnippet, "PrimaryKeyJoinColumn") || JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, "PrimaryKeyJoinColumn"),
            elementCollection,
            embedded,
            embeddedId,
            valueLikeTarget,
            peerEntityAssociation,
            inverseMergeEligible,
            handlingCategory
        ));
    }

    private static String detectPrimaryAnnotationSimpleName(List<String> annotations, String snippet, String associationKind, boolean elementCollection, boolean embedded, boolean embeddedId) {
        if (associationKind != null) {
            return switch (associationKind) {
                case "one-to-one" -> "OneToOne";
                case "one-to-many" -> "OneToMany";
                case "many-to-one" -> "ManyToOne";
                case "many-to-many" -> "ManyToMany";
                default -> null;
            };
        }
        if (elementCollection) {
            return "ElementCollection";
        }
        if (embeddedId) {
            return "EmbeddedId";
        }
        if (embedded) {
            return "Embedded";
        }
        for (String candidate : List.of("OneToOne", "OneToMany", "ManyToOne", "ManyToMany", "ElementCollection", "EmbeddedId", "Embedded")) {
            if (JavaJpaDomainSemanticsSupport.hasAnnotation(annotations, candidate) || JavaJpaDomainSemanticsSupport.containsAnnotationSnippet(snippet, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isCollectionValued(String declaredType, String associationKind, boolean elementCollection) {
        String lowered = declaredType == null ? "" : declaredType.toLowerCase(Locale.ROOT);
        if ("one-to-many".equals(associationKind) || "many-to-many".equals(associationKind) || elementCollection) {
            return true;
        }
        return lowered.startsWith("list<")
            || lowered.startsWith("set<")
            || lowered.startsWith("collection<")
            || lowered.startsWith("iterable<")
            || lowered.startsWith("map<")
            || lowered.startsWith("java.util.list<")
            || lowered.startsWith("java.util.set<")
            || lowered.startsWith("java.util.collection<")
            || lowered.startsWith("java.lang.iterable<")
            || lowered.startsWith("java.util.map<");
    }
}
