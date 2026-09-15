# timesfm-harness

TimesFM 1.0 200M decoder-only time-series forecasting inference crate (Rust/candle) — engineering pod harness

> **Advanced Coding** — Architect → implement → review → test, with a code-index MCP and push-guarded git perms.
>
> Generated with [`create-agent-harness`](https://github.com/ruvnet/agent-harness-generator). Multi-host scaffolding with a kernel that resolves native → wasm → js (js backend in the published beta; see `harness doctor`).

## Install

```bash
npm install -g timesfm-harness
timesfm-harness init
timesfm-harness doctor
```

## Agents

| Agent | Role |
|---|---|
| `architect` | Designs the change before code is written. |
| `implementer` | Writes code that matches the surrounding style. |
| `reviewer` | Hunts correctness bugs in the diff. |
| `test-writer` | Adds the missing tests for the change. |

This harness ships with the **claude-code** adapter.

## License

MIT
