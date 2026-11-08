# Whitespace fixture

These two lines are intentionally similar but not byte-identical. The first
uses four spaces after the label; the second uses one tab.

```text
spaces:    four columns
tabs:	one tab character
```

The code fence keeps the characters visible instead of asking a parser to
interpret them as indentation. Reviewers can inspect the raw file or a hex
viewer to see the difference. There is no executable snippet here.
