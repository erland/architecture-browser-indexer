# Container deployment

This repository is intended to be hosted at:

- GitHub owner: `erland`
- repository: `architecture-browser-indexer`

Suggested container image name:

- `ghcr.io/erland/architecture-browser-indexer`

## Build locally

```bash
docker build -t architecture-browser-indexer:local .
```

## Run CLI mode in container

```bash
docker run --rm \
  -v "$PWD":/workspace \
  -v "$PWD/lib":/app/lib \
  architecture-browser-indexer:local \
  --source /workspace \
  --output /workspace/output/index.json
```

## Run worker mode in container

Create `jobs/request.json`:

```json
{
  "jobId": "job-001",
  "repositoryId": "architecture-browser-indexer",
  "sourcePath": "/workspace",
  "outputPath": "/workspace/output/index.json",
  "snapshotOut": "/workspace/output/snapshot.json"
}
```

Then run:

```bash
docker compose -f docker-compose.worker.yml up --build
```

Worker mode reads:

- `/jobs/request.json`

and writes:

- `/jobs/result.json`

## GitHub Container Registry

For GitHub-hosted publishing, use an image like:

- `ghcr.io/erland/architecture-browser-indexer:<tag>`

A typical future CI flow would:
- build the jar
- build the image
- push to GHCR
- optionally publish release artifacts
