use bevy_ecs::message::MessageRegistry;
use bevy_ecs::prelude::*;
use physics::{collisions, velocity};
use temper_components::player::grounded::OnGround;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;
use temper_core::block_state_id::BlockStateId;
use temper_core::dimension::Dimension;
use temper_core::pos::{ChunkBlockPos, ChunkPos};
use temper_entities::PhysicalRegistry;
use temper_entities::bundles::PigBundle;
use temper_entities::markers::HasCollisions;
use temper_entities::markers::entity_types::Pig;
use temper_macros::block;
use temper_messages::entity_update::SendEntityUpdate;
use temper_state::create_test_state;

#[test]
fn falling_entity_lands_when_velocity_step_crosses_floor() {
    let mut world = World::new();
    let (state, _temp_dir) = create_test_state();

    {
        let mut chunk = state
            .0
            .world
            .get_or_generate_mut(ChunkPos::new(0, 0), Dimension::Overworld)
            .expect("Failed to create test chunk");
        chunk.set_block(ChunkBlockPos::new(0, 64, 0), block!("stone"));
    }

    world.insert_resource(state);
    world.insert_resource(PhysicalRegistry::new());
    MessageRegistry::register_message::<SendEntityUpdate>(&mut world);

    let mut bundle = PigBundle::new(Position::new(0.5, 65.2, 0.5));
    bundle.velocity = Velocity::new(0.0, -2.4, 0.0);

    let entity = world.spawn((bundle, Pig, HasCollisions)).id();

    let mut schedule = Schedule::default();
    schedule.add_systems((velocity::handle, collisions::handle).chain());
    schedule.run(&mut world);

    let pos = world.get::<Position>(entity).unwrap();
    let vel = world.get::<Velocity>(entity).unwrap();
    let grounded = world.get::<OnGround>(entity).unwrap();

    assert_eq!(pos.coords.y, 65.0);
    assert_eq!(vel.vec.y, 0.0);
    assert!(grounded.0);
}
