package info.isaksson.erland.architecturebrowser.indexer.extract.model;

import java.util.Map;

public record ExtractionSummary(
    int filesVisited,
    int filesExtracted,
    Map<String, Integer> extractedByLanguage,
    Map<String, Integer> extractedByMode,
    int entityCount,
    int relationshipCount
) {
    public ExtractionSummary {
        extractedByLanguage = extractedByLanguage == null ? Map.of() : Map.copyOf(extractedByLanguage);
        extractedByMode = extractedByMode == null ? Map.of() : Map.copyOf(extractedByMode);
    }
}
