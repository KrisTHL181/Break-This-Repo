use glam::{ivec3, IVec3};
use strum::IntoEnumIterator;
use kubi_shared::block::{Block, RenderType, Transparency};
use crate::world::chunk::CHUNK_SIZE;
use crate::rendering::world::ChunkVertex;

pub mod data;
mod builder;

use data::MeshGenData;
use builder::{MeshBuilder, CubeFace, DiagonalFace};

pub fn generate_mesh(position: IVec3, data: MeshGenData) -> (
  (Vec<ChunkVertex>, Vec<u32>),
  (Vec<ChunkVertex>, Vec<u32>),
) {
  let get_block = |pos: IVec3| -> Block {
    if pos.x < 0 {
      data.block_data_neg_x[(CHUNK_SIZE as i32 + pos.x) as usize][pos.y as usize][pos.z as usize]
    } else if pos.x >= CHUNK_SIZE as i32 {
      data.block_data_pos_x[pos.x as usize - CHUNK_SIZE][pos.y as usize][pos.z as usize]
    } else if pos.y < 0 {
      data.block_data_neg_y[pos.x as usize][(CHUNK_SIZE as i32 + pos.y) as usize][pos.z as usize]
    } else if pos.y >= CHUNK_SIZE as i32 {
      data.block_data_pos_y[pos.x as usize][pos.y as usize - CHUNK_SIZE][pos.z as usize]
    } else if pos.z < 0 {
      data.block_data_neg_z[pos.x as usize][pos.y as usize][(CHUNK_SIZE as i32 + pos.z) as usize]
    } else if pos.z >= CHUNK_SIZE as i32 {
      data.block_data_pos_z[pos.x as usize][pos.y as usize][pos.z as usize - CHUNK_SIZE]
    } else {
      data.block_data[pos.x as usize][pos.y as usize][pos.z as usize]
    }
  };

  let mut builder = MeshBuilder::new_with_offset((position * CHUNK_SIZE as i32).as_vec3());
  let mut trans_builder = MeshBuilder::new_with_offset((position * CHUNK_SIZE as i32).as_vec3());

  for x in 0..CHUNK_SIZE as i32 {
    for y in 0..CHUNK_SIZE as i32 {
      for z in 0..CHUNK_SIZE as i32 {
        let coord = ivec3(x, y, z);
        let block = get_block(coord);
        let descriptor = block.descriptor();
        match descriptor.render {
          RenderType::None => continue,
          RenderType::Cube(trans_type, textures) => {
            for face in CubeFace::iter() {
              let facing_direction = face.normal();
              let facing_coord = coord + facing_direction;
              let facing_block = get_block(facing_coord);
              let facing_descriptor = facing_block.descriptor();
              let face_obstructed = match trans_type {
                Transparency::Solid => matches!(facing_descriptor.render, RenderType::Cube(Transparency::Solid, _)),
                Transparency::Binary | Transparency::Trans => {
                  match facing_descriptor.render {
                    RenderType::Cube(trans_type, _) => match trans_type {
                      Transparency::Solid => true,
                      Transparency::Binary | Transparency::Trans => block == facing_block,
                    },
                    _ => false,
                  }
                },
              };
              if !face_obstructed {
                let face_texture = match face {
                  CubeFace::Top    => textures.top,
                  CubeFace::Front  => textures.front,
                  CubeFace::Left   => textures.left,
                  CubeFace::Right  => textures.right,
                  CubeFace::Back   => textures.back,
                  CubeFace::Bottom => textures.bottom,
                };
                let target_builder = match trans_type {
                  Transparency::Trans => &mut trans_builder,
                  _ => &mut builder,
                };
                target_builder.add_face(face, coord, face_texture as u8);
              }
            }
          },
          RenderType::Cross(textures) => {
            builder.add_diagonal_face(
              coord, 
              DiagonalFace::LeftZ, 
              textures.0.front as u8, 
              textures.0.back as u8
            );
            builder.add_diagonal_face(
              coord, 
              DiagonalFace::RigthZ, 
              textures.1.front as u8, 
              textures.1.back as u8
            );
          },
        }
      }
    }
  }

  (builder.finish(), trans_builder.finish())
}
