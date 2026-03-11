# HTTP worker service

This step adds a thin HTTP worker wrapper around the existing worker-mode pipeline.

## Endpoints

- `GET /health`
- `POST /api/index-jobs/run`

## Request shape

`POST /api/index-jobs/run` accepts the same core fields as `WorkerJobRequest`:

- `jobId`
- `repositoryId`
- `sourcePath` or `gitUrl`
- `gitRef`
- `outputPath` (optional for HTTP; auto-generated if omitted)
- `snapshotIn`
- `snapshotOut`

## Response shape

The HTTP worker returns:

- job status/timestamps/output path
- the generated IR document inline
- manifest preview data when the sibling manifest file exists
- execution summary from worker mode

## Runtime notes

- The HTTP worker is intentionally thin and delegates to the existing `WorkerModeService`.
- CLI mode and file-based worker mode remain supported.
- The Docker image now starts in HTTP worker mode by default.
