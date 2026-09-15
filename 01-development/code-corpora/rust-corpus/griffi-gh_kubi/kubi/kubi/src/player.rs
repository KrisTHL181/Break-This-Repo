use glam::Mat4;
use shipyard::{Component, AllStoragesViewMut, UniqueViewMut};
use kubi_shared::{
  entity::{Entity, Health},
  player::{Player, PLAYER_HEALTH, PlayerHolding},
  block::Block,
  networking::{
    client::{Username, Client, ClientIdMap},
    messages::ClientInitData
  }
};
use crate::{
  camera::Camera,
  client_physics::ClPhysicsActor,
  player_controller::PlayerController,
  transform::Transform,
  world::raycast::LookingAtBlock
};

#[derive(Component)]
pub struct MainPlayer;

pub fn spawn_player (
  mut storages: AllStoragesViewMut,
) {
  log::info!("spawning player");
  storages.add_entity(((
    Player,
    MainPlayer,
    Entity,
    Health::new(PLAYER_HEALTH),
    Transform::default(),
    Camera::default(),
    PlayerController::DEFAULT_FPS_CTL,
    LookingAtBlock::default(),
    PlayerHolding(Some(Block::Cobblestone)),
    Username("LocalPlayer".into()),
  ),(
    ClPhysicsActor::default(),
  )));
}

pub fn spawn_local_player_multiplayer (
  storages: &mut AllStoragesViewMut,
  init: ClientInitData
) {
  log::info!("spawning local multiplayer player");
  let entity_id = storages.add_entity(((
    Player,
    Client(init.client_id),
    MainPlayer,
    Entity,
    init.health,
    Transform(Mat4::from_rotation_translation(init.direction, init.position)),
    Camera::default(),
    PlayerController::DEFAULT_FPS_CTL,
    LookingAtBlock::default(),
    PlayerHolding::default(),
  ),(
    Username(init.username),
    ClPhysicsActor::default(),
  )));

  //Add ourself to the client id map
  let mut client_id_map = storages.borrow::<UniqueViewMut<ClientIdMap>>().unwrap();
  client_id_map.0.insert(init.client_id, entity_id);
}

pub fn spawn_remote_player_multiplayer(
  storages: &mut AllStoragesViewMut,
  init: ClientInitData
) {
  log::info!("spawning remote multiplayer player");

  //Spawn player locally
  let entity_id = storages.add_entity((
    Username(init.username),
    Client(init.client_id),
    Player,
    Entity,
    init.health,
    Transform(Mat4::from_rotation_translation(init.direction, init.position)),
    PlayerHolding::default(),
  ));

  //Add it to the client id map
  let mut client_id_map = storages.borrow::<UniqueViewMut<ClientIdMap>>().unwrap();
  client_id_map.0.insert(init.client_id, entity_id);
}
