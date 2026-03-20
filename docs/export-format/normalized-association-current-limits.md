# Normalized association current limits

This note summarizes the current conservative limits of normalized association metadata.

- Collection lower bounds default to `0` in the first version.
- `1..*` is not inferred without stronger evidence.
- Some source-side bounds remain conservative first-version approximations.
- Exact UML-style multiplicity semantics depend on extracted framework evidence such as JPA association type, `optional`, and `@JoinColumn(nullable = ...)`.
- JPA-specific metadata remains part of the export as provenance/evidence and is not removed by normalization.

See [normalized-association-metadata-contract](normalized-association-metadata-contract.md) for the full contract and rationale.
