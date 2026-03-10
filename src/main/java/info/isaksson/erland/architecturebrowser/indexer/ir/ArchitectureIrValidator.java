package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.LogicalScope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ArchitectureIrValidator {
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
        }
        Set<String> entityIds = document.entities().stream().map(ArchitectureEntity::id).collect(java.util.stream.Collectors.toSet());
        for (ArchitectureRelationship relationship : document.relationships()) {
            requireUniqueId(relationship.id(), "relationship", ids, messages);
            if (!entityIds.contains(relationship.fromEntityId())) {
                messages.add("relationship references missing fromEntityId: " + relationship.id());
            }
            if (!entityIds.contains(relationship.toEntityId())) {
                messages.add("relationship references missing toEntityId: " + relationship.id());
            }
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ValidationResult(boolean isValid, List<String> messages) {
    }
}
