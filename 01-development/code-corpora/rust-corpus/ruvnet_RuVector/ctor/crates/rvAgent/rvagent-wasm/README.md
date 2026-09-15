# rvagent-wasm

WASM bindings for rvAgent — run AI agents entirely in the browser or Node.js.

## Features

- **WasmAgent** — Full agent execution in browser/Node.js with conversation history
- **WasmMcpServer** — MCP JSON-RPC server running in the browser (no backend required)
- **Virtual Filesystem** — In-memory file operations for sandboxed execution
- **Gallery System** — Built-in agent templates with RVF container export
- **Zero Dependencies** — Runs entirely client-side via WebAssembly

## Installation

```bash
npm install @ruvector/rvagent-wasm
```

## Building from Source

```bash
cd crates/rvAgent/rvagent-wasm

# Node.js target (used by ruflo / @claude-flow/cli >= 3.10.4)
wasm-pack build --target nodejs --release

# Browser target
wasm-pack build --target web --release
```

## Usage

### WasmAgent (Node.js / ruflo integration)

```javascript
// Node.js CommonJS (used by ruflo / @claude-flow/cli)
const { WasmAgent, JsModelProvider } = require('@ruvector/rvagent-wasm/rvagent_wasm.js');

// Browser ESM
// import init, { WasmAgent } from '@ruvector/rvagent-wasm';
// await init();

// Create an agent
const agent = new WasmAgent(JSON.stringify({
  model: "anthropic:claude-sonnet-4-20250514",
  name: "my-agent",
  instructions: "You are a helpful coding assistant.",
  max_turns: 50
}));

// Wire a real LLM via JsModelProvider (ADR-129 Gap 1 pattern)
const provider = new JsModelProvider(async (messagesJson) => {
  const messages = JSON.parse(messagesJson);
  const response = await callYourLLM({ messages });
  return JSON.stringify({ role: 'assistant', content: response.content });
});
agent.set_model_provider(provider.complete.bind(provider));

// Send a prompt
const result = await agent.prompt("Write a hello world function");
console.log(result.response);

// Execute tools directly
agent.execute_tool('{"tool": "write_file", "path": "hello.js", "content": "console.log(\"Hello!\");"}');

// Check state
console.log(agent.turn_count());      // 1
console.log(agent.file_count());      // 1
console.log(agent.get_todos());       // []
```

### WasmMcpServer

Run an MCP server entirely in the browser:

```javascript
const { WasmMcpServer } = require('@ruvector/rvagent-wasm/rvagent_wasm.js');

const mcp = new WasmMcpServer("rvagent-wasm");

// Handle MCP JSON-RPC requests
const response = mcp.handle_request(JSON.stringify({
  jsonrpc: "2.0",
  id: 1,
  method: "initialize",
  params: {}
}));

// List available tools
const tools = mcp.list_tools();

// Call a tool
const result = mcp.call_tool("write_file", JSON.stringify({
  path: "demo.txt",
  content: "Hello from WASM!"
}));
```

### Gallery System

Access built-in agent templates directly:

```javascript
const { WasmGallery } = require('@ruvector/rvagent-wasm/rvagent_wasm.js');

const gallery = new WasmGallery();

// List all templates
const templates = gallery.list();

// Search templates
const results = gallery.search("security testing");

// Get template details
const coder = gallery.get("coder");

// Load as RVF container
const rvfBytes = gallery.loadRvf("coder"); // Uint8Array
```

## Available Tools

| Tool | Description |
|------|-------------|
| `read_file` | Read file from virtual filesystem |
| `write_file` | Write file to virtual filesystem |
| `edit_file` | Apply string replacement to a file |
| `list_files` | List all files in virtual filesystem |
| `write_todos` | Manage todo list |

**Note:** OS-level tools (`execute`, `glob`, `grep`) are intentionally omitted as they require system access unavailable in the browser sandbox.

## MCP Methods

| Method | Description |
|--------|-------------|
| `initialize` | Initialize MCP connection |
| `ping` | Health check |
| `tools/list` | List available tools |
| `tools/call` | Execute a tool |
| `resources/list` | List virtual filesystem as resources |
| `prompts/list` | List prompts from active template |
| `gallery/list` | List all agent templates |
| `gallery/search` | Search templates by query |
| `gallery/get` | Get template details |
| `gallery/load` | Load template as active config |
| `gallery/configure` | Apply config overrides |
| `gallery/categories` | List template categories |

## API Reference

### WasmAgent

| Method | Description |
|--------|-------------|
| `new(configJson)` | Create agent from JSON config |
| `set_model_provider(callback)` | Set JS callback for LLM calls |
| `prompt(input)` | Send prompt, get response (async) |
| `execute_tool(toolJson)` | Execute a tool directly |
| `get_state()` | Get conversation state as JSON |
| `get_todos()` | Get todo list as JSON |
| `get_tools()` | Get available tools |
| `reset()` | Clear state and start fresh |
| `version()` | Get crate version |
| `name()` | Get agent name |
| `model()` | Get model identifier |
| `turn_count()` | Get current turn count |
| `is_stopped()` | Check if agent is stopped |
| `file_count()` | Get virtual filesystem file count |

### WasmMcpServer

| Method | Description |
|--------|-------------|
| `new(name)` | Create MCP server |
| `handle_request(json)` | Handle JSON-RPC request |
| `list_tools()` | Get available tools as JSON |
| `call_tool(name, paramsJson)` | Call tool by name |
| `gallery()` | Get gallery info |
| `is_initialized()` | Check initialization status |
| `name()` | Get server name |
| `version()` | Get server version |

## Development

```bash
# Install wasm-pack
cargo install wasm-pack

# Build for Node.js (published artifact)
wasm-pack build --target nodejs --release

# Build for web
wasm-pack build --target web --release

# Run Rust unit tests (61 tests, no WASM runtime needed)
cargo test

# Run WASM tests in browser
wasm-pack test --headless --chrome
```

## Security

- Request size limit: 100 KB
- Path length limit: 256 characters
- Content length limit: 1 MB
- Path traversal (`..`) blocked
- Todo count limit: 1000 items

## Architecture

```
rvagent-wasm/
├── src/
│   ├── lib.rs        # WasmAgent — main agent type
│   ├── backends.rs   # WasmStateBackend — virtual filesystem
│   ├── bridge.rs     # JsModelProvider — JS interop
│   ├── gallery.rs    # WasmGallery — template system
│   ├── mcp.rs        # WasmMcpServer — MCP protocol
│   ├── rvf.rs        # RVF container support
│   └── tools.rs      # Tool definitions and executor
└── pkg/              # Built WASM package
    ├── rvagent_wasm.js
    ├── rvagent_wasm.d.ts
    └── rvagent_wasm_bg.wasm
```

## ruflo / @claude-flow/cli Integration

Compatible with `@claude-flow/cli >= 3.10.4`. ADR-129 closes two gaps in the ruflo-side wiring:

- **Gap 1**: Wire `JsModelProvider` so `wasm_agent_prompt` uses a real LLM (not echo stub)
- **Gap 2**: Call `WasmRvfBuilder.addMcpTools()` in `buildRvfContainer` and expose `wasm_agent_compose`

All needed methods (`JsModelProvider`, `set_model_provider`, `addMcpTools`, `get_state`,
`get_todos`, `reset`) are implemented in this WASM package. The gaps are purely TypeScript
wiring issues in the ruflo consumer layer.

See [ADR-129](https://github.com/ruvnet/ruflo/blob/main/v3/docs/adr/ADR-129-rvagent-full-integration.md).

## Related Crates

| Crate | Description |
|-------|-------------|
| `rvagent-core` | Agent state, graph, config |
| `rvagent-backends` | Backend protocol + implementations |
| `rvagent-tools` | Full tool implementations |
| `rvagent-mcp` | Native MCP client/server |
| `rvagent-cli` | Terminal UI |

## License

MIT OR Apache-2.0
