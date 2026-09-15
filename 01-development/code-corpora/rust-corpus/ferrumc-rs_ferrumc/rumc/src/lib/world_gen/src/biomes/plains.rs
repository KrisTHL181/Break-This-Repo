//! Plains biome: grass over dirt, with sparse oak trees.

use crate::BiomeGenerator;
use crate::biomes::tree_placement::TreePlacer;
use crate::biomes::trees::{Tree, TreeKind};
use crate::errors::WorldGenError;
use crate::terrain_noise::NoiseGenerator;
use ferrumc_macros::block;
use ferrumc_world::block_state_id::BlockStateId;
use ferrumc_world::chunk::Chunk;
use ferrumc_world::pos::ChunkBlockPos;

/// Minimum surface height for a tree to spawn. Columns below this are close to water and should
/// remain treeless (they are often on beach/ocean transitions).
const TREE_MIN_SURFACE_Y: i16 = 64;

pub(crate) struct PlainsBiome {
    dirt_depth_noise: NoiseGenerator,
    trees: TreePlacer,
}

impl BiomeGenerator for PlainsBiome {
    fn biome_id(&self) -> u8 {
        40 // minecraft:plains
    }

    fn _biome_name(&self) -> String {
        "plains".to_string()
    }

    fn decorate(
        &self,
        chunk: &mut Chunk,
        x: u8,
        z: u8,
        surface_y: i16,
    ) -> Result<(), WorldGenError> {
        chunk.set_block(
            ChunkBlockPos::new(x, surface_y, z),
            block!("grass_block", { snowy: false }),
        );

        let dirt_depth = (self.dirt_depth_noise.get(f32::from(x), f32::from(z)) * 5.0) + 3.0;
        for i in 1..=dirt_depth as i16 {
            chunk.set_block(ChunkBlockPos::new(x, surface_y - i, z), block!("dirt"));
        }

        Ok(())
    }

    fn tree_at(&self, global_x: i32, global_z: i32, surface_y: i16) -> Option<Tree> {
        if !self.trees.should_place_tree(global_x, global_z, surface_y) {
            return None;
        }
        Some(Tree {
            kind: TreeKind::Oak,
            surface_y,
            trunk_height: self.trees.trunk_height(global_x, global_z, 4, 3),
        })
    }

    fn new(seed: u64) -> Self {
        PlainsBiome {
            dirt_depth_noise: NoiseGenerator::new(seed, 0.1, 4, None),
            // Sparse oaks: wide spacing, large grove patches with open clearings between them.
            trees: TreePlacer::new(seed.wrapping_add(7), 0.012, 4, 0.70, TREE_MIN_SURFACE_Y),
        }
    }
}
