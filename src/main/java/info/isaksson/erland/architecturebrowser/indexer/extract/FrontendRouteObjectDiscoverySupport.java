package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FrontendRouteObjectDiscoverySupport {
    private static final Pattern PATH_PATTERN = Pattern.compile("\\bpath\\s*:\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern ANGULAR_COMPONENT_PATTERN = Pattern.compile("\\bcomponent\\s*:\\s*([A-Z][A-Za-z0-9_]*)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*\\(\\)\\s*=>\\s*import\\([^)]*\\)\\s*\\.then\\s*\\([^=]*=>\\s*[^.]*\\.([A-Z][A-Za-z0-9_]*)\\)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_STRING_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*['\"][^#\"']*#([A-Z][A-Za-z0-9_]*)['\"]");
    private static final Pattern ANGULAR_GUARD_PATTERN = Pattern.compile("\\bcan(?:Activate|ActivateChild|Deactivate|Match|Load)\\s*:\\s*\\[([^]]+)]");
    private static final Pattern ANGULAR_RESOLVE_PATTERN = Pattern.compile("\\bresolve\\s*:\\s*\\{([^}]+)}");
    private static final Pattern REDIRECT_TO_PATTERN = Pattern.compile("\\bredirectTo\\s*:\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern REACT_ELEMENT_PATTERN = Pattern.compile("\\belement\\s*:\\s*<\\s*([A-Z][A-Za-z0-9_]*)\\b");
    private static final Pattern REACT_COMPONENT_PATTERN = Pattern.compile("\\b(?:Component|component)\\s*:\\s*([A-Z][A-Za-z0-9_]*)");

    private final FrontendRouteFrameworkInferenceSupport frameworkInferenceSupport = new FrontendRouteFrameworkInferenceSupport();

    List<FrontendRouteCandidate> discover(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        List<ObjectSpan> objectSpans = objectSpans(sourceText);
        List<FrontendRouteCandidate> result = new ArrayList<>();
        for (ObjectSpan span : objectSpans) {
            String snippet = span.snippet();
            String path = FrontendRoutePatternSupport.firstGroup(PATH_PATTERN, snippet);
            if (path == null || !looksLikeRouteObject(snippet)) {
                continue;
            }
            String framework = frameworkInferenceSupport.inferObjectFramework(relativePath, sourceText, snippet, namedEntities);
            LinkedHashSet<String> targets = new LinkedHashSet<>();
            FrontendRoutePatternSupport.addIfPresent(targets, FrontendRoutePatternSupport.firstGroup(ANGULAR_COMPONENT_PATTERN, snippet));
            FrontendRoutePatternSupport.addIfPresent(targets, FrontendRoutePatternSupport.firstGroup(REACT_ELEMENT_PATTERN, snippet));
            FrontendRoutePatternSupport.addIfPresent(targets, FrontendRoutePatternSupport.firstGroup(REACT_COMPONENT_PATTERN, snippet));

            LinkedHashSet<String> lazyLoads = new LinkedHashSet<>();
            FrontendRoutePatternSupport.addIfPresent(lazyLoads, FrontendRoutePatternSupport.firstGroup(ANGULAR_LOAD_CHILDREN_PATTERN, snippet));
            FrontendRoutePatternSupport.addIfPresent(lazyLoads, FrontendRoutePatternSupport.firstGroup(ANGULAR_LOAD_CHILDREN_STRING_PATTERN, snippet));

            LinkedHashSet<String> guards = new LinkedHashSet<>();
            Matcher guardsMatcher = ANGULAR_GUARD_PATTERN.matcher(snippet);
            while (guardsMatcher.find()) {
                guards.addAll(FrontendRoutePatternSupport.typeIdentifiers(guardsMatcher.group(1)));
            }

            LinkedHashSet<String> resolvers = new LinkedHashSet<>();
            Matcher resolversMatcher = ANGULAR_RESOLVE_PATTERN.matcher(snippet);
            while (resolversMatcher.find()) {
                resolvers.addAll(FrontendRoutePatternSupport.typeIdentifiers(resolversMatcher.group(1)));
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
                FrontendRoutePatternSupport.firstGroup(REDIRECT_TO_PATTERN, snippet)
            ));
        }
        result.sort(Comparator.comparingInt(FrontendRouteCandidate::start));
        return List.copyOf(result);
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
            if (ch == '\\' && (inSingle || inDouble || inTemplate)) {
                escaping = true;
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

    private record ObjectSpan(int start, int end, String snippet) {
    }
}
