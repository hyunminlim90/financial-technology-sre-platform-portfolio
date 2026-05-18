# R2DBC Governance Persistence

## Purpose

This document defines the reactive persistence boundary for governance records.

The current priority is moving append-only governance history away from in-memory storage and toward a persistent R2DBC-backed boundary.

## Principles

- append-only governance history
- reactive-only persistence
- no JPA
- no Hibernate
- no blocking repositories
- no payload persistence
- no customer data persistence

## Current Scope

The initial R2DBC boundary starts with recommendation governance persistence:

- RecommendationRecord

Additional governance records should follow the same pattern:

- domain record
- persistence entity
- mapper
- reactive repository
- profile-based store selection

## Profile Boundary

In-memory stores remain active for non-R2DBC profiles:

```text
@Profile("!r2dbc")
```

R2DBC stores are activated only when the `r2dbc` profile is enabled:

```text
@Profile("r2dbc")
```

## Reactive-Only Policy

Persistence must use:

- Spring WebFlux
- Spring Data R2DBC
- ReactiveCrudRepository

Persistence must not use:

- JPA
- Hibernate
- JDBC template
- blocking data access

## Metadata Safety

Governance persistence must not store keys containing:

- payload
- customer
- token
- secret
- password
- payment
- rawLog
- prompt

## Recommended Indexes

Dashboard and governance queries are time-oriented.

Recommended indexes include:

- `recommendation_records (generated_at desc)`
- `recommendation_records (incident_id, generated_at desc)`
- future governance tables on `(incident_id, status, created_at)`

## Dashboard Query Patterns

The persistence layer should support:

- recent records ordered by event time
- incident-specific history queries
- append-only audit retrieval
- trend-oriented time-range queries

## Non-goals

This boundary does not complete database migration.

It does not include:

- Flyway
- Liquibase
- full governance table rollout
- blocking compatibility layers
