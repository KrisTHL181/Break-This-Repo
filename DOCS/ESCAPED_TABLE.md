# Escaped-table fixture

The first row contains a literal pipe inside a cell; the second puts the pipe
inside a code span. Both should remain one cell in a Markdown-aware renderer.

| expression | meaning |
| --- | --- |
| `cat \| dog` | escaped separator |
| `cat | dog` | code span with a pipe |

If a parser splits either value into extra columns, the mismatch is visible in
the preview. This is an isolated text fixture with no executable content.
