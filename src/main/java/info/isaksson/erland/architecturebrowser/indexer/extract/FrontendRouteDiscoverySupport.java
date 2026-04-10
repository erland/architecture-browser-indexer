package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class FrontendRouteDiscoverySupport {
    private final FrontendRouteObjectDiscoverySupport objectDiscoverySupport = new FrontendRouteObjectDiscoverySupport();
    private final FrontendRouteJsxDiscoverySupport jsxDiscoverySupport = new FrontendRouteJsxDiscoverySupport();

    List<FrontendRouteCandidate> discover(String relativePath, String sourceText, Map<String, ExtractedEntityFact> namedEntities) {
        if (sourceText == null || sourceText.isBlank()) {
            return List.of();
        }
        List<FrontendRouteCandidate> candidates = new ArrayList<>();
        candidates.addAll(objectDiscoverySupport.discover(relativePath, sourceText, namedEntities));
        candidates.addAll(jsxDiscoverySupport.discover(relativePath, sourceText, namedEntities));
        candidates.sort(Comparator.comparingInt(FrontendRouteCandidate::start));
        return List.copyOf(candidates);
    }
}
