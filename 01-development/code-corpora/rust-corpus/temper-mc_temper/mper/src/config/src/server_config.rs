//! # The server configuration module.
//!
//! Contains the server configuration struct and its related functions.

use figment::providers::Format;
use serde_derive::{Deserialize, Serialize};
use std::env::temp_dir;
use temper_general_purpose::paths::get_root_path;
pub(crate) const DEFAULT_CONFIG: &str =
    include_str!("../../../assets/data/configs/main-config.toml");

/// The server configuration struct.
///
/// Fields:
/// - `host`: The IP/host that the server will bind to.
/// - `port`: The port that the server will bind to. (0-65535)
/// - `motd`: The message of the day that is displayed to clients. It will randomly select one from the list.
/// - `max_players`: The maximum number of players that can be connected to the server.
/// - `tps`: The ticks per second that the server will run at.
/// - `database` - [DatabaseConfig]: The configuration for the database.
/// - `world`: The name of the world that the server will load.
/// - `network_compression_threshold`: The threshold at which the server will compress network packets.
/// - `whitelist`: Whether the server whitelist is enabled or not.
/// - `chunk_render_distance`: The render distance of the chunks. This is the number of chunks that will be
///   loaded around the player.
/// - `op_by_default`: Whether players are op by default or not.
/// - `default_gamemode`: The default gamemode that players will be in when they join the server.
/// - `block_scanner_ips`: Whether to enable the block scanner IPs feature. This will block IPs that are known to be used by scanners.
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct ServerConfig {
    pub host: String,
    pub port: u16, // 0-65535
    pub motd: Vec<String>,
    pub max_players: u32,
    pub tps: u32,
    pub database: DatabaseConfig,
    pub world: String,
    pub network_compression_threshold: i32, // Can be negative
    pub verify_decompressed_packets: bool,
    pub encryption_enabled: bool,
    pub online_mode: bool,
    pub whitelist: bool,
    pub chunk_render_distance: u32,
    pub op_by_default: bool,
    pub default_gamemode: String,
    pub block_scanner_ips: bool,
    pub dashboard: DashboardConfig,
    pub performance: PerformanceConfig,
    pub world_gen: WorldGenConfig,
}

/// The database configuration section from [ServerConfig].
///
/// Fields:
/// - `db_path`: The path to the database. This is relative to the server root path.
/// - `verify_chunk_data`: Whether to verify chunk data when loading it from the database.
/// - `map_size`: The max size of the database's memory map. Basically you need this to be big enough
///   to hold everything before it starts writing to disk. This isn't memory use though, it's just
///   how much we can map into memory if needed, so you can set this to an insane number if you want,
///   but it won't actually use that much memory, it'll just show up as virtual memory use.
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct DatabaseConfig {
    pub db_path: String,
    pub verify_chunk_data: bool,
    pub map_size: u64,
}

/// The dashboard configuration section from [ServerConfig].
///
/// Fields:
/// - `port`: The port that the dashboard will bind to. (0-65535)
/// - `secret`: The secret key for accessing the dashboard.
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct DashboardConfig {
    pub serve_dashboard: bool,
    pub serve_page: bool,
    pub port: u16,
    pub secret: String,
}

#[derive(Debug, Deserialize, Serialize, Default)]
pub struct PerformanceConfig {
    pub chunks_per_tick_min: u32,
    pub chunks_per_tick: i32,
}

/// World generation config
///
/// Fields:
/// - `seed`: The seed to use
/// - `generator`: The generator to use
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct WorldGenConfig {
    pub seed: String,
    pub generator: String,
}

pub fn create_config() -> ServerConfig {
    let config_location = get_root_path().join("configs");
    let main_config_file = config_location.join("config.toml");
    match figment::Figment::new()
        // Load the default configuration
        .merge(figment::providers::Toml::string(DEFAULT_CONFIG))
        // Then override it with the main config file
        .merge(figment::providers::Toml::file(main_config_file))
        .extract()
    {
        Ok(config) => config,
        Err(e) => {
            eprintln!("Failed to load server configuration: {e}");
            std::process::exit(1);
        }
    }
}

pub fn create_dummy_config() -> ServerConfig {
    let db_path = temp_dir();
    ServerConfig {
        host: "0.0.0.0".to_string(),
        port: 25565,
        motd: vec!["A Temper Server".to_string()],
        max_players: 100,
        tps: 20,
        database: DatabaseConfig {
            db_path: db_path.to_string_lossy().to_string(),
            verify_chunk_data: true,
            map_size: 1024,
        },
        world: "world".to_string(),
        network_compression_threshold: 256,
        verify_decompressed_packets: true,
        encryption_enabled: false,
        online_mode: false,
        whitelist: false,
        chunk_render_distance: 8,
        default_gamemode: "survival".to_string(),
        block_scanner_ips: true,
        dashboard: DashboardConfig {
            serve_dashboard: false,
            serve_page: false,
            port: 8080,
            secret: "not very secret".to_string(),
        },
        op_by_default: true,
        performance: PerformanceConfig {
            chunks_per_tick_min: 5,
            chunks_per_tick: 10,
        },
        world_gen: WorldGenConfig {
            seed: "dummy".to_string(),
            generator: "normal".to_string(),
        },
    }
}
