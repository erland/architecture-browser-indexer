# Priority 1 — Step 2: Split Java syntax-tree traversal from Java extraction orchestration

This step separates recursive syntax-tree walking from extraction-stage orchestration.

## What changed

- Introduced `JavaSyntaxTreeTraversal` as a dedicated traversal helper.
- Moved recursive child walking and ownership propagation out of `JavaSyntaxTreeExtractionStage`.
- Reduced `JavaSyntaxTreeExtractionStage` to node handling/orchestration logic.
- Added `JavaSyntaxTreeTraversalTest` to freeze ownership propagation behavior.

## Resulting seam

- `JavaSyntaxTreeTraversal` owns recursion and child visitation order.
- `JavaSyntaxTreeExtractionStage` owns semantic handling for a single visited node and returns updated ownership for descendants.

## Why this matters

This creates a clear seam for the next steps:

- member extraction can be split independently from traversal,
- type-level and method-level extraction can evolve without touching recursion,
- stage tests can focus on semantics while traversal tests focus on ownership propagation.
