# Step 9 — Java browser/dependency view bridge

Step 9 reuses the existing Java-specific browser/dependency view metadata as migration evidence for the canonical viewpoint catalog.

## Why this step exists

The indexer already exports Java-oriented browser views such as:

- `javaEndpointGraph`
- `javaEntityModelGraph`
- `javaEventFlowGraph`
- `javaWritePathGraph`

Those views are still useful and may already be consumed elsewhere. The goal of this step is **not** to remove them. The goal is to let canonical viewpoint ids point at the same evidence so platform/browser work can prefer the normalized catalog without needing to understand Java-first metadata families directly.

## Bridge mappings

Implemented bridge mappings:

- `javaEndpointGraph` → `api-surface`
- `javaWritePathGraph` → `request-handling`
- `javaEntityModelGraph` → `persistence-model`
- `javaEventFlowGraph` → `event-flow`

## What gets populated

When the Java browser-view metadata is present, the canonical viewpoint descriptors are enriched with:

- `preferredDependencyViews` from the Java browser-view descriptor's preferred/type/module dependency view ids when those dependency views are populated
- `evidenceSources` including `java-browser-views` and `java-dependency-views` when relevant

## Migration behavior

This step keeps migration safe:

- the old Java-specific metadata remains in `metadata.dependencyViews`
- the canonical `viewpoints` catalog is enriched alongside it
- existing consumers of the Java browser-view metadata can continue to work unchanged
- newer consumers can begin to rely on canonical viewpoint ids plus preferred dependency-view ids

## Notes

This bridge is intentionally conservative:

- it does not remove or rename any Java-specific metadata
- it only emits preferred dependency-view ids when those dependency-view payloads are actually present
- it allows canonical viewpoints such as `event-flow` to appear from existing Java browser-view evidence even before a broader cross-language normalized event-flow semantic layer exists
