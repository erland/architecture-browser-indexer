package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ArchitectureIrDependencyMetadataSupport {
    private ArchitectureIrDependencyMetadataSupport() {}

    static Map<String, Object> mutableCopy(Map<String, Object> metadata) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (metadata != null) {
            copy.putAll(metadata);
        }
        return copy;
    }

    static Map<String, Object> immutable(Map<String, Object> metadata) {
        return Map.copyOf(metadata);
    }

    static void addIfPresent(Set<String> sink, Object value) {
        if (value == null) {
            return;
        }
        String asString = String.valueOf(value).trim();
        if (!asString.isEmpty()) {
            sink.add(asString);
        }
    }

    static void addFrameworkMetadata(
        Set<String> frameworks,
        Set<String> frameworkRelationships,
        Set<String> architectureViewKinds,
        ArchitectureRelationship relationship
    ) {
        if (relationship == null || relationship.metadata() == null) {
            return;
        }
        Object framework = relationship.metadata().get("framework");
        addIfPresent(frameworks, framework);
        Object frameworkRelationship = relationship.metadata().get("frameworkRelationship");
        Object relationshipType = relationship.metadata().get("relationshipType");
        addIfPresent(frameworkRelationships, frameworkRelationship);
        addIfPresent(frameworkRelationships, relationshipType);
        addArchitectureViewKinds(architectureViewKinds, relationship.kind(), framework, relationship.metadata().get("dependencySource"), frameworkRelationship, relationshipType);
    }

    static void addArchitectureViewKinds(Set<String> sink, RelationshipKind relationshipKind, Object framework, Object dependencySource, Object frameworkRelationship, Object relationshipType) {
        String dependencySourceValue = dependencySource == null ? "" : String.valueOf(dependencySource).trim();
        String frameworkRelationshipValue = frameworkRelationship == null ? "" : String.valueOf(frameworkRelationship).trim();
        String relationshipTypeValue = relationshipType == null ? "" : String.valueOf(relationshipType).trim();
        String frameworkValue = framework == null ? "" : String.valueOf(framework).trim();
        boolean frameworkSpecificSource = dependencySourceValue.startsWith("react:") || dependencySourceValue.startsWith("angular:");
        boolean javaFrameworkSemantic = "jax-rs".equals(frameworkValue) || "jpa".equals(frameworkValue) || "cdi".equals(frameworkValue);
        if (!frameworkRelationshipValue.isEmpty() || !relationshipTypeValue.isEmpty() || frameworkSpecificSource || javaFrameworkSemantic) {
            sink.add("framework");
        }
        if (relationshipKind == RelationshipKind.EXPOSES || "jax-rs".equals(frameworkValue) && ("endpoint".equals(relationshipTypeValue) || relationshipKind == RelationshipKind.EXPOSES)) {
            sink.add("endpoint");
        }
        String javaSemanticKey = !frameworkRelationshipValue.isEmpty() ? frameworkRelationshipValue : relationshipTypeValue;
        switch (javaSemanticKey) {
            case "publishesEvent", "observesEvent", "eventObservedBy" -> sink.add("observer-event");
            case "hasAssociation", "embeds", "inheritsPersistenceModel" -> sink.add("entity-model");
            case "writePath" -> sink.add("write-path");
            default -> {}
        }
        String key = !frameworkRelationshipValue.isEmpty() ? frameworkRelationshipValue : dependencySourceValue;
        if (key.isEmpty()) {
            return;
        }
        switch (key) {
            case "renders", "templateRenders", "usesDirective", "usesPipe", "declares", "imports", "exports", "bootstraps" -> sink.add("composition");
            case "targets", "childOf", "lazyLoads", "guards", "resolves" -> sink.add("route");
            case "provides", "providedBy", "injects", "resolvesTo", "providesContext", "consumesContext" -> sink.add("provider-di");
            case "usesHook" -> sink.add("hook");
            default -> {
                if (dependencySourceValue.contains("route")) {
                    sink.add("route");
                }
            }
        }
    }

    static Map<String, Object> shapeImportEvidenceMetadata(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        Map<String, Object> metadata,
        boolean internalTarget,
        boolean externalTarget,
        String targetBoundary,
        String targetClassification
    ) {
        Map<String, Object> shaped = mutableCopy(metadata);
        shaped.putIfAbsent("dependencyView", "evidence");
        shaped.put("dependencyTier", "supporting-evidence");
        shaped.put("architecturePrimary", false);
        shaped.put("recommendedForArchitectureViews", false);
        shaped.put("dependencyTargetInternal", internalTarget);
        shaped.put("dependencyTargetExternal", externalTarget);
        shaped.put("dependencyTargetBoundary", targetBoundary);
        shaped.put("dependencyTargetClassification", targetClassification);
        shaped.put("evidenceKind", "file-import");
        if (!shaped.containsKey("dependencySources")) {
            Object dependencySource = relationship.metadata() == null ? null : relationship.metadata().get("dependencySource");
            if (dependencySource != null) {
                shaped.put("dependencySources", List.of(String.valueOf(dependencySource)));
            }
        }
        shaped.put("evidenceSourceEntityId", relationship.fromEntityId());
        shaped.put("evidenceTargetEntityId", relationship.toEntityId());
        if (source != null && source.name() != null) {
            shaped.put("evidenceSourceName", source.name());
        }
        if (target != null && target.name() != null) {
            shaped.put("evidenceTargetName", target.name());
        }
        return immutable(shaped);
    }

    static Map<String, Object> putSummaryCollections(
        Map<String, Object> metadata,
        Set<String> dependencySources,
        Set<String> dependencyCategories,
        Set<String> frameworks,
        Set<String> frameworkRelationships,
        Set<String> architectureViewKinds,
        Set<String> evidenceRelationshipIds,
        Set<String> evidenceLabels
    ) {
        metadata.put("dependencySources", List.copyOf(dependencySources));
        metadata.put("dependencyCategories", List.copyOf(dependencyCategories));
        metadata.put("frameworks", List.copyOf(frameworks));
        metadata.put("frameworkRelationships", List.copyOf(frameworkRelationships));
        metadata.put("architectureViewKinds", List.copyOf(architectureViewKinds));
        metadata.put("evidenceRelationshipIds", List.copyOf(evidenceRelationshipIds));
        metadata.put("evidenceLabels", List.copyOf(evidenceLabels));
        return metadata;
    }
}
