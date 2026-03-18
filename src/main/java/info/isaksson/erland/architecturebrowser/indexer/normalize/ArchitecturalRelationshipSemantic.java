package info.isaksson.erland.architecturebrowser.indexer.normalize;

/**
 * Canonical architectural relationship semantics reserved for the normalization layer.
 *
 * Step 4 seam note: Step 3/6 will expose these on the export contract once the mapping rules are
 * implemented and validated.
 */
public enum ArchitecturalRelationshipSemantic {
    SERVES_REQUEST("serves-request"),
    INVOKES_USE_CASE("invokes-use-case"),
    ACCESSES_PERSISTENCE("accesses-persistence"),
    STORED_IN("stored-in"),
    CALLS_EXTERNAL_SYSTEM("calls-external-system"),
    PUBLISHES_EVENT("publishes-event"),
    TRIGGERS_CONSUMER("triggers-consumer"),
    DEPENDS_ON_MODULE("depends-on-module"),
    BELONGS_TO_MODULE("belongs-to-module"),
    MAPS_TO_CONTRACT("maps-to-contract"),
    ENFORCES_SECURITY("enforces-security");

    private final String id;

    ArchitecturalRelationshipSemantic(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
