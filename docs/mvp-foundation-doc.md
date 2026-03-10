# Architecture Browser MVP Foundation

## 1. Purpose

This document defines the minimum shared foundation needed to start implementation of the Architecture Browser in a new chat without relying on hidden context.

It is intended to remove the largest early uncertainties by defining:

- Phase 1 MVP scope
- repository split and responsibilities
- initial supported language/framework matrix
- initial architecture IR skeleton
- platform/indexer handoff contract
- fixture and sample-repository strategy
- first implementation slice
- key assumptions and constraints

This document is intentionally narrower than the functional specifications and development plans. It is meant to anchor implementation, not replace detailed planning.

---

## 2. Product Goal

Build a self-hosted tool that helps architects browse source repositories and quickly understand:

- how the codebase is laid out
- what major modules/components exist
- where entry points are
- where persistence and integrations happen
- how major dependencies flow
- which files and symbols support those conclusions

The MVP should optimize for **architecture comprehension**, not live editing, not perfect semantic navigation, and not LLM-based analysis.

---

## 3. Repository Split

The implementation should start with **two Git repositories**.

## 3.1 architecture-browser-platform

Owns:

- installer and deployment packaging
- backend API
- web UI
- workspace management
- repository registration
- index-run tracking
- snapshot persistence
- architecture browsing views
- search over imported results
- overlays, notes, and saved views later in MVP
- audit/ops/admin basics

Does not own:

- source parsing
- Tree-sitter extraction logic
- framework interpretation rules
- IR generation from raw source

## 3.2 architecture-browser-indexer

Owns:

- source acquisition
- file scanning and classification
- Tree-sitter-based extraction
- framework-aware interpretation
- normalized architecture IR generation
- diagnostics
- completeness metadata
- result publication/export

Does not own:

- user-facing browsing
- workspace management
- snapshot UI
- overlays
- saved views
- platform persistence of imported results

## 3.3 Why this split

This split is recommended because it:

- keeps user-facing concerns separate from deterministic code-intelligence logic
- allows the indexer to be tested fixture-first
- prevents the platform from becoming tightly coupled to parser internals
- still keeps the architecture small enough for MVP delivery

---

## 4. Phase 1 MVP Scope

The MVP must stay deliberately small.

## 4.1 In-scope capabilities

### Platform
- create and manage workspaces
- register repositories
- request indexing
- track index-run status
- import snapshots from the indexer
- browse snapshot overview
- browse repository/module layout
- browse dependency view
- browse entry-point view
- browse data/integration view
- search entities
- open entity detail with source-context references
- basic Docker Compose installation

### Indexer
- acquire local path or Git repository input
- scan files and detect technologies
- extract structural facts from supported technologies
- interpret selected framework markers
- emit versioned architecture IR
- emit diagnostics and completeness metadata
- support partial results
- run via CLI and worker mode

## 4.2 Explicitly out of scope for Phase 1

- LLM/chat features
- code modification
- IDE-like editing
- full semantic language-server integration
- advanced diff/comparison
- advanced authentication/SSO
- multi-tenant scale concerns
- high-availability deployment
- plugin marketplace
- broad language coverage
- advanced incremental indexing optimizations
- perfect framework inference
- rich diagram editing

## 4.3 User experience target

An architect should be able to:

1. install the platform with a documented local/on-prem path
2. connect a repository
3. trigger analysis
4. open a browser UI
5. get a useful architecture-oriented overview within minutes

---

## 5. Initial Supported Technology Matrix

The MVP should support only a small initial matrix.

## 5.1 Priority 1 technologies

### Java
Support detection/extraction for:
- packages
- classes
- interfaces
- methods
- imports
- annotations

Initial framework-aware interpretation:
- Spring-style REST controllers
- Spring-style service/repository markers
- Quarkus/JAX-RS style resource markers where straightforward
- startup/bootstrap markers where straightforward

### TypeScript / JavaScript
Support detection/extraction for:
- modules
- functions
- classes
- imports/exports
- decorators where present

Initial framework-aware interpretation:
- React component/module structure at high level
- Node/Express-style route markers where straightforward
- client/service grouping where straightforward

### SQL
Support:
- basic schema artifact detection
- table/view names where straightforward
- file-level SQL classification

### YAML / JSON / properties
Support:
- configuration file classification
- extraction of selected architecture-relevant markers
- capture of config-derived references where feasible

## 5.2 Deferred technologies

Defer until after MVP proof:
- C#
- Go
- Python
- Kotlin
- BPMN/UML/XMI-like artifacts
- Docker/Kubernetes deep interpretation
- message brokers beyond basic config markers
- advanced frontend framework support beyond broad React/Node patterns

---

## 6. MVP Architecture Principles

Implementation should follow these principles from the start.

## 6.1 Deterministic first
The indexer must produce deterministic results for the same source input and version.

## 6.2 Architecture-oriented, not parser-oriented
The platform UI must present architecture concepts, not ASTs or raw parser facts.

## 6.3 Version everything important
Version:
- IR schema
- indexer version
- import contract
- snapshot metadata

## 6.4 Preserve traceability
Every higher-level inferred concept should point back to supporting source files/facts where possible.

## 6.5 Separate facts from user annotations
Imported facts are immutable per snapshot. User notes/tags/overlays must be stored separately.

## 6.6 Prefer precomputed browsing
Heavy work belongs in indexing/import. The browsing UI should read prepared data, not run heavy live analysis.

## 6.7 Keep MVP install simple
The first-class installation path should be Docker Compose.

---

## 7. Initial Architecture IR Skeleton

The IR must be defined **before implementation starts**, but only as an MVP skeleton.

It should be:
- versioned
- small
- extensible
- stable enough for both repos to implement against

## 7.1 Top-level structure

Recommended initial top-level structure:

```json
{
  "schemaVersion": "1.0",
  "indexerVersion": "0.1.0",
  "runMetadata": {},
  "sourceUnits": [],
  "scopes": [],
  "entities": [],
  "relationships": [],
  "sourceRefs": [],
  "diagnostics": [],
  "completeness": {}
}
```

## 7.2 Top-level fields

### schemaVersion
Version of the IR schema.

### indexerVersion
Version of the indexer that produced the payload.

### runMetadata
Metadata about this indexing run.

Minimum fields:
- runId
- startedAt
- completedAt
- outcome
- mode (full/partial/incremental if relevant later)

### sourceUnits
Describes the sources analyzed.

Minimum fields per source unit:
- id
- repositoryName
- sourceType
- sourceLocation
- branch
- revision
- detectedTechnologies

### scopes
Logical containers in the architecture hierarchy.

Purpose:
Represent nested browseable boundaries such as repository, module, package, folder, component-group.

Minimum fields:
- id
- kind
- name
- parentScopeId
- sourceUnitId
- path
- metadata

Initial allowed scope kinds:
- repository
- module
- package
- folder
- logical-group

### entities
Architecture-relevant nodes.

Minimum fields:
- id
- kind
- name
- scopeId
- displayName
- description
- metadata
- sourceRefIds
- tags

Initial allowed entity kinds:
- component
- endpoint
- datastore
- message-channel
- external-system
- startup-point
- config-artifact
- ui-module
- service
- persistence-adapter

Notes:
- keep the initial taxonomy small
- allow `metadata` for framework-specific or language-specific details
- do not encode every low-level symbol as a first-class architecture entity unless needed

### relationships
Connections between scopes/entities.

Minimum fields:
- id
- kind
- fromId
- toId
- direction
- metadata
- sourceRefIds

Initial allowed relationship kinds:
- contains
- depends-on
- exposes
- calls
- reads-from
- writes-to
- publishes-to
- consumes-from
- configures
- implements

### sourceRefs
Traceability references into source.

Minimum fields:
- id
- sourceUnitId
- filePath
- symbolName
- lineStart
- lineEnd
- columnStart
- columnEnd
- excerptLabel
- metadata

For MVP, line/column may be optional if unavailable.

### diagnostics
Warnings/errors/information from indexing.

Minimum fields:
- id
- level
- code
- message
- sourceUnitId
- filePath
- relatedScopeId
- relatedEntityId
- metadata

Allowed levels:
- info
- warning
- error

### completeness
Indicates whether the run is complete or partial.

Minimum fields:
- status
- analyzedFileCount
- skippedFileCount
- failedFileCount
- notes

Allowed status:
- complete
- partial
- failed

## 7.3 IR rules

### Rule 1: stable IDs within a payload
IDs must be unique within a payload.

### Rule 2: human-meaningful names
Entities/scopes should include display names useful in the UI.

### Rule 3: metadata is extensibility, not a dumping ground
Only use metadata for structured additional fields that are not yet in the core schema.

### Rule 4: preserve traceability
Inferred entities and relationships should reference supporting sourceRefs where possible.

### Rule 5: do not overfit the IR to one framework
The initial schema should work across Java and TypeScript without being overly framework-specific.

---

## 8. Initial Entity Interpretation Guidance

The first interpretation rules should be conservative and high-value.

## 8.1 Good first inferred concepts

Infer these early:
- endpoint
- service
- persistence-adapter
- ui-module
- startup-point
- datastore
- external-system
- config-artifact

## 8.2 Concepts to avoid over-inferring in MVP

Avoid trying to perfectly infer:
- bounded contexts
- domain aggregates
- business capabilities
- exact runtime call chains
- precise data lineage
- perfect ownership/team mapping
- perfect module boundaries when code structure is weak

Those can be layered later.

---

## 9. Platform / Indexer Handoff Contract

This must be explicit from the start.

## 9.1 MVP handoff approach

Recommended MVP approach:

- the platform creates an index run request
- the indexer executes analysis externally
- the indexer produces a versioned IR payload
- the platform imports that payload as a snapshot

For MVP, the simplest integration choices are:

### Preferred first integration
**File-based or HTTP POST-based handoff**

Choose one of these:

#### Option A — HTTP POST
Indexer posts result payload to a platform import endpoint.

Pros:
- simple integration story
- easy to evolve toward worker deployment
- closer to eventual production shape

#### Option B — file handoff
Indexer writes a JSON payload file that platform imports.

Pros:
- easiest for local debugging
- easy fixture reuse
- good for initial end-to-end development

## 9.2 Recommendation
Use:
- **file-based handoff in earliest development**
- then move quickly to **HTTP POST import** once both sides are stable enough

## 9.3 Import contract expectations

Platform import contract should require:
- schemaVersion
- indexerVersion
- runMetadata
- sourceUnits
- scopes
- entities
- relationships
- diagnostics
- completeness

Import must:
- reject malformed payloads
- preserve previous successful snapshots
- allow partial snapshots if completeness says partial
- store import diagnostics separately from user annotations

---

## 10. Fixture and Sample Repository Strategy

The project should be driven by representative examples from day one.

## 10.1 Why fixtures matter

Fixtures are needed to:
- stabilize the IR
- keep interpretation rules honest
- avoid overdesign
- give deterministic regression coverage
- let platform and indexer evolve together

## 10.2 Minimum fixture set

Create at least these four fixture classes:

### Fixture A — small Java backend
Characteristics:
- REST controller/resource
- service class
- repository/persistence class
- config file
- startup entry point

Purpose:
Validate backend extraction and architecture mapping.

### Fixture B — small TypeScript frontend
Characteristics:
- React components
- service or API client module
- state/store or module grouping
- config file

Purpose:
Validate frontend layout, UI module grouping, and client/service mapping.

### Fixture C — small Node or TS backend
Characteristics:
- route declarations
- service module
- config file
- outbound integration example

Purpose:
Validate non-Java service interpretation.

### Fixture D — mixed config-heavy repository
Characteristics:
- YAML
- JSON
- properties
- SQL files
- limited code

Purpose:
Validate config and non-code signal capture.

## 10.3 Optional real sample repositories

If available, also define 1–2 real internal or synthetic repositories representative of expected users.

Requirements:
- small enough for repeated test runs
- legally and practically safe to store/use
- structurally representative of target architecture styles

---

## 11. First Implementation Slice

Implementation should begin with one narrow end-to-end slice.

## 11.1 Recommended first slice

### Indexer
- source acquisition for local path
- file inventory
- Java + TS minimal extraction
- initial IR generation
- JSON export

### Platform
- workspace creation
- repository registration
- snapshot import
- snapshot overview page
- simple entity/scopes list page

## 11.2 Outcome of first slice

At the end of the first slice, it should be possible to:

1. point the indexer at a small sample repo
2. produce a valid IR payload
3. import it into the platform
4. browse overview + scopes/entities in the UI

This should happen **before** building advanced dependency views or search polish.

## 11.3 Why this slice first

It proves:
- repository split is viable
- IR is concrete enough
- import contract works
- browsing model is useful
- core technical risk is reduced early

---

## 12. Recommended Build Order

The platform should not be fully built first.
The indexer should not be fully built first.
They should move in staggered parallel.

## 12.1 Practical order

### Step 1
Create this MVP foundation and accept it as the starting contract.

### Step 2
Start **indexer slightly first**:
- repo skeleton
- IR schema/classes
- fixture scaffolding
- local scan
- minimal extraction

### Step 3
Start **platform shortly after**:
- repo skeleton
- workspace/repository model
- snapshot import contract
- minimal overview UI

### Step 4
Connect them with the first end-to-end slice.

### Step 5
Expand incrementally:
- richer entities
- dependency view
- entry points
- data/integration view
- search
- then overlays/saved views

## 12.2 Why indexer slightly first

The largest early unknowns are:
- what can be extracted reliably
- what the IR naturally wants to contain
- what traceability is available

Those questions should be resolved before too much UI design hardens around assumptions.

---

## 13. Minimal Non-Functional Baseline

The MVP should satisfy these basic qualities:

- deterministic results for the same inputs
- on-prem friendly deployment
- no mandatory cloud dependencies
- one primary documented install path
- acceptable responsiveness for small/medium repositories
- fixture-driven regression testing
- explicit diagnostics for partial/failing analysis

---

## 14. Key Risks and Early Mitigations

## 14.1 Risk: IR churn
Mitigation:
- define a small versioned schema now
- use fixture payloads immediately
- avoid broad entity taxonomy too early

## 14.2 Risk: overbuilding platform before real data exists
Mitigation:
- connect real payload import early
- validate every browse view against real fixtures

## 14.3 Risk: overbuilding indexer sophistication too early
Mitigation:
- prefer conservative inference
- prioritize useful architecture entities over perfect semantic analysis

## 14.4 Risk: too much language/framework scope
Mitigation:
- stay with Java, TypeScript/JavaScript, SQL, and config files for Phase 1

## 14.5 Risk: losing architect focus
Mitigation:
- optimize the UI around comprehension tasks:
  - overview
  - layout
  - dependencies
  - entry points
  - integrations
  - drill-down traceability

---

## 15. Ready-to-Start Checklist for a New Chat

A new implementation chat should treat the following as given:

- two repositories:
  - architecture-browser-platform
  - architecture-browser-indexer
- Docker Compose is the primary MVP install path
- initial supported technologies:
  - Java
  - TypeScript/JavaScript
  - SQL
  - YAML/JSON/properties
- initial IR skeleton defined in this document
- file-based handoff may be used first, moving toward HTTP POST import
- fixtures drive development from the start
- first slice is:
  - minimal indexer extraction
  - minimal platform import
  - minimal overview browse flow

---

## 16. Suggested First Prompt for a New Chat

A good first implementation prompt would be:

“Use the attached `Architecture Browser MVP Foundation` as the baseline contract. Start with the `architecture-browser-indexer` repository. Create a downloadable step-by-step plan for implementing the first end-to-end slice: repository skeleton, IR schema/classes, fixture scaffolding, local-path acquisition, file inventory, minimal Java/TypeScript extraction, and JSON export compatible with the foundation document. Keep the scope minimal and implementation-ready.”

A parallel follow-up prompt could then start the platform repo from the same foundation.

---

## 17. Final Recommendation

Do not wait to design the perfect final model.

Do define the MVP foundation explicitly now, version the IR from day one, start the indexer slightly ahead of the platform, and connect both repos as early as possible with one real end-to-end slice.
