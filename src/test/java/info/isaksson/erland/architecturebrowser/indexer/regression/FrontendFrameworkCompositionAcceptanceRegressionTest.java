package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

class FrontendFrameworkCompositionAcceptanceRegressionTest extends AbstractFrontendArchitectureFixtureTestSupport {
    @Test
    void angularFrameworkCompositionAcceptanceStaysStable() {
        ArchitectureIndexDocument document = buildAngularDocument();
        var deps = dependencyViewList(document, "compositionTypeDependencies");
        assertFrameworkRelationshipPresent(deps, "declares");
        assertFrameworkRelationshipPresent(deps, "templateRenders");
        assertFrameworkRelationshipPresent(deps, "usesDirective");
        assertFrameworkRelationshipPresent(deps, "usesPipe");
        assertFrontendBrowserViewIds(document, "angularModuleGraph");
    }

    @Test
    void reactFrameworkCompositionAcceptanceStaysStable() {
        ArchitectureIndexDocument document = buildReactDocument();
        assertFrameworkRelationshipPresent(dependencyViewList(document, "compositionTypeDependencies"), "renders");
        assertFrameworkRelationshipPresent(dependencyViewList(document, "hookTypeDependencies"), "usesHook");
        assertFrontendBrowserViewIds(document, "reactComponentCompositionGraph", "reactHookGraph");
    }
}
