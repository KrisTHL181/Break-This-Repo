use crate::world::{
  neighbors::AllChunkNeighbors,
  chunk::BlockData
};

pub struct MeshGenData {
  pub block_data: BlockData,
  pub block_data_pos_z: BlockData,
  pub block_data_neg_z: BlockData,
  pub block_data_pos_y: BlockData,
  pub block_data_neg_y: BlockData,
  pub block_data_pos_x: BlockData,
  pub block_data_neg_x: BlockData,
}
impl AllChunkNeighbors<'_> {
  pub fn mesh_data(&self) -> Option<MeshGenData> {
    let center_block_data = self.center.block_data.as_ref()?;
    let front_block_data = self.front.block_data.as_ref()?;
    let back_block_data = self.back.block_data.as_ref()?;
    let top_block_data = self.top.block_data.as_ref()?;
    let bottom_block_data = self.bottom.block_data.as_ref()?;
    let right_block_data = self.right.block_data.as_ref()?;
    let left_block_data = self.left.block_data.as_ref()?;
    Some(MeshGenData {
      block_data: center_block_data.blocks.clone(),
      block_data_pos_z: front_block_data.blocks.clone(),
      block_data_neg_z: back_block_data.blocks.clone(),
      block_data_pos_y: top_block_data.blocks.clone(),
      block_data_neg_y: bottom_block_data.blocks.clone(),
      block_data_pos_x: right_block_data.blocks.clone(),
      block_data_neg_x: left_block_data.blocks.clone(),
    })
  }
}
