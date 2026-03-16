# Refactoring Step 9 — Clean up support/helper and interpretation classes

## Goal
Reduce complexity in the interpretation layer by separating classification heuristics from rule orchestration, while keeping interpretation behavior and external registry wiring stable.

## What changed

### Java backend interpretation cleanup
- introduced `JavaBackendRoleClassifier`
  - owns Java backend role classification heuristics for services/resources/persistence adapters
  - centralizes framework detection and owner/dependency-based role signals
- introduced `JavaBackendInterpretationClassification`
  - small internal result model for rule classification output
- introduced `JavaEndpointInterpreterSupport`
  - owns endpoint annotation detection, HTTP method mapping, and path normalization
- reduced `JavaBackendInterpretationRule`
  - now focuses on orchestration of interpreted entities and relationships
  - delegates role classification and endpoint/path logic to focused helpers

### TypeScript/frontend interpretation cleanup
- introduced `TypeScriptFrontendClassifier`
  - owns UI-module, service, and startup-point heuristics for TypeScript/Angular/React entities
- reduced `TypeScriptFrontendInterpretationRule`
  - now delegates classification to the helper and focuses on emitting interpreted facts

### Targeted safety-net coverage
- added `InterpretationHelperClassificationTest`
  - protects Java backend helper classification for service/repository metadata
  - protects TypeScript frontend helper classification for UI/service/startup profiles

## Behavioral intent
This step is intended as a structural cleanup only:
- keep `InterpretationRegistry.defaultRegistry()` stable
- keep interpreted role kinds stable
- keep metadata such as `backendProfile`, `frameworks`, `uiProfile`, and `serviceProfile` stable
- keep endpoint path/method normalization stable

## Follow-up opportunities
- extract common metadata-merging helpers currently duplicated across extract/interpret support classes
- consider introducing a small interpretation-result builder to further reduce duplication when emitting role entities and relationships
- if the Java or TypeScript interpretation rules continue growing, split their collaborators into backend/frontend subpackages
