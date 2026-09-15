//! Structured scenario failures.
//!
//! Every assertion in every runner produces one of these variants on
//! mismatch. The runners' fallible (`check_*`) entry points return
//! `Result<(), ScenarioFailure>`, so external drivers (proptest,
//! shadow-model differential, corpus replay) can call them in a
//! tight loop without `catch_unwind` or string-parsing panic
//! messages.
//!
//! `Display` reproduces the legacy panic messages so the panicking
//! `assert_*` wrappers and `#[should_panic(expected = "…")]`
//! meta-tests work without changes.

use crate::common::scenario::buffer_scenario::CursorExpect;
use fresh::test_api::Caret;

#[derive(Debug, Clone, serde::Serialize, serde::Deserialize)]
#[serde(tag = "kind", rename_all = "snake_case")]
pub enum ScenarioFailure {
    BufferTextMismatch {
        description: String,
        expected: String,
        actual: String,
    },
    PrimaryCursorMismatch {
        description: String,
        expected: CursorExpect,
        actual: Caret,
    },
    CursorCountMismatch {
        description: String,
        expected: usize,
        actual: usize,
    },
    SecondaryCursorMismatch {
        description: String,
        index: usize,
        expected: CursorExpect,
        actual: Caret,
    },
    SelectionTextMismatch {
        description: String,
        expected: String,
        actual: String,
    },
    ForwardTraceFailed {
        description: String,
        expected: String,
        actual: String,
    },
    ReverseTraceFailed {
        description: String,
        undo_count: usize,
        expected: String,
        actual: String,
    },
    ViewportTopByteMismatch {
        description: String,
        expected: usize,
        actual: usize,
    },
    SnapshotFieldMismatch {
        description: String,
        field: String,
        expected: String,
        actual: String,
    },
    ModalStateMismatch {
        description: String,
        expected: String,
        actual: String,
    },
    WorkspaceStateMismatch {
        description: String,
        field: String,
        expected: String,
        actual: String,
    },
    InputProjectionFailed {
        description: String,
        reason: String,
    },
    ShadowDisagreement {
        description: String,
        shadow: String,
        field: String,
        editor_value: String,
        shadow_value: String,
    },
}

impl std::fmt::Display for ScenarioFailure {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ScenarioFailure::BufferTextMismatch {
                description,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] buffer text mismatch\n   expected = {expected:?}\n   actual   = {actual:?}",
            ),
            ScenarioFailure::PrimaryCursorMismatch {
                description,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] primary cursor mismatch:\n   expected = {expected:?}\n   actual   = {actual:?}",
            ),
            ScenarioFailure::CursorCountMismatch {
                description,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] cursor count mismatch (got {actual} cursors, expected {expected})",
            ),
            ScenarioFailure::SecondaryCursorMismatch {
                description,
                index,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] secondary cursor mismatch (index {index}):\n   expected = {expected:?}\n   actual   = {actual:?}",
            ),
            ScenarioFailure::SelectionTextMismatch {
                description,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] selection text mismatch\n   expected = {expected:?}\n   actual   = {actual:?}",
            ),
            ScenarioFailure::ForwardTraceFailed {
                description,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] forward trace failed: buffer text after actions\n   expected = {expected:?}\n   actual   = {actual:?}",
            ),
            ScenarioFailure::ReverseTraceFailed {
                description,
                undo_count,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] reverse trace failed: {undo_count} undos should yield the initial buffer\n   expected = {expected:?}\n   actual   = {actual:?}",
            ),
            ScenarioFailure::ViewportTopByteMismatch {
                description,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] viewport_top_byte mismatch: expected {expected}, got {actual}",
            ),
            ScenarioFailure::SnapshotFieldMismatch {
                description,
                field,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] render snapshot field {field} mismatch\n   expected = {expected}\n   actual   = {actual}",
            ),
            ScenarioFailure::ModalStateMismatch {
                description,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] modal state mismatch\n   expected = {expected}\n   actual   = {actual}",
            ),
            ScenarioFailure::WorkspaceStateMismatch {
                description,
                field,
                expected,
                actual,
            } => write!(
                f,
                "[{description}] workspace {field} mismatch\n   expected = {expected}\n   actual   = {actual}",
            ),
            ScenarioFailure::InputProjectionFailed {
                description,
                reason,
            } => write!(
                f,
                "[{description}] input projection failed: {reason}",
            ),
            ScenarioFailure::ShadowDisagreement {
                description,
                shadow,
                field,
                editor_value,
                shadow_value,
            } => write!(
                f,
                "[{description}] shadow {shadow} disagrees on {field}\n   editor = {editor_value}\n   shadow = {shadow_value}",
            ),
        }
    }
}

impl std::error::Error for ScenarioFailure {}
