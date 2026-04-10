package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Conservative TypeScript relationship normalization mapping.
 */
final class TypeScriptArchitectureRelationshipNormalizationRule implements ArchitectureRelationshipNormalizationRule {
    @Override
    public NormalizedArchitectureRelationship normalize(ArchitectureRelationshipNormalizationContext context) {
        ArchitectureRelationship relationship = context.relationship();
        if (relationship == null) {
            return null;
        }
        ArchitectureEntity source = context.entitiesById().get(relationship.fromEntityId());
        ArchitectureEntity target = context.entitiesById().get(relationship.toEntityId());
        if (!TypeScriptArchitectureMetadataSupport.isTypeScriptBacked(relationship, source, target, context)) {
            return null;
        }

        List<String> semantics = new ArrayList<>();
        if (TypeScriptArchitectureRelationshipSemanticsSupport.containsRoute(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.CONTAINS_ROUTE.id());
        }
        if (TypeScriptArchitectureRelationshipSemanticsSupport.redirectsTo(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.REDIRECTS_TO.id());
        }
        if (TypeScriptArchitectureRelationshipSemanticsSupport.navigatesTo(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.NAVIGATES_TO.id());
        }
        if (TypeScriptArchitectureRelationshipSemanticsSupport.guardsRoute(relationship, source, target, context)) {
            semantics.add(ArchitecturalRelationshipSemantic.GUARDS_ROUTE.id());
        }
        if (TypeScriptArchitectureRelationshipSemanticsSupport.invokesUseCase(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id());
        }
        if (TypeScriptArchitectureRelationshipSemanticsSupport.callsExternalSystem(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.CALLS_EXTERNAL_SYSTEM.id());
        }
        if (TypeScriptArchitectureRelationshipSemanticsSupport.accessesPersistence(relationship, source, target)) {
            semantics.add(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id());
        }
        return semantics.isEmpty() ? null : new NormalizedArchitectureRelationship(semantics);
    }
}
