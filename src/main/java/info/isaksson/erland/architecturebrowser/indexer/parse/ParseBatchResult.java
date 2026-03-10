package info.isaksson.erland.architecturebrowser.indexer.parse;

import java.util.List;
import java.util.Map;

public record ParseBatchResult(
    List<SourceParseResult> results,
    Map<ParseLanguage, Integer> attemptedByLanguage,
    Map<ParseStatus, Integer> countsByStatus
) {
    public ParseBatchResult {
        results = results == null ? List.of() : List.copyOf(results);
        attemptedByLanguage = attemptedByLanguage == null ? Map.of() : Map.copyOf(attemptedByLanguage);
        countsByStatus = countsByStatus == null ? Map.of() : Map.copyOf(countsByStatus);
    }
}
