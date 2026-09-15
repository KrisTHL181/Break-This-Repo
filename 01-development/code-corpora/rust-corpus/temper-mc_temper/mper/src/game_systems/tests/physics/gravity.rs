use bevy_ecs::prelude::*;
use bevy_math::{DVec3, Vec3A};
use physics::gravity::handle;
use temper_components::player::grounded::OnGround;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;
use temper_core::block_state_id::BlockStateId;
use temper_core::dimension::Dimension;
use temper_core::pos::ChunkPos;
use temper_entities::markers::{HasGravity, HasWaterDrag};
use temper_macros::block;
use temper_state::{GlobalStateResource, create_test_state};

fn create_chunk_with_water(state: &GlobalStateResource, chunk_pos: ChunkPos) {
    let mut chunk = state
        .0
        .world
        .get_or_generate_mut(chunk_pos, Dimension::Overworld)
        .expect("Failed to load or generate chunk");

    chunk.fill(block!("water", { level: 0 }));
}

#[test]
fn gravity_application() {
    let mut world = World::new();
    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let entity = world
        .spawn((
            Velocity { vec: Vec3A::ZERO },
            OnGround(false),
            Position {
                coords: DVec3::new(0.0, 100.0, 0.0),
            },
            HasGravity,
        ))
        .id();

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    let vel = world.get::<Velocity>(entity).unwrap();
    assert!(vel.vec.y < 0.0);
}

#[test]
fn gravity_no_gravity_when_grounded() {
    let mut world = World::new();
    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let entity = world
        .spawn((
            Velocity { vec: Vec3A::ZERO },
            OnGround(true),
            Position {
                coords: DVec3::new(0.0, 100.0, 0.0),
            },
            HasGravity,
        ))
        .id();

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    let vel = world.get::<Velocity>(entity).unwrap();
    assert_eq!(vel.vec.y, 0.0);
}

#[test]
fn gravity_water_entity_not_in_water() {
    let mut world = World::new();
    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let entity = world
        .spawn((
            Velocity { vec: Vec3A::ZERO },
            OnGround(false),
            Position {
                coords: DVec3::new(0.0, 100.0, 0.0),
            },
            HasGravity,
            HasWaterDrag,
        ))
        .id();

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    let vel = world.get::<Velocity>(entity).unwrap();
    assert!(vel.vec.y < 0.0);
}

#[test]
fn gravity_water_entity_no_gravity_when_grounded() {
    let mut world = World::new();
    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let entity = world
        .spawn((
            Velocity { vec: Vec3A::ZERO },
            OnGround(true),
            Position {
                coords: DVec3::new(0.0, 100.0, 0.0),
            },
            HasGravity,
            HasWaterDrag,
        ))
        .id();

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    let vel = world.get::<Velocity>(entity).unwrap();
    assert_eq!(vel.vec.y, 0.0);
}

#[test]
fn gravity_water_entity_in_water_no_gravity() {
    let mut world = World::new();
    let (state, _temp_dir) = create_test_state();

    let chunk_pos = ChunkPos::new(0, 0);
    create_chunk_with_water(&state, chunk_pos);

    world.insert_resource(state);

    let entity = world
        .spawn((
            Velocity { vec: Vec3A::ZERO },
            OnGround(false),
            Position {
                coords: DVec3::new(0.0, 65.0, 0.0),
            },
            HasGravity,
            HasWaterDrag,
        ))
        .id();

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    let vel = world.get::<Velocity>(entity).unwrap();
    assert_eq!(vel.vec.y, 0.0);
}
