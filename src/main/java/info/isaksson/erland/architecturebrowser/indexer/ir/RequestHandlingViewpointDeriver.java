package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

import java.util.ArrayList;
import java.util.List;

final class RequestHandlingViewpointDeriver implements ArchitectureViewpointDeriver {
    @Override
    public ArchitectureViewpoint derive(ViewpointEvidence evidence) {
        List<String> entrypoints = evidence.entityIdsForRole(ArchitecturalRole.API_ENTRYPOINT.id());
        List<String> services = evidence.entityIdsForRole(ArchitecturalRole.APPLICATION_SERVICE.id());
        boolean hasEntrypoint = !entrypoints.isEmpty();
        boolean hasDownstreamSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id());
        String availability;
        double confidence;
        if (hasEntrypoint && hasDownstreamSemantics) {
            availability = "available";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.78
                + (evidence.hasSemantic(ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id()) ? 0.08 : 0.0)
                + (evidence.hasSemantic(ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id()) ? 0.06 : 0.0));
        } else if (hasEntrypoint || hasDownstreamSemantics || !services.isEmpty()) {
            availability = "partial";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.42
                + (hasEntrypoint ? 0.12 : 0.0)
                + (hasDownstreamSemantics ? 0.12 : 0.0)
                + (!services.isEmpty() ? 0.08 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = new ArrayList<>();
        seedEntityIds.addAll(entrypoints);
        seedEntityIds.addAll(services);
        seedEntityIds = seedEntityIds.stream().distinct().sorted().toList();
        return new ArchitectureViewpoint(
            "request-handling",
            "Request handling",
            "Highlights request-serving paths from entrypoints through application services.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            ArchitectureViewpointDerivationSupport.roleIdsPresent(evidence,
                ArchitecturalRole.API_ENTRYPOINT.id(), ArchitecturalRole.APPLICATION_SERVICE.id()),
            ArchitectureViewpointDerivationSupport.presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.SERVES_REQUEST.id(),
                ArchitecturalRelationshipSemantic.INVOKES_USE_CASE.id(),
                ArchitecturalRelationshipSemantic.ACCESSES_PERSISTENCE.id()),
            null,
            ArchitectureViewpointDerivationSupport.evidenceSources(evidence, hasEntrypoint || !services.isEmpty(), true, false, false)
        );
    }
}
