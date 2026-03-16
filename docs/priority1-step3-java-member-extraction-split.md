# Priority 1 — Step 3: Split Java member extraction from type-level extraction flow

## What changed

This step reduces `JavaSyntaxTreeExtractionStage` by separating member handling from type-level extraction.

A dedicated internal helper was introduced:

- `JavaMemberExtractionFlow`

`JavaSyntaxTreeExtractionStage` now keeps the type-declaration path in `handleTraversalNode(...)` while delegating field and method handling to the member-flow helper.

## Responsibilities now delegated to the member flow

### Field path
- field entity creation
- containment relationship emission
- declared-type dependency emission for fields
- JPA field fact enrichment

### Method path
- method entity creation
- containment relationship emission
- return-type dependency emission
- parameter-type dependency emission
- JAX-RS endpoint enrichment
- JPA method enrichment
- CDI event enrichment
- write-path enrichment

## Why this step matters

This makes the stage easier to read and prepares the next refactor steps:

- type-level traversal/orchestration can evolve independently of member extraction
- member extraction can be split further without disturbing type-level flow
- future context/result object extraction becomes easier because the member path is now localized

## Intended behavior

This is a structural refactor only. Existing Java extraction behavior should remain unchanged, and the existing stage-level safety-net tests should continue to protect:

- type / field / method extraction
- containment relationships
- declared-type dependencies
- JAX-RS endpoint facts
- JPA field/method facts
- CDI event facts
- write-path facts
