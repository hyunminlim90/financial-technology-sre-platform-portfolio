# Action DSL Policy Engine

## Why This Exists

String-only recommended actions are easy to read, but they are hard to control safely.
We need an internal structure that lets the server validate risk, approval, rollback, and payment safety before any recommendation is shown to a human.

## Limits Of String Actions

- A plain sentence does not reliably express risk level.
- A plain sentence does not reliably express blast radius.
- A plain sentence does not tell us whether approval is mandatory.
- A plain sentence cannot safely encode payment safety constraints.
- String filters are useful as a fallback, but they are not enough as the primary control model.

## ActionCommand Structure

`ActionCommand` is the phase-1 internal DSL for recommended actions.

It carries:

- `type`
- `targetLayer`
- `targetService`
- `riskLevel`
- `blastRadius`
- `preconditions`
- `forbiddenIf`
- `approvalPolicy`
- `rollbackPolicy`
- `verificationPolicy`
- `paymentSafety`
- `humanReadableDescription`

The human-readable `action` string stays in place for UI, LLM output, and backward compatibility.
The server now uses `ActionCommand` for structured control.

## Policy Engine Flow

Current flow:

1. `RunbookCandidateSelector` creates string action plus `ActionCommand`
2. `ActionPolicyEngine` evaluates each candidate
3. Denied actions are removed from recommendation candidates
4. Denied reasons are added to forbidden actions
5. Allowed actions move into `RecommendationAssembler`
6. `ActionCommandGuardrail` verifies that the structured command survives through response generation

## Phase 1 Scope

Phase 1 intentionally stays small:

- hardcoded policy evaluation
- structured action creation for the first runbook actions
- guardrail validation for command presence and required control fields
- no execution support
- no automatic remediation

## Current Policies

- `SCALE_OUT_WORKER` is denied when forbidden evidence flags are present
- payment-critical action types are denied
- high-risk actions must require approval
- rollback-required actions must have rollback support
- verification-required actions must have verification checks

## Important Boundary

This does not create automatic execution.
The AI still does not execute actions.
The DSL is only an internal control model for recommendation, approval, verification, and rollback safety.

## Next Phase

Planned next-step expansions:

- YAML-based policy definitions
- DB persistence for command metadata
- richer evidence-to-policy flag mapping
- stronger action lifecycle tracking across recommendation, approval, and postmortem
