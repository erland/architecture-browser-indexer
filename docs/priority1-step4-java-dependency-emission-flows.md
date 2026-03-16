# Priority 1 — Step 4: Isolate Java dependency emission into narrower flows

This step introduces `JavaDependencyEmissionFlow` as a dedicated seam between Java extraction orchestration and low-level declared-type dependency emission.

## What moved

`JavaSyntaxTreeExtractionStage` now delegates these responsibilities to `JavaDependencyEmissionFlow`:

- type-level extends/implements/declared-type relationship emission
- field declared-type dependency emission
- method return-type dependency emission
- method parameter dependency emission

## Why this helps

Previously, `JavaSyntaxTreeExtractionStage` mixed:

- syntax-tree traversal orchestration
- type/member extraction
- dependency relationship emission policy

With this split, the stage still coordinates extraction, but the dependency-emission policy is now centralized behind a narrower internal API.

## Next likely step

The next cleanup can split dependency categories further, for example:

- hierarchy and declared-type rollups
- member API dependencies
- framework-specific dependency emission
