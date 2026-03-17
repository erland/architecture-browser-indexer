package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendArchitectureEndToEndFixtureRegressionTest extends AbstractFrontendArchitectureFixtureTestSupport {

    @Test
    void angularFixtureStillProducesBroadFrontendArchitecturePicture() {
        ArchitectureIndexDocument document = buildAngularDocument();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());
        assertHasDependencyViews(document,
            "compositionTypeDependencies",
            "routeTypeDependencies",
            "providerTypeDependencies",
            "frameworkModuleDependencies");
        assertFrontendBrowserViewIds(document,
            "angularModuleGraph",
            "angularProviderGraph",
            "routeGraph");
    }

    @Test
    void reactFixtureStillProducesBroadFrontendArchitecturePicture() {
        ArchitectureIndexDocument document = buildReactDocument();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());
        assertHasDependencyViews(document,
            "compositionTypeDependencies",
            "routeTypeDependencies",
            "providerTypeDependencies",
            "hookTypeDependencies",
            "frameworkTypeDependencies");
        assertFrontendBrowserViewIds(document,
            "routeGraph",
            "reactComponentCompositionGraph",
            "reactContextGraph",
            "reactHookGraph");
    }
}
