package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

record JavaMethodContext(
    JavaExtractionContext extractionContext,
    SyntaxNode methodNode,
    ExtractedEntityFact methodEntity,
    String ownerTypeEntityId,
    String ownerQualifiedName,
    String ownerTypeSnippet,
    SourceReference sourceRef,
    String snippet
) {}
