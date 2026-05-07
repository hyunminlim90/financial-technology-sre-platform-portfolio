# Admin API Hardening

## Protected paths

- /internal/admin/**
- /internal/alerts/**

## Protected Internal Paths

```text
/internal/admin/**
/internal/alerts/**
```

## Alert Webhook Boundary

`/internal/alerts/**` endpoints are internal operational ingestion points.

They are NOT public product APIs.

They must be protected by at least one of:

- internal-only routing
- shared secret header
- Authorization bearer token
- NetworkPolicy
- mTLS / service mesh policy

Alert ingestion must never trigger automatic remediation.

Alert ingestion may only generate recommendations.

## Application-level protection

- `agent.admin.security.enabled=false` by default
- when enabled, requests must include `X-FIN-SRE-INTERNAL-ADMIN`
- header value must be injected by environment variable
- never commit the secret to Git

## Network-level protection

The app-level filter is not the only protection.

Istio/Cloudflare must not expose `/internal/admin/**` or `/internal/alerts/**` to public users.

Recommended controls:

- exclude `/internal/admin/**` from public VirtualService
- exclude `/internal/alerts/**` from public VirtualService
- or expose it only through an internal Gateway
- optionally protect with Cloudflare Access
- restrict by NetworkPolicy where possible

## Principle

Public route exposure must fail closed.
