package info.isaksson.erland.architecturebrowser.indexer.ir.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Stable exported viewpoint descriptor contract.
 *
 * Step 7 introduces a document-level, framework-agnostic viewpoint catalog that can seed
 * browser/platform viewpoint selection without requiring consumers to understand technology-
 * specific metadata catalogs directly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitectureViewpoint(
    String id,
    String title,
    String description,
    String availability,
    Double confidence,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> seedEntityIds,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> seedRoleIds,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> expandViaSemantics,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> preferredDependencyViews,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> evidenceSources
) {
    public ArchitectureViewpoint {
        id = trimToNull(id);
        title = trimToNull(title);
        description = trimToNull(description);
        availability = trimToNull(availability);
        seedEntityIds = canonicalizeStrings(seedEntityIds);
        seedRoleIds = canonicalizeStrings(seedRoleIds);
        expandViaSemantics = canonicalizeStrings(expandViaSemantics);
        preferredDependencyViews = canonicalizeStrings(preferredDependencyViews);
        evidenceSources = canonicalizeStrings(evidenceSources);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<String> canonicalizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
            .filter(value -> value != null && !value.isBlank())
            .map(String::trim)
            .distinct()
            .sorted()
            .toList();
    }
}
