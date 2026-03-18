package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.List;
import java.util.Map;

final class FrontendRoutingExtractor {
    private static final FrontendRouteDiscoverySupport discoverySupport = new FrontendRouteDiscoverySupport();
    private static final FrontendNavigationDiscoverySupport navigationDiscoverySupport = new FrontendNavigationDiscoverySupport();
    private static final FrontendRoutePathNormalizationSupport pathNormalizationSupport = new FrontendRoutePathNormalizationSupport();
    private static final FrontendRouteEmissionSupport emissionSupport = new FrontendRouteEmissionSupport();

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
        List<FrontendRouteCandidate> discoveredCandidates = discoverySupport.discover(relativePath, sourceText, namedEntities);
        List<FrontendNavigationCandidate> navigationCandidates = navigationDiscoverySupport.discover(relativePath, sourceText, namedEntities);
        if (discoveredCandidates.isEmpty() && navigationCandidates.isEmpty()) {
            return;
        }
        List<FrontendRouteCandidate> normalizedCandidates = pathNormalizationSupport.normalize(discoveredCandidates);
        emissionSupport.emit(accumulator, relativePath, normalizedCandidates, navigationCandidates, namedEntities, pathNormalizationSupport);
    }
}
