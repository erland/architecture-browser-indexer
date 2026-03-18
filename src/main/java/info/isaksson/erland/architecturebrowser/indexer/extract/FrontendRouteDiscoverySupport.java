package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FrontendRouteDiscoverySupport {
    private static final Pattern PATH_PATTERN = Pattern.compile("\\bpath\\s*:\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern ANGULAR_COMPONENT_PATTERN = Pattern.compile("\\bcomponent\\s*:\\s*([A-Z][A-Za-z0-9_]*)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*\\(\\)\\s*=>\\s*import\\([^)]*\\)\\s*\\.then\\s*\\([^=]*=>\\s*[^.]*\\.([A-Z][A-Za-z0-9_]*)\\)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_STRING_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*['\"][^#\"']*#([A-Z][A-Za-z0-9_]*)['\"]");
    private static final Pattern ANGULAR_GUARD_PATTERN = Pattern.compile("\\bcan(?:Activate|ActivateChild|Deactivate|Match|Load)\\s*:\\s*\\[([^]]+)]");
    private static final Pattern ANGULAR_RESOLVE_PATTERN = Pattern.compile("\\bresolve\\s*:\\s*\\{([^}]+)}");
    private static final Pattern REDIRECT_TO_PATTERN = Pattern.compile("\\bredirectTo\\s*:\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern REACT_ELEMENT_PATTERN = Pattern.compile("\\belement\\s*:\\s*<\\s*([A-Z][A-Za-z0-9_]*)\\b");
    private static final Pattern REACT_COMPONENT_PATTERN = Pattern.compile("\\b(?:Component|component)\\s*:\\s*([A-Z][A-Za-z0-9_]*)");
    private static final Pattern JSX_ROUTE_PATTERN = Pattern.compile("<Route\\b([^>]*)>", Pattern.DOTALL);
    private static final Pattern JSX_ROUTE_SELF_CLOSING_PATTERN = Pattern.compile("<Route\\b([^>]*)/>", Pattern.DOTALL);
    private static final Pattern JSX_PATH_ATTR_PATTERN = Pattern.compile("\\bpath\\s*=\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern JSX_ELEMENT_ATTR_PATTERN = Pattern.compile("\\belement\\s*=\\s*\\{\\s*<\\s*([A-Z][A-Za-z0-9_]*)\\b");

    List<FrontendRouteCandidate> discover(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        if (sourceText == null || sourceText.isBlank()) {
            return List.of();
        }
        List<FrontendRouteCandidate> candidates = new ArrayList<>();
        candidates.addAll(extractObjectRoutes(relativePath, sourceText, namedEntities));
        candidates.addAll(extractJsxRoutes(relativePath, sourceText, namedEntities));
        candidates.sort(Comparator.comparingInt(FrontendRouteCandidate::start));
        return List.copyOf(candidates);
    }

    private List<FrontendRouteCandidate> extractObjectRoutes(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        List<ObjectSpan> objectSpans = objectSpans(sourceText);
        List<FrontendRouteCandidate> result = new ArrayList<>();
        for (ObjectSpan span : objectSpans) {
            String snippet = span.snippet();
            String path = firstGroup(PATH_PATTERN, snippet);
            if (path == null || !looksLikeRouteObject(snippet)) {
                continue;
            }
            String framework = inferObjectFramework(relativePath, sourceText, snippet, namedEntities);
            LinkedHashSet<String> targets = new LinkedHashSet<>();
            addIfPresent(targets, firstGroup(ANGULAR_COMPONENT_PATTERN, snippet));
            addIfPresent(targets, firstGroup(REACT_ELEMENT_PATTERN, snippet));
            addIfPresent(targets, firstGroup(REACT_COMPONENT_PATTERN, snippet));

            LinkedHashSet<String> lazyLoads = new LinkedHashSet<>();
            addIfPresent(lazyLoads, firstGroup(ANGULAR_LOAD_CHILDREN_PATTERN, snippet));
            addIfPresent(lazyLoads, firstGroup(ANGULAR_LOAD_CHILDREN_STRING_PATTERN, snippet));

            LinkedHashSet<String> guards = new LinkedHashSet<>();
            Matcher guardsMatcher = ANGULAR_GUARD_PATTERN.matcher(snippet);
            while (guardsMatcher.find()) {
                guards.addAll(typeIdentifiers(guardsMatcher.group(1)));
            }

            LinkedHashSet<String> resolvers = new LinkedHashSet<>();
            Matcher resolversMatcher = ANGULAR_RESOLVE_PATTERN.matcher(snippet);
            while (resolversMatcher.find()) {
                resolvers.addAll(typeIdentifiers(resolversMatcher.group(1)));
            }

            result.add(new FrontendRouteCandidate(
                framework,
                path,
                null,
                span.start(),
                span.end(),
                oneBasedLine(sourceText, span.start()),
                snippet,
                List.copyOf(targets),
                List.copyOf(lazyLoads),
                List.copyOf(guards),
                List.copyOf(resolvers),
                "route-object",
                firstGroup(REDIRECT_TO_PATTERN, snippet)
            ));
        }
        return result;
    }

    private List<FrontendRouteCandidate> extractJsxRoutes(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        if (!looksLikeReactSource(relativePath, sourceText, namedEntities)) {
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
            String path = firstGroup(JSX_PATH_ATTR_PATTERN, attrs);
            if (path == null) {
                continue;
            }
            LinkedHashSet<String> targets = new LinkedHashSet<>();
            addIfPresent(targets, firstGroup(JSX_ELEMENT_ATTR_PATTERN, attrs));
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

    private static boolean looksLikeRouteObject(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return false;
        }
        String lower = snippet.toLowerCase(Locale.ROOT);
        return lower.contains("path:") && (lower.contains("component:")
            || lower.contains("loadchildren:")
            || lower.contains("children:")
            || lower.contains("element:")
            || lower.contains("component:")
            || lower.contains("redirectto:"));
    }

    private static boolean looksLikeReactSource(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerSource = sourceText.toLowerCase(Locale.ROOT);
        if (relativePath.endsWith(".tsx") || lowerSource.contains("react-router") || lowerSource.contains("<route")) {
            return true;
        }
        return namedEntities.values().stream().anyMatch(entity -> "react".equals(entity.metadata().get("framework")));
    }

    private static String inferObjectFramework(String relativePath, String sourceText, String snippet, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerSnippet = snippet.toLowerCase(Locale.ROOT);
        String lowerSource = sourceText.toLowerCase(Locale.ROOT);
        if (lowerSnippet.contains("loadchildren") || lowerSnippet.contains("canactivate") || lowerSnippet.contains("resolve:")) {
            return "angular";
        }
        if (lowerSnippet.contains("element:") || lowerSource.contains("react-router") || relativePath.endsWith(".tsx")) {
            return "react";
        }
        for (String candidate : new String[] {
            firstGroup(ANGULAR_COMPONENT_PATTERN, snippet),
            firstGroup(REACT_ELEMENT_PATTERN, snippet),
            firstGroup(REACT_COMPONENT_PATTERN, snippet),
            firstGroup(ANGULAR_LOAD_CHILDREN_PATTERN, snippet),
            firstGroup(ANGULAR_LOAD_CHILDREN_STRING_PATTERN, snippet)
        }) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            ExtractedEntityFact entity = namedEntities.get(candidate);
            if (entity != null && entity.metadata().get("framework") instanceof String framework && !framework.isBlank()) {
                return framework;
            }
        }
        return lowerSource.contains("@angular/router") ? "angular" : "react";
    }

    private static List<ObjectSpan> objectSpans(String source) {
        List<ObjectSpan> spans = new ArrayList<>();
        if (source == null || source.isBlank()) {
            return spans;
        }
        List<Integer> stack = new ArrayList<>();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inTemplate = false;
        boolean escaping = false;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (ch == '\\') {
                escaping = inSingle || inDouble || inTemplate;
                continue;
            }
            if (!inDouble && !inTemplate && ch == '\'') {
                inSingle = !inSingle;
                continue;
            }
            if (!inSingle && !inTemplate && ch == '"') {
                inDouble = !inDouble;
                continue;
            }
            if (!inSingle && !inDouble && ch == '`') {
                inTemplate = !inTemplate;
                continue;
            }
            if (inSingle || inDouble || inTemplate) {
                continue;
            }
            if (ch == '{') {
                stack.add(i);
            } else if (ch == '}' && !stack.isEmpty()) {
                int start = stack.remove(stack.size() - 1);
                spans.add(new ObjectSpan(start, i + 1, source.substring(start, i + 1)));
            }
        }
        spans.sort(Comparator.comparingInt(ObjectSpan::start));
        return spans;
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

    private static String firstGroup(Pattern pattern, String text) {
        if (pattern == null || text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static LinkedHashSet<String> typeIdentifiers(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        Matcher matcher = Pattern.compile("([A-Z][A-Za-z0-9_]*)").matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private static void addIfPresent(LinkedHashSet<String> targets, String value) {
        if (value != null && !value.isBlank()) {
            targets.add(value);
        }
    }

    private record ObjectSpan(int start, int end, String snippet) {
    }
}
