# Governance Timeline Projection Evolution Contract

## 1. Projection Evolution Goals

This document defines future evolution semantics for Governance Timeline
projection persistence and projection-backed runtime behavior.

Projection evolution goals include:

- append-oriented schema evolution
- preserved historical audit continuity
- stable API evolution
- stable cursor evolution

## 2. Schema Evolution Principles

Projection schema evolution should follow these principles:

- breaking evolution should be minimized
- append-oriented field evolution should be preferred
- historical projection continuity must remain preserved
- projection overwrite should be minimized

Schema change exists to extend projection utility, not to rewrite historical
audit meaning.

## 3. Backward Compatibility Expectations

Projection evolution should preserve compatibility for existing consumers.

Required expectations:

- the existing API contract should remain preserved whenever possible
- frontend compatibility should remain preserved
- runtime summary compatibility should remain preserved
- timeline type compatibility should remain preserved

Projection evolution should favor additive compatibility over disruptive shape
changes.

## 4. Cursor Compatibility Expectations

Projection evolution must preserve cursor safety and pagination behavior.

Required expectations:

- opaque cursor semantics remain preserved
- cursor ordering compatibility remains preserved
- `occurredAt DESC, eventId DESC` compatibility remains preserved
- stable pagination compatibility remains preserved
- cursor invalidation should be minimized

Cursor transport and internal encoding may evolve, but externally visible
cursor semantics should remain stable.

## 5. Metadata Evolution Expectations

Projection metadata evolution must preserve sanitization boundaries.

Required expectations:

- sanitized metadata boundary remains preserved
- unsafe payload evolution is not allowed
- secret, token, and password persistence is not allowed
- raw prompt and raw response persistence is not allowed

Metadata evolution must not become a path for unsafe source payload expansion.

## 6. Replay and Rebuild Compatibility

Projection evolution must remain compatible with replay, rebuild, and archive
flows.

Required expectations:

- historical replay and rebuild compatibility remains preserved
- replay idempotency compatibility remains preserved
- retention and archive compatibility remains preserved

Schema evolution should not make rebuild, backfill, or recovery unsafe for
existing audit history.

## 7. Operator-facing Evolution Visibility

Evolution visibility is operator-facing informational semantics only.

Evolution visibility must not imply:

- auto-remediation semantics
- governance action trigger semantics

Evolution visibility should help operators understand compatibility posture,
schema rollout state, and safe runtime interpretation only.

## 8. Migration Expectations

Expected future migration behavior:

- runtime aggregation to persistent projection migration compatibility remains
  preserved
- projection schema evolution compatibility remains preserved
- frontend and API compatibility remains preserved
- cursor contract remains preserved

Internal schema strategy may evolve, but externally visible Timeline behavior
must remain stable.

## 9. Non-goals

This contract does not introduce:

- automatic schema migration orchestration
- event sourcing migration
- cross-region schema coordination
- distributed schema lock orchestration
