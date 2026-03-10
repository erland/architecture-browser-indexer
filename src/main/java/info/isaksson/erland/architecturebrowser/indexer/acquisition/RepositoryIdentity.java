package info.isaksson.erland.architecturebrowser.indexer.acquisition;

final class RepositoryIdentity {
    private RepositoryIdentity() {
    }

    static String resolve(String explicitRepositoryId, String fallbackName) {
        if (explicitRepositoryId != null && !explicitRepositoryId.isBlank()) {
            return explicitRepositoryId.trim();
        }
        return fallbackName == null || fallbackName.isBlank() ? "repository" : fallbackName;
    }
}
