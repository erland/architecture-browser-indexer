# Priority 1 — Step 5: Explicit Java stage context/result objects

This step makes the internal Java extraction stage less ad hoc by moving stage context records out of `JavaSyntaxTreeExtractionStage` and introducing explicit result objects for the main internal subflows.

## What changed

- Moved stage context records into package-level records:
  - `JavaExtractionContext`
  - `JavaTypeContext`
  - `JavaFieldContext`
  - `JavaMethodContext`
- Added explicit result objects for stage-level flows:
  - `JavaMemberExtractionResult`
  - `JavaTypeTraversalResult`
- Reduced `JavaSyntaxTreeExtractionStage` so it delegates:
  - type-node handling to `handleTypeNode(...)`
  - member-node handling to `JavaMemberExtractionFlow`

## Why this matters

The stage now has narrower internal contracts:

- stage inputs are no longer trapped in private nested records
- the type-handling branch has an explicit result object instead of mutating local ownership state inline
- member handling now returns a dedicated result object, which is a better seam for later split-outs and targeted tests

## Expected follow-up

The next steps can now continue shrinking `JavaSyntaxTreeExtractionStage` by:

1. splitting more member-level policy into dedicated supports
2. narrowing dependency emission further
3. making the stage primarily orchestration and flow composition
