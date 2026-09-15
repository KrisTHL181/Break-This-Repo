//! Persistent permission-rule construction and save-preview formatting.
//!
//! This module owns the policy for turning an already-classified approval
//! request into exact ask/allow rules. It deliberately has no modal state or
//! rendering code: views consume the validated rules and their bounded text
//! preview without re-parsing tool inputs.

use std::path::Path;

use codewhale_config::ToolAskRule;
use codewhale_execpolicy::PermissionAction;
use serde_json::Value;

use crate::tools::canonical_action::canonical_action_alias;

/// Human-readable preview of rules an approval action would append.
///
/// This is intentionally derived from the already validated persistent-rule
/// candidates; the approval UI must not re-parse tool inputs such as patches.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct PermissionRuleSavePreview {
    pub action: PermissionAction,
    pub rule_count: usize,
    pub entries: Vec<String>,
    pub omitted: usize,
}

impl PermissionRuleSavePreview {
    #[must_use]
    pub fn summary(&self) -> String {
        let action = match self.action {
            PermissionAction::Allow => "allow",
            PermissionAction::Ask => "ask",
            PermissionAction::Deny => "deny",
        };
        let noun = if self.rule_count == 1 {
            "rule"
        } else {
            "rules"
        };
        format!("{} {action} {noun}", self.rule_count)
    }
}

pub(super) const SAVE_PREVIEW_MAX_ENTRIES: usize = 4;

#[must_use]
pub(super) fn build_save_preview(
    rules: &[ToolAskRule],
    max_entries: usize,
) -> Option<PermissionRuleSavePreview> {
    if rules.is_empty() {
        return None;
    }

    let entries = rules
        .iter()
        .take(max_entries)
        .map(format_save_entry)
        .collect();
    Some(PermissionRuleSavePreview {
        action: rules[0].action,
        rule_count: rules.len(),
        entries,
        omitted: rules.len().saturating_sub(max_entries),
    })
}

#[must_use]
fn format_save_entry(rule: &ToolAskRule) -> String {
    let mut parts = vec![format!("tool={}", sanitize_preview_value(&rule.tool))];
    if let Some(command) = &rule.command {
        parts.push(format!("command={}", sanitize_preview_value(command)));
    }
    if let Some(path) = &rule.path {
        parts.push(format!("path={}", sanitize_preview_value(path)));
    }
    if rule.command_exact {
        parts.push("command_exact=true".to_string());
    }
    if let Some(workspace) = &rule.workspace {
        parts.push(format!("workspace={}", sanitize_preview_value(workspace)));
    }
    parts.join(" ")
}

#[must_use]
fn sanitize_preview_value(value: &str) -> String {
    value
        .replace('\\', "\\\\")
        .replace('\r', "\\r")
        .replace('\n', "\\n")
        .replace('\t', "\\t")
}

#[must_use]
pub(super) fn build_persistent_ask_rules(
    tool_name: &str,
    params: &Value,
    workspace: &Path,
) -> Vec<ToolAskRule> {
    let semantic = canonical_action_alias(tool_name, params);
    match semantic {
        "exec_shell" => build_exec_shell_ask_rules(params),
        // File writes save an exact, workspace-relative path so a later
        // edit/write of the same file is matched. read_file stays out: this
        // boundary is about persisting *write* approvals only.
        "write_file" | "edit_file" => build_file_write_ask_rules(semantic, params, workspace),
        "apply_patch" => build_apply_patch_ask_rules(params, workspace),
        _ => Vec::new(),
    }
}

#[must_use]
pub(super) fn build_persistent_allow_rules(
    tool_name: &str,
    params: &Value,
    workspace: &Path,
    exact_rules: &[ToolAskRule],
) -> Vec<ToolAskRule> {
    if exact_rules.is_empty() {
        return Vec::new();
    }

    if tool_name == "exec_shell" {
        let Some(command) = params.get("command").and_then(Value::as_str) else {
            return Vec::new();
        };
        if !matches!(
            codewhale_execpolicy::command_safety::analyze_command(command).level,
            codewhale_execpolicy::command_safety::SafetyLevel::Safe
                | codewhale_execpolicy::command_safety::SafetyLevel::WorkspaceSafe
        ) {
            return Vec::new();
        }
    }

    let workspace = workspace.to_string_lossy();
    let Some(workspace) = codewhale_execpolicy::normalize_workspace_scope(workspace.as_ref())
    else {
        return Vec::new();
    };

    exact_rules
        .iter()
        .cloned()
        .map(|rule| rule.into_exact_workspace_allow(workspace.clone()))
        .collect()
}

#[must_use]
fn build_exec_shell_ask_rules(params: &Value) -> Vec<ToolAskRule> {
    let Some(command) = params
        .get("command")
        .and_then(Value::as_str)
        .map(str::trim)
        .filter(|command| !command.is_empty())
    else {
        return Vec::new();
    };
    vec![ToolAskRule::exec_shell(command)]
}

#[must_use]
fn build_file_write_ask_rules(
    tool_name: &str,
    params: &Value,
    workspace: &Path,
) -> Vec<ToolAskRule> {
    let Some(path) = params
        .get("path")
        .and_then(Value::as_str)
        .map(str::trim)
        .filter(|path| !path.is_empty())
    else {
        return Vec::new();
    };
    // Reuse the canonical matcher normalization so the saved rule equals what
    // runtime matching compares against. `None` (and the degenerate
    // workspace-root case) means the path is empty, traversing, drive-relative,
    // or outside the workspace, so we save nothing and the `S` shortcut and
    // preview stay disabled.
    let workspace = workspace.to_string_lossy();
    let Some(relative) =
        codewhale_execpolicy::normalize_workspace_relative_path(path, workspace.as_ref())
            .filter(|relative| !relative.is_empty())
    else {
        return Vec::new();
    };
    vec![ToolAskRule::file_path(tool_name, relative)]
}

#[must_use]
fn build_apply_patch_ask_rules(params: &Value, workspace: &Path) -> Vec<ToolAskRule> {
    let Ok(preflight) = crate::tools::apply_patch::preflight_apply_patch(params) else {
        return Vec::new();
    };
    let workspace = workspace.to_string_lossy();
    let mut rules = Vec::new();

    for path in preflight.touched_files {
        let Some(relative) =
            codewhale_execpolicy::normalize_workspace_relative_path(&path, workspace.as_ref())
                .filter(|relative| !relative.is_empty())
        else {
            return Vec::new();
        };
        let rule = ToolAskRule::file_path("apply_patch", relative);
        if !rules.contains(&rule) {
            rules.push(rule);
        }
    }

    rules
}
