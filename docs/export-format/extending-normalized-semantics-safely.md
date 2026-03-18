# Contributor guidance for extending normalized semantics safely

This guide is a practical checklist for future contributors.

Use it when you want to add:

- a new architectural role
- a new architectural trait
- a new relationship semantic
- a new viewpoint
- a new language/framework mapping into the normalized layer

## Before adding anything new

Ask these questions first.

### Is the meaning architectural?
The addition should help explain the architecture of the system, not merely expose a parser or framework detail.

### Is the meaning reusable?
Prefer ids that more than one language/framework can emit over time.

### Can the existing vocabulary already express it?
Check whether the concept can already be modeled using:

- an existing role
- an existing trait
- an existing semantic
- existing viewpoint derivation

### Is the evidence strong enough?
If the evidence is weak or highly indirect, keep it in framework metadata first.

## Preferred extension flow

1. add or refine raw evidence in extraction/interpretation only if needed
2. add mapping logic in the normalization seam
3. keep the mapping conservative
4. add or update viewpoint derivation only after normalized meaning exists
5. add tests
6. add or update curated examples
7. update docs
8. review whether compatibility/versioning notes need updates

## Practical rules by concept type

### Adding a role
Add a new role only when the entity has a stable architectural responsibility that downstream consumers are likely to care about.

Good candidates:

- reusable across languages
- clearly distinct from existing roles
- observable from strong evidence

Avoid:

- framework-name roles
- roles that duplicate an existing trait
- speculative roles inferred from naming alone unless heavily constrained

### Adding a trait
Use a trait for cross-cutting characteristics that may apply to different role families.

Good candidates:

- stable properties such as external exposure, persistence, configuration ownership

Avoid:

- traits that simply restate the entity kind
- traits that always duplicate a role with no extra value

### Adding a relationship semantic
Add a relationship semantic when a structural edge kind is not expressive enough for architectural reasoning.

Good candidates:

- request flow
- use-case delegation
- persistence access
- external integration

Avoid inventing a semantic if the structural relationship kind already carries the full architectural meaning.

### Adding a viewpoint
A viewpoint should represent a user-meaningful architectural slice, not just an internal implementation convenience.

A good viewpoint:

- is likely to be useful in the browser/platform
- can be derived conservatively from normalized semantics
- has understandable availability rules
- can be seeded from entities/roles/semantics in a stable way

## Compatibility guidance

Treat these as platform-facing:

- canonical role ids
- canonical trait ids
- canonical semantic ids
- canonical viewpoint ids

Changing their meaning requires extra care.

### Usually safe changes
- adding new optional mappings that emit existing ids more often when evidence is strong
- adding new examples/tests/docs
- enriching evidence sources or preferred dependency views

### Review carefully
- introducing a brand new canonical id
- changing derivation thresholds in a way that materially changes viewpoint availability
- changing the intended meaning of an existing id

### Potentially breaking
- removing a canonical id
- renaming a canonical id
- reusing an existing canonical id for a meaningfully different concept

## Test expectations

A normalized semantics change should usually include:

- focused rule/unit tests
- at least one regression or scenario test
- example updates when representative output changes

For new canonical vocabulary, prefer at least one example that demonstrates:

- why the mapping exists
- when it should appear
- when it should not overclaim

## Example-driven discipline

Curated examples are not decorative. They are part of the contract hardening story.

Whenever possible:

- add a positive example
- add a boundary case or non-overclaim example
- make failures easy to understand from the example name

## Cross-language extension guidance

When adding TypeScript, SQL, config, or other support:

- do not copy Java-specific concepts directly unless the architectural meaning is identical
- map into the same canonical vocabulary where possible
- prefer adding supporting evidence before inventing new canonical ids
- let unsupported languages advertise fewer viewpoints rather than weak ones

## Review checklist

Before merging a normalized semantics change, verify:

- the meaning is architectural and reusable
- the mapping is conservative
- the vocabulary is not unnecessarily duplicated
- tests cover the intended behavior
- curated examples remain aligned
- docs explain the new behavior
- compatibility implications were considered

## Rule of thumb

When in doubt:

- keep raw/framework evidence richer
- keep canonical vocabulary smaller
- only promote stable architectural meaning into the normalized layer
