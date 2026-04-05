# Source read HTTP endpoint

This step adds the worker HTTP endpoint for on-demand source retrieval.

## Endpoint

- `POST /api/source-files/read`

## Request

```json
{
  "sourceHandle": "src_...",
  "path": "src/main/java/example/App.java",
  "startLine": 12,
  "endLine": 34
}
```

## Response

```json
{
  "sourceHandle": "src_...",
  "path": "src/main/java/example/App.java",
  "language": null,
  "totalLineCount": 120,
  "fileSizeBytes": 4096,
  "requestedStartLine": 12,
  "requestedEndLine": 34,
  "sourceText": "..."
}
```

Notes:
- The endpoint currently returns the full file text.
- `language` remains `null` until the next step adds explicit detection.
- `startLine` and `endLine` are echoed back for viewer convenience but are not yet used to trim the returned text.
- File access still goes through retained-source validation from Step 4.
