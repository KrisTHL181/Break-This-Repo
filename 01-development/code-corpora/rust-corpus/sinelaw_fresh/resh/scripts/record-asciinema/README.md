# Asciinema demo recorder

Regenerates `homepage/public/fresh-demo.cast`, the recorded Fresh demo
played inside the hero on the landing page via
`homepage/public/vendor/asciinema-player/`.

## Files

| Path | Purpose |
| --- | --- |
| `record.py` | Python driver. Forks a pty, runs `fresh`, sends a canned key timeline, writes asciicast v2 to disk. |
| `setup-demo.sh` | Creates a small Rust project (main.rs, notes.md, Cargo.toml) with a git history and a local uncommitted edit so the git gutter and Review Diff have content to show. |
| `regenerate.sh` | One-shot: builds fresh if needed, sets up the demo workspace, runs the recorder. |

## Regenerate the demo

```sh
scripts/record-asciinema/regenerate.sh
```

The cast lands at `homepage/public/fresh-demo.cast` (~270 KB, ~35 s).
Rebuild the site with `bun run build` to deploy.

## Override paths / binary

```sh
FRESH=/path/to/fresh \
DEMO_DIR=/tmp/my-demo \
OUTPUT=/tmp/custom.cast \
  scripts/record-asciinema/regenerate.sh
```

Or call the Python driver directly:

```sh
scripts/record-asciinema/setup-demo.sh /tmp/demo
scripts/record-asciinema/record.py /tmp/out.cast \
    --fresh ./target/debug/fresh \
    --demo /tmp/demo
```

## What the demo shows

1. `main.rs` open with Rust syntax highlighting and a dirty git marker.
2. The command palette (Ctrl+P) and its command catalogue.
3. `Select Theme` with live-preview theme cycling.
4. Fuzzy file finder opening `notes.md`, then buffer switcher back.
5. Multi-cursor (Ctrl+D ×3) on the word `pub`.
6. `Live Grep (Find in Files)` with its split preview panel.
7. Magit-style `Review Diff` with hunk navigation and `s` to stage.

## Editing the timeline

The sequence lives in `record.py` under the `TIMELINE = []` block. Each
tuple is `(delay_before_event_seconds, bytes_to_send)`. An empty payload
is a pure pause.

Before adding a new step, drive it manually in tmux to make sure the
command name, key binding and visible effect match your expectation:

```sh
tmux new-session -d -s f -x 110 -y 30 \
    "cd /tmp/fresh-demo-workspace && ./target/debug/fresh main.rs"
tmux send-keys -t f C-p
tmux capture-pane -t f -p       # read the visible pane
```

When the cast doesn't match what you meant, it's almost always one of:

- the palette defaults to `>command` mode — use Backspace to fall into
  file-finder mode,
- `Review Diff` is a real command but `review diff` / `quit` might not
  match what you expect — verify with the palette first,
- multi-cursor needs a prior selection: `Ctrl+Shift+Right` selects the
  word, then `Ctrl+D` extends to the next match.
