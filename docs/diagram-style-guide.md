# Diagram Style Guide

## Purpose

Consistent, interview-ready diagrams using Mermaid across interview notes and implementation logs.

## Conventions

- Titles: short and action-oriented
- Actors/nodes: stable names (`SPA`, `GW`, `KC`, `OrderSvc`, `PaymentSvc`)
- Always include `X-Request-Id` when relevant to flows
- Prefer:
  - sequenceDiagram for request/response flows
  - graph LR/TD for component and data flows
  - flowchart for decision trees/incident flows
  - stateDiagram for lifecycles
- Keep labels minimal; move details to surrounding text

## Templates

### Sequence (HTTP + Auth)
```mermaid
sequenceDiagram
  autonumber
  participant SPA as React SPA
  participant KC as Keycloak
  participant GW as Gateway
  participant SVC as Service

  SPA->>KC: Auth (PKCE: code_challenge S256)
  KC-->>SPA: Auth code
  SPA->>KC: Token (code + code_verifier)
  KC-->>SPA: Access/Refresh tokens
  SPA->>GW: Request (Authorization: Bearer, X-Request-Id)
  GW->>GW: Validate JWT (issuer, aud, exp)
  GW->>SVC: Forward + security headers
  SVC-->>GW: Response
  GW-->>SPA: Response
```

### Component (Gateway Security Perimeter)
```mermaid
graph LR
  Client[Client / SPA]
  GW[API Gateway]
  KC[Keycloak]
  PS[Product Service]
  CS[Cart Service]
  OS[Order Service]
  PayS[Payment Service]

  Client -->|HTTPS| GW
  GW -->|OIDC/JWT Validate| KC
  GW -->|lb://| PS
  GW -->|lb://| CS
  GW -->|lb://| OS
  GW -->|lb://| PayS
```

### Flow (Incident Escalation)
```mermaid
flowchart TD
  A[Violation Detected] --> B{Severity >= Threshold?}
  B -- No --> C[Log + Metrics]
  B -- Yes --> D[Block Origin]
  D --> E[Audit Event]
  D --> F[Notify Security]
  E --> G[Review]
  F --> G
```


