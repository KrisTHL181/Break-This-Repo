use std::{
    path::Path,
    sync::{Arc, Mutex},
};

use async_trait::async_trait;
use codewhale_config::ConfigToml;
use codewhale_core::Runtime;
use codewhale_execpolicy::{AskForApproval, ExecPolicyEngine};
use codewhale_hooks::{HookDispatcher, HookEvent, HookSink};
use codewhale_mcp::McpManager;
use codewhale_protocol::{ToolKind, ToolOutput, ToolPayload};
use codewhale_state::StateStore;
use codewhale_tools::{
    FunctionCallError, ToolCall, ToolCallSource, ToolDescriptor, ToolHandler, ToolInvocation,
    ToolRegistry,
};
use serde_json::json;
use uuid::Uuid;

struct FixtureTool {
    kind: ToolKind,
    output: ToolOutput,
}

#[async_trait]
impl ToolHandler for FixtureTool {
    fn kind(&self) -> ToolKind {
        self.kind
    }

    async fn handle(&self, _invocation: ToolInvocation) -> Result<ToolOutput, FunctionCallError> {
        Ok(self.output.clone())
    }
}

#[derive(Default)]
struct RecordingSink(Mutex<Vec<HookEvent>>);

#[async_trait]
impl HookSink for RecordingSink {
    async fn emit(&self, event: &HookEvent) -> anyhow::Result<()> {
        self.0
            .lock()
            .expect("recording hook lock")
            .push(event.clone());
        Ok(())
    }
}

async fn invoke_fixture(
    name: &str,
    kind: ToolKind,
    payload: ToolPayload,
    output: ToolOutput,
) -> (serde_json::Value, Vec<HookEvent>) {
    let mut registry = ToolRegistry::default();
    registry
        .register(
            ToolDescriptor {
                name: name.into(),
                input_schema: json!({"type":"object"}),
                output_schema: json!({"type":"object"}),
                supports_parallel_tool_calls: true,
                timeout_ms: None,
            },
            Arc::new(FixtureTool { kind, output }),
        )
        .expect("register fixture tool");

    let recording = Arc::new(RecordingSink::default());
    let mut hooks = HookDispatcher::default();
    hooks.add_sink(recording.clone());
    let state_path = std::env::temp_dir().join(format!(
        "codewhale-core-tool-success-{name}-{}.db",
        Uuid::new_v4().simple()
    ));
    let runtime = Runtime::new(
        ConfigToml::default(),
        StateStore::open(Some(state_path)).expect("open temporary state"),
        Arc::new(registry),
        Arc::new(McpManager::default()),
        ExecPolicyEngine::new(vec![], vec![]),
        hooks,
    );
    let result = runtime
        .invoke_tool(
            ToolCall {
                name: name.into(),
                payload,
                source: ToolCallSource::Direct,
                raw_tool_call_id: None,
            },
            AskForApproval::Never,
            Path::new("/tmp/codewhale"),
        )
        .await
        .expect("application failure remains a transport-successful tool result");
    let events = recording.0.lock().expect("recording hook lock").clone();
    (result, events)
}

fn assert_failed_lifecycle(events: &[HookEvent], expected_tool: &str) {
    let terminal = events
        .iter()
        .find_map(|event| match event {
            HookEvent::ToolLifecycle {
                tool_name,
                phase,
                payload,
                ..
            } if tool_name == expected_tool && phase == "failed" => Some(payload),
            _ => None,
        })
        .expect("failed application lifecycle hook");
    assert_eq!(terminal["ok"], false);
}

#[tokio::test]
async fn invoke_tool_preserves_application_failure_as_a_tool_result() {
    let (result, events) = invoke_fixture(
        "application_failure_tool",
        ToolKind::Function,
        ToolPayload::Function {
            arguments: "{}".into(),
        },
        ToolOutput::Function {
            body: Some(json!({"message": "application failure remains visible"})),
            success: false,
        },
    )
    .await;

    assert_eq!(result["ok"], false);
    assert_eq!(result["status"], "failed");
    assert!(result.get("error").is_none());
    assert_eq!(result["output"]["type"], "function");
    assert_eq!(result["output"]["success"], false);
    assert_eq!(
        result["output"]["body"]["message"],
        "application failure remains visible"
    );
    assert_eq!(result["events"][1]["event"], "tool_call_result");
    assert_eq!(result["events"][1]["output"]["success"], false);
    assert_failed_lifecycle(&events, "application_failure_tool");
}

#[tokio::test]
async fn invoke_tool_fails_closed_for_malformed_mcp_error_metadata() {
    let malformed_result = json!({
        "content": [{"type": "text", "text": "malformed failure remains visible"}],
        "isError": "unknown"
    });
    let (result, events) = invoke_fixture(
        "malformed_mcp_failure_tool",
        ToolKind::Mcp,
        ToolPayload::Mcp {
            server: "fixture".into(),
            tool: "malformed".into(),
            raw_arguments: json!({}),
            raw_tool_call_id: None,
        },
        ToolOutput::Mcp {
            result: malformed_result,
        },
    )
    .await;

    assert_eq!(result["ok"], false);
    assert_eq!(result["status"], "failed");
    assert!(result.get("error").is_none());
    assert_eq!(result["output"]["type"], "mcp");
    assert_eq!(result["output"]["result"]["isError"], "unknown");
    assert_eq!(
        result["output"]["result"]["content"][0]["text"],
        "malformed failure remains visible"
    );
    assert_eq!(result["events"][1]["event"], "tool_call_result");
    assert_eq!(
        result["events"][1]["output"]["result"]["isError"],
        "unknown"
    );
    assert_failed_lifecycle(&events, "malformed_mcp_failure_tool");
}
