use crate::configuration::require_boolean::{require_false, require_true};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
#[serde(untagged)]
pub enum BoundariesConfig {
    Enabled(EnabledBoundariesConfig),
    Disabled(DisabledBoundariesConfig),
}

#[derive(Serialize, Deserialize)]
#[serde(deny_unknown_fields)]
pub struct EnabledBoundariesConfig {
    #[serde(deserialize_with = "require_true")]
    enabled: bool,
    pub min_y: i32,
    pub teleport_message: String,
}

#[derive(Serialize, Deserialize)]
pub struct DisabledBoundariesConfig {
    #[serde(deserialize_with = "require_false")]
    enabled: bool,
}

impl Default for BoundariesConfig {
    fn default() -> Self {
        Self::Enabled(EnabledBoundariesConfig {
            enabled: true,
            min_y: -64,
            teleport_message: "<red>You have reached the bottom of the world.</red>".into(),
        })
    }
}
