use bevy_ecs::schedule::{IntoScheduleConfigs, Schedule, SystemSet};

pub mod pig;
pub mod spawn;
mod warden;

#[derive(SystemSet, Debug, Hash, PartialEq, Eq, Clone)]
pub struct MobLoadSystems;

#[derive(SystemSet, Debug, Hash, PartialEq, Eq, Clone)]
pub struct MobSaveSystems;

pub fn register_tick_systems(schedule: &mut Schedule) {
    schedule.add_systems(
        (
            pathfinding::tick_pathfinder,
            pig::tick_pig,
            pig::tick_pig_particles,
        )
            .chain(),
    );

    schedule.add_systems((warden::init_warden, warden::tick_warden).chain());
}

pub fn register_load_systems(schedule: &mut Schedule) {
    schedule.add_systems(spawn::load_mob_bundles.in_set(MobLoadSystems));
}

pub fn register_save_systems(schedule: &mut Schedule) {
    schedule.add_systems(spawn::save_mob_bundles.in_set(MobSaveSystems));
}
