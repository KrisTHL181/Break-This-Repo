use shipyard::{AllStoragesView, Unique};
use serde::{Serialize, Deserialize};
use std::{fs, net::SocketAddr, path::PathBuf};

#[derive(Serialize, Deserialize)]
pub struct ConfigTableServer {
  pub address: SocketAddr,
  pub max_clients: usize,
  pub timeout_ms: u64,
  pub password: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub struct ConfigTableWorld {
  pub file: Option<PathBuf>,
  pub seed: u64,
  pub preheat_radius: u32,
}

#[derive(Serialize, Deserialize)]
pub struct ConfigTableQuery {
  pub name: Option<String>
}

#[derive(Unique, Serialize, Deserialize)]
pub struct ConfigTable {
  pub server: ConfigTableServer,
  pub world: ConfigTableWorld,
  pub query: ConfigTableQuery,
}

pub fn read_config(
  storages: AllStoragesView,
) {
  log::info!("Reading config...");
  let config_str = fs::read_to_string("Server.toml").expect("No config file found");
  let config: ConfigTable = toml::from_str(&config_str).expect("Invalid configuration file");
  storages.add_unique(config);
}
