package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central seam for converting interpreted or framework-specific evidence into canonical
 * architecture-facing entity roles and traits.
 *
 * Step 4 intentionally introduces the seam before Java-first mappings are added. The default
 * service is conservative and preserves current behavior until explicit rules are registered.
 */
public final class ArchitectureEntityNormalizationService {
    private static final ArchitectureEntityNormalizationService DEFAULT = new ArchitectureEntityNormalizationService(List.of());

    private final List<ArchitectureEntityNormalizationRule> rules;

    private ArchitectureEntityNormalizationService(List<ArchitectureEntityNormalizationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static ArchitectureEntityNormalizationService defaultService() {
        return DEFAULT;
    }

    public static ArchitectureEntityNormalizationService of(List<ArchitectureEntityNormalizationRule> rules) {
        return new ArchitectureEntityNormalizationService(rules == null ? List.of() : rules);
    }

    public ArchitectureEntity normalizeEntity(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity == null) {
            return null;
        }
        List<String> roles = new ArrayList<>();
        if (entity.architecturalRoles() != null) {
            roles.addAll(entity.architecturalRoles());
        }
        List<String> traits = new ArrayList<>();
        if (entity.architecturalTraits() != null) {
            traits.addAll(entity.architecturalTraits());
        }

        ArchitectureEntityNormalizationContext context = new ArchitectureEntityNormalizationContext(entity, entitiesById);
        for (ArchitectureEntityNormalizationRule rule : rules) {
            if (rule == null) {
                continue;
            }
            NormalizedArchitectureEntity normalized = rule.normalize(context);
            if (normalized == null) {
                continue;
            }
            if (normalized.architecturalRoles() != null) {
                roles.addAll(normalized.architecturalRoles());
            }
            if (normalized.architecturalTraits() != null) {
                traits.addAll(normalized.architecturalTraits());
            }
        }

        return new ArchitectureEntity(
            entity.id(),
            entity.kind(),
            entity.origin(),
            entity.name(),
            entity.displayName(),
            entity.scopeId(),
            entity.sourceRefs(),
            entity.metadata(),
            roles.isEmpty() ? null : roles,
            traits.isEmpty() ? null : traits
        );
    }

    public Map<String, ArchitectureEntity> normalizeEntitiesById(Map<String, ArchitectureEntity> entitiesById) {
        Map<String, ArchitectureEntity> normalized = new LinkedHashMap<>();
        if (entitiesById == null || entitiesById.isEmpty()) {
            return normalized;
        }
        for (ArchitectureEntity entity : entitiesById.values()) {
            ArchitectureEntity normalizedEntity = normalizeEntity(entity, entitiesById);
            normalized.put(normalizedEntity.id(), normalizedEntity);
        }
        return normalized;
    }
}
