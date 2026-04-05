# Source-view contract end-to-end tests

This step adds end-to-end regression coverage for the retained-source source-view contract through the worker-facing flow.

## Covered flows

1. **Local-path run -> sourceAccess -> source read**
   - Run endpoint returns a stable `sourceAccess` payload.
   - The returned `sourceHandle` can be used to read a repository-relative source file.
   - The retained local root remains the original local source tree.
   - Successful reads update `lastAccessedAt` in the handle registry.

2. **Git run -> retained checkout -> source read**
   - Temporary Git workspaces are retained under the worker retention area.
   - The run endpoint returns a retained-checkout `sourceAccess` payload.
   - The returned `sourceHandle` can be used to read a file from the retained checkout.
   - The original temporary Git workspace is no longer required after retention.

3. **Invalid path rejection through a real run-issued handle**
   - A handle returned from the run endpoint still enforces repository-relative path rules.
   - Parent traversal is rejected during source-file reads.

## Test seam

The tests use a worker-mode test double that still runs through the real `WorkerModeService.runJob(...)` and `HttpWorkerService.readSourceFile(...)` flow. The double only replaces the actual indexing execution and writes a deterministic architecture-index output file and manifest so the contract can be exercised without running the full parser pipeline.

This keeps the tests focused on the source-view contract rather than on parsing or IR assembly.
