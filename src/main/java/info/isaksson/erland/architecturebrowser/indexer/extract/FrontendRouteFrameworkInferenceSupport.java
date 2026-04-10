package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class FrontendRouteFrameworkInferenceSupport {
    private static final Pattern ANGULAR_COMPONENT_PATTERN = Pattern.compile("\\bcomponent\\s*:\\s*([A-Z][A-Za-z0-9_]*)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*\\(\\)\\s*=>\\s*import\\([^)]*\\)\\s*\\.then\\s*\\([^=]*=>\\s*[^.]*\\.([A-Z][A-Za-z0-9_]*)\\)");
    private static final Pattern ANGULAR_LOAD_CHILDREN_STRING_PATTERN = Pattern.compile("\\bloadChildren\\s*:\\s*['\"][^#\"']*#([A-Z][A-Za-z0-9_]*)['\"]");
    private static final Pattern REACT_ELEMENT_PATTERN = Pattern.compile("\\belement\\s*:\\s*<\\s*([A-Z][A-Za-z0-9_]*)\\b");
    private static final Pattern REACT_COMPONENT_PATTERN = Pattern.compile("\\b(?:Component|component)\\s*:\\s*([A-Z][A-Za-z0-9_]*)");

    String inferObjectFramework(String relativePath, String sourceText, String snippet, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerSnippet = snippet.toLowerCase(Locale.ROOT);
        String lowerSource = sourceText.toLowerCase(Locale.ROOT);
        if (lowerSnippet.contains("loadchildren") || lowerSnippet.contains("canactivate") || lowerSnippet.contains("resolve:")) {
            return "angular";
        }
        if (lowerSnippet.contains("element:") || lowerSource.contains("react-router") || relativePath.endsWith(".tsx")) {
            return "react";
        }
        for (String candidate : new String[] {
            FrontendRoutePatternSupport.firstGroup(ANGULAR_COMPONENT_PATTERN, snippet),
            FrontendRoutePatternSupport.firstGroup(REACT_ELEMENT_PATTERN, snippet),
            FrontendRoutePatternSupport.firstGroup(REACT_COMPONENT_PATTERN, snippet),
            FrontendRoutePatternSupport.firstGroup(ANGULAR_LOAD_CHILDREN_PATTERN, snippet),
            FrontendRoutePatternSupport.firstGroup(ANGULAR_LOAD_CHILDREN_STRING_PATTERN, snippet)
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

    boolean looksLikeReactSource(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        String lowerSource = sourceText.toLowerCase(Locale.ROOT);
        if (relativePath.endsWith(".tsx") || lowerSource.contains("react-router") || lowerSource.contains("<route")) {
            return true;
        }
        return namedEntities.values().stream().anyMatch(entity -> "react".equals(entity.metadata().get("framework")));
    }
}
