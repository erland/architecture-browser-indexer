package info.isaksson.erland.architecturebrowser.indexer.ir;

import java.util.List;
import java.util.Map;

record ArchitectureIrBrowserViewCompositionInputs(
    List<Map<String, Object>> compositionTypeDependencies,
    List<Map<String, Object>> compositionModuleDependencies,
    List<Map<String, Object>> routeTypeDependencies,
    List<Map<String, Object>> routeModuleDependencies,
    List<Map<String, Object>> providerTypeDependencies,
    List<Map<String, Object>> providerModuleDependencies,
    List<Map<String, Object>> hookTypeDependencies,
    List<Map<String, Object>> hookModuleDependencies,
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
}
