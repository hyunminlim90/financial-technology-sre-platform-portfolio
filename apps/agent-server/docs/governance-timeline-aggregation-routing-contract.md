# Governance Timeline Aggregation Routing Contract

## 1. Aggregation Routing Goals

This document defines the activation and safety contract for Timeline
aggregation routing.

The goal is to preserve the existing Timeline API while defining how future
projection-backed aggregation may be introduced safely beside the current
runtime fan-out path.

## 2. Default Runtime Fan-out Path

The default aggregation mode remains `RUNTIME_FAN_OUT`.

In the current architecture phase:

- `RUNTIME_FAN_OUT` is the active default
- the current source fan-out aggregation path remains authoritative
- controller API behavior remains unchanged

This default must remain stable until explicit activation requirements for
projection-backed routing are satisfied.

## 3. Projection-backed Future Path

`PROJECTION_BACKED` is a future aggregation mode.

This means:

- projection-backed aggregation is not the default
- projection-backed routing must not be activated implicitly
- controller API contract must remain unchanged across future mode activation

Future projection-backed routing must preserve the same operator-facing query
semantics as the runtime fan-out path.

## 4. Safe Activation Requirements

Projection-backed routing may be activated only through explicit configuration.

Required activation preconditions include:

- projection store must be sufficiently bootstrapped
- projection query adapter must support cursor, filter, and from-to semantics
  with the same behavior as the runtime path
- metrics, health, and runtime surfaces must expose mode differences to
  operators
- projection-backed integration testing must pass before activation

Safe activation must not rely on hidden defaults or implicit runtime switching.

## 5. Rollback Expectations

Rollback to `RUNTIME_FAN_OUT` must remain possible.

Required rollback expectations:

- returning mode to `RUNTIME_FAN_OUT` restores the source fan-out path
- rollback must not change the API response contract
- frontend contract must remain preserved

Rollback safety is required so that projection-backed activation can be
reversed without changing operator-visible Timeline semantics.

## 6. Metrics and Observability Expectations

Aggregation routing must support operator-visible mode awareness.

Required expectations:

- aggregation mode visibility remains available
- runtime fan-out versus projection-backed query behavior must be
  distinguishable to operators
- only low-cardinality metric tags are allowed

Forbidden metric tags include:

- cursor values
- `eventId`
- raw exception detail

## 7. Compatibility Requirements

Routing activation and rollback must preserve:

- controller API contract stability
- `occurredAt DESC, eventId DESC` ordering compatibility
- opaque cursor semantics
- degraded response semantics

Routing must change implementation path only. It must not change the externally
visible Timeline contract.

## 8. Non-goals

This contract does not introduce:

- actual bean wiring switch
- `@Primary` activation
- controller dependency changes
- runtime switch endpoint
- R2DBC implementation
- projection-backed mode activation
