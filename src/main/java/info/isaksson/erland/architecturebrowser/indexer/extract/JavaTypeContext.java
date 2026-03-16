package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

record JavaTypeContext(
    JavaExtractionContext extractionContext,
    SyntaxNode typeNode,
    ExtractedEntityFact typeEntity
) {}
