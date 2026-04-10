package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.Map;

final class TypeScriptArchitectureEntitySemanticsSupport {
    private TypeScriptArchitectureEntitySemanticsSupport() {
    }

    static boolean isApiEntrypoint(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() == EntityKind.STARTUP_POINT) {
            return true;
        }
        if (entity.kind() != EntityKind.UI_MODULE) {
            return false;
        }
        if (TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "uiProfile", "page-or-router")
            || TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "matchType", "page-or-router")) {
            return true;
        }
        String sourceEntityId = TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        return source != null && isRouteOrPageLike(source);
    }

    static boolean isUiPage(ArchitectureEntity entity, ArchitectureEntityNormalizationContext context) {
        FrontendRouteEvidence routeEvidence = context.frontendRouteEvidenceOptional().orElse(null);
        if (routeEvidence != null) {
            if (routeEvidence.declaredRoute() && !looksLikeLayout(entity, routeEvidence)) {
                return true;
            }
            if (!TypeScriptArchitectureMetadataSupport.blank(routeEvidence.routeFullPath()) && !looksLikeLayout(entity, routeEvidence) && !routeEvidence.redirect()) {
                return true;
            }
        }
        if (entity.kind() != EntityKind.UI_MODULE && entity.kind() != EntityKind.STARTUP_POINT) {
            return false;
        }
        if (TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "uiProfile", "page-or-router")
            || TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "matchType", "page-or-router")) {
            return !looksLikeLayout(entity, routeEvidence);
        }
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        return lowerName.endsWith("page")
            || lowerName.endsWith("screen")
            || lowerName.endsWith("view")
            || path.contains("/pages/")
            || path.contains("/screens/");
    }

    static boolean isUiLayout(ArchitectureEntity entity, ArchitectureEntityNormalizationContext context) {
        FrontendRouteEvidence routeEvidence = context.frontendRouteEvidenceOptional().orElse(null);
        if (routeEvidence != null && looksLikeLayout(entity, routeEvidence)) {
            return true;
        }
        if (entity.kind() != EntityKind.UI_MODULE && entity.kind() != EntityKind.STARTUP_POINT) {
            return false;
        }
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        String snippet = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceSnippet"));
        return lowerName.endsWith("layout")
            || lowerName.endsWith("shell")
            || path.contains("/layouts/")
            || snippet.contains("<router-outlet")
            || snippet.contains("<outlet")
            || snippet.contains("children:")
            || snippet.contains("outlet:");
    }

    static boolean isUiNavigationNode(ArchitectureEntity entity, ArchitectureEntityNormalizationContext context) {
        if (entity.kind() != EntityKind.UI_MODULE && entity.kind() != EntityKind.CLASS && entity.kind() != EntityKind.FUNCTION) {
            return false;
        }
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        String snippet = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceSnippet"));
        boolean explicitNavStructure = lowerName.endsWith("menu")
            || lowerName.endsWith("sidebar")
            || lowerName.endsWith("navbar")
            || lowerName.endsWith("navigation")
            || lowerName.endsWith("nav")
            || lowerName.endsWith("breadcrumbs")
            || lowerName.endsWith("tabs")
            || path.contains("/navigation/")
            || path.contains("/menu/")
            || path.contains("/menus/")
            || path.contains("/sidebar/");
        boolean navigationGrounding = snippet.contains("<nav")
            || snippet.contains("<navlink")
            || snippet.contains("<link")
            || snippet.contains("routerlink")
            || snippet.contains("navigate(")
            || snippet.contains("navigatebyurl(");
        return explicitNavStructure && navigationGrounding && context.frontendRouteEvidenceOptional().isEmpty();
    }

    static boolean isApplicationService(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() != EntityKind.SERVICE) {
            return false;
        }
        if (TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "serviceProfile", "state-module")
            || TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "matchType", "state-module")
            || TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "serviceProfile", "function-service")
            || TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "serviceProfile", "angular-injectable")) {
            return true;
        }
        String sourceEntityId = TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        return source != null && isAngularInjectableOrStateLike(source);
    }

    static boolean isIntegrationAdapter(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() != EntityKind.SERVICE) {
            return false;
        }
        if (TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "serviceProfile", "api-client-or-service")
            || TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "matchType", "api-client-or-service")) {
            return true;
        }
        String sourceEntityId = TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        return source != null && isApiClientLike(source);
    }

    static boolean isConfigurationProvider(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() == EntityKind.CONFIG_ARTIFACT && TypeScriptArchitectureMetadataSupport.isTypeScriptBacked(entity, entitiesById)) {
            return true;
        }
        if (entity.kind() != EntityKind.UI_MODULE) {
            return false;
        }
        if (TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "uiProfile", "react-context")
            || TypeScriptArchitectureMetadataSupport.metadataEquals(entity.metadata(), "matchType", "react-context")) {
            return true;
        }
        String sourceEntityId = TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        if (source == null) {
            return false;
        }
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(source.name());
        String snippet = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(source.metadata(), "sourceSnippet"));
        return lowerName.endsWith("provider")
            || lowerName.endsWith("config")
            || snippet.contains("createcontext")
            || snippet.contains("usecontext");
    }

    static boolean isFrameworkManaged(ArchitectureEntity entity) {
        return TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "decorators", "injectable")
            || TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "decorators", "component")
            || TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "decorators", "ngmodule")
            || TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "decorators", "directive")
            || TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "decorators", "pipe")
            || TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "frameworks", "angular")
            || TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "frameworks", "react");
    }

    static boolean routeLike(ArchitectureEntity entity) {
        return hasRole(entity, ArchitecturalRole.UI_PAGE.id()) || hasRole(entity, ArchitecturalRole.UI_LAYOUT.id());
    }

    static boolean hasRole(ArchitectureEntity entity, String expectedRole) {
        return entity != null
            && entity.architecturalRoles() != null
            && entity.architecturalRoles().stream().anyMatch(role -> expectedRole.equalsIgnoreCase(role));
    }

    static boolean looksLikeFrontendNavigationSource(ArchitectureEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.kind() == EntityKind.UI_MODULE && TypeScriptArchitectureMetadataSupport.metadataContains(entity.metadata(), "uiProfile", "page-or-router")) {
            return true;
        }
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        String snippet = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.firstNonBlank(
            TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "routeSnippet"),
            TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceSnippet")
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

    private static boolean looksLikeLayout(ArchitectureEntity entity, FrontendRouteEvidence routeEvidence) {
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        String snippet = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.firstNonBlank(
            TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "routeSnippet"),
            TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceSnippet")
        ));
        return lowerName.endsWith("layout")
            || lowerName.endsWith("shell")
            || path.contains("/layouts/")
            || path.contains("/shell/")
            || snippet.contains("children:")
            || snippet.contains("<router-outlet")
            || snippet.contains("<outlet")
            || (routeEvidence != null && TypeScriptArchitectureMetadataSupport.equalsIgnoreCase(routeEvidence.routePath(), ""));
    }

    private static boolean isRouteOrPageLike(ArchitectureEntity entity) {
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        return lowerName.endsWith("page")
            || lowerName.endsWith("route")
            || lowerName.endsWith("routes")
            || lowerName.endsWith("screen")
            || lowerName.endsWith("view")
            || path.contains("/pages/")
            || path.contains("/routes/");
    }

    private static boolean isAngularInjectableOrStateLike(ArchitectureEntity entity) {
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        String snippet = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceSnippet"));
        return TypeScriptArchitectureMetadataSupport.metadataListContains(entity.metadata(), "decorators", "injectable")
            || lowerName.endsWith("service")
            || lowerName.endsWith("store")
            || lowerName.endsWith("reducer")
            || lowerName.endsWith("selector")
            || lowerName.endsWith("slice")
            || path.contains("/store/")
            || snippet.contains("createreducer")
            || snippet.contains("configurestore")
            || snippet.contains("createslice");
    }

    private static boolean isApiClientLike(ArchitectureEntity entity) {
        String lowerName = TypeScriptArchitectureMetadataSupport.lower(entity.name());
        String path = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "path"));
        String snippet = TypeScriptArchitectureMetadataSupport.lower(TypeScriptArchitectureMetadataSupport.stringMetadata(entity.metadata(), "sourceSnippet"));
        return lowerName.endsWith("client")
            || lowerName.endsWith("gateway")
            || lowerName.endsWith("api")
            || path.contains("/api/")
            || path.contains("/services/")
            || snippet.contains("httpclient")
            || snippet.contains("fetch(")
            || snippet.contains("axios.");
    }
}
