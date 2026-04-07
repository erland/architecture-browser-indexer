# Indexer plan — per-snapshot referenced source file export

## Goal
Enable the indexer to export the full text of source files referenced by `sourceRefs`, so the platform can store those files per snapshot and serve source view without depending on retained worker checkouts.

## Scope
In scope:
- collect referenced file paths from the final architecture output
- read full file contents for those referenced files during indexing
- emit those files as part of the indexer output contract for snapshot import
- include basic metadata needed by the platform source viewer

Out of scope:
- cross-snapshot deduplication
- clickable code navigation
- storing every file in the repository
- binary file archival
- partial snippet-only storage

## Assumptions
- Source view should work from snapshot-owned data, not from retained worker checkouts.
- The first implementation stores each referenced file once per snapshot export, even if the same file is unchanged across many snapshots.
- Only text files that are referenced by `sourceRefs` should be exported.
- Existing `sourceRefs.path` values are repository-relative and are suitable as snapshot file keys.

## Proposed export shape
Add a new top-level artifact section in the indexer snapshot/export contract, conceptually like:
- `sourceFiles[]`
  - `path`
  - `language`
  - `sizeBytes`
  - `lineCount`
  - `content`

If your current export model prefers sidecar files instead of one large JSON payload, use that pattern, but keep the same logical data shape.

## Step 1 — Define the snapshot source-file export contract
### Deliverables
- Add a documented contract for exported referenced source files.
- Decide whether source files travel:
  - inline inside the main snapshot JSON, or
  - as a sidecar artifact referenced from the manifest.
- Define field names and validation rules.

### Implementation notes
Prefer a sidecar artifact if the main snapshot JSON is already large. Keep the contract stable and explicit.

### Verification
- Review the documented contract in `docs/`.
- Confirm the contract supports all current platform viewer needs: path, language, size, line count, content.

## Step 2 — Add source-file export model classes
### Deliverables
- Add indexer DTO/model types for exported source files.
- Add serializer support for the new source-file artifact shape.

### Implementation notes
Keep these types separate from runtime worker source-retention types. This is snapshot export data, not worker cache metadata.

### Verification
- Unit test JSON serialization/deserialization of the new source-file model.

## Step 3 — Collect unique referenced file paths from the final snapshot content
### Deliverables
- Add a service that scans exported entities, relationships, scopes, and diagnostics for `sourceRefs`.
- Normalize and de-duplicate referenced file paths.

### Implementation notes
Only collect valid, non-empty repository-relative paths. Store each path once per snapshot export.

### Verification
- Unit tests for path collection from mixed objects with duplicate `sourceRefs`.
- Verify that one file referenced by many objects is exported once.

## Step 4 — Reuse safe text-file reading rules for export-time collection
### Deliverables
- Add or reuse safe file-reading logic for snapshot artifact export.
- Reject binaries, directories, unreadable files, and oversized files.

### Implementation notes
Reuse the same basic text-file rules you already added for source-view reads where possible, but keep the export service independent from worker retention concerns.

### Verification
- Unit tests for binary rejection, missing-file handling, and oversized-file rejection.

## Step 5 — Add source language detection to exported files
### Deliverables
- Populate `language` on exported source files using the same language detection logic used by the source viewer contract.

### Implementation notes
Keep one consistent language mapping across worker/source-view/export flows.

### Verification
- Unit tests that exported files include expected language values for Java, TypeScript, TSX, XML, JSON, YAML, SQL, and Markdown.

## Step 6 — Build the referenced source-file artifact during indexing
### Deliverables
- Add a dedicated service that, after the architecture snapshot has been assembled, collects referenced paths and reads those files into an export artifact.
- Include size and line-count metadata.

### Implementation notes
Run this late enough that final `sourceRefs` are already known, but before export bundle writing finishes.

### Verification
- Unit/integration test that a sample indexed project produces source-file artifacts for all referenced files.

## Step 7 — Include the source-file artifact in the export bundle/manifest
### Deliverables
- Update bundle writing so source-file artifacts are emitted with the snapshot export.
- Update manifest metadata so the platform importer can detect and read the artifact.

### Implementation notes
If your export already supports multi-file artifacts, use that pattern to avoid bloating the main JSON payload.

### Verification
- Integration test that the export bundle contains the source-file artifact and manifest entry.

## Step 8 — Add validation and reporting for source-file export coverage
### Deliverables
- Add exporter diagnostics/reporting for:
  - number of referenced paths collected
  - number of files exported
  - number skipped or rejected
- Decide whether skipped files are warnings or hard failures.

### Implementation notes
Recommended first version: skip invalid/unreadable files with warnings, but do not fail the whole snapshot unless a stricter mode is explicitly required.

### Verification
- Tests for warning/report behavior when one referenced file is missing or rejected.

## Step 9 — Add end-to-end tests for snapshot source-file export
### Deliverables
- Add end-to-end tests that index a sample project and verify:
  - exported `sourceFiles` exist
  - content matches the real file
  - duplicate references do not duplicate files in the artifact

### Verification
- End-to-end test coverage for Java and TS/TSX sample inputs.

## Step 10 — Document the platform import expectations
### Deliverables
- Add docs explaining:
  - artifact shape
  - expected platform import behavior
  - file-size/binary rules
  - simple per-snapshot storage policy

### Verification
- Documentation placed in `docs/` and aligned with platform plan.

## Recommended implementation order
1. Define contract
2. Add model/serialization
3. Collect unique referenced paths
4. Reuse safe text-file validation
5. Add language detection
6. Build export artifact
7. Include in bundle/manifest
8. Add reporting
9. Add end-to-end tests
10. Document platform expectations

## Expected end state
After this plan is complete, each successful index/export includes a referenced-source-file artifact containing the full text of every text file referenced by the exported snapshot, once per snapshot export.

## Suggested verification commands
Run what matches your local setup:

```bash
mvn test
```

```bash
mvn -Dtest='*Source*','*Export*','*Snapshot*' test
```

```bash
mvn package
```
