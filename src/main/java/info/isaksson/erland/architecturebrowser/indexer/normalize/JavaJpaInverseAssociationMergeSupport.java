package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.NormalizedAssociation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Merges high-confidence inverse JPA field-level associations into one canonical relationship.
 */
final class JavaJpaInverseAssociationMergeSupport {
    private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("(?:private|protected|public)?\\s*(?:static\\s+)?(?:final\\s+)?[\\w<>\\[\\], ?]+\\s+(\\w+)\\s*(?:=|;)");
    private JavaJpaInverseAssociationMergeSupport() {}

    static List<ArchitectureRelationship> mergeInverseJpaAssociations(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return List.of();
        }
        List<ArchitectureRelationship> merged = new ArrayList<>();
        Set<String> consumed = new LinkedHashSet<>();
        for (ArchitectureRelationship relationship : relationships) {
            if (relationship == null || consumed.contains(relationship.id())) {
                continue;
            }
            ArchitectureRelationship inverse = findInversePair(relationship, relationships, consumed);
            if (inverse == null) {
                merged.add(explicitlyHandledRelationship(relationship, relationships));
                consumed.add(relationship.id());
                continue;
            }
            ArchitectureRelationship canonical = canonicalRelationship(relationship, inverse, entitiesById);
            merged.add(canonical);
            consumed.add(relationship.id());
            consumed.add(inverse.id());
        }
        return List.copyOf(merged);
    }

    private static ArchitectureRelationship findInversePair(
        ArchitectureRelationship relationship,
        List<ArchitectureRelationship> relationships,
        Set<String> consumed
    ) {
        for (ArchitectureRelationship candidate : relationships) {
            if (candidate == null || Objects.equals(candidate.id(), relationship.id()) || consumed.contains(candidate.id())) {
                continue;
            }
            if (isInversePair(relationship, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean isInversePair(ArchitectureRelationship left, ArchitectureRelationship right) {
        if (!isJpaAssociation(left) || !isJpaAssociation(right)) {
            return false;
        }
        if (!Objects.equals(left.fromEntityId(), right.toEntityId())
            || !Objects.equals(left.toEntityId(), right.fromEntityId())) {
            return false;
        }
        String leftCardinality = associationCardinality(left);
        String rightCardinality = associationCardinality(right);
        if (leftCardinality == null || rightCardinality == null) {
            return false;
        }
        return switch (leftCardinality) {
            case "many-to-one" -> "one-to-many".equals(rightCardinality) && mappedByMatches(right, left);
            case "one-to-many" -> "many-to-one".equals(rightCardinality) && mappedByMatches(left, right);
            case "one-to-one" -> "one-to-one".equals(rightCardinality)
                && (mappedByMatches(left, right) || mappedByMatches(right, left));
            case "many-to-many" -> "many-to-many".equals(rightCardinality)
                && (mappedByMatches(left, right) || mappedByMatches(right, left));
            default -> false;
        };
    }

    private static boolean mappedByMatches(ArchitectureRelationship inverseSide, ArchitectureRelationship owningSide) {
        String mappedBy = normalizedString(valueAtPath(inverseSide.metadata(), "jpaAssociationEvidence", "mappedBy"));
        if (mappedBy == null) {
            mappedBy = normalizedString(inverseSide.metadata().get("mappedBy"));
        }
        if (mappedBy == null) {
            return false;
        }
        String propertyName = normalizedString(propertyNameForRelationship(owningSide));
        if (mappedBy.equals(propertyName)) {
            return true;
        }
        String memberName = normalizedString(owningSide.metadata().get("ownerMemberName"));
        return mappedBy.equals(memberName);
    }

    private static ArchitectureRelationship canonicalRelationship(
        ArchitectureRelationship left,
        ArchitectureRelationship right,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        ArchitectureRelationship canonicalBase = chooseCanonicalBase(left, right);
        ArchitectureRelationship paired = canonicalBase == left ? right : left;
        String cardinality = associationCardinality(canonicalBase);
        String associationKind = promotedAssociationKind(canonicalBase, paired, cardinality);
        AssociationEndBounds bounds = deriveMergedBounds(canonicalBase, paired);
        List<String> semantics = new ArrayList<>();
        if (canonicalBase.architecturalSemantics() != null) {
            semantics.addAll(canonicalBase.architecturalSemantics());
        }
        if (paired.architecturalSemantics() != null) {
            semantics.addAll(paired.architecturalSemantics());
        }
        List<String> evidenceIds = List.of(canonicalBase.id(), paired.id());
        NormalizedAssociation normalizedAssociation = new NormalizedAssociation(
            associationKind,
            cardinality,
            bounds.sourceLowerBound(),
            bounds.sourceUpperBound(),
            bounds.targetLowerBound(),
            bounds.targetUpperBound(),
            Boolean.TRUE,
            evidenceIds,
            owningSideEntityId(left, right),
            owningSideMemberId(left, right),
            inverseSideEntityId(left, right),
            inverseSideMemberId(left, right)
        );
        return new ArchitectureRelationship(
            canonicalBase.id(),
            canonicalBase.kind(),
            canonicalBase.fromEntityId(),
            canonicalBase.toEntityId(),
            canonicalBase.label(),
            canonicalizedSourceRefs(canonicalBase, paired),
            mergedMetadata(canonicalBase, paired, entitiesById),
            semantics.isEmpty() ? null : semantics,
            normalizedAssociation
        );
    }


    private static ArchitectureRelationship explicitlyHandledRelationship(ArchitectureRelationship relationship, List<ArchitectureRelationship> relationships) {
        if (relationship == null) {
            return null;
        }
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>(relationship.metadata());
        String handlingCategory = explicitHandlingCategory(relationship);
        if (handlingCategory != null) {
            metadata.put("jpaAssociationHandling", handlingCategory);
        }
        if (isExplicitNonPeerJpaValueLikeRelationship(relationship)) {
            metadata.put("jpaAssociationPeerEntity", Boolean.FALSE);
            metadata.put("jpaNonPeerAssociation", Boolean.TRUE);
            return new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                Map.copyOf(metadata),
                relationship.architecturalSemantics(),
                null
            );
        }
        if (!isJpaAssociation(relationship)) {
            return new ArchitectureRelationship(
                relationship.id(),
                relationship.kind(),
                relationship.fromEntityId(),
                relationship.toEntityId(),
                relationship.label(),
                relationship.sourceRefs(),
                Map.copyOf(metadata),
                relationship.architecturalSemantics(),
                relationship.normalizedAssociation()
            );
        }
        metadata.put("jpaAssociationPeerEntity", Boolean.TRUE);
        if (!hasMappedBy(relationship)) {
            metadata.put("jpaAssociationUnidirectional", Boolean.TRUE);
        }
        NormalizedAssociation normalizedAssociation = relationship.normalizedAssociation();
        if (normalizedAssociation == null) {
            metadata.put("jpaAssociationExplicitlyHandled", Boolean.TRUE);
            boolean explicitUnidirectional = !Boolean.TRUE.equals(metadata.get("inverseJpaAssociationMerged"))
                && !metadata.containsKey("mappedBy")
                && valueAtPath(metadata, "jpaAssociationEvidence", "mappedBy") == null
                && !hasAmbiguousSwappedJpaAssociation(relationship, relationships);
            if (explicitUnidirectional) {
                normalizedAssociation = new NormalizedAssociation(
                    existingAssociationKind(relationship, relationship),
                    associationCardinality(relationship),
                    stringValue(metadata.get("sourceLowerBound")),
                    stringValue(metadata.get("sourceUpperBound")),
                    stringValue(metadata.get("targetLowerBound")),
                    stringValue(metadata.get("targetUpperBound")),
                    Boolean.FALSE,
                    List.of(relationship.id()),
                    relationship.fromEntityId(),
                    propertyNameForRelationship(relationship),
                    null,
                    null
                );
            }
        }
        return new ArchitectureRelationship(
            relationship.id(),
            relationship.kind(),
            relationship.fromEntityId(),
            relationship.toEntityId(),
            relationship.label(),
            relationship.sourceRefs(),
            Map.copyOf(metadata),
            relationship.architecturalSemantics(),
            normalizedAssociation
        );
    }

    private static String explicitHandlingCategory(ArchitectureRelationship relationship) {
        String evidenceCategory = normalizedString(valueAtPath(relationship.metadata(), "jpaAssociationEvidence", "handlingCategory"));
        if (evidenceCategory != null && !"peer-entity-association".equals(evidenceCategory)) {
            return evidenceCategory;
        }
        if (isExplicitNonPeerJpaValueLikeRelationship(relationship)) {
            return evidenceCategory == null ? "value-like-non-peer" : evidenceCategory;
        }
        if (isJpaAssociation(relationship)) {
            return hasMappedBy(relationship) ? "inverse-peer-association" : "unidirectional-peer-association";
        }
        return evidenceCategory;
    }

    private static boolean isExplicitNonPeerJpaValueLikeRelationship(ArchitectureRelationship relationship) {
        return Boolean.TRUE.equals(booleanEvidenceOrNull(relationship, "valueLikeTarget"))
            || Boolean.TRUE.equals(booleanEvidenceOrNull(relationship, "elementCollection"))
            || Boolean.TRUE.equals(booleanEvidenceOrNull(relationship, "embedded"))
            || Boolean.TRUE.equals(booleanEvidenceOrNull(relationship, "embeddedId"));
    }

    private static ArchitectureRelationship chooseCanonicalBase(ArchitectureRelationship left, ArchitectureRelationship right) {
        String leftCardinality = associationCardinality(left);
        String rightCardinality = associationCardinality(right);
        if ("one-to-many".equals(leftCardinality) && "many-to-one".equals(rightCardinality)) {
            return left;
        }
        if ("one-to-many".equals(rightCardinality) && "many-to-one".equals(leftCardinality)) {
            return right;
        }
        if (hasMappedBy(left) && !hasMappedBy(right)) {
            return right;
        }
        if (hasMappedBy(right) && !hasMappedBy(left)) {
            return left;
        }
        return left.id().compareTo(right.id()) <= 0 ? left : right;
    }


    private static AssociationEndBounds deriveMergedBounds(ArchitectureRelationship canonicalBase, ArchitectureRelationship paired) {
        String canonicalCardinality = associationCardinality(canonicalBase);
        String pairedCardinality = associationCardinality(paired);

        if ("one-to-many".equals(canonicalCardinality) && "many-to-one".equals(pairedCardinality)) {
            AssociationEndBounds canonicalBounds = boundsForRelationship(canonicalBase);
            AssociationEndBounds pairedBounds = boundsForRelationship(paired);
            return new AssociationEndBounds(
                pairedBounds.targetLowerBound(),
                pairedBounds.targetUpperBound(),
                canonicalBounds.targetLowerBound(),
                canonicalBounds.targetUpperBound()
            );
        }
        if ("one-to-many".equals(pairedCardinality) && "many-to-one".equals(canonicalCardinality)) {
            AssociationEndBounds canonicalBounds = boundsForRelationship(canonicalBase);
            AssociationEndBounds pairedBounds = boundsForRelationship(paired);
            return new AssociationEndBounds(
                canonicalBounds.sourceLowerBound(),
                canonicalBounds.sourceUpperBound(),
                pairedBounds.sourceLowerBound(),
                pairedBounds.sourceUpperBound()
            );
        }

        AssociationEndBounds canonicalBounds = boundsForRelationship(canonicalBase);
        AssociationEndBounds pairedBounds = boundsForRelationship(paired);
        return new AssociationEndBounds(
            combineLowerBound(
                multiplicityLowerForEntity(canonicalBase, canonicalBase.fromEntityId(), canonicalBounds),
                multiplicityLowerForEntity(paired, canonicalBase.fromEntityId(), pairedBounds)
            ),
            combineUpperBound(
                multiplicityUpperForEntity(canonicalBase, canonicalBase.fromEntityId(), canonicalBounds),
                multiplicityUpperForEntity(paired, canonicalBase.fromEntityId(), pairedBounds)
            ),
            combineLowerBound(
                multiplicityLowerForEntity(canonicalBase, canonicalBase.toEntityId(), canonicalBounds),
                multiplicityLowerForEntity(paired, canonicalBase.toEntityId(), pairedBounds)
            ),
            combineUpperBound(
                multiplicityUpperForEntity(canonicalBase, canonicalBase.toEntityId(), canonicalBounds),
                multiplicityUpperForEntity(paired, canonicalBase.toEntityId(), pairedBounds)
            )
        );
    }

    private static AssociationEndBounds boundsForRelationship(ArchitectureRelationship relationship) {
        return new AssociationEndBounds(
            stringValue(relationship.metadata().get("sourceLowerBound")),
            stringValue(relationship.metadata().get("sourceUpperBound")),
            stringValue(relationship.metadata().get("targetLowerBound")),
            stringValue(relationship.metadata().get("targetUpperBound"))
        );
    }

    private static String multiplicityLowerForEntity(ArchitectureRelationship relationship, String entityId, AssociationEndBounds bounds) {
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

    private static String multiplicityUpperForEntity(ArchitectureRelationship relationship, String entityId, AssociationEndBounds bounds) {
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

    private static String combineLowerBound(String first, String second) {
        return chooseConservativeBound(first, second, true);
    }

    private static String combineUpperBound(String first, String second) {
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

    private static String normalizeBound(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void putIfPresent(java.util.LinkedHashMap<String, Object> metadata, String key, String value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
    private static Map<String, Object> mergedMetadata(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>(canonicalBase.metadata());
        metadata.put("inverseJpaAssociationMerged", Boolean.TRUE);
        metadata.put("jpaAssociationPeerEntity", Boolean.TRUE);
        metadata.put("jpaAssociationHandling", "merged-bidirectional-peer-association");
        metadata.put("inverseJpaAssociationRelationshipIds", List.of(canonicalBase.id(), paired.id()).stream().sorted().toList());
        if (!metadata.containsKey("associationCardinality")) {
            metadata.put("associationCardinality", associationCardinality(canonicalBase));
        }
        String promotedAssociationKind = promotedAssociationKind(canonicalBase, paired, associationCardinality(canonicalBase));
        if (promotedAssociationKind != null) {
            metadata.put("associationKind", promotedAssociationKind);
            if ("containment".equalsIgnoreCase(promotedAssociationKind)) {
                metadata.put("containmentPromoted", Boolean.TRUE);
            }
        }
        AssociationEndBounds bounds = deriveMergedBounds(canonicalBase, paired);
        putIfPresent(metadata, "sourceLowerBound", bounds.sourceLowerBound());
        putIfPresent(metadata, "sourceUpperBound", bounds.sourceUpperBound());
        putIfPresent(metadata, "targetLowerBound", bounds.targetLowerBound());
        putIfPresent(metadata, "targetUpperBound", bounds.targetUpperBound());
        if (entitiesById != null && entitiesById.containsKey(canonicalBase.fromEntityId())) {
            metadata.putIfAbsent("sourceEntityId", canonicalBase.fromEntityId());
        }
        if (entitiesById != null && entitiesById.containsKey(canonicalBase.toEntityId())) {
            metadata.putIfAbsent("targetEntityId", canonicalBase.toEntityId());
        }
        return Map.copyOf(metadata);
    }

    private static List<info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference> canonicalizedSourceRefs(
        ArchitectureRelationship left,
        ArchitectureRelationship right
    ) {
        List<info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference> refs = new ArrayList<>();
        if (left.sourceRefs() != null) {
            refs.addAll(left.sourceRefs());
        }
        if (right.sourceRefs() != null) {
            refs.addAll(right.sourceRefs());
        }
        return refs.stream().distinct().toList();
    }

    private static String existingAssociationKind(ArchitectureRelationship left, ArchitectureRelationship right) {
        String value = stringValue(left.metadata().get("associationKind"));
        if (value != null) {
            return value;
        }
        return stringValue(right.metadata().get("associationKind"));
    }

    private static String promotedAssociationKind(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        String associationCardinality
    ) {
        String existing = normalizedString(existingAssociationKind(canonicalBase, paired));
        if (existing != null && !"association".equals(existing)) {
            return existing;
        }
        if (shouldPromoteToContainment(canonicalBase, paired, associationCardinality)) {
            return "containment";
        }
        return existingAssociationKind(canonicalBase, paired);
    }

    private static boolean shouldPromoteToContainment(
        ArchitectureRelationship canonicalBase,
        ArchitectureRelationship paired,
        String associationCardinality
    ) {
        if (associationCardinality == null || "many-to-many".equals(associationCardinality)) {
            return false;
        }
        ArchitectureRelationship owningSide = owningRelationship(canonicalBase, paired);
        ArchitectureRelationship inverseSide = inverseRelationship(canonicalBase, paired);
        if (owningSide == null || inverseSide == null) {
            return false;
        }
        boolean requiredOwnership = hasRequiredOwnership(owningSide);
        if (!requiredOwnership) {
            return false;
        }
        boolean inverseOrphanRemoval = booleanEvidence(inverseSide, "orphanRemoval");
        boolean inverseCascadeRemove = booleanEvidence(inverseSide, "cascadeRemove") || booleanEvidence(inverseSide, "cascadeAll");
        boolean ownerIdentityBound = booleanEvidence(owningSide, "mapsId") || booleanEvidence(owningSide, "primaryKeyJoinColumn");
        return switch (associationCardinality) {
            case "one-to-many" -> inverseOrphanRemoval && inverseCascadeRemove;
            case "one-to-one" -> ownerIdentityBound || (inverseOrphanRemoval && inverseCascadeRemove);
            default -> false;
        };
    }

    private static ArchitectureRelationship owningRelationship(ArchitectureRelationship first, ArchitectureRelationship second) {
        if (hasMappedBy(first) && !hasMappedBy(second)) {
            return second;
        }
        if (hasMappedBy(second) && !hasMappedBy(first)) {
            return first;
        }
        return null;
    }

    private static ArchitectureRelationship inverseRelationship(ArchitectureRelationship first, ArchitectureRelationship second) {
        if (hasMappedBy(first) && !hasMappedBy(second)) {
            return first;
        }
        if (hasMappedBy(second) && !hasMappedBy(first)) {
            return second;
        }
        return null;
    }

    private static boolean hasRequiredOwnership(ArchitectureRelationship relationship) {
        if (relationship == null) {
            return false;
        }
        Boolean associationOptional = booleanEvidenceOrNull(relationship, "associationOptional");
        Boolean joinColumnNullable = booleanEvidenceOrNull(relationship, "joinColumnNullable");
        if (Boolean.FALSE.equals(associationOptional) || Boolean.FALSE.equals(joinColumnNullable)) {
            return true;
        }
        return "1".equals(normalizeBound(multiplicityLowerForEntity(
            relationship,
            relationship.toEntityId(),
            boundsForRelationship(relationship)
        )));
    }

    private static boolean booleanEvidence(ArchitectureRelationship relationship, String key) {
        return Boolean.TRUE.equals(booleanEvidenceOrNull(relationship, key));
    }

    private static Boolean booleanEvidenceOrNull(ArchitectureRelationship relationship, String key) {
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

    private static boolean isJpaAssociation(ArchitectureRelationship relationship) {
        return relationship != null
            && "jpa".equalsIgnoreCase(stringValue(relationship.metadata().get("framework")))
            && "hasAssociation".equalsIgnoreCase(stringValue(relationship.metadata().get("relationshipType")))
            && associationCardinality(relationship) != null;
    }

    private static boolean hasMappedBy(ArchitectureRelationship relationship) {
        return normalizedString(valueAtPath(relationship.metadata(), "jpaAssociationEvidence", "mappedBy")) != null
            || normalizedString(relationship.metadata().get("mappedBy")) != null;
    }

    private static String associationCardinality(ArchitectureRelationship relationship) {
        String value = normalizedString(relationship.metadata().get("associationCardinality"));
        if (value != null) {
            return value;
        }
        return normalizedString(valueAtPath(relationship.metadata(), "jpaAssociationEvidence", "associationKind"));
    }

    private static String owningSideEntityId(ArchitectureRelationship left, ArchitectureRelationship right) {
        if (hasMappedBy(left) && !hasMappedBy(right)) {
            return right.fromEntityId();
        }
        if (hasMappedBy(right) && !hasMappedBy(left)) {
            return left.fromEntityId();
        }
        return left.fromEntityId();
    }

    private static String inverseSideEntityId(ArchitectureRelationship left, ArchitectureRelationship right) {
        if (hasMappedBy(left) && !hasMappedBy(right)) {
            return left.fromEntityId();
        }
        if (hasMappedBy(right) && !hasMappedBy(left)) {
            return right.fromEntityId();
        }
        return right.fromEntityId();
    }

    private static String owningSideMemberId(ArchitectureRelationship left, ArchitectureRelationship right) {
        if (hasMappedBy(left) && !hasMappedBy(right)) {
            return propertyNameForRelationship(right);
        }
        if (hasMappedBy(right) && !hasMappedBy(left)) {
            return propertyNameForRelationship(left);
        }
        return propertyNameForRelationship(left);
    }

    private static String inverseSideMemberId(ArchitectureRelationship left, ArchitectureRelationship right) {
        if (hasMappedBy(left) && !hasMappedBy(right)) {
            return propertyNameForRelationship(left);
        }
        if (hasMappedBy(right) && !hasMappedBy(left)) {
            return propertyNameForRelationship(right);
        }
        return propertyNameForRelationship(right);
    }



    private static boolean hasAmbiguousSwappedJpaAssociation(
        ArchitectureRelationship relationship,
        List<ArchitectureRelationship> relationships
    ) {
        if (relationship == null || relationships == null) {
            return false;
        }
        for (ArchitectureRelationship candidate : relationships) {
            if (candidate == null || Objects.equals(candidate.id(), relationship.id())) {
                continue;
            }
            if (!isJpaAssociation(candidate)) {
                continue;
            }
            if (Objects.equals(candidate.fromEntityId(), relationship.toEntityId())
                && Objects.equals(candidate.toEntityId(), relationship.fromEntityId())) {
                return true;
            }
        }
        return false;
    }

    private static String propertyNameForRelationship(ArchitectureRelationship relationship) {
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

    private static String inferPropertyNameFromSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = FIELD_NAME_PATTERN.matcher(snippet.replace("\n", " ").replace("\r", " "));
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private record AssociationEndBounds(String sourceLowerBound, String sourceUpperBound, String targetLowerBound, String targetUpperBound) {}

    private static Object valueAtPath(Map<String, Object> metadata, String nestedKey, String key) {
        if (metadata == null) {
            return null;
        }
        Object nested = metadata.get(nestedKey);
        if (nested instanceof Map<?, ?> map) {
            return map.get(key);
        }
        return null;
    }

    private static String normalizedString(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isBlank() ? null : string.toLowerCase(Locale.ROOT);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isBlank() ? null : string;
    }
}
