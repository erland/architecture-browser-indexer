package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertContainsViews;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasBrowserViewIds;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasPackageDependency;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrCompositionEndToEndFixtureRegressionTest {

    @Test
    @SuppressWarnings("unchecked")
    void realisticJavaBackendFixturePreservesIrCompositionOutputsEndToEnd() {
        ArchitectureIndexDocument document = JavaBackendArchitectureFixtureTestData.buildDocumentFromFixture();
        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        List<Map<String, Object>> packageDependencies = (List<Map<String, Object>>) dependencyViews.get("packageDependencies");
        List<Map<String, Object>> packageMetrics = (List<Map<String, Object>>) dependencyViews.get("packageMetrics");
        Map<String, Object> javaBrowserViews = (Map<String, Object>) dependencyViews.get("javaBrowserViews");
        Map<String, Object> browserViewCatalog = (Map<String, Object>) dependencyViews.get("browserViewCatalog");
        Map<String, Object> evidenceStatus = (Map<String, Object>) dependencyViews.get("evidenceStatus");

                assertContainsViews(dependencyViews.get("primaryArchitectureViews"), "packageDependencies", "typeDependencies", "moduleDependencies");
        assertContainsViews(dependencyViews.get("recommendedEntryPoints"), "packageDependencies", "typeDependencies", "moduleDependencies", "evidenceDependencies");

        assertHasPackageDependency(packageDependencies, "com.example.orders.api", "com.example.orders.service");
        assertHasPackageDependency(packageDependencies, "com.example.orders.service", "com.example.orders.domain");
        assertTrue(packageMetrics.stream().anyMatch(metric ->
                "com.example.orders.service".equals(metric.get("packageName"))
                    && ((Number) metric.get("outgoingDependencyCount")).intValue() >= 1),
            () -> "Expected package metrics for service package. packageMetrics=" + packageMetrics);

        assertEquals("javaEndpointGraph", javaBrowserViews.get("defaultViewId"));
        assertHasBrowserViewIds(browserViewDescriptors(javaBrowserViews),
            "javaEndpointGraph",
            "javaEntityModelGraph",
            "javaEventFlowGraph",
            "javaWritePathGraph");
        assertEquals("java", browserViewCatalog.get("defaultFamily"));
        assertTrue(((List<?>) browserViewCatalog.get("availableFamilies")).contains("java"));
        assertEquals(Boolean.FALSE, evidenceStatus.get("recommendedForArchitectureViews"));

        ArchitectureEntity servicePackage = document.entities().stream()
            .filter(entity -> "MODULE".equals(entity.kind().name()))
            .filter(entity -> "com.example.orders.service".equals(entity.name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing service package entity. Entities=" + document.entities()));
        assertTrue(((Number) servicePackage.metadata().get("outgoingDependencyCount")).intValue() >= 1,
            () -> "Expected enriched package dependency count on service package. Entity=" + servicePackage);

        assertTrue(document.relationships().stream().anyMatch(rel ->
                "package".equals(rel.metadata().get("dependencyView"))
                    && "com.example.orders.service".equals(rel.metadata().get("dependencySourcePackageName"))
                    && "com.example.orders.domain".equals(rel.metadata().get("dependencyTargetPackageName"))),
            () -> "Expected synthesized package dependency relationship. Relationships=" + document.relationships());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> browserViewDescriptors(Map<String, Object> browserViews) {
        Object views = browserViews.get("views");
        if (views instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        Object ids = browserViews.get("availableViews");
        if (ids instanceof List<?> list) {
            return ((List<?>) ids).stream().map(id -> Map.<String, Object>of("id", String.valueOf(id))).toList();
        }
        return List.of();
    }
}
