# Priority 3 — Step 4: Isolate JPA/CDI/write-path detail helpers behind narrower internal APIs

This partial step introduces dedicated helper/support classes for the low-level detail logic that currently lives inside `JavaStructuralExtractor`:

- `JavaJpaDetailSupport`
- `JavaCdiDetailSupport`
- `JavaWritePathDetailSupport`

It also adds focused helper tests that protect the intended narrow APIs.

## Intended next integration

The next safe change is to wire `JavaStructuralExtractor` to these helpers incrementally:

1. JPA metadata/detail analysis
2. CDI publisher/observer detection + relationship metadata shaping
3. Write-path detection + variable-type inference

That keeps the regex-heavy/detail-heavy code out of the main orchestrator while preserving the current emitted IR contract.
