use strum::EnumIter;
use glam::{ivec3, vec3, IVec3, Vec3};
use std::f32::consts::FRAC_1_SQRT_2;
use crate::rendering::world::ChunkVertex;

#[repr(usize)]
#[derive(Clone, Copy, Debug, EnumIter)]
pub enum CubeFace {
  Top    = 0,
  Front  = 4,
  Left   = 2,
  Right  = 3,
  Back   = 1,
  Bottom = 5,
}
impl CubeFace {
  pub const fn normal(self) -> IVec3 {
    CUBE_FACE_NORMALS_IVEC3[self as usize]
  }
}

const CUBE_FACE_VERTICES: [[Vec3; 4]; 6] = [
  [vec3(0., 1., 0.), vec3(0., 1., 1.), vec3(1., 1., 0.), vec3(1., 1., 1.)],
  [vec3(0., 0., 0.), vec3(0., 1., 0.), vec3(1., 0., 0.), vec3(1., 1., 0.)],
  [vec3(0., 0., 1.), vec3(0., 1., 1.), vec3(0., 0., 0.), vec3(0., 1., 0.)],
  [vec3(1., 0., 0.), vec3(1., 1., 0.), vec3(1., 0., 1.), vec3(1., 1., 1.)],
  [vec3(1., 0., 1.), vec3(1., 1., 1.), vec3(0., 0., 1.), vec3(0., 1., 1.)],
  [vec3(0., 0., 1.), vec3(0., 0., 0.), vec3(1., 0., 1.), vec3(1., 0., 0.)],
];
const CUBE_FACE_NORMALS_IVEC3: [IVec3; 6] = [
  ivec3( 0,  1,  0),
  ivec3( 0,  0, -1),
  ivec3(-1,  0,  0),
  ivec3( 1,  0,  0),
  ivec3( 0,  0,  1),
  ivec3( 0, -1,  0)
];
const CUBE_FACE_NORMALS: [Vec3; 6] = [
  vec3(0., 1., 0.),
  vec3(0., 0., -1.),
  vec3(-1.,0., 0.),
  vec3(1., 0., 0.),
  vec3(0., 0., 1.),
  vec3(0., -1.,0.)
];
const CUBE_FACE_INDICES: [u32; 6] = [0, 1, 2, 2, 1, 3];

#[repr(usize)]
pub enum DiagonalFace {
  RigthZ = 0,
  LeftZ  = 1,
}
const CROSS_FACES: [[Vec3; 4]; 2] = [
  [
    vec3(0., 0., 0.),
    vec3(0., 1., 0.),
    vec3(1., 0., 1.),
    vec3(1., 1., 1.),
  ],
  [
    vec3(0., 0., 1.),
    vec3(0., 1., 1.),
    vec3(1., 0., 0.),
    vec3(1., 1., 0.),
  ]
];
const CROSS_FACE_NORMALS: [Vec3; 2] = [
  vec3(-FRAC_1_SQRT_2, 0., FRAC_1_SQRT_2),
  vec3( FRAC_1_SQRT_2, 0., FRAC_1_SQRT_2),
];
const CROSS_FACE_NORMALS_BACK: [Vec3; 2] = [
  vec3( FRAC_1_SQRT_2, 0., -FRAC_1_SQRT_2),
  vec3(-FRAC_1_SQRT_2, 0., -FRAC_1_SQRT_2),
];
const CROSS_FACE_INDICES: [u32; 12] = [
  0, 1, 2, 2, 1, 3, //Front side
  6, 5, 4, 7, 5, 6, //Back side
];


const UV_COORDS: [[f32; 2]; 4] = [
  [0., 1.],
  [0., 0.],
  [1., 1.],
  [1., 0.],
];

#[derive(Default)]
pub struct MeshBuilder {
  offset: Vec3,
  vertex_buffer: Vec<ChunkVertex>,
  index_buffer: Vec<u32>,
  idx_counter: u32,
}
impl MeshBuilder {
  pub fn new() -> Self {
    Self::default()
  }

  pub fn new_with_offset(offset: Vec3) -> Self {
    Self { offset, ..Self::new() }
  }

  pub fn add_face(&mut self, face: CubeFace, coord: IVec3, texture: u8) {
    let coord = coord.as_vec3();
    let face_index = face as usize;
    
    //Push vertices
    let norm = CUBE_FACE_NORMALS[face_index];
    let vert = CUBE_FACE_VERTICES[face_index];
    self.vertex_buffer.reserve(4);
    for i in 0..4 {
      self.vertex_buffer.push(ChunkVertex {
        position: (coord + vert[i] + self.offset).to_array(),
        normal: norm.to_array(),
        uv: UV_COORDS[i],
        tex_index: texture as u32
      });
    }

    //Push indices
    self.index_buffer.extend_from_slice(&CUBE_FACE_INDICES.map(|x| x + self.idx_counter));

    //Increment idx counter
    self.idx_counter += 4;
  }

  pub fn add_diagonal_face(&mut self, coord: IVec3, face_type: DiagonalFace, front_texture: u8, back_texture: u8) {
    //Push vertices
    let face_type = face_type as usize;
    let vertices = CROSS_FACES[face_type];
    let normal_front = CROSS_FACE_NORMALS[face_type].to_array();
    let normal_back = CROSS_FACE_NORMALS_BACK[face_type].to_array();
    self.vertex_buffer.reserve(8);
    for i in 0..4 { //push front vertices
      self.vertex_buffer.push(ChunkVertex {
        position: (coord.as_vec3() + vertices[i] + self.offset).to_array(),
        normal: normal_front,
        uv: UV_COORDS[i],
        tex_index: front_texture as u32
      })
    }
    for i in 0..4 { //push back vertices
      self.vertex_buffer.push(ChunkVertex {
        position: (coord.as_vec3() + vertices[i] + self.offset).to_array(),
        normal: normal_back,
        uv: UV_COORDS[i],
        tex_index: back_texture as u32
      })
    }

    //Push indices
    self.index_buffer.extend_from_slice(&CROSS_FACE_INDICES.map(|x| x + self.idx_counter));

    //Increment idx counter
    self.idx_counter += 8;
  }

  //XXX: needs offset supprt
  // pub fn add_model(&mut self, position: Vec3, vertices: &[ChunkVertex], indices: Option<&[u32]>) {
  //   //push vertices
  //   self.vertex_buffer.extend(vertices.iter().map(|vertex| {
  //     let mut vertex = *vertex;
  //     vertex.position[0] += position.x;
  //     vertex.position[0] += position.y;
  //     vertex.position[0] += position.z;
  //     vertex
  //   }));
  //   //push indices
  //   if let Some(indices) = indices {
  //     self.index_buffer.extend(indices.iter().map(|x| x + self.idx_counter));
  //   } else {
  //     self.index_buffer.extend(0..(self.vertex_buffer.len() as u32));
  //   }
  //   //increment idx counter
  //   self.idx_counter += vertices.len() as u32;
  // }

  pub fn finish(self) -> (Vec<ChunkVertex>, Vec<u32>) {
    (self.vertex_buffer, self.index_buffer)
  }
}
