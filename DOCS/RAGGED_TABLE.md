# Ragged table

This table intentionally gives one row more pipe-separated cells than the
header advertises. Markdown renderers disagree on whether to discard the
extra cells, display them, or treat the row as plain text.

| file | mood |
| --- | --- |
| `README.md` | protected above the marker |
| `DOCS/` | fair game | extra cell |
| `assets/` | pictures, words, and experiments |

The mismatch is confined to this example and contains no data that a program
should consume. It exists to make parser behavior visible in a diff.
