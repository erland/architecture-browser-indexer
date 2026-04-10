package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.LinkedHashSet;
import java.util.Map;

final class AngularDependencyInjectionExtractor {
    private AngularDependencyInjectionExtractor() {
    }

    static void extract(
        ExtractionAccumulator accumulator,
        String relativePath,
        String sourceText,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        if (accumulator == null || relativePath == null || namedEntities == null || namedEntities.isEmpty()) {
            return;
        }
        if (!AngularDependencyInjectionDetectionSupport.looksLikeAngularDiSource(relativePath, sourceText, namedEntities)) {
            return;
        }

        for (ExtractedEntityFact entity : new LinkedHashSet<>(namedEntities.values())) {
            if (entity == null) {
                continue;
            }
            if (AngularDependencyInjectionDetectionSupport.isAngularProviderOwner(entity)) {
                AngularProviderRelationshipEmitter.extractProviderRelationships(accumulator, relativePath, entity, namedEntities);
            }
            if (AngularDependencyInjectionDetectionSupport.isAngularInjectableConsumer(entity)) {
                AngularConstructorInjectionRelationshipEmitter.extractConstructorInjectionRelationships(accumulator, relativePath, entity, namedEntities);
            }
        }
    }
}
