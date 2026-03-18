package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.Objects;

record FrontendNavigationCandidate(
    String framework,
    String sourceKind,
    String targetLiteral,
    int startLine,
    String snippet
) {
    FrontendNavigationCandidate {
        framework = framework == null || framework.isBlank() ? "react" : framework;
        sourceKind = sourceKind == null || sourceKind.isBlank() ? "link" : sourceKind;
        targetLiteral = targetLiteral == null ? "" : targetLiteral;
        snippet = Objects.toString(snippet, "");
    }
}
