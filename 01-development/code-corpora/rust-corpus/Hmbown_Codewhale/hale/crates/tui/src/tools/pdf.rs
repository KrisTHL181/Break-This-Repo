//! Shared PDF-to-text adapter.
//!
//! PDF parsing is intentionally delegated to the optional `pdftotext`
//! executable. Keeping the adapter here gives file and web tools one error
//! contract without carrying a second parser and font stack in Codewhale.

use std::ffi::OsStr;
use std::fmt;
use std::io::Write;
use std::path::Path;
use std::process::Stdio;
use std::time::Duration;

use serde_json::json;
use tokio::io::{AsyncRead, AsyncReadExt};
use tokio_util::sync::CancellationToken;

use super::spec::ToolError;

const PDF_TEXT_TIMEOUT: Duration = Duration::from_secs(30);
const PDF_PIPE_DRAIN_TIMEOUT: Duration = Duration::from_secs(1);
const MAX_PDF_STDOUT_BYTES: usize = 16 * 1024 * 1024;
const MAX_PDF_STDERR_BYTES: usize = 32 * 1024;

#[derive(Debug, Clone, PartialEq, Eq)]
pub(super) enum PdfTextError {
    BinaryUnavailable,
    Cancelled,
    TimedOut,
    Execution(String),
}

impl fmt::Display for PdfTextError {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Self::BinaryUnavailable => formatter.write_str(
                "PDF text extraction requires the optional `pdftotext` executable (Poppler)",
            ),
            Self::Cancelled => formatter.write_str("PDF text extraction was cancelled"),
            Self::TimedOut => write!(
                formatter,
                "PDF text extraction timed out after {} seconds",
                PDF_TEXT_TIMEOUT.as_secs()
            ),
            Self::Execution(message) => formatter.write_str(message),
        }
    }
}

/// One typed mapping shared by local-file and fetched-PDF consumers.
///
/// The missing-binary message is deliberately a small JSON object. The
/// `NotAvailable` variant gives the runtime a failed terminal status while
/// callers that inspect the variant retain machine-readable recovery data.
pub(super) fn into_tool_error(error: PdfTextError) -> ToolError {
    match error {
        PdfTextError::BinaryUnavailable => ToolError::not_available(
            json!({
                "type": "binary_unavailable",
                "kind": "pdf",
                "binary": "pdftotext",
                "reason": "optional pdftotext executable is not installed",
                "hint": "install Poppler and ensure pdftotext is on PATH"
            })
            .to_string(),
        ),
        PdfTextError::Cancelled => ToolError::cancelled("PDF text extraction was cancelled"),
        PdfTextError::TimedOut => ToolError::Timeout {
            seconds: PDF_TEXT_TIMEOUT.as_secs(),
        },
        PdfTextError::Execution(message) => ToolError::execution_failed(message),
    }
}

#[derive(Clone, Copy)]
pub(crate) struct PdfTextCommand<'a> {
    binary: &'a OsStr,
    timeout: Duration,
    cancel: Option<&'a CancellationToken>,
}

impl<'a> PdfTextCommand<'a> {
    pub(super) fn system(cancel: Option<&'a CancellationToken>) -> Self {
        Self {
            binary: OsStr::new("pdftotext"),
            timeout: PDF_TEXT_TIMEOUT,
            cancel,
        }
    }

    #[cfg(test)]
    pub(super) fn test(
        binary: &'a OsStr,
        timeout: Duration,
        cancel: Option<&'a CancellationToken>,
    ) -> Self {
        Self {
            binary,
            timeout,
            cancel,
        }
    }
}

pub(super) async fn extract_path(
    path: &Path,
    page_range: Option<(u32, u32)>,
    command: PdfTextCommand<'_>,
) -> Result<String, PdfTextError> {
    extract_path_with_command(path, page_range, command).await
}

pub(super) async fn extract_bytes(
    bytes: &[u8],
    command: PdfTextCommand<'_>,
) -> Result<String, PdfTextError> {
    let mut input = tempfile::NamedTempFile::new().map_err(|error| {
        PdfTextError::Execution(format!("failed to stage fetched PDF: {error}"))
    })?;
    input.write_all(bytes).map_err(|error| {
        PdfTextError::Execution(format!("failed to stage fetched PDF: {error}"))
    })?;
    input.flush().map_err(|error| {
        PdfTextError::Execution(format!("failed to stage fetched PDF: {error}"))
    })?;
    extract_path_with_command(input.path(), None, command).await
}

async fn extract_path_with_command(
    path: &Path,
    page_range: Option<(u32, u32)>,
    request: PdfTextCommand<'_>,
) -> Result<String, PdfTextError> {
    if request.cancel.is_some_and(CancellationToken::is_cancelled) {
        return Err(PdfTextError::Cancelled);
    }

    let mut command = tokio::process::Command::new(request.binary);
    crate::utils::suppress_tokio_console_window(&mut command);
    command.arg("-layout");
    if let Some((start, end)) = page_range {
        command.arg("-f").arg(start.to_string());
        command.arg("-l").arg(end.to_string());
    }
    command
        .arg(path)
        .arg("-")
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .kill_on_drop(true);

    let mut child = command.spawn().map_err(|error| {
        if error.kind() == std::io::ErrorKind::NotFound {
            PdfTextError::BinaryUnavailable
        } else {
            PdfTextError::Execution(format!("failed to launch pdftotext: {error}"))
        }
    })?;
    let stdout = child
        .stdout
        .take()
        .ok_or_else(|| PdfTextError::Execution("failed to capture pdftotext stdout".to_string()))?;
    let stderr = child
        .stderr
        .take()
        .ok_or_else(|| PdfTextError::Execution("failed to capture pdftotext stderr".to_string()))?;
    let stdout_task = tokio::spawn(read_bounded(stdout, MAX_PDF_STDOUT_BYTES));
    let stderr_task = tokio::spawn(read_bounded(stderr, MAX_PDF_STDERR_BYTES));

    let status = tokio::select! {
        result = child.wait() => result.map_err(|error| {
            PdfTextError::Execution(format!("failed to wait for pdftotext: {error}"))
        })?,
        () = wait_for_cancellation(request.cancel) => {
            terminate_child(&mut child).await;
            finish_capture_tasks(stdout_task, stderr_task).await?;
            return Err(PdfTextError::Cancelled);
        }
        () = tokio::time::sleep(request.timeout) => {
            terminate_child(&mut child).await;
            finish_capture_tasks(stdout_task, stderr_task).await?;
            return Err(PdfTextError::TimedOut);
        }
    };
    let (stdout, stderr) = finish_capture_tasks(stdout_task, stderr_task).await?;

    if stdout.truncated {
        return Err(PdfTextError::Execution(format!(
            "pdftotext output exceeded the {} byte safety limit",
            MAX_PDF_STDOUT_BYTES
        )));
    }
    if !status.success() {
        let stderr_truncated = stderr.truncated;
        let stderr = sanitized_text(&stderr.bytes);
        let suffix = if stderr_truncated { " [truncated]" } else { "" };
        let stderr = if stderr.is_empty() {
            "no diagnostic output".to_string()
        } else {
            stderr
        };
        return Err(PdfTextError::Execution(format!(
            "pdftotext failed (exit {:?}): {stderr}{suffix}",
            status.code()
        )));
    }
    Ok(String::from_utf8_lossy(&stdout.bytes).into_owned())
}

async fn wait_for_cancellation(cancel: Option<&CancellationToken>) {
    match cancel {
        Some(cancel) => cancel.cancelled().await,
        None => std::future::pending::<()>().await,
    }
}

async fn terminate_child(child: &mut tokio::process::Child) {
    let _ = child.kill().await;
    let _ = child.wait().await;
}

struct BoundedOutput {
    bytes: Vec<u8>,
    truncated: bool,
}

async fn read_bounded(
    mut reader: impl AsyncRead + Unpin,
    max_bytes: usize,
) -> std::io::Result<BoundedOutput> {
    let mut bytes = Vec::with_capacity(max_bytes.min(8 * 1024));
    let mut buffer = [0u8; 8 * 1024];
    let mut truncated = false;
    loop {
        let read = reader.read(&mut buffer).await?;
        if read == 0 {
            break;
        }
        let remaining = max_bytes.saturating_sub(bytes.len());
        let retained = read.min(remaining);
        bytes.extend_from_slice(&buffer[..retained]);
        truncated |= retained < read;
    }
    Ok(BoundedOutput { bytes, truncated })
}

async fn finish_capture_tasks(
    mut stdout: tokio::task::JoinHandle<std::io::Result<BoundedOutput>>,
    mut stderr: tokio::task::JoinHandle<std::io::Result<BoundedOutput>>,
) -> Result<(BoundedOutput, BoundedOutput), PdfTextError> {
    let joined = tokio::time::timeout(PDF_PIPE_DRAIN_TIMEOUT, async {
        tokio::join!(&mut stdout, &mut stderr)
    })
    .await;
    let (stdout, stderr) = match joined {
        Ok(output) => output,
        Err(_) => {
            stdout.abort();
            stderr.abort();
            let _ = tokio::join!(stdout, stderr);
            return Err(PdfTextError::Execution(
                "pdftotext output pipes did not close after process termination".to_string(),
            ));
        }
    };
    let stdout = stdout
        .map_err(|error| PdfTextError::Execution(format!("stdout reader failed: {error}")))?
        .map_err(|error| PdfTextError::Execution(format!("stdout reader failed: {error}")))?;
    let stderr = stderr
        .map_err(|error| PdfTextError::Execution(format!("stderr reader failed: {error}")))?
        .map_err(|error| PdfTextError::Execution(format!("stderr reader failed: {error}")))?;
    Ok((stdout, stderr))
}

fn sanitized_text(bytes: &[u8]) -> String {
    String::from_utf8_lossy(bytes)
        .trim()
        .chars()
        .map(|character| match character {
            '\n' | '\t' => character,
            character if character.is_control() => '\u{fffd}',
            character => character,
        })
        .collect()
}

#[cfg(test)]
mod tests;
