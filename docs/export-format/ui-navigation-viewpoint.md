# UI navigation viewpoint

This note explains how architects should interpret the canonical `ui-navigation` viewpoint.

It is intentionally written from the point of view of a person reading the export or a browser visualization, not from the point of view of the extractor internals.

## What the `ui-navigation` viewpoint is for

The `ui-navigation` viewpoint is a conservative, architect-focused slice of the export that helps answer questions such as:

- which user-facing pages or route targets exist
- how route containers and child pages are structured
- which pages can navigate to which other pages when there is grounded evidence
- where explicit redirects exist
- where route guards/protection boundaries are visible

The viewpoint is meant to support navigation-oriented architecture analysis.

It is **not** meant to be a complete behavioral model of the entire frontend.

## What the viewpoint shows

The viewpoint is derived from normalized export data and is expected to be rendered without any framework-specific browser logic.

At a high level, architects should expect to see:

- **pages** represented by canonical roles such as `ui-page`
- **layouts / route shells** represented by `ui-layout`
- **explicit navigation structures** such as grounded sidebars or menus represented by `ui-navigation-node`
- **route containment** represented by `contains-route`
- **page-to-page navigation** represented by `navigates-to`
- **redirect behavior** represented by `redirects-to`
- **route protection / route guard relationships** represented by `guards-route`

## What the viewpoint does not claim to show

This MVP does **not** claim to model all UI behavior.

In particular, do not interpret the viewpoint as a complete model of:

- every click path inside arbitrary components
- every dynamic or computed URL
- every state-dependent navigation branch
- dialogs, drawers, tabs, wizards, or other transient UI states unless they are represented through grounded route/page evidence
- detailed workflow semantics beyond route/page reachability
- framework runtime behavior in full generality

The correct interpretation is:

> this viewpoint shows the **grounded route/page navigation structure that the indexer could normalize with sufficient confidence**, not the entire behavior of the application.

## Reading the main entity roles

### `ui-page`

A `ui-page` is a user-reachable route, screen, or page.

Typical examples:

- `/login`
- `/dashboard`
- `/reports/:id`

A `ui-page` usually means there was grounded route/page evidence, not just a generic frontend module.

### `ui-layout`

A `ui-layout` is a route shell, route container, or layout that owns or structures child routes.

Typical examples:

- an authenticated app shell
- a dashboard layout that contains child pages
- a parent route that exists mainly to host nested routes

### `ui-navigation-node`

A `ui-navigation-node` is an explicitly grounded navigation structure such as a sidebar, menu, or nav group.

This role should be interpreted conservatively. It is only meaningful when there is enough evidence that the structure is actually part of navigation, not just a generic component.

## Reading the main relationship semantics

### `contains-route`

`contains-route` means the source structurally contains or owns the target route.

Typical interpretation:

- a layout contains child pages
- a parent route contains nested routes

This is about **route hierarchy**, not user action.

### `navigates-to`

`navigates-to` means there is grounded evidence that the source can lead the user to the target route/page.

Typical examples:

- a page with a static link to another route
- a grounded navigation node that points to a page
- an obvious imperative navigation call with a static route target

This is the main semantic for “a user can get from here to there”.

### `redirects-to`

`redirects-to` means the route configuration explicitly redirects one route to another.

Typical examples:

- `/` redirects to `/home`
- an auth entry route redirects to `/login`

This is stronger than inferred navigation because it usually comes from explicit route configuration.

### `guards-route`

`guards-route` means a guard/protection element is explicitly connected to a route.

Typical examples:

- an auth guard protecting `/admin`
- a role guard protecting `/reports`

This means the route has an explicit protection/control boundary. It does **not** by itself explain every runtime condition behind that protection.

## Declared routes vs inferred navigation

A key distinction in this viewpoint is the difference between:

- **declared routes**
- **inferred navigation**

### Declared routes

Declared routes come from grounded route declaration evidence.

Examples:

- route configuration objects
- nested child route declarations
- explicit route path declarations
- explicit redirect declarations

These are generally the strongest and most reliable parts of the viewpoint.

### Inferred navigation

Inferred navigation means the indexer found grounded evidence that one page/navigation structure can lead to another page, but the relationship is not itself a route declaration.

Examples:

- a static `<Link to="/reports">`
- a grounded `routerLink="/settings"`
- an obvious `navigate("/dashboard")`

These edges are still useful, but they should be interpreted as **evidence-backed reachability**, not as a full declarative routing truth.

## How to interpret confidence and evidence

Where confidence and evidence are shown in the export or browser, they should be read as signals about how directly the relationship was grounded.

A practical reading model is:

- **high confidence**: explicit route declarations, explicit redirects, explicit guard declarations, static route literals
- **medium confidence**: clear but partially inferred navigation from a grounded page/navigation structure
- **lower confidence**: weakly grounded or partially inferred frontend navigation patterns that were still strong enough to export

The exact numeric or textual confidence representation may evolve, but the architectural meaning should stay the same:

- stronger confidence means closer to explicit source evidence
- weaker confidence means more interpretation was involved

## Good evidence vs weak evidence

### Good evidence

Good evidence is direct, explicit, and stable.

Examples:

- route declaration with a path literal and known child routes
- redirect declaration with a literal target
- guard declaration attached directly to a route
- static link literal to a known declared route
- navigation node with explicit links to declared pages

These cases are the best foundation for architectural reasoning.

### Weak evidence

Weak evidence is indirect, ambiguous, or only partially grounded.

Examples:

- heavily computed route strings
- navigation assembled through runtime state in a way that does not leave stable static evidence
- generic components that look page-like but do not have grounded route evidence
- handlers that might navigate under certain conditions but do not expose a clear static target

Weak evidence should not be over-read. In many cases, the correct behavior for the exporter is to omit a canonical navigation edge rather than pretend certainty.

## How architects should use the viewpoint

Good uses of `ui-navigation` include:

- understanding the page/route surface of a frontend application
- identifying main route containers and major navigation hubs
- spotting explicit redirects and protected routes
- comparing route structure with backend/domain slices
- checking whether important areas of the application are reachable through explicit navigation structures

Less appropriate uses include:

- proving every user journey is represented
- assuming missing edges mean navigation is impossible
- treating weakly grounded inferred navigation as equivalent to explicit route declarations
- treating the view as a substitute for runtime UX testing

## Typical interpretation pitfalls

### Pitfall 1: “If an edge is missing, users can never get there.”

Not necessarily.

A missing edge often means only that the indexer did not have enough static evidence to export a conservative canonical relationship.

### Pitfall 2: “All pages are modeled.”

Not necessarily.

The viewpoint models pages/routes that were grounded well enough to normalize as `ui-page` or `ui-layout`.

### Pitfall 3: “This is a workflow model.”

No.

This viewpoint is about route/page navigation structure and reachability, not full end-user workflow semantics.

### Pitfall 4: “The browser knows React Router / Angular Router details.”

It should not.

Those details should already have been normalized away by the indexer before the viewpoint is rendered.

## Suggested way to explain the viewpoint to stakeholders

A good short description is:

> The UI navigation viewpoint shows the conservative page-and-route navigation structure that could be grounded from the codebase. It highlights pages, route containers, redirects, guards, and strong page-to-page navigation evidence, but it does not claim to represent all UI behavior.

## Related files

Use these together with this note:

- `ui-navigation-baseline.md` — original baseline and vocabulary decisions
- `ui-navigation-evidence-inventory.md` — what raw frontend evidence currently feeds the viewpoint
- `ui-navigation-normalization-seam.md` — internal seam used by normalization rules
- `viewpoint-availability-derivation.md` — when the viewpoint is emitted and how it is seeded/expanded
- `examples/ui-navigation-export.json` — curated example payload

## Current repository note

This repository contains the export/indexer-side documentation for the viewpoint.

If the browser repository also contains user-facing analysis-view documentation, that documentation should explain the same interpretation rules in browser-specific terms, especially:

- how edge types are rendered
- how confidence/evidence is surfaced in the UI
- what partial availability means in the browser
