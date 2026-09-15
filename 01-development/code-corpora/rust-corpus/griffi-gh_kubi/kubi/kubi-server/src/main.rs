use shipyard::{IntoWorkload, Workload, WorkloadModificator, World};
use std::{thread, time::Duration};
use kubi_shared::fixed_timestamp::{FixedTimestamp, init_fixed_timestamp_storage};

mod util;
mod config;
mod server;
mod client;
mod world;
mod auth;

use config::read_config;
use server::{bind_server, update_server, log_server_errors};
use client::{init_client_maps, on_client_disconnect, sync_client_positions};
use auth::authenticate_players;
use world::{init_world, save::save_modified, update_world};

fn initialize() -> Workload {
  (
    init_fixed_timestamp_storage,
    read_config,
    bind_server,
    init_client_maps,
    init_world.after_all(read_config),
  ).into_workload()
}

fn update() -> Workload {
  (
    update_server,
    (
      log_server_errors,
      authenticate_players,
      update_world,
      sync_client_positions,
      on_client_disconnect,
    ).into_workload(),
    save_modified
      .into_workload()
      .make_fixed(10000, 0),
  ).into_sequential_workload()
}

fn main() {
  kubi_logging::init();
  let world = World::new();
  world.add_workload(initialize);
  world.add_workload(update);
  world.run_workload(initialize).unwrap();
  log::info!("The server is now running");
  loop {
    world.run_workload(update).unwrap();
    thread::sleep(Duration::from_millis(16));
  }
}
