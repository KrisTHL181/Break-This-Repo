use shipyard::Component;
use serde::{Serialize, Deserialize};

#[derive(Component)]
pub struct Entity;

#[derive(Component, Serialize, Deserialize, Clone, Copy, Debug)]
pub struct Health {
  pub current: u8,
  pub max: u8,
}
impl Health {
  pub fn new(health: u8) -> Self {
    Self {
      current: health,
      max: health
    }
  }
}

// impl PartialEq for Health {
//   fn eq(&self, other: &Self) -> bool {
//     self.current == other.current
//   }
// }
// impl Eq for Health {}
// impl PartialOrd for Health {
//   fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
//     self.current.partial_cmp(&other.current)
//   }
// }
// impl Ord for Health {
//   fn cmp(&self, other: &Self) -> std::cmp::Ordering {
//     self.current.cmp(&other.current)
//   }
// }
