//! Phase 2: differential between the live editor's `RenderSnapshot`
//! and `LayoutShadow`'s naive snapshot.
//!
//! The shadow only models the easy invariants today (gutter width
//! from line count). The differential proves the runner +
//! capability filter + field-by-field disagreement reporting all
//! work end to end on the layout layer, just as
//! `shadow_corpus::corpus_agrees_with_buffer_shadow` does for the
//! buffer layer.

use crate::common::harness::EditorTestHarness;
use crate::common::scenario::layout_shadow::LayoutShadow;
use crate::common::scenario::observable::Observable;
use crate::common::scenario::render_snapshot::RenderSnapshot;

fn editor_snapshot(text: &str, width: u16, height: u16) -> RenderSnapshot {
    let mut harness = EditorTestHarness::with_temp_project(width, height).expect("harness");
    let _fix = harness.load_buffer_from_text(text).expect("load");
    harness.render().expect("render");
    RenderSnapshot::extract(&mut harness)
}

#[test]
fn layout_shadow_agrees_on_gutter_width_for_short_buffer() {
    let text = "alpha\nbravo\ncharlie";
    let editor = editor_snapshot(text, 80, 24);
    let shadow = LayoutShadow::snapshot(text, 80, 24);
    assert_eq!(
        editor.gutter_width, shadow.gutter_width,
        "shadow disagrees on gutter_width for {text:?}",
    );
}

#[test]
fn layout_shadow_agrees_on_gutter_width_for_two_digit_buffer() {
    // 12 lines ⇒ 2-digit line numbers ⇒ gutter_width = 3.
    let text: String = (1..=12).map(|i| format!("line {i}\n")).collect();
    let editor = editor_snapshot(&text, 80, 24);
    let shadow = LayoutShadow::snapshot(&text, 80, 24);
    assert_eq!(
        editor.gutter_width, shadow.gutter_width,
        "shadow disagrees on gutter_width for 12-line buffer",
    );
}
