# Platform-facing snapshot source-file export contract

This document defines the platform-facing contract for the indexer snapshot source-file sidecar artifact introduced for snapshot-owned source viewing.

## Purpose

The source-file sidecar artifact allows the platform to import and durably store the subset of repository files that are referenced by exported `sourceRefs` for a given snapshot.

This contract is intentionally separate from the main architecture payload JSON so that:

- the main architecture document remains focused on architecture data
- source-file text can be stored per snapshot without inflating the primary payload structure
- platform import can treat source files as snapshot-owned artifacts with independent persistence and cleanup

## Artifact shape

For each export bundle, the indexer may emit an additional JSON sidecar file named:

- `<payload-file-name>.source-files.json`

Example:

- `architecture-index.json`
- `architecture-index.json.source-files.json`

The manifest metadata describes the presence of this sidecar.

## Manifest metadata

The indexer writes the following manifest metadata entries when the sidecar artifact is emitted:

- `snapshotSourceFilesArtifact.fileName`
- `snapshotSourceFilesArtifact.contentType`
- `snapshotSourceFilesArtifact.sizeBytes`
- `snapshotSourceFilesArtifact.sha256`
- `snapshotSourceFilesArtifact.contractVersion`
- `snapshotSourceFilesArtifact.fileCount`

Additional summary metadata may also be present, including:

- `snapshotSourceReferencedFileCount`
- `skippedReferencedFileCount`
- `skippedReferencedFileDetails`
- `maxReferencedFiles`
- `maxReferencedFileSizeBytes`

The platform should rely on the `snapshotSourceFilesArtifact.*` keys to locate and validate the sidecar artifact. Summary metadata is informative and may be used for diagnostics.

## Sidecar top-level contract

The sidecar JSON uses contract type:

- `snapshot-source-files/v1`

Top-level shape:

```json
{
  "contractType": "snapshot-source-files/v1",
  "files": [
    {
      "relativePath": "src/main/java/com/example/MyService.java",
      "language": "java",
      "contentType": "text/x-java-source",
      "sizeBytes": 1842,
      "totalLineCount": 73,
      "textContent": "package com.example;\n..."
    }
  ],
  "metadata": {
    "referencedRelativePaths": [
      "src/main/java/com/example/MyService.java"
    ],
    "referencedFileCount": 1,
    "readReferencedFileCount": 1,
    "skippedReferencedFiles": [],
    "skippedReferencedFileCount": 0,
    "skippedReferencedFileDetails": [],
    "maxReferencedFiles": 500,
    "maxReferencedFileSizeBytes": 262144
  }
}
```

## File entry contract

Each entry in `files` represents one repository-relative file stored once for the snapshot.

Required fields:

- `relativePath`
- `textContent`

Viewer-oriented metadata fields:

- `language`
- `contentType`
- `sizeBytes`
- `totalLineCount`

### `relativePath`

Rules:

- repository-relative path only
- normalized to `/` separators
- must not be absolute
- must not escape the indexed source root
- duplicate file entries are suppressed by `relativePath`, keeping the first-seen entry

The platform should use `relativePath` as the stable lookup key within a snapshot.

### `language`

A viewer-friendly language identifier. Current examples include:

- `java`
- `javascript`
- `typescript`
- `jsx`
- `tsx`
- `json`
- `yaml`
- `xml`
- `sql`
- `properties`
- `markdown`
- `plaintext`

The platform should treat this as a rendering hint for syntax highlighting.

### `contentType`

A text-oriented MIME-like content classification suitable for diagnostics and debugging. The platform may persist this value but does not need to depend on it for rendering.

### `sizeBytes`

The byte size of `textContent` as exported by the indexer.

### `totalLineCount`

The total line count for the exported file content, used by the platform source viewer for line-number and range behavior.

### `textContent`

The full exported text content for the referenced file.

This is the durable source-view payload the platform should store per snapshot.

## Metadata contract

The `metadata` object provides import diagnostics and operator visibility.

### `referencedRelativePaths`

The normalized unique set of repository-relative file paths derived from exported `sourceRefs` before read/skip filtering.

### `referencedFileCount`

The number of unique referenced paths collected from `sourceRefs`.

### `readReferencedFileCount`

The number of referenced files successfully read and emitted in `files`.

### `skippedReferencedFiles`

A list of relative paths that were skipped during export.

### `skippedReferencedFileDetails`

Structured skip details. Current reason codes may include:

- `invalid_path`
- `path_outside_source_root`
- `missing_file`
- `not_regular_file`
- `binary_or_non_text`
- `file_too_large`
- `minified_asset`
- `source_map`
- `referenced_file_limit_exceeded`
- `read_error`

The platform may persist this metadata for diagnostics, but it should not treat skipped files as fatal to snapshot import.

### Limits

Current export safeguards include:

- maximum referenced files per snapshot
- maximum file size per exported file
- rejection of binary or non-text files
- rejection of invalid and escaping paths
- skipping of selected unwanted web artifacts such as minified assets and source maps

The platform should assume that absence of a referenced file in `files` means the file may have been skipped for one of these reasons.

## Platform import expectations

The platform should:

1. read the sidecar artifact referenced from manifest metadata
2. validate `contractType == snapshot-source-files/v1`
3. store each `files[]` entry once for the imported snapshot, keyed by snapshot + `relativePath`
4. preserve viewer metadata (`language`, `sizeBytes`, `totalLineCount`, `contentType`)
5. store the full `textContent` for later in-app source viewing
6. optionally persist `metadata` for diagnostics and support visibility

The platform should not depend on the indexer worker remaining online or retaining a checkout after this import path is adopted.

## Snapshot lifecycle expectation

The source-file sidecar is designed for snapshot-owned storage.

Expected lifecycle:

- source files are imported together with the snapshot
- source files are served by the platform from snapshot-owned storage
- deleting the snapshot should delete the stored source files belonging to that snapshot

This is the intended durable replacement for retained-worker checkout based source viewing.

## Error-handling expectations

The presence of skipped files does not invalidate the artifact.

Platform import should fail only for structural contract problems such as:

- missing sidecar when manifest claims it exists
- unreadable sidecar artifact
- invalid JSON
- unsupported `contractType`
- malformed required file fields

The platform should not fail import merely because some referenced files were skipped and therefore are absent from `files`.

## Backward compatibility

During transition, some exports may not contain snapshot source-file sidecar artifacts.

Platform behavior should therefore remain tolerant of:

- no `snapshotSourceFilesArtifact.*` metadata
- no sidecar artifact present in older bundles
- older retained-worker source-view flows still existing temporarily

The sidecar-based import should become the preferred durable source-view path going forward.
