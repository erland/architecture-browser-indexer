# Source-handle registry service

This step introduces a dedicated `RetainedSourceHandleRegistryService` for retained-source management.

## Purpose

The registry service becomes the single worker-side seam for retained source-handle records. It is responsible for:

- creating retained-source records for local-path and retained Git runs
- persisting handle records under `source-retention/handles`
- reloading records by `sourceHandle`
- validating active records before use
- updating `lastAccessedAt`
- enumerating and identifying expired records
- deleting records and retained Git roots when a handle is removed

## Why this is needed

Step 2 proved that source roots can be preserved. Step 3 formalizes that behavior behind a service boundary so later steps can depend on one abstraction instead of scattered file-system helpers.

This keeps later work cleaner for:

- safe source-file reads
- TTL cleanup/pruning
- access auditing / touch-on-read
- future endpoint implementation

## Current boundary

`WorkerModeService` now uses `RetainedSourceHandleRegistryService` when a successful run should expose `sourceAccess`.

The service currently supports:

- `createLocalPathRecord(...)`
- `createRetainedGitCheckout(...)`
- `save(...)`
- `find(...)`
- `getRequired(...)`
- `getActive(...)`
- `touch(...)`
- `list()`
- `findExpired(...)`
- `delete(...)`
- `validateRecord(...)`
- `isExpired(...)`

## Notes

This step does **not** yet add the source-file read endpoint. That remains a later step.
