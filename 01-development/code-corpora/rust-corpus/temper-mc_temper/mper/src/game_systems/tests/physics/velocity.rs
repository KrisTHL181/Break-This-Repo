use bevy_ecs::message::MessageRegistry;
use bevy_ecs::prelude::*;
use bevy_math::Vec3A;
use physics::velocity::handle;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;
use temper_messages::entity_update::SendEntityUpdate;

#[test]
fn velocity_updates_position() {
    let mut world = World::new();
    let entity = world
        .spawn((
            Velocity {
                vec: Vec3A::new(1.0, 2.0, 3.0),
            },
            Position {
                coords: Vec3A::ZERO.as_dvec3(),
            },
        ))
        .id();
    MessageRegistry::register_message::<SendEntityUpdate>(&mut world);

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    let pos = world.get::<Position>(entity).unwrap();
    assert_eq!(pos.coords, Vec3A::new(1.0, 2.0, 3.0).as_dvec3());
}

#[test]
fn velocity_no_update_when_unchanged() {
    let mut world = World::new();
    let entity = world
        .spawn((
            Velocity { vec: Vec3A::ZERO },
            Position {
                coords: Vec3A::ZERO.as_dvec3(),
            },
        ))
        .id();

    MessageRegistry::register_message::<SendEntityUpdate>(&mut world);

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    assert!(world.get::<Position>(entity).is_some());
    assert_eq!(
        world.get::<Position>(entity).unwrap().coords,
        Vec3A::ZERO.as_dvec3()
    );

    let reader = world.get_resource::<Messages<SendEntityUpdate>>().unwrap();
    let mut cursor = reader.get_cursor();
    let mut messages = vec![];
    for msg in cursor.read(reader) {
        messages.push(msg);
    }
    assert_eq!(messages.len(), 0);
}

#[test]
fn velocity_multiple_steps() {
    let mut world = World::new();
    let entity = world
        .spawn((
            Velocity {
                vec: Vec3A::new(0.5, 0.0, 0.0),
            },
            Position {
                coords: Vec3A::ZERO.as_dvec3(),
            },
        ))
        .id();

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    for _ in 0..4 {
        schedule.run(&mut world);
    }

    let pos = world.get::<Position>(entity).unwrap();
    assert_eq!(pos.coords, Vec3A::new(2.0, 0.0, 0.0).as_dvec3());
}

#[test]
fn velocity_multiple_entities() {
    let mut world = World::new();
    let entity1 = world
        .spawn((
            Velocity {
                vec: Vec3A::new(1.0, 0.0, 0.0),
            },
            Position {
                coords: Vec3A::ZERO.as_dvec3(),
            },
        ))
        .id();
    let entity2 = world
        .spawn((
            Velocity {
                vec: Vec3A::new(0.0, 1.0, 0.0),
            },
            Position {
                coords: Vec3A::ZERO.as_dvec3(),
            },
        ))
        .id();

    let mut schedule = Schedule::default();
    schedule.add_systems(handle);

    schedule.run(&mut world);

    let pos1 = world.get::<Position>(entity1).unwrap();
    let pos2 = world.get::<Position>(entity2).unwrap();
    assert_eq!(pos1.coords, Vec3A::new(1.0, 0.0, 0.0).as_dvec3());
    assert_eq!(pos2.coords, Vec3A::new(0.0, 1.0, 0.0).as_dvec3());
}
