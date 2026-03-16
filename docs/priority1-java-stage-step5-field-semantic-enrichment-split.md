# Priority 1 Java Stage Step 5 — Isolate field semantic enrichment from field structural extraction

## What changed

This step isolates JPA field semantic policy behind a dedicated helper:

- `JavaJpaFieldSemantics`

`JavaFieldExtractionFlow` now stays focused on structural field extraction and delegates field semantic enrichment to that helper.

## Why this helps

Before this step, `JavaFieldExtractionFlow` still directly owned JPA field semantics. After this step:

- field entity creation and structural dependency emission remain in `JavaFieldExtractionFlow`
- JPA association metadata application is isolated in `JavaJpaFieldSemantics`
- later field-specific cleanup can continue without mixing structure and persistence semantics again

## Intended behavior

No output contract changes are intended. This is a structural split only.
