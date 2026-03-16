package info.isaksson.erland.architecturebrowser.indexer.interpret;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.List;
import java.util.Locale;

final class TypeScriptFrontendClassifier {
    String classifyUiProfile(ExtractedEntityFact entity) {
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

        if (!(angularUi || pageOrRouterLike || providerOrConsumer || reactFunctionComponent || reactClassComponent || uiByLegacyName)) {
            return null;
        }
        if (angularUi) {
            return angularUiProfile(decorators);
        }
        if (providerOrConsumer) {
            return "react-context";
        }
        if (pageOrRouterLike) {
            return "page-or-router";
        }
        if (reactFunctionComponent) {
            return "react-function-component";
        }
        if (reactClassComponent) {
            return "react-class-component";
        }
        return "ui-module";
    }

    String classifyServiceProfile(ExtractedEntityFact entity) {
        List<String> decorators = normalized(InterpretationContext.listMetadata(entity, "decorators"));
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String path = lower(InterpretationContext.path(entity));
        String declarationKind = lower(InterpretationContext.stringMetadata(entity, "declarationKind"));
        String snippet = lower(InterpretationContext.primaryRef(entity) == null ? "" : String.valueOf(InterpretationContext.primaryRef(entity).snippet()));

        boolean angularService = hasAnyDecorator(decorators, "injectable") || lowerName.endsWith("service");
        boolean apiClient = matchesSuffix(lowerName, "client", "api", "gateway", "service")
            || containsPathSegment(path, "/services/")
            || containsPathSegment(path, "/api/")
            || snippet.contains("httpclient")
            || snippet.contains("fetch(")
            || snippet.contains("axios.");
        boolean stateModule = matchesSuffix(lowerName, "store", "reducer", "selector", "slice")
            || containsPathSegment(path, "/store/")
            || snippet.contains("createreducer")
            || snippet.contains("configurestore")
            || snippet.contains("createslice");
        boolean serviceFunction = isFunctionLike(entity, declarationKind) && (apiClient || stateModule);
        boolean serviceType = isClassLike(entity, declarationKind) || isFunctionLike(entity, declarationKind);
        if (!(serviceType && (angularService || apiClient || stateModule || serviceFunction))) {
            return null;
        }
        if (angularService) {
            return "angular-injectable";
        }
        if (stateModule) {
            return "state-module";
        }
        if (apiClient) {
            return "api-client-or-service";
        }
        return "function-service";
    }

    boolean isStartupPoint(ExtractedEntityFact entity) {
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        String path = InterpretationContext.path(entity);
        boolean startupByName = lowerName.equals("main") || lowerName.contains("bootstrap") || lowerName.equals("renderapp") || lowerName.equals("startup");
        boolean startupByPath = path != null && (path.endsWith("/main.ts") || path.endsWith("/main.tsx") || path.endsWith("/index.tsx") || path.endsWith("/bootstrap.ts"));
        return startupByName || startupByPath;
    }

    String startupMatchType(ExtractedEntityFact entity) {
        String lowerName = entity.name().toLowerCase(Locale.ROOT);
        return lowerName.equals("main") || lowerName.contains("bootstrap") || lowerName.equals("renderapp") || lowerName.equals("startup") ? "name" : "path";
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
        return values.stream().map(TypeScriptFrontendClassifier::lower).toList();
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
