use std::sync::Arc;
use atomic::{Atomic, Ordering};
use glam::{IVec3, ivec3};
use kubi_shared::{
  data::io_thread::{IOCommand, IOResponse, IOThreadManager},
  networking::{channels::Channel, messages::ClientToServerMessage},
  worldgen::AbortState,
};
use shipyard::{View, UniqueView, UniqueViewMut, IntoIter, Workload, IntoWorkload, NonSendSync, track};
use uflow::SendMode;
use wgpu::util::DeviceExt;
use crate::{
  networking::UdpClient,
  player::MainPlayer,
  rendering::{BufferPair, Renderer},
  settings::GameSettings,
  state::GameState,
  transform::Transform,
};
use super::{
  ChunkStorage, ChunkMeshStorage,
  chunk::{Chunk, DesiredChunkState, CHUNK_SIZE, ChunkMesh, CurrentChunkState, ChunkData},
  tasks::{ChunkTaskManager, ChunkTaskResponse, ChunkTask},
  queue::BlockUpdateQueue,
};

const WORLD_SEED: u64 = 0xfeb_face_dead_cafe;

const MAX_CHUNK_OPS_INGAME: usize = 8;
const MAX_CHUNK_OPS: usize = 32;

pub fn update_loaded_world_around_player() -> Workload {
  (
    update_chunks_if_player_moved,
    process_state_changes,
    process_completed_tasks,
  ).into_sequential_workload()
}

pub fn update_chunks_if_player_moved(
  v_settings: UniqueView<GameSettings>,
  v_local_player: View<MainPlayer>,
  v_transform: View<Transform, track::All>,
  mut vm_world: UniqueViewMut<ChunkStorage>,
) {
  //Check if the player actually moved 
  //TODO fix this also triggers on rotation, only activate when the player crosses the chunk border
  let Some((_, transform)) = (&v_local_player, v_transform.inserted_or_modified()).iter().next() else {
    return
  };

  //Read game settings
  let load_distance = (v_settings.render_distance + 1) as i32;

  //If it did, get it's position and current chunk
  let player_position = transform.0.to_scale_rotation_translation().2;
  let player_position_ivec3 = player_position.as_ivec3();
  let player_at_chunk = ivec3(
    player_position_ivec3.x.div_euclid(CHUNK_SIZE as i32),
    player_position_ivec3.y.div_euclid(CHUNK_SIZE as i32),
    player_position_ivec3.z.div_euclid(CHUNK_SIZE as i32),
  );

  //Then, mark *ALL* chunks with ToUnload
  for (_, chunk) in &mut vm_world.chunks {
    chunk.desired_state = DesiredChunkState::Unloaded;
  }

  //Then mark chunks that are near to the player
  for x in -load_distance..=load_distance {
    for y in -load_distance..=load_distance {
      for z in -load_distance..=load_distance {
        let chunk_pos_offset = ivec3(x, y, z);
        let chunk_pos = player_at_chunk + chunk_pos_offset;
        let is_border = {
          chunk_pos_offset.x.abs() == load_distance ||
          chunk_pos_offset.y.abs() == load_distance ||
          chunk_pos_offset.z.abs() == load_distance
        };
        //If chunk doesn't exist create it
        let chunk = match vm_world.chunks.get_mut(&chunk_pos) {
          Some(chunk) => chunk,
          None => {
            let chunk = Chunk::new(chunk_pos);
            unsafe {
              vm_world.chunks.insert_unique_unchecked(chunk_pos, chunk);
            }
            vm_world.chunks.get_mut(&chunk_pos).unwrap()
          }
        };
        let desired = match is_border {
          true  => DesiredChunkState::Loaded,
          false => DesiredChunkState::Rendered,
        };
        chunk.desired_state = desired;
      }
    }
  }
}

fn process_state_changes(
  task_manager: UniqueView<ChunkTaskManager>,
  io: Option<UniqueView<IOThreadManager>>,
  mut udp_client: Option<UniqueViewMut<UdpClient>>,
  mut world: UniqueViewMut<ChunkStorage>,
  mut vm_meshes: NonSendSync<UniqueViewMut<ChunkMeshStorage>>,
) {
  if !world.is_modified() {
    return
  }

  //HACK: cant iterate over chunks.keys() or chunk directly!
  let hashmap_keys: Vec<IVec3> = world.chunks.keys().copied().collect();
  for position in hashmap_keys {
    let chunk = world.chunks.get_mut(&position).unwrap();

    //If the chunk is being unloaded, it's essentially dead at this point and we shouldn't bother it
    if chunk.current_state == CurrentChunkState::Unloading {
      continue
    }
    // If the chunk is already in the desired state, skip it
    // (except one annoying edge case where chunk is rendered but dirty, then we need to recalculate the mesh)
    if chunk.desired_state.matches_current(chunk.current_state) &&
      !(chunk.desired_state == DesiredChunkState::Rendered && chunk.mesh_dirty) {
      continue
    }
    match chunk.desired_state {
      // DesiredChunkState::Unloaded | DesiredChunkState::Nothing:
      // Loading -> Nothing
      DesiredChunkState::Unloaded | DesiredChunkState::Nothing if chunk.current_state == CurrentChunkState::Loading => {
        if let Some(abortion) = &chunk.abortion {
          let _ = abortion.compare_exchange(
            AbortState::Continue, AbortState::Abort,
            Ordering::Relaxed, Ordering::Relaxed
          );
        }
        chunk.abortion = None;
        chunk.current_state = CurrentChunkState::Nothing;
      },

      // DesiredChunkState::Unloaded | DesiredChunkState::Nothing:
      // (Loaded, CalculatingMesh) -> Nothing
      DesiredChunkState::Unloaded | DesiredChunkState::Nothing if matches!(
        chunk.current_state,
        CurrentChunkState::Loaded | CurrentChunkState::CalculatingMesh,
      ) => {
        // chunk.block_data = None; //HACK when downgrading, keep the data so we can save it
        chunk.current_state = CurrentChunkState::Nothing;
      },

      // DesiredChunkState::Unloaded | DesiredChunkState::Nothing:
      // (Rendered | RecalculatingMesh) -> Nothing
      DesiredChunkState::Unloaded | DesiredChunkState::Nothing if matches!(
        chunk.current_state,
        CurrentChunkState::Rendered | CurrentChunkState::RecalculatingMesh,
      ) => {
        if let Some(mesh_index) = chunk.mesh_index {
          vm_meshes.remove(mesh_index).unwrap();
        }
        chunk.mesh_index = None;
        chunk.current_state = CurrentChunkState::Nothing;
      },

      // DesiredChunkState::Loaded:
      // CalculatingMesh -> Loaded
      DesiredChunkState::Loaded if chunk.current_state == CurrentChunkState::CalculatingMesh => {
        chunk.current_state = CurrentChunkState::Loaded;
      },

      // DesiredChunkState::Unloaded | DesiredChunkState::Nothing | DesiredChunkState::Loaded:
      // (Rendered | RecalculatingMesh) -> Loaded
      DesiredChunkState::Unloaded | DesiredChunkState::Nothing | DesiredChunkState::Loaded if matches!(
        chunk.current_state, CurrentChunkState::Rendered | CurrentChunkState::RecalculatingMesh
      ) => {
        if let Some(mesh_index) = chunk.mesh_index {
          vm_meshes.remove(mesh_index).unwrap();
        }
        chunk.mesh_index = None;
        chunk.current_state = CurrentChunkState::Loaded;
      },

      // DesiredChunkState::Loaded | DesiredChunkState::Rendered:
      // Nothing -> Loading
      DesiredChunkState::Loaded | DesiredChunkState::Rendered if chunk.current_state == CurrentChunkState::Nothing => {
        let mut abortion = None;
        //start load task
        if let Some(client) = &mut udp_client {
          client.0.send(
            postcard::to_allocvec(&ClientToServerMessage::ChunkSubRequest {
              chunk: position,
            }).unwrap().into_boxed_slice(),
            Channel::SubReq as usize,
            SendMode::Reliable
          );
        } else {

          // If the chunk exists in the save file (and save file is there in the first place),
          // ... we'll try to load it
          // Otherwise, we'll run worldgen

          let mut should_run_worldgen = true;

          if let Some(io) = &io {
            if io.chunk_exists(position) {
              // Try to load the chunk from the save file
              // In case that fails, we will run worldgen once the IO thread responds
              io.send(IOCommand::LoadChunk { position });
              should_run_worldgen = false;
            }
          }

          if should_run_worldgen {
            let atomic = Arc::new(Atomic::new(AbortState::Continue));
            task_manager.spawn_task(ChunkTask::ChunkWorldgen {
              seed: WORLD_SEED,
              position,
              abortion: Some(Arc::clone(&atomic)),
            });
            abortion = Some(atomic);
          }
        }

        //Update chunk state
        let chunk = world.chunks.get_mut(&position).unwrap();
        chunk.current_state = CurrentChunkState::Loading;
        chunk.abortion = abortion;

        // ===========
        //log::trace!("Started loading chunk {position}");
      },

      // DesiredChunkState::Rendered:
      // Loaded -> CalculatingMesh
      // Rendered (dirty) -> RecalculatingMesh
      DesiredChunkState::Rendered if (chunk.current_state == CurrentChunkState::Loaded || chunk.mesh_dirty) => {
        //get needed data
        let Some(neighbors) = world.neighbors_all(position) else {
          continue
        };
        let Some(data) = neighbors.mesh_data() else {
          continue
        };
        //spawn task
        task_manager.spawn_task(ChunkTask::GenerateMesh { data, position });
        //Update chunk state
        let chunk = world.chunks.get_mut(&position).unwrap();
        if chunk.mesh_dirty {
          chunk.current_state = CurrentChunkState::RecalculatingMesh;
        } else {
          chunk.current_state = CurrentChunkState::CalculatingMesh;
        }
        chunk.mesh_dirty = false;
        chunk.abortion = None; //Can never abort at this point
        // ===========
        //log::trace!("Started generating mesh for chunk {position}");
      }

      _ => {}, //panic!("Illegal state transition: {:?} -> {:?}", chunk.current_state, chunk.desired_state),
    }
  }

  //Now, separately process state change the state from Nothing to Unloading or Unloaded
  world.chunks.retain(|&position, chunk: &mut Chunk| {
    if chunk.desired_state == DesiredChunkState::Unloaded {
      assert!(chunk.current_state == CurrentChunkState::Nothing, "looks like chunk did not get properly downgraded to Nothing before unloading, this is a bug");

      chunk.current_state = CurrentChunkState::Unloading;

      //If in multiplayer, send a message to the server to unsubscribe from the chunk
      if let Some(client) = &mut udp_client {
        client.0.send(
          postcard::to_allocvec(
            &ClientToServerMessage::ChunkUnsubscribe { chunk: position }
          ).unwrap().into_boxed_slice(),
          Channel::SubReq as usize,
          SendMode::Reliable
        );
        // and i think that's it, just kill the chunk right away, the server will take care of the rest
        //
        // because uflow's reliable packets are ordered, there should be no need to wait for the server to confirm the unsubscription
        // because client won't be able to subscribe to it again until the server finishes processing the unsubscription
        // :ferrisClueless:
        return false
      }

      // If in singleplayer and have an open save file, we need to save the chunk to the disk

      // ==========================================================
      //TODO IMPORTANT: WAIT FOR CHUNK TO FINISH SAVING FIRST BEFORE TRANSITIONING TO UNLOADED
      // OTHERWISE WE WILL LOSE THE SAVE DATA IF THE USER COMES BACK TO THE CHUNK TOO QUICKLY
      // ==========================================================
      //XXX: CHECK IF WE REALLY NEED THIS OR IF WE CAN JUST KILL THE CHUNK RIGHT AWAY
      //CHANGES TO CHUNK SAVING LOGIC SHOULD HAVE MADE THE ABOVE COMMENT OBSOLETE

      if let Some(io) = &io {
        if let Some(block_data) = &chunk.block_data {
          // Only save the chunk if it has been modified
          if chunk.data_modified {
            // log::debug!("issue save command");
            chunk.data_modified = false;
            io.send(IOCommand::SaveChunk {
              position,
              data: block_data.blocks.clone(),
            });
          }
        }
      }

      return false
    }
    true
  });

}

fn process_completed_tasks(
  task_manager: UniqueView<ChunkTaskManager>,
  io: Option<UniqueView<IOThreadManager>>,
  mut world: UniqueViewMut<ChunkStorage>,
  mut meshes: NonSendSync<UniqueViewMut<ChunkMeshStorage>>,
  renderer: UniqueView<Renderer>,
  state: UniqueView<GameState>,
  mut queue: UniqueViewMut<BlockUpdateQueue>,
) {
  let mut ops: usize = 0;

  //TODO reduce code duplication between loaded/generated chunks

  // Process IO first
  if let Some(io) = &io {
    for response in io.poll() {
      let IOResponse::ChunkLoaded { position, data } = response else {
        //TODO this is bad
        panic!("Unexpected IO response: {:?}", response);
      };

      //check if chunk exists
      let Some(chunk) = world.chunks.get_mut(&position) else {
        log::warn!("LOADED blocks data discarded: chunk doesn't exist");
        continue
      };

      //we cannot have abortion here but just in case, reset it
      chunk.abortion = None;

      //check if chunk still wants it
      if !matches!(chunk.desired_state, DesiredChunkState::Loaded | DesiredChunkState::Rendered) {
        log::warn!("LOADED block data discarded: state undesirable: {:?}", chunk.desired_state);
        continue
      }

      // check if we actually got the data
      if let Some(data) = data {
        // If we did get the data, yay :3
        chunk.block_data = Some(ChunkData {
          blocks: data
        });
        chunk.current_state = CurrentChunkState::Loaded;
      } else {
        // If we didn't get the data, we need to run worldgen
        // XXX: will this ever happen? we should always have the data in the save file
        let atomic = Arc::new(Atomic::new(AbortState::Continue));
        task_manager.spawn_task(ChunkTask::ChunkWorldgen {
          seed: WORLD_SEED,
          position,
          abortion: Some(Arc::clone(&atomic)),
        });
        chunk.abortion = Some(atomic);
      }

      ops += 1;
    }

    //return early if we've reached the limit
    if ops >= match *state {
      GameState::InGame => MAX_CHUNK_OPS_INGAME,
      _ => MAX_CHUNK_OPS,
    } { return }
    // XXX: this will completely skip polling the task manager if we've reached the limit
    //      this is probably fine, but it might be worth revisiting later
  }

  for res in task_manager.poll() {
    match res {
      ChunkTaskResponse::ChunkWorldgenDone { position, chunk_data, mut queued } => {
        //TODO this can fuck shit up really badly if io op gets overwritten by worldgen chunk
        //TODO only accept if loading stage, not loaded

        //If unwanted chunk is already loaded
        //It would be ~~...unethical~~ impossible to abort the operation at this point
        //Instead, we'll just throw it away

        //check if chunk exists
        let Some(chunk) = world.chunks.get_mut(&position) else {
          //to compensate, actually push the ops counter back by one
          log::warn!("blocks data discarded: chunk doesn't exist");
          continue
        };

        chunk.abortion = None;

        //check if chunk still wants it
        if !matches!(chunk.desired_state, DesiredChunkState::Loaded | DesiredChunkState::Rendered) {
          log::warn!("block data discarded: state undesirable: {:?}", chunk.desired_state);
          continue
        }

        //set the block data
        chunk.block_data = Some(ChunkData {
          blocks: chunk_data
        });

        //update chunk state
        chunk.current_state = CurrentChunkState::Loaded;

        //push queued blocks
        queue.0.append(&mut queued);
        drop(queued); //`queued` is empty after `append`

        //increase ops counter
        ops += 1;
      },
      ChunkTaskResponse::GenerateMeshDone {
        position,
        vertices, indices,
        trans_vertices, trans_indices,
      } => {
        //check if chunk exists
        let Some(chunk) = world.chunks.get_mut(&position) else {
          log::warn!("mesh discarded: chunk doesn't exist");
          continue
        };

        //check if chunk still wants it
        if chunk.desired_state != DesiredChunkState::Rendered {
          log::warn!("mesh discarded: state undesirable: {:?}", chunk.desired_state);
          continue
        }

        //apply the mesh
        //TODO: Skip if mesh is empty? (i.e. set to None)
        //TODO

        let vtx_buffer = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
          label: Some("chunk_vertex_buffer"),
          contents: bytemuck::cast_slice(&vertices),
          usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::VERTEX,
        });

        let idx_buffer = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
          label: Some("chunk_vertex_buffer"),
          contents: bytemuck::cast_slice(&indices),
          usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::INDEX,
        });

        let vtx_buffer_trans = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
          label: Some("chunk_trans_vertex_buffer"),
          contents: bytemuck::cast_slice(&trans_vertices),
          usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::VERTEX,
        });

        let idx_buffer_trans = renderer.device().create_buffer_init(&wgpu::util::BufferInitDescriptor {
          label: Some("chunk_trans_index_buffer"),
          contents: bytemuck::cast_slice(&trans_indices),
          usage: wgpu::BufferUsages::COPY_DST | wgpu::BufferUsages::INDEX,
        });

        let main_buffer_pair = BufferPair {
          vertex: vtx_buffer,
          vertex_len: vertices.len() as u32,
          index: idx_buffer,
          index_len: indices.len() as u32,
        };

        let trans_buffer_pair = BufferPair {
          vertex: vtx_buffer_trans,
          vertex_len: trans_vertices.len() as u32,
          index: idx_buffer_trans,
          index_len: trans_indices.len() as u32,
        };

        let mesh = ChunkMesh {
          main: main_buffer_pair,
          trans: trans_buffer_pair,
        };

        if let Some(index) = chunk.mesh_index {
          meshes.update(index, mesh).expect("Mesh update failed");
        } else {
          let mesh_index = meshes.insert(mesh);
          chunk.mesh_index = Some(mesh_index);
        }

        //update chunk state
        chunk.current_state = CurrentChunkState::Rendered;

        //increase ops counter
        ops += 1;
      }
    }
    if ops >= match *state {
      GameState::InGame => MAX_CHUNK_OPS_INGAME,
      _ => MAX_CHUNK_OPS,
    } { break }
  }
}

/// Save all modified chunks to the disk
pub fn save_on_exit(
  io: Option<UniqueView<IOThreadManager>>,
  world: UniqueView<ChunkStorage>,
) {
  let Some(io) = io else {
    log::warn!("no IO thread manager, skipping save on exit");
    return
  };
  for (&position, chunk) in &world.chunks {
    if let Some(block_data) = &chunk.block_data {
      if chunk.data_modified {
        io.send(IOCommand::SaveChunk {
          position,
          data: block_data.blocks.clone(),
        });
      }
    }
  }
}
