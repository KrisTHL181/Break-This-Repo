# TLS `BadRecordMac` transport-retry reproduction

Standalone reproduction for the bug fixed in commit
`fix(providers): retry on TLS transport faults (BadRecordMac) across all providers`.

## What it reproduces

Users on flaky networks / corrupting middleboxes / some VPNs hit:

```
Stream error: IO error: received fatal alert: BadRecordMac
```

A `BadRecordMac` is a TLS record-authentication failure. It is transient: a
fresh connection on retry almost always succeeds. The bug was that jcode failed
these immediately instead of retrying, for two reasons:

1. **OpenAI** maintained its own `is_retryable_error` allowlist that did not
   call the shared `is_transient_transport_error` and omitted every TLS term, so
   a `BadRecordMac` on the websocket path surfaced at `attempt=1`
   (`will_retry=false`).
2. **claude / copilot / openrouter / openai** classified on `e.to_string()`.
   When a transport cause is wrapped behind `anyhow`'s `.context(...)`,
   `to_string()` only returns the top-level context (e.g. `Failed to send
   request to ... API`) and **masks** the underlying `BadRecordMac`, so the
   classifier never sees it. (The anthropic provider already used
   `format!("{e:#}")` and was unaffected.)

## How it works

```
reqwest client (rustls)  ->  corrupting TCP proxy  ->  real TLS server (rustls)
```

The proxy flips one byte inside the first large client->server TLS
`application_data` record. The server's AEAD check then fails and it emits a TLS
`bad_record_mac` fatal alert, which the client surfaces exactly as users see it.

The harness then:

- wraps the error like the providers do (`.context(...)`),
- runs the **verbatim** shipping `is_transient_transport_error`,
- runs the **old** vs **new** OpenAI `is_retryable_error`,

and asserts old logic misses it while the fix retries.

## Run

```bash
cd scripts/repro/tls-bad-record-mac
cargo run
# exit 0 + "SUCCESS: realistic BadRecordMac reproduced and both fixes validated."
```

It is intentionally a standalone crate (own `[workspace]` table) so its
TLS/cert dev-dependencies do not touch the main build.
