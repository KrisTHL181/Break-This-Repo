//! Shared route-sized inline output with recoverable session spillover.

use std::path::PathBuf;

use crate::tools::spec::{ToolContext, ToolError};

#[derive(Debug)]
pub(crate) struct OverflowArtifact {
    pub(crate) session_id: String,
    pub(crate) absolute_path: PathBuf,
    pub(crate) relative_path: PathBuf,
    pub(crate) byte_size: u64,
    pub(crate) preview: String,
}

#[derive(Debug)]
pub(crate) struct BoundedText {
    pub(crate) content: String,
    pub(crate) artifact: Option<OverflowArtifact>,
}

pub(crate) fn inline_char_budget(context: &ToolContext) -> usize {
    context
        .route_context_window
        .map(|tokens| {
            let chars = u64::from(tokens).saturating_mul(4).saturating_mul(3) / 100;
            usize::try_from(chars).unwrap_or(100_000)
        })
        .unwrap_or(100_000)
        .clamp(1, 100_000)
}

pub(crate) fn bound_text<F>(
    content: String,
    context: &ToolContext,
    artifact_id: F,
    subject: &str,
) -> Result<BoundedText, ToolError>
where
    F: FnOnce(&str) -> String,
{
    let budget = inline_char_budget(context);
    if content.chars().count() <= budget {
        return Ok(BoundedText {
            content,
            artifact: None,
        });
    }

    let artifact_id = artifact_id(&content);
    let (absolute_path, relative_path) =
        crate::artifacts::write_session_artifact(&context.state_namespace, &artifact_id, &content)
            .map_err(|error| {
                ToolError::execution_failed(format!(
                    "failed to preserve {subject} content artifact: {error}"
                ))
            })?;
    let relative = crate::artifacts::format_artifact_relative_path(&relative_path);
    let mut head = content
        .chars()
        .take(budget.saturating_sub(256))
        .collect::<String>();
    let mut footer = overflow_footer(subject, &relative, head.len(), content.len());
    let allowed_head = budget.saturating_sub(footer.chars().count());
    head = content.chars().take(allowed_head).collect();
    footer = overflow_footer(subject, &relative, head.len(), content.len());
    while !head.is_empty() && head.chars().count() + footer.chars().count() > budget {
        head.pop();
        footer = overflow_footer(subject, &relative, head.len(), content.len());
    }
    let preview = content.chars().take(200).collect();

    Ok(BoundedText {
        content: format!("{head}{footer}"),
        artifact: Some(OverflowArtifact {
            session_id: context.state_namespace.clone(),
            absolute_path,
            relative_path,
            byte_size: content.len() as u64,
            preview,
        }),
    })
}

fn overflow_footer(subject: &str, relative: &str, head_bytes: usize, total_bytes: usize) -> String {
    format!(
        "\n\n[Content overflow: first {head_bytes} of {total_bytes} bytes shown; full {subject} saved to {relative}. Recovery: call retrieve_tool_result with ref={relative}.]"
    )
}
