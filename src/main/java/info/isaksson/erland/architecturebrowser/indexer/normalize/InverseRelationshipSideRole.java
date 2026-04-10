package info.isaksson.erland.architecturebrowser.indexer.normalize;

/**
 * Framework-neutral side semantics for a relationship that participates in inverse-pair
 * normalization.
 * <p>
 * The role describes whether the relationship was emitted from the owning side of the association,
 * the inverse side, or whether the framework could not determine a role. This allows canonical-side
 * selection to stay generic across frameworks without depending on free-form metadata keys such as
 * {@code mappedBy}.
 */
enum InverseRelationshipSideRole {
    OWNING,
    INVERSE,
    UNSPECIFIED
}
