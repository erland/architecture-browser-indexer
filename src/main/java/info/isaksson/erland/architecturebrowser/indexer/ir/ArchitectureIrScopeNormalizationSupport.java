package info.isaksson.erland.architecturebrowser.indexer.ir;

final class ArchitectureIrScopeNormalizationSupport {
    private ArchitectureIrScopeNormalizationSupport() {
    }

    static String normalizeScopeId(String scopeId, String repositoryScopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return repositoryScopeId;
        }
        return scopeId;
    }
}
