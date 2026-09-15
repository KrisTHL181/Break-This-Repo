#![cfg_attr(
  all(windows, not(debug_assertions)),
  windows_subsystem = "windows"
)]

fn main() {
  kubilib::kubi_main();
}
