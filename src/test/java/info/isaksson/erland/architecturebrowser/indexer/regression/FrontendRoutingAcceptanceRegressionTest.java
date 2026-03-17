package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

class FrontendRoutingAcceptanceRegressionTest extends AbstractFrontendArchitectureFixtureTestSupport {
    @Test
    void angularRoutingAcceptanceStaysStable() {
        ArchitectureIndexDocument document = buildAngularDocument();
        assertFrameworkRelationshipPresent(dependencyViewList(document, "routeTypeDependencies"), "targets");
        assertFrontendBrowserViewIds(document, "routeGraph");
    }

    @Test
    void reactRoutingAcceptanceStaysStable() {
        ArchitectureIndexDocument document = buildReactDocument();
        assertFrameworkRelationshipPresent(dependencyViewList(document, "routeTypeDependencies"), "targets");
        assertFrontendBrowserViewIds(document, "routeGraph");
    }
}
