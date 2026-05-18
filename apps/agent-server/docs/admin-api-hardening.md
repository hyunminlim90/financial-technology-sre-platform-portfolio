# Internal Operational API Hardening Policy

## Protected paths

- /internal/admin/**
- /internal/alerts/**
- /internal/recommendations/**
- /internal/execution-plans/**
- /internal/execution-results/**
- /internal/verification-results/**
- /internal/postmortem-drafts/**
- /internal/learning-candidates/**
- /internal/knowledge-promotion-plans/**
- /internal/knowledge-updates/**

## Protected Internal Paths

```text
/internal/admin/**
/internal/alerts/**
/internal/recommendations/**
/internal/incidents/**
/internal/execution-plans/**
/internal/execution-results/**
/internal/verification-results/**
/internal/postmortem-drafts/**
/internal/learning-candidates/**
/internal/knowledge-promotion-plans/**
/internal/knowledge-updates/**
```

## Application-level Boundary

All protected internal paths are guarded by:

```text
InternalOperationalApiFilter
```

Configuration:

```yaml
agent:
  internal:
    security:
      enabled: false
      require-header: true
      header-name: X-FIN-SRE-INTERNAL
```

Fail-closed behavior:

- disabled -> 404
- missing/invalid secret -> 403
- valid shared secret -> pass

## Recommendation Query Boundary

`/internal/recommendations/**` endpoints expose operational decision records.

They are NOT public product APIs.

Recommendation records must not contain:

- full alert webhook payload
- full LLM prompt
- payment payload
- sensitive customer data

## Network-level protection

The app-level filter is not the only protection.

Istio/Cloudflare must not expose `/internal/admin/**`, `/internal/alerts/**`, `/internal/recommendations/**`, `/internal/execution-plans/**`, `/internal/execution-results/**`, `/internal/verification-results/**`, `/internal/postmortem-drafts/**`, `/internal/learning-candidates/**`, `/internal/knowledge-promotion-plans/**`, or `/internal/knowledge-updates/**` to public users.

Recommended controls:

- exclude `/internal/admin/**` from public VirtualService
- exclude `/internal/alerts/**` from public VirtualService
- exclude `/internal/recommendations/**` from public VirtualService
- exclude `/internal/execution-plans/**` from public VirtualService
- exclude `/internal/execution-results/**` from public VirtualService
- exclude `/internal/verification-results/**` from public VirtualService
- exclude `/internal/postmortem-drafts/**` from public VirtualService
- exclude `/internal/learning-candidates/**` from public VirtualService
- exclude `/internal/knowledge-promotion-plans/**` from public VirtualService
- exclude `/internal/knowledge-updates/**` from public VirtualService
- or expose it only through an internal Gateway
- optionally protect with Cloudflare Access
- restrict by NetworkPolicy where possible

## Principle

Public route exposure must fail closed.
