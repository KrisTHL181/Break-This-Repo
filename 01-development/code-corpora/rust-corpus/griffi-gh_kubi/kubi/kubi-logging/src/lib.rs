//! Custom env_logger options and styling

/// Custom env_logger options and styling
#[inline]
#[cfg(not(target_os = "android"))]
pub fn init() {
  use log::Level;
  use std::io::Write;
  use env_logger::{fmt::Color, Builder, Env};

  let env = Env::default()
    .filter_or("RUST_LOG", "trace,gilrs=warn,rusty_xinput=warn,wgpu=warn,wgpu_core=warn,wgpu_hal=warn,hui=info,hui-winit=info,hui-glium=info,hui-wgpu=info,naga=warn,calloop=warn");
  Builder::from_env(env)
    .format(|buf, record| {
      let mut level_style = buf.style();
      level_style.set_color(match record.level() {
        Level::Error => Color::Red,
        Level::Warn => Color::Yellow,
        Level::Debug | Level::Trace => Color::Cyan,
        _ => Color::Blue
      }).set_bold(true);

      let mut bold_style = buf.style();
      bold_style.set_bold(true);

      let mut location_style = buf.style();
      location_style.set_bold(true);
      location_style.set_dimmed(true);

      let mut location_line_style = buf.style();
      location_line_style.set_dimmed(true);
      
      let text = format!("{}", record.args());

      writeln!(
        buf,
        "{} {:<50}\t{}{}{}{}",
        level_style.value(match record.level() {
          Level::Error => "[e]",
          Level::Warn =>  "[w]",
          Level::Info =>  "[i]",
          Level::Debug => "[d]",
          Level::Trace => "[t]",
        }),
        text,
        bold_style.value((text.len() > 50).then_some("\n ╰─ ").unwrap_or_default()),
        location_style.value(record.target()),
        location_line_style.value(" :"),
        location_line_style.value(record.line().unwrap_or(0))
      )
    })
    .init();
}

/// Custom env_logger options and styling
#[inline]
#[cfg(target_os = "android")]
pub fn init() {
  use log::LevelFilter;
  use android_logger::Config;
  android_logger::init_once(
    Config::default().with_max_level(LevelFilter::Trace),
  );
}
