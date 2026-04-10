package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

final class TypeScriptArchitectureRelationshipSemanticsSupport {
    private TypeScriptArchitectureRelationshipSemanticsSupport() {
    }

    static boolean containsRoute(
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
            && TypeScriptArchitectureEntitySemanticsSupport.routeLike(source)
            && TypeScriptArchitectureEntitySemanticsSupport.routeLike(target);
    }

    static boolean redirectsTo(
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
            && !TypeScriptArchitectureMetadataSupport.blank(sourceRoute.redirectTargetLiteral())
            && !TypeScriptArchitectureMetadataSupport.blank(targetRoute.routeFullPath())
            && sourceRoute.redirectTargetLiteral().equals(targetRoute.routeFullPath());
    }

    static boolean navigatesTo(
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
        if (TypeScriptArchitectureMetadataSupport.blank(navigationEvidence.navigationTargetLiteral())
            || TypeScriptArchitectureMetadataSupport.blank(targetRoute.routeFullPath())
            || !navigationEvidence.navigationTargetLiteral().equals(targetRoute.routeFullPath())) {
            return false;
        }
        return TypeScriptArchitectureEntitySemanticsSupport.routeLike(source)
            || TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.UI_PAGE.id())
            || TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.UI_LAYOUT.id())
            || TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.UI_NAVIGATION_NODE.id())
            || TypeScriptArchitectureEntitySemanticsSupport.looksLikeFrontendNavigationSource(source);
    }

    static boolean guardsRoute(
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
            && TypeScriptArchitectureEntitySemanticsSupport.routeLike(source)
            && target != null
            && !TypeScriptArchitectureEntitySemanticsSupport.routeLike(target);
    }

    static boolean invokesUseCase(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.API_ENTRYPOINT.id())
            && TypeScriptArchitectureEntitySemanticsSupport.hasRole(target, ArchitecturalRole.APPLICATION_SERVICE.id());
    }

    static boolean callsExternalSystem(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && (TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.INTEGRATION_ADAPTER.id())
                || TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id()))
            && (target != null && (target.kind() == EntityKind.EXTERNAL_SYSTEM
                || TypeScriptArchitectureEntitySemanticsSupport.hasRole(target, ArchitecturalRole.EXTERNAL_DEPENDENCY.id())));
    }

    static boolean accessesPersistence(ArchitectureRelationship relationship, ArchitectureEntity source, ArchitectureEntity target) {
        return isInvocationRelationship(relationship.kind())
            && (TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.APPLICATION_SERVICE.id())
                || TypeScriptArchitectureEntitySemanticsSupport.hasRole(source, ArchitecturalRole.INTEGRATION_ADAPTER.id()))
            && (target != null && (target.kind() == EntityKind.DATASTORE
                || TypeScriptArchitectureEntitySemanticsSupport.hasRole(target, ArchitecturalRole.PERSISTENT_ENTITY.id())));
    }

    private static boolean isInvocationRelationship(RelationshipKind kind) {
        return kind == RelationshipKind.USES || kind == RelationshipKind.DEPENDS_ON || kind == RelationshipKind.CALLS || kind == RelationshipKind.READS || kind == RelationshipKind.WRITES;
    }

    private static boolean isFrameworkRelationship(ArchitectureRelationship relationship, String expectedFrameworkRelationship) {
        return relationship != null
            && expectedFrameworkRelationship != null
            && expectedFrameworkRelationship.equalsIgnoreCase(TypeScriptArchitectureMetadataSupport.stringMetadata(relationship.metadata(), "frameworkRelationship"));
    }
}
