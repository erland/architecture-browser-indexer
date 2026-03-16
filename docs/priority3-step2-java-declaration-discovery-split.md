# Priority 3 — Step 2: Split Java declaration discovery out of `JavaStructuralExtractor`

## What changed

This step extracts the declared-type discovery pass into its own package-private helper so `JavaStructuralExtractor` no longer owns both:

1. the declaration discovery pre-pass, and
2. the later entity/relationship extraction traversal.

## New structure

- `JavaDeclarationDiscovery` now owns the recursive declared-type discovery pass
- `JavaDeclaredType` is now a dedicated package-private model used across the Java extraction seam
- `JavaStructuralExtractor` delegates to `JavaDeclarationDiscovery.discoverDeclaredTypes(...)`

## Why this helps

This creates a cleaner seam for later Priority 3 work:

- declaration discovery can evolve independently from entity/relationship extraction
- later steps can split declaration mapping from semantic enrichment without reopening the whole extractor
- the declared-type model is no longer trapped as a private nested record inside `JavaStructuralExtractor`

## Safety net

- added `JavaDeclarationDiscoveryTest`
- existing Java extractor seam safety nets remain the higher-level regression guard

## Continuation guidance

The next useful split is to separate Java declaration/entity mapping from the later dependency and semantic relationship emission pass, so `JavaStructuralExtractor` becomes a much thinner orchestrator.
