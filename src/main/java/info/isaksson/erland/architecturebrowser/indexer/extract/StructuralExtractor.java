package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;

public interface StructuralExtractor {
    ParseLanguage language();

    ExtractionAccumulator extract(SourceParseResult parseResult, ExtractionAccumulator accumulator);
}
