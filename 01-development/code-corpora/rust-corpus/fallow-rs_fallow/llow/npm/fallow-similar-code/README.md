# fallow-similar-code

Optional first-party native sidecar used by Fallow's local similar-code analysis.
Install it through the main `fallow` package. Then run:

```bash
fallow similar-code setup --local
```

Setup downloads the exact pinned Apache-2.0 model artifacts into the user cache.
Analysis after setup is offline. Source is passed to the native sidecar only on
stdin and is never logged or cached by this package.

The wrapper only launches an exact-version `@fallow-cli/fallow-similar-code-*`
platform package after verifying its detached Ed25519 signature and the SHA-256
digest embedded in that exact platform package. Missing or mismatched integrity
metadata fails closed. The wrapper does not contain or execute a JavaScript
model runtime.
