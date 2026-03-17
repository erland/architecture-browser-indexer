package info.isaksson.erland.architecturebrowser.indexer.regression;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureIndexDocument;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.EntityKind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasBrowserViewDescriptor;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasBrowserViewIds;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasFrameworkDependencyView;
import static info.isaksson.erland.architecturebrowser.indexer.testing.ArchitectureContractAssertions.assertHasUiModuleProfile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypeScriptArchitectureFrameworkRegressionTest extends AbstractTypeScriptArchitectureFixtureTestSupport {

    @Test
    void layeredReactAndAngularFixturesPreserveRolesAndDeclarationKinds() {
        ArchitectureIndexDocument reactDocument = buildDocument(TypeScriptArchitectureFixtureFixtures.layeredReactFixture());
        ArchitectureEntity orderDto = entity(reactDocument, EntityKind.INTERFACE, "OrderDto");
        ArchitectureEntity orderService = entity(reactDocument, EntityKind.CLASS, "OrderService");
        assertEquals("interface", orderDto.metadata().get("declarationKind"));
        assertEquals("class", orderService.metadata().get("declarationKind"));
        assertTrue(reactDocument.entities().stream().anyMatch(entity ->
            entity.kind() == EntityKind.UI_MODULE
                && "OrdersPage".equals(entity.name())
                && ("page-or-router".equals(entity.metadata().get("uiProfile"))
                    || "react-function-component".equals(entity.metadata().get("uiProfile"))
                    || entity.metadata().get("uiProfile") == null)
        ));

        ArchitectureIndexDocument angularDocument = buildDocument(TypeScriptArchitectureFixtureFixtures.angularFixture());
        assertHasUiModuleProfile(angularDocument.entities(), "OrderListComponent", "angular-component");
        assertHasUiModuleProfile(angularDocument.entities(), "OrdersModule", "angular-module");
        assertTrue(angularDocument.entities().stream().anyMatch(entity ->
            entity.kind() == EntityKind.SERVICE && "OrderService".equals(entity.name())));
    }

    @Test
    void frameworkRelationshipsFlowIntoTypeAndModuleRollups() {
        ArchitectureIndexDocument document = buildDocument(TypeScriptArchitectureFixtureFixtures.frameworkRelationshipsFixture());

        Map<String, Object> dependencyViews = dependencyViews(document);
        List<Map<String, Object>> frameworkTypeDependencies = dependencyViewList(document, "frameworkTypeDependencies");
        List<Map<String, Object>> frameworkModuleDependencies = dependencyViewList(document, "frameworkModuleDependencies");
        List<Map<String, Object>> compositionTypeDependencies = dependencyViewList(document, "compositionTypeDependencies");
        List<Map<String, Object>> routeTypeDependencies = dependencyViewList(document, "routeTypeDependencies");
        List<Map<String, Object>> providerTypeDependencies = dependencyViewList(document, "providerTypeDependencies");
        List<Map<String, Object>> hookTypeDependencies = dependencyViewList(document, "hookTypeDependencies");

        assertTrue(!frameworkTypeDependencies.isEmpty());
        assertTrue(!frameworkModuleDependencies.isEmpty());
        assertHasFrameworkDependencyView(compositionTypeDependencies, "renders");
        assertHasFrameworkDependencyView(routeTypeDependencies, "targets");
        assertTrue(providerTypeDependencies.stream().anyMatch(dep -> {
            List<?> relationships = (List<?>) dep.get("frameworkRelationships");
            return relationships.contains("providesContext") || relationships.contains("consumesContext") || relationships.contains("injects") || relationships.contains("providedBy");
        }));
        assertHasFrameworkDependencyView(hookTypeDependencies, "usesHook");
        assertTrue(frameworkModuleDependencies.stream().anyMatch(dep -> {
            List<?> viewKinds = (List<?>) dep.get("architectureViewKinds");
            List<?> frameworks = (List<?>) dep.get("frameworks");
            return viewKinds.contains("framework") && (frameworks.contains("react") || frameworks.contains("angular"));
        }));

        @SuppressWarnings("unchecked") Map<String, Object> frontendArchitectureViews = (Map<String, Object>) dependencyViews.get("frontendArchitectureViews");
        assertEquals(List.of("frameworkTypeDependencies", "frameworkModuleDependencies"), frontendArchitectureViews.get("frameworkAware"));
        @SuppressWarnings("unchecked") Map<String, Object> frontendBrowserViews = (Map<String, Object>) dependencyViews.get("frontendBrowserViews");
        assertEquals("angularModuleGraph", frontendBrowserViews.get("defaultViewId"));
        @SuppressWarnings("unchecked") List<Map<String, Object>> browserViewDescriptors = (List<Map<String, Object>>) frontendBrowserViews.get("views");
        assertHasBrowserViewIds(browserViewDescriptors, "angularModuleGraph", "angularProviderGraph", "routeGraph", "reactComponentCompositionGraph", "reactContextGraph", "reactHookGraph");
        assertHasBrowserViewDescriptor(browserViewDescriptors, "angularModuleGraph", "angular", "compositionTypeDependencies", "compositionModuleDependencies", "declares");
        assertHasBrowserViewDescriptor(browserViewDescriptors, "angularProviderGraph", "angular", "providerTypeDependencies", "providerModuleDependencies", "injects");
        assertHasBrowserViewDescriptor(browserViewDescriptors, "routeGraph", "frontend", "routeTypeDependencies", "routeModuleDependencies", "targets");
        assertHasBrowserViewDescriptor(browserViewDescriptors, "reactComponentCompositionGraph", "react", "compositionTypeDependencies", "compositionModuleDependencies", "renders");
        assertHasBrowserViewDescriptor(browserViewDescriptors, "reactContextGraph", "react", "providerTypeDependencies", "providerModuleDependencies", "providesContext");
        assertHasBrowserViewDescriptor(browserViewDescriptors, "reactHookGraph", "react", "hookTypeDependencies", "hookModuleDependencies", "usesHook");
    }
}
