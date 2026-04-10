package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.Map;
import java.util.List;

final class ArchitectureIrBrowserViewMetadataBuilder {
    private ArchitectureIrBrowserViewMetadataBuilder() {
    }

    static ArchitectureIrBrowserViewComposition compose(ArchitectureIrBrowserViewCompositionInputs inputs) {
        if (inputs == null) {
            return ArchitectureIrBrowserViewComposition.empty();
        }
        Map<String, Object> frontendBrowserViews = ArchitectureIrFrontendBrowserViewSupport.buildFrontendBrowserViews(
            inputs.compositionTypeDependencies(),
            inputs.compositionModuleDependencies(),
            inputs.routeTypeDependencies(),
            inputs.routeModuleDependencies(),
            inputs.providerTypeDependencies(),
            inputs.providerModuleDependencies(),
            inputs.hookTypeDependencies(),
            inputs.hookModuleDependencies()
        );
        Map<String, Object> javaBrowserViews = ArchitectureIrJavaBrowserViewSupport.buildJavaBrowserViews(
            inputs.endpointTypeDependencies(),
            inputs.endpointModuleDependencies(),
            inputs.entityAssociationRelationships(),
            inputs.entityModelTypeDependencies(),
            inputs.entityModelModuleDependencies(),
            inputs.observerTypeDependencies(),
            inputs.observerModuleDependencies(),
            inputs.writePathTypeDependencies(),
            inputs.writePathModuleDependencies()
        );
        Map<String, Object> browserViewCatalog = ArchitectureIrBrowserViewFamilyCatalogSupport.buildBrowserViewCatalog(frontendBrowserViews, javaBrowserViews);
        return new ArchitectureIrBrowserViewComposition(frontendBrowserViews, javaBrowserViews, browserViewCatalog);
    }

    static Map<String, Object> buildFrontendBrowserViews(
        List<Map<String, Object>> compositionTypeDependencies,
        List<Map<String, Object>> compositionModuleDependencies,
        List<Map<String, Object>> routeTypeDependencies,
        List<Map<String, Object>> routeModuleDependencies,
        List<Map<String, Object>> providerTypeDependencies,
        List<Map<String, Object>> providerModuleDependencies,
        List<Map<String, Object>> hookTypeDependencies,
        List<Map<String, Object>> hookModuleDependencies
    ) {
        return ArchitectureIrFrontendBrowserViewSupport.buildFrontendBrowserViews(
            compositionTypeDependencies,
            compositionModuleDependencies,
            routeTypeDependencies,
            routeModuleDependencies,
            providerTypeDependencies,
            providerModuleDependencies,
            hookTypeDependencies,
            hookModuleDependencies
        );
    }

    static Map<String, Object> buildJavaBrowserViews(
        List<Map<String, Object>> endpointTypeDependencies,
        List<Map<String, Object>> endpointModuleDependencies,
        List<Map<String, Object>> entityAssociationRelationships,
        List<Map<String, Object>> entityModelTypeDependencies,
        List<Map<String, Object>> entityModelModuleDependencies,
        List<Map<String, Object>> observerTypeDependencies,
        List<Map<String, Object>> observerModuleDependencies,
        List<Map<String, Object>> writePathTypeDependencies,
        List<Map<String, Object>> writePathModuleDependencies
    ) {
        return ArchitectureIrJavaBrowserViewSupport.buildJavaBrowserViews(
            endpointTypeDependencies,
            endpointModuleDependencies,
            entityAssociationRelationships,
            entityModelTypeDependencies,
            entityModelModuleDependencies,
            observerTypeDependencies,
            observerModuleDependencies,
            writePathTypeDependencies,
            writePathModuleDependencies
        );
    }

    static Map<String, Object> buildBrowserViewCatalog(
        Map<String, Object> frontendBrowserViews,
        Map<String, Object> javaBrowserViews
    ) {
        return ArchitectureIrBrowserViewFamilyCatalogSupport.buildBrowserViewCatalog(frontendBrowserViews, javaBrowserViews);
    }

    static List<String> stringList(Object value) {
        return ArchitectureIrBrowserViewDescriptorFactory.stringList(value);
    }
}
