package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FrontendRouteJsxDiscoverySupport {
    private static final Pattern JSX_ROUTE_PATTERN = Pattern.compile("<Route\\b([^>]*)>", Pattern.DOTALL);
    private static final Pattern JSX_ROUTE_SELF_CLOSING_PATTERN = Pattern.compile("<Route\\b([^>]*)/>", Pattern.DOTALL);
    private static final Pattern JSX_PATH_ATTR_PATTERN = Pattern.compile("\\bpath\\s*=\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern JSX_ELEMENT_ATTR_PATTERN = Pattern.compile("\\belement\\s*=\\s*\\{\\s*<\\s*([A-Z][A-Za-z0-9_]*)\\b");

    private final FrontendRouteFrameworkInferenceSupport frameworkInferenceSupport = new FrontendRouteFrameworkInferenceSupport();

    List<FrontendRouteCandidate> discover(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        if (!frameworkInferenceSupport.looksLikeReactSource(relativePath, sourceText, namedEntities)) {
            return List.of();
        }
        List<FrontendRouteCandidate> result = new ArrayList<>();
        extractJsxRoutes(result, sourceText, JSX_ROUTE_SELF_CLOSING_PATTERN);
        extractJsxRoutes(result, sourceText, JSX_ROUTE_PATTERN);
        return dedupeRoutes(result);
    }

    private void extractJsxRoutes(List<FrontendRouteCandidate> result, String sourceText, Pattern pattern) {
        Matcher matcher = pattern.matcher(sourceText);
        while (matcher.find()) {
            String attrs = matcher.group(1);
            String path = FrontendRoutePatternSupport.firstGroup(JSX_PATH_ATTR_PATTERN, attrs);
            if (path == null) {
                continue;
            }
            LinkedHashSet<String> targets = new LinkedHashSet<>();
            FrontendRoutePatternSupport.addIfPresent(targets, FrontendRoutePatternSupport.firstGroup(JSX_ELEMENT_ATTR_PATTERN, attrs));
            result.add(new FrontendRouteCandidate(
                "react",
                path,
                null,
                matcher.start(),
                matcher.end(),
                oneBasedLine(sourceText, matcher.start()),
                matcher.group(),
                List.copyOf(targets),
                List.of(),
                List.of(),
                List.of(),
                "jsx-route",
                ""
            ));
        }
    }

    private static List<FrontendRouteCandidate> dedupeRoutes(List<FrontendRouteCandidate> routes) {
        LinkedHashMap<String, FrontendRouteCandidate> deduped = new LinkedHashMap<>();
        for (FrontendRouteCandidate route : routes) {
            String key = route.framework() + ":" + route.start() + ":" + route.path();
            deduped.putIfAbsent(key, route);
        }
        return List.copyOf(deduped.values());
    }

    private static int oneBasedLine(String text, int offset) {
        int line = 1;
        int max = Math.min(Math.max(offset, 0), text.length());
        for (int i = 0; i < max; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
