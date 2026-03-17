package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityOrigin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureIrDependencyViewCatalogSupportTest {
    @Test
    void composesDependencyCatalogAndBrowserInputsFromViewKinds() {
        ArchitectureEntity sourceType = new ArchitectureEntity(
            "entity:type:source",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "com.example.api.OrderService",
            "com.example.api.OrderService",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.api.OrderService", "packageName", "com.example.api", "sourceRoot", "src/main/java")
        );
        ArchitectureEntity targetType = new ArchitectureEntity(
            "entity:type:target",
            EntityKind.CLASS,
            EntityOrigin.OBSERVED,
            "com.example.domain.Order",
            "com.example.domain.Order",
            "scope:repo",
            List.of(),
            Map.of("qualifiedName", "com.example.domain.Order", "packageName", "com.example.domain", "sourceRoot", "src/main/java")
        );

        ArchitectureIrDependencyViewCatalogSupport.DependencyViewCatalog catalog = ArchitectureIrDependencyViewCatalogSupport.compose(
            Map.of(sourceType.id(), sourceType, targetType.id(), targetType),
            List.of(
                Map.of("architectureViewKinds", List.of("composition"), "sourceTypeId", sourceType.id(), "targetTypeId", targetType.id()),
                Map.of("architectureViewKinds", List.of("endpoint"), "sourceTypeId", sourceType.id(), "targetTypeId", targetType.id())
            ),
            List.of(Map.of("sourcePackageName", "com.example.api", "targetPackageName", "com.example.domain")),
            List.of(
                Map.of("architectureViewKinds", List.of("composition"), "sourceModuleName", "src/main/java", "targetModuleName", "src/main/java"),
                Map.of("architectureViewKinds", List.of("endpoint"), "sourceModuleName", "src/main/java", "targetModuleName", "src/main/java")
            ),
            List.of()
        );

        Map<String, Object> dependencyViews = catalog.dependencyViews();
        List<String> recommended = (List<String>) dependencyViews.get("recommendedEntryPoints");
        List<String> primary = (List<String>) dependencyViews.get("primaryArchitectureViews");

        assertTrue(((List<?>) dependencyViews.get("compositionTypeDependencies")).size() == 1);
        assertTrue(((List<?>) dependencyViews.get("endpointTypeDependencies")).size() == 1);
        assertTrue(recommended.contains("endpointTypeDependencies"));
        assertTrue(primary.contains("endpointTypeDependencies"));
        assertEquals(1, catalog.browserViewInputs().compositionTypeDependencies().size());
        assertEquals(1, catalog.browserViewInputs().endpointModuleDependencies().size());
    }
}
