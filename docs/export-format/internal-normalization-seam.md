# Internal normalization seam

Step 4 introduces an explicit internal normalization seam between interpretation and IR/export assembly.

## Purpose

The goal is to keep language/framework-specific evidence inside the indexer while giving the export layer a stable place to source canonical architectural semantics from.

Recommended flow:

1. extraction produces raw structural/framework evidence
2. interpretation enriches raw entities and relationships
3. normalization maps evidence into canonical architectural roles, traits, and later relationship semantics
4. IR/export serializes the normalized results into the public contract

## Initial implementation shape

The current implementation adds:

- canonical vocabulary enums for:
  - entity roles
  - entity traits
  - relationship semantics
- `ArchitectureEntityNormalizationRule` for isolated mapping rules
- `ArchitectureEntityNormalizationService` as the central aggregation seam
- an IR assembly hook so entities pass through normalization before later composition/enrichment

## Conservative behavior in Step 4

Step 4 does **not** yet introduce Java-first mapping rules. The default normalization service is intentionally conservative and preserves existing behavior until explicit rules are added in later steps.

## Why this seam matters

Without this seam, later Java/TypeScript/SQL/config mappings would likely spread string literals and framework assumptions across interpretation and IR code. Centralizing them here keeps the vocabulary architectural and makes the mapping rules easier to test in isolation.
