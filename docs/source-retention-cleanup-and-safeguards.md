# Source Retention Cleanup and Operational Safeguards

## Summary
Step 8 adds bounded retention behavior for retained source roots used by on-demand source viewing.

The main goals are:
- prevent retained Git checkouts from growing without bound
- prune expired handles without background workers
- prune broken handle records when their retained roots disappear
- keep the first version operationally simple

## Default retention behavior
Git-retained source roots now use a configurable TTL.

Default:
- 7 days

Configuration:
- Java system property: `archbrowser.worker.source-retention.git-ttl-hours`
- Environment variable: `ARCH_BROWSER_SOURCE_RETENTION_GIT_TTL_HOURS`

The configured value is interpreted as whole hours and must be greater than zero.

Examples:
- `24` → 24 hours
- `168` → 7 days

Local-path references are still recorded as external references and do not get a TTL by default.

## Pruning behavior
Best-effort pruning now runs in three places:
- on HTTP worker startup
- before each worker run
- before each source-file read

This keeps the implementation background-free while still cleaning up expired state during normal worker traffic.

## What gets pruned
A retained-source handle is pruned when:
- the handle has expired based on `expiresAt`
- the retained root no longer exists
- the retained root exists but is no longer a directory

For retained Git checkouts, pruning removes both:
- the registry JSON record
- the retained checkout directory under `source-retention/roots/<sourceHandle>`

For local-path references, pruning removes the registry JSON record when the referenced root no longer exists.

## Operational notes
- Cleanup is best-effort. A pruning failure should not block the worker from serving indexing or source-view requests.
- Successful source-file resolution continues to update `lastAccessedAt` through the registry service.
- The platform should still treat source access as temporary and be prepared for retrieval failures after retention expiry.

## Current limits
This step does not add:
- background scheduled cleanup
- per-repository retention quotas
- byte-budget eviction policies
- inventory-based allowlisting for source reads

Those can be added later if retained source growth becomes a practical issue.
