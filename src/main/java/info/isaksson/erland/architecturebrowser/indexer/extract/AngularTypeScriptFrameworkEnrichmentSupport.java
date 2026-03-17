package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.Map;

final class AngularTypeScriptFrameworkEnrichmentSupport {
    private AngularTypeScriptFrameworkEnrichmentSupport() {
    }

    static void extract(TypeScriptExtractionContext context, Map<String, ExtractedEntityFact> namedEntities) {
        AngularFrameworkRelationshipExtractor.extract(context.accumulator(), context.relativePath(), namedEntities);
        AngularTemplateCompositionExtractor.extract(context.accumulator(), context.relativePath(), namedEntities);
        AngularDependencyInjectionExtractor.extract(
            context.accumulator(),
            context.relativePath(),
            context.parseResult().request().sourceText(),
            namedEntities
        );
    }
}
