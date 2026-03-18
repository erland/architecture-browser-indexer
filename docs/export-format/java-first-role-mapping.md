# Java-First Role Mapping

Step 5 wires the new normalization seam into the first real technology slice: Java backend evidence.

## Implemented mappings

The default normalization service now applies a conservative Java rule that maps existing Java/JAX-RS/JPA/repository evidence into canonical contract fields:

- JAX-RS resource / resource role / Java endpoint -> `api-entrypoint`
- API entrypoint -> trait `externally-exposed`
- Java service evidence -> `application-service`
- JPA entity evidence -> `persistent-entity`
- JPA entity -> trait `persistent`
- repository / DAO / mapper / persistence adapter evidence -> `persistence-access`

## Evidence sources reused

The mapping stays additive and reuses evidence already emitted by the indexer, including:

- Java extraction metadata such as `jaxRsResource`, `jpaEntity`, `jpaKind`, `annotations`, and `packageName`
- Java interpretation metadata such as `entityRole`, `backendProfile`, and `sourceEntityId`
- inferred Java endpoint entities (`ENDPOINT`) that already carry `sourceLanguage=java`

## Conservative boundaries

Step 5 intentionally does **not** yet over-infer broader domain semantics.

Examples:

- JPA `@Embeddable` types are not automatically promoted to `domain-entity`
- no automatic `domain-entity` role is added for every JPA entity
- no cross-language mapping is attempted yet

## Outcome

The export contract now contains real canonical roles/traits for Java snapshots without removing any framework-specific metadata needed for diagnostics or traceability.
