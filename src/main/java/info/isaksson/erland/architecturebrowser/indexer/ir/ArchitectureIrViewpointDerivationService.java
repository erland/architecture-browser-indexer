package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step 7: derives conservative canonical viewpoint availability from normalized roles,
 * traits, relationship semantics, and already assembled dependency-view evidence.
 */
public final class ArchitectureIrViewpointDerivationService {
    private static final List<ArchitectureViewpointDeriver> VIEWPOINT_DERIVERS = List.of(
        new ApiSurfaceViewpointDeriver(),
        new RequestHandlingViewpointDeriver(),
        new PersistenceModelViewpointDeriver(),
        new IntegrationMapViewpointDeriver(),
        new ModuleDependenciesViewpointDeriver(),
        new UiNavigationViewpointDeriver()
    );

    private ArchitectureIrViewpointDerivationService() {
    }

    public static List<ArchitectureViewpoint> derive(
        List<ArchitectureEntity> entities,
        List<ArchitectureRelationship> relationships,
        Map<String, Object> dependencyViews
    ) {
        ViewpointEvidence evidence = ViewpointEvidence.from(entities, relationships, dependencyViews);
        List<ArchitectureViewpoint> viewpoints = new ArrayList<>();
        for (ArchitectureViewpointDeriver deriver : VIEWPOINT_DERIVERS) {
            viewpoints.add(deriver.derive(evidence));
        }
        return ArchitectureIrJavaViewpointBridgeSupport.apply(viewpoints, dependencyViews);
    }
}
