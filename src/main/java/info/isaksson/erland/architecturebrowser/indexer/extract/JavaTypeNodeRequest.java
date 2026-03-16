package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionMode;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.Map;

record JavaTypeNodeRequest(
    SourceParseResult parseResult,
    ExtractionAccumulator accumulator,
    String relativePath,
    String packageName,
    ExtractionMode extractionMode,
    String packageScopeId,
    String fileEntityId,
    SyntaxNode node,
    JavaOwnerContext ownerContext,
    Map<String, String> importsBySimpleName,
    Map<String, JavaDeclaredType> declaredTypes,
    JavaExtractionContext extractionContext
) {
}
