package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

final class ModuleDependenciesViewpointDeriver implements ArchitectureViewpointDeriver {
    @Override
    public ArchitectureViewpoint derive(ViewpointEvidence evidence) {
        boolean hasModuleDependencyViews = evidence.hasDependencyViewList("moduleDependencies")
            || evidence.hasDependencyViewList("compositionModuleDependencies")
            || evidence.hasDependencyViewList("routeModuleDependencies")
            || evidence.hasDependencyViewList("providerModuleDependencies")
            || evidence.hasDependencyViewList("hookModuleDependencies")
            || evidence.hasDependencyViewList("endpointModuleDependencies")
            || evidence.hasDependencyViewList("entityModelModuleDependencies")
            || evidence.hasDependencyViewList("observerModuleDependencies")
            || evidence.hasDependencyViewList("writePathModuleDependencies");
        boolean hasModuleSemantic = evidence.hasSemantic(ArchitecturalRelationshipSemantic.DEPENDS_ON_MODULE.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.BELONGS_TO_MODULE.id());
        boolean hasModuleBoundaries = !evidence.entityIdsForRole(ArchitecturalRole.MODULE_BOUNDARY.id()).isEmpty();
        String availability;
        double confidence;
        if (hasModuleDependencyViews || hasModuleSemantic) {
            availability = "available";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.74
                + (hasModuleDependencyViews ? 0.12 : 0.0)
                + (hasModuleSemantic ? 0.06 : 0.0)
                + (hasModuleBoundaries ? 0.04 : 0.0));
        } else if (hasModuleBoundaries || evidence.entityCountByKind(EntityKind.MODULE) > 1) {
            availability = "partial";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.38
                + (hasModuleBoundaries ? 0.14 : 0.0)
                + (evidence.entityCountByKind(EntityKind.MODULE) > 1 ? 0.12 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        return new ArchitectureViewpoint(
            "module-dependencies",
            "Module dependencies",
            "Shows component and module dependency structure available from the exported graph.",
            availability,
            confidence,
            null,
            ArchitectureViewpointDerivationSupport.roleIdsPresent(evidence, ArchitecturalRole.MODULE_BOUNDARY.id()),
            ArchitectureViewpointDerivationSupport.presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.DEPENDS_ON_MODULE.id(),
                ArchitecturalRelationshipSemantic.BELONGS_TO_MODULE.id()),
            null,
            ArchitectureViewpointDerivationSupport.moduleEvidenceSources(evidence, hasModuleDependencyViews, hasModuleSemantic, hasModuleBoundaries)
        );
    }
}
