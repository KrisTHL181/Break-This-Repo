# Mermaid glitch fixture

The fenced diagram below is intentionally incomplete. A Mermaid-capable
renderer may show an error, a blank panel, or the source text depending on its
version.

```mermaid
flowchart LR
    visitor[Visitor] --> pull_request[Pull request]
    pull_request -->
```

The unfinished edge is the entire experiment. It is isolated in a document,
contains no script that can run, and does not change the repository's own
workflow definitions.
