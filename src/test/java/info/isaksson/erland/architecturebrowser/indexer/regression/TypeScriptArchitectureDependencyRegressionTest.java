package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasExternalDependencyTarget;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasTypeDependency;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptArchitectureDependencyRegressionTest extends AbstractTypeScriptArchitectureFixtureTestSupport {

    @Test
    void layeredReactFixturePreservesDependencyAndEvidenceSignals() {
        ArchitectureIndexDocument document = buildDocument(TypeScriptArchitectureFixtureFixtures.layeredReactFixture());
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> packageDependencies = dependencyViewList(document, "packageDependencies");
        List<Map<String, Object>> moduleDependencies = dependencyViewList(document, "moduleDependencies");
        assertTrue(!packageDependencies.isEmpty() || !moduleDependencies.isEmpty());

        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertHasTypeDependency(typeDependencies, "OrdersStore", "OrderService", "field");

        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertTrue(
            evidenceDependencies.stream().anyMatch(dep -> "src/app/pages/OrdersPage.tsx".equals(dep.get("sourceName")))
                && (evidenceDependencies.stream().anyMatch(dep -> "react".equals(dep.get("targetName")) && Boolean.TRUE.equals(dep.get("externalTarget")))
                    || document.entities().stream().anyMatch(entity -> "react".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED)),
            () -> "Expected React import evidence for OrdersPage. evidenceDependencies=" + evidenceDependencies + ", entities=" + document.entities()
        );
    }

    @Test
    void angularFixturePreservesBoundarySignals() {
        ArchitectureIndexDocument document = buildDocument(TypeScriptArchitectureFixtureFixtures.angularFixture());
        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertHasTypeDependency(typeDependencies, "OrderListComponent", "OrderService", "field");

        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertTrue(
            evidenceDependencies.stream().anyMatch(dep -> "@angular/core".equals(dep.get("targetName")) && Boolean.TRUE.equals(dep.get("externalTarget")))
                || document.entities().stream().anyMatch(entity -> "@angular/core".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED),
            () -> "Expected Angular core external dependency evidence. evidenceDependencies=" + evidenceDependencies + ", entities=" + document.entities()
        );
    }
}
