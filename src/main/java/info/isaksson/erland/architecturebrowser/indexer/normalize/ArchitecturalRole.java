package info.isaksson.erland.architecturebrowser.indexer.normalize;

/**
 * Canonical architecture-facing entity roles.
 *
 * Step 4 seam note: Java/TypeScript/SQL/config-specific evidence should map into these ids via the
 * normalization layer rather than scattering role strings through extraction, interpretation, and
 * IR export code.
 */
public enum ArchitecturalRole {
    API_ENTRYPOINT("api-entrypoint"),
    APPLICATION_SERVICE("application-service"),
    DOMAIN_SERVICE("domain-service"),
    DOMAIN_ENTITY("domain-entity"),
    VALUE_OBJECT("value-object"),
    PERSISTENT_ENTITY("persistent-entity"),
    PERSISTENCE_ACCESS("persistence-access"),
    INTEGRATION_ADAPTER("integration-adapter"),
    EXTERNAL_DEPENDENCY("external-dependency"),
    ASYNC_ENTRYPOINT("async-entrypoint"),
    EVENT("event"),
    EVENT_HANDLER("event-handler"),
    CONFIGURATION_PROVIDER("configuration-provider"),
    MODULE_BOUNDARY("module-boundary");

    private final String id;

    ArchitecturalRole(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
