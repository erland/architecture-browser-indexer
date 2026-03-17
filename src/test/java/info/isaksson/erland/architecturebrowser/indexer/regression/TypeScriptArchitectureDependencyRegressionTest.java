package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        assertTrue(typeDependencies.stream().anyMatch(dep ->
            "OrdersStore".equals(dep.get("sourceTypeName"))
                && "OrderService".equals(dep.get("targetTypeName"))
                && ((List<?>) dep.get("dependencySources")).contains("field")
                && "internal".equals(dep.get("targetBoundary"))
        ));

        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertTrue(
            evidenceDependencies.stream().anyMatch(dep ->
                "src/app/pages/OrdersPage.tsx".equals(dep.get("sourceName"))
                    && "react".equals(dep.get("targetName"))
                    && Boolean.TRUE.equals(dep.get("externalTarget"))
                    && ((List<?>) dep.get("dependencySources")).contains("import")
            ) || document.entities().stream().anyMatch(entity ->
                "react".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED
            )
        );
    }

    @Test
    void angularFixturePreservesBoundarySignals() {
        ArchitectureIndexDocument document = buildDocument(TypeScriptArchitectureFixtureFixtures.angularFixture());
        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertTrue(typeDependencies.stream().anyMatch(dep ->
            "OrderListComponent".equals(dep.get("sourceTypeName"))
                && "OrderService".equals(dep.get("targetTypeName"))
                && ((List<?>) dep.get("dependencySources")).contains("field")
                && "internal".equals(dep.get("targetBoundary"))
        ));

        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertTrue(
            evidenceDependencies.stream().anyMatch(dep -> "@angular/core".equals(dep.get("targetName")))
                || document.entities().stream().anyMatch(entity -> "@angular/core".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED)
        );
    }
}
