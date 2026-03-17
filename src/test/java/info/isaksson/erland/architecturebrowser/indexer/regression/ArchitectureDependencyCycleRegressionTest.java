package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureDependencyCycleRegressionTest extends AbstractArchitectureDependencyFixtureTestSupport {

    @Test
    void bidirectionalPackageFixturePreservesCycleSignalsForArchitecturalSmells() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            javaFile(
                "src/main/java/com/example/domain/OrderService.java",
                "com.example.domain",
                List.of("com.example.infrastructure.OrderRepository"),
                List.of(javaClass(
                    "OrderService",
                    List.of(),
                    List.of(),
                    List.of(field("OrderRepository", "orderRepository")),
                    List.of()
                ))
            ),
            javaFile(
                "src/main/java/com/example/infrastructure/OrderRepository.java",
                "com.example.infrastructure",
                List.of("com.example.domain.OrderService"),
                List.of(javaClass(
                    "OrderRepository",
                    List.of(),
                    List.of(),
                    List.of(field("OrderService", "orderService")),
                    List.of()
                ))
            )
        ));

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> packageDependencies = dependencyViewList(document, "packageDependencies");
        assertTrue(hasPackageDependency(packageDependencies, "com.example.domain", "com.example.infrastructure", "field", true, false));
        assertTrue(hasPackageDependency(packageDependencies, "com.example.infrastructure", "com.example.domain", "field", true, false));

        List<Map<String, Object>> packageMetrics = dependencyViewList(document, "packageMetrics");
        assertTrue(packageMetrics.stream().anyMatch(metric ->
            "com.example.domain".equals(metric.get("packageName"))
                && Integer.valueOf(1).equals(metric.get("incomingDependencyCount"))
                && Integer.valueOf(1).equals(metric.get("outgoingDependencyCount"))
        ));
        assertTrue(packageMetrics.stream().anyMatch(metric ->
            "com.example.infrastructure".equals(metric.get("packageName"))
                && Integer.valueOf(1).equals(metric.get("incomingDependencyCount"))
                && Integer.valueOf(1).equals(metric.get("outgoingDependencyCount"))
        ));
    }
}
