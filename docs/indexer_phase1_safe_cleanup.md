# Phase 1 — Safe cleanup inside indexer

This cleanup removes the legacy retained-worker source retrieval flow now that snapshot source files are exported inline to the platform.

Removed:
- worker `/api/source-files/read` endpoint
- retained-source handle and cleanup services
- legacy `sourceAccess` handoff in worker run responses
- retained-source tests and JSON contract tests

Kept:
- inline `snapshotSourceFiles` in the worker run response
- snapshot source-file export pipeline
- language detection used by snapshot source-file metadata

Behavioral result:
- worker jobs no longer retain source roots after success
- temporary Git workspaces are cleaned up immediately after successful runs
- the durable source-view path is now the platform-owned snapshot source-file flow
