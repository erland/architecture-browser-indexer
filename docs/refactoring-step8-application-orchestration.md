# Refactoring Step 8 — Introduce application orchestration below CLI

## Goal
Move end-to-end indexing orchestration out of `IndexerCli` so the pipeline has a reusable application-level entrypoint that can be used by CLI and worker execution modes.

## What changed

### New application-layer types
- `IndexRunRequest`
  - immutable request model for one indexing run
- `IndexRunResult`
  - immutable result model containing the generated document, output path, summary payload, and temporary-workspace metadata
- `IndexerApplicationService`
  - reusable orchestration service for acquisition, scan, parse, extract, interpret, topology, IR assembly, validation, JSON writing, export bundle writing, and snapshot handling

### `IndexerCli` after the split
`IndexerCli` now mainly handles:
1. argument parsing
2. mode selection (`--help`, `--version`, HTTP worker, worker request mode, normal CLI mode)
3. validation of required normal-mode arguments
4. delegation to `IndexerApplicationService`
5. summary printing and best-effort temp workspace cleanup

### `WorkerModeService` after the split
`WorkerModeService` no longer invokes `IndexerCli.main(...)` to execute a job. It now:
1. maps worker request fields into `IndexRunRequest`
2. delegates to `IndexerApplicationService`
3. preserves worker result handling/logging
4. performs the same best-effort temp workspace cleanup after successful runs

## Behavioral intent
This step is intended to be a structural refactor only:
- keep CLI arguments and behavior stable
- keep worker request/result handling stable
- keep output JSON/export generation stable
- keep incremental snapshot handling stable
- keep summary payload generation stable

## Follow-up opportunities
- add focused tests directly for `IndexerApplicationService`
- introduce an application-level summary builder to further shrink `IndexerApplicationService`
- consider moving temp workspace cleanup into a dedicated lifecycle helper shared by CLI and worker modes
