# Curated export examples

These examples are intentionally small, human-readable samples that illustrate the main export families and the new normalized viewpoint semantics.

## Baseline examples
- `minimal-export.json` — smallest useful successful document
- `java-backend-export.json` — backend-focused example with endpoint, CDI/JPA semantics, dependency views, and Java browser views
- `frontend-export.json` — frontend-focused example with routes, composition/provider/hook relationships, dependency views, and frontend browser views
- `mixed-full-export.json` — mixed example showing both frontend and Java browser/dependency-view families in one document
- `minimal-success.json` — existing baseline fixture copied from current checked-in fixtures
- `partial-result.json` — existing partial/degraded baseline fixture copied from current checked-in fixtures

## Step 10 viewpoint scenario examples
- `java-rest-persistence-export.json` — simple REST + service + repository + JPA entity scenario. Demonstrates `api-entrypoint`, `application-service`, `persistent-entity`, `persistence-access`, plus `serves-request`, `invokes-use-case`, and `accesses-persistence`.
- `java-persistence-only-export.json` — persistence-focused batch/job scenario with no exposed API. Demonstrates that `persistence-model` can be available while `api-surface` and `request-handling` remain unavailable.
- `java-external-integration-export.json` — exposed API that delegates to an external integration adapter. Demonstrates `integration-map` together with `calls-external-system`.

These examples are curated contract aids. They should stay aligned with current indexer logic and conservative derivation rules rather than describing aspirational future output.
