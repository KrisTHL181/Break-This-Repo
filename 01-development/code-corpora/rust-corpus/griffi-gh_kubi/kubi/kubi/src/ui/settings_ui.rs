use hui::{
  element::{br::Break, container::Container, slider::Slider, text::Text, ElementList, UiElementExt},
  layout::{Alignment, Direction},
  signal::Signal,
  rect_frame,
  size,
};
use shipyard::{NonSendSync, UniqueView, UniqueViewMut};
use winit::keyboard::KeyCode;
use crate::{
  hui_integration::UiState,
  input::RawKbmInputState,
  rendering::Renderer,
  settings::GameSettings
};

#[derive(Signal)]
enum SettingsSignal {
  SetRenderDistance(u8),
  SetEnableDynamicCrosshair(bool),
  SetEnableVsync(bool),
  // SetEnableDebugChunkBorder(bool),
  SetMouseSensitivity(f32),
}

// hUI doesn't have a checkbox element yet
// so we'll have to use sliders for now
fn checkbox(
  ui: &mut ElementList,
  text: &'static str,
  value: bool,
  signal: impl Fn(bool) -> SettingsSignal + 'static,
) {
  const WIDTH_PX: f32 = 50.;
  const HEIGHT_PX: f32 = WIDTH_PX / 2.;
  const HANDLE_PX: f32 = HEIGHT_PX;
  const TRACK_HEIGHT_RATIO: f32 = 0.75;
  const TRACK_HEIGHT_PX: f32 = HEIGHT_PX * TRACK_HEIGHT_RATIO;

  Container::default()
    .with_direction(Direction::Horizontal)
    .with_align(Alignment::Center)
    .with_gap(5.)
    .with_children(|ui| {
      Text::new(text)
        .add_child(ui);
      Slider::new(value as u32 as f32)
        .with_size(size!(WIDTH_PX, HEIGHT_PX))
        .with_track_height(TRACK_HEIGHT_RATIO)
        .with_track(rect_frame! {
          color: (0.5, 0.5, 0.5),
          corner_radius: TRACK_HEIGHT_PX * 0.5,
        })
        .with_track_active(rect_frame! {
          color: (0., 0., 0.75),
          corner_radius: TRACK_HEIGHT_PX * 0.5,
        })
        .with_handle_size((HANDLE_PX, 1.))
        .with_handle(rect_frame! {
          color: (0., 0., 1.),
          corner_radius: HANDLE_PX * 0.5,
        })
        .on_change(move |f| signal(f >= 0.5))
        .add_child(ui);
      Text::new(if value { "On" } else { "Off" })
        .add_child(ui);
    })
    .add_child(ui);
}

pub fn f1_held_settings_condition(
  kbd: UniqueView<RawKbmInputState>,
) -> bool {
  //f1 must be held down to open settings
  //TODO implement ModalManager instead of this
  kbd.keyboard_state.contains(KeyCode::F1 as u32)
}

pub fn render_settings_ui(
  mut ui: NonSendSync<UniqueViewMut<UiState>>,
  mut ren: UniqueViewMut<Renderer>,
  mut settings: UniqueViewMut<GameSettings>,
) {
  Container::default()
    .with_size(size!(100%))
    .with_background((0., 0., 0., 0.5))
    .with_align(Alignment::Center)
    .with_children(|ui| {
      Container::default()
        .with_background(rect_frame! {
          color: (0.2, 0.2, 0.2),
          corner_radius: 8.
        })
        .with_size(size!(600, 300))
        .with_direction(Direction::Horizontal)
        .with_gap(10.)
        .with_padding(10.)
        .with_children(|ui| {
          Container::default()
            .with_size(size!(100%, auto))
            .with_align(Alignment::Center)
            .with_children(|ui| {
              Text::new("Settings")
                .with_text_size(32)
                .add_child(ui);
            })
            .add_child(ui);
          Break.add_child(ui);

          Text::new("Render Distance")
            .add_child(ui);
          Slider::new(settings.render_distance as f32 / 16.)
            .with_size(size!(300, auto))
            .on_change(|f| SettingsSignal::SetRenderDistance((f * 16.).round() as u8))
            .add_child(ui);
          Text::new(format!("{} Chunks", settings.render_distance))
            .add_child(ui);
          Break.add_child(ui);

          checkbox(
            ui,
            "Vsync",
            settings.vsync,
            SettingsSignal::SetEnableVsync
          );
          Break.add_child(ui);

          checkbox(
            ui,
            "Dynamic Crosshair",
            settings.dynamic_crosshair,
            SettingsSignal::SetEnableDynamicCrosshair
          );
          Break.add_child(ui);

          // checkbox(
          //   ui,
          //   "Debug Chunk Border",
          //   settings.debug_draw_current_chunk_border,
          //   SettingsSignal::SetEnableDebugChunkBorder
          // );
          // Break.add_child(ui);

          Text::new("Mouse Sensitivity")
            .add_child(ui);
          Slider::new(settings.mouse_sensitivity / 5.)
            .with_size(size!(300, (Slider::DEFAULT_HEIGHT)))
            .on_change(|f| SettingsSignal::SetMouseSensitivity(5. * f))
            .add_child(ui);
          Text::new(format!("{:.2}", settings.mouse_sensitivity))
            .add_child(ui);
        })
        .add_child(ui);
    })
    .add_root(&mut ui.hui, ren.size_vec2());

  ui.hui.process_signals(|signal: SettingsSignal| match signal {
    SettingsSignal::SetRenderDistance(value) => settings.render_distance = value,
    SettingsSignal::SetEnableDynamicCrosshair(value) => settings.dynamic_crosshair = value,
    SettingsSignal::SetEnableVsync(value) => {
      settings.vsync = value;
      ren.reload_settings(&settings);
    },
    // SettingsSignal::SetEnableDebugChunkBorder(value) => settings.debug_draw_current_chunk_border = value && cfg!(not(target_os = "android")),
    SettingsSignal::SetMouseSensitivity(value) => settings.mouse_sensitivity = value,
  });
}

#[allow(clippy::just_underscores_and_digits)]
pub fn render_settings_ui2(
  _0: NonSendSync<UniqueViewMut<UiState>>,
  _1: UniqueViewMut<Renderer>,
  _2: UniqueViewMut<GameSettings>,
) {
  render_settings_ui(_0, _1, _2);
}
