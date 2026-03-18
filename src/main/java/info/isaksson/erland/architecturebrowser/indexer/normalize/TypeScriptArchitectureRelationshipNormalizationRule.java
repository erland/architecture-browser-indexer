package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative TypeScript relationship normalization mapping.
 */
final class TypeScriptArchitectureRelationshipNormalizationRule implements ArchitectureRelationshipNormalizationRule {
    @Override
    public NormalizedArchitectureRelationship normalize(ArchitectureRelationshipNormalizationContext context) {
        ArchitectureRelationship relationship = context.relationship();
        if (relationship == null) {
            return null;
        }
        ArchitectureEntity source = context.entitiesById().get(relationship.fromEntityId());
        ArchitectureEntity target = context.entitiesById().get(relationship.toEntityId());
        if (!isTypeScriptBacked(relationship, source, target, context)) {
            return null;
        }

        List<String> semantics = new ArrayList<>();
        if (containsRoute(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.CONTAINS_ROUTE.id());
        }
        if (redirectsTo(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.REDIRECTS_TO.id());
        }
        if (navigatesTo(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.NAVIGATES_TO.id());
        }
        if (guardsRoute(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.GUARDS_ROUTE.id());
        }
        if (invokesUseCase(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id());
        }
        if (callsExternalSystem(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id());
        }
        if (accessesPersistence(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id());
        }
        return semantics.isEmpty() ? null : new NormalizedArchitectureRelationship(semantics);
    }

    private static boolean containsRoute(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (!isFrameworkRelationship(relationship, "childOf")) {
            return false;
        }
        FrontendRouteEvidence childRoute = context.sourceRouteEvidenceOptional().orElse(null);
        FrontendRouteEvidence parentRoute = context.targetRouteEvidenceOptional().orElse(null);
        return childRoute != null
            && parentRoute != null
            && childRoute.declaredRoute()
            && parentRoute.declaredRoute()
            && routeLike(source)
            && routeLike(target);
    }

    private static boolean redirectsTo(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (!(isFrameworkRelationship(relationship, "redirects") || context.relationshipRouteEvidenceOptional().filter(FrontendRouteEvidence::redirect).isPresent())) {
            return false;
        }
        FrontendRouteEvidence sourceRoute = context.sourceRouteEvidenceOptional().orElse(null);
        FrontendRouteEvidence targetRoute = context.targetRouteEvidenceOptional().orElse(null);
        return sourceRoute != null
            && targetRoute != null
            && sourceRoute.declaredRoute()
            && targetRoute.declaredRoute()
            && !blank(sourceRoute.redirectTargetLiteral())
            && !blank(targetRoute.routeFullPath())
            && sourceRoute.redirectTargetLiteral().equals(targetRoute.routeFullPath());
    }

    private static boolean navigatesTo(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        FrontendNavigationEvidence navigationEvidence = context.frontendNavigationEvidenceOptional().orElse(null);
        FrontendRouteEvidence targetRoute = context.targetRouteEvidenceOptional().orElse(null);
        if (navigationEvidence == null || targetRoute == null || !targetRoute.declaredRoute()) {
            return false;
        }
        if (!(navigationEvidence.staticLink() || navigationEvidence.imperativeNavigation())) {
            return false;
        }
        if (blank(navigationEvidence.navigationTargetLiteral())
            || blank(targetRoute.routeFullPath())
            || !navigationEvidence.navigationTargetLiteral().equals(targetRoute.routeFullPath())) {
            return false;
        }
        return routeLike(source)
            || hasRole(source, ArchitecturalRole.UI_PAGE.id())
            || hasRole(source, ArchitecturalRole.UI_LAYOUT.id())
            || hasRole(source, ArchitecturalRole.UI_NAVIGATION_NODE.id())
            || looksLikeFrontendNavigationSource(source);
    }

    private static boolean guardsRoute(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (!isFrameworkRelationship(relationship, "guards")) {
            return false;
        }
        FrontendRouteEvidence sourceRoute = context.sourceRouteEvidenceOptional().orElse(null);
        return sourceRoute != null
            && sourceRoute.declaredRoute()
            && sourceRoute.guarded()
            && routeLike(source)
            && target != null
            && !routeLike(target);
    }

    private static boolean invokesUseCase(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && hasRole(source, ArchitecturalRole.API_ENTRYPOINT.id())
            && hasRole(target, ArchitecturalRole.APPLICATION_SERVICE.id());
    }

    private static boolean callsExternalSystem(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && (hasRole(source, ArchitecturalRole.INTEGRATION_ADAPTER.id()) || hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id()))
            && (target != null && (target.kind() == EntityKind.EXTERNAL_SYSTEM || hasRole(target, ArchitecturalRole.EXTERNAL_DEPENDENCY.id())));
    }

    private static boolean accessesPersistence(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && (hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id()) || hasRole(source, ArchitecturalRole.INTEGRATION_ADAPTER.id()))
            && (target != null && (target.kind() == EntityKind.DATASTORE || hasRole(target, ArchitecturalRole.PERSISTENT_ENTITY.id())));
    }

    private static boolean isInvocationRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.USES || kind == RelationshipKind.DEPENDS_ON || kind == RelationshipKind.CALLS || kind == RelationshipKind.READS || kind == RelationshipKind.WRITES;
    }

    private static boolean routeLike(ArchitectureEntity entity) {
        return hasRole(entity, ArchitecturalRole.UI_PAGE.id()) || hasRole(entity, ArchitecturalRole.UI_LAYOUT.id());
    }

    private static boolean hasRole(ArchitectureEntity entity, String expectedRole) {
        return entity != null
            && entity.architecturalRoles() != null
            && entity.architecturalRoles().stream().anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }

    private static boolean looksLikeFrontendNavigationSource(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.kind() == EntityKind.UI_MODULE && metadataContains(entity.metadata(), "uiProfile", "page-or-router")) {
            return true;
        }
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity.metadata(), "path"));
        String snippet = lower(firstNonBlank(
            stringMetadata(entity.metadata(), "routeSnippet"),
            stringMetadata(entity.metadata(), "sourceSnippet")
        ));
        return lowerName.endsWith("page")
            || lowerName.endsWith("screen")
            || lowerName.endsWith("view")
            || lowerName.endsWith("menu")
            || lowerName.endsWith("sidebar")
            || lowerName.endsWith("navigation")
            || lowerName.endsWith("nav")
            || path.contains("/pages/")
            || path.contains("/screens/")
            || path.contains("/navigation/")
            || path.contains("/menu/")
            || snippet.contains("<link")
            || snippet.contains("<navlink")
            || snippet.contains("routerlink")
            || snippet.contains("navigate(")
            || snippet.contains("navigatebyurl(");
    }

    private static boolean isFrameworkRelationship(ArchitectureRelationship relationship, String expectedFrameworkRelationship) {
        return relationship != null
            && expectedFrameworkRelationship != null
            && expectedFrameworkRelationship.equalsIgnoreCase(stringMetadata(relationship.metadata(), "frameworkRelationship"));
    }

    private static boolean isTypeScriptBacked(
        ArchitectureRelationship relationship,
        ArchitectureEntity source,
        ArchitectureEntity target,
        ArchitectureRelationshipNormalizationContext context
    ) {
        if (metadataContains(relationship.metadata(), "sourceLanguage", "typescript")
            || metadataContains(relationship.metadata(), "sourceLanguage", "tsx")
            || metadataContains(relationship.metadata(), "uiProfile", "page-or-router")
            || metadataContains(relationship.metadata(), "serviceProfile", "api-client-or-service")
            || metadataContains(relationship.metadata(), "framework", "react")
            || metadataContains(relationship.metadata(), "framework", "angular")
            || context.relationshipRouteEvidenceOptional().isPresent()
            || context.frontendNavigationEvidenceOptional().isPresent()) {
            return true;
        }
        return isTypeScriptEntity(source, context) || isTypeScriptEntity(target, context);
    }

    private static boolean isTypeScriptEntity(ArchitectureEntity entity, ArchitectureRelationshipNormalizationContext context) {
        if (entity == null) {
            return false;
        }
        if (metadataContains(entity.metadata(), "language", "typescript")
            || metadataContains(entity.metadata(), "language", "tsx")
            || metadataContains(entity.metadata(), "sourceLanguage", "typescript")
            || metadataContains(entity.metadata(), "sourceLanguage", "tsx")
            || metadataContains(entity.metadata(), "framework", "react")
            || metadataContains(entity.metadata(), "framework", "angular")
            || FrontendRouteEvidence.fromEntity(entity).isPresent()) {
            return true;
        }
        Object sourceEntityId = entity.metadata().get("sourceEntityId");
        if (sourceEntityId == null) {
            return false;
        }
        ArchitectureEntity sourceEntity = context.entitiesById().get(String.valueOf(sourceEntityId));
        return sourceEntity != null && sourceEntity != entity && isTypeScriptEntity(sourceEntity, context);
    }

    private static boolean metadataContains(java.util.Map<String, Object> metadata, String key, String expected) {
        Object value = metadata == null ? null : metadata.get(key);
        return value != null && String.valueOf(value).toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private static String stringMetadata(java.util.Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!blank(value)) {
                return value;
            }
        }
        return null;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
