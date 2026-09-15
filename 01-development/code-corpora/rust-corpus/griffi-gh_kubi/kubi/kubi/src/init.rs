use shipyard::{AllStoragesView, UniqueViewMut};
use std::{env, net::SocketAddr, path::Path};
use crate::{
  networking::{GameType, ServerAddress},
  state::{GameState, NextState}
};
use kubi_shared::data::{io_thread::IOThreadManager, open_local_save_file};

pub fn initialize_from_args(
  all_storages: AllStoragesView,
) {
  // If an address is provided, we're in multiplayer mode (the first argument is the address)
  // Otherwise, we're in singleplayer mode and working with local stuff
  let args: Vec<String> = env::args().collect();
  if cfg!(target_os = "android") || (args.get(1) == Some(&"android".into())) {
    // TODO REMOVE: temporarily bypass menu on Android as hUI (0.1.0-alpha.5) doesnt play well with touchscreens (yet? :3)
    // TODO REMOVE: disable save files on Android as they're stored in relative path rn
    all_storages.add_unique(GameType::Singleplayer);
    all_storages.borrow::<UniqueViewMut<NextState>>().unwrap().0 = Some(GameState::LoadingWorld);
  } else if args.get(1) == Some(&"play".into()) {
    // Open the local save file
    let save_file = open_local_save_file(Path::new("./world.kubi")).expect("failed to open save file");
    all_storages.add_unique(IOThreadManager::new(save_file));
    // Switch the state and kick off the world loading
    all_storages.add_unique(GameType::Singleplayer);
    all_storages.borrow::<UniqueViewMut<NextState>>().unwrap().0 = Some(GameState::LoadingWorld);
  } else if args.len() > 1 {
    // Parse the address and switch the state to connecting
    let address = args[1].parse::<SocketAddr>().expect("invalid address");
    all_storages.add_unique(GameType::Muliplayer);
    all_storages.add_unique(ServerAddress(address));
    all_storages.borrow::<UniqueViewMut<NextState>>().unwrap().0 = Some(GameState::Connecting);
  } else {
    all_storages.borrow::<UniqueViewMut<NextState>>().unwrap().0 = Some(GameState::MainMenu);
  }
}
