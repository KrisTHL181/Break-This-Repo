# Invalid YAML fixture

This fenced block is labeled `yaml` but its list indentation is intentionally
inconsistent. Strict YAML readers should reject it; tolerant tooling may report
a surprising tree.

```yaml
repository: break-this-repo
experiments:
  - broken-link
   - extra-indent
```

It is a documentation-only sample. Nothing in the repository loads this block
as configuration, and it contains no credentials or executable instructions.
