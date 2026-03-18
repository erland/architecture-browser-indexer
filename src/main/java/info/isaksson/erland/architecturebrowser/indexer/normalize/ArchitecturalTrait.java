package info.isaksson.erland.architecturebrowser.indexer.normalize;

/**
 * Canonical secondary architectural traits.
 */
public enum ArchitecturalTrait {
    STATEFUL("stateful"),
    TRANSACTIONAL("transactional"),
    PERSISTENT("persistent"),
    EXTERNALLY_EXPOSED("externally-exposed"),
    SCHEDULED("scheduled"),
    MESSAGE_DRIVEN("message-driven"),
    FRAMEWORK_MANAGED("framework-managed"),
    SECURITY_RELEVANT("security-relevant"),
    CONFIGURATION_DRIVEN("configuration-driven");

    private final String id;

    ArchitecturalTrait(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
