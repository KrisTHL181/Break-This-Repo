use glam::IVec3;
use serde::{Serialize, Deserialize};
use crate::block::Block;

#[derive(Serialize, Deserialize, Clone, Copy, Debug)]
pub struct QueuedBlock {
  pub position: IVec3,
  pub block_type: Block,
  /// Only replace air blocks
  pub soft: bool,
}
