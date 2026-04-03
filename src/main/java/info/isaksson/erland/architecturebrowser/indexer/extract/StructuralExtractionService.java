package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.Map;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractionSummary;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;

public final class StructuralExtractionService {
    private final StructuralExtractorRegistry registry;

    public StructuralExtractionService(StructuralExtractorRegistry registry) {
        this.registry = registry;
    }

    public StructuralExtractionResult extract(ParseBatchResult parseBatchResult) {
        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        Map<String, JavaDeclaredType> workspaceDeclaredTypes = JavaWorkspaceDeclarationRegistry.discover(parseBatchResult);
        registry.find(info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage.JAVA)
            .filter(JavaStructuralExtractor.class::isInstance)
            .map(JavaStructuralExtractor.class::cast)
            .ifPresent(extractor -> extractor.setWorkspaceDeclaredTypes(workspaceDeclaredTypes));
        if (parseBatchResult != null) {
            for (SourceParseResult result : parseBatchResult.results()) {
                registry.find(result.request().language())
                    .ifPresent(extractor -> extractor.extract(result, accumulator));
            }
        }
        return new StructuralExtractionResult(
            accumulator.scopes(),
            accumulator.entities(),
            accumulator.relationships(),
            accumulator.diagnostics(),
            new ExtractionSummary(
                accumulator.filesVisited(),
                accumulator.filesExtracted(),
                accumulator.extractedByLanguage(),
                accumulator.extractedByMode(),
                accumulator.entities().size(),
                accumulator.relationships().size()
            )
        );
    }
}
