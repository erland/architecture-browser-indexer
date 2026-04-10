package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

final class ApiSurfaceViewpointDeriver implements ArchitectureViewpointDeriver {
    @Override
    public ArchitectureViewpoint derive(ViewpointEvidence evidence) {
        java.util.List<String> entrypoints = evidence.entityIdsForRole(ArchitecturalRole.API_ENTRYPOINT.id());
        boolean hasEntryPoints = !entrypoints.isEmpty();
        String availability = hasEntryPoints ? "available" : "unavailable";
        double confidence = hasEntryPoints
            ? ArchitectureViewpointDerivationSupport.clamp(0.72 + Math.min(0.24, entrypoints.size() * 0.08))
            : 0.0;
        return new ArchitectureViewpoint(
            "api-surface",
            "API surface",
            "Highlights externally exposed API entrypoints and the first service hop behind them when available.",
            availability,
            confidence,
            hasEntryPoints ? entrypoints : null,
            hasEntryPoints ? java.util.List.of(ArchitecturalRole.API_ENTRYPOINT.id()) : null,
            evidence.hasSemantic(ArchitecturalRelationshipSemantic.SERVES_REQUEST.id())
                ? java.util.List.of(ArchitecturalRelationshipSemantic.SERVES_REQUEST.id())
                : null,
            null,
            ArchitectureViewpointDerivationSupport.evidenceSources(evidence, hasEntryPoints, false, false, false)
        );
    }
}
