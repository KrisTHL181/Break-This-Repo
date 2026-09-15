use temper_codec::net_types::length_prefixed_vec::LengthPrefixedVec;
use temper_codec::net_types::network_position::NetworkPosition;
use temper_codec::net_types::var_int::VarInt;
use temper_config::ServerConfig;
use temper_macros::{NetEncode, packet};

#[derive(NetEncode)]
#[packet(packet_id = "login", state = "play")]
pub struct LoginPlayPacket<'a> {
    pub entity_id: i32,
    pub is_hardcore: bool,
    pub dimension_names: LengthPrefixedVec<&'a str>,
    pub max_players: VarInt,
    pub view_distance: VarInt,
    pub simulation_distance: VarInt,
    pub reduced_debug_info: bool,
    pub enable_respawn_screen: bool,
    pub do_limited_crafting: bool,
    pub dimension_type: VarInt,
    pub dimension_name: &'a str,
    pub seed_hash: i64,
    pub gamemode: u8,
    pub previous_gamemode: i8,
    pub is_debug: bool,
    pub is_flat: bool,
    pub has_death_location: bool,
    pub death_dimension_name: Option<&'a str>,
    pub death_location: Option<NetworkPosition>, // change this to actual Position. this won't work!!
    pub portal_cooldown: VarInt,
    pub sea_level: VarInt,
    pub online_mode: bool,
    pub enforces_secure_chat: bool,
}

impl LoginPlayPacket<'_> {
    pub fn new(conn_id: i32, gamemode: u8, config: &ServerConfig) -> Self {
        Self {
            entity_id: conn_id,
            is_hardcore: false,
            dimension_names: LengthPrefixedVec::new(["minecraft:overworld"].into()),
            max_players: VarInt::from(config.max_players as i32),
            view_distance: VarInt::from(config.chunk_render_distance as i32),
            simulation_distance: VarInt::from(config.chunk_render_distance as i32),
            reduced_debug_info: false,
            enable_respawn_screen: true,
            do_limited_crafting: false,
            dimension_type: VarInt::new(0),
            dimension_name: "minecraft:overworld",
            seed_hash: 0,
            gamemode,
            previous_gamemode: -1,
            is_debug: false,
            is_flat: false,
            has_death_location: false,
            death_dimension_name: None,
            death_location: None,
            portal_cooldown: VarInt::from(0),
            sea_level: VarInt::from(63),
            online_mode: config.online_mode,
            enforces_secure_chat: false,
        }
    }
}
