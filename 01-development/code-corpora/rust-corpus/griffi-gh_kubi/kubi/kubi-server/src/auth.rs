use glam::{Vec3, Mat4, vec3};
use shipyard::{UniqueView, NonSendSync, EntitiesViewMut, ViewMut, UniqueViewMut, AllStoragesView, IntoIter};
use uflow::{server::Event as ServerEvent, SendMode};
use kubi_shared::{
  networking::{
    messages::{
      ClientToServerMessage,
      ServerToClientMessage,
      InitData,
      ClientInitData,
      ClientToServerMessageType,
    },
    client::{Client, ClientId, Username},
    channels::Channel,
  }, 
  player::{Player, PLAYER_HEALTH}, 
  transform::Transform, entity::{Entity, Health}
};
use crate::{
  config::ConfigTable, 
  server::{ServerEvents, UdpServer, IsMessageOfType}, 
  client::{ClientAddress, ClientAddressMap}
};
pub use kubi_shared::networking::client::ClientIdMap;

pub fn authenticate_players(
  storages: AllStoragesView,
) {
  let mut client_entity_map = storages.borrow::<UniqueViewMut<ClientIdMap>>().unwrap();
  let mut client_addr_map = storages.borrow::<UniqueViewMut<ClientAddressMap>>().unwrap();
  let server = storages.borrow::<NonSendSync<UniqueView<UdpServer>>>().unwrap();
  let events = storages.borrow::<UniqueView<ServerEvents>>().unwrap();
  let config = storages.borrow::<UniqueView<ConfigTable>>().unwrap();
  
  for event in &events.0 {
    // NOT using `check_message_auth` here because the user is not authed yet!
    let ServerEvent::Receive(client_addr, data) = event else{
      continue
    };
    if !event.is_message_of_type::<{ClientToServerMessageType::ClientHello as u8}>() {
      continue
    }
    let Some(client) = server.0.client(client_addr) else {
      log::error!("Client doesn't exist");
      continue
    };
    let Ok(parsed_message) = postcard::from_bytes(data) else {
      log::error!("Malformed message");
      continue
    };
    let ClientToServerMessage::ClientHello { username, password } = parsed_message else {
      unreachable!()
    };

    log::info!("ClientHello; username={} password={:?}", username, password);

    // Handle password auth
    if let Some(server_password) = &config.server.password {
      if let Some(user_password) = &password {
        if server_password != user_password {
          client.borrow_mut().send(
            postcard::to_allocvec(&ServerToClientMessage::ServerFuckOff {
              reason: "Incorrect password".into()
            }).unwrap().into_boxed_slice(), 
            Channel::Auth as usize,
            SendMode::Reliable
          );
          continue
        }
      } else {
        client.borrow_mut().send(
          postcard::to_allocvec(&ServerToClientMessage::ServerFuckOff {
            reason: "This server is password protected".into()
          }).unwrap().into_boxed_slice(), 
          Channel::Auth as usize,
          SendMode::Reliable
        );
        continue
      }
    }

    //Find the player ID
    let max_clients = config.server.max_clients as ClientId;
    let Some(client_id) = (0..max_clients).find(|id| {
      !client_entity_map.0.contains_key(id) 
    }) else {
      client.borrow_mut().send(
        postcard::to_allocvec(&ServerToClientMessage::ServerFuckOff {
          reason: "Can't find a free spot for you!".into()
        }).unwrap().into_boxed_slice(), 
        Channel::Auth as usize,
        SendMode::Reliable
      );
      continue
    };

    //Spawn the user
    let entity_id = {
      storages.borrow::<EntitiesViewMut>().unwrap().add_entity((
        &mut storages.borrow::<ViewMut<Entity>>().unwrap(),
        &mut storages.borrow::<ViewMut<Player>>().unwrap(),
        &mut storages.borrow::<ViewMut<Health>>().unwrap(),
        &mut storages.borrow::<ViewMut<Client>>().unwrap(),
        &mut storages.borrow::<ViewMut<ClientAddress>>().unwrap(),
        &mut storages.borrow::<ViewMut<Transform>>().unwrap(),
        &mut storages.borrow::<ViewMut<Username>>().unwrap(),
      ), (
        Entity,
        Player,
        Health::new(PLAYER_HEALTH),
        Client(client_id),
        ClientAddress(*client_addr),
        Transform(Mat4::from_translation(vec3(0., 60., 0.))),
        Username(username.clone()),
      ))
    };

    //Add the user to the ClientIdMap and ClientAddressMap
    client_entity_map.0.insert(client_id, entity_id);
    client_addr_map.0.insert(*client_addr, entity_id);

    //Create init data
    let init_data = {
      let mut user = None;
      let mut users = Vec::with_capacity(client_entity_map.0.len() - 1);
      for (client, username, transform, &health) in (
        &storages.borrow::<ViewMut<Client>>().unwrap(),
        &storages.borrow::<ViewMut<Username>>().unwrap(),
        &storages.borrow::<ViewMut<Transform>>().unwrap(),
        &storages.borrow::<ViewMut<Health>>().unwrap(),
      ).iter() {
        let (_, direction, position) = transform.0.to_scale_rotation_translation();
        let idata = ClientInitData {
          client_id: client.0,
          username: username.0.clone(),
          position,
          velocity: Vec3::ZERO,
          direction,
          health,
        };
        if client_id == client.0 {
          user = Some(idata);
        } else {
          users.push(idata);
        }
      }
      InitData {
        user: user.unwrap(),
        users
      }
    };

    //Announce new player to other clients
    {
      let message = &ServerToClientMessage::PlayerConnected {
        init: init_data.user.clone()
      };
      for (other_client_addr, _) in client_addr_map.0.iter() {
        //TODO: ONLY JOINED CLIENTS HERE! USE URL AS REFERENCE
        // https://github.com/griffi-gh/kubi/blob/96a6693faa14580fca560f4a64f0e88e595a8ca0/kubi-server/src/world.rs#L144
        let Some(other_client) = server.0.client(other_client_addr) else {
          log::error!("Other client doesn't exist");
          continue
        };
        other_client.borrow_mut().send(
          postcard::to_allocvec(&message).unwrap().into_boxed_slice(),
          Channel::SysEvt as usize,
          SendMode::Reliable
        );
      }
    }

    //Approve the user and send init data
    client.borrow_mut().send(
      postcard::to_allocvec(&ServerToClientMessage::ServerHello {
        init: init_data
      }).unwrap().into_boxed_slice(), 
      Channel::Auth as usize,
      SendMode::Reliable
    );

    log::info!("{username}({client_id}) joined the game!")
  }
}
