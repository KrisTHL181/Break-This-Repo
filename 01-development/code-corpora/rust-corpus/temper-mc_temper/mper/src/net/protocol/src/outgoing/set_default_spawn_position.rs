use temper_codec::net_types::network_position::NetworkPosition;
use temper_macros::{NetEncode, packet};

#[derive(NetEncode)]
#[packet(packet_id = "set_default_spawn_position", state = "play")]
pub struct SetDefaultSpawnPositionPacket {
    pub dimension: String,
    pub spawn_position: NetworkPosition,
    pub yaw: f32,
    pub pitch: f32,
}

impl Default for SetDefaultSpawnPositionPacket {
    fn default() -> Self {
        Self::new()
    }
}

impl SetDefaultSpawnPositionPacket {
    pub fn new() -> Self {
        Self {
            dimension: "minecraft:overworld".to_string(),
            spawn_position: NetworkPosition::new(0, 0, 0),
            yaw: 0.0,
            pitch: 0.0,
        }
    }
}
