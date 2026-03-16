# Refactoring Step 3 — Introduce internal extraction result models where needed

This step reduces hidden coupling between the Java extraction traversal and the semantic collaborator logic by introducing explicit internal context models.

## What changed

`JavaStructuralExtractor` now uses internal context/result records to carry extraction state into semantic collaborators instead of passing long parameter lists around:

- `JavaExtractionContext`
- `JavaTypeContext`
- `JavaFieldContext`
- `JavaMethodContext`

## Intent

The main goal is to make the semantic collaborators depend on a stable internal contract rather than on ad hoc collections of method parameters.

That gives us:

- clearer ownership of per-file extraction state
- clearer ownership of owner-type/member context
- a single place for method snippet/source-reference derivation
- less hidden coupling between traversal code and semantic enrichment code

## Why this is useful

Before this step, the semantic collaborators depended on several repeated argument bundles such as:

- relative path
- package name
- imports
- declared types
- owner type identity
- source text
- method snippet/source ref derivation

Those bundles were easy to drift apart when refactoring.

With the new internal models:

- file-level state is grouped in `JavaExtractionContext`
- type-level semantic work uses `JavaTypeContext`
- field-level semantic work uses `JavaFieldContext`
- method-level semantic work uses `JavaMethodContext`

This makes later extraction-result or assembler refactors safer.

## Scope of the change

This step is intentionally internal and behavior-preserving:

- no architect-facing IR contract changes are intended
- no new extracted semantics are introduced
- the accumulator/output model is still unchanged

## Continuation notes

A natural next follow-up would be to push this one step further and let semantic collaborators return explicit semantic result objects, for example:

- updated entity metadata patches
- emitted relationship batches
- emitted observed endpoint/event/write-path facts

That would reduce direct accumulator coupling even further, but was intentionally left out of this step to keep the change smaller and safer.
