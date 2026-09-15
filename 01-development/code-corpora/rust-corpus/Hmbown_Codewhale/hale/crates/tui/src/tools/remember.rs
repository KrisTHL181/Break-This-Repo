//! `remember` tool — model-callable capture into the native memory store.
//!
//! Lets the model itself notice a durable preference, convention, or fact
//! worth keeping across sessions and write it to the native memory store
//! (Markdown + SQLite FTS5 under `memory/global/MEMORY.md`). The tool is
//! auto-approved and side-effecting only on the user-owned memory files,
//! so it doesn't get gated behind the same approval flow as shell or
//! arbitrary file writes.
//!
//! Only registered when `[memory] enabled = true` (or
//! `DEEPSEEK_MEMORY=on`). When disabled, the tool isn't surfaced to the
//! model at all, so prompts that mention `remember` simply fall through.

use async_trait::async_trait;
use serde_json::{Value, json};

use super::spec::{
    ApprovalRequirement, ToolCapability, ToolContext, ToolError, ToolResult, ToolSpec, required_str,
};

/// Tool that appends one bullet to the user memory file.
pub struct RememberTool;

#[async_trait]
impl ToolSpec for RememberTool {
    fn name(&self) -> &'static str {
        "remember"
    }

    fn description(&self) -> &'static str {
        "Curate the durable notes that surface in future sessions. Use this \
         when the user states a preference, a convention they want enforced, \
         or a fact about themselves or their workflow that you should not \
         have to relearn next time. Keep notes terse (one sentence). Don't \
         store secrets, transient tasks, or reasoning scratch — those belong \
         in a checklist or in the conversation.\n\n\
         Memory you only add to decays: corrections pile up behind the notes \
         they contradict, and what gets injected next session drifts toward \
         noise. So also maintain it. When something you were told before \
         turns out to be wrong or has changed, `revise` it; when it stops \
         being true at all, `retire` it. Both need `replaces` (the exact \
         existing note) and `evidence` (what in this session showed it), and \
         both are written to an audit log."
    }

    fn input_schema(&self) -> Value {
        json!({
            "type": "object",
            "properties": {
                "action": {
                    "type": "string",
                    "enum": ["append", "revise", "retire"],
                    "description": "append (default) adds a new note; revise replaces one \
                                    existing note; retire removes one."
                },
                "note": {
                    "type": "string",
                    "description": "The single-sentence durable note. Required for append \
                                    and revise; ignored for retire."
                },
                "replaces": {
                    "type": "string",
                    "description": "The exact text of the existing note being revised or \
                                    retired. Must match exactly one note."
                },
                "evidence": {
                    "type": "string",
                    "description": "What in this session justifies the change — the \
                                    observation, correction, or user statement. Required \
                                    for revise and retire."
                },
                "scope": {
                    "type": "string",
                    "enum": ["global", "workspace"],
                    "description": "Native backend scope; defaults to global."
                }
            },
            "required": []
        })
    }

    fn capabilities(&self) -> Vec<ToolCapability> {
        vec![ToolCapability::WritesFiles]
    }

    fn approval_requirement(&self) -> ApprovalRequirement {
        // Memory writes are scoped to the user's own memory file; gating
        // them behind the standard shell/write approval would defeat the
        // point of automatic memory.
        ApprovalRequirement::Auto
    }

    async fn execute(&self, input: Value, context: &ToolContext) -> Result<ToolResult, ToolError> {
        let action = input
            .get("action")
            .and_then(Value::as_str)
            .unwrap_or("append");
        if !matches!(action, "append" | "revise" | "retire") {
            return Err(ToolError::invalid_input(format!(
                "unknown memory action `{action}`; expected append, revise, or retire"
            )));
        }
        // `note` stays required for the paths that write one, so an omitted
        // note still fails the same way it always has rather than silently
        // recording an empty bullet.
        let note = if action == "retire" {
            String::new()
        } else {
            required_str(&input, "note")?.to_string()
        };
        let path = context.memory_path.as_ref().ok_or_else(|| {
            ToolError::execution_failed(
                "user memory is disabled — set `[memory] enabled = true` in config.toml or \
                 `DEEPSEEK_MEMORY=on` in the environment to enable",
            )
        })?;

        if let Some(store) = crate::native_memory::NativeMemoryStore::from_global_path(path) {
            let scope = match input
                .get("scope")
                .and_then(Value::as_str)
                .unwrap_or("global")
            {
                "global" => crate::native_memory::MemoryScope::Global,
                "workspace" => crate::native_memory::MemoryScope::Workspace,
                other => {
                    return Err(ToolError::invalid_input(format!(
                        "unknown memory scope `{other}`; expected global or workspace"
                    )));
                }
            };
            let workspace_id = if scope == crate::native_memory::MemoryScope::Workspace {
                Some(
                    crate::native_memory::NativeMemoryStore::workspace_id(&context.workspace)
                        .map_err(|error| {
                            ToolError::execution_failed(format!(
                                "failed to resolve workspace memory scope: {error}"
                            ))
                        })?
                        .ok_or_else(|| {
                            ToolError::execution_failed(
                                "workspace memory requires a git repository with an origin",
                            )
                        })?,
                )
            } else {
                None
            };
            let scope_label = if scope == crate::native_memory::MemoryScope::Global {
                "global"
            } else {
                "workspace"
            };

            // `revise` and `retire` edit durable context in place, so both
            // demand the exact note they target and the evidence for the
            // change. The store records each one in its journal.
            if action == "retire" {
                let replaces = required_str(&input, "replaces")?;
                let evidence = required_str(&input, "evidence")?;
                let retired = store
                    .retire(scope, workspace_id.as_deref(), replaces, evidence)
                    .map_err(|error| {
                        ToolError::execution_failed(format!("failed to retire memory: {error}"))
                    })?;
                return Ok(
                    ToolResult::success(format!("retired from native memory: {retired}"))
                        .with_metadata(json!({
                            "memory_backend": "native",
                            "action": "retire",
                            "scope": scope_label,
                            "retired": retired,
                            "journal": store.journal_path(),
                            "untrusted": true
                        })),
                );
            }

            let hit = if action == "revise" {
                let replaces = required_str(&input, "replaces")?;
                let evidence = required_str(&input, "evidence")?;
                store
                    .revise(scope, workspace_id.as_deref(), replaces, &note, evidence)
                    .map_err(|error| {
                        ToolError::execution_failed(format!("failed to revise memory: {error}"))
                    })?
            } else {
                store
                    .remember(scope, workspace_id.as_deref(), &note)
                    .map_err(|error| {
                        ToolError::execution_failed(format!(
                            "failed to write native memory: {error}"
                        ))
                    })?
            };
            let verb = if action == "revise" {
                "revised"
            } else {
                "remembered"
            };
            return Ok(ToolResult::success(format!(
                "{verb} in native memory: {}:{}-{}",
                hit.source.display(),
                hit.line_start,
                hit.line_end
            ))
            .with_metadata(json!({
                "memory_backend": "native",
                "action": action,
                "scope": scope_label,
                "source": hit.source,
                "line_start": hit.line_start,
                "line_end": hit.line_end,
                "untrusted": true
            })));
        }

        Err(ToolError::execution_failed(format!(
            "native memory store not found at {} — expected the              `memory/global/MEMORY.md` layout; the legacy single-file memory              path was removed in v0.9.4",
            path.display()
        )))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;
    use tempfile::tempdir;

    fn ctx_with_memory(path: PathBuf) -> ToolContext {
        let mut ctx = ToolContext::new(path.parent().unwrap_or_else(|| std::path::Path::new(".")));
        ctx.memory_path = Some(path);
        ctx
    }

    #[tokio::test]
    async fn returns_error_when_memory_disabled() {
        let tmp = tempdir().unwrap();
        let mut ctx = ToolContext::new(tmp.path());
        ctx.memory_path = None; // explicitly disabled

        let tool = RememberTool;
        let err = tool
            .execute(json!({"note": "use 4 spaces for indentation"}), &ctx)
            .await
            .unwrap_err();
        assert!(err.to_string().contains("memory is disabled"), "{err}");
    }

    #[tokio::test]
    async fn rejects_legacy_plain_file_memory_path() {
        // The legacy single-file path (`memory.md`) was removed in v0.9.4;
        // only the native `memory/global/MEMORY.md` layout is writable.
        let tmp = tempdir().unwrap();
        let path = tmp.path().join("memory.md");
        let ctx = ctx_with_memory(path);

        let tool = RememberTool;
        let err = tool
            .execute(json!({"note": "use 4 spaces for indentation"}), &ctx)
            .await
            .unwrap_err();
        assert!(err.to_string().contains("native memory store"), "{err}");
    }

    #[tokio::test]
    async fn native_backend_capture_updates_markdown_and_fts_index() {
        let tmp = tempdir().unwrap();
        let root = tmp.path().join("memory");
        let path = root.join("global/MEMORY.md");
        let mut ctx = ToolContext::new(tmp.path());
        ctx.memory_path = Some(path);

        let result = RememberTool
            .execute(
                json!({"note": "Prefer bounded receipts", "scope": "global"}),
                &ctx,
            )
            .await
            .expect("native capture should succeed");
        assert!(result.success);
        assert_eq!(result.metadata.unwrap()["memory_backend"], "native");

        let hits = crate::native_memory::NativeMemoryStore::new(root)
            .search("receipts", 5)
            .expect("native capture should update index");
        assert_eq!(hits.len(), 1);
        assert_eq!(hits[0].text, "Prefer bounded receipts");
    }

    /// The whole point of revision: a corrected fact replaces the wrong one
    /// instead of piling up behind it, so what gets injected next session is
    /// the truth and not both versions of it.
    #[tokio::test]
    async fn revise_replaces_the_note_instead_of_accumulating() {
        let tmp = tempdir().unwrap();
        let root = tmp.path().join("memory");
        let mut ctx = ToolContext::new(tmp.path());
        ctx.memory_path = Some(root.join("global/MEMORY.md"));

        RememberTool
            .execute(json!({"note": "Deploys run on Fridays"}), &ctx)
            .await
            .expect("append");
        let result = RememberTool
            .execute(
                json!({
                    "action": "revise",
                    "replaces": "Deploys run on Fridays",
                    "note": "Deploys run on Tuesdays",
                    "evidence": "user moved the deploy window this session"
                }),
                &ctx,
            )
            .await
            .expect("revise");
        assert!(result.success);

        let store = crate::native_memory::NativeMemoryStore::new(&root);
        let hits = store.search("Deploys", 10).expect("search");
        assert_eq!(hits.len(), 1, "the stale note must be gone: {hits:?}");
        assert_eq!(hits[0].text, "Deploys run on Tuesdays");
    }

    #[tokio::test]
    async fn retire_removes_the_note_and_keeps_it_in_the_journal() {
        let tmp = tempdir().unwrap();
        let root = tmp.path().join("memory");
        let mut ctx = ToolContext::new(tmp.path());
        ctx.memory_path = Some(root.join("global/MEMORY.md"));

        RememberTool
            .execute(json!({"note": "Uses the staging cluster"}), &ctx)
            .await
            .expect("append");
        RememberTool
            .execute(
                json!({
                    "action": "retire",
                    "replaces": "Uses the staging cluster",
                    "evidence": "staging was decommissioned"
                }),
                &ctx,
            )
            .await
            .expect("retire");

        let store = crate::native_memory::NativeMemoryStore::new(&root);
        assert!(
            store.search("staging", 10).expect("search").is_empty(),
            "retired note must leave the searchable set"
        );
        let journal = std::fs::read_to_string(store.journal_path()).expect("journal");
        assert!(journal.contains("Uses the staging cluster"), "{journal}");
        assert!(journal.contains("staging was decommissioned"), "{journal}");
    }

    /// Editing durable context without saying why is the thing the journal
    /// exists to prevent, so it is refused at the tool boundary.
    #[tokio::test]
    async fn in_place_edits_require_evidence() {
        let tmp = tempdir().unwrap();
        let root = tmp.path().join("memory");
        let mut ctx = ToolContext::new(tmp.path());
        ctx.memory_path = Some(root.join("global/MEMORY.md"));

        RememberTool
            .execute(json!({"note": "Prefers tabs"}), &ctx)
            .await
            .expect("append");
        let err = RememberTool
            .execute(
                json!({"action": "revise", "replaces": "Prefers tabs", "note": "Prefers spaces"}),
                &ctx,
            )
            .await
            .unwrap_err();
        assert!(err.to_string().to_lowercase().contains("evidence"), "{err}");
    }

    /// A note that appears twice is ambiguous, and editing either one is a
    /// guess that silently rewrites the wrong durable fact.
    #[tokio::test]
    async fn ambiguous_target_is_refused_rather_than_guessed() {
        let tmp = tempdir().unwrap();
        let root = tmp.path().join("memory");
        let mut ctx = ToolContext::new(tmp.path());
        ctx.memory_path = Some(root.join("global/MEMORY.md"));

        for _ in 0..2 {
            RememberTool
                .execute(json!({"note": "Ships on Mondays"}), &ctx)
                .await
                .expect("append");
        }
        let err = RememberTool
            .execute(
                json!({
                    "action": "revise",
                    "replaces": "Ships on Mondays",
                    "note": "Ships on Thursdays",
                    "evidence": "schedule changed"
                }),
                &ctx,
            )
            .await
            .unwrap_err();
        assert!(err.to_string().contains("refusing to guess"), "{err}");
    }

    #[tokio::test]
    async fn revising_an_absent_note_is_an_error_not_an_append() {
        let tmp = tempdir().unwrap();
        let root = tmp.path().join("memory");
        let mut ctx = ToolContext::new(tmp.path());
        ctx.memory_path = Some(root.join("global/MEMORY.md"));

        RememberTool
            .execute(json!({"note": "Anchor note"}), &ctx)
            .await
            .expect("append");
        let err = RememberTool
            .execute(
                json!({
                    "action": "revise",
                    "replaces": "Never written",
                    "note": "Something else",
                    "evidence": "cleanup"
                }),
                &ctx,
            )
            .await
            .unwrap_err();
        assert!(err.to_string().contains("no memory note matches"), "{err}");
    }

    #[tokio::test]
    async fn rejects_missing_note_field() {
        let tmp = tempdir().unwrap();
        let path = tmp.path().join("memory.md");
        let ctx = ctx_with_memory(path);

        let tool = RememberTool;
        let err = tool.execute(json!({}), &ctx).await.unwrap_err();
        assert!(err.to_string().to_lowercase().contains("note"), "{err}");
    }
}
