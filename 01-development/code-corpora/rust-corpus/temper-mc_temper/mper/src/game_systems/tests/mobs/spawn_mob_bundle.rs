use bevy_ecs::prelude::*;
use mobs::pig::tick_pig;
use mobs::spawn::{handle_despawn_mob, handle_spawn_mob_bundle};
use pathfinding::{Pathfinder, PathfinderSearch, pos_to_block};
use std::collections::HashMap;
use temper_components::bossbar::BossbarOwner;
use temper_components::entity_identity::Identity;
use temper_components::last_chunk_pos::LastChunkPos;
use temper_components::mob_ai::PigAI;
use temper_components::player::grounded::OnGround;
use temper_components::player::player_marker::PlayerMarker;
use temper_components::player::position::Position;
use temper_components::player::velocity::Velocity;
use temper_core::dimension::Dimension;
use temper_entities::MobBundle;
use temper_entities::entity_types::EntityTypeEnum;
use temper_entities::markers::entity_types::{Axolotl, Bat, Cow, Fox, Pig, Warden};
use temper_entities::markers::{HasCollisions, HasGravity, HasWaterDrag};
use temper_entities::mob_definition::MobProfile;
use temper_entities::{
    AxolotlBundle, BatBundle, CowBundle, FoxBundle, MobKind, PigBundle, WardenBundle,
};
use temper_messages::{DespawnMob, SpawnMobBundle};
use temper_state::{GlobalStateResource, create_test_state};

#[derive(Clone)]
struct SpawnedMob {
    identity: Identity,
    position: Position,
    last_chunk: LastChunkPos,
    kind: MobKind,
    gravity: bool,
    collisions: bool,
    water_drag: bool,
}

fn emit_spawn_bundles(mut writer: MessageWriter<SpawnMobBundle>) {
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Pig(PigBundle::new(Position::new(5.5, 64.0, 7.5))),
        persist: true,
    });
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Fox(FoxBundle::new(Position::new(8.5, 64.0, 9.5))),
        persist: true,
    });
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Cow(CowBundle::new(Position::new(11.5, 64.0, 9.5))),
        persist: true,
    });
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Bat(BatBundle::new(Position::new(14.5, 70.0, 9.5))),
        persist: true,
    });
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Axolotl(AxolotlBundle::new(Position::new(17.5, 62.0, 9.5))),
        persist: true,
    });
}

fn emit_unpersisted_cow(mut writer: MessageWriter<SpawnMobBundle>) {
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Cow(CowBundle::new(Position::new(21.5, 64.0, 9.5))),
        persist: false,
    });
}

fn emit_persisted_cow(mut writer: MessageWriter<SpawnMobBundle>) {
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Cow(CowBundle::new(Position::new(22.5, 64.0, 9.5))),
        persist: true,
    });
}

fn emit_pig(mut writer: MessageWriter<SpawnMobBundle>) {
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Pig(PigBundle::new(Position::new(24.5, 64.0, 9.5))),
        persist: false,
    });
}

fn emit_warden(mut writer: MessageWriter<SpawnMobBundle>) {
    writer.write(SpawnMobBundle {
        bundle: MobBundle::Warden(WardenBundle::new(Position::new(27.5, 64.0, 9.5))),
        persist: false,
    });
}

fn emit_all_mob_bundles(mut writer: MessageWriter<SpawnMobBundle>) {
    for (index, kind) in MobBundle::all_kinds().iter().copied().enumerate() {
        let position = Position::new(index as f64 + 0.5, 80.0, 32.5);
        let bundle = MobBundle::new(kind, position);
        let data = bundle.serialize_for_chunk();
        let decoded = MobBundle::deserialize(kind, &data).expect("bundle should deserialize");

        assert_eq!(decoded.kind(), kind);
        assert_eq!(decoded.position().xyz(), position.xyz());

        writer.write(SpawnMobBundle {
            bundle,
            persist: false,
        });
    }
}

fn assert_registry_round_trip(
    bundle: MobBundle,
    kind: EntityTypeEnum,
    profile: MobProfile,
    position: Position,
) {
    assert_eq!(bundle.kind(), kind);
    assert_eq!(bundle.profile(), profile);
    assert_eq!(bundle.position().xyz(), position.xyz());

    let data = bundle.serialize_for_chunk();
    let decoded = MobBundle::deserialize(kind, &data).expect("bundle should deserialize");

    assert_eq!(decoded.kind(), kind);
    assert_eq!(decoded.profile(), profile);
    assert_eq!(decoded.position().xyz(), position.xyz());
}

fn spawned_markers<M: Component>(world: &mut World) -> Vec<SpawnedMob> {
    let mut query = world.query::<(
        &Identity,
        &Position,
        &LastChunkPos,
        &MobKind,
        Has<M>,
        Has<HasGravity>,
        Has<HasCollisions>,
        Has<HasWaterDrag>,
    )>();

    query
        .iter(world)
        .filter(|(_, _, _, _, is_marker, _, _, _)| *is_marker)
        .map(
            |(identity, position, last_chunk, kind, _, gravity, collisions, water_drag)| {
                SpawnedMob {
                    identity: identity.clone(),
                    position: *position,
                    last_chunk: *last_chunk,
                    kind: *kind,
                    gravity,
                    collisions,
                    water_drag,
                }
            },
        )
        .collect()
}

fn lone_marker<M: Component>(world: &mut World, label: &str) -> SpawnedMob {
    let matches = spawned_markers::<M>(world);
    assert_eq!(matches.len(), 1, "expected one {label}");
    matches[0].clone()
}

fn assert_spawn_profile(
    mob: &SpawnedMob,
    kind: EntityTypeEnum,
    gravity: bool,
    collisions: bool,
    water_drag: bool,
) {
    assert_eq!(mob.kind, MobKind(kind));
    assert_eq!(mob.gravity, gravity);
    assert_eq!(mob.collisions, collisions);
    assert_eq!(mob.water_drag, water_drag);
    assert_eq!(mob.last_chunk.0, mob.position.chunk());
}

fn assert_stored(state: &GlobalStateResource, mob: &SpawnedMob, kind: EntityTypeEnum, label: &str) {
    let chunk = state
        .0
        .world
        .get_chunk(mob.position.chunk(), Dimension::Overworld)
        .unwrap_or_else(|_| panic!("{label} chunk should be present"));
    let stored = chunk
        .entities
        .get(&mob.identity.uuid)
        .unwrap_or_else(|| panic!("{label} should be persisted"));
    assert_eq!(stored.value().0, kind);
}

#[test]
fn mob_registry_round_trips_supported_bundle_metadata() {
    assert_registry_round_trip(
        MobBundle::Pig(PigBundle::new(Position::new(1.5, 64.0, 2.5))),
        EntityTypeEnum::Pig,
        MobProfile::Ground,
        Position::new(1.5, 64.0, 2.5),
    );
    assert_registry_round_trip(
        MobBundle::Fox(FoxBundle::new(Position::new(3.5, 64.0, 4.5))),
        EntityTypeEnum::Fox,
        MobProfile::Ground,
        Position::new(3.5, 64.0, 4.5),
    );
    assert_registry_round_trip(
        MobBundle::Cow(CowBundle::new(Position::new(5.5, 64.0, 6.5))),
        EntityTypeEnum::Cow,
        MobProfile::Ground,
        Position::new(5.5, 64.0, 6.5),
    );
    assert_registry_round_trip(
        MobBundle::Bat(BatBundle::new(Position::new(7.5, 70.0, 8.5))),
        EntityTypeEnum::Bat,
        MobProfile::CollisionOnly,
        Position::new(7.5, 70.0, 8.5),
    );
    assert_registry_round_trip(
        MobBundle::Axolotl(AxolotlBundle::new(Position::new(9.5, 62.0, 10.5))),
        EntityTypeEnum::Axolotl,
        MobProfile::GravityNoDrag,
        Position::new(9.5, 62.0, 10.5),
    );
    assert_registry_round_trip(
        MobBundle::Warden(WardenBundle::new(Position::new(11.5, 64.0, 12.5))),
        EntityTypeEnum::Warden,
        MobProfile::Ground,
        Position::new(11.5, 64.0, 12.5),
    );
}

#[test]
fn every_registered_mob_kind_constructs_round_trips_and_spawns() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let mut schedule = Schedule::default();
    schedule.add_systems((emit_all_mob_bundles, handle_spawn_mob_bundle).chain());
    schedule.run(&mut world);

    let mut spawned_mobs = world.query::<(
        &MobKind,
        &Position,
        Has<HasGravity>,
        Has<HasCollisions>,
        Has<HasWaterDrag>,
    )>();
    let spawned_mobs = spawned_mobs
        .iter(&world)
        .map(|(kind, position, gravity, collisions, drag)| {
            (kind.0, (*position, gravity, collisions, drag))
        })
        .collect::<HashMap<_, _>>();

    assert_eq!(
        spawned_mobs.len(),
        MobBundle::all_kinds().len(),
        "every registered mob kind should spawn exactly once"
    );

    for (index, kind) in MobBundle::all_kinds().iter().copied().enumerate() {
        let position = Position::new(index as f64 + 0.5, 80.0, 32.5);
        let Some((spawned_position, gravity, collisions, drag)) = spawned_mobs.get(&kind) else {
            panic!("missing spawned mob for {kind:?}");
        };

        assert_eq!(spawned_position.xyz(), position.xyz());

        match MobBundle::new(kind, position).profile() {
            MobProfile::Ground => {
                assert!(*gravity, "{kind:?} should have gravity");
                assert!(*collisions, "{kind:?} should have collisions");
                assert!(*drag, "{kind:?} should have water drag");
            }
            MobProfile::CollisionOnly => {
                assert!(!*gravity, "{kind:?} should not have gravity");
                assert!(*collisions, "{kind:?} should have collisions");
                assert!(!*drag, "{kind:?} should not have water drag");
            }
            MobProfile::GravityNoDrag => {
                assert!(*gravity, "{kind:?} should have gravity");
                assert!(*collisions, "{kind:?} should have collisions");
                assert!(!*drag, "{kind:?} should not have water drag");
            }
        }
    }
}

#[test]
fn spawn_bundle_messages_cover_markers_profiles_and_storage() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state.clone());

    let mut schedule = Schedule::default();
    schedule.add_systems((emit_spawn_bundles, handle_spawn_mob_bundle).chain());
    schedule.run(&mut world);

    let pig = lone_marker::<Pig>(&mut world, "pig");
    assert_spawn_profile(&pig, EntityTypeEnum::Pig, true, true, true);
    assert_stored(&state, &pig, EntityTypeEnum::Pig, "pig");

    let pasture = lone_marker::<Cow>(&mut world, "cow");
    assert_spawn_profile(&pasture, EntityTypeEnum::Cow, true, true, true);
    assert_stored(&state, &pasture, EntityTypeEnum::Cow, "cow");

    let fox = lone_marker::<Fox>(&mut world, "fox");
    assert_spawn_profile(&fox, EntityTypeEnum::Fox, true, true, true);
    assert_stored(&state, &fox, EntityTypeEnum::Fox, "fox");

    let cave_flyer = lone_marker::<Bat>(&mut world, "bat");
    assert_spawn_profile(&cave_flyer, EntityTypeEnum::Bat, false, true, false);
    assert_stored(&state, &cave_flyer, EntityTypeEnum::Bat, "bat");

    let swimmer = lone_marker::<Axolotl>(&mut world, "axolotl");
    assert_spawn_profile(&swimmer, EntityTypeEnum::Axolotl, true, true, false);
    assert_stored(&state, &swimmer, EntityTypeEnum::Axolotl, "axolotl");

    let mut pig_runtime = world.query::<(Has<Pig>, Has<Pathfinder>)>();
    let has_pathfinder = pig_runtime
        .iter(&world)
        .find_map(|(is_pig, pathfinder)| is_pig.then_some(pathfinder))
        .expect("pig runtime components should be present");
    assert!(has_pathfinder);
}

#[test]
fn spawn_mob_bundle_respects_persist_flag() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state.clone());

    let mut schedule = Schedule::default();
    schedule.add_systems((emit_unpersisted_cow, handle_spawn_mob_bundle).chain());
    schedule.run(&mut world);

    let mut cow_query = world.query::<(&Identity, &Position, Has<Cow>)>();
    let cows: Vec<_> = cow_query
        .iter(&world)
        .filter(|(_, _, is_cow)| *is_cow)
        .map(|(identity, position, _)| (identity.uuid, *position))
        .collect();

    assert_eq!(cows.len(), 1, "cow should still spawn into the ECS");
    let (cow_uuid, cow_position) = cows[0];

    let chunk = state
        .0
        .world
        .get_chunk(cow_position.chunk(), Dimension::Overworld);
    if let Ok(chunk) = chunk {
        assert!(
            chunk.entities.get(&cow_uuid).is_none(),
            "unpersisted cow should not be written to the chunk entity map"
        );
    }
}

#[test]
fn despawn_mob_message_removes_entity_and_persisted_data() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state.clone());

    let mut spawn_schedule = Schedule::default();
    spawn_schedule.add_systems((emit_persisted_cow, handle_spawn_mob_bundle).chain());
    spawn_schedule.run(&mut world);

    let mut cow_query = world.query::<(Entity, &Identity, &Position, Has<Cow>)>();
    let cows: Vec<_> = cow_query
        .iter(&world)
        .filter(|(_, _, _, is_cow)| *is_cow)
        .map(|(entity, identity, position, _)| (entity, identity.uuid, *position))
        .collect();
    assert_eq!(cows.len(), 1, "one cow should be spawned");
    let (cow_entity, cow_uuid, cow_position) = cows[0];

    let chunk = state
        .0
        .world
        .get_chunk(cow_position.chunk(), Dimension::Overworld)
        .expect("cow chunk should be present");
    assert!(
        chunk.entities.get(&cow_uuid).is_some(),
        "cow should be persisted before despawn"
    );

    let emit_despawn = move |mut writer: MessageWriter<DespawnMob>| {
        writer.write(DespawnMob {
            entity: cow_entity,
            remove_from_chunk: true,
        });
    };

    let mut despawn_schedule = Schedule::default();
    despawn_schedule.add_systems((emit_despawn, handle_despawn_mob).chain());
    despawn_schedule.run(&mut world);

    let mut cow_query = world.query::<Has<Cow>>();
    assert!(
        cow_query.iter(&world).all(|is_cow| !is_cow),
        "despawned cow should be removed from the ECS"
    );

    let chunk = state
        .0
        .world
        .get_chunk(cow_position.chunk(), Dimension::Overworld)
        .expect("cow chunk should still be present");
    assert!(
        chunk.entities.get(&cow_uuid).is_none(),
        "despawned cow should be removed from persisted chunk data"
    );
}

#[test]
fn pig_bundle_supplies_ai_and_pathfinder_components() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let mut spawn_schedule = Schedule::default();
    spawn_schedule.add_systems((emit_pig, handle_spawn_mob_bundle).chain());
    spawn_schedule.run(&mut world);

    let mut spawned_pigs =
        world.query::<(Has<Pig>, Has<PigAI>, Has<Pathfinder>, Has<PathfinderSearch>)>();
    let pigs: Vec<_> = spawned_pigs
        .iter(&world)
        .filter(|(is_pig, _, _, _)| *is_pig)
        .collect();
    assert_eq!(pigs.len(), 1, "one pig should be spawned");
    let (_, has_ai, has_pathfinder, has_search) = pigs[0];
    assert!(
        has_ai && has_pathfinder && !has_search,
        "standard mob spawn should include bundle components but not pathfinding search state"
    );
}

#[test]
fn pig_ai_requests_path_toward_nearest_player() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let pig_position = Position::new(24.5, 64.0, 9.5);
    let player_position = Position::new(30.5, 64.0, 9.5);

    let mut spawn_schedule = Schedule::default();
    spawn_schedule.add_systems((emit_pig, handle_spawn_mob_bundle).chain());
    spawn_schedule.run(&mut world);
    world.spawn((player_position, PlayerMarker));

    let mut tick_schedule = Schedule::default();
    tick_schedule.add_systems(tick_pig);
    tick_schedule.run(&mut world);

    let mut pigs = world.query::<(
        Has<Pig>,
        &Position,
        &Velocity,
        &OnGround,
        &PigAI,
        &Pathfinder,
    )>();
    let pigs: Vec<_> = pigs
        .iter(&world)
        .filter(|(is_pig, _, _, _, _, _)| *is_pig)
        .collect();

    assert_eq!(pigs.len(), 1, "one pig should be spawned");
    let (_, position, _, _, ai, pathfinder) = pigs[0];
    assert_eq!(position.xyz(), pig_position.xyz());
    assert_eq!(pathfinder.target, Some(pos_to_block(&player_position)));
    assert_eq!(ai.repath_cooldown, 40);
}

#[test]
fn warden_bundle_supplies_bossbar_owner() {
    let mut world = World::new();
    temper_messages::register_messages(&mut world);

    let (state, _temp_dir) = create_test_state();
    world.insert_resource(state);

    let mut spawn_schedule = Schedule::default();
    spawn_schedule.add_systems((emit_warden, handle_spawn_mob_bundle).chain());
    spawn_schedule.run(&mut world);

    let mut spawned_wardens = world.query::<(
        Has<Warden>,
        Has<BossbarOwner>,
        Has<PathfinderSearch>,
        Has<HasGravity>,
        Has<HasCollisions>,
        Has<HasWaterDrag>,
    )>();
    let wardens: Vec<_> = spawned_wardens
        .iter(&world)
        .filter(|(is_warden, _, _, _, _, _)| *is_warden)
        .collect();

    assert_eq!(wardens.len(), 1, "one warden should be spawned");
    let (_, has_owner, has_search, gravity, collisions, drag) = wardens[0];
    assert!(has_owner);
    assert!(!has_search);
    assert!(gravity);
    assert!(collisions);
    assert!(drag);
}
