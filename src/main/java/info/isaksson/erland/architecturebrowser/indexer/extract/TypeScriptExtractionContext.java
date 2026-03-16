package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

record TypeScriptExtractionContext(
    SourceParseResult parseResult,
    ExtractionAccumulator accumulator,
    String relativePath,
    ExtractionMode extractionMode,
    SyntaxNode root,
    ExtractedEntityFact fileEntity
) {
}
