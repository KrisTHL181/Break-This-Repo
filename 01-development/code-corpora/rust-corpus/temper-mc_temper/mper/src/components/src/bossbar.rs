use bevy_ecs::component::Component;
use uuid::Uuid;

#[derive(Component, Debug, Clone, Copy)]
pub struct BossbarOwner(Uuid);

impl Default for BossbarOwner {
    fn default() -> Self {
        Self(Uuid::new_v4())
    }
}

impl BossbarOwner {
    pub fn new(id: Uuid) -> Self {
        BossbarOwner(id)
    }

    pub fn id(&self) -> Uuid {
        self.0
    }
}
