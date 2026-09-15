//! `InputScenario` — mouse, IME composition, and keyboard chord
//! events as data.
//!
//! Mouse coordinates project to (line, byte) through the current
//! [`RenderSnapshot`]. IME composition decomposes into `InsertChar`
//! actions. Keyboard chord disambiguation is surfaced as a sequence
//! of `Action`s.
//!
//! Phase 9 minimal: mouse-click projection through the
//! `(row, col) → byte` mapping on the test API. The full
//! drag-as-selection and wheel-as-scroll flows depend on extending
//! `EditorTestApi` with a `cell_to_byte` accessor.
//!
//! Asserts on the final [`RenderSnapshot`] (cursor moved to the
//! clicked cell).

use crate::common::harness::EditorTestHarness;
use crate::common::scenario::context::{MouseButton, MouseEvent};
use crate::common::scenario::failure::ScenarioFailure;
use crate::common::scenario::input_event::InputEvent;
use crate::common::scenario::observable::Observable;
use crate::common::scenario::render_snapshot::{RenderSnapshot, RenderSnapshotExpect};

#[derive(Debug, Clone, Default, serde::Serialize, serde::Deserialize)]
pub struct InputScenario {
    pub description: String,
    pub initial_text: String,
    pub events: Vec<InputEvent>,
    pub expected: RenderSnapshotExpect,
}

pub fn check_input_scenario(s: InputScenario) -> Result<(), ScenarioFailure> {
    let mut harness = EditorTestHarness::with_temp_project(80, 24)
        .expect("EditorTestHarness::with_temp_project failed");
    let _fixture = harness
        .load_buffer_from_text(&s.initial_text)
        .expect("load_buffer_from_text failed");
    harness.render().expect("initial render failed");

    for ev in &s.events {
        dispatch_input(&mut harness, ev, &s.description)?;
    }
    harness.render().expect("final render failed");

    let snapshot = RenderSnapshot::extract(&mut harness);
    if let Some((field, expected, actual)) = s.expected.check_against(&snapshot) {
        return Err(ScenarioFailure::SnapshotFieldMismatch {
            description: s.description,
            field: field.into(),
            expected,
            actual,
        });
    }
    Ok(())
}

pub fn assert_input_scenario(s: InputScenario) {
    if let Err(f) = check_input_scenario(s) {
        panic!("{f}");
    }
}

fn dispatch_input(
    harness: &mut EditorTestHarness,
    ev: &InputEvent,
    description: &str,
) -> Result<(), ScenarioFailure> {
    match ev {
        InputEvent::Action(a) => {
            harness.api_mut().dispatch(a.clone());
            Ok(())
        }
        InputEvent::Mouse(MouseEvent::Click {
            row,
            col,
            button: MouseButton::Left,
        }) => {
            // Route through the existing `Editor::handle_mouse` path
            // via the test_api `dispatch_mouse_click` accessor. The
            // editor's real handler does the cell→byte projection
            // we need (gutter offset, wrap-aware), so we don't have
            // to model it here.
            let consumed = harness.api_mut().dispatch_mouse_click(*col, *row);
            if !consumed {
                return Err(ScenarioFailure::InputProjectionFailed {
                    description: description.into(),
                    reason: format!(
                        "Editor did not consume Mouse::Click({col},{row}) — likely outside the buffer area"
                    ),
                });
            }
            Ok(())
        }
        InputEvent::Mouse(other_button) => Err(ScenarioFailure::InputProjectionFailed {
            description: description.into(),
            reason: format!(
                "Mouse {other_button:?} not yet routed; only Click(Left) is wired in Phase 9 minimal."
            ),
        }),
        InputEvent::Compose(chars) => {
            for c in chars {
                harness
                    .api_mut()
                    .dispatch(fresh::test_api::Action::InsertChar(*c));
            }
            Ok(())
        }
        other => Err(ScenarioFailure::InputProjectionFailed {
            description: description.into(),
            reason: format!("InputScenario does not handle {other:?} — wrong scenario type"),
        }),
    }
}
