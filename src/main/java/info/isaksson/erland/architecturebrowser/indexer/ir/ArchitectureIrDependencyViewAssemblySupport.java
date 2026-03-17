package info.isaksson.erland.architecturebrowser.indexer.ir;

import info.isaksson.erland.architecturebrowser.indexer.ir.model.ArchitectureEntity;

import java.util.List;
import java.util.Map;

final class ArchitectureIrDependencyViewAssemblySupport {
    private ArchitectureIrDependencyViewAssemblySupport() {
    }

    static Map<String, Object> buildDependencyViews(ArchitectureIrDependencyViewAssemblyInputs inputs) {
        List<ArchitectureIrNormalizedDependencyContext> contexts = ArchitectureIrDependencyNormalizationSupport.normalize(inputs);
        List<Map<String, Object>> typeDependencies = ArchitectureIrTypeDependencyViewBuilder.build(contexts);
        List<Map<String, Object>> packageDependencies = ArchitectureIrPackageDependencyViewBuilder.build(contexts);
        List<Map<String, Object>> moduleDependencies = ArchitectureIrModuleDependencyViewBuilder.build(contexts);
        List<Map<String, Object>> evidenceDependencies = ArchitectureIrEvidenceDependencyViewBuilder.build(contexts);
        return ArchitectureIrDependencyViewPostProcessor.finalizeDependencyViews(
            inputs.entitiesById(),
            typeDependencies,
            packageDependencies,
            moduleDependencies,
            evidenceDependencies
        );
    }

    static String normalizeScopeId(String scopeId, String repositoryScopeId) {
        return ArchitectureIrScopeNormalizationSupport.normalizeScopeId(scopeId, repositoryScopeId);
    }

    static String stringMetadata(ArchitectureEntity entity, String key, String defaultValue) {
        if (entity == null || entity.metadata() == null) {
            return defaultValue;
        }
        Object value = entity.metadata().get(key);
        return value instanceof String s && !s.isBlank() ? s : defaultValue;
    }
}
