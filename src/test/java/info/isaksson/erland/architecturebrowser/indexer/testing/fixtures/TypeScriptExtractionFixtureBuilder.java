package info.isaksson.erland.architecturebrowser.indexer.testing.fixtures;

import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractionService;
import info.isaksson.erland.architecturebrowser.indexer.extract.StructuralExtractorRegistry;
import info.isaksson.erland.architecturebrowser.indexer.extract.model.StructuralExtractionResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseBatchResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.ParseLanguage;
import info.isaksson.erland.architecturebrowser.indexer.parse.SourceParseResult;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;

public final class TypeScriptExtractionFixtureBuilder {
    private TypeScriptExtractionFixtureBuilder() {}

    public static StructuralExtractionResult extract(String relativePath, String source, SyntaxNode root) {
        SourceParseResult parseResult = ParseFixtureBuilder.parsedFile(relativePath, ParseLanguage.TYPESCRIPT, source, root);
        ParseBatchResult batch = ParseFixtureBuilder.successfulBatch(List.of(parseResult));
        return new StructuralExtractionService(StructuralExtractorRegistry.defaultRegistry()).extract(batch);
    }
}
