use bevy_ecs::prelude::Component;

#[derive(Debug, Default, Clone)]
pub struct PlayerProperty {
    pub name: String,
    pub value: String,
    pub signature: Option<String>,
}

#[derive(Debug, Component, Default, Clone)]
pub struct PlayerProperties {
    pub properties: Vec<PlayerProperty>,
}
