package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

record JavaFieldContext(
    JavaExtractionContext extractionContext,
    SyntaxNode fieldNode,
    ExtractedEntityFact fieldEntity,
    String ownerTypeEntityId,
    String ownerQualifiedName,
    String ownerTypeSnippet
) {}
