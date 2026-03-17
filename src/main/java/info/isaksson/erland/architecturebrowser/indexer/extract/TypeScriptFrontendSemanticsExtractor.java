package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.Map;

final class TypeScriptFrontendSemanticsExtractor {
    private TypeScriptFrontendSemanticsExtractor() {
    }

    static void extract(TypeScriptExtractionContext context, Map<String, ExtractedEntityFact> namedEntities) {
        AngularTypeScriptFrameworkEnrichmentSupport.extract(context, namedEntities);
        ReactTypeScriptFrameworkEnrichmentSupport.extract(context, namedEntities);
        FrontendRoutingExtractor.extract(context.accumulator(), context.relativePath(), context.parseResult().request().sourceText(), namedEntities);
    }
}
