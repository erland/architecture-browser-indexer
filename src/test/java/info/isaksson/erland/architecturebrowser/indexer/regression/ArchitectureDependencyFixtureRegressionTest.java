package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureDependencyFixtureRegressionTest extends AbstractArchitectureDependencyFixtureTestSupport {

    @Test
    void layeredPackageFixtureProducesArchitectFriendlyPackageAndModuleViews() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            javaFile(
                "src/main/java/com/example/api/ApiController.java",
                "com.example.api",
                List.of(
                    "com.example.application.OrderApplicationService",
                    "org.springframework.web.context.request.RequestContext"
                ),
                List.of(
                    javaClass(
                        "ApiController",
                        List.of(),
                        List.of(),
                        List.of(
                            field("OrderApplicationService", "applicationService"),
                            field("RequestContext", "requestContext")
                        ),
                        List.of()
                    )
                )
            ),
            javaFile(
                "src/main/java/com/example/application/OrderApplicationService.java",
                "com.example.application",
                List.of(
                    "com.example.domain.Order",
                    "com.example.infrastructure.OrderRepository"
                ),
                List.of(
                    javaClass(
                        "OrderApplicationService",
                        List.of(),
                        List.of(),
                        List.of(field("OrderRepository", "orderRepository")),
                        List.of(method("Order", "loadOrder", List.of()))
                    )
                )
            ),
            javaFile(
                "src/main/java/com/example/domain/Order.java",
                "com.example.domain",
                List.of(),
                List.of(javaClass("Order", List.of(), List.of(), List.of(), List.of()))
            ),
            javaFile(
                "src/main/java/com/example/infrastructure/OrderRepository.java",
                "com.example.infrastructure",
                List.of(),
                List.of(javaInterface("OrderRepository", List.of(), List.of(), List.of()))
            )
        ));

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> packageDependencies = dependencyViewList(document, "packageDependencies");
        assertTrue(hasPackageDependency(packageDependencies, "com.example.api", "com.example.application", "field", true, false));
        assertTrue(hasPackageDependency(packageDependencies, "com.example.application", "com.example.domain", "returnType", true, false));
        assertTrue(hasPackageDependency(packageDependencies, "com.example.application", "com.example.infrastructure", "field", true, false));
        assertTrue(hasPackageDependency(packageDependencies, "com.example.api", "org.springframework.web.context.request", "field", false, true));
    }
}
