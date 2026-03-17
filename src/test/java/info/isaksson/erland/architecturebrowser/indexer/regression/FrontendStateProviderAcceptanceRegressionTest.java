package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendStateProviderAcceptanceRegressionTest extends AbstractFrontendArchitectureFixtureTestSupport {
    @Test
    void angularProviderAcceptanceStaysStable() {
        ArchitectureIndexDocument document = buildAngularDocument();
        List<Map<String, Object>> providerTypeDependencies = dependencyViewList(document, "providerTypeDependencies");
        assertTrue(providerTypeDependencies.stream().anyMatch(dep -> {
            List<?> relationships = (List<?>) dep.get("frameworkRelationships");
            return relationships.contains("injects") || relationships.contains("providedBy") || relationships.contains("resolvesTo");
        }));
        assertFrontendBrowserViewIds(document, "angularProviderGraph");
    }

    @Test
    void reactProviderAcceptanceStaysStable() {
        ArchitectureIndexDocument document = buildReactDocument();
        List<Map<String, Object>> providerTypeDependencies = dependencyViewList(document, "providerTypeDependencies");
        assertTrue(providerTypeDependencies.stream().anyMatch(dep -> {
            List<?> relationships = (List<?>) dep.get("frameworkRelationships");
            return relationships.contains("providesContext") || relationships.contains("consumesContext");
        }));
        assertFrontendBrowserViewIds(document, "reactContextGraph");
    }
}
