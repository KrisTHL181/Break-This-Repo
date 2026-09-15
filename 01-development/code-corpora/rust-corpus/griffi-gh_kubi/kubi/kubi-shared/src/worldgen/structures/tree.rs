use glam::IVec3;
use super::Structure;
use crate::{block::Block, worldgen::WorldGenerator};

#[derive(Clone, Copy, Debug)]
pub struct TreeStructure {
  pub height: i32,
}

impl Default for TreeStructure {
  fn default() -> Self {
    Self { height: 5 }
  }
}

impl Structure for TreeStructure {
  fn place(&self, gen: &mut WorldGenerator, root: IVec3) {
    //check the block below the tree, if it's grass, replace it with dirt
    //XXX: This won't work if root.y == 0
    if root.y != 0 && gen.query(root - IVec3::Y) == Block::Grass {
      gen.place(root - IVec3::Y, Block::Dirt);
    }

    //Tree stem
    for y in root.y..root.y + self.height {
      gen.place_or_queue(IVec3::new(root.x, y, root.z), Block::Wood);
    }

    //Tree leaves
    //Try to create the following shape:
    //(a 5x2x5 cube that wraps around the stem with a 3x1x3 cube on top)
    //  xxx
    // xx|xx
    // xx|xx
    //   |

    for y in 0..=4_i32 {
      for x in -2..=2_i32 {
        for z in -2..=2_i32 {
          //Do not overwrite the stem
          if y < 3 && x == 0 && z == 0 {
            continue
          }
          // Cut off the corners of the top layer
          if y >= 3 && (x.abs() > 1 || z.abs() > 1) {
            continue
          }
          let position = IVec3::new(
            root.x + x,
            root.y + self.height - 3 + y,
            root.z + z
          );
          gen.place_or_queue(position, Block::Leaf);
        }
      }
    }
  }
}
