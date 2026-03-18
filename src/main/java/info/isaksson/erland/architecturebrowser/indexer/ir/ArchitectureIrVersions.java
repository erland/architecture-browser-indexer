package info.isaksson.erland.architecturebrowser.indexer.ir;

public final class ArchitectureIrVersions {
    /**
     * Baseline stable export contract version.
     *
     * The current JSON Schema treats the document/entity/relationship shapes as strict
     * ({@code additionalProperties: false}), so later normalization fields such as
     * architectural roles, traits, relationship semantics, or document-level viewpoints
     * should only be introduced together with coordinated schema/example/validator updates
     * and an explicit schema-version review. Step 1 intentionally keeps the version pinned
     * while documenting these extension points.
     */
    public static final String CURRENT_SCHEMA_VERSION = "1.0.0";

    private ArchitectureIrVersions() {
    }
}
