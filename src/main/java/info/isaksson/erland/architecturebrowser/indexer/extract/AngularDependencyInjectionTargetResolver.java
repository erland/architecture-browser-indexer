package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;

import java.util.LinkedHashMap;
import java.util.Map;

final class AngularDependencyInjectionTargetResolver {
    private AngularDependencyInjectionTargetResolver() {
    }

    static ExtractedEntityFact resolveAngularDiTarget(
        ExtractionAccumulator accumulator,
        String relativePath,
        String rawReference,
        Map<String, ExtractedEntityFact> namedEntities,
        EntityKind fallbackKind
    ) {
        String normalized = AngularReferenceSupport.normalizeReference(rawReference);
        ExtractedEntityFact existing = namedEntities.get(normalized);
        if (existing != null) {
            return existing;
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("targetClassification", fallbackKind == EntityKind.MODULE ? "angular-di-token" : "angular-di-target");
        metadata.put("resolution", "inferred-angular-di-target");
        metadata.put("external", false);
        metadata.put("inferredInternal", true);
        if (fallbackKind == EntityKind.MODULE) {
            metadata.put("angularToken", true);
            metadata.put("entityRole", "token");
            metadata.put("uiProfile", "angular-di-token");
        }
        ExtractedEntityFact inferred = ExtractionSupport.inferredTypeEntity(
            "angular",
            fallbackKind,
            normalized,
            relativePath,
            1,
            Map.copyOf(metadata)
        );
        accumulator.addEntity(inferred);
        return inferred;
    }
}
