# Runtime Operational Reliability Evidence Routing Dispatch Phase Closure

## 1. Purpose

This document closes the Operational Reliability evidence routing and dispatch
contract phase.

The goal of this phase is to stabilize the vendor-neutral runtime path from
adapter discovery metadata through adapter selection, query routing,
semantic routing plan construction, and dispatch contract boundary before any
real adapter invocation, transport integration, or persistent evidence execution
path is introduced.

## 2. Completed Scope

The completed routing and dispatch scope now includes:

- EvidenceAdapterRegistry
- EvidenceAdapterRegistration
- EvidenceAdapterDescriptor
- EvidenceAdapterAvailability
- EvidenceAdapterRejectionReason
- EvidenceAdapterSelector
- EvidenceAdapterSelection
- EvidenceAdapterSelectionPolicy
- EvidenceAdapterSelectionRejectionReason
- EvidenceAdapterSelectionScope
- EvidenceQueryRouter
- EvidenceQueryRoute
- EvidenceQueryRoutingDecision
- EvidenceQueryRoutingRejectionReason
- EvidenceQueryRoutingScope
- EvidenceRoutingPlan
- EvidenceRoutingPlanBuilder
- EvidenceRoutingPlanStatus
- EvidenceRoutingPlanRejectionReason
- EvidenceRoutingPlanScope
- EvidenceDispatchContract
- EvidenceDispatchRequest
- EvidenceDispatchResult
- EvidenceDispatchStatus
- EvidenceDispatchRejectionReason

This phase completes the semantic metadata path from discovery to dispatch candidate
without introducing actual evidence adapter execution.

## 3. Registry Semantics

Registry semantics are now fixed as:

- registry는 discovery-only
- adapter 등록 순서는 의미 없음
- source type 기준 조회
- 동일 source type 다중 adapter 허용
- unavailable adapter도 registry 등록 가능
- adapter descriptor는 vendor-neutral metadata만 노출
- raw credential/configuration 노출 금지

Registry registration remains metadata only.

- registration != activation
- registration != execution permission
- registration != health check result

## 4. Selection Semantics

Selection semantics are now fixed as:

- selector는 adapter 실행자가 아님
- selector는 query를 실행하지 않음
- registry discovery 결과만 기반으로 selection
- AVAILABLE adapter 우선
- DEPRECATED adapter는 restricted selection
- UNAVAILABLE adapter는 기본 selection 제외
- UNKNOWN adapter는 uncertainty selection 후보
- selection failure는 system failure가 아니라 evidence uncertainty

Selection therefore remains a semantic routing precursor, not an execution step.

## 5. Query Routing Semantics

Query routing semantics are now fixed as:

- router는 query 실행자가 아님
- router는 adapter selection 결과를 routing metadata로 변환
- route accepted는 adapter execution permission이 아님
- unavailable adapter route 금지
- deprecated adapter route는 RESTRICTED route
- unknown adapter route는 UNCERTAIN route
- routing failure는 system failure가 아니라 evidence uncertainty
- raw credential/configuration 노출 금지

Routing remains a metadata transformation boundary only.

## 6. Routing Plan Semantics

Routing plan semantics are now fixed as:

- routing plan은 semantic metadata
- accepted route 없으면 plan reject
- restricted route가 포함되면 plan은 RESTRICTED
- uncertain route가 포함되면 plan은 UNCERTAIN
- unavailable / rejected route 포함 금지
- accepted plan != execution permission
- routing plan은 recommendation authority가 아님
- routing plan은 execution authority가 아님

The routing plan is therefore a semantic routing summary, not an execution plan.

## 7. Dispatch Contract Semantics

Dispatch contract semantics are now fixed as:

- dispatch contract는 actual executor가 아님
- dispatch contract는 실제 adapter executor가 아님
- routing plan ACCEPTED / RESTRICTED / UNCERTAIN 상태만 dispatch candidate 가능
- REJECTED routing plan은 dispatch 금지
- dispatch request는 raw credential/configuration 포함 금지
- dispatch result는 normalized EvidenceQueryResult만 허용
- dispatch failure는 system failure가 아니라 evidence uncertainty
- accepted dispatch != execution permission

Dispatch remains a contract boundary for future execution wiring, not the execution itself.

## 8. Payment Consistency Routing Rule

Payment consistency routing semantics are now fixed as:

- payment consistency route/dispatch에는 payment-supporting adapter 필수
- payment consistency query는 payment-supporting route 필수
- payment consistency dispatch에는 payment route 필수
- payment supporting adapter 없으면 selection, routing, dispatch 모두 reject 가능

Payment consistency routing is therefore a mandatory semantic admission rule,
not an optional preference.

## 9. Runtime Invariants

The following invariants are now phase-complete and must be preserved:

- registry는 discovery-only
- selector는 adapter 실행자가 아님
- router는 query 실행자가 아님
- routing plan은 semantic metadata
- dispatch contract는 actual executor가 아님
- accepted route/plan/dispatch != execution permission
- payment consistency route/dispatch에는 payment-supporting adapter 필수
- unavailable adapter route 금지
- deprecated adapter는 restricted route/plan만 허용
- unknown adapter는 uncertain route/plan만 허용
- adapter/routing/dispatch failure는 system failure가 아니라 evidence uncertainty
- raw credential/configuration 노출 금지
- recommendation authority 없음
- execution authority 없음
- portfolio knowledge source 수정 금지

These invariants define the stable runtime metadata boundary for adapter discovery,
selection, routing, plan construction, and dispatch candidacy.

## 10. Deferred Scope

The following work remains intentionally deferred:

- actual adapter invocation
- WebClient/Reactor integration
- adapter timeout/retry policy
- adapter health check
- evidence dispatch executor
- persistent evidence store
- production adapter configuration
- observability authentication/authorization

Future execution wiring must preserve the routing and dispatch invariants
established in this phase.

## 11. Non-Goals

This phase closure does not introduce:

- actual adapter execution
- evidence dispatch execution engine
- WebClient or Reactor transport implementation
- Spring bean activation
- persistent evidence storage
- recommendation generation
- execution permission grant
- observability authentication wiring

## 12. Phase Closure Summary

The Runtime Operational Reliability evidence routing and dispatch contract phase
is complete.

The runtime now has stable semantic boundaries across:

- adapter registry discovery
- adapter selection
- query routing metadata
- routing plan construction
- dispatch candidate contract
- payment consistency routing requirements
- evidence uncertainty preservation

Future implementations must preserve the established boundary that registry,
selection, routing, planning, and dispatch contracts are semantic metadata layers,
not actual adapter executors, not recommendation engines, and not execution authorities.
