//! Shadow model framework.
//!
//! A shadow model is an alternate implementation of `step :
//! BufferState × Action → BufferState`. The framework runs every
//! applicable scenario through both the live editor and the shadow,
//! asserts equal observables, and reports typed disagreements.
//!
//! The trait is one method (`evaluate`) on top of a capability
//! advertisement (`supports`). New shadows live in
//! `tests/common/shadows/` (created when the second shadow lands)
//! and are picked up by the corpus-differential test in
//! `tests/semantic/shadow_corpus.rs`.
//!
//! Phase 1 ships only [`BufferShadow`], which delegates to the live
//! editor — a no-op differential that proves the trait, the corpus
//! loop, and the disagreement-reporting plumbing all work end to end
//! before any real reference shadow is written.

use crate::common::scenario::buffer_scenario::BufferScenario;
use crate::common::scenario::failure::ScenarioFailure;
use crate::common::scenario::property::{evaluate_actions, BufferState};

/// Capabilities a shadow advertises. The corpus differential filters
/// scenarios to those whose context the shadow can simulate.
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct ShadowCapabilities {
    pub buffer: bool,
    pub workspace: bool,
    pub fs: bool,
    pub lsp: bool,
    pub layout: bool,
    pub style: bool,
}

impl ShadowCapabilities {
    pub const BUFFER_ONLY: Self = Self {
        buffer: true,
        workspace: false,
        fs: false,
        lsp: false,
        layout: false,
        style: false,
    };
}

/// One reference implementation of editor semantics. The runner uses
/// `evaluate` to produce a [`BufferState`] from an initial text and
/// an action sequence, then compares against the live editor's
/// state.
pub trait ShadowModel {
    /// Stable identifier for the shadow, used in disagreement
    /// failures. Should be unique across shadows registered in the
    /// corpus differential.
    fn name(&self) -> &'static str;

    fn supports(&self) -> ShadowCapabilities;

    /// Apply `actions` to a fresh state seeded with `initial_text`
    /// and return the final observable. Must not panic on safe
    /// inputs.
    fn evaluate(&self, initial_text: &str, actions: &[fresh::test_api::Action]) -> BufferState;
}

/// True if `shadow` claims it can simulate everything the scenario
/// requires.
pub fn supports_scenario<S: ShadowModel + ?Sized>(shadow: &S, _s: &BufferScenario) -> bool {
    // BufferScenario only needs the `buffer` capability today. As
    // additional context fields land (workspace, fs, …) this check
    // fans out.
    shadow.supports().buffer
}

/// Run a `BufferScenario` against a shadow model and compare the
/// resulting [`BufferState`] field-by-field against the live editor.
///
/// Returns `Ok(())` on agreement or
/// [`ScenarioFailure::ShadowDisagreement`] on the first divergent
/// field.
pub fn check_buffer_scenario_against_shadow<S: ShadowModel + ?Sized>(
    s: &BufferScenario,
    shadow: &S,
) -> Result<(), ScenarioFailure> {
    let editor = evaluate_actions(&s.initial_text, &s.actions);
    let shadow_state = shadow.evaluate(&s.initial_text, &s.actions);

    fn disagreement(
        s: &BufferScenario,
        shadow_name: &str,
        field: &str,
        editor_value: String,
        shadow_value: String,
    ) -> ScenarioFailure {
        ScenarioFailure::ShadowDisagreement {
            description: s.description.clone(),
            shadow: shadow_name.to_string(),
            field: field.to_string(),
            editor_value,
            shadow_value,
        }
    }

    if editor.buffer_text != shadow_state.buffer_text {
        return Err(disagreement(
            s,
            shadow.name(),
            "buffer_text",
            format!("{:?}", editor.buffer_text),
            format!("{:?}", shadow_state.buffer_text),
        ));
    }
    if editor.primary != shadow_state.primary {
        return Err(disagreement(
            s,
            shadow.name(),
            "primary",
            format!("{:?}", editor.primary),
            format!("{:?}", shadow_state.primary),
        ));
    }
    if editor.all_carets != shadow_state.all_carets {
        return Err(disagreement(
            s,
            shadow.name(),
            "all_carets",
            format!("{:?}", editor.all_carets),
            format!("{:?}", shadow_state.all_carets),
        ));
    }
    if editor.selection_text != shadow_state.selection_text {
        return Err(disagreement(
            s,
            shadow.name(),
            "selection_text",
            format!("{:?}", editor.selection_text),
            format!("{:?}", shadow_state.selection_text),
        ));
    }
    Ok(())
}

/// Identity shadow: re-runs the scenario through the live editor.
///
/// The differential against the live editor is structurally a no-op
/// (running the same code twice agrees with itself), but the test is
/// still valuable: it exercises the entire shadow plumbing
/// (capability advertisement → evaluate → field-by-field compare →
/// typed disagreement) on every corpus scenario, so when a real
/// reference shadow ships next, the wiring is already proven and the
/// only thing under test is the new shadow's semantics.
pub struct BufferShadow;

impl ShadowModel for BufferShadow {
    fn name(&self) -> &'static str {
        "BufferShadow(identity)"
    }

    fn supports(&self) -> ShadowCapabilities {
        ShadowCapabilities::BUFFER_ONLY
    }

    fn evaluate(&self, initial_text: &str, actions: &[fresh::test_api::Action]) -> BufferState {
        evaluate_actions(initial_text, actions)
    }
}
