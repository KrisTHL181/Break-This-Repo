use crate::{ToolError, ToolResult};

/// Machine-readable terminal state for one tool call.
///
/// This is intentionally separate from [`ToolResult::success`]: a cancelled
/// call still needs a legacy model-visible result so the call/result transcript
/// stays well formed, while the runtime must not report that call as a generic
/// failure or infer cancellation from output text.
#[derive(Debug, Clone, Copy, PartialEq, Eq, serde::Serialize, serde::Deserialize)]
#[serde(rename_all = "snake_case")]
pub enum ToolTerminalStatus {
    Succeeded,
    Failed,
    Denied,
    InvalidArguments,
    Cancelled,
    TimedOut,
}

impl ToolTerminalStatus {
    #[must_use]
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Succeeded => "succeeded",
            Self::Failed => "failed",
            Self::Denied => "denied",
            Self::InvalidArguments => "invalid_arguments",
            Self::Cancelled => "cancelled",
            Self::TimedOut => "timed_out",
        }
    }
}

/// Terminal wrapper around the v0.9.1-compatible tool result/error contract.
///
/// Exactly one of `result` and `error` is populated by the constructors. The
/// existing UI and transcript paths can keep consuming [`Self::legacy_result`]
/// while audit and orchestration use [`Self::status`] directly.
#[derive(Debug, Clone)]
pub struct ToolExecutionOutcome {
    pub status: ToolTerminalStatus,
    result: Option<ToolResult>,
    error: Option<ToolError>,
}

impl ToolExecutionOutcome {
    #[must_use]
    pub fn from_legacy(result: Result<ToolResult, ToolError>) -> Self {
        match result {
            Ok(result) => {
                let status = if result.success {
                    ToolTerminalStatus::Succeeded
                } else {
                    ToolTerminalStatus::Failed
                };
                Self {
                    status,
                    result: Some(result),
                    error: None,
                }
            }
            Err(error) => {
                let status = match &error {
                    ToolError::InvalidInput { .. } | ToolError::MissingField { .. } => {
                        ToolTerminalStatus::InvalidArguments
                    }
                    ToolError::PathEscape { .. } | ToolError::PermissionDenied { .. } => {
                        ToolTerminalStatus::Denied
                    }
                    ToolError::Timeout { .. } => ToolTerminalStatus::TimedOut,
                    ToolError::Cancelled { .. } => ToolTerminalStatus::Cancelled,
                    ToolError::ExecutionFailed { .. } | ToolError::NotAvailable { .. } => {
                        ToolTerminalStatus::Failed
                    }
                };
                Self {
                    status,
                    result: None,
                    error: Some(error),
                }
            }
        }
    }

    /// Construct a cancelled outcome while retaining the legacy benign result
    /// used to close the provider tool-call pair.
    #[must_use]
    pub fn cancelled(result: ToolResult) -> Self {
        Self {
            status: ToolTerminalStatus::Cancelled,
            result: Some(result),
            error: None,
        }
    }

    pub fn legacy_result(&self) -> Result<ToolResult, ToolError> {
        match (&self.result, &self.error) {
            (Some(result), None) => Ok(result.clone()),
            (None, Some(error)) => Err(error.clone()),
            _ => unreachable!("ToolExecutionOutcome must contain exactly one result or error"),
        }
    }

    pub fn into_legacy_result(self) -> Result<ToolResult, ToolError> {
        match (self.result, self.error) {
            (Some(result), None) => Ok(result),
            (None, Some(error)) => Err(error),
            _ => unreachable!("ToolExecutionOutcome must contain exactly one result or error"),
        }
    }
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use super::*;

    #[test]
    fn legacy_results_map_to_explicit_terminal_statuses() {
        let cases = [
            (Ok(ToolResult::success("ok")), ToolTerminalStatus::Succeeded),
            (Ok(ToolResult::error("failed")), ToolTerminalStatus::Failed),
            (
                Err(ToolError::invalid_input("bad arguments")),
                ToolTerminalStatus::InvalidArguments,
            ),
            (
                Err(ToolError::missing_field("path")),
                ToolTerminalStatus::InvalidArguments,
            ),
            (
                Err(ToolError::path_escape(PathBuf::from("../secret"))),
                ToolTerminalStatus::Denied,
            ),
            (
                Err(ToolError::permission_denied("no")),
                ToolTerminalStatus::Denied,
            ),
            (
                Err(ToolError::Timeout { seconds: 5 }),
                ToolTerminalStatus::TimedOut,
            ),
            (
                Err(ToolError::cancelled("stop")),
                ToolTerminalStatus::Cancelled,
            ),
            (
                Err(ToolError::execution_failed("boom")),
                ToolTerminalStatus::Failed,
            ),
            (
                Err(ToolError::not_available("missing")),
                ToolTerminalStatus::Failed,
            ),
        ];

        for (legacy, expected) in cases {
            let outcome = ToolExecutionOutcome::from_legacy(legacy);
            assert_eq!(outcome.status, expected);
            let populated =
                usize::from(outcome.result.is_some()) + usize::from(outcome.error.is_some());
            assert_eq!(populated, 1);
        }
    }

    #[test]
    fn cancelled_result_keeps_legacy_pair_but_not_failure_status() {
        let legacy = ToolResult::error("not executed");
        let outcome = ToolExecutionOutcome::cancelled(legacy.clone());

        assert_eq!(outcome.status, ToolTerminalStatus::Cancelled);
        let restored = outcome.legacy_result().expect("legacy result");
        assert_eq!(restored.content, legacy.content);
        assert!(!restored.success);
    }
}
