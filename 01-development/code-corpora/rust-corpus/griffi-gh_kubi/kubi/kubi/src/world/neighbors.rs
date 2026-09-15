use glam::{IVec3, ivec3};
use super::chunk::Chunk;

#[derive(Clone, Copy)]
pub struct ChunkNeighbors<'a> {
  pub center: Option<&'a Chunk>,
  pub top:    Option<&'a Chunk>,
  pub bottom: Option<&'a Chunk>,
  pub left:   Option<&'a Chunk>,
  pub right:  Option<&'a Chunk>,
  pub front:  Option<&'a Chunk>,
  pub back:   Option<&'a Chunk>,
}
#[derive(Clone, Copy)]
pub struct AllChunkNeighbors<'a> {
  pub center: &'a Chunk,
  pub top:    &'a Chunk,
  pub bottom: &'a Chunk,
  pub left:   &'a Chunk,
  pub right:  &'a Chunk,
  pub front:  &'a Chunk,
  pub back:   &'a Chunk,
}
pub struct AllChunkNeighborsMut<'a> {
  pub center: &'a mut Chunk,
  pub top:    &'a mut Chunk,
  pub bottom: &'a mut Chunk,
  pub left:   &'a mut Chunk,
  pub right:  &'a mut Chunk,
  pub front:  &'a mut Chunk,
  pub back:   &'a mut Chunk,
}

impl<'a> ChunkNeighbors<'a> {
  pub fn all(&self) -> Option<AllChunkNeighbors<'a>> {
    Some(AllChunkNeighbors {
      center: self.center?,
      top:    self.top?,
      bottom: self.bottom?,
      left:   self.left?,
      right:  self.right?,
      front:  self.front?,
      back:   self.back?,
    })
  }
}
impl<'a> From<AllChunkNeighborsMut<'a>> for AllChunkNeighbors<'a> {
  fn from(neighbors: AllChunkNeighborsMut<'a>) -> Self {
    AllChunkNeighbors {
      center: neighbors.center,
      top:    neighbors.top,
      bottom: neighbors.bottom,
      left:   neighbors.left,
      right:  neighbors.right,
      front:  neighbors.front,
      back:   neighbors.back,
    }
  }
}
impl<'a> From<AllChunkNeighbors<'a>> for ChunkNeighbors<'a> {
  fn from(neighbors: AllChunkNeighbors<'a>) -> Self {
    ChunkNeighbors {
      center: Some(neighbors.center),
      top:    Some(neighbors.top),
      bottom: Some(neighbors.bottom),
      left:   Some(neighbors.left),
      right:  Some(neighbors.right),
      front:  Some(neighbors.front),
      back:   Some(neighbors.back),
    }
  }
}
impl<'a> From<AllChunkNeighborsMut<'a>> for ChunkNeighbors<'a> {
  fn from(neighbors: AllChunkNeighborsMut<'a>) -> Self {
    ChunkNeighbors {
      center: Some(neighbors.center),
      top:    Some(neighbors.top),
      bottom: Some(neighbors.bottom),
      left:   Some(neighbors.left),
      right:  Some(neighbors.right),
      front:  Some(neighbors.front),
      back:   Some(neighbors.back),
    }
  }
}

impl super::ChunkStorage {
  pub fn neighbors(&self, coords: IVec3) -> ChunkNeighbors {
    ChunkNeighbors {
      center: self.chunks.get(&coords),
      top:    self.chunks.get(&(coords + ivec3(0, 1, 0))),
      bottom: self.chunks.get(&(coords - ivec3(0, 1, 0))),
      left:   self.chunks.get(&(coords - ivec3(1, 0, 0))),
      right:  self.chunks.get(&(coords + ivec3(1, 0, 0))),
      front:  self.chunks.get(&(coords + ivec3(0, 0, 1))),
      back:   self.chunks.get(&(coords - ivec3(0, 0, 1))),
    }
  }
  pub fn neighbors_all(&self, coords: IVec3) -> Option<AllChunkNeighbors> {
    self.neighbors(coords).all()
  }
  pub fn neighbors_all_mut(&mut self, coords: IVec3) -> Option<AllChunkNeighborsMut> {
    let [
      center,
      top,
      bottom,
      left,
      right,
      front,
      back
    ] = self.chunks.get_many_mut([
      &coords,
      &(coords + ivec3(0, 1, 0)),
      &(coords - ivec3(0, 1, 0)),
      &(coords - ivec3(1, 0, 0)),
      &(coords + ivec3(1, 0, 0)),
      &(coords + ivec3(0, 0, 1)),
      &(coords - ivec3(0, 0, 1)),
    ]);
    Some(AllChunkNeighborsMut {
      center: center?,
      top:    top?,
      bottom: bottom?,
      left:   left?,
      right:  right?,
      front:  front?,
      back:   back?,
    })
  }
}
