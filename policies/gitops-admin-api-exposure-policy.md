# Internal Admin API Exposure Policy

## Protected Paths

The following paths must NEVER be exposed publicly:

```text
/internal/admin/**
```

---

## Operational Principle

Internal admin APIs are operational control interfaces.

They are NOT public product APIs.

These APIs exist for:
- operational maintenance
- knowledge ingestion
- vector reindex
- runtime operational control

They must remain:
- restricted
- auditable
- human-controlled

---

## Exposure Model

### Allowed

```text
Operator
→ Internal route
→ Internal Admin API
```

### Future Recommended Topology

```text
Operator Browser
→ Zero Trust / MFA
→ Internal SRE Console Backend
→ /internal/admin/**
```

---

## Forbidden Exposure

The following are prohibited:

- public browser direct access
- wildcard public ingress
- public Cloudflare hostname
- bypass around Policy/Guardrail
- direct external internet exposure

---

## Required Protection Layers

### 1. Application Layer

Protected by:
- InternalAdminApiFilter
- internal admin header validation
- disabled by default configuration

---

### 2. Istio Layer

Public gateways:
- MUST NOT expose `/internal/admin/**`

Internal gateways:
- MAY expose `/internal/admin/**`

---

### 3. Cloudflare / Edge Layer

Public internet exposure is prohibited.

Recommended:
- Zero Trust Access
- MFA
- restricted operator identity
- internal-only routing

---

### 4. GitOps Review Policy

GitOps review MUST reject:

- wildcard ingress exposing admin paths
- public VirtualService exposing `/internal/admin/**`
- unrestricted public gateway mappings

---

## Forbidden Examples

### ❌ Wildcard public exposure

```yaml
hosts:
  - "*"
```

```yaml
match:
  - uri:
      prefix: /
```

This may accidentally expose:

```text
/internal/admin/**
```

---

### ❌ Public Cloudflare hostname

```text
admin.example.com
```

must NOT route directly to admin APIs.

---

## Human-in-the-loop Principle

Internal admin APIs:
- must remain operator-triggered
- must never be AI-triggered
- must preserve PolicyEngine and Guardrail boundaries

AI recommendation is allowed.

AI operational execution is prohibited.