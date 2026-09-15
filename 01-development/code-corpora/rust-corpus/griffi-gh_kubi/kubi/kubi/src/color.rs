use glam::{Vec4, vec4};

#[inline(always)]
pub fn color_rgba(r: u8, g: u8, b: u8, a: u8) -> Vec4 {
  vec4(r as f32 / 255., g as f32 / 255., b as f32 / 255., a as f32 / 255.)
}

#[inline(always)]
pub fn color_hex(c: u32) -> Vec4 {
  let c = c.to_be_bytes();
  color_rgba(c[0], c[1], c[2], c[3])
}
