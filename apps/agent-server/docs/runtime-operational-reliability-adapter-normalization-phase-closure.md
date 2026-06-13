# Runtime Operational Reliability Adapter Normalization Phase Closure

## 1. Purpose

This document closes the Operational Reliability adapter normalization contract phase.

The goal of this phase is to stabilize the vendor-specific adapter normalization
contract for Prometheus, Loki, and Tempo before any real HTTP adapter, transport,
authentication, retry, or production observability integration is introduced.

## 2. Completed Scope

The completed adapter normalization scope now includes:

PrometheusEvidenceAdapterContract
PrometheusEvidenceQuery
PrometheusEvidenceMapping
PrometheusMetricSemanticType
PrometheusEvidenceRejectionReason

LokiEvidenceAdapterContract
LokiEvidenceQuery
LokiEvidenceMapping
LokiLogSemanticType
LokiEvidenceRejectionReason

TempoEvidenceAdapterContract
TempoEvidenceQuery
TempoEvidenceMapping
TempoTraceSemanticType
TempoEvidenceRejectionReason

This phase completes the contract boundary between vendor-specific evidence adapters
and normalized semantic evidence accepted by the operational reliability runtime.

## 3. Adapter Normalization Model

The adapter normalization model is now fixed as:

- vendor-specific query contract remains outside semantic runtime
- vendor-specific payload never crosses the semantic runtime boundary directly
- mapping layers convert vendor-specific evidence into normalized EvidenceSignal
- all adapter output must become normalized EvidenceSignal
- normalized evidence is the only evidence shape allowed to flow into semantic runtime

The normalization model is therefore a contract boundary, not a transport implementation.

## 4. Source-Specific Boundaries

Source-specific boundaries are now phase-complete:

- Prometheus = METRICS only
- Loki = LOGS only
- Tempo = TRACES only

These boundaries ensure that each adapter contract is scoped to the correct evidence
source and does not blur source semantics across runtime layers.

## 5. Raw Payload Protection

Raw payload protection is a hard invariant across all adapter contracts.

- raw payload exposure 금지
- customer/payment payload, token, secret, internal IP 노출 금지
- raw Prometheus metric response does not enter semantic runtime directly
- raw Loki log payload does not enter semantic runtime directly
- raw Tempo trace/span payload does not enter semantic runtime directly

Adapter mappings may expose only sanitized normalized summaries suitable for semantic use.

## 6. High-Cardinality Protection

High-cardinality protection is mandatory across normalized adapter output.

- high-cardinality identifiers 노출 금지
- Prometheus labels with high-cardinality risk must not become semantic evidence
- Loki labels with high-cardinality risk must not become semantic evidence
- Tempo traceId/spanId and similar identifiers must not become semantic evidence

The normalized runtime contract prefers bounded semantic signals over raw observability identifiers.

## 7. Payment Safety Evidence Rule

Payment safety remains explicitly protected in adapter normalization.

- payment-related evidence는 sanitized consistency metadata 없으면 safety evidence 승격 금지
- Prometheus payment consistency metrics require consistency metadata before PAYMENT_SAFETY promotion
- Loki payment-related logs require sanitized consistency metadata before PAYMENT_SAFETY promotion
- Tempo payment-related traces require sanitized consistency metadata before PAYMENT_SAFETY promotion

Payment safety promotion therefore depends on explicit sanitized consistency context,
not on vendor payload presence alone.

## 8. Adapter Failure Semantics

Adapter failure semantics are intentionally bounded.

- adapter failure != system failure
- query 실패는 system failure가 아니라 adapter failure / unknown evidence
- failed adapter collection may still preserve runtime uncertainty instead of false certainty
- adapter failure does not create recommendation authority
- adapter failure does not create execution authority

These semantics keep adapter transport problems from being misclassified
as operational truth.

## 9. Runtime Invariants

The following invariants are now phase-complete and must be preserved:

- Prometheus = METRICS only
- Loki = LOGS only
- Tempo = TRACES only
- raw payload exposure 금지
- high-cardinality identifiers 노출 금지
- customer/payment payload, token, secret, internal IP 노출 금지
- payment-related evidence는 sanitized consistency metadata 없으면 safety evidence 승격 금지
- adapter failure != system failure
- all adapter output must become normalized EvidenceSignal
- adapter contracts have no recommendation authority
- adapter contracts have no execution authority
- adapter contracts do not mutate portfolio knowledge source

These invariants define the stable safety and normalization boundary for
future vendor-specific adapter implementations.

## 10. Deferred Scope

The following work remains intentionally deferred:

- actual Prometheus HTTP adapter
- actual Loki HTTP adapter
- actual Tempo / OpenTelemetry adapter
- WebClient / Reactor integration
- query timeout / retry policy
- adapter health check
- persistent evidence storage
- adapter configuration management
- production observability authentication

Future implementations must preserve the source-specific boundaries,
raw payload protection, high-cardinality protection, payment safety rule,
and adapter failure semantics established in this phase.

## 11. Non-Goals

This phase closure does not introduce:

- real Prometheus HTTP execution
- real Loki HTTP execution
- real Tempo / OpenTelemetry execution
- raw payload persistence
- scheduler or polling runtime
- Spring bean activation
- recommendation generation
- execution permission grant
- production observability authentication wiring

## 12. Phase Closure Summary

The Runtime Operational Reliability adapter normalization contract phase is complete.

The runtime now has stable vendor-specific normalization contracts for:

- Prometheus metrics evidence
- Loki logs evidence
- Tempo traces evidence
- sanitized payment safety promotion rules
- raw payload protection
- high-cardinality protection
- normalized semantic evidence output

Future adapter implementations must preserve the established boundary that
vendor-specific adapters are normalization contracts only, not recommendation engines,
not execution authorities, and not raw observability payload passthrough paths.
