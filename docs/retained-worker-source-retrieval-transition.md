# Retained worker source retrieval transition

## Purpose

This note marks the earlier retained-worker source retrieval path as **transitional compatibility behavior** rather than the long-term durable source-view architecture.

The durable direction is now:

- the indexer exports **referenced source files once per snapshot** as a sidecar artifact
- the platform imports and stores those source files as **snapshot-owned data**
- source viewing is then served from platform snapshot storage rather than from a live worker checkout

## Current status

The indexer still contains the earlier retained-worker source retrieval flow, including:

- retained source handles
- retained source roots/checkouts
- worker endpoint `POST /api/source-files/read`
- cleanup and retention logic for retained worker source roots

This behavior remains useful for:

- backward compatibility with the existing platform source-view implementation
- incremental rollout while the platform import/storage changes are implemented
- debugging and transitional validation during migration

## New preferred source-view path

For new platform integration work, the preferred path is:

1. index a source tree
2. export the architecture payload
3. export the `snapshot-source-files/v1` sidecar artifact
4. platform imports both the architecture snapshot and the source-file sidecar
5. platform serves source view from its own snapshot-owned storage

Under this model, later source viewing should not require:

- a retained worker checkout
- a live worker filesystem cache
- re-reading files from the worker after indexing completes

## Why the retained-worker path is no longer the target design

The retained-worker path has several drawbacks as a durable source-view mechanism:

- it depends on worker-local retained files surviving cleanup/restart
- it adds lifecycle coupling between browsing and worker cache retention
- it complicates operational guarantees for source view
- it makes source-view durability depend on the indexer service rather than the imported snapshot

The snapshot-owned sidecar approach solves those issues better because:

- source files travel with the snapshot export
- source-view lifetime matches snapshot lifetime
- snapshot deletion can delete related stored source files cleanly
- platform can serve source view without a later worker callback

## Compatibility policy

Until the platform has fully migrated to snapshot-owned source storage, the retained-worker retrieval path should be treated as:

- supported for compatibility
- acceptable for transitional use
- not the preferred long-term contract for new platform work

## Recommended migration order

1. complete indexer export of `snapshot-source-files/v1`
2. implement platform import and storage of snapshot-owned source files
3. switch platform source-view reads to platform-owned snapshot data
4. keep worker source-read flow available temporarily for rollback/compatibility
5. once no supported platform path depends on it, simplify or remove the retained-worker source retrieval flow

## Eventual removal candidates

Once platform-owned snapshot source-file storage is in place and stable, these areas become candidates for simplification or removal:

- retained source handle registry that exists only for source viewing
- worker source-file read endpoint used only for post-index browsing
- retained checkout cleanup logic that exists only for source viewing
- platform contracts that persist worker `sourceAccess` solely for later source reads

Removal should only happen after:

- platform import of snapshot source files is complete
- source-view UX uses snapshot-owned data by default
- backward compatibility needs have been retired

## Guidance for new changes

New development should:

- prefer snapshot-source-file sidecar export and import
- avoid adding new product dependencies on retained worker source reads
- treat retained-worker source retrieval as a migration bridge

## Summary

The earlier retained-worker source retrieval path remains available for compatibility, but it is now a transitional implementation.

The long-term durable source-view design is:

- **indexer exports referenced source files per snapshot**
- **platform stores them per snapshot**
- **source view is served from platform snapshot storage**
