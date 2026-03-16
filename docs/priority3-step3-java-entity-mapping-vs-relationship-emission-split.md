# Priority 3 — Step 3: Split Java entity mapping from Java relationship/evidence emission

## What changed

This step separates two different concerns that had still been coupled inside `JavaStructuralExtractor`:

1. **Entity mapping** — turning Java syntax nodes into extracted entity facts
2. **Relationship/evidence emission** — turning declared types and hierarchy syntax into dependency and hierarchy facts

The split introduces two focused collaborators:

- `JavaEntityMapper`
- `JavaRelationshipEvidenceEmitter`

`JavaStructuralExtractor` now orchestrates these seams instead of directly owning both responsibilities.

## Why this matters

This reduces the remaining size and mixed responsibility inside `JavaStructuralExtractor` and creates a cleaner boundary for the next hotspot work:

- declaration/entity mapping can now evolve without touching dependency resolution behavior
- relationship/evidence behavior can now be hardened and refactored without changing how entities are shaped
- the later step of reducing `JavaStructuralExtractor` to a true stage orchestrator becomes simpler

## Current responsibility split

### `JavaEntityMapper`
Owns:
- type entity mapping
- field entity mapping
- method entity mapping
- declaration-kind shaping and core metadata population

### `JavaRelationshipEvidenceEmitter`
Owns:
- type hierarchy relationship emission
- declared-type dependency emission
- type reference normalization
- type reference resolution against imports/current package/declared types
- dependency metadata shaping

### `JavaStructuralExtractor`
Now mainly coordinates:
- traversal
- accumulator writes
- entity creation delegation
- dependency/hierarchy delegation
- Java semantic enrichers (JAX-RS/JPA/CDI/write paths)

## Safety net additions

Added focused tests for the new seams:

- `JavaEntityMapperTest`
- `JavaRelationshipEvidenceEmitterTest`

These are intentionally narrow seam tests and complement the broader existing safety-net tests.

## Recommended next step

Proceed with:

**Step 4 — Isolate JPA/CDI/write-path detail helpers behind narrower internal APIs**

That step should keep reducing what remains inside `JavaStructuralExtractor` while preserving the current seam-oriented test coverage.
