# Source View Retained-Source Contract

## Status
Accepted design note for **Source View — Step 1**.

## Goal
Define the lookup contract the platform will persist after a successful indexing run so it can later request source text from the indexer **without** depending on worker-local temp paths.

## Decision
The retained-source lookup key is a durable **`sourceHandle`** owned by the indexer worker.

The platform must persist the `sourceAccess` block returned from the worker run response and later call source-read APIs using:
- `sourceHandle`
- repository-relative `path`
- optional `startLine`
- optional `endLine`

The platform must **never** send:
- absolute file system paths
- raw worker temp directories
- arbitrary host file locations

## Why `sourceHandle`
Alternative lookup keys were considered:

### `jobId`
Rejected as the main lookup key.
- A job id identifies one execution, not a durable retained source root.
- It couples later source retrieval too closely to transient worker execution details.

### `snapshotOut`
Rejected as the main lookup key.
- It is an output artifact path, not a stable source-root identity.
- It leaks file-system-oriented details across the service boundary.

### `repositoryId + revision`
Rejected as the only lookup key.
- It is not always sufficient for local-path runs.
- It may not be unique across multiple retained workspaces or retention windows.

### `sourceHandle`
Accepted.
- Stable across later source-read calls.
- Lets the worker change its internal retention/storage strategy without breaking the platform contract.
- Avoids exposing file system details outside the indexer.

## Run-response contract
The worker run response is extended with an optional `sourceAccess` section.

### Fields
- `lookupKeyKind`: currently always `SOURCE_HANDLE`
- `sourceHandle`: durable worker-owned identifier to persist in platform metadata
- `retainedRootKind`: worker-side classification such as `LOCAL_PATH_REFERENCE` or `RETAINED_GIT_CHECKOUT`
- `acquisitionType`: normalized acquisition type such as `LOCAL_PATH` or `GIT`
- `repositoryId`: repository identity when known
- `sourceRevision`: resolved revision/commit when known
- `retentionPolicy`: worker-owned policy name or short classification
- `createdAt`: when the retained source record was created
- `expiresAt`: when the retained source record expires, if applicable

### JSON example
```json
{
  "jobId": "job-001",
  "status": "SUCCESS",
  "outputPath": "/tmp/worker/job-001/architecture-index.json",
  "snapshotOut": "/tmp/worker/job-001/snapshot.json",
  "sourceAccess": {
    "lookupKeyKind": "SOURCE_HANDLE",
    "sourceHandle": "src_01JQ7D2S3R7M7A6K9N8Y4K1B7C",
    "retainedRootKind": "RETAINED_GIT_CHECKOUT",
    "acquisitionType": "GIT",
    "repositoryId": "sample-task-tracker",
    "sourceRevision": "4f1c9ef2f1d7a5cccb48e2a8d6f7a11b7f2de999",
    "retentionPolicy": "ttl-7d",
    "createdAt": "2026-04-05T15:00:00Z",
    "expiresAt": "2026-04-12T15:00:00Z"
  }
}
```

## Planned source-read contract
The first source-read endpoint will be keyed by `sourceHandle + path`.

### Request
```json
{
  "sourceHandle": "src_01JQ7D2S3R7M7A6K9N8Y4K1B7C",
  "path": "src/main/java/com/example/FooService.java",
  "startLine": 18,
  "endLine": 44
}
```

### Response
```json
{
  "sourceHandle": "src_01JQ7D2S3R7M7A6K9N8Y4K1B7C",
  "path": "src/main/java/com/example/FooService.java",
  "language": "java",
  "totalLineCount": 240,
  "fileSizeBytes": 8012,
  "requestedStartLine": 18,
  "requestedEndLine": 44,
  "sourceText": "package com.example;\n..."
}
```

## Path semantics
- `path` is always repository-relative.
- The indexer resolves that path against the retained source root for the provided `sourceHandle`.
- Resolution must reject traversal and any path that escapes the retained root.

## Compatibility notes
- `sourceAccess` is optional during rollout.
- Platform integration must treat missing `sourceAccess` as “source viewing unavailable for this run”.
- Future fields may be added to `sourceAccess` and source-read responses; clients must ignore unknown fields.
