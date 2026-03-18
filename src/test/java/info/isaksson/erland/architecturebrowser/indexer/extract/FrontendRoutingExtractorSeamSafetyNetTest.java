package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.RelationshipKind;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRoutingExtractorSeamSafetyNetTest {

    @Test
    void preservesAngularRouteEntitiesAndGuardResolverRelationshipsThroughRoutingSeam() {
        String source = """
            import { Routes } from '@angular/router';
            export const routes: Routes = [
              {
                path: 'orders',
                component: OrdersPage,
                canActivate: [AuthGuard],
                resolve: { summary: OrdersResolver },
                children: [
                  { path: 'details', component: OrderDetailsPage }
                ]
              }
            ];
            """;

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        namedEntities.put("OrdersPage", entity("OrdersPage", EntityKind.UI_MODULE));
        namedEntities.put("OrderDetailsPage", entity("OrderDetailsPage", EntityKind.UI_MODULE));
        namedEntities.put("AuthGuard", entity("AuthGuard", EntityKind.CLASS));
        namedEntities.put("OrdersResolver", entity("OrdersResolver", EntityKind.CLASS));

        FrontendRoutingExtractor.extract(accumulator, "src/app/app.routes.ts", source, namedEntities);

        ExtractedEntityFact ordersRoute = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.UI_MODULE && "angular-route:/orders".equals(entity.name()))
            .findFirst().orElseThrow();
        ExtractedEntityFact detailsRoute = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.UI_MODULE && "angular-route:/orders/details".equals(entity.name()))
            .findFirst().orElseThrow();

        assertEquals("/orders", ordersRoute.metadata().get("routeFullPath"));
        assertEquals("/orders/details", detailsRoute.metadata().get("routeFullPath"));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && ordersRoute.id().equals(rel.fromEntityId())
            && entity("OrdersPage", EntityKind.UI_MODULE).id().equals(rel.toEntityId())
            && "targets".equals(rel.metadata().get("frameworkRelationship"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && ordersRoute.id().equals(rel.fromEntityId())
            && entity("AuthGuard", EntityKind.CLASS).id().equals(rel.toEntityId())
            && "guards".equals(rel.metadata().get("frameworkRelationship"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && ordersRoute.id().equals(rel.fromEntityId())
            && entity("OrdersResolver", EntityKind.CLASS).id().equals(rel.toEntityId())
            && "resolves".equals(rel.metadata().get("frameworkRelationship"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && detailsRoute.id().equals(rel.fromEntityId())
            && ordersRoute.id().equals(rel.toEntityId())
            && "childOf".equals(rel.metadata().get("frameworkRelationship"))));
    }


    @Test
    void preservesRedirectAndStaticNavigationEvidenceThroughRoutingSeam() {
        String source = """
            import { Link } from 'react-router-dom';
            export const routes = [
              { path: 'legacy', redirectTo: '/orders' }
            ];
            export function OrdersPage() {
              return <Link to="/reports">Reports</Link>;
            }
            export function LegacyEntry() {
              navigate('/orders');
              return <section />;
            }
            """;

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        namedEntities.put("OrdersPage", entityWithPath("OrdersPage", EntityKind.FUNCTION, "src/app/router.tsx", 5));
        namedEntities.put("LegacyEntry", entityWithPath("LegacyEntry", EntityKind.FUNCTION, "src/app/router.tsx", 8));

        FrontendRoutingExtractor.extract(accumulator, "src/app/router.tsx", source, namedEntities);

        ExtractedEntityFact legacyRoute = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.UI_MODULE && "react-route:/legacy".equals(entity.name()))
            .findFirst().orElseThrow();
        ExtractedEntityFact ordersRoute = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.UI_MODULE && "react-route:/orders".equals(entity.name()))
            .findFirst().orElseThrow();
        ExtractedEntityFact reportsRoute = accumulator.entities().stream()
            .filter(entity -> entity.kind() == EntityKind.UI_MODULE && "react-route:/reports".equals(entity.name()))
            .findFirst().orElseThrow();

        assertEquals("/orders", legacyRoute.metadata().get("redirectTargetLiteral"));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && legacyRoute.id().equals(rel.fromEntityId())
            && ordersRoute.id().equals(rel.toEntityId())
            && "redirects".equals(rel.metadata().get("frameworkRelationship"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && entityWithPath("OrdersPage", EntityKind.FUNCTION, "src/app/router.tsx", 5).id().equals(rel.fromEntityId())
            && reportsRoute.id().equals(rel.toEntityId())
            && "linksToRoute".equals(rel.metadata().get("frameworkRelationship"))
            && "link".equals(rel.metadata().get("routeSourceKind"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> rel.kind() == RelationshipKind.DEPENDS_ON
            && entityWithPath("LegacyEntry", EntityKind.FUNCTION, "src/app/router.tsx", 8).id().equals(rel.fromEntityId())
            && ordersRoute.id().equals(rel.toEntityId())
            && "navigatesToRoute".equals(rel.metadata().get("frameworkRelationship"))
            && "navigate-call".equals(rel.metadata().get("routeSourceKind"))));
    }

    private static ExtractedEntityFact entity(String name, EntityKind kind) {
        return entityWithPath(name, kind, null, null);
    }

    private static ExtractedEntityFact entityWithPath(String name, EntityKind kind, String path, Integer startLine) {
        return new ExtractedEntityFact(
            "entity:test:" + name,
            kind,
            EntityOrigin.OBSERVED,
            name,
            name,
            "scope:test",
            path == null ? List.of() : List.of(new info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference(path, startLine, startLine, name, Map.of())),
            Map.of("qualifiedName", name)
        );
    }
}
