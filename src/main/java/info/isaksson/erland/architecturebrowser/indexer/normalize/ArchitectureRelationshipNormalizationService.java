package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central seam for converting interpreted or framework-specific evidence into canonical
 * architecture-facing relationship semantics.
 *
 * <p>Step 6 enables a conservative Java-first rule by default while preserving opt-in composition
 * for additional rules.</p>
 */
public final class ArchitectureRelationshipNormalizationService {
    private static final ArchitectureRelationshipNormalizationService DEFAULT = new ArchitectureRelationshipNormalizationService(List.of(
        new JavaArchitectureRelationshipNormalizationRule(),
        new TypeScriptArchitectureRelationshipNormalizationRule(),
        new SqlConfigArchitectureRelationshipNormalizationRule()
    ));

    private final List<ArchitectureRelationshipNormalizationRule> rules;

    private ArchitectureRelationshipNormalizationService(List<ArchitectureRelationshipNormalizationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static ArchitectureRelationshipNormalizationService defaultService() {
        return DEFAULT;
    }

    public static ArchitectureRelationshipNormalizationService of(List<ArchitectureRelationshipNormalizationRule> rules) {
        return new ArchitectureRelationshipNormalizationService(rules == null ? List.of() : rules);
    }

    public ArchitectureRelationship normalizeRelationship(
        ArchitectureRelationship relationship,
        Map<String, ArchitectureEntity> entitiesById,
        Map<String, ArchitectureRelationship> relationshipsById
    ) {
        if (relationship == null) {
            return null;
        }
        List<String> semantics = new ArrayList<>();
        if (relationship.architecturalSemantics() != null) {
            semantics.addAll(relationship.architecturalSemantics());
        }

        ArchitectureRelationshipNormalizationContext context = new ArchitectureRelationshipNormalizationContext(
            relationship,
            entitiesById,
            relationshipsById
        );
        for (ArchitectureRelationshipNormalizationRule rule : rules) {
            if (rule == null) {
                continue;
            }
            NormalizedArchitectureRelationship normalized = rule.normalize(context);
            if (normalized == null || normalized.architecturalSemantics() == null) {
                continue;
            }
            semantics.addAll(normalized.architecturalSemantics());
        }

        return new ArchitectureRelationship(
            relationship.id(),
            relationship.kind(),
            relationship.fromEntityId(),
            relationship.toEntityId(),
            relationship.label(),
            relationship.sourceRefs(),
            relationship.metadata(),
            semantics.isEmpty() ? null : semantics
        );
    }

    public List<ArchitectureRelationship> normalizeRelationships(
        List<ArchitectureRelationship> relationships,
        Map<String, ArchitectureEntity> entitiesById
    ) {
        if (relationships == null || relationships.isEmpty()) {
            return List.of();
        }
        Map<String, ArchitectureRelationship> relationshipsById = new LinkedHashMap<>();
        for (ArchitectureRelationship relationship : relationships) {
            relationshipsById.put(relationship.id(), relationship);
        }
        List<ArchitectureRelationship> normalized = new ArrayList<>(relationships.size());
        for (ArchitectureRelationship relationship : relationships) {
            normalized.add(normalizeRelationship(relationship, entitiesById, relationshipsById));
        }
        return List.copyOf(normalized);
    }
}
