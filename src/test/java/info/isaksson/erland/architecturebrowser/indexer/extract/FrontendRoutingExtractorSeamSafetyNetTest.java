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

    private static ExtractedEntityFact entity(String name, EntityKind kind) {
        return new ExtractedEntityFact(
            "entity:test:" + name,
            kind,
            EntityOrigin.OBSERVED,
            name,
            name,
            "scope:test",
            List.of(),
            Map.of("qualifiedName", name)
        );
    }
}
