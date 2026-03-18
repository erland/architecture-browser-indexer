package info.isaksson.erland.architecturebrowser.indexer.ir;

public final class ArchitectureIrVersions {
    /**
     * Baseline stable export contract version.
     *
     * The current JSON Schema treats the document/entity/relationship shapes as strict
     * ({@code additionalProperties: false}). Step 2 introduces first-class entity-level
     * architectural roles and traits as coordinated stable-contract additions, so the
     * schema version now advances to reflect the new shape.
     */
    public static final String CURRENT_SCHEMA_VERSION = "1.1.0";

    private ArchitectureIrVersions() {
    }
}
