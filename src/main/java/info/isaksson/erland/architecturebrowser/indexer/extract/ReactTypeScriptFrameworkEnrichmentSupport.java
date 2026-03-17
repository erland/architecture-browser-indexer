package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.Map;

final class ReactTypeScriptFrameworkEnrichmentSupport {
    private ReactTypeScriptFrameworkEnrichmentSupport() {
    }

    static void extract(TypeScriptExtractionContext context, Map<String, ExtractedEntityFact> namedEntities) {
        ReactJsxCompositionExtractor.extract(context.accumulator(), context.relativePath(), namedEntities);
        ReactContextGraphExtractor.extract(
            context.accumulator(),
            context.relativePath(),
            context.parseResult().request().sourceText(),
            namedEntities
        );
        ReactCustomHookExtractor.extract(
            context.accumulator(),
            context.relativePath(),
            context.parseResult().request().sourceText(),
            namedEntities
        );
    }
}
