use bevy_ecs::component::Component;
use bitcode_derive::{Decode, Encode};
use std::collections::HashMap;
use type_hash::TypeHash;
use uuid::Uuid;

#[derive(Debug, Clone, Copy, Decode, Encode, TypeHash, Default, Eq, PartialEq)]
pub enum BossbarSenderState {
    Additive,
    Subtractive,
    Update,

    #[default]
    Informed,
}

#[derive(Component, Debug, Clone, Default)]
pub struct BossbarSender(pub HashMap<Uuid, BossbarSenderState>);

impl BossbarSender {
    pub fn add(&mut self, uuid: Uuid) {
        self.0.insert(uuid, BossbarSenderState::Additive);
    }

    pub fn update(&mut self, uuid: Uuid) {
        if matches!(
            self.0.get(&uuid),
            Some(BossbarSenderState::Informed | BossbarSenderState::Update)
        ) {
            self.0.insert(uuid, BossbarSenderState::Update);
        }
    }

    pub fn remove(&mut self, uuid: Uuid) {
        self.0.insert(uuid, BossbarSenderState::Subtractive);
    }

    pub fn informed(&mut self, uuid: Uuid) {
        match self.0.get(&uuid) {
            Some(BossbarSenderState::Subtractive) => {
                self.0.remove(&uuid);
            }
            Some(_) => {
                self.0.insert(uuid, BossbarSenderState::Informed);
            }
            None => {}
        }
    }

    pub fn get_state(&self, uuid: Uuid) -> Option<BossbarSenderState> {
        self.0.get(&uuid).cloned()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn update_does_not_start_tracking_unknown_bossbar() {
        let uuid = Uuid::new_v4();
        let mut sender = BossbarSender::default();

        sender.update(uuid);

        assert_eq!(sender.get_state(uuid), None);
    }

    #[test]
    fn update_marks_informed_bossbar_for_update() {
        let uuid = Uuid::new_v4();
        let mut sender = BossbarSender::default();

        sender.add(uuid);
        sender.informed(uuid);
        sender.update(uuid);

        assert_eq!(sender.get_state(uuid), Some(BossbarSenderState::Update));
    }
}
