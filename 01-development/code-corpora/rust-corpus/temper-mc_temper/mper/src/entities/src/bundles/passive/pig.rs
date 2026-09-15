crate::define_mob! {
    PigDefinition {
        kind = Pig,
        vanilla = PIG,
        bundle = PigBundle,
        marker = crate::markers::entity_types::Pig,
        profile = Ground,
        runtime = {
            ai: temper_components::mob_ai::PigAI,
            pathfinder: temper_components::pathfinder::Pathfinder,
        },
        persisted = {
            identity: temper_components::entity_identity::Identity => clone,
            metadata: temper_components::metadata::EntityMetadata => copy,
            combat: temper_components::combat::CombatProperties => copy,
            spawn: temper_components::spawn::SpawnProperties => clone,
            position: temper_components::player::position::Position => copy,
            rotation: temper_components::player::rotation::Rotation => copy,
            velocity: temper_components::player::velocity::Velocity => copy,
            on_ground: temper_components::player::grounded::OnGround => copy,
            last_synced_position: temper_components::last_synced_position::LastSyncedPosition => copy,
        },
    }
}
