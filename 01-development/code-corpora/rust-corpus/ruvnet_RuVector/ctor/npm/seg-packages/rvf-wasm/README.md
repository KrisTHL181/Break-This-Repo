# @ruvector/rvf-wasm

RuVector Format (RVF) WASM build for browsers and edge functions. Query vectors directly in the browser with zero backend.

## Install

```bash
npm install @ruvector/rvf-wasm
```

## Usage

`@ruvector/rvf-wasm` itself is a low-level C-ABI module (raw `rvf_*` exported
functions operating on integer handles and WASM linear-memory pointers) — it
does **not** export a `WasmRvfStore` class or any other high-level wrapper.
For ergonomic browser usage, install `@ruvector/rvf` and use its `wasm`
backend, which wraps this module for you:

```bash
npm install @ruvector/rvf
```

```javascript
import { RvfDatabase } from '@ruvector/rvf';

const db = await RvfDatabase.create('unused-in-wasm', { dimensions: 384 }, 'wasm');
await db.ingestBatch([{ id: 'a', vector: new Float32Array(384) }]);
const results = await db.query(new Float32Array(384), 10);
console.log(results); // [{ id, distance }]

// The WASM backend is in-memory only (no filesystem access). To persist a
// store across page loads, serialize it to bytes and save them yourself
// (e.g. to IndexedDB/OPFS), then reload with RvfDatabase.openBytes():
const bytes = await db.exportBytes();
// ...write `bytes` to IndexedDB/OPFS...
const reopened = await RvfDatabase.openBytes(bytes, 'wasm');
```

See [#705](https://github.com/ruvnet/RuVector/issues/705) for background: an
earlier version of this README described an unshipped `WasmRvfStore` API.

## Features

- ~46 KB control plane (full store API)
- ~5.5 KB tile microkernel (query-only)
- In-memory store with HNSW indexing
- Segment inspection and status
- No backend required

## License

MIT
