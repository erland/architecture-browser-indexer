package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Conservative TypeScript/Angular/React normalization mapping.
 *
 * <p>Step 12 keeps the mapping intentionally architectural and only emits roles when the existing
 * extraction/interpretation evidence is already reasonably mature. The goal is to broaden reuse of
 * the normalized vocabulary without forcing downstream consumers to understand framework-specific
 * metadata such as Angular decorators or React/router naming conventions.</p>
 */
final class TypeScriptArchitectureEntityNormalizationRule implements ArchitectureEntityNormalizationRule {
    @Override
    public NormalizedArchitectureEntity normalize(ArchitectureEntityNormalizationContext context) {
        ArchitectureEntity entity = context.entity();
        if (entity == null || !isTypeScriptBacked(entity, context.entitiesById())) {
            return null;
        }

        List<String> roles = new ArrayList<>();
        List<String> traits = new ArrayList<>();

        if (isApiEntrypoint(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.API_ENTRYPOINT.id());
            addIfMissing(traits, ArchitecturalTrait.EXTERNALLY_EXPOSED.id());
        }
        if (isUiPage(entity, context)) {
            addIfMissing(roles, ArchitecturalRole.UI_PAGE.id());
            addIfMissing(traits, ArchitecturalTrait.USER_FACING.id());
            if (context.frontendRouteEvidenceOptional().filter(FrontendRouteEvidence::declaredRoute).isPresent()) {
                addIfMissing(traits, ArchitecturalTrait.ROUTE_DECLARED.id());
            }
        }
        if (isUiLayout(entity, context)) {
            addIfMissing(roles, ArchitecturalRole.UI_LAYOUT.id());
            addIfMissing(traits, ArchitecturalTrait.USER_FACING.id());
            if (context.frontendRouteEvidenceOptional().filter(FrontendRouteEvidence::declaredRoute).isPresent()) {
                addIfMissing(traits, ArchitecturalTrait.ROUTE_DECLARED.id());
            }
        }
        if (isUiNavigationNode(entity, context)) {
            addIfMissing(roles, ArchitecturalRole.UI_NAVIGATION_NODE.id());
            addIfMissing(traits, ArchitecturalTrait.USER_FACING.id());
        }
        if (isApplicationService(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.APPLICATION_SERVICE.id());
        }
        if (isIntegrationAdapter(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.INTEGRATION_ADAPTER.id());
        }
        if (isConfigurationProvider(entity, context.entitiesById())) {
            addIfMissing(roles, ArchitecturalRole.CONFIGURATION_PROVIDER.id());
            addIfMissing(traits, ArchitecturalTrait.CONFIGURATION_DRIVEN.id());
        }
        if (isFrameworkManaged(entity)) {
            addIfMissing(traits, ArchitecturalTrait.FRAMEWORK_MANAGED.id());
        }

        if (roles.isEmpty() && traits.isEmpty()) {
            return null;
        }
        return new NormalizedArchitectureEntity(
            roles.isEmpty() ? null : roles,
            traits.isEmpty() ? null : traits
        );
    }

    private static boolean isTypeScriptBacked(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (metadataEquals(entity, "language", "typescript")
            || metadataEquals(entity, "sourceLanguage", "typescript")
            || metadataEquals(entity, "language", "tsx")
            || metadataEquals(entity, "sourceLanguage", "tsx")
            || metadataEquals(entity, "framework", "react")
            || metadataEquals(entity, "framework", "angular")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        if (sourceEntityId == null || sourceEntityId.isBlank()) {
            return false;
        }
        ArchitectureEntity source = entitiesById.get(sourceEntityId);
        return source != null && isTypeScriptBacked(source, Map.of());
    }

    private static boolean isApiEntrypoint(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() == EntityKind.STARTUP_POINT) {
            return true;
        }
        if (entity.kind() != EntityKind.UI_MODULE) {
            return false;
        }
        if (metadataEquals(entity, "uiProfile", "page-or-router")
            || metadataEquals(entity, "matchType", "page-or-router")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        return source != null && isRouteOrPageLike(source);
    }


    private static boolean isUiPage(ArchitectureEntity entity, ArchitectureEntityNormalizationContext context) {
        FrontendRouteEvidence routeEvidence = context.frontendRouteEvidenceOptional().orElse(null);
        if (routeEvidence != null) {
            if (routeEvidence.declaredRoute() && !looksLikeLayout(entity, routeEvidence)) {
                return true;
            }
            if (!blank(routeEvidence.routeFullPath()) && !looksLikeLayout(entity, routeEvidence) && !routeEvidence.redirect()) {
                return true;
            }
        }
        if (entity.kind() != EntityKind.UI_MODULE && entity.kind() != EntityKind.STARTUP_POINT) {
            return false;
        }
        if (metadataEquals(entity, "uiProfile", "page-or-router") || metadataEquals(entity, "matchType", "page-or-router")) {
            return !looksLikeLayout(entity, routeEvidence);
        }
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity, "path"));
        return lowerName.endsWith("page")
            || lowerName.endsWith("screen")
            || lowerName.endsWith("view")
            || path.contains("/pages/")
            || path.contains("/screens/");
    }

    private static boolean isUiLayout(ArchitectureEntity entity, ArchitectureEntityNormalizationContext context) {
        FrontendRouteEvidence routeEvidence = context.frontendRouteEvidenceOptional().orElse(null);
        if (routeEvidence != null && looksLikeLayout(entity, routeEvidence)) {
            return true;
        }
        if (entity.kind() != EntityKind.UI_MODULE && entity.kind() != EntityKind.STARTUP_POINT) {
            return false;
        }
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity, "path"));
        String snippet = lower(stringMetadata(entity, "sourceSnippet"));
        return lowerName.endsWith("layout")
            || lowerName.endsWith("shell")
            || path.contains("/layouts/")
            || snippet.contains("<router-outlet")
            || snippet.contains("<outlet")
            || snippet.contains("children:")
            || snippet.contains("outlet:");
    }

    private static boolean isUiNavigationNode(ArchitectureEntity entity, ArchitectureEntityNormalizationContext context) {
        if (entity.kind() != EntityKind.UI_MODULE && entity.kind() != EntityKind.CLASS && entity.kind() != EntityKind.FUNCTION) {
            return false;
        }
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity, "path"));
        String snippet = lower(stringMetadata(entity, "sourceSnippet"));
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

    private static boolean looksLikeLayout(ArchitectureEntity entity, FrontendRouteEvidence routeEvidence) {
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity, "path"));
        String snippet = lower(firstNonBlank(
            stringMetadata(entity, "routeSnippet"),
            stringMetadata(entity, "sourceSnippet")
        ));
        return lowerName.endsWith("layout")
            || lowerName.endsWith("shell")
            || path.contains("/layouts/")
            || path.contains("/shell/")
            || snippet.contains("children:")
            || snippet.contains("<router-outlet")
            || snippet.contains("<outlet")
            || (routeEvidence != null && equalsIgnoreCase(routeEvidence.routePath(), ""));
    }

    private static boolean isApplicationService(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() != EntityKind.SERVICE) {
            return false;
        }
        if (metadataEquals(entity, "serviceProfile", "state-module")
            || metadataEquals(entity, "matchType", "state-module")
            || metadataEquals(entity, "serviceProfile", "function-service")
            || metadataEquals(entity, "serviceProfile", "angular-injectable")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        return source != null && isAngularInjectableOrStateLike(source);
    }

    private static boolean isIntegrationAdapter(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() != EntityKind.SERVICE) {
            return false;
        }
        if (metadataEquals(entity, "serviceProfile", "api-client-or-service")
            || metadataEquals(entity, "matchType", "api-client-or-service")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        return source != null && isApiClientLike(source);
    }

    private static boolean isConfigurationProvider(ArchitectureEntity entity, Map<String, ArchitectureEntity> entitiesById) {
        if (entity.kind() == EntityKind.CONFIG_ARTIFACT && isTypeScriptBacked(entity, entitiesById)) {
            return true;
        }
        if (entity.kind() != EntityKind.UI_MODULE) {
            return false;
        }
        if (metadataEquals(entity, "uiProfile", "react-context") || metadataEquals(entity, "matchType", "react-context")) {
            return true;
        }
        String sourceEntityId = stringMetadata(entity, "sourceEntityId");
        ArchitectureEntity source = sourceEntityId == null ? null : entitiesById.get(sourceEntityId);
        if (source == null) {
            return false;
        }
        String lowerName = lower(source.name());
        String snippet = lower(stringMetadata(source, "sourceSnippet"));
        return lowerName.endsWith("provider")
            || lowerName.endsWith("config")
            || snippet.contains("createcontext")
            || snippet.contains("usecontext");
    }

    private static boolean isFrameworkManaged(ArchitectureEntity entity) {
        return metadataListContains(entity, "decorators", "injectable")
            || metadataListContains(entity, "decorators", "component")
            || metadataListContains(entity, "decorators", "ngmodule")
            || metadataListContains(entity, "decorators", "directive")
            || metadataListContains(entity, "decorators", "pipe")
            || metadataListContains(entity, "frameworks", "angular")
            || metadataListContains(entity, "frameworks", "react");
    }

    private static boolean isRouteOrPageLike(ArchitectureEntity entity) {
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity, "path"));
        return lowerName.endsWith("page")
            || lowerName.endsWith("route")
            || lowerName.endsWith("routes")
            || lowerName.endsWith("screen")
            || lowerName.endsWith("view")
            || path.contains("/pages/")
            || path.contains("/routes/");
    }

    private static boolean isAngularInjectableOrStateLike(ArchitectureEntity entity) {
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity, "path"));
        String snippet = lower(stringMetadata(entity, "sourceSnippet"));
        return metadataListContains(entity, "decorators", "injectable")
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
        String lowerName = lower(entity.name());
        String path = lower(stringMetadata(entity, "path"));
        String snippet = lower(stringMetadata(entity, "sourceSnippet"));
        return lowerName.endsWith("client")
            || lowerName.endsWith("gateway")
            || lowerName.endsWith("api")
            || path.contains("/api/")
            || path.contains("/services/")
            || snippet.contains("httpclient")
            || snippet.contains("fetch(")
            || snippet.contains("axios.");
    }

    private static boolean metadataEquals(ArchitectureEntity entity, String key, String expected) {
        Object value = entity.metadata().get(key);
        return value != null && String.valueOf(value).equalsIgnoreCase(expected);
    }

    private static boolean metadataListContains(ArchitectureEntity entity, String key, String expectedSuffixOrValue) {
        Object value = entity.metadata().get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                .map(String::valueOf)
                .map(TypeScriptArchitectureEntityNormalizationRule::lower)
                .anyMatch(item -> item.equals(lower(expectedSuffixOrValue)) || item.endsWith(lower(expectedSuffixOrValue)));
        }
        return value != null && lower(String.valueOf(value)).contains(lower(expectedSuffixOrValue));
    }

    private static String stringMetadata(ArchitectureEntity entity, String key) {
        Object value = entity.metadata().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static void addIfMissing(List<String> values, String value) {
        if (value != null && !value.isBlank() && !values.contains(value)) {
            values.add(value);
        }
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

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
