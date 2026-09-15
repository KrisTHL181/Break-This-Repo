# @helix-db/helix-db-embedded

Native embedded runtime and graph algorithms for `@helix-db/helix-db`.

```sh
npm install @helix-db/helix-db @helix-db/helix-db-embedded
```

Applications use the public SDK:

```ts
import { Client } from "@helix-db/helix-db";

const client = await Client.embedded({
  kind: "inMemory",
  database: "app",
});
```

The package bundles native libraries for macOS arm64/x64, Linux glibc
arm64/x64, and Windows x64.
