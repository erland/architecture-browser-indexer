# Architecture Browser Platform — Development Plan

## 1. Goal

Build the user-facing self-hosted platform for the Architecture Browser product. This repository owns installer packaging, backend API, persistence for workspaces/snapshots/overlays, and the web UI for browsing architecture-oriented views over indexed code facts produced by the separate indexer repository.

## 2. Technology Choices

### Proposed stack
- Frontend: React + TypeScript
- Backend API: Java with Quarkus
- Persistence: PostgreSQL
- Packaging: Docker Compose for MVP
- Background processing: backend-managed job execution and queue abstraction
- Auth for MVP: simple local auth or reverse-proxy auth passthrough
- Data exchange with indexer: versioned JSON-based import contract

### Brief justification
- React + TypeScript aligns well with rich interactive architecture views.
- Quarkus is a strong fit for a compact backend with REST APIs, jobs, and packaged deployment.
- PostgreSQL provides robust relational storage for snapshots, metadata, overlays, and audit records.
- Docker Compose gives the easiest initial install path for internal teams and architects.

## 3. Repository Responsibilities

This repository includes:
- platform backend API
- platform web application
- database migrations
- installer/deployment assets
- import pipeline for normalized indexer results
- browse/query services
- overlay/saved-view/audit services

This repository excludes:
- source-code parsing
- Tree-sitter extraction logic
- framework detection extractors
- normalized IR generation from raw repositories

## 4. Project Structure

Proposed top-level structure:

- `/apps/web` — React frontend
- `/apps/api` — Quarkus backend
- `/libs/contracts` — shared API/import schemas and DTO definitions
- `/libs/view-models` — view shaping and shared frontend/backend query models if useful
- `/deploy/docker-compose` — local packaged deployment
- `/docs` — architecture notes, install docs, API/import docs
- `/db/migrations` — database schema migrations
- `/scripts` — local helper scripts

## 5. Delivery Strategy

Build the platform in thin vertical slices so that every step results in a working, testable increment. Prioritize easy install and useful browse experience over broad feature count.

## 6. Assumptions

- The indexer repo will publish a stable normalized import payload.
- The MVP supports a small number of repositories and snapshots.
- Initial users are internal architects and technical reviewers.
- We can defer advanced SSO and HA concerns until after MVP validation.

## 7. Step-by-Step Plan

## Step 1. Create platform monorepo baseline

### Deliverables
- repository skeleton
- frontend app scaffold
- backend app scaffold
- shared contracts module
- docs/README with local run instructions
- Docker Compose skeleton with database

### Verification
- frontend starts locally
- backend starts locally
- database container starts
- one command boots local dev environment

## Step 2. Define core domain model and import contract

### Deliverables
- domain model for workspace, repository registration, run, snapshot, entity, relationship, overlay, saved view, audit event
- versioned import contract for normalized indexer payloads
- initial ERD/schema documentation
- migrations for first database schema

### Verification
- migration applies cleanly
- contract validation tests pass
- sample import payload can be parsed and stored in a stubbed path

## Step 3. Implement workspace and repository management APIs

### Deliverables
- CRUD APIs for workspaces
- CRUD APIs for repository registrations
- validation rules
- basic audit records
- frontend pages/forms for management

### Verification
- create/update/list/archive flows work
- validation errors shown properly
- audit entries recorded

## Step 4. Implement index-run orchestration and status tracking

### Deliverables
- run request API
- run history model
- run state transitions
- UI for current/recent run status
- stub integration adapter for indexer invocation/import

### Verification
- run can be requested
- state changes are persisted
- failure and success states are visible in UI

## Step 5. Implement snapshot import pipeline

### Deliverables
- endpoint/service to import normalized index results
- immutable snapshot creation
- imported entity and relationship persistence
- error/diagnostic handling for bad imports

### Verification
- valid sample payload creates snapshot
- invalid payload is rejected safely
- partial payload behavior follows defined rules

## Step 6. Build snapshot catalog and overview pages

### Deliverables
- snapshot list page
- snapshot metadata page
- system overview page
- top-level repository/module rollups
- favorite/baseline markers

### Verification
- imported snapshots can be browsed
- overview renders meaningful summary
- navigation between workspace and snapshot screens works

## Step 7. Build repository/module layout explorer

### Deliverables
- tree/layout API
- repository/module/package explorer UI
- classification badges and counts
- drill-down navigation

### Verification
- users can expand structures smoothly
- drill-down reveals lower-level scopes
- large sample data remains usable

## Step 8. Build dependency and relationship views

### Deliverables
- dependency query APIs
- graph/list dependency view
- inbound/outbound filtering
- scoped focus mode

### Verification
- dependency filtering works
- selected entity focus works
- high-level dependency inspection is understandable

## Step 9. Build entry-point and data/integration views

### Deliverables
- entry-point explorer
- persistence/integration explorer
- detail panels for endpoints, datastores, channels, external systems
- cross-links back to owners and source context

### Verification
- entry points are listable and drill-down works
- data/integration elements can be filtered by scope
- detail views show relationships clearly

## Step 10. Implement search and entity detail pages

### Deliverables
- search API
- search UI
- generic entity detail page
- source-context drill-down metadata display

### Verification
- entity search returns useful results
- clicking result opens correct detail context
- duplicate names across scopes are disambiguated

## Step 11. Implement overlays, notes, and saved views

### Deliverables
- overlay/tag APIs
- note support
- saved view persistence
- UI for creating and reusing saved views

### Verification
- overlays do not modify imported facts
- saved views reopen correctly
- audit entries are produced

## Step 12. Implement snapshot comparison summary

### Deliverables
- compare-two-snapshots API
- high-level summary UI for additions/removals/major changes
- comparison navigation from snapshot catalog

### Verification
- meaningful summary shown for changed snapshots
- unchanged areas are identified clearly
- compare operation is safe for large datasets at MVP scale

## Step 13. Add administration, retention, and operational visibility

### Deliverables
- admin pages for runs, repositories, and retention
- health/status pages
- retention policy implementation
- diagnostic visibility for failed imports/runs

### Verification
- retention actions work safely
- diagnostics visible for failed runs
- basic operations documentation complete

## Step 14. Harden packaging, tests, and installer experience

### Deliverables
- polished Docker Compose deployment
- sample environment files
- first-run setup flow
- test coverage across import, browse, and management flows
- end-to-end smoke test

### Verification
- fresh install works from documented steps
- sample repository can be indexed/imported and browsed
- smoke tests pass in CI

## 8. Testing Strategy

### Backend
- unit tests for domain services
- contract tests for import validation
- repository/integration tests for persistence
- API tests for main flows

### Frontend
- component tests for browse screens
- state/query tests
- end-to-end smoke tests for core user journeys

### Cross-cutting
- golden JSON fixtures for import payloads
- representative snapshot fixtures
- deterministic query result expectations where practical

## 9. Minimal Safety Net

Before major refactors or feature expansion, ensure:
- migration tests pass
- import fixture tests pass
- snapshot browse smoke flow passes
- search/detail flow passes

## 10. Verification Commands

The repository should expose clear commands for:
- install dependencies
- run frontend dev server
- run backend dev server
- run database via Docker Compose
- run backend tests
- run frontend tests
- run end-to-end smoke tests
- start packaged local stack

## 11. Risks and Mitigations

### Risk: import contract churn
Mitigation: version the import contract early and add fixture-based compatibility tests.

### Risk: browse queries become too slow
Mitigation: precompute rollups and denormalized browse tables or materialized views as needed.

### Risk: UI becomes too developer-centric
Mitigation: validate every screen against architect tasks, not parser internals.

### Risk: packaging complexity
Mitigation: keep MVP deployment minimal and document one golden path first.

## 12. Expected End State

At MVP completion, the repository should deliver:
- a self-hosted platform installable with Docker Compose
- workspace and repository registration
- index run orchestration and snapshot import
- architecture browsing views
- search, overlays, saved views, and comparison summary
- operational basics suitable for internal architectural use
