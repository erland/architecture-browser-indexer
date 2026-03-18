package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step 7: derives conservative canonical viewpoint availability from normalized roles,
 * traits, relationship semantics, and already assembled dependency-view evidence.
 */
public final class ArchitectureIrViewpointDerivationService {
    private ArchitectureIrViewpointDerivationService() {
    }

    public static List<ArchitectureViewpoint> derive(
        List<ArchitectureEntity> entities,
        List<ArchitectureRelationship> relationships,
        Map<String, Object> dependencyViews
    ) {
        ViewpointEvidence evidence = ViewpointEvidence.from(entities, relationships, dependencyViews);
        List<ArchitectureViewpoint> viewpoints = new ArrayList<>();
        viewpoints.add(apiSurface(evidence));
        viewpoints.add(requestHandling(evidence));
        viewpoints.add(persistenceModel(evidence));
        viewpoints.add(integrationMap(evidence));
        viewpoints.add(moduleDependencies(evidence));
        viewpoints.add(uiNavigation(evidence));
        return ArchitectureIrJavaViewpointBridgeSupport.apply(viewpoints, dependencyViews);
    }

    private static ArchitectureViewpoint apiSurface(ViewpointEvidence evidence) {
        List<String> entrypoints = evidence.entityIdsForRole(ArchitecturalRole.API_ENTRYPOINT.id());
        boolean hasEntryPoints = !entrypoints.isEmpty();
        String availability = hasEntryPoints ? "available" : "unavailable";
        double confidence = hasEntryPoints
            ? clamp(0.72 + Math.min(0.24, entrypoints.size() * 0.08))
            : 0.0;
        return new ArchitectureViewpoint(
            "api-surface",
            "API surface",
            "Highlights externally exposed API entrypoints and the first service hop behind them when available.",
            availability,
            confidence,
            hasEntryPoints ? entrypoints : null,
            hasEntryPoints ? List.of(ArchitecturalRole.API_ENTRYPOINT.id()) : null,
            evidence.presentSemantics(ArchitecturalRelationshipSemantic.SERVES_REQUEST.id()),
            null,
            evidenceSources(evidence, hasEntryPoints, false, false, false)
        );
    }

    private static ArchitectureViewpoint requestHandling(ViewpointEvidence evidence) {
        List<String> entrypoints = evidence.entityIdsForRole(ArchitecturalRole.API_ENTRYPOINT.id());
        List<String> services = evidence.entityIdsForRole(ArchitecturalRole.APPLICATION_SERVICE.id());
        boolean hasEntrypoint = !entrypoints.isEmpty();
        boolean hasDownstreamSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id());
        String availability;
        double confidence;
        if (hasEntrypoint && hasDownstreamSemantics) {
            availability = "available";
            confidence = clamp(0.78 + (evidence.hasSemantic(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id()) ? 0.08 : 0.0)
                + (evidence.hasSemantic(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id()) ? 0.06 : 0.0));
        } else if (hasEntrypoint || hasDownstreamSemantics || !services.isEmpty()) {
            availability = "partial";
            confidence = clamp(0.42
                + (hasEntrypoint ? 0.12 : 0.0)
                + (hasDownstreamSemantics ? 0.12 : 0.0)
                + (!services.isEmpty() ? 0.08 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = new ArrayList<>();
        seedEntityIds.addAll(entrypoints);
        seedEntityIds.addAll(services);
        seedEntityIds = seedEntityIds.stream().distinct().sorted().toList();
        return new ArchitectureViewpoint(
            "request-handling",
            "Request handling",
            "Highlights request-serving paths from entrypoints through application services.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            roleIdsPresent(evidence, ArchitecturalRole.API_ENTRYPOINT.id(), ArchitecturalRole.APPLICATION_SERVICE.id()),
            presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.SERVES_REQUEST.id(),
                ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id(),
                ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id()),
            null,
            evidenceSources(evidence, hasEntrypoint || !services.isEmpty(), true, false, false)
        );
    }

    private static ArchitectureViewpoint persistenceModel(ViewpointEvidence evidence) {
        List<String> persistentEntities = evidence.entityIdsForRole(ArchitecturalRole.PERSISTENT_ENTITY.id());
        List<String> persistenceAccess = evidence.entityIdsForRole(ArchitecturalRole.PERSISTENCE_ACCESS.id());
        boolean hasCorePersistenceRoles = !persistentEntities.isEmpty() || !persistenceAccess.isEmpty();
        boolean hasPersistenceSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.STORED_IN.id());
        String availability;
        double confidence;
        if (hasCorePersistenceRoles) {
            availability = "available";
            confidence = clamp(0.76
                + (!persistentEntities.isEmpty() ? 0.08 : 0.0)
                + (!persistenceAccess.isEmpty() ? 0.08 : 0.0)
                + (hasPersistenceSemantics ? 0.04 : 0.0));
        } else if (hasPersistenceSemantics || evidence.hasEntityKind(EntityKind.DATASTORE)) {
            availability = "partial";
            confidence = clamp(0.44
                + (hasPersistenceSemantics ? 0.16 : 0.0)
                + (evidence.hasEntityKind(EntityKind.DATASTORE) ? 0.08 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = mergeIds(persistentEntities, persistenceAccess);
        return new ArchitectureViewpoint(
            "persistence-model",
            "Persistence model",
            "Highlights persistent entities together with persistence access paths.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            roleIdsPresent(evidence, ArchitecturalRole.PERSISTENT_ENTITY.id(), ArchitecturalRole.PERSISTENCE_ACCESS.id()),
            presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id(),
                ArchitecturalRelationshipSemantic.STORED_IN.id()),
            null,
            evidenceSources(evidence, hasCorePersistenceRoles, hasPersistenceSemantics, true, false)
        );
    }

    private static ArchitectureViewpoint integrationMap(ViewpointEvidence evidence) {
        List<String> adapters = evidence.entityIdsForRole(ArchitecturalRole.INTEGRATION_ADAPTER.id());
        List<String> externals = evidence.entityIdsForRole(ArchitecturalRole.EXTERNAL_DEPENDENCY.id());
        boolean hasExternalCallSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id());
        boolean hasExternalKinds = evidence.hasEntityKind(EntityKind.EXTERNAL_SYSTEM);
        String availability;
        double confidence;
        if ((hasExternalCallSemantics && (!adapters.isEmpty() || !externals.isEmpty())) || (!adapters.isEmpty() && !externals.isEmpty())) {
            availability = "available";
            confidence = clamp(0.72
                + (hasExternalCallSemantics ? 0.10 : 0.0)
                + (!adapters.isEmpty() ? 0.08 : 0.0)
                + (!externals.isEmpty() ? 0.08 : 0.0));
        } else if (hasExternalCallSemantics || !adapters.isEmpty() || !externals.isEmpty() || hasExternalKinds) {
            availability = "partial";
            confidence = clamp(0.40
                + (hasExternalCallSemantics ? 0.18 : 0.0)
                + ((!adapters.isEmpty() || !externals.isEmpty() || hasExternalKinds) ? 0.14 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = mergeIds(adapters, externals);
        return new ArchitectureViewpoint(
            "integration-map",
            "Integration map",
            "Highlights exported integration-facing dependencies when present in the graph.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            roleIdsPresent(evidence, ArchitecturalRole.INTEGRATION_ADAPTER.id(), ArchitecturalRole.EXTERNAL_DEPENDENCY.id()),
            presentSemantics(evidence, ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id()),
            null,
            evidenceSources(evidence, !adapters.isEmpty() || !externals.isEmpty() || hasExternalKinds, hasExternalCallSemantics, false, false)
        );
    }


    private static ArchitectureViewpoint uiNavigation(ViewpointEvidence evidence) {
        List<String> pages = evidence.entityIdsForRole(ArchitecturalRole.UI_PAGE.id());
        List<String> layouts = evidence.entityIdsForRole(ArchitecturalRole.UI_LAYOUT.id());
        List<String> navigationNodes = evidence.entityIdsForRole(ArchitecturalRole.UI_NAVIGATION_NODE.id());
        boolean hasUiRoles = !pages.isEmpty() || !layouts.isEmpty() || !navigationNodes.isEmpty();
        boolean hasCoreNavigationSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.NAVIGATES_TO.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.CONTAINS_ROUTE.id());
        boolean hasSecondaryNavigationSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.REDIRECTS_TO.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.GUARDS_ROUTE.id());
        String availability;
        double confidence;
        if (hasUiRoles && hasCoreNavigationSemantics) {
            availability = "available";
            confidence = clamp(0.76
                + (!pages.isEmpty() ? 0.08 : 0.0)
                + (!layouts.isEmpty() ? 0.06 : 0.0)
                + (!navigationNodes.isEmpty() ? 0.04 : 0.0)
                + (hasSecondaryNavigationSemantics ? 0.04 : 0.0));
        } else if (hasUiRoles || hasCoreNavigationSemantics || hasSecondaryNavigationSemantics) {
            availability = "partial";
            confidence = clamp(0.44
                + (hasUiRoles ? 0.16 : 0.0)
                + (hasCoreNavigationSemantics ? 0.16 : 0.0)
                + (hasSecondaryNavigationSemantics ? 0.08 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = mergeIds(pages, layouts, navigationNodes);
        return new ArchitectureViewpoint(
            "ui-navigation",
            "UI navigation",
            "Highlights user-facing pages, layouts, and navigation structures together with canonical navigation relationships.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            roleIdsPresent(evidence,
                ArchitecturalRole.UI_LAYOUT.id(),
                ArchitecturalRole.UI_NAVIGATION_NODE.id(),
                ArchitecturalRole.UI_PAGE.id()),
            presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.CONTAINS_ROUTE.id(),
                ArchitecturalRelationshipSemantic.GUARDS_ROUTE.id(),
                ArchitecturalRelationshipSemantic.NAVIGATES_TO.id(),
                ArchitecturalRelationshipSemantic.REDIRECTS_TO.id()),
            null,
            uiNavigationEvidenceSources(evidence, hasUiRoles, hasCoreNavigationSemantics || hasSecondaryNavigationSemantics)
        );
    }

    private static ArchitectureViewpoint moduleDependencies(ViewpointEvidence evidence) {
        boolean hasModuleDependencyViews = evidence.hasDependencyViewList("moduleDependencies")
            || evidence.hasDependencyViewList("compositionModuleDependencies")
            || evidence.hasDependencyViewList("routeModuleDependencies")
            || evidence.hasDependencyViewList("providerModuleDependencies")
            || evidence.hasDependencyViewList("hookModuleDependencies")
            || evidence.hasDependencyViewList("endpointModuleDependencies")
            || evidence.hasDependencyViewList("entityModelModuleDependencies")
            || evidence.hasDependencyViewList("observerModuleDependencies")
            || evidence.hasDependencyViewList("writePathModuleDependencies");
        boolean hasModuleSemantic = evidence.hasSemantic(ArchitecturalRelationshipSemantic.DEPENDS_ON_MODULE.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.BELONGS_TO_MODULE.id());
        boolean hasModuleBoundaries = !evidence.entityIdsForRole(ArchitecturalRole.MODULE_BOUNDARY.id()).isEmpty();
        String availability;
        double confidence;
        if (hasModuleDependencyViews || hasModuleSemantic) {
            availability = "available";
            confidence = clamp(0.74
                + (hasModuleDependencyViews ? 0.12 : 0.0)
                + (hasModuleSemantic ? 0.06 : 0.0)
                + (hasModuleBoundaries ? 0.04 : 0.0));
        } else if (hasModuleBoundaries || evidence.entityCountByKind(EntityKind.MODULE) > 1) {
            availability = "partial";
            confidence = clamp(0.38
                + (hasModuleBoundaries ? 0.14 : 0.0)
                + (evidence.entityCountByKind(EntityKind.MODULE) > 1 ? 0.12 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        return new ArchitectureViewpoint(
            "module-dependencies",
            "Module dependencies",
            "Shows component and module dependency structure available from the exported graph.",
            availability,
            confidence,
            null,
            roleIdsPresent(evidence, ArchitecturalRole.MODULE_BOUNDARY.id()),
            presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.DEPENDS_ON_MODULE.id(),
                ArchitecturalRelationshipSemantic.BELONGS_TO_MODULE.id()),
            null,
            moduleEvidenceSources(evidence, hasModuleDependencyViews, hasModuleSemantic, hasModuleBoundaries)
        );
    }

    private static List<String> moduleEvidenceSources(ViewpointEvidence evidence, boolean hasModuleDependencyViews, boolean hasModuleSemantic, boolean hasModuleBoundaries) {
        LinkedHashSet<String> sources = new LinkedHashSet<>();
        if (hasModuleDependencyViews) {
            sources.add("dependency-views");
        }
        if (hasModuleSemantic || hasModuleBoundaries) {
            sources.add("normalized-semantics");
        }
        return sources.isEmpty() ? null : List.copyOf(sources);
    }

    private static List<String> evidenceSources(
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


    private static List<String> uiNavigationEvidenceSources(
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

    private static List<String> presentSemantics(ViewpointEvidence evidence, String... semantics) {
        return java.util.Arrays.stream(semantics)
            .filter(evidence::hasSemantic)
            .distinct()
            .sorted()
            .toList();
    }

    private static List<String> roleIdsPresent(ViewpointEvidence evidence, String... roleIds) {
        return java.util.Arrays.stream(roleIds)
            .filter(roleId -> !evidence.entityIdsForRole(roleId).isEmpty())
            .distinct()
            .sorted()
            .toList();
    }

    @SafeVarargs
    private static List<String> mergeIds(List<String>... lists) {
        return java.util.Arrays.stream(lists)
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .distinct()
            .sorted()
            .toList();
    }

    private static double clamp(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private record ViewpointEvidence(
        List<ArchitectureEntity> entities,
        List<ArchitectureRelationship> relationships,
        Map<String, Object> dependencyViews,
        Map<String, List<String>> entityIdsByRole,
        Set<String> relationshipSemantics,
        boolean hasJavaInterpretationEvidence,
        boolean hasJpaEvidence,
        boolean hasExternalSystemEvidence,
        boolean hasFrontendEvidence,
        Map<EntityKind, Long> entityCountsByKind
    ) {
        static ViewpointEvidence from(
            List<ArchitectureEntity> entities,
            List<ArchitectureRelationship> relationships,
            Map<String, Object> dependencyViews
        ) {
            List<ArchitectureEntity> safeEntities = entities == null ? List.of() : List.copyOf(entities);
            List<ArchitectureRelationship> safeRelationships = relationships == null ? List.of() : List.copyOf(relationships);
            Map<String, Object> safeDependencyViews = dependencyViews == null ? Map.of() : Map.copyOf(dependencyViews);
            Map<String, List<String>> entityIdsByRole = safeEntities.stream()
                .filter(entity -> entity.architecturalRoles() != null)
                .flatMap(entity -> entity.architecturalRoles().stream().map(role -> Map.entry(role, entity.id())))
                .collect(Collectors.groupingBy(
                    Map.Entry::getKey,
                    Collectors.mapping(Map.Entry::getValue, Collectors.collectingAndThen(Collectors.toCollection(java.util.TreeSet::new), List::copyOf))
                ));
            Set<String> relationshipSemantics = safeRelationships.stream()
                .map(ArchitectureRelationship::architecturalSemantics)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toCollection(java.util.TreeSet::new));
            boolean hasJavaInterpretationEvidence = safeEntities.stream().anyMatch(entity -> hasJavaInterpretationMetadata(entity.metadata()))
                || safeRelationships.stream().anyMatch(relationship -> hasJavaInterpretationMetadata(relationship.metadata()));
            boolean hasJpaEvidence = safeEntities.stream().anyMatch(entity -> hasJpaMetadata(entity.metadata()));
            boolean hasExternalSystemEvidence = safeEntities.stream().anyMatch(entity -> entity.kind() == EntityKind.EXTERNAL_SYSTEM)
                || safeEntities.stream().anyMatch(entity -> hasExternalMetadata(entity.metadata()));
            boolean hasFrontendEvidence = safeEntities.stream().anyMatch(entity -> hasFrontendMetadata(entity.metadata()))
                || safeRelationships.stream().anyMatch(relationship -> hasFrontendMetadata(relationship.metadata()));
            Map<EntityKind, Long> entityCountsByKind = safeEntities.stream()
                .collect(Collectors.groupingBy(ArchitectureEntity::kind, Collectors.counting()));
            return new ViewpointEvidence(
                safeEntities,
                safeRelationships,
                safeDependencyViews,
                entityIdsByRole,
                relationshipSemantics,
                hasJavaInterpretationEvidence,
                hasJpaEvidence,
                hasExternalSystemEvidence,
                hasFrontendEvidence,
                entityCountsByKind
            );
        }

        List<String> entityIdsForRole(String roleId) {
            return entityIdsByRole.getOrDefault(roleId, List.of());
        }

        boolean hasSemantic(String semantic) {
            return relationshipSemantics.contains(semantic);
        }

        List<String> presentSemantics(String semantic) {
            return hasSemantic(semantic) ? List.of(semantic) : null;
        }

        boolean hasEntityKind(EntityKind kind) {
            return entityCountsByKind.getOrDefault(kind, 0L) > 0;
        }

        long entityCountByKind(EntityKind kind) {
            return entityCountsByKind.getOrDefault(kind, 0L);
        }

        boolean hasDependencyViewList(String key) {
            Object value = dependencyViews.get(key);
            return value instanceof Collection<?> collection && !collection.isEmpty();
        }

        private static boolean hasJavaInterpretationMetadata(Map<String, Object> metadata) {
            if (metadata == null || metadata.isEmpty()) {
                return false;
            }
            return metadata.containsKey("backendProfile")
                || containsIgnoreCase(metadata.get("frameworks"), "jax-rs")
                || containsIgnoreCase(metadata.get("frameworks"), "spring")
                || containsIgnoreCase(metadata.get("technology"), "java")
                || containsIgnoreCase(metadata.get("language"), "java");
        }

        private static boolean hasJpaMetadata(Map<String, Object> metadata) {
            if (metadata == null || metadata.isEmpty()) {
                return false;
            }
            return containsIgnoreCase(metadata.get("frameworks"), "jpa")
                || containsIgnoreCase(metadata.get("annotations"), "@Entity")
                || containsIgnoreCase(metadata.get("backendProfile"), "jpa")
                || containsIgnoreCase(metadata.get("backendProfile"), "repository")
                || containsIgnoreCase(metadata.get("entityRole"), "repository");
        }

        private static boolean hasFrontendMetadata(Map<String, Object> metadata) {
            if (metadata == null || metadata.isEmpty()) {
                return false;
            }
            return containsIgnoreCase(metadata.get("framework"), "react")
                || containsIgnoreCase(metadata.get("framework"), "angular")
                || containsIgnoreCase(metadata.get("frameworks"), "react")
                || containsIgnoreCase(metadata.get("frameworks"), "angular")
                || metadata.containsKey("routePath")
                || metadata.containsKey("routeDeclarationKind")
                || metadata.containsKey("routeSourceKind")
                || metadata.containsKey("navigationTargetLiteral")
                || metadata.containsKey("redirectTargetLiteral");
        }

        private static boolean hasExternalMetadata(Map<String, Object> metadata) {
            if (metadata == null || metadata.isEmpty()) {
                return false;
            }
            return containsIgnoreCase(metadata.get("entityRole"), "external")
                || containsIgnoreCase(metadata.get("backendProfile"), "integration")
                || containsIgnoreCase(metadata.get("kindHint"), "external");
        }

        private static boolean containsIgnoreCase(Object value, String needle) {
            if (value == null) {
                return false;
            }
            String lowerNeedle = needle.toLowerCase(Locale.ROOT);
            if (value instanceof Collection<?> collection) {
                return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(text -> text.toLowerCase(Locale.ROOT))
                    .anyMatch(text -> text.contains(lowerNeedle));
            }
            String text = value.toString().toLowerCase(Locale.ROOT);
            return text.contains(lowerNeedle);
        }
    }
}
