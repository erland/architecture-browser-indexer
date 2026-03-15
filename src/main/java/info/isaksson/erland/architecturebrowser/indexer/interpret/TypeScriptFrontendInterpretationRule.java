package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TypeScriptFrontendInterpretationRule implements InterpretationRule {
    @Override
    public String ruleId() {
        return "typescript-frontend-high-value";
    }

    @Override
    public void apply(InterpretationContext context, InterpretationAccumulator accumulator) {
        for (ExtractedEntityFact entity : context.entitiesByLanguage("typescript")) {
            inferUiModule(entity, accumulator);
            inferService(entity, accumulator);
            inferStartupPoint(entity, accumulator);
        }
    }

    private void inferUiModule(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        List<String> decorators = normalized(InterpretationContext.listMetadata(entity, "decorators"));
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String path = lower(InterpretationContext.path(entity));
        String declarationKind = lower(InterpretationContext.stringMetadata(entity, "declarationKind"));
        String snippet = lower(InterpretationContext.primaryRef(entity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(entity).snippet()));

        boolean angularUi = hasAnyDecorator(decorators, "component", "directive", "pipe", "ngmodule");
        boolean pageOrRouterLike = matchesSuffix(lowerName, "page", "route", "routes", "layout", "screen", "view")
            || containsPathSegment(path, "/pages/")
            || containsPathSegment(path, "/routes/");
        boolean providerOrConsumer = matchesSuffix(lowerName, "provider", "consumer") || snippet.contains("createcontext") || snippet.contains("usecontext");
        boolean reactFunctionComponent = isFunctionLike(entity, declarationKind)
            && startsUpperCase(entity.name())
            && (snippet.contains("<") || path.endsWith(".tsx") || matchesSuffix(lowerName, "component", "page", "screen", "layout", "view", "provider", "consumer"));
        boolean reactClassComponent = isClassLike(entity, declarationKind)
            && startsUpperCase(entity.name())
            && (snippet.contains("extends react.component") || snippet.contains("extends component") || path.endsWith(".tsx")
                || matchesSuffix(lowerName, "component", "page", "screen", "layout", "view", "provider", "consumer"));
        boolean uiByLegacyName = lowerName.endsWith("component") || lowerName.endsWith("module");

        if (angularUi || pageOrRouterLike || providerOrConsumer || reactFunctionComponent || reactClassComponent || uiByLegacyName) {
            String profile = angularUi ? angularUiProfile(decorators)
                : providerOrConsumer ? "react-context"
                : pageOrRouterLike ? "page-or-router"
                : reactFunctionComponent ? "react-function-component"
                : reactClassComponent ? "react-class-component"
                : "name-based-ui";
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.UI_MODULE, " ui module", Map.of(
                "matchType", profile,
                "uiProfile", profile,
                "sourceLanguage", "typescript"
            ));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-ui-module", entity.sourceRefs(), Map.of(
                    "sourceLanguage", "typescript",
                    "uiProfile", profile
                )
            ), ruleId());
        }
    }

    private void inferService(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        List<String> decorators = normalized(InterpretationContext.listMetadata(entity, "decorators"));
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String path = lower(InterpretationContext.path(entity));
        String declarationKind = lower(InterpretationContext.stringMetadata(entity, "declarationKind"));

        boolean angularService = hasAnyDecorator(decorators, "injectable");
        boolean apiClient = matchesSuffix(lowerName, "service", "api", "client", "gateway", "facade")
            || containsPathSegment(path, "/services/")
            || containsPathSegment(path, "/api/")
            || containsPathSegment(path, "/clients/");
        boolean stateModule = matchesSuffix(lowerName, "store", "state", "reducer", "slice")
            || containsPathSegment(path, "/state/")
            || containsPathSegment(path, "/store/")
            || containsPathSegment(path, "/stores/");
        boolean serviceFunction = isFunctionLike(entity, declarationKind) && (apiClient || stateModule);
        boolean serviceType = isClassLike(entity, declarationKind) || isFunctionLike(entity, declarationKind);

        if (serviceType && (angularService || apiClient || stateModule || serviceFunction)) {
            String profile = angularService ? "angular-injectable"
                : stateModule ? "state-module"
                : apiClient ? "api-client-or-service"
                : "function-service";
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.SERVICE, " service", Map.of(
                "matchType", profile,
                "serviceProfile", profile,
                "sourceLanguage", "typescript"
            ));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-service", entity.sourceRefs(), Map.of(
                    "sourceLanguage", "typescript",
                    "serviceProfile", profile
                )
            ), ruleId());
        }
    }

    private void inferStartupPoint(ExtractedEntityFact entity, InterpretationAccumulator accumulator) {
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String path = InterpretationContext.path(entity);
        boolean startupByName = lowerName.equals("main") || lowerName.contains("bootstrap") || lowerName.equals("renderapp") || lowerName.equals("startup");
        boolean startupByPath = path != null && (path.endsWith("/main.ts") || path.endsWith("/main.tsx") || path.endsWith("/index.tsx") || path.endsWith("/bootstrap.ts"));
        if (startupByName || startupByPath) {
            var role = InterpretationSupport.roleEntity(ruleId(), entity, EntityKind.STARTUP_POINT, " startup point", Map.of("matchType", startupByName ? "name" : "path"));
            accumulator.addEntity(role, ruleId());
            accumulator.addRelationship(InterpretationSupport.relationship(
                ruleId(), RelationshipKind.USES, entity.id(), role.id(), "interpreted-as-startup-point", entity.sourceRefs(), Map.of("sourceLanguage", "typescript")
            ), ruleId());
        }
    }


    private static boolean isFunctionLike(ExtractedEntityFact entity, String declarationKind) {
        return "function".equals(declarationKind) || entity.kind() == EntityKind.FUNCTION;
    }

    private static boolean isClassLike(ExtractedEntityFact entity, String declarationKind) {
        return "class".equals(declarationKind) || entity.kind() == EntityKind.CLASS;
    }

    private static boolean hasAnyDecorator(List<String> decorators, String... names) {
        for (String name : names) {
            String normalizedName = name.toLowerCase(Locale.ROOT);
            if (decorators.stream().anyMatch(value -> value.endsWith(normalizedName))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> normalized(List<String> values) {
        return values.stream().map(TypeScriptFrontendInterpretationRule::lower).toList();
    }

    private static String angularUiProfile(List<String> decorators) {
        if (hasAnyDecorator(decorators, "component")) {
            return "angular-component";
        }
        if (hasAnyDecorator(decorators, "directive")) {
            return "angular-directive";
        }
        if (hasAnyDecorator(decorators, "pipe")) {
            return "angular-pipe";
        }
        if (hasAnyDecorator(decorators, "ngmodule")) {
            return "angular-module";
        }
        return "angular-ui";
    }

    private static boolean matchesSuffix(String lowerName, String... suffixes) {
        for (String suffix : suffixes) {
            if (lowerName.endsWith(suffix.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPathSegment(String path, String segment) {
        return path != null && path.contains(segment);
    }

    private static boolean startsUpperCase(String value) {
        return value != null && !value.isBlank() && Character.isUpperCase(value.charAt(0));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
