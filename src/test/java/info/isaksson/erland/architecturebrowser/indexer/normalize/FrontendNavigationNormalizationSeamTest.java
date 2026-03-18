package info.isaksson.erland.architecturebrowser.indexer.normalize;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureRelationship;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendNavigationNormalizationSeamTest {

    @Test
    void entityNormalizationContextExposesStructuredFrontendRouteEvidence() {
        ArchitectureEntity entity = new ArchitectureEntity(
            "entity:react-route:/orders",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "OrdersPage",
            "OrdersPage",
            "scope:repo",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "declared-route",
                "routeDeclarationKind", "route-object",
                "routeFullPath", "/orders",
                "guardReference", "authGuard"
            )
        );

        ArchitectureEntityNormalizationContext context = new ArchitectureEntityNormalizationContext(entity, Map.of(entity.id(), entity));

        FrontendRouteEvidence evidence = context.frontendRouteEvidenceOptional().orElseThrow();
        assertEquals("react", evidence.framework());
        assertEquals("declared-route", evidence.routeSourceKind());
        assertEquals("route-object", evidence.routeDeclarationKind());
        assertEquals("/orders", evidence.routeFullPath());
        assertTrue(evidence.guarded());
    }

    @Test
    void relationshipNormalizationContextExposesStructuredNavigationAndRouteEvidence() {
        ArchitectureEntity source = new ArchitectureEntity(
            "entity:component:orders-menu",
            EntityKind.UI_MODULE,
            EntityOrigin.OBSERVED,
            "OrdersMenu",
            "OrdersMenu",
            "scope:repo",
            List.of(),
            Map.of("framework", "react")
        );
        ArchitectureEntity target = new ArchitectureEntity(
            "entity:react-route:/reports",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "ReportsPage",
            "ReportsPage",
            "scope:repo",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "declared-route",
                "routeDeclarationKind", "route-object",
                "routeFullPath", "/reports"
            )
        );
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:orders-menu:reports-route",
            RelationshipKind.USES,
            source.id(),
            target.id(),
            "links to reports",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "link",
                "navigationTargetLiteral", "/reports",
                "emittedRelationshipKind", "linksToRoute"
            )
        );

        ArchitectureRelationshipNormalizationContext context = new ArchitectureRelationshipNormalizationContext(
            relationship,
            Map.of(source.id(), source, target.id(), target),
            Map.of(relationship.id(), relationship)
        );

        FrontendNavigationEvidence navigationEvidence = context.frontendNavigationEvidenceOptional().orElseThrow();
        assertTrue(navigationEvidence.staticLink());
        assertEquals("/reports", navigationEvidence.navigationTargetLiteral());
        assertEquals("linksToRoute", navigationEvidence.emittedRelationshipKind());

        FrontendRouteEvidence targetRouteEvidence = context.targetRouteEvidenceOptional().orElseThrow();
        assertEquals("/reports", targetRouteEvidence.routeFullPath());
        assertTrue(targetRouteEvidence.declaredRoute());
    }

    @Test
    void normalizationRulesCanConsumeFrontendNavigationEvidenceThroughSingleSeam() {
        ArchitectureEntity target = new ArchitectureEntity(
            "entity:react-route:/reports",
            EntityKind.UI_MODULE,
            EntityOrigin.INFERRED,
            "ReportsPage",
            "ReportsPage",
            "scope:repo",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "declared-route",
                "routeDeclarationKind", "route-object",
                "routeFullPath", "/reports"
            )
        );
        ArchitectureRelationship relationship = new ArchitectureRelationship(
            "rel:navigate:reports",
            RelationshipKind.USES,
            "entity:component:dashboard",
            target.id(),
            "navigate to reports",
            List.of(),
            Map.of(
                "framework", "react",
                "routeSourceKind", "navigate",
                "navigationTargetLiteral", "/reports",
                "emittedRelationshipKind", "navigatesToRoute"
            )
        );

        ArchitectureRelationshipNormalizationService service = ArchitectureRelationshipNormalizationService.of(List.of(context -> {
            if (context.frontendNavigationEvidenceOptional().filter(FrontendNavigationEvidence::imperativeNavigation).isPresent()
                && context.targetRouteEvidenceOptional().filter(FrontendRouteEvidence::declaredRoute).isPresent()) {
                return new NormalizedArchitectureRelationship(List.of(ArchitecturalRelationshipSemantic.NAVIGATES_TO.id()));
            }
            return null;
        }));

        ArchitectureRelationship normalized = service.normalizeRelationship(
            relationship,
            Map.of(target.id(), target),
            Map.of(relationship.id(), relationship)
        );

        assertEquals(List.of("navigates-to"), normalized.architecturalSemantics());
    }
}
