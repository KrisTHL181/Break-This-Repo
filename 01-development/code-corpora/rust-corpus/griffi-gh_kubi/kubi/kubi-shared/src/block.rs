use glam::{vec4, Vec4};
use serde::{Serialize, Deserialize};
use strum::EnumIter;
use num_enum::TryFromPrimitive;
use crate::item::Item;

#[derive(Serialize, Deserialize, Clone, Copy, Debug, EnumIter)]
#[repr(u8)]
pub enum BlockTexture {
  Stone,
  Dirt,
  GrassTop,
  GrassSide,
  Sand,
  Bedrock,
  Wood,
  WoodTop,
  Leaf,
  Torch,
  TallGrass,
  Snow,
  GrassSideSnow,
  Cobblestone,
  Planks,
  WaterSolid,
  Water,
}

#[derive(Serialize, Deserialize, Clone, Copy, Debug, Default, PartialEq, Eq, EnumIter, TryFromPrimitive)]
#[repr(u8)]
pub enum Block {
  #[default]
  Air,
  Marker,
  Stone,
  Dirt,
  Grass,
  Sand,
  Cobblestone,
  TallGrass,
  Planks,
  Torch,
  Wood,
  Leaf,
  Water,
}

impl Block {
  #[inline]
  pub const fn descriptor(self) -> BlockDescriptor {
    match self {
      Self::Air => BlockDescriptor {
        name: "air",
        render: RenderType::None,
        collision: CollisionType::None,
        raycast_collision: false,
        drops: None,
        submerge: None,
      },
      Self::Marker => BlockDescriptor {
        name: "marker",
        render: RenderType::None,
        collision: CollisionType::None,
        raycast_collision: false,
        drops: None,
        submerge: None,
      },
      Self::Stone => BlockDescriptor {
        name: "stone",
        render: RenderType::Cube(
          Transparency::Solid,
          CubeTexture::all(BlockTexture::Stone)
        ),
        collision: CollisionType::Solid,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Dirt => BlockDescriptor {
        name: "dirt",
        render: RenderType::Cube(
          Transparency::Solid,
          CubeTexture::all(BlockTexture::Dirt)
        ),
        collision: CollisionType::Solid,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Grass => BlockDescriptor {
        name: "grass",
        render: RenderType::Cube(
          Transparency::Solid,
          CubeTexture::top_sides_bottom(
            BlockTexture::GrassTop,
            BlockTexture::GrassSide,
            BlockTexture::Dirt
          )
        ),
        collision: CollisionType::Solid,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Sand => BlockDescriptor {
        name: "sand",
        render: RenderType::Cube(
          Transparency::Solid,
          CubeTexture::all(BlockTexture::Sand)
        ),
        collision: CollisionType::Solid,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Cobblestone => BlockDescriptor {
        name: "cobblestone",
        render: RenderType::Cube(
          Transparency::Solid,
          CubeTexture::all(BlockTexture::Cobblestone)
        ),
        collision: CollisionType::Solid,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::TallGrass => BlockDescriptor {
        name: "tall grass",
        render: RenderType::Cross(CrossTexture::all(BlockTexture::TallGrass)),
        collision: CollisionType::None,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Planks => BlockDescriptor {
        name: "planks",
        render: RenderType::Cube(
          Transparency::Solid,
          CubeTexture::all(BlockTexture::Planks)
        ),
        collision: CollisionType::Solid, 
        raycast_collision: true, 
        drops: None,
        submerge: None,
      },
      Self::Torch => BlockDescriptor {
        name: "torch",
        render: RenderType::Cross(CrossTexture::all(BlockTexture::Torch)),
        collision: CollisionType::None,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Wood => BlockDescriptor {
        name: "leaf",
        render: RenderType::Cube(
          Transparency::Solid,
          CubeTexture::horizontal_vertical(BlockTexture::Wood, BlockTexture::WoodTop)
        ),
        collision: CollisionType::Solid,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Leaf => BlockDescriptor {
        name: "leaf",
        render: RenderType::Cube(
          Transparency::Binary,
          CubeTexture::all(BlockTexture::Leaf)
        ),
        collision: CollisionType::Solid,
        raycast_collision: true,
        drops: None,
        submerge: None,
      },
      Self::Water => BlockDescriptor {
        name: "water",
        render: RenderType::Cube(
          Transparency::Trans,
          CubeTexture::all(BlockTexture::Water)
        ),
        collision: CollisionType::None,
        raycast_collision: true,
        drops: None,
        submerge: Some(vec4(0., 0., 0.25, 0.75)),
      },
    }
  }
}

#[derive(Clone, Copy, Debug)]
pub struct BlockDescriptor {
  pub name: &'static str,
  pub render: RenderType,
  pub collision: CollisionType,
  pub raycast_collision: bool,
  pub drops: Option<Item>,
  pub submerge: Option<Vec4>,
}

#[derive(Clone, Copy, Debug)]
pub struct CubeTexture {
  pub top: BlockTexture,
  pub bottom: BlockTexture,
  pub left: BlockTexture,
  pub right: BlockTexture,
  pub front: BlockTexture,
  pub back: BlockTexture,
}
impl CubeTexture {
  pub const fn top_sides_bottom(top: BlockTexture, sides: BlockTexture, bottom: BlockTexture) -> Self {
    Self {
      top,
      bottom,
      left: sides,
      right: sides,
      front: sides,
      back: sides,
    }
  }
  pub const fn horizontal_vertical(horizontal: BlockTexture, vertical: BlockTexture) -> Self {
    Self::top_sides_bottom(vertical, horizontal, vertical)
  }
  pub const fn all(texture: BlockTexture) -> Self {
    Self::horizontal_vertical(texture, texture)
  }
}

#[derive(Clone, Copy, Debug)]
pub struct CrossTextureSides {
  pub front: BlockTexture,
  pub back: BlockTexture
}
impl CrossTextureSides {
  pub const fn all(texture: BlockTexture) -> Self {
    Self {
      front: texture,
      back: texture
    }
  }
}

#[derive(Clone, Copy, Debug)]
pub struct CrossTexture(pub CrossTextureSides, pub CrossTextureSides);
impl CrossTexture {
  pub const fn all(texture: BlockTexture) -> Self {
    Self(
      CrossTextureSides::all(texture), 
      CrossTextureSides::all(texture)
    )
  }
}

#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum CollisionType {
  None,
  Solid,
}

#[derive(Clone, Copy, Debug)]
pub enum Transparency {
  Solid,
  Binary,
  Trans,
}

#[derive(Clone, Copy, Debug)]
pub enum RenderType {
  None,
  Cube(Transparency, CubeTexture),
  Cross(CrossTexture),
}
