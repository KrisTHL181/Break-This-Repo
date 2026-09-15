# Helix anonymous telemetry

This crate defines the typed JSON events sent by the Helix CLI, server, and
embedded runtime to:

```text
POST https://telemetry.helix-db.com/v1/events
Content-Type: application/json
```

`HELIX_TELEMETRY_ENDPOINT` overrides the endpoint. The only accepted sources
are `helix-cli`, `helix-server`, and `helix-embedded`.

## Privacy and limits

Query events include the canonical query AST, latency, outcome, errors or
warnings, planner diagnostics, optional `cluster_id` from `HELIX_CLUSTER_ID`,
and optional `tenant_id` from the query request's `x-helix-tenant-id` header.
Embedded requests have no transport tenant header and omit `tenant_id`. Events
never include returned rows, parameters, embeddings, or email. `full`
telemetry may include `user_id`; `basic` strips it and `off` disables
telemetry.

The client enforces the ingestion contract before sending:

- at most 500 events per envelope;
- at most 1 MiB per envelope;
- at most 16 KiB per event properties object.

Server and embedded recording is bounded and non-blocking. Delivery failures do
not affect query execution or change public query responses.

## CLI durability

The CLI atomically writes complete envelopes to
`~/.helix/metrics/spool/<uuid>.json` before delivery. A file is removed after
any HTTP response, including non-202 responses, because only a request with no
response is safe to retry. Pending files survive timeouts and are pruned after
30 days or when the spool exceeds 16 MiB. Obsolete protobuf, partial, rejected,
and legacy daily telemetry files are discarded.

Authenticated `GatewayWfeService.SendQueryEvents` is not part of this crate.
That cloud-only transport is owned by the gateway in `helix-hyperscale`.
