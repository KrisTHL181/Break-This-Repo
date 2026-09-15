use bevy_ecs::prelude::*;
use temper_components::pathfinder::Pathfinder;
use temper_components::player::position::Position;
use temper_core::pos::BlockPos;
use temper_entities::PhysicalRegistry;
use temper_entities::components::{Baby, EntityMetadata};
use temper_state::GlobalStateResource;

use crate::astar::{AStarSearch, SearchStep};

/// Internal A* search state, kept separate from the public Pathfinder component
/// to avoid a dependency on this crate from temper-components.
#[derive(Component, Default)]
pub struct PathfinderSearch(pub(crate) Option<AStarSearch>);

type PathfinderQuery<'w, 's> = Query<
    'w,
    's,
    (
        Entity,
        &'static mut Pathfinder,
        Option<&'static mut PathfinderSearch>,
        &'static Position,
        &'static EntityMetadata,
        Has<Baby>,
    ),
>;

/// Advances incremental A* searches for all entities with a Pathfinder component.
/// Must run before mob AI systems so the updated path is available each tick.
pub fn tick_pathfinder(
    mut commands: Commands,
    mut query: PathfinderQuery,
    state: Res<GlobalStateResource>,
    registry: Res<PhysicalRegistry>,
) {
    let world = &state.0.world;

    for (entity, mut pf, search, pos, metadata, is_baby) in &mut query {
        let Some(props) = registry.get(metadata.protocol_id(), is_baby) else {
            continue;
        };

        let mut search = match search {
            Some(s) => s,
            None => {
                commands.entity(entity).insert(PathfinderSearch::default());
                continue;
            }
        };

        // Restart search when target changes, but keep the old path so the mob
        // keeps moving while the new path is being computed.
        if pf.target != pf.last_target {
            pf.last_target = pf.target;
            search.0 = None;

            if let Some(goal) = pf.target {
                let start = pos_to_block(pos);
                if start == goal {
                    pf.path = vec![goal];
                    pf.waypoint = 1; // already at goal, nothing to traverse
                    pf.is_searching = false;
                } else {
                    search.0 = Some(AStarSearch::new(start, goal, pf.max_nodes, props));
                    pf.is_searching = true;
                }
            }
        }

        // Advance the in-progress search by up to budget_per_tick expansions.
        let budget = pf.budget_per_tick;
        if let Some(ref mut astar) = search.0 {
            match astar.step(world, budget) {
                SearchStep::Found(nodes) => {
                    pf.path = nodes;
                    pf.waypoint = 1; // node 0 is the start position
                    pf.is_searching = false;
                    search.0 = None;
                }
                SearchStep::NoPath => {
                    pf.path.clear();
                    pf.waypoint = 0;
                    pf.is_searching = false;
                    search.0 = None;
                }
                SearchStep::Continue => {}
            }
        }
    }
}

/// Convert a world-space position to a block position.
///
/// The small Y epsilon compensates for imprecise collision resolution
/// that can leave entities at e.g. y=64.9999 instead of exactly y=65.0.
pub fn pos_to_block(pos: &Position) -> BlockPos {
    const EPSILON: f64 = 1e-4;
    BlockPos::of(
        pos.x.floor() as i32,
        (pos.y + EPSILON).floor() as i32,
        pos.z.floor() as i32,
    )
}

#[cfg(test)]
mod tests {
    use super::*;
    use bevy_ecs::prelude::{Entity, Mut, Schedule, World};
    use temper_components::pathfinder::Pathfinder;
    use temper_components::player::position::Position;
    use temper_core::block_state_id::BlockStateId;
    use temper_core::pos::{BlockPos, ChunkPos};
    use temper_data::generated::entities::EntityType as VanillaEntityType;
    use temper_entities::PhysicalRegistry;
    use temper_entities::components::EntityMetadata;
    use temper_macros::block;
    use temper_state::create_test_state;
    use temper_world::Dimension;
    use tempfile::TempDir;

    struct TestEnv {
        ecs: World,
        _temp_dir: TempDir,
    }

    impl TestEnv {
        fn new() -> Self {
            let (state, temp_dir) = create_test_state();

            let chunk_pos = ChunkPos::new(0, 0);
            {
                let mut chunk = state
                    .0
                    .world
                    .get_or_generate_mut(chunk_pos, Dimension::Overworld)
                    .unwrap();
                for x in 0u8..16 {
                    for z in 0u8..16 {
                        chunk.set_block(
                            BlockPos::of(i32::from(x), 64, i32::from(z)).chunk_block_pos(),
                            block!("stone"),
                        );
                    }
                }
            }

            let mut ecs = World::new();
            ecs.insert_resource(state);
            ecs.insert_resource(PhysicalRegistry::new());
            Self {
                ecs,
                _temp_dir: temp_dir,
            }
        }

        fn spawn_pig(&mut self, x: f64, z: f64) -> Entity {
            self.ecs
                .spawn((
                    Pathfinder::default(),
                    PathfinderSearch::default(),
                    Position::new(x, 65.0, z),
                    EntityMetadata::from_vanilla(&VanillaEntityType::PIG),
                ))
                .id()
        }

        fn run(&mut self) {
            let mut schedule = Schedule::default();
            schedule.add_systems(tick_pathfinder);
            schedule.run(&mut self.ecs);
        }

        fn pathfinder(&self, entity: Entity) -> &Pathfinder {
            self.ecs.get::<Pathfinder>(entity).unwrap()
        }

        fn pathfinder_mut(&mut self, entity: Entity) -> Mut<'_, Pathfinder> {
            self.ecs.get_mut::<Pathfinder>(entity).unwrap()
        }
    }

    #[test]
    fn same_start_and_goal_gives_immediate_path() {
        let mut env = TestEnv::new();
        let pig = env.spawn_pig(0.0, 0.0);

        env.pathfinder_mut(pig).target = Some(BlockPos::of(0, 65, 0));
        env.run();

        let pf = env.pathfinder(pig);
        assert_eq!(pf.path.len(), 1);
        assert!(!pf.is_searching);
        assert!(!pf.has_path());
    }

    #[test]
    fn setting_target_starts_search() {
        let mut env = TestEnv::new();
        let pig = env
            .ecs
            .spawn((
                Pathfinder::new(1, 500),
                PathfinderSearch::default(),
                Position::new(0.0, 65.0, 0.0),
                EntityMetadata::from_vanilla(&VanillaEntityType::PIG),
            ))
            .id();

        env.pathfinder_mut(pig).target = Some(BlockPos::of(5, 65, 0));
        env.run();

        assert!(env.pathfinder(pig).is_searching);
    }

    #[test]
    fn path_found_after_enough_ticks() {
        let mut env = TestEnv::new();
        let pig = env.spawn_pig(0.0, 0.0);

        env.pathfinder_mut(pig).target = Some(BlockPos::of(5, 65, 0));
        for _ in 0..50 {
            env.run();
            if !env.pathfinder(pig).is_searching {
                break;
            }
        }

        let pf = env.pathfinder(pig);
        assert!(!pf.is_searching);
        assert!(pf.has_path());
        assert_eq!(pf.path.last(), Some(&BlockPos::of(5, 65, 0)));
    }

    #[test]
    fn changing_target_restarts_search() {
        let mut env = TestEnv::new();
        let pig = env
            .ecs
            .spawn((
                Pathfinder::new(1, 500),
                PathfinderSearch::default(),
                Position::new(0.0, 65.0, 0.0),
                EntityMetadata::from_vanilla(&VanillaEntityType::PIG),
            ))
            .id();

        env.pathfinder_mut(pig).target = Some(BlockPos::of(3, 65, 0));
        for _ in 0..50 {
            env.run();
            if !env.pathfinder(pig).is_searching {
                break;
            }
        }
        assert!(env.pathfinder(pig).has_path());

        env.pathfinder_mut(pig).target = Some(BlockPos::of(5, 65, 5));
        env.run();
        assert!(env.pathfinder(pig).is_searching);
    }

    #[test]
    fn no_target_leaves_path_empty() {
        let mut env = TestEnv::new();
        let pig = env.spawn_pig(0.0, 0.0);
        env.run();

        let pf = env.pathfinder(pig);
        assert!(!pf.has_path());
        assert!(!pf.is_searching);
    }
}
