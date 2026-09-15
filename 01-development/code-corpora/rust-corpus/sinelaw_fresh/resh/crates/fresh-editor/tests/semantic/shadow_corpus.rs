//! Corpus-wide shadow-model differential.
//!
//! Every `BufferScenario` registered in [`super::corpus`] is run
//! through every applicable [`ShadowModel`] and asserted to produce
//! the same observable as the live editor.
//!
//! Phase 1 ships with the no-op [`BufferShadow`] only — running the
//! corpus through the live editor twice. The test exists so the
//! plumbing (capability filter → evaluate → field-by-field compare →
//! typed disagreement) is exercised on every scenario before any
//! real reference shadow lands; once a non-identity shadow is added,
//! the same loop catches its disagreements without per-shadow
//! scaffolding.

use crate::common::scenario::shadow::{
    check_buffer_scenario_against_shadow, supports_scenario, BufferShadow, ShadowModel,
};
use crate::semantic::corpus;

#[test]
fn corpus_agrees_with_buffer_shadow() {
    let shadow: Box<dyn ShadowModel> = Box::new(BufferShadow);
    let mut checked = 0;
    let mut skipped = 0;
    for scenario in corpus::buffer_scenarios() {
        if !supports_scenario(shadow.as_ref(), &scenario) {
            skipped += 1;
            continue;
        }
        if let Err(failure) = check_buffer_scenario_against_shadow(&scenario, shadow.as_ref()) {
            panic!("shadow disagreement: {failure}");
        }
        checked += 1;
    }
    assert!(
        checked > 0,
        "shadow corpus is empty — corpus::buffer_scenarios() returned nothing"
    );
    eprintln!(
        "shadow corpus: {checked} scenarios checked, {skipped} skipped (capability mismatch)",
    );
}
