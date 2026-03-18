# Entity roles and traits contract (Step 2)

Step 2 introduces two first-class optional entity-level fields in the stable export contract:

- `architecturalRoles`
- `architecturalTraits`

## Design

Both fields are serialized as arrays of strings. The indexer canonicalizes them before export so output remains deterministic:

- blanks are discarded
- duplicates are removed
- values are sorted lexicographically

## Compatibility

Because entity objects are schema-strict, introducing these stable fields required a schema version advance from `1.0.0` to `1.3.0`.

## Initial representative examples

The curated Java backend example now demonstrates:

- `api-entrypoint`
- `application-service`
- `domain-entity`
- `persistent-entity`
- traits like `http-resource`, `stateless`, and `jpa-managed`


## UI navigation additions

The canonical trait set now also reserves:

- `user-facing` for entities that represent user-visible pages, layouts, or explicit navigation structures
- `route-declared` for entities grounded by direct route declaration evidence rather than only inference

These traits are additive and can coexist with earlier roles or traits.
