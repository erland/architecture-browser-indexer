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

class FrontendRouteDiscoveryAndEmissionModularizationTest {

    @Test
    void composes_object_and_jsx_discovery_through_dedicated_helpers() {
        String angularSource = """
            import { Routes } from '@angular/router';
            export const routes: Routes = [
              { path: 'orders', component: OrdersPage }
            ];
            """;
        String reactSource = """
            import { Route } from 'react-router-dom';
            export function Router() {
              return <Route path="/reports" element={<ReportsPage />} />;
            }
            """;

        Map<String, ExtractedEntityFact> angularEntities = new LinkedHashMap<>();
        angularEntities.put("OrdersPage", entity("OrdersPage", EntityKind.UI_MODULE, "angular"));
        Map<String, ExtractedEntityFact> reactEntities = new LinkedHashMap<>();
        reactEntities.put("ReportsPage", entity("ReportsPage", EntityKind.UI_MODULE, "react"));

        FrontendRouteDiscoverySupport discovery = new FrontendRouteDiscoverySupport();
        List<FrontendRouteCandidate> angularRoutes = discovery.discover("src/app/app.routes.ts", angularSource, angularEntities);
        List<FrontendRouteCandidate> reactRoutes = discovery.discover("src/app/router.tsx", reactSource, reactEntities);

        assertEquals(List.of("orders"), angularRoutes.stream().map(FrontendRouteCandidate::path).toList());
        assertEquals(List.of("/reports"), reactRoutes.stream().map(FrontendRouteCandidate::path).toList());
    }

    @Test
    void emits_navigation_and_route_entities_through_dedicated_factory_and_source_resolver() {
        ExtractionAccumulator accumulator = new ExtractionAccumulator();
        FrontendRoutePathNormalizationSupport normalization = new FrontendRoutePathNormalizationSupport();
        Map<String, ExtractedEntityFact> namedEntities = new LinkedHashMap<>();
        namedEntities.put("OrdersPage", entityWithPath("OrdersPage", EntityKind.FUNCTION, "react", "src/app/router.tsx", 3));

        List<FrontendRouteCandidate> routes = normalization.normalize(List.of(
            new FrontendRouteCandidate("react", "orders", null, 0, 10, 2, "{ path: 'orders' }", List.of(), List.of(), List.of(), List.of(), "route-object", "")
        ));
        List<FrontendNavigationCandidate> navigation = List.of(
            new FrontendNavigationCandidate("react", "link", "/orders", 3, "<Link to=\"/orders\" />")
        );

        new FrontendRouteEmissionSupport().emit(accumulator, "src/app/router.tsx", routes, navigation, namedEntities, normalization);

        assertTrue(accumulator.entities().stream().anyMatch(entity -> "/orders".equals(entity.metadata().get("routeFullPath"))));
        assertTrue(accumulator.relationships().stream().anyMatch(rel -> "linksToRoute".equals(rel.metadata().get("frameworkRelationship"))));
    }

    private static ExtractedEntityFact entity(String name, EntityKind kind, String framework) {
        return new ExtractedEntityFact(
            "entity:test:" + name,
            kind,
            EntityOrigin.OBSERVED,
            name,
            name,
            "scope:test",
            List.of(),
            Map.of("qualifiedName", name, "framework", framework)
        );
    }

    private static ExtractedEntityFact entityWithPath(String name, EntityKind kind, String framework, String path, int line) {
        return new ExtractedEntityFact(
            "entity:test:" + name,
            kind,
            EntityOrigin.OBSERVED,
            name,
            name,
            "scope:test",
            List.of(new info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference(path, line, line, name, Map.of())),
            Map.of("qualifiedName", name, "framework", framework)
        );
    }
}
