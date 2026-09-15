use glam::IVec3;
use super::WorldGenerator;

mod tree;
pub use tree::TreeStructure;

pub trait Structure {
  fn place(&self, gen: &mut WorldGenerator, root_pos: IVec3);
}
