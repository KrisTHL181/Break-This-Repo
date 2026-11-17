# dsh-acp-tui

HDSL-web's first-party terminal interface for DeepSeek Harness.

`dsh-acp-tui` is a terminal front end in the spirit of kimi-code's: a transcript of the
conversation, a composer at the bottom, a status row with the model, the working
directory, the git branch and context usage, tool cards with their output, a
thinking block, pickers for models and sessions, and a permission dialog.

It is also the **absolute compatibility option**: the panel keeps it as the fallback for
an instance whose dsh version moves. It may be installed next to
[`dsh-inproc-tui`](../dsh-inproc-tui/README.md); while that one fits the instance this one
stays dormant, and it takes over by itself when the instance's version stops matching.

It is **not** an in-process harness plugin. It starts the harness as a child process
and speaks [Agent Client Protocol](https://agentclientprotocol.com) v1 to it over
newline-delimited JSON-RPC:

```
dsh-acp-tui ──spawn──▶ node <dsh>/lib/bin.js --profile acp
         ◀─stdio──▶ ACP: initialize / session/new / session/prompt / session/update …
```

That choice is the compatibility story. ACP is the harness's documented client
surface; no harness package is imported, so a new dsh release (or a different
version on a different panel instance) does not change a single line here. Features
the harness does not advertise degrade instead of crashing: without `session/list`
`/resume` says there is nothing to resume, without `session/set_config_option`
`/model` reports the harness cannot switch, and without reasoning chunks the
thinking block simply stays empty.

## Install

Into a Harness profile of its own — a terminal interface is an *app* plugin and must
not share the `web` profile with the panel's web app:

```sh
dsh plugin --profile dsh-acp-tui add @nas200-timmy/dsh-acp-tui
```

The panel does this for you: the instance's **安装** page lists it as a bundled
terminal interface and installs it from the tarball inside the panel jar, so the
install works offline.

## Run

```sh
dsh-acp-tui                     # new session in the current directory
dsh-acp-tui --resume <session>  # continue an earlier session
```

When the panel starts it, it passes `HSDL_DSH_ENTRY`, the path of that instance's
`@deepseek-ai/dsh/lib/bin.js`. Started by hand, point the variable at the harness
you want to drive (or put `dsh` on `PATH`).

| Variable | Meaning |
|---|---|
| `HSDL_DSH_ENTRY` | The harness entry file to spawn (also accepts `DSH_ENTRY`) |
| `HSDL_TUI_ACP_PROFILE` | The profile that speaks ACP (default `acp`) |
| `HSDL_TUI_LANG` | `zh` or `en` (defaults to the terminal locale) |
| `HSDL_TUI_THEME` | `dark` or `light` (defaults to the terminal background) |
| `HSDL_TUI_DEBUG` | Print protocol notes to the terminal |
| `NO_COLOR` | Disable colour |

## Keys

| Key | Action |
|---|---|
| `enter` | Send (queues while the agent is working) |
| `ctrl+j` / `shift+enter` | Newline |
| `esc` | Interrupt the current turn, or close a dialog |
| `ctrl+c` | Interrupt, then clear the composer, then quit on a second press |
| `ctrl+d` | Quit when the composer is empty |
| `ctrl+o` | Expand or collapse tool output and thinking |
| `pgup` / `pgdn` | Scroll the transcript |
| `up` / `down` | History (single-line composer) or move the caret |
| `tab` | Complete a `/command` |

Commands: `/help`, `/model`, `/effort`, `/resume`, `/new`, `/clear`, `/quit`.

## Design notes

- **Zero dependencies.** No bundler, no transpiler, no runtime packages: the whole
  terminal is hand-written against Node's `fs`, `child_process` and `stream`.
  `npm run check` is the build — it syntax-checks every file and refuses a manifest
  that declares dependencies.
- **The renderer is a cell grid.** Frames are arrays of styled rows of exactly the
  terminal width; the painter diffs rows and anchors each one with an absolute
  cursor position, so a wide CJK character or a resize cannot desynchronise the
  screen. This is also what keeps the panel's web-shell cheap: only changed rows go
  over the WebSocket.
- **No terminal feature a browser terminal lacks.** Alternate screen, bracketed
  paste, SGR colour, `CSI H`/`K`, `CSI ?2026` (synchronised output, ignored where
  unsupported) — no kitty keyboard protocol, no mouse mode, no sixel.
- **Streaming is honest.** ACP commits a message before it is delivered, so text
  arrives in blocks; the transcript reveals a block progressively so a long answer
  reads as it would from a token stream, without inventing content.

## Licence

GPL-3.0-or-later, like the panel it ships with.
