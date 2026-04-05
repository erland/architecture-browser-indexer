# Source View Retention Policy

## Status
Implemented baseline for **Source View — Step 2**.

## Scope
This policy covers how the indexer worker preserves source roots after a successful indexing run so the platform can request source later.

## Baseline behavior

### Local-path runs
- The worker does **not** copy the source tree.
- It records a retained-source handle that points at the normalized original local source root.
- The returned `retainedRootKind` is `LOCAL_PATH_REFERENCE`.
- The returned `retentionPolicy` is `local-path-reference`.
- No `expiresAt` is set in the current baseline.

### Git runs
- The worker retains the successful checkout under its workspace in:
  - `source-retention/roots/<sourceHandle>/repo`
- The worker writes one JSON handle record in:
  - `source-retention/handles/<sourceHandle>.json`
- The returned `retainedRootKind` is `RETAINED_GIT_CHECKOUT`.
- The returned `retentionPolicy` is `ttl-7d`.
- The baseline TTL is **7 days** from handle creation.

## Notes
- This step preserves retrievable source roots and writes handle records, but it does **not** yet implement read APIs or cleanup pruning.
- Cleanup/pruning is planned for a later step.
- The platform must persist only the `sourceAccess` block and later send `sourceHandle + repository-relative path`.
