# dsh-inproc-tui

HDSL-web's **in-process** terminal interface for DeepSeek Harness 0.2.x — the
fuller sibling of [`dsh-acp-tui`](../dsh-acp-tui/README.md).

`dsh-acp-tui` drives the harness from outside over ACP, which is why it runs on
0.1.x too. This one is a **mode bundle**: its patch is layered over `dsh-base`,
the terminal runs inside the harness process, and it talks to the agent registry,
the live session log and the assistant stream directly. That is the whole trade —
it buys token-level streaming, live reasoning, approval prompts, questions and
in-session model switching, and it gives up the right to run on an older harness.

## What it does

- **Token-level text** — the answer appears as the model writes it, not when the
  message commits (`agent/assistant-stream`, the harness's own delta feed).
- **Live reasoning** — reasoning deltas stream into a thinking block; the durable
  message is the fallback when a provider sends no deltas.
- **Tool cards from the harness's own tools** — `tool/call` / `tool/result`
  events become cards with argument preview, duration, output and diff tinting.
  It adds no tools of its own; the tool surface is whatever the profile composed.
- **Approvals** — answers `approval/request` with a dialog (allow once / reject)
  for the agent this terminal drives; another agent's request is delegated down
  the waterfall (a sub-agent, the host's own answerer).
- **Questions** — answers `user-questions/request` the same way: only this
  agent's `ask_user_question`; agentless host questions (`/auth`) are delegated.
- **Model and effort switching** — the provider/model catalogue from `ctx.llm`,
  written through `agent.model.select` so it reaches the next model step.
- **Sessions** — `/resume` lists stored sessions (`sessionPersistence.list`) and
  resumes one; `/new` starts a fresh conversation; `/compact` runs the profile's
  compaction service.
- **Queueing, cancellation, scrolling, `ctrl+o`** — same key map as `dsh-acp-tui`.

## Install and run

```sh
dsh plugin --profile dsh-inproc-tui add @nas200-timmy/dsh-inproc-tui
dsh --profile dsh-inproc-tui                 # new session in the current directory
dsh --profile dsh-inproc-tui --resume <id>   # resume a stored session
```

The panel does this for you from the instance's **安装** page: the tarball is
inside the panel jar, so the install needs no registry. The panel then opens the
terminal by booting this profile — a pure bundle has no executable of its own.

**0.2.x only.** The bundle patch is written against the 0.2.x service seams, so
the peer ranges name the whole line (`^0.2.0-rc.1`) rather than the rcs that
happened to exist when this build was cut. dsh judges an installation with
`includePrerelease`, so 0.2.1-rc.5 and 0.2.2-rc.3 pass that gate too; this build
was re-run against 0.2.2-rc.3. 0.3 is a new line and needs a new verification.
An older instance gets `dsh-acp-tui` instead, which speaks ACP and is meant for
exactly that case.

Both terminals may be installed side by side: this one is preferred while it fits the
instance, and `dsh-acp-tui` waits as the fallback. When the instance's dsh version moves
(down, or to a line without the 0.2.x seams), the panel starts the ACP one without any
reinstall. The list marks the row that will actually run as **active**.

## Layout

```
cordis.patch.yml     the profile layer: persona and this plugin's row (the
                     optional services sit in the row's inject, not in code)
src/index.js         Cordis plugin: name / inject / apply
src/app.js           the terminal state machine, key map and commands
src/agent.js         agent create/resume, prompt, model selection, sessions
src/protocol.js      the peer-free decisions: argv, waterfall scope, question
                     error shape (unit-tested without a harness)
test/unit.test.js    node:test unit tests for src/protocol.js
shared/              the terminal toolkit both first-party terminals use (packed
                     into the tarball by the build)
LICENSE              GPL-3.0-or-later, copied from the repository root
```

## Checking

```sh
node scripts/check.mjs   # syntax, patch present, harness-only peers, i18n parity
node --test              # unit tests (test/unit.test.js), no harness needed
npm run check            # the same
```

Both are offline. `HSDL_TUI_DEBUG=1` (or the older `HSDL_UI_DEBUG`) writes
internal failures to stderr without touching the interface.

The build packs this directory (and `plugins/shared`) into
`first-party-plugins/nas200-timmy-dsh-inproc-tui-<version>.tgz` inside the jar; see
[plugins/README.md](../README.md).

## Licence

GPL-3.0-or-later; the full text is in [LICENSE](LICENSE), copied from the
repository root.
