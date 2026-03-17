package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.ArchitectureIrValidator;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureDependencyReasonRegressionTest extends AbstractArchitectureDependencyFixtureTestSupport {

    @Test
    void hierarchyAndApiCouplingFixtureKeepsDifferentDependencyReasonsVisible() {
        ArchitectureIndexDocument document = buildDocument(List.of(
            javaFile(
                "src/main/java/com/example/api/OrderQuery.java",
                "com.example.api",
                List.of(),
                List.of(javaInterface("OrderQuery", List.of(), List.of(), List.of()))
            ),
            javaFile(
                "src/main/java/com/example/dto/OrderDto.java",
                "com.example.dto",
                List.of(),
                List.of(javaClass("OrderDto", List.of(), List.of(), List.of(), List.of()))
            ),
            javaFile(
                "src/main/java/com/example/application/DefaultOrderQuery.java",
                "com.example.application",
                List.of("com.example.api.OrderQuery", "com.example.dto.OrderDto"),
                List.of(javaClass(
                    "DefaultOrderQuery",
                    List.of(),
                    List.of("OrderQuery"),
                    List.of(),
                    List.of(method("OrderDto", "fetch", List.of()))
                ))
            )
        ));

        assertTrue(ArchitectureIrValidator.validate(document).isValid());

        List<Map<String, Object>> packageDependencies = dependencyViewList(document, "packageDependencies");
        assertTrue(packageDependencies.stream().anyMatch(dep ->
            "com.example.application".equals(dep.get("sourcePackageName"))
                && "com.example.api".equals(dep.get("targetPackageName"))
                && ((List<?>) dep.get("dependencySources")).contains("implements")
                && ((List<?>) dep.get("dependencyCategories")).contains("hierarchy")
                && Boolean.TRUE.equals(dep.get("internalTarget"))
        ));
        assertTrue(packageDependencies.stream().anyMatch(dep ->
            "com.example.application".equals(dep.get("sourcePackageName"))
                && "com.example.dto".equals(dep.get("targetPackageName"))
                && ((List<?>) dep.get("dependencySources")).contains("returnType")
                && ((List<?>) dep.get("dependencyCategories")).contains("api")
                && Boolean.TRUE.equals(dep.get("internalTarget"))
        ));

        List<Map<String, Object>> typeDependencies = dependencyViewList(document, "typeDependencies");
        assertTrue(typeDependencies.stream().anyMatch(dep ->
            "com.example.application.DefaultOrderQuery".equals(dep.get("sourceTypeName"))
                && "com.example.api.OrderQuery".equals(dep.get("targetTypeName"))
                && ((List<?>) dep.get("dependencySources")).contains("implements")
                && "internal".equals(dep.get("targetBoundary"))
                && "observed-source-type".equals(dep.get("targetClassification"))
        ));
    }
}
