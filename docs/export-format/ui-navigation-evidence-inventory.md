# UI navigation evidence inventory (Step 3)

This note inventories the current frontend raw evidence that can feed later `ui-navigation` normalization work.

## Grounded evidence currently exposed by extraction

### Declared routes

The frontend routing extractor now emits route entities with conservative raw evidence in metadata:

- `route = true`
- `routePath`
- `routeFullPath`
- `routeDeclarationKind` (`route-object`, `jsx-route`)
- `routeSourceKind = declared-route`
- `redirectTargetLiteral` when a declared route includes an explicit redirect

### Route structure and route targets

Route relationships now expose raw evidence suitable for later normalization:

- `frameworkRelationship = childOf`
- `frameworkRelationship = targets`
- `frameworkRelationship = lazyLoads`
- `frameworkRelationship = guards`
- `frameworkRelationship = resolves`
- `frameworkRelationship = redirects`

Additional metadata carried on these relationships may include:

- `routeDeclarationKind`
- `routeSourceKind`
- `parentRouteEntityId`
- `parentRoutePath`
- `parentRouteFullPath`
- `guardReference`
- `redirectTargetLiteral`
- `navigationTargetLiteral`

### Explicit static navigation evidence

The extractor now emits conservative navigation evidence for obvious static frontend navigation constructs:

- React `<Link to="/x">` / `<NavLink to="/x">`
- Angular `routerLink="/x"`
- Angular `[routerLink]="['/x']"`
- imperative `navigate('/x')`
- imperative `navigateByUrl('/x')`
- imperative `router.navigate(['/x'])`

These are emitted as framework relationships from the nearest grounded source entity in the same file to an inferred route entity:

- `frameworkRelationship = linksToRoute`
- `frameworkRelationship = navigatesToRoute`

Relationship metadata includes:

- `routeSourceKind = link | navigate-call`
- `navigationLiteral`
- `navigationTargetLiteral`

## Evidence intentionally deferred for later steps

The MVP still does **not** attempt to extract or normalize:

- computed/dynamic route strings
- state-dependent conditional navigation
- tabs, drawers, dialogs, or transient overlays
- weak menu/sidebar heuristics without clear static route targets
- broad workflow semantics beyond actual route/page evidence

## Why this is enough for the next step

This is now sufficient to build a single normalization seam for:

- declared route/page entities
- route containment
- redirects
- guard relationships
- explicit static navigation edges

The export contract remains framework-agnostic. These fields are internal evidence that later normalization rules can consume conservatively.
