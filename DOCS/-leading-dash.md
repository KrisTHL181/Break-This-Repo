# Leading-dash path

This document intentionally starts with a hyphen in its filename. Command-line
tools that accept paths as options may need `--` before the path, for example:

```sh
git add -- DOCS/-leading-dash.md
```

The example is not run by the repository and changes no configuration. The
fixture only makes option/path disambiguation visible.
