# JavaSyntaxTreeExtractionStage final seam map

This document describes where Java extraction responsibilities now live after the Priority 1 refactor.

## Orchestration boundary

`JavaSyntaxTreeExtractionStage` is the registered Java extraction stage. Its role is to validate inputs, wire collaborators, and delegate compilation-unit extraction. It should remain small and coordination-focused.

## Traversal and dispatch

- `JavaCompilationUnitExtractionFlow` prepares compilation-unit level entities/scopes/import handling and starts traversal.
- `JavaSyntaxTreeTraversal` performs recursive syntax-tree walking.
- `JavaTraversalNodeDispatchFlow` decides how visited nodes are handed off based on current owner context.

## Type handling

- `JavaTypeDeclarationFlow` owns type-declaration recognition, type entity creation, type containment, and type-level dependency/semantic handling.

## Member handling

- `JavaFieldExtractionFlow` owns field entity creation, containment, structural field dependency emission, and delegates JPA field semantics.
- `JavaMethodExtractionFlow` owns method entity creation, containment, structural method dependency emission, and delegates method semantics.

## Dependency handling

- `JavaDependencyEmissionFlow` owns explicit dependency emission flows for types, fields, returns, and parameters.
- `JavaRelationshipEvidenceEmitter` remains the lower-level dependency/evidence engine.

## Semantics

Method semantics are delegated through `JavaMethodSemanticsFlow` and then split by concern:

- `JavaJaxRsMethodSemantics`
- `JavaJpaMethodSemantics`
- `JavaCdiMethodSemantics`
- `JavaWritePathMethodSemantics`

Field semantics are delegated through:

- `JavaJpaFieldSemantics`

## Shared supports

Shared Java extraction details live in:

- `JavaSourceReferenceSupport`
- `JavaDeclaredTypeSupport`
- `JavaOwnershipSupport`

These classes should contain reusable stable logic only.

## Request / result contracts

Explicit contracts used across flows:

- `JavaOwnerContext`
- `JavaTypeNodeRequest`
- `JavaMemberNodeRequest`
- `JavaTypeTraversalResult`

These contracts are intended to reduce implicit owner/state mutation and keep orchestration logic readable.

## Maintenance rule of thumb

When adding or adjusting Java extraction behavior:

- do not add new detailed semantics directly into `JavaSyntaxTreeExtractionStage`
- place behavior in the narrowest flow/helper that owns that concern
- add or update seam tests before broadening orchestration logic
