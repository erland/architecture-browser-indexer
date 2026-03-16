# Priority 1 Java Stage Step 6 — Replace remaining implicit owner/state mutation with explicit result objects

## What changed

This step replaces several remaining implicit owner/state handoffs with explicit package-level context and request/result objects:

- `JavaOwnerContext`
- `JavaTypeNodeRequest`
- `JavaMemberNodeRequest`
- updated `JavaTypeTraversalResult` to carry `JavaOwnerContext`

## Why this helps

Previously, several stage and flow methods still passed raw owner state around as multiple separate values (`owningTypeEntityId`, `owningQualifiedName`, `owningTypeSnippet`) together with long parameter lists for node handling.

After this step:

- owner state is represented explicitly by `JavaOwnerContext`
- type-node handling uses `JavaTypeNodeRequest`
- member-node handling uses `JavaMemberNodeRequest`
- traversal handoff stays explicit instead of relying on scattered local tuple-like state

## Intent

This is a structural cleanup step only. The Java extraction contract should remain unchanged while the internal flow contracts become narrower and easier to evolve.
