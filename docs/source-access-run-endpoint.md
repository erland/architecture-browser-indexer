# Source-access metadata on the worker run endpoint

This step finalizes the run-endpoint contract for source viewing.

## Objective
When a worker indexing run completes successfully and retained-source metadata is available, the HTTP run response must expose a top-level `sourceAccess` block that the platform can persist for later source-file reads.

## Contract
The worker run endpoint response now carries:

- `summary.sourceAccess` for backward-compatible diagnostics and low-level inspection
- top-level `sourceAccess` for the platform integration contract

The top-level `sourceAccess` object is derived from the worker summary and contains:

- `lookupKeyKind`
- `sourceHandle`
- `retainedRootKind`
- `acquisitionType`
- `repositoryId`
- `sourceRevision`
- `retentionPolicy`
- `createdAt`
- `expiresAt`

## Behavior rules
- `sourceAccess` is additive and optional.
- Clients must treat missing `sourceAccess` as “source viewing unavailable for this run”.
- The run endpoint remains backward compatible for clients that ignore the new field.
- The platform should persist only the top-level `sourceAccess` contract and later send `sourceHandle + repository-relative path` to the source-read endpoint.

## Implementation notes
A dedicated `HttpWorkerSourceAccessMapper` now converts retained-source metadata from the worker summary map into the typed `HttpWorkerSourceAccess` value returned on `HttpWorkerRunResponse`.

This keeps the run-endpoint contract explicit and isolated from the internal summary structure.
