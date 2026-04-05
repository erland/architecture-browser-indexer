# Architecture Browser — Indexer Plan for On-Demand Source Viewing

## Goal
Add a backend capability in the **indexer** component that lets the platform request the source text for a selected indexed object **on demand**, without embedding full source files in the exported IR document.

This plan assumes the first release is **read-only**:
- no clickable navigation inside the source view
- no editing
- no symbol-to-symbol jumping
- no source persistence inside the platform database

## Starting state
The current indexer already provides:
- end-to-end indexing through `IndexerApplicationService`
- HTTP worker support through `IndexerWorkerHttpServer`
- synchronous run execution through `HttpWorkerService` and `WorkerModeService`
- source traceability in exported `sourceRefs`
- path/line metadata in source references

The current worker also appears to clean up temporary workspaces for temporary acquisitions in `WorkerModeService`, which would currently prevent later file retrieval for Git-based runs.

## Assumptions
1. The platform can identify a snapshot/run/repository context that corresponds to the source currently being browsed.
2. `sourceRefs.path` values are relative, repository-root-relative paths and should remain the public file locator between platform and indexer.
3. The first version may rely on indexer-side retained workspaces or retained checkouts for a bounded time window.
4. Only text files that were part of the indexed file inventory should be retrievable.
5. The feature should work for both local-path indexing and Git indexing, but Git indexing may require a retention policy.

## Scope
### In scope
- retain enough source context to reopen the indexed source tree after a run
- add worker HTTP endpoints for source retrieval
- validate and safely resolve relative file paths
- return file content plus metadata useful to the platform viewer
- support optional line-range requests for initial focus
- tests for retained-workspace lookup, file resolution, and HTTP API behavior

### Out of scope
- clickable source symbols
- IDE/LSP-style navigation
- editing or saving files
- source diffing
- storing full source text in the exported IR
- arbitrary filesystem browsing

---

# Step 1 — Define the retained-source contract and lookup key

## Objective
Choose the minimal identifier set the platform will use to ask the indexer for a file.

## Deliverables
- New indexer-side design note in `docs/` describing source retrieval lookup semantics.
- Explicit request/response contract for a source-view endpoint.
- Decision on whether lookup is keyed by:
  - `snapshotOut` / produced snapshot path metadata
  - `jobId`
  - `repositoryId + revision`
  - or a new durable `sourceHandle`

## Recommended approach
Introduce a durable **source handle** recorded by the worker after a successful run. It should map to:
- retained root directory
- repository identifier
- acquisition type (`local-path` or `git`)
- source revision / git ref if known
- file inventory manifest path if available
- retention metadata such as `createdAt`, `expiresAt`, and `lastAccessedAt`

Return that `sourceHandle` in the worker run response so the platform can persist it alongside the imported snapshot metadata.

## Suggested response additions
Extend the HTTP worker run response with a `sourceAccess` section, for example:
- `sourceHandle`
- `retainedRootKind`
- `retentionPolicy`
- `expiresAt`
- optional `sourceRevision`

## Why first
This avoids tying the later API to internal temp paths or worker-only job folders.

## Verification
- Design review confirms the platform can persist and later send the chosen lookup key.
- Contract document clearly states that the platform never sends absolute filesystem paths.

---

# Step 2 — Preserve retrievable source roots after indexing

## Objective
Make source retrieval possible after a successful run.

## Deliverables
- Source retention support in the worker path.
- A retention strategy for local-path and Git acquisitions.
- Cleanup policy documentation.

## Implementation notes
### Local-path runs
For local-path inputs, the source already exists outside the worker. The retained source record can reference the normalized original source root path, provided the indexer is allowed to read it later.

### Git runs
For Git inputs, the current code deletes temporary workspaces when `runResult.temporaryWorkspace()` is true. Replace unconditional cleanup with retention-aware behavior:
- retain the acquired checkout under a stable directory inside the worker workspace
- record the retained root in the source-handle registry
- clean up later via TTL or explicit pruning

### Registry options
Keep the first version simple:
- file-based registry under the HTTP worker workspace, for example `build/http-worker/source-handles/`
- one JSON record per source handle

Record at least:
- source handle
- retained root path
- repositoryId
- acquisition type
- gitUrl / gitRef if present
- source revision if known
- createdAt / expiresAt / lastAccessedAt
- optional indexed file inventory path

## Refactoring targets
Likely files to touch:
- `src/main/java/.../worker/WorkerModeService.java`
- `src/main/java/.../worker/http/HttpWorkerService.java`
- possibly `IndexerApplicationService` result plumbing if additional source metadata must be returned

## Verification
- Successful Git-indexed run leaves a retained checkout or retained handle entry instead of immediately deleting the checkout.
- Successful local-path run records a retrievable handle without copying the local source tree.
- Existing indexing behavior remains unchanged when source retention is enabled.

---

# Step 3 — Add a source-handle registry service

## Objective
Centralize retained-source lookup and lifecycle management.

## Deliverables
- New service class responsible for:
  - creating source-handle records
  - loading handle records
  - validating expiry
  - resolving retained roots
  - updating `lastAccessedAt`
- Unit tests for registry behavior.

## Suggested structure
Create a small `worker/sourceaccess/` package with focused collaborators such as:
- `SourceHandleRegistry`
- `SourceHandleRecord`
- `SourceHandleResolver`
- `SourceHandleRetentionPolicy`

## Behavior rules
- Reject unknown handles.
- Reject expired handles.
- Reject handles whose retained root no longer exists.
- Return a platform-safe error classification for each failure mode.

## Verification
- Creating and reading a handle record works from tests using temp directories.
- Expired and missing-handle cases return deterministic failures.

---

# Step 4 — Add safe file-resolution and text-file validation

## Objective
Prevent the new API from becoming a generic filesystem browser.

## Deliverables
- Safe path resolution helper.
- Text-file detection and size guardrails.
- Optional file-in-inventory validation.

## Required rules
When resolving a requested file:
1. Accept only a repository-relative path.
2. Normalize the path.
3. Reject blank paths.
4. Reject traversal attempts such as `..` escaping the retained root.
5. Reject paths that resolve outside the retained root.
6. Reject directories.
7. Reject files above a configured size limit.
8. Reject binary files.
9. Preferably reject files that were not in the indexed file inventory.

## Suggested output metadata
The resolved-source response should include:
- canonical relative path
- detected language or file type
- total line count
- file size
- optional requested line range echo
- source text

## Verification
- Unit tests cover path traversal, missing file, directory request, oversize file, binary file, and valid text file.

---

# Step 5 — Add a worker HTTP endpoint for source retrieval

## Objective
Expose a small read-only API that the platform can call after the user clicks “Show source”.

## Deliverables
- New HTTP route in the indexer worker.
- Request/response DTOs.
- Error mapping for validation and lookup failures.

## Recommended endpoint
Add one endpoint first:
- `POST /api/source-files/read`

Use POST rather than GET for easier future expansion and to keep request payload structured.

## Suggested request fields
- `sourceHandle`
- `path` (relative path from `sourceRefs`)
- optional `startLine`
- optional `endLine`
- optional `maxBytes` or `maxLines` override within server limits

## Suggested response fields
- `sourceHandle`
- `path`
- `language`
- `lineCount`
- `content`
- `requestedRange`
- `resolvedRange`
- `truncated`
- optional `encoding`

## Suggested implementation seam
Create a dedicated HTTP service layer parallel to `HttpWorkerService`, for example:
- `worker/http/HttpSourceFileService`
- `worker/http/model/HttpSourceFileRequest`
- `worker/http/model/HttpSourceFileResponse`

Update `IndexerWorkerHttpServer` to register the new route and route-specific validation.

## Verification
- Worker serves a valid source file via HTTP with JSON response.
- Invalid requests return stable 4xx error responses.
- Missing/expired handle returns a stable not-found/expired error shape.

---

# Step 6 — Return source access metadata from the run endpoint

## Objective
Let the platform persist the information needed for later source retrieval.

## Deliverables
- Extended run response model.
- Tests proving the source handle is included after a successful run.

## Implementation notes
Update the objects returned by:
- `HttpWorkerService.runJob(...)`
- `HttpWorkerRunResponse`

Add a source-access section with the new handle and retention metadata.

This is the bridge between the indexing flow and the later source-view flow.

## Verification
- Existing platform-facing run response remains backward compatible except for additive fields.
- Response now contains source access metadata for successful runs.

---

# Step 7 — Add source language detection for viewer friendliness

## Objective
Help the platform render syntax highlighting without duplicating language-detection logic.

## Deliverables
- Lightweight language detection support in the source-file response.
- Unit tests for common file extensions already supported by the indexer.

## Suggested behavior
Start with extension-based mapping only. Return values such as:
- `java`
- `typescript`
- `tsx`
- `javascript`
- `jsx`
- `json`
- `yaml`
- `sql`
- `xml`
- `plaintext`

Keep it simple and deterministic.

## Verification
- Representative file names map to expected viewer language identifiers.

---

# Step 8 — Add retention cleanup and operational safeguards

## Objective
Prevent retained Git workspaces from growing without bound.

## Deliverables
- Retention TTL setting and documented default.
- Pruning logic.
- Operational notes in `docs/http-worker-service.md` or a new source-access doc.

## Suggested first version
- configurable TTL, for example 24h or 7d depending on expected usage
- prune expired source handles on worker startup and/or after each run
- best-effort background-free pruning during request handling and run completion

## Verification
- Expired retained Git checkout is removed by pruning logic.
- Registry entry is removed or marked invalid when root is missing.

---

# Step 9 — Add end-to-end tests for the source-view contract

## Objective
Prove the new capability works through the actual worker HTTP flow.

## Deliverables
- Integration tests covering:
  - local-path indexing then source retrieval
  - Git indexing then source retrieval using retained workspace
  - invalid path rejection
  - expired handle rejection
  - missing file rejection

## Suggested test strategy
- use temp fixture repositories
- run the worker service in-process where possible
- verify that returned file content matches expected fixture text
- verify line-range echo fields and detected language

## Verification
- Tests pass locally with standard Maven test command.

---

# Step 10 — Document the platform integration contract

## Objective
Freeze a simple first-version contract that platform work can build against.

## Deliverables
- A source-access contract doc under `docs/`
- example request/response payloads
- operational notes about retention behavior and limitations

## Contract points to document
- platform persists `sourceHandle` per imported snapshot
- platform sends only `sourceHandle + relative path + optional range`
- worker returns full file text for read-only viewing
- no clickable symbol navigation in v1
- retrieval may fail when retention expires

## Verification
- Another developer could implement the platform consumer from the document alone.

---

# Suggested implementation sequence
1. Step 1 — retained-source contract
2. Step 2 — retain source roots after run
3. Step 3 — source-handle registry
4. Step 4 — safe file-resolution
5. Step 5 — source retrieval HTTP endpoint
6. Step 6 — include source-access metadata in run response
7. Step 7 — language detection
8. Step 8 — pruning / safeguards
9. Step 9 — end-to-end tests
10. Step 10 — documentation

---

# Verification commands
Run these after each meaningful step where applicable.

## Build and unit tests
```bash
cd indexer
mvn test
```

## Package without skipping tests
```bash
cd indexer
mvn package
```

## Start HTTP worker locally
```bash
cd indexer
mvn exec:java -Dexec.mainClass=info.isaksson.erland.architecturebrowser.indexer.worker.http.IndexerWorkerHttpServer
```

## Manual source retrieval check
After implementing the new endpoint, exercise:
- one local-path run
- one source retrieval request using returned `sourceHandle`
- verify returned content, path, and language

---

# Expected end state
After this plan is complete, the indexer will:
- retain enough source context after a run to support later drill-down
- expose a safe on-demand source retrieval endpoint
- return file content and metadata suitable for a syntax-highlighted read-only viewer
- avoid embedding full source text in the exported IR document
- support the platform’s first non-clickable source-view feature with bounded operational cost
