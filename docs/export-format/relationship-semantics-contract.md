# Relationship semantics contract

Step 3 introduces an optional first-class relationship field:

- `architecturalSemantics`

This field is intended to hold normalized architectural relationship semantics that a platform consumer can use without inspecting framework-specific metadata.

## Contract shape

- field lives directly on `ArchitectureRelationship`
- field is optional
- values are canonicalized for deterministic output:
  - trim surrounding whitespace
  - drop blanks
  - de-duplicate
  - sort
- unchanged producers/fixtures can omit the field

## Compatibility note

Because relationship objects are schema-strict, introducing this stable field required a schema version advance from `1.1.0` to `1.3.0`.

## Scope of this step

This step only adds the stable contract field and validation support. It does **not** yet require the indexer to emit real semantics from technology-specific evidence. That mapping work belongs in later steps such as Java-first relationship normalization.
