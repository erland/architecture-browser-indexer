package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRoutingPhasesTest {

    @Test
    void discoversNormalizesAndEmitsRoutesThroughExplicitPhases() {
        String source = """
            import { Routes } from '@angular/router';
            export const routes: Routes = [
              {
                path: 'orders',
                component: OrdersPage,
                children: [
                  { path: 'details', component: OrderDetailsPage }
                ]
              }
            ];
            """;

        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        namedEntities.put("OrdersPage", entity("OrdersPage", EntityKind.UI_MODULE));
        namedEntities.put("OrderDetailsPage", entity("OrderDetailsPage", EntityKind.UI_MODULE));

        FrontendRouteDiscoverySupport discovery = new FrontendRouteDiscoverySupport();
        List<FrontendRouteCandidate> discovered = discovery.discover("src/app/app.routes.ts", source, namedEntities);
        assertEquals(2, discovered.size());
        assertEquals(List.of("orders", "details"), discovered.stream().map(FrontendRouteCandidate::path).toList());

        FrontendRoutePathNormalizationSupport normalization = new FrontendRoutePathNormalizationSupport();
        List<FrontendRouteCandidate> normalized = normalization.normalize(discovered);
        assertEquals(List.of("/orders", "/orders/details"), normalized.stream().map(FrontendRouteCandidate::fullPath).toList());

        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        new FrontendRouteEmissionSupport().emit(accumulator, "src/app/app.routes.ts", normalized, namedEntities, normalization);
        assertTrue(accumulator.entities().stream().anyMatch(entity -> "/orders/details".equals(entity.metadata().get("routeFullPath"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> "childOf".equals(rel.metadata().get("frameworkRelationship"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> "targets".equals(rel.metadata().get("frameworkRelationship"))));
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
