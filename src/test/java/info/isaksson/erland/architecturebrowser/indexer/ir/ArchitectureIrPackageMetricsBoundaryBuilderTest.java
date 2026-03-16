package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrPackageMetricsBoundaryBuilderTest {

    @Test
    @SuppressWarnings("unchecked")
    void packageMetricsAndBoundarySummaryStayAvailableForJavaBackendFixture() {
        ArchitectureIndexDocument document = ArchitectureIrFactoryJavaBackendSafetyNetTestData.buildDocumentFromFixture();

        Map<String, Object> dependencyViews = (Map<String, Object>) document.metadata().get("dependencyViews");
        assertNotNull(dependencyViews);

        List<Map<String, Object>> packageMetrics = (List<Map<String, Object>>) dependencyViews.get("packageMetrics");
        assertNotNull(packageMetrics);
        assertFalse(packageMetrics.isEmpty());
        assertTrue(packageMetrics.stream().allMatch(metric -> metric.containsKey("packageName")));
        assertTrue(packageMetrics.stream().allMatch(metric -> metric.containsKey("declaredTypeCount")));
        assertTrue(packageMetrics.stream().allMatch(metric -> metric.containsKey("incomingDependencyCount")));
        assertTrue(packageMetrics.stream().allMatch(metric -> metric.containsKey("outgoingDependencyCount")));

        Map<String, Object> boundarySummary = (Map<String, Object>) dependencyViews.get("boundarySummary");
        assertNotNull(boundarySummary);
        assertTrue(boundarySummary.containsKey("typeInternalCount"));
        assertTrue(boundarySummary.containsKey("typeExternalCount"));
        assertTrue(boundarySummary.containsKey("packageInternalCount"));
        assertTrue(boundarySummary.containsKey("packageExternalCount"));
        assertTrue(boundarySummary.containsKey("moduleInternalCount"));
        assertTrue(boundarySummary.containsKey("moduleExternalCount"));
    }
}
