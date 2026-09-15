use crate::{BlockBehavior, BlockDispatch};
use std::collections::HashMap;
use temper_block_data::{PlacedBlocks, PlacementContext};
use temper_block_properties::Direction;
use temper_blocks_generated::{TorchBlock, WallTorchBlock, WallTorchBlockType};
use temper_core::block_face::BlockFace;
use temper_core::block_state_id::BlockStateId;

impl BlockBehavior for TorchBlock {
    fn get_placement_state(&mut self, context: PlacementContext) -> PlacedBlocks {
        place_torch(context, self.clone())
    }

    fn is_solid(&self) -> bool {
        false
    }
}

impl BlockBehavior for WallTorchBlock {
    fn get_placement_state(&mut self, context: PlacementContext) -> PlacedBlocks {
        place_torch(
            context,
            match self.block_type {
                WallTorchBlockType::WallTorch => TorchBlock::Torch,
                WallTorchBlockType::SoulWallTorch => TorchBlock::SoulTorch,
            },
        )
    }

    fn is_solid(&self) -> bool {
        false
    }
}

fn place_torch(context: PlacementContext, ty: TorchBlock) -> PlacedBlocks {
    let block = match context.face {
        BlockFace::Top | BlockFace::Bottom => {
            if context
                .level
                .get_block(context.block_pos.below(), context.dimension)
                .unwrap_or_default()
                .is_solid()
            {
                ty.try_into()
                    .expect("Should be able to convert TorchBlock to id")
            } else {
                return PlacedBlocks {
                    blocks: HashMap::with_capacity(0),
                    take_item: false,
                    place_original: false,
                };
            }
        }
        face => {
            let block_type = match ty {
                TorchBlock::Torch => WallTorchBlockType::WallTorch,
                TorchBlock::SoulTorch => WallTorchBlockType::SoulWallTorch,
            };

            let facing = match face {
                BlockFace::East => Direction::East,
                BlockFace::West => Direction::West,
                BlockFace::North => Direction::North,
                BlockFace::South => Direction::South,
                _ => unreachable!(),
            };

            WallTorchBlock { block_type, facing }
                .try_into()
                .expect("Should be able to convert")
        }
    };

    PlacedBlocks {
        blocks: [(context.block_pos, BlockStateId::new(block))]
            .into_iter()
            .collect::<HashMap<_, _>>(),
        take_item: true,
        place_original: false,
    }
}
