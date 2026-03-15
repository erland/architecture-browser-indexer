package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FrontendRoutingExtractor {
    private static final Pattern PATH_PATTERN = Pattern.compile("\\bpath\\s*:\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern ANGULAR_COMPONENT_PATTERN = Pattern.compile("\\bcomponent\\s*:\\s*([A-Z][A-Za-z0-9_]*)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*\\(\\)\\s*=>\\s*import\\([^)]*\\)\\s*\\.then\\s*\\([^=]*=>\\s*[^.]*\\.([A-Z][A-Za-z0-9_]*)\\)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_STRING_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*['\"][^#\"']*#([A-Z][A-Za-z0-9_]*)['\"]");
    private static final Pattern ANGULAR_GUARD_PATTERN = Pattern.compile("\\bcan(?:Activate|ActivateChild|Deactivate|Match|Load)\\s*:\\s*\\[([^]]+)]");
    private static final Pattern ANGULAR_RESOLVE_PATTERN = Pattern.compile("\\bresolve\\s*:\\s*\\{([^}]+)}");
    private static final Pattern REACT_ELEMENT_PATTERN = Pattern.compile("\\belement\\s*:\\s*<\\s*([A-Z][A-Za-z0-9_]*)\\b");
    private static final Pattern REACT_COMPONENT_PATTERN = Pattern.compile("\\b(?:Component|component)\\s*:\\s*([A-Z][A-Za-z0-9_]*)");
    private static final Pattern JSX_ROUTE_PATTERN = Pattern.compile("<Route\\b([^>]*)>", Pattern.DOTALL);
    private static final Pattern JSX_ROUTE_SELF_CLOSING_PATTERN = Pattern.compile("<Route\\b([^>]*)/>", Pattern.DOTALL);
    private static final Pattern JSX_PATH_ATTR_PATTERN = Pattern.compile("\\bpath\\s*=\\s*['\"]([^'\"]*)['\"]");
    private static final Pattern JSX_ELEMENT_ATTR_PATTERN = Pattern.compile("\\belement\\s*=\\s*\\{\\s*<\\s*([A-Z][A-Za-z0-9_]*)\\b");

    private FrontendRoutingExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        String sourceText,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || sourceText == null || sourceText.isBlank()) {
            return;
        }
        List<RouteCandidate> candidates = new ArrayList<>();
        candidates.addAll(extractObjectRoutes(relativePath, sourceText, namedEntities));
        candidates.addAll(extractJsxRoutes(relativePath, sourceText, namedEntities));
        if (candidates.isEmpty()) {
            return;
        }
        candidates.sort(Comparator.comparingInt(RouteCandidate::start));
        List<RouteCandidate> resolvedCandidates = new ArrayList<>();
        Map<RouteCandidate, ExtractedEntityFact> routeEntities = new LinkedHashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            RouteCandidate candidate = candidates.get(i);
            RouteCandidate parent = findParent(candidate, candidates, i);
            String fullPath = fullRoutePath(candidate, parent);
            RouteCandidate resolvedCandidate = candidate.withFullPath(fullPath);
            resolvedCandidates.add(resolvedCandidate);
            ExtractedEntityFact routeEntity = routeEntity(resolvedCandidate.framework(), relativePath, resolvedCandidate.path(), resolvedCandidate.fullPath(), resolvedCandidate.startLine(), resolvedCandidate.snippet());
            accumulator.addEntity(routeEntity);
            routeEntities.put(resolvedCandidate, routeEntity);
        }
        candidates = resolvedCandidates;
        for (RouteCandidate candidate : candidates) {
            ExtractedEntityFact routeEntity = routeEntities.get(candidate);
            RouteCandidate parent = findParent(candidate, candidates, candidates.indexOf(candidate));
            if (parent != null) {
                ExtractedEntityFact parentEntity = routeEntities.get(parent);
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    routeEntity.id(),
                    parentEntity.id(),
                    candidate.path(),
                    routeRef(candidate, relativePath),
                    "typescript",
                    relationshipMetadata(candidate.framework(), "childOf", candidate.path(), fullRoutePath(candidate, parent), true)
                ));
            }
            for (String targetName : candidate.targets()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, targetName, namedEntities, relativePath, "targets", "route-target", EntityKind.UI_MODULE);
            }
            for (String lazyTarget : candidate.lazyLoads()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, lazyTarget, namedEntities, relativePath, "lazyLoads", "route-lazy-target", EntityKind.MODULE);
            }
            for (String guard : candidate.guards()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, guard, namedEntities, relativePath, "guards", "route-guard", EntityKind.CLASS);
            }
            for (String resolver : candidate.resolvers()) {
                addRouteTargetRelationship(accumulator, candidate, routeEntity, resolver, namedEntities, relativePath, "resolves", "route-resolver", EntityKind.CLASS);
            }
        }
    }

    private static void addRouteTargetRelationship(
        ExtractionAccumulator accumulator,
        RouteCandidate candidate,
        ExtractedEntityFact routeEntity,
        String targetName,
        Map<String, ExtractedEntityFact> namedEntities,
        String relativePath,
        String frameworkRelationship,
        String targetClassification,
        EntityKind fallbackKind
    ) {
        if (targetName == null || targetName.isBlank()) {
            return;
        }
        ExtractedEntityFact targetEntity = namedEntities.get(targetName);
        boolean resolved = targetEntity != null;
        if (!resolved) {
            targetEntity = ExtractionSupport.inferredTypeEntity(
                candidate.framework(),
                fallbackKind,
                targetName,
                relativePath,
                candidate.startLine(),
                Map.of(
                    "framework", candidate.framework(),
                    "targetClassification", targetClassification,
                    "resolution", "inferred-frontend-route-target",
                    "external", false,
                    "inferredInternal", true
                )
            );
            accumulator.addEntity(targetEntity);
        }
        accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
            routeEntity.id(),
            targetEntity.id(),
            targetName,
            routeRef(candidate, relativePath),
            "typescript",
            relationshipMetadata(candidate.framework(), frameworkRelationship, candidate.path(), candidate.fullPath(), resolved)
        ));
    }

    private static List<RouteCandidate> extractObjectRoutes(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        List<ObjectSpan> objectSpans = objectSpans(sourceText);
        List<RouteCandidate> result = new ArrayList<>();
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

            result.add(new RouteCandidate(
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
                List.copyOf(resolvers)
            ));
        }
        return result;
    }

    private static List<RouteCandidate> extractJsxRoutes(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        if (!looksLikeReactSource(relativePath, sourceText, namedEntities)) {
            return List.of();
        }
        List<RouteCandidate> result = new ArrayList<>();
        extractJsxRoutes(result, sourceText, JSX_ROUTE_SELF_CLOSING_PATTERN);
        extractJsxRoutes(result, sourceText, JSX_ROUTE_PATTERN);
        return dedupeRoutes(result);
    }

    private static void extractJsxRoutes(List<RouteCandidate> result, String sourceText, Pattern pattern) {
        Matcher matcher = pattern.matcher(sourceText);
        while (matcher.find()) {
            String attrs = matcher.group(1);
            String path = firstGroup(JSX_PATH_ATTR_PATTERN, attrs);
            if (path == null) {
                continue;
            }
            LinkedHashSet<String> targets = new LinkedHashSet<>();
            addIfPresent(targets, firstGroup(JSX_ELEMENT_ATTR_PATTERN, attrs));
            result.add(new RouteCandidate(
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
                List.of()
            ));
        }
    }

    private static List<RouteCandidate> dedupeRoutes(List<RouteCandidate> routes) {
        LinkedHashMap<String, RouteCandidate> deduped = new LinkedHashMap<>();
        for (RouteCandidate route : routes) {
            String key = route.framework() + ":" + route.start() + ":" + route.path();
            deduped.putIfAbsent(key, route);
        }
        return List.copyOf(deduped.values());
    }

    private static RouteCandidate findParent(RouteCandidate candidate, List<RouteCandidate> candidates, int candidateIndex) {
        RouteCandidate parent = null;
        for (int i = 0; i < candidates.size(); i++) {
            if (i == candidateIndex) {
                continue;
            }
            RouteCandidate possibleParent = candidates.get(i);
            if (!possibleParent.framework().equals(candidate.framework())) {
                continue;
            }
            if (possibleParent.start() < candidate.start() && possibleParent.end() > candidate.end()) {
                if (parent == null || (possibleParent.end() - possibleParent.start()) < (parent.end() - parent.start())) {
                    parent = possibleParent;
                }
            }
        }
        return parent;
    }

    private static String fullRoutePath(RouteCandidate candidate, RouteCandidate parent) {
        String own = normalizedPath(candidate.path());
        if (parent == null) {
            return own;
        }
        String parentPath = normalizedPath(parent.fullPath() == null ? parent.path() : parent.fullPath());
        if ("/".equals(own)) {
            return parentPath;
        }
        if ("/".equals(parentPath)) {
            return own;
        }
        if (own.isBlank()) {
            return parentPath;
        }
        return normalizedPath(parentPath + "/" + own.replaceFirst("^/", ""));
    }

    private static String normalizedPath(String raw) {
        String path = raw == null ? "" : raw.strip();
        if (path.isBlank()) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        path = path.replaceAll("//+", "/");
        return path.isBlank() ? "/" : path;
    }

    private static ExtractedEntityFact routeEntity(String framework, String relativePath, String routePath, String fullPath, int line, String snippet) {
        String qualifiedName = framework + "-route:" + fullPath;
        return ExtractionSupport.inferredTypeEntity(
            framework,
            EntityKind.UI_MODULE,
            qualifiedName,
            relativePath,
            line,
            Map.of(
                "framework", framework,
                "route", true,
                "routePath", routePath,
                "routeFullPath", fullPath,
                "entityRole", "route",
                "targetClassification", "route-node",
                "external", false,
                "inferredInternal", true,
                "routeSnippet", snippet == null ? "" : snippet
            )
        );
    }

    private static Map<String, Object> relationshipMetadata(String framework, String frameworkRelationship, String path, String fullPath, boolean resolved) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", framework);
        metadata.put("frameworkRelationship", frameworkRelationship);
        metadata.put("dependencySource", framework + ":route-" + frameworkRelationship);
        metadata.put("dependencyCategory", "framework");
        metadata.put("routePath", path);
        metadata.put("routeFullPath", fullPath);
        metadata.put("resolvedFromRouteExtraction", resolved);
        metadata.put("targetClassification", "route-target");
        return Map.copyOf(metadata);
    }

    private static SourceReference routeRef(RouteCandidate candidate, String relativePath) {
        return ExtractionSupport.sourceRef(relativePath, candidate.startLine(), candidate.snippet(), Map.of(
            "language", "typescript",
            "framework", candidate.framework(),
            "routePath", candidate.path(),
            "routeFullPath", candidate.fullPath()
        ));
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
        for (String candidate : List.of(
            firstGroup(ANGULAR_COMPONENT_PATTERN, snippet),
            firstGroup(REACT_ELEMENT_PATTERN, snippet),
            firstGroup(REACT_COMPONENT_PATTERN, snippet),
            firstGroup(ANGULAR_LOAD_CHILDREN_PATTERN, snippet),
            firstGroup(ANGULAR_LOAD_CHILDREN_STRING_PATTERN, snippet)
        )) {
            if (candidate == null) {
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

    private record RouteCandidate(
        String framework,
        String path,
        String fullPath,
        int start,
        int end,
        int startLine,
        String snippet,
        List<String> targets,
        List<String> lazyLoads,
        List<String> guards,
        List<String> resolvers
    ) {
        RouteCandidate {
            framework = framework == null || framework.isBlank() ? "react" : framework;
            path = path == null ? "" : path;
            fullPath = fullPath == null ? path : fullPath;
            snippet = Objects.toString(snippet, "");
            targets = targets == null ? List.of() : List.copyOf(targets);
            lazyLoads = lazyLoads == null ? List.of() : List.copyOf(lazyLoads);
            guards = guards == null ? List.of() : List.copyOf(guards);
            resolvers = resolvers == null ? List.of() : List.copyOf(resolvers);
        }

        RouteCandidate withFullPath(String updatedFullPath) {
            return new RouteCandidate(framework, path, updatedFullPath, start, end, startLine, snippet, targets, lazyLoads, guards, resolvers);
        }
    }
}
