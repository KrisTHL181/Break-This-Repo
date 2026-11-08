# Word-break opportunity fixture

The long token below includes an explicit `<wbr>` opportunity:

supercalifragilistic<wbr>expialidocious

Narrow layouts may break at that point; wide layouts may keep the word on one
line. The element adds no script, link, or external resource—it only exposes
word-wrapping differences.
