package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.NormalizedAssociation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Builds browser-facing catalogs for canonical normalized associations so downstream
 * entity/persistence views can prefer a single normalized edge while keeping the raw
 * relationships as provenance via evidence ids.
 */
final class ArchitectureIrNormalizedAssociationCatalogSupport {
    private ArchitectureIrNormalizedAssociationCatalogSupport() {
    }

    static Map<String, Object> build(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        List<Map<String, Object>> entityAssociationRelationships = buildEntityAssociationRelationships(relationships, entitiesById);
        Map<String, Object> catalogs = buildRelationshipCatalogs(entityAssociationRelationships);
        if (entityAssociationRelationships.isEmpty() && catalogs.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        if (!entityAssociationRelationships.isEmpty()) {
            result.put("entityAssociationRelationships", List.copyOf(entityAssociationRelationships));
        }
        if (!catalogs.isEmpty()) {
            result.put("relationshipCatalogs", catalogs);
        }
        return Map.copyOf(result);
    }

    private static List<Map<String, Object>> buildEntityAssociationRelationships(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        for (ArchitectureRelationship relationship : relationships) {
            if (!isCanonicalEntityAssociation(relationship)) {
                continue;
            }
            entries.add(toEntityAssociationEntry(relationship, entitiesById));
        }
        return List.copyOf(entries);
    }

    private static boolean isCanonicalEntityAssociation(ArchitectureRelationship relationship) {
        if (relationship == null || relationship.normalizedAssociation() == null) {
            return false;
        }
        if (relationship.id() != null && relationship.id().startsWith("rel:topology-")) {
            return false;
        }
        Map<String, Object> metadata = relationship.metadata();
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        if (Boolean.TRUE.equals(metadata.get("jpaNonPeerAssociation"))) {
            return false;
        }
        String handling = stringValue(metadata.get("jpaAssociationHandling"));
        if (handling != null && ("value-like-non-peer".equalsIgnoreCase(handling)
            || "value-collection".equalsIgnoreCase(handling)
            || "embedded-value".equalsIgnoreCase(handling)
            || "embedded-identifier".equalsIgnoreCase(handling))) {
            return false;
        }
        String framework = stringValue(metadata.get("framework"));
        if (!"jpa".equalsIgnoreCase(framework)) {
            return false;
        }
        String relationshipType = stringValue(metadata.get("relationshipType"));
        return "hasAssociation".equalsIgnoreCase(relationshipType);
    }

    private static Map<String, Object> toEntityAssociationEntry(
        ArchitectureRelationship relationship,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        NormalizedAssociation normalized = relationship.normalizedAssociation();
        ArchitectureEntity source = entitiesById == null ? null : entitiesById.get(relationship.fromEntityId());
        ArchitectureEntity target = entitiesById == null ? null : entitiesById.get(relationship.toEntityId());

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("relationshipId", relationship.id());
        entry.put("sourceEntityId", relationship.fromEntityId());
        entry.put("targetEntityId", relationship.toEntityId());
        putIfPresent(entry, "sourceEntityName", source == null ? null : source.name());
        putIfPresent(entry, "targetEntityName", target == null ? null : target.name());
        entry.put("framework", "jpa");
        entry.put("relationshipType", "normalizedAssociation");
        entry.put("browserViewKind", "relationship-catalog");
        entry.put("architectureViewKinds", List.of("entity-model"));
        putIfPresent(entry, "relationshipKind", relationship.kind() == null ? null : relationship.kind().name());
        putIfPresent(entry, "label", relationship.label());
        putIfPresent(entry, "associationKind", normalized.associationKind());
        putIfPresent(entry, "associationCardinality", normalized.associationCardinality());
        putIfPresent(entry, "sourceLowerBound", normalized.sourceLowerBound());
        putIfPresent(entry, "sourceUpperBound", normalized.sourceUpperBound());
        putIfPresent(entry, "targetLowerBound", normalized.targetLowerBound());
        putIfPresent(entry, "targetUpperBound", normalized.targetUpperBound());
        if (normalized.bidirectional() != null) {
            entry.put("bidirectional", normalized.bidirectional());
        }
        putIfPresent(entry, "owningSideEntityId", normalized.owningSideEntityId());
        putIfPresent(entry, "owningSideMemberId", normalized.owningSideMemberId());
        putIfPresent(entry, "inverseSideEntityId", normalized.inverseSideEntityId());
        putIfPresent(entry, "inverseSideMemberId", normalized.inverseSideMemberId());
        List<String> evidenceIds = normalized.evidenceRelationshipIds() == null ? List.of() : normalized.evidenceRelationshipIds();
        entry.put("evidenceRelationshipIds", List.copyOf(evidenceIds));
        entry.put("evidenceRelationshipCount", evidenceIds.size());
        entry.put("canonicalRelationshipId", relationship.id());
        entry.put("recommendedForArchitectureViews", Boolean.TRUE);
        entry.put("canonicalForEntityViews", Boolean.TRUE);
        entry.put("rawRelationshipEvidenceRetained", Boolean.TRUE);

        Map<String, Object> metadata = relationship.metadata();
        if (metadata != null) {
            putIfPresent(entry, "jpaAssociationHandling", stringValue(metadata.get("jpaAssociationHandling")));
            putIfPresent(entry, "sourceEntityScopeId", stringValue(metadata.get("sourceEntityScopeId")));
            putIfPresent(entry, "targetEntityScopeId", stringValue(metadata.get("targetEntityScopeId")));
        }
        return Map.copyOf(entry);
    }

    private static Map<String, Object> buildRelationshipCatalogs(List<Map<String, Object>> entityAssociationRelationships) {
        if (entityAssociationRelationships == null || entityAssociationRelationships.isEmpty()) {
            return Map.of();
        }
        LinkedHashSet<String> cardinalities = new LinkedHashSet<>();
        LinkedHashSet<String> kinds = new LinkedHashSet<>();
        for (Map<String, Object> entry : entityAssociationRelationships) {
            addIfPresent(cardinalities, entry.get("associationCardinality"));
            addIfPresent(kinds, entry.get("associationKind"));
        }
        Map<String, Object> entityAssociations = new LinkedHashMap<>();
        entityAssociations.put("id", "entityAssociationRelationships");
        entityAssociations.put("title", "Entity association relationships");
        entityAssociations.put("description", "Canonical normalized entity associations prepared for browser-native persistence and entity-model views.");
        entityAssociations.put("relationshipCatalogKind", "entity-associations");
        entityAssociations.put("browserViewKind", "relationship-catalog");
        entityAssociations.put("framework", "jpa");
        entityAssociations.put("frameworks", List.of("jpa"));
        entityAssociations.put("architectureViewKinds", List.of("entity-model"));
        entityAssociations.put("available", Boolean.TRUE);
        entityAssociations.put("relationshipCount", entityAssociationRelationships.size());
        entityAssociations.put("associationCardinalities", List.copyOf(cardinalities));
        entityAssociations.put("associationKinds", List.copyOf(kinds));
        entityAssociations.put("recommendedForArchitectureViews", Boolean.TRUE);
        entityAssociations.put("canonicalForEntityViews", Boolean.TRUE);
        entityAssociations.put("retainsRawRelationshipEvidence", Boolean.TRUE);
        return Map.of("entityAssociations", Map.copyOf(entityAssociations));
    }

    private static void addIfPresent(LinkedHashSet<String> sink, Object value) {
        String string = stringValue(value);
        if (string != null) {
            sink.add(string);
        }
    }

    private static void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String string = String.valueOf(value).trim();
        return string.isBlank() ? null : string;
    }
}
