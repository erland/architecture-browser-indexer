# Architecture Browser Platform — Functional Specification

## 1. Purpose

The Architecture Browser Platform is a self-hosted product that allows architects and technical stakeholders to connect source-code repositories, trigger analysis, and browse the resulting system structure through architecture-oriented views.

The platform does not itself parse source code. It manages workspaces, repositories, indexing runs, snapshots, browsing, saved views, overlays, search, administration, and operational concerns. It consumes normalized analysis results produced by the indexer repository.

## 2. Scope

### In scope
- Workspace creation and management
- Repository registration and connection metadata
- Indexing run orchestration
- Snapshot catalog and browsing
- Architecture-oriented views over indexed results
- Search and filtering across normalized entities
- Saved views and bookmarks
- Manual overlays and tags
- Basic access control and auditability
- Installer and operational administration

### Out of scope
- Source parsing implementation
- Tree-sitter grammar development
- Semantic extraction algorithms
- Live IDE-style editing or code authoring
- LLM-generated summaries or chat assistance in the MVP
- Code modification or repository writeback

## 3. Actors

### Architect
Uses the platform to understand codebase layout, dependencies, entry points, persistence, integrations, and high-level structure.

### Technical reviewer
Uses the platform to inspect architecture facts, navigate drill-down views, and validate assumptions.

### Administrator
Installs, configures, monitors, and maintains the platform.

### Indexing worker/system
Receives indexing requests, processes repositories externally via the indexer, and publishes normalized results back to the platform.

## 4. Core Concepts

### Workspace
A logical container for repositories, snapshots, saved views, and overlays.

### Repository registration
A configured source location associated with a workspace, including source type, branch selection, refresh settings, and credentials references.

### Index run
A single execution request to analyze one or more repositories and produce a normalized result.

### Snapshot
An immutable, browseable result set representing the architecture state of one repository or workspace at a given point in time.

### Architecture entity
A normalized domain object exposed to users, such as:
- repository
- module
- package
- component
- endpoint
- datastore
- message channel
- external system
- dependency
- runtime entry point

### Overlay
User- or system-defined metadata applied to entities, such as ownership, lifecycle, domain tags, review status, risk, or manual notes.

### Saved view
A user-defined, named presentation of filters, layout choices, and selected scope.

## 5. User Goals

The platform must support these goals:

1. Connect one or more repositories to a workspace.
2. Trigger or schedule analysis.
3. View a high-level system map without reading raw code first.
4. Identify major modules and structural boundaries.
5. Explore dependencies between modules and subsystems.
6. Discover APIs, persistence touchpoints, and external integrations.
7. Drill down from architecture concepts to supporting files and symbols.
8. Save useful filtered views for later reuse.
9. Annotate findings with overlays or notes.
10. Compare snapshots at a high level.

## 6. Functional Capabilities

## 6.1 Workspace Management

The platform shall allow a user to:
- create a workspace
- rename a workspace
- archive a workspace
- browse available workspaces
- see the repositories, snapshots, and saved views within a workspace

The platform shall prevent deletion or archival from silently discarding indexed data without explicit user action.

## 6.2 Repository Registration

The platform shall allow a user to register repositories by:
- local path reference
- Git URL
- other supported source connectors added later

For each repository registration, the platform shall support:
- display name
- source location
- selected branch or revision target
- optional refresh policy
- optional technology hints
- credentials reference where required

The platform shall validate that required connection information is present before allowing indexing.

## 6.3 Indexing Orchestration

The platform shall allow a user to:
- request an index run manually
- re-run indexing for an existing repository
- request a workspace-wide refresh
- observe current run status
- view recent completed and failed runs

The platform shall present run states such as:
- queued
- running
- succeeded
- partially succeeded
- failed
- cancelled

The platform shall store run diagnostics sufficient for administrators and users to understand failures at a high level.

## 6.4 Snapshot Management

The platform shall create immutable snapshots from completed index runs.

The platform shall allow a user to:
- browse snapshots
- open a snapshot for exploration
- see snapshot metadata such as time, repositories included, source revisions, and run outcome
- mark a snapshot as baseline or favorite
- compare one snapshot to another at summary level

The platform shall preserve older snapshots until deleted by explicit retention policy or administrator action.

## 6.5 Architecture Overview View

The platform shall provide a system overview for a snapshot showing:
- included repositories
- major modules/components
- major dependencies
- notable entry points
- notable persistence and integration elements

The overview shall be optimized for fast understanding rather than completeness.

## 6.6 Repository and Module Layout View

The platform shall provide a browseable structural view showing:
- repositories
- directories/packages/modules
- classified areas where available, such as API/domain/infrastructure/UI/tests/config
- roll-up counts and indicators

The platform shall allow expansion and drill-down from higher-level structures to lower-level structures.

## 6.7 Dependency View

The platform shall provide a dependency-oriented view for a snapshot.

A user shall be able to:
- see dependencies between modules/components
- filter by dependency type where available
- focus on a selected scope
- inspect inbound and outbound relationships

The platform shall support both summarized and drill-down dependency inspection.

## 6.8 Entry Point View

The platform shall provide a view of runtime entry points, including where detected:
- HTTP endpoints
- message consumers
- scheduled jobs
- command-line entry points
- application startup points

The platform shall allow the user to navigate from an entry point to its owning component and supporting files.

## 6.9 Data and Integration View

The platform shall provide a view of:
- databases and persistence touchpoints
- queues/topics/channels
- outbound clients/integrations
- external systems where identified

The user shall be able to filter this view by repository, module, or integration type.

## 6.10 Search

The platform shall provide search over normalized entities and related metadata.

A user shall be able to search by:
- entity name
- repository
- module
- tag/overlay
- type
- selected metadata fields

Search results shall support direct navigation to the relevant detail view.

## 6.11 Entity Detail View

For any architecture entity, the platform shall provide a detail view showing:
- type
- name
- parent scope
- relationships
- selected metadata
- associated files/symbols where available
- overlays/notes
- diagnostic warnings if relevant

The detail view shall make clear which information is inferred and which is directly observed when such distinction is available.

## 6.12 Drill-down to Source Context

The platform shall support drill-down from an architecture entity to supporting source context.

This may include:
- file paths
- symbol names
- line references if available
- extracted framework markers
- associated configuration references

The MVP does not require full in-browser code editing.

## 6.13 Saved Views

The platform shall allow users to save named views containing:
- selected snapshot
- filters
- layout preferences
- focal entities
- hidden/shown categories

Users shall be able to reopen, rename, duplicate, and delete saved views.

## 6.14 Overlays and Tags

The platform shall allow users to define and apply overlays such as:
- owner/team
- domain
- lifecycle state
- risk
- review status
- custom free-text notes

The platform shall support bulk application where feasible.

Overlay changes shall not modify the underlying snapshot facts.

## 6.15 Snapshot Comparison

The platform shall provide a high-level comparison between two snapshots, including:
- added or removed modules/components
- major dependency changes
- added or removed entry points
- notable integration or persistence changes

Detailed semantic diffing is not required in the MVP.

## 6.16 Notifications and Status Feedback

The platform shall inform users when:
- indexing starts
- indexing completes
- indexing partially succeeds
- indexing fails
- snapshot import succeeds or fails

The platform shall present actionable failure summaries where possible.

## 6.17 Administration

The platform shall provide administrator capabilities for:
- workspace oversight
- repository connection oversight
- index run history
- retention policy configuration
- user access configuration
- health/status visibility

## 6.18 Auditability

The platform shall record auditable events for key user and system actions such as:
- workspace creation
- repository registration changes
- index run requests
- overlay changes
- saved view changes
- administrative configuration changes

## 7. Behavior Rules

### 7.1 Immutable snapshots
Once a snapshot is created, its architecture facts shall not be modified in place.

### 7.2 User overlays are separate from indexed facts
Overlay and note changes shall be stored separately from imported facts.

### 7.3 Partial results
If an index run produces partial but usable output, the platform may create a partial snapshot if the run metadata indicates partial completeness.

The platform shall clearly identify such snapshots as partial.

### 7.4 Access control
Users shall only see workspaces and related data they are authorized to access.

### 7.5 Failed runs
Failed runs shall not replace the last successful snapshot automatically.

### 7.6 Deletion safety
Deletion of repositories, snapshots, or workspaces shall require explicit confirmation and shall communicate data impact.

## 8. Inputs and Outputs

### Inputs
- workspace metadata
- repository registration information
- indexing requests
- imported normalized analysis results
- user-defined overlays, notes, and saved views
- administrator settings

### Outputs
- workspace and repository listings
- run status and diagnostics
- snapshot catalogs
- architecture views
- entity detail pages
- saved views
- audit records
- exportable summary data where supported later

## 9. Validation and Error Handling

The platform shall validate:
- required repository connection information
- snapshot import structure
- uniqueness constraints where needed
- user permissions for restricted actions

When errors occur, the platform shall:
- preserve existing successful snapshots
- provide readable error summaries
- retain detailed diagnostics for administration
- avoid corrupting current browseable state

## 10. Edge Cases

The platform shall handle:
- empty repositories
- repositories with unsupported files only
- mixed-language repositories
- very large repositories by progressive loading or summarized views
- partially classified structures
- duplicate names across scopes
- incomplete imports from the indexer
- repositories that disappear or become inaccessible after registration

## 11. Non-Functional Considerations

The system should:
- be easy to install for a non-developer architect using packaged deployment
- feel responsive in normal browse flows
- support incremental refresh rather than full rebuild for every action where possible
- separate user-facing browsing from heavy indexing work
- preserve deterministic imported facts
- support on-premises deployment
- avoid requiring external cloud services in the MVP

## 12. Assumptions and Dependencies

- Source analysis is performed by a separate indexer repository/service.
- The platform receives normalized output in a stable import format.
- Initial deployment targets a small to medium internal team.
- Docker-based installation is acceptable for the MVP.
- Authentication may begin simply and become more advanced later.
