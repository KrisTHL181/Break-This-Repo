use bevy_ecs::prelude::Resource;
use std::collections::HashMap;
use temper_text::TextComponent;
use uuid::Uuid;

mod bossbar_data;
mod update_kinds;

pub use bossbar_data::*;
pub use update_kinds::*;

#[derive(Resource)]
pub struct BossBarResource {
    pub update_queue: crossbeam_queue::SegQueue<(Uuid, UpdateBBKind)>,
    pub boss_bars: HashMap<Uuid, BossBarData>,
}

impl Default for BossBarResource {
    fn default() -> Self {
        Self::new()
    }
}

impl BossBarResource {
    pub fn new() -> Self {
        Self {
            update_queue: Default::default(),
            boss_bars: Default::default(),
        }
    }

    pub fn add_bar(&self, data: BossBarData) -> Uuid {
        let uuid = Uuid::new_v4();
        self.update_queue.push((uuid, UpdateBBKind::Add { data }));
        uuid
    }

    pub fn register_bar_with_id(&mut self, uuid: Uuid, data: BossBarData) {
        self.boss_bars.insert(uuid, data.clone());
        self.update_queue.push((uuid, UpdateBBKind::Add { data }));
    }

    pub fn remove_bar(&self, uuid: Uuid) {
        self.update_queue.push((uuid, UpdateBBKind::Remove));
    }

    pub fn update_health(&self, uuid: Uuid, new_health: f32, new_max: f32) {
        self.update_queue.push((
            uuid,
            UpdateBBKind::UpdateHealth {
                new_health,
                new_max,
            },
        ));
    }

    pub fn update_title(&self, uuid: Uuid, title: TextComponent) {
        self.update_queue
            .push((uuid, UpdateBBKind::UpdateTitle { title }));
    }

    pub fn update_style(&self, uuid: Uuid, color: BossbarColor, dividers: BossbarDividers) {
        self.update_queue
            .push((uuid, UpdateBBKind::UpdateStyle { color, dividers }));
    }

    pub fn update_flags(&self, uuid: Uuid, flags: BossbarFlags) {
        self.update_queue
            .push((uuid, UpdateBBKind::UpdateFlags { flags }));
    }

    pub fn queue_networking(&self, uuid: Uuid, additive: bool) {
        self.update_queue
            .push((uuid, UpdateBBKind::UpdateNetworking { additive }));
    }
}
