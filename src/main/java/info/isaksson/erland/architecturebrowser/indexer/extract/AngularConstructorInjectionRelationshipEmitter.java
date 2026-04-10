package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

final class AngularConstructorInjectionRelationshipEmitter {
    private AngularConstructorInjectionRelationshipEmitter() {
    }

    static void extractConstructorInjectionRelationships(
        ExtractionAccumulator accumulator,
        String relativePath,
        ExtractedEntityFact ownerEntity,
        Map<String, ExtractedEntityFact> namedEntities
    ) {
        String snippet = Objects.toString(AngularSourceSupport.primaryRef(ownerEntity, relativePath).snippet(), "");
        if (snippet.isBlank()) {
            return;
        }
        for (String constructorParameters : AngularDependencyInjectionReferenceSupport.extractConstructorParameterBlocks(snippet)) {
            for (String parameter : AngularLiteralSupport.splitTopLevel(constructorParameters, ',')) {
                AngularInjectionReference reference = AngularDependencyInjectionReferenceSupport.parseInjectionReference(parameter);
                if (reference == null || reference.targetName().isBlank()) {
                    continue;
                }
                ExtractedEntityFact injectedEntity = AngularDependencyInjectionTargetResolver.resolveAngularDiTarget(
                    accumulator,
                    relativePath,
                    reference.targetName(),
                    namedEntities,
                    reference.kind()
                );
                accumulator.addRelationship(ExtractionSupport.dependencyRelationship(
                    ownerEntity.id(),
                    injectedEntity.id(),
                    reference.label(),
                    AngularSourceSupport.primaryRef(ownerEntity, relativePath),
                    "typescript",
                    injectionRelationshipMetadata(reference, true)
                ));
            }
        }
    }

    private static Map<String, Object> injectionRelationshipMetadata(AngularInjectionReference reference, boolean resolved) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("framework", "angular");
        metadata.put("frameworkRelationship", "injects");
        metadata.put("dependencySource", "angular:injects");
        metadata.put("dependencyCategory", "di");
        metadata.put("dependencyTargetBoundary", "internal");
        metadata.put("targetClassification", reference.kind() == info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind.MODULE ? "angular-di-token" : "angular-di-target");
        metadata.put("resolvedFromAngularDiExtraction", resolved);
        metadata.put("injectionKind", "constructor");
        metadata.put("injectionReferenceKind", reference.referenceKind());
        metadata.put("angularReference", reference.targetName());
        return Map.copyOf(metadata);
    }
}
