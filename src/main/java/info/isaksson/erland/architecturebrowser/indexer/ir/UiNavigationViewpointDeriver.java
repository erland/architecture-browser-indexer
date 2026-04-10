package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureViewpoint;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRelationshipSemantic;
import info.isaksson.erland.architecturebrowser.indexer.normalize.ArchitecturalRole;

import java.util.List;

final class UiNavigationViewpointDeriver implements ArchitectureViewpointDeriver {
    @Override
    public ArchitectureViewpoint derive(ViewpointEvidence evidence) {
        List<String> pages = evidence.entityIdsForRole(ArchitecturalRole.UI_PAGE.id());
        List<String> layouts = evidence.entityIdsForRole(ArchitecturalRole.UI_LAYOUT.id());
        List<String> navigationNodes = evidence.entityIdsForRole(ArchitecturalRole.UI_NAVIGATION_NODE.id());
        boolean hasUiRoles = !pages.isEmpty() || !layouts.isEmpty() || !navigationNodes.isEmpty();
        boolean hasCoreNavigationSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.NAVIGATES_TO.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.CONTAINS_ROUTE.id());
        boolean hasSecondaryNavigationSemantics = evidence.hasSemantic(ArchitecturalRelationshipSemantic.REDIRECTS_TO.id())
            || evidence.hasSemantic(ArchitecturalRelationshipSemantic.GUARDS_ROUTE.id());
        String availability;
        double confidence;
        if (hasUiRoles && hasCoreNavigationSemantics) {
            availability = "available";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.76
                + (!pages.isEmpty() ? 0.08 : 0.0)
                + (!layouts.isEmpty() ? 0.06 : 0.0)
                + (!navigationNodes.isEmpty() ? 0.04 : 0.0)
                + (hasSecondaryNavigationSemantics ? 0.04 : 0.0));
        } else if (hasUiRoles || hasCoreNavigationSemantics || hasSecondaryNavigationSemantics) {
            availability = "partial";
            confidence = ArchitectureViewpointDerivationSupport.clamp(0.44
                + (hasUiRoles ? 0.16 : 0.0)
                + (hasCoreNavigationSemantics ? 0.16 : 0.0)
                + (hasSecondaryNavigationSemantics ? 0.08 : 0.0));
        } else {
            availability = "unavailable";
            confidence = 0.0;
        }
        List<String> seedEntityIds = ArchitectureViewpointDerivationSupport.mergeIds(pages, layouts, navigationNodes);
        return new ArchitectureViewpoint(
            "ui-navigation",
            "UI navigation",
            "Highlights user-facing pages, layouts, and navigation structures together with canonical navigation relationships.",
            availability,
            confidence,
            seedEntityIds.isEmpty() ? null : seedEntityIds,
            ArchitectureViewpointDerivationSupport.roleIdsPresent(evidence,
                ArchitecturalRole.UI_LAYOUT.id(),
                ArchitecturalRole.UI_NAVIGATION_NODE.id(),
                ArchitecturalRole.UI_PAGE.id()),
            ArchitectureViewpointDerivationSupport.presentSemantics(evidence,
                ArchitecturalRelationshipSemantic.CONTAINS_ROUTE.id(),
                ArchitecturalRelationshipSemantic.GUARDS_ROUTE.id(),
                ArchitecturalRelationshipSemantic.NAVIGATES_TO.id(),
                ArchitecturalRelationshipSemantic.REDIRECTS_TO.id()),
            null,
            ArchitectureViewpointDerivationSupport.uiNavigationEvidenceSources(
                evidence,
                hasUiRoles,
                hasCoreNavigationSemantics || hasSecondaryNavigationSemantics)
        );
    }
}
