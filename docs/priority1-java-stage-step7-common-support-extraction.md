# Priority 1 Java Stage Step 7 — Common support extraction

This step extracts stable support helpers used by multiple Java extraction flows.

## Added support classes

- `JavaSourceReferenceSupport`
- `JavaDeclaredTypeSupport`
- `JavaOwnershipSupport`

## Purpose

These helpers reduce repeated detail logic around:

- source-reference and snippet shaping
- metadata list conversion and parameter-name extraction
- choosing the dependency source entity based on owner context

## Notes

This step is intentionally narrow. It does not change the Java extraction contract.
It creates reusable seams for later cleanup while keeping flow behavior stable.
