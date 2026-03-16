package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.Map;

final class TypeScriptFrontendSemanticsExtractor {
    private TypeScriptFrontendSemanticsExtractor() {
    }

    static void extract(TypeScriptExtractionContext context, Map<String, ExtractedEntityFact> namedEntities) {
        AngularFrameworkRelationshipExtractor.extract(context.accumulator(), context.relativePath(), namedEntities);
        AngularTemplateCompositionExtractor.extract(context.accumulator(), context.relativePath(), namedEntities);
        AngularDependencyInjectionExtractor.extract(context.accumulator(), context.relativePath(), context.parseResult().request().sourceText(), namedEntities);
        ReactJsxCompositionExtractor.extract(context.accumulator(), context.relativePath(), namedEntities);
        ReactContextGraphExtractor.extract(context.accumulator(), context.relativePath(), context.parseResult().request().sourceText(), namedEntities);
        ReactCustomHookExtractor.extract(context.accumulator(), context.relativePath(), context.parseResult().request().sourceText(), namedEntities);
        FrontendRoutingExtractor.extract(context.accumulator(), context.relativePath(), context.parseResult().request().sourceText(), namedEntities);
    }
}
