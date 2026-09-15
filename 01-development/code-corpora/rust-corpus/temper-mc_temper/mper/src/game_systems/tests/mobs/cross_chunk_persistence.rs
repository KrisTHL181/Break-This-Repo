use background::cross_chunk_border;
use bevy_ecs::prelude::*;
use mobs::spawn::{handle_spawn_mob_bundle, load_mob_bundles, save_mob_bundles};
use physics::chunk_boundary;
use temper_components::entity_identity::Identity;
use temper_components::last_chunk_pos::LastChunkPos;
use temper_components::player::position::Position;
use temper_core::dimension::Dimension;
use temper_entities::entity_types::EntityTypeEnum;
use temper_entities::markers::entity_types::Fox;
use temper_entities::markers::{HasCollisions, HasGravity, HasWaterDrag};
use temper_entities::{FoxBundle, MobBundle, MobKind};
use temper_messages::load_chunk_entities::LoadChunkEntities;
use temper_messages::save_chunk_entities::SaveChunkEntities;
use temper_state::create_test_state;

fn emit_save_for(
    chunk: temper_core::pos::ChunkPos,
) -> impl FnMut(MessageWriter<SaveChunkEntities>) {
    move |mut writer: MessageWriter<SaveChunkEntities>| {
        writer.write(SaveChunkEntities(chunk));
    }
}

fn emit_load_for(
    chunk: temper_core::pos::ChunkPos,
) -> impl FnMut(MessageWriter<LoadChunkEntities>) {
    move |mut writer: MessageWriter<LoadChunkEntities>| {
        writer.write(LoadChunkEntities(chunk));
    }
}

#[test]
fn mob_crossing_a_chunk_border_reloads_from_its_new_chunk() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state.clone());

    let old_position = Position::new(15.5, 64.0, 8.0);
    let new_position = Position::new(16.5, 64.0, 8.0);
    let old_chunk = old_position.chunk();
    let new_chunk = new_position.chunk();
    let fox_bundle = FoxBundle::new(old_position);
    let expected_identity = fox_bundle.identity.clone();

    let fox_entity = world
        .spawn((
            fox_bundle,
            Fox,
            MobKind(EntityTypeEnum::Fox),
            HasGravity,
            HasCollisions,
            HasWaterDrag,
            LastChunkPos::new(old_chunk),
        ))
        .id();

    {
        let old_chunk_ref = state
            .0
            .world
            .get_or_generate_chunk(old_chunk, Dimension::Overworld)
            .expect("old chunk should be cached");
        old_chunk_ref.entities.clear();
        old_chunk_ref.mark_dirty();
    }

    let mut initial_save_schedule = Schedule::default();
    initial_save_schedule.add_systems((emit_save_for(old_chunk), save_mob_bundles).chain());
    initial_save_schedule.run(&mut world);

    {
        let mut position = world
            .get_mut::<Position>(fox_entity)
            .expect("fox should still be alive");
        *position = new_position;
    }

    let mut boundary_schedule = Schedule::default();
    boundary_schedule.add_systems(
        (
            chunk_boundary::handle,
            cross_chunk_border::cross_chunk_border,
        )
            .chain(),
    );
    boundary_schedule.run(&mut world);

    {
        let old_chunk_ref = state
            .0
            .world
            .get_chunk(old_chunk, Dimension::Overworld)
            .expect("old chunk should exist");
        assert!(
            !old_chunk_ref.entities.contains_key(&expected_identity.uuid),
            "fox should no longer be stored in the old chunk after crossing the border"
        );
    }
    {
        let new_chunk_ref = state
            .0
            .world
            .get_chunk(new_chunk, Dimension::Overworld)
            .expect("new chunk should exist");
        let stored = new_chunk_ref
            .entities
            .get(&expected_identity.uuid)
            .expect("fox should be stored in its new chunk after crossing the border");
        assert_eq!(stored.value().0, EntityTypeEnum::Fox);

        let stored_bundle = MobBundle::deserialize(stored.value().0, &stored.value().1)
            .expect("stored fox bundle should deserialize");
        assert_eq!(stored_bundle.position().coords, new_position.coords);
    }

    world.despawn(fox_entity);
    let mut load_schedule = Schedule::default();
    load_schedule.add_systems(
        (
            emit_load_for(new_chunk),
            load_mob_bundles,
            handle_spawn_mob_bundle,
        )
            .chain(),
    );
    load_schedule.run(&mut world);

    let mut fox_query = world.query::<(&Identity, &Position, &LastChunkPos, Has<Fox>)>();
    let loaded_foxes: Vec<_> = fox_query
        .iter(&world)
        .filter(|(_, _, _, is_fox)| *is_fox)
        .collect();
    assert_eq!(
        loaded_foxes.len(),
        1,
        "fox should load once from the new chunk"
    );

    let (identity, position, last_chunk, is_fox) = loaded_foxes[0];
    assert!(is_fox, "reloaded entity should still have the Fox marker");
    assert_eq!(identity.uuid, expected_identity.uuid);
    assert_eq!(position.coords, new_position.coords);
    assert_eq!(last_chunk.0, new_chunk);
}
