use super::*;
use crate::tools::spec::ToolContext;
use std::path::PathBuf;

struct ArtifactRootRestore(Option<PathBuf>);

impl Drop for ArtifactRootRestore {
    fn drop(&mut self) {
        crate::artifacts::set_test_artifact_sessions_root(self.0.take());
    }
}

fn runtime() -> tokio::runtime::Runtime {
    tokio::runtime::Builder::new_current_thread()
        .enable_all()
        .build()
        .expect("test runtime")
}

#[test]
fn raw_pdf_production_path_preserves_exact_bytes_without_extractor() {
    let _lock = crate::artifacts::TEST_ARTIFACT_SESSIONS_GUARD
        .lock()
        .unwrap_or_else(|error| error.into_inner());
    let temporary = tempfile::tempdir().expect("artifact root");
    let prior =
        crate::artifacts::set_test_artifact_sessions_root(Some(temporary.path().join("sessions")));
    let _restore = ArtifactRootRestore(prior);
    let bytes = b"%PDF-1.7\nraw fixture that is intentionally not parseable\n%%EOF";
    let missing = temporary.path().join("definitely-not-pdftotext");
    let document = runtime()
        .block_on(extract_fetched_document(
            Format::Raw,
            "https://example.com/raw.pdf",
            "application/pdf",
            bytes,
            true,
            None,
            PdfTextCommand::test(missing.as_os_str(), Duration::from_millis(50), None),
        ))
        .expect("signed raw PDF must bypass the missing extractor");
    let (content, artifact) = render_extracted(
        "https://example.com/raw.pdf",
        "application/pdf",
        Format::Raw,
        document,
        bytes,
        &ToolContext::new("."),
    )
    .expect("raw PDF preservation must not require pdftotext");
    let artifact = artifact.expect("raw PDF artifact");
    assert!(content.contains("PDF response saved"), "{content}");
    assert_eq!(std::fs::read(artifact.absolute_path).unwrap(), bytes);
}

#[test]
fn raw_pdf_spoofs_and_contradictory_media_mime_create_no_artifact() {
    let _lock = crate::artifacts::TEST_ARTIFACT_SESSIONS_GUARD
        .lock()
        .unwrap_or_else(|error| error.into_inner());
    let temporary = tempfile::tempdir().expect("artifact root");
    let sessions = temporary.path().join("sessions");
    let prior = crate::artifacts::set_test_artifact_sessions_root(Some(sessions.clone()));
    let _restore = ArtifactRootRestore(prior);
    let missing = temporary.path().join("definitely-not-pdftotext");
    let request = PdfTextCommand::test(missing.as_os_str(), Duration::from_millis(50), None);
    let runtime = runtime();

    for (url, content_type, bytes, expected) in [
        (
            "https://example.com/download",
            "application/pdf",
            b"plain text pretending to be a PDF".as_slice(),
            "PDF signature",
        ),
        (
            "https://example.com/spoof.pdf",
            "text/plain",
            b"plain text pretending to be a PDF".as_slice(),
            "PDF signature",
        ),
        (
            "https://example.com/signed",
            "image/png",
            b"%PDF-1.7\n%%EOF".as_slice(),
            "did not match its PDF bytes",
        ),
    ] {
        let error = runtime
            .block_on(extract_fetched_document(
                Format::Raw,
                url,
                content_type,
                bytes,
                true,
                None,
                request,
            ))
            .expect_err("invalid PDF response must fail before raw preservation");
        assert!(error.to_string().contains(expected), "{error}");
    }
    assert!(
        !sessions.exists(),
        "rejected bytes must not create artifacts"
    );
}

#[tokio::test]
async fn fetched_pdf_missing_helper_is_a_failed_typed_outcome() {
    let temporary = tempfile::tempdir().expect("tempdir");
    let missing = temporary.path().join("definitely-not-pdftotext");
    let error = extract_fetched_document(
        Format::Text,
        "https://example.com/document.pdf",
        "application/pdf",
        b"%PDF-1.7\n%%EOF",
        true,
        None,
        PdfTextCommand::test(missing.as_os_str(), Duration::from_secs(1), None),
    )
    .await
    .expect_err("missing helper must fail the fetched PDF call");
    let payload = match &error {
        ToolError::NotAvailable { message } => {
            serde_json::from_str::<Value>(message).expect("structured unavailable payload")
        }
        other => panic!("unexpected error: {other:?}"),
    };
    assert_eq!(payload["type"], "binary_unavailable");
    assert_eq!(
        crate::tools::spec::ToolExecutionOutcome::from_legacy(Err(error)).status,
        crate::tools::spec::ToolTerminalStatus::Failed
    );
}
