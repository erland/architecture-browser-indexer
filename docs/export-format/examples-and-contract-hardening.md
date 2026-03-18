# Examples and contract hardening

Step 10 adds scenario-oriented curated examples and focused contract tests so the new normalized semantics remain concrete, reviewable, and grounded in actual indexer behavior.

## Added scenario examples

### `java-rest-persistence-export.json`
A minimal backend slice with:
- one API entrypoint
- one application service
- one repository / persistence access component
- one JPA entity

This is the primary grounded example for:
- `api-surface`
- `request-handling`
- `persistence-model`
- `serves-request`
- `invokes-use-case`
- `accesses-persistence`

### `java-persistence-only-export.json`
A batch/job-oriented scenario with persistence but no exposed HTTP API.

This example is intentionally important for negative assertions:
- `persistence-model` is available
- `api-surface` is unavailable
- `request-handling` is unavailable

This protects against over-eager future derivation logic.

### `java-external-integration-export.json`
A backend slice where a service delegates to an external integration adapter.

This is the primary grounded example for:
- `integration-map`
- `calls-external-system`


### `ui-navigation-export.json`
A conservative frontend routing/navigation slice with:
- one route shell / layout
- two user-facing pages
- one explicit sidebar/menu navigation structure
- one redirect edge
- one guard edge

This is the primary grounded example for:
- `ui-navigation`
- `ui-page`
- `ui-layout`
- `ui-navigation-node`
- `contains-route`
- `navigates-to`
- `redirects-to`
- `guards-route`

## Hardening intent

The goal is not to add more aspirational sample output. The goal is to keep curated examples aligned with what the current contract and current normalization logic can plausibly emit.

When the indexer logic changes, maintainers should update:
- the example payloads
- the focused scenario assertions
- any explanatory documentation tied to those examples

## Test expectations

The scenario tests should make failures easy to interpret by checking:
- viewpoint availability for each scenario
- presence of canonical roles / traits where expected
- presence of canonical relationship semantics where expected
- absence of viewpoints that the evidence does not justify
- canonical UI-navigation viewpoint shape for conservative frontend routing/navigation evidence

This is intentionally stronger than schema validation alone.
