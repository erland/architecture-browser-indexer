package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertContainsViews;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasTypeDependency;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendDependencyViewsRegressionTest extends AbstractFrontendFrameworkBaselineTestSupport {
    @Test
    void currentFrontendDependencyViewsRemainTypeAndPathDriven() {
        ArchitectureIndexDocument document = buildDependencyViewsDocument();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());
        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertHasTypeDependency(typeDependencies, "OrdersStore", "OrderService", "field");
        assertHasTypeDependency(typeDependencies, "OrderListComponent", "OrderService", "field");
        List<Map<String, Object>> packageDependencies = dependencyViewList(document, "packageDependencies");
        List<Map<String, Object>> moduleDependencies = dependencyViewList(document, "moduleDependencies");
        assertNotNull(packageDependencies);
        assertFalse(moduleDependencies.isEmpty());
        assertTrue(moduleDependencies.stream().anyMatch(dep -> "src".equals(dep.get("sourceModuleName")) && "src".equals(dep.get("targetModuleName")) && Boolean.TRUE.equals(dep.get("internalTarget"))));
        List<Map<String, Object>> evidenceDependencies = dependencyViewList(document, "evidenceDependencies");
        assertTrue(evidenceDependencies.stream().anyMatch(dep -> "src/pages/OrdersPage.tsx".equals(dep.get("sourceName")) && "react".equals(dep.get("targetName")) && Boolean.TRUE.equals(dep.get("externalTarget"))) || document.entities().stream().anyMatch(entity -> "react".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED));
        assertTrue(evidenceDependencies.stream().anyMatch(dep -> "src/app/orders/order-list.component.ts".equals(dep.get("sourceName")) && "@angular/core".equals(dep.get("targetName")) && Boolean.TRUE.equals(dep.get("externalTarget"))) || document.entities().stream().anyMatch(entity -> "@angular/core".equals(entity.name()) && entity.origin() == EntityOrigin.INFERRED));
        @SuppressWarnings("unchecked") Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        assertContainsViews(dependencyViews.get("recommendedEntryPoints"), "typeDependencies", "moduleDependencies", "evidenceDependencies");
    }
}
