package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FrontendNavigationDiscoverySupport {
    private static final Pattern JSX_LINK_PATTERN = Pattern.compile("<(?:Link|NavLink)\\b[^>]*\\bto\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.DOTALL);
    private static final Pattern ANGULAR_LINK_ATTR_PATTERN = Pattern.compile("\\brouterLink\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern ANGULAR_LINK_BINDING_PATTERN = Pattern.compile("\\[routerLink]\\s*=\\s*\"\\s*\\[\\s*['\"]([^'\"]+)['\"]\\s*]\\s*\"");
    private static final Pattern NAVIGATE_CALL_PATTERN = Pattern.compile("\\bnavigate\\s*\\(\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern NAVIGATE_BY_URL_PATTERN = Pattern.compile("\\bnavigateByUrl\\s*\\(\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern ROUTER_NAVIGATE_ARRAY_PATTERN = Pattern.compile("\\brouter\\s*\\.\\s*navigate\\s*\\(\\s*\\[\\s*['\"]([^'\"]+)['\"]\\s*]\\s*\\)");

    List<FrontendNavigationCandidate> discover(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        if (sourceText == null || sourceText.isBlank()) {
            return List.of();
        }
        String framework = inferFramework(relativePath, sourceText, namedEntities);
        Map<String, FrontendNavigationCandidate> deduped = new LinkedHashMap<>();
        collect(deduped, sourceText, JSX_LINK_PATTERN, framework, "link");
        collect(deduped, sourceText, ANGULAR_LINK_ATTR_PATTERN, framework, "link");
        collect(deduped, sourceText, ANGULAR_LINK_BINDING_PATTERN, framework, "link");
        collect(deduped, sourceText, NAVIGATE_CALL_PATTERN, framework, "navigate-call");
        collect(deduped, sourceText, NAVIGATE_BY_URL_PATTERN, framework, "navigate-call");
        collect(deduped, sourceText, ROUTER_NAVIGATE_ARRAY_PATTERN, framework, "navigate-call");
        return List.copyOf(deduped.values());
    }

    private void collect(Map<String, FrontendNavigationCandidate> sink, String sourceText, Pattern pattern, String framework, String sourceKind) {
        Matcher matcher = pattern.matcher(sourceText);
        while (matcher.find()) {
            String literal = matcher.group(1);
            if (literal == null || literal.isBlank()) {
                continue;
            }
            int start = matcher.start();
            int end = Math.min(sourceText.length(), matcher.end());
            String snippet = sourceText.substring(start, end);
            FrontendNavigationCandidate candidate = new FrontendNavigationCandidate(
                framework,
                sourceKind,
                literal,
                oneBasedLine(sourceText, start),
                snippet
            );
            sink.putIfAbsent(framework + ":" + sourceKind + ":" + candidate.startLine() + ":" + literal, candidate);
        }
    }

    private static String inferFramework(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerSource = sourceText.toLowerCase(Locale.ROOT);
        if (relativePath.endsWith(".tsx") || lowerSource.contains("react-router") || lowerSource.contains("<link") || lowerSource.contains("<navlink")) {
            return "react";
        }
        if (lowerSource.contains("@angular/router") || lowerSource.contains("routerlink") || lowerSource.contains("navigatebyurl")) {
            return "angular";
        }
        return namedEntities.values().stream()
            .map(entity -> Objects.toString(entity.metadata().get("framework"), ""))
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse("react");
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
