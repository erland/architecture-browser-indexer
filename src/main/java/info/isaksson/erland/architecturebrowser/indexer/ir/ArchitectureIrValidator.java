package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ArchitectureIrValidator {
    /**
     * Step 1 baseline: validation currently enforces only the existing stable export shape.
     * Future normalization fields should remain optional at introduction time and extend these
     * checks only when present so unchanged examples and fixtures keep validating.
     */
    private ArchitectureIrValidator() {
    }

    public static ValidationResult validate(ArchitectureIndexDocument document) {
        List<String> messages = new ArrayList<>();
        if (document == null) {
            messages.add("document must not be null");
            return new ValidationResult(false, messages);
        }
        if (isBlank(document.schemaVersion())) {
            messages.add("schemaVersion must be present");
        }
        if (isBlank(document.indexerVersion())) {
            messages.add("indexerVersion must be present");
        }
        if (document.runMetadata() == null) {
            messages.add("runMetadata must be present");
        }
        if (document.source() == null) {
            messages.add("source must be present");
        }
        if (document.completeness() == null) {
            messages.add("completeness must be present");
        }

        Set<String> ids = new HashSet<>();
        for (LogicalScope scope : document.scopes()) {
            requireUniqueId(scope.id(), "scope", ids, messages);
            if (isBlank(scope.name())) {
                messages.add("scope name must be present for " + scope.id());
            }
        }
        for (ArchitectureEntity entity : document.entities()) {
            requireUniqueId(entity.id(), "entity", ids, messages);
            if (isBlank(entity.name())) {
                messages.add("entity name must be present for " + entity.id());
            }
            validateNormalizedEntityStrings(entity.id(), "architecturalRoles", entity.architecturalRoles(), messages);
            validateNormalizedEntityStrings(entity.id(), "architecturalTraits", entity.architecturalTraits(), messages);
        }
        Set<String> entityIds = document.entities().stream().map(ArchitectureEntity::id).collect(java.util.stream.Collectors.toSet());
        validateViewpoints(document.viewpoints(), document.entities(), document.relationships(), messages);

        for (ArchitectureRelationship relationship : document.relationships()) {
            requireUniqueId(relationship.id(), "relationship", ids, messages);
            if (!entityIds.contains(relationship.fromEntityId())) {
                messages.add("relationship references missing fromEntityId: " + relationship.id());
            }
            if (!entityIds.contains(relationship.toEntityId())) {
                messages.add("relationship references missing toEntityId: " + relationship.id());
            }
            validateNormalizedRelationshipStrings(relationship.id(), "architecturalSemantics", relationship.architecturalSemantics(), messages);
            validateNormalizedAssociation(relationship, entityIds, messages);
        }
        return new ValidationResult(messages.isEmpty(), List.copyOf(messages));
    }

    private static void requireUniqueId(String id, String type, Set<String> ids, List<String> messages) {
        if (isBlank(id)) {
            messages.add(type + " id must be present");
            return;
        }
        if (!ids.add(id)) {
            messages.add("duplicate id within payload: " + id);
        }
    }

    private static void validateNormalizedEntityStrings(String entityId, String fieldName, List<String> values, List<String> messages) {
        if (values == null) {
            return;
        }
        Set<String> observed = new HashSet<>();
        for (String value : values) {
            if (isBlank(value)) {
                messages.add(fieldName + " must not contain blank values for " + entityId);
                continue;
            }
            String normalized = value.trim();
            if (!observed.add(normalized)) {
                messages.add(fieldName + " must not contain duplicates for " + entityId + ": " + normalized);
            }
        }
    }


    private static void validateViewpoints(
        List<ArchitectureViewpoint> viewpoints,
        List<ArchitectureEntity> entities,
        List<ArchitectureRelationship> relationships,
        List<String> messages
    ) {
        if (viewpoints == null) {
            return;
        }

        Set<String> entityIds = entities.stream().map(ArchitectureEntity::id).collect(java.util.stream.Collectors.toSet());
        Set<String> relationshipSemantics = relationships.stream()
            .map(ArchitectureRelationship::architecturalSemantics)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toSet());
        Set<String> viewpointIds = new HashSet<>();
        for (ArchitectureViewpoint viewpoint : viewpoints) {
            if (viewpoint == null) {
                messages.add("viewpoints must not contain null entries");
                continue;
            }
            if (isBlank(viewpoint.id())) {
                messages.add("viewpoint id must be present");
            } else if (!viewpointIds.add(viewpoint.id())) {
                messages.add("duplicate viewpoint id within payload: " + viewpoint.id());
            }
            if (isBlank(viewpoint.title())) {
                messages.add("viewpoint title must be present for " + Objects.toString(viewpoint.id(), "<unknown>"));
            }
            if (isBlank(viewpoint.description())) {
                messages.add("viewpoint description must be present for " + Objects.toString(viewpoint.id(), "<unknown>"));
            }
            if (isBlank(viewpoint.availability())) {
                messages.add("viewpoint availability must be present for " + Objects.toString(viewpoint.id(), "<unknown>"));
            }
            if (viewpoint.confidence() == null) {
                messages.add("viewpoint confidence must be present for " + Objects.toString(viewpoint.id(), "<unknown>"));
            } else if (viewpoint.confidence() < 0.0 || viewpoint.confidence() > 1.0) {
                messages.add("viewpoint confidence must be between 0.0 and 1.0 for " + Objects.toString(viewpoint.id(), "<unknown>"));
            }

            validateNormalizedViewpointStrings(viewpoint.id(), "seedEntityIds", viewpoint.seedEntityIds(), messages);
            validateNormalizedViewpointStrings(viewpoint.id(), "seedRoleIds", viewpoint.seedRoleIds(), messages);
            validateNormalizedViewpointStrings(viewpoint.id(), "expandViaSemantics", viewpoint.expandViaSemantics(), messages);
            validateNormalizedViewpointStrings(viewpoint.id(), "preferredDependencyViews", viewpoint.preferredDependencyViews(), messages);
            validateNormalizedViewpointStrings(viewpoint.id(), "evidenceSources", viewpoint.evidenceSources(), messages);

            if (viewpoint.seedEntityIds() != null) {
                for (String entityId : viewpoint.seedEntityIds()) {
                    if (!entityIds.contains(entityId)) {
                        messages.add("viewpoint references missing seedEntityId for " + Objects.toString(viewpoint.id(), "<unknown>") + ": " + entityId);
                    }
                }
            }
            if (viewpoint.expandViaSemantics() != null) {
                for (String semantic : viewpoint.expandViaSemantics()) {
                    if (!relationshipSemantics.contains(semantic)) {
                        messages.add("viewpoint references unknown expandViaSemantics value for " + Objects.toString(viewpoint.id(), "<unknown>") + ": " + semantic);
                    }
                }
            }
        }
    }

    private static void validateNormalizedViewpointStrings(String viewpointId, String fieldName, List<String> values, List<String> messages) {
        if (values == null) {
            return;
        }
        Set<String> observed = new HashSet<>();
        for (String value : values) {
            if (isBlank(value)) {
                messages.add(fieldName + " must not contain blank values for viewpoint " + Objects.toString(viewpointId, "<unknown>"));
                continue;
            }
            String normalized = value.trim();
            if (!observed.add(normalized)) {
                messages.add(fieldName + " must not contain duplicates for viewpoint " + Objects.toString(viewpointId, "<unknown>") + ": " + normalized);
            }
        }
    }

    private static void validateNormalizedRelationshipStrings(String relationshipId, String fieldName, List<String> values, List<String> messages) {
        if (values == null) {
            return;
        }
        Set<String> observed = new HashSet<>();
        for (String value : values) {
            if (isBlank(value)) {
                messages.add(fieldName + " must not contain blank values for " + relationshipId);
                continue;
            }
            String normalized = value.trim();
            if (!observed.add(normalized)) {
                messages.add(fieldName + " must not contain duplicates for " + relationshipId + ": " + normalized);
            }
        }
    }

    private static void validateNormalizedAssociation(ArchitectureRelationship relationship, Set<String> entityIds, List<String> messages) {
        if (relationship.normalizedAssociation() == null) {
            return;
        }
        var association = relationship.normalizedAssociation();
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.associationKind", association.associationKind(), messages);
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.associationCardinality", association.associationCardinality(), messages);
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.sourceLowerBound", association.sourceLowerBound(), messages);
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.sourceUpperBound", association.sourceUpperBound(), messages);
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.targetLowerBound", association.targetLowerBound(), messages);
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.targetUpperBound", association.targetUpperBound(), messages);
        validateNormalizedRelationshipStrings(relationship.id(), "normalizedAssociation.evidenceRelationshipIds", association.evidenceRelationshipIds(), messages);
        validateOptionalEntityReference(relationship.id(), "normalizedAssociation.owningSideEntityId", association.owningSideEntityId(), entityIds, messages);
        validateOptionalEntityReference(relationship.id(), "normalizedAssociation.inverseSideEntityId", association.inverseSideEntityId(), entityIds, messages);
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.owningSideMemberId", association.owningSideMemberId(), messages);
        validateOptionalNormalizedString(relationship.id(), "normalizedAssociation.inverseSideMemberId", association.inverseSideMemberId(), messages);
    }

    private static void validateOptionalNormalizedString(String relationshipId, String fieldName, String value, List<String> messages) {
        if (value == null) {
            return;
        }
        if (isBlank(value)) {
            messages.add(fieldName + " must not be blank for " + relationshipId);
        }
    }

    private static void validateOptionalEntityReference(String relationshipId, String fieldName, String entityId, Set<String> entityIds, List<String> messages) {
        if (entityId == null) {
            return;
        }
        if (isBlank(entityId)) {
            messages.add(fieldName + " must not be blank for " + relationshipId);
            return;
        }
        if (!entityIds.contains(entityId)) {
            messages.add(fieldName + " references missing entity for " + relationshipId + ": " + entityId);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ValidationResult(boolean isValid, List<String> messages) {
    }
}
