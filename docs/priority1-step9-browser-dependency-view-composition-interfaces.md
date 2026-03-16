# Priority 1 Step 9 — Reduce browser/dependency-view assembly handoff to explicit composition interfaces

## What changed

This step introduces explicit package-level handoff objects around dependency-view and browser-view assembly so the remaining IR composition flow is less dependent on long argument lists and loosely coordinated `Map` handoffs.

### New internal handoff records

- `ArchitectureIrDependencyViewAssemblyInputs`
- `ArchitectureIrBrowserViewCompositionInputs`
- `ArchitectureIrBrowserViewComposition`

## Intent

Before this step, the dependency-view flow handed many intermediate lists directly into browser-view shaping helpers. That worked, but it made the composition boundary implicit and harder to evolve safely.

After this step:

- `ArchitectureIrAssemblyStateBuilder` passes an explicit dependency-view assembly input object
- `ArchitectureIrAssemblyCompositionSupport` remains the orchestration point for dependency-view construction
- `ArchitectureIrBrowserViewMetadataBuilder` now accepts an explicit browser-view composition input object and returns a structured composition result
- browser-view insertion into the final dependency-view metadata map happens through one explicit composition handoff instead of several ad hoc calls

## Why this helps

- reduces long parameter-list handoffs between IR composition helpers
- makes browser-view shaping easier to test in isolation
- creates a clearer seam for the final shrink pass of `ArchitectureIrAssemblyCompositionSupport`
- keeps the external document contract unchanged while making the internal assembly flow more explicit

## Expected verification

Run the existing IR and frontend regression suites, especially:

```bash
mvn test -Dtest=ArchitectureIrAssemblyCompositionSupportSafetyNetTest
mvn test -Dtest=ArchitectureIrSeamHardeningTest
mvn test -Dtest=FrontendArchitectureEndToEndFixtureRegressionTest
mvn test -Dtest=TypeScriptArchitectureFixtureRegressionTest
```

## Continuation

The next step can now focus on making the remaining internal god classes orchestration-first and documenting the resulting seams, without needing to untangle browser/dependency-view argument plumbing at the same time.
