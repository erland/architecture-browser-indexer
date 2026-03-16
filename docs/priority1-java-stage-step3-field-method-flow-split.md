# Priority 1 Java Stage Step 3 — Split field extraction from method extraction completely

## Summary

This step separates field extraction and method extraction into dedicated internal flows:

- `JavaFieldExtractionFlow`
- `JavaMethodExtractionFlow`

`JavaSyntaxTreeExtractionStage` still keeps a very small member-dispatch seam, but the concrete field and method handling logic no longer lives inside the stage.

## What moved

### JavaFieldExtractionFlow

Owns:

- field entity creation
- containment emission
- field declared-type dependency emission
- JPA field enrichment

### JavaMethodExtractionFlow

Owns:

- method entity creation
- containment emission
- return-type dependency emission
- parameter dependency emission
- method semantic enrichment via `JavaMethodSemanticsFlow`

## Why this helps

The Java stage can now treat member handling as two separate concerns instead of one combined member flow. That makes the next steps safer:

- method structural extraction can later be separated from method semantics more cleanly
- field extraction can later isolate JPA-specific policy behind even narrower seams
- tests can target field and method behavior independently

## Notes

This is intended as a structural refactor only. No public extraction contract should change.
