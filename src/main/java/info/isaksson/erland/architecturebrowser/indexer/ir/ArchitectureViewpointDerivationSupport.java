package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

final class ArchitectureViewpointDerivationSupport {
    private ArchitectureViewpointDerivationSupport() {
    }

    static List<String> moduleEvidenceSources(ViewpointEvidence evidence, boolean hasModuleDependencyViews, boolean hasModuleSemantic, boolean hasModuleBoundaries) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (hasModuleDependencyViews) {
            sources.add("dependency-views");
        }
        if (hasModuleSemantic || hasModuleBoundaries) {
            sources.add("normalized-semantics");
        }
        return sources.isEmpty() ? null : List.copyOf(sources);
    }

    static List<String> evidenceSources(
        ViewpointEvidence evidence,
        boolean hasRoleEvidence,
        boolean hasSemanticEvidence,
        boolean persistenceRelated,
        boolean integrationRelated
    ) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (hasRoleEvidence) {
            sources.add("normalized-roles");
        }
        if (hasSemanticEvidence) {
            sources.add("normalized-semantics");
        }
        if (evidence.hasJavaInterpretationEvidence()) {
            sources.add("java-interpretation");
        }
        if (persistenceRelated && evidence.hasJpaEvidence()) {
            sources.add("jpa");
        }
        if (integrationRelated && evidence.hasExternalSystemEvidence()) {
            sources.add("external-system");
        }
        return sources.isEmpty() ? null : List.copyOf(sources);
    }

    static List<String> uiNavigationEvidenceSources(
        ViewpointEvidence evidence,
        boolean hasRoleEvidence,
        boolean hasSemanticEvidence
    ) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (hasRoleEvidence) {
            sources.add("normalized-roles");
        }
        if (hasSemanticEvidence) {
            sources.add("normalized-semantics");
        }
        if (evidence.hasFrontendEvidence()) {
            sources.add("frontend-routing");
        }
        return sources.isEmpty() ? null : List.copyOf(sources);
    }

    static List<String> presentSemantics(ViewpointEvidence evidence, String... semantics) {
        return java.util.Arrays.stream(semantics)
            .filter(evidence::hasSemantic)
            .distinct()
            .sorted()
            .toList();
    }

    static List<String> roleIdsPresent(ViewpointEvidence evidence, String... roleIds) {
        return java.util.Arrays.stream(roleIds)
            .filter(roleId -> !evidence.entityIdsForRole(roleId).isEmpty())
            .distinct()
            .sorted()
            .toList();
    }

    @SafeVarargs
    static List<String> mergeIds(List<String>... lists) {
        return java.util.Arrays.stream(lists)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .toList();
    }

    static double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
