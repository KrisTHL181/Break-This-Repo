use std::collections::BinaryHeap;

use arrayvec::ArrayVec;
use rustc_hash::FxHashMap;
use temper_components::physical::PhysicalProperties;
use temper_core::block_state_id::BlockStateId;
use temper_core::pos::BlockPos;
use temper_world::Dimension;

use temper_core::block_properties;

use crate::cost::{IMPASSABLE, block_penalty};

/// A path from start to goal, expressed as block positions (feet position).
pub struct Path {
    pub nodes: Vec<BlockPos>,
}

// Internal node in the priority queue.
// Ord is inverted so BinaryHeap acts as a min-heap on estimated_cost.
#[derive(Eq, PartialEq)]
struct Candidate {
    estimated_cost: i32, // f = g + h
    real_cost: i32,      // g
    pos: BlockPos,
}

impl Ord for Candidate {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        other.estimated_cost.cmp(&self.estimated_cost)
    }
}

impl PartialOrd for Candidate {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

/// Entity dimensions for pathfinding, computed from PhysicalProperties.
#[derive(Clone, Copy)]
pub(crate) struct EntityDimensions {
    /// Height in blocks (rounded up). E.g. pig=1, zombie=2, enderman=3.
    height_blocks: u8,
    /// Half-width in blocks (rounded up). E.g. 0.45 -> 1 block.
    /// TODO: Use this for wider entities like spiders that occupy multiple blocks horizontally.
    #[allow(dead_code)]
    half_width_blocks: u8,
    // TODO: Use fire_immune from PhysicalProperties to avoid lava/fire penalties
    // for entities like blazes, striders, etc.
}

impl EntityDimensions {
    fn from_physical(props: &PhysicalProperties) -> Self {
        Self {
            height_blocks: props.bounding_box.height().ceil() as u8,
            half_width_blocks: (props.bounding_box.width() / 2.0).ceil() as u8,
        }
    }
}

/// Result of a single incremental A* step.
pub(crate) enum SearchStep {
    /// A path was found; contains the nodes from start to goal.
    Found(Vec<BlockPos>),
    /// No path exists (open set exhausted or node limit reached).
    NoPath,
    /// Budget exhausted; call `step` again next tick.
    Continue,
}

/// Incremental A* search state. Call `step` each tick with a node budget.
pub(crate) struct AStarSearch {
    open: BinaryHeap<Candidate>,
    g_score: FxHashMap<BlockPos, i32>,
    came_from: FxHashMap<BlockPos, BlockPos>,
    start: BlockPos,
    goal: BlockPos,
    dims: EntityDimensions,
    pub nodes_expanded: usize,
    pub max_nodes: usize,
}

impl AStarSearch {
    pub(crate) fn new(
        start: BlockPos,
        goal: BlockPos,
        max_nodes: usize,
        physical: &PhysicalProperties,
    ) -> Self {
        let dims = EntityDimensions::from_physical(physical);
        let mut open = BinaryHeap::new();
        let mut g_score = FxHashMap::default();
        g_score.insert(start, 0);
        open.push(Candidate {
            estimated_cost: heuristic(start, goal),
            real_cost: 0,
            pos: start,
        });
        Self {
            open,
            g_score,
            came_from: FxHashMap::default(),
            start,
            goal,
            dims,
            nodes_expanded: 0,
            max_nodes,
        }
    }

    /// Advance the search by up to `budget` node expansions.
    pub(crate) fn step(&mut self, world: &temper_world::World, budget: usize) -> SearchStep {
        let mut expanded = 0;
        while let Some(Candidate { real_cost, pos, .. }) = self.open.pop() {
            if self.nodes_expanded >= self.max_nodes {
                return SearchStep::NoPath;
            }
            self.nodes_expanded += 1;
            expanded += 1;

            if pos == self.goal {
                let nodes = reconstruct_path(&self.came_from, pos, self.start);
                return SearchStep::Found(nodes);
            }

            if real_cost > *self.g_score.get(&pos).unwrap_or(&i32::MAX) {
                if expanded >= budget {
                    return SearchStep::Continue;
                }
                continue;
            }

            for (neighbor, move_cost) in neighbors(world, pos, self.dims) {
                let tentative_g = real_cost + move_cost;
                if self
                    .g_score
                    .get(&neighbor)
                    .is_none_or(|&best| tentative_g < best)
                {
                    self.g_score.insert(neighbor, tentative_g);
                    self.came_from.insert(neighbor, pos);
                    self.open.push(Candidate {
                        estimated_cost: tentative_g + heuristic(neighbor, self.goal),
                        real_cost: tentative_g,
                        pos: neighbor,
                    });
                }
            }

            if expanded >= budget {
                return SearchStep::Continue;
            }
        }
        // Open set exhausted
        SearchStep::NoPath
    }
}

/// Find a path for a land mob using weighted A*.
///
/// `start` and `goal` are the block positions of the mob's feet.
/// `physical` provides the entity's dimensions for collision checking.
/// Returns `None` if no path is found within `max_nodes` node expansions.
pub fn find_path(
    world: &temper_world::World,
    start: BlockPos,
    goal: BlockPos,
    max_nodes: usize,
    physical: &PhysicalProperties,
) -> Option<Path> {
    if start == goal {
        return Some(Path { nodes: vec![goal] });
    }

    let mut search = AStarSearch::new(start, goal, max_nodes, physical);
    match search.step(world, usize::MAX) {
        SearchStep::Found(nodes) => Some(Path { nodes }),
        SearchStep::NoPath | SearchStep::Continue => None,
    }
}

/// Heuristic using octile distance (accounts for diagonal movement).
/// Returns cost estimate scaled to match movement costs (cardinal=10, diagonal=14).
fn heuristic(a: BlockPos, b: BlockPos) -> i32 {
    let dx = (a.pos.x - b.pos.x).abs();
    let dy = (a.pos.y - b.pos.y).abs();
    let dz = (a.pos.z - b.pos.z).abs();

    // Octile distance on XZ plane + vertical distance
    let min_xz = dx.min(dz);
    let max_xz = dx.max(dz);

    // Diagonal moves cost 14, cardinal moves cost 10
    // min_xz diagonals + (max_xz - min_xz) cardinals + dy vertical
    min_xz * COST_DIAGONAL + (max_xz - min_xz) * COST_CARDINAL + dy * COST_CARDINAL
}

fn reconstruct_path(
    came_from: &FxHashMap<BlockPos, BlockPos>,
    target: BlockPos,
    start: BlockPos,
) -> Vec<BlockPos> {
    let mut current = target;
    let mut nodes = vec![current];
    while current != start {
        current = came_from[&current];
        nodes.push(current);
    }
    nodes.reverse();
    nodes
}

/// Cardinal directions (cost multiplier: 10).
const CARDINALS: [(i32, i32); 4] = [(1, 0), (-1, 0), (0, 1), (0, -1)];

/// Diagonal directions (cost multiplier: 14, approximation of 10 * sqrt(2)).
const DIAGONALS: [(i32, i32); 4] = [(1, 1), (1, -1), (-1, 1), (-1, -1)];

/// Base movement cost for cardinal directions.
const COST_CARDINAL: i32 = 10;

/// Base movement cost for diagonal directions (approx. 10 * sqrt(2)).
const COST_DIAGONAL: i32 = 14;

/// Extra cost for stepping up one block.
const COST_STEP_UP: i32 = 10;

/// Maximum number of neighbors per node (4 cardinal + 4 diagonal).
const MAX_NEIGHBORS: usize = 8;

/// Generate passable neighbors for a land mob.
/// Handles flat walking, stepping up 1 block, and stepping down 1 block.
/// Supports both cardinal and diagonal movement.
fn neighbors(
    world: &temper_world::World,
    pos: BlockPos,
    dims: EntityDimensions,
) -> ArrayVec<(BlockPos, i32), MAX_NEIGHBORS> {
    let mut result = ArrayVec::new();

    // Cardinal directions
    for (dx, dz) in CARDINALS {
        if let Some((dest, cost)) = try_move(world, pos, dx, dz, COST_CARDINAL, dims) {
            result.push((dest, cost));
        }
    }

    // Diagonal directions (require both adjacent cardinal directions to be passable)
    for (dx, dz) in DIAGONALS {
        // Check corner-cutting: both adjacent cells must be passable at feet level
        let side1_passable =
            block_penalty(get_block(world, pos.pos.x + dx, pos.pos.y, pos.pos.z)) != IMPASSABLE;
        let side2_passable =
            block_penalty(get_block(world, pos.pos.x, pos.pos.y, pos.pos.z + dz)) != IMPASSABLE;

        if side1_passable
            && side2_passable
            && let Some((dest, cost)) = try_move(world, pos, dx, dz, COST_DIAGONAL, dims)
        {
            result.push((dest, cost));
        }
    }

    result
}

/// Try to move from `pos` in direction `(dx, dz)` with base cost `base_cost`.
/// Returns the destination and total movement cost if the move is valid.
fn try_move(
    world: &temper_world::World,
    pos: BlockPos,
    dx: i32,
    dz: i32,
    base_cost: i32,
    dims: EntityDimensions,
) -> Option<(BlockPos, i32)> {
    let nx = pos.pos.x + dx;
    let nz = pos.pos.z + dz;

    // Walk flat
    if let Some(terrain_cost) = can_stand_at(world, nx, pos.pos.y, nz, dims) {
        return Some((BlockPos::of(nx, pos.pos.y, nz), base_cost + terrain_cost));
    }

    // Step up 1 block — need space above current position for the full entity height
    if is_clear_above(world, pos.pos.x, pos.pos.y, pos.pos.z, dims.height_blocks)
        && let Some(terrain_cost) = can_stand_at(world, nx, pos.pos.y + 1, nz, dims)
    {
        return Some((
            BlockPos::of(nx, pos.pos.y + 1, nz),
            base_cost + COST_STEP_UP + terrain_cost,
        ));
    }

    // Step down 1 block — neighbor column must be open at current height
    if block_penalty(get_block(world, nx, pos.pos.y, nz)) != IMPASSABLE
        && let Some(terrain_cost) = can_stand_at(world, nx, pos.pos.y - 1, nz, dims)
    {
        return Some((
            BlockPos::of(nx, pos.pos.y - 1, nz),
            base_cost + terrain_cost,
        ));
    }

    None
}

/// Check if an entity can stand with feet at (x, y, z):
/// - solid block at (x, y-1, z) as floor (uses `block_properties::is_solid`)
/// - passable blocks for the full body height at (x, y, z) to (x, y+height-1, z)
///
/// Returns `Some(terrain_cost)` if valid, `None` if not.
fn can_stand_at(
    world: &temper_world::World,
    x: i32,
    y: i32,
    z: i32,
    dims: EntityDimensions,
) -> Option<i32> {
    // Check for solid floor (shared definition with the collision system)
    if !block_properties::is_solid(get_block(world, x, y - 1, z)) {
        return None; // no solid floor
    }

    // Check all blocks occupied by the body
    let mut total_penalty = 0;
    for dy in 0..i32::from(dims.height_blocks) {
        let body_penalty = block_penalty(get_block(world, x, y + dy, z));
        if body_penalty == IMPASSABLE {
            return None;
        }
        total_penalty += body_penalty.max(0);
    }

    Some(total_penalty)
}

/// Check if there's enough vertical clearance above a position.
/// Used for step-up checks where the entity needs headroom.
fn is_clear_above(world: &temper_world::World, x: i32, y: i32, z: i32, height: u8) -> bool {
    for dy in 1..=i32::from(height) {
        if block_penalty(get_block(world, x, y + dy, z)) == IMPASSABLE {
            return false;
        }
    }
    true
}

fn get_block(world: &temper_world::World, x: i32, y: i32, z: i32) -> BlockStateId {
    let pos = BlockPos::of(x, y, z);
    world
        .get_chunk(pos.chunk(), Dimension::Overworld)
        .map(|chunk| chunk.get_block(pos.chunk_block_pos()))
        .unwrap_or_default() // chunk not in cache or DB = air; mob won't path there (no solid floor)
}

#[cfg(test)]
mod tests {
    use super::*;
    use temper_core::pos::ChunkPos;
    use temper_macros::block;
    use temper_state::create_test_state;
    use tempfile::TempDir;

    fn pig() -> PhysicalProperties {
        PhysicalProperties::from_vanilla(&temper_data::generated::entities::EntityType::PIG)
    }

    fn zombie() -> PhysicalProperties {
        PhysicalProperties::from_vanilla(&temper_data::generated::entities::EntityType::ZOMBIE)
    }

    /// Test helper that provides a flat world (stone floor at y=64) and
    /// convenience methods for placing blocks and running pathfinding.
    struct TestEnv {
        state: temper_state::GlobalStateResource,
        _temp_dir: TempDir,
    }

    impl TestEnv {
        /// Creates a new test environment with a stone floor at y=64.
        fn new() -> Self {
            let (state, _temp_dir) = create_test_state();
            let env = Self { state, _temp_dir };
            env.fill_floor(64);
            env
        }

        /// Fills an entire chunk layer at the given y with stone.
        fn fill_floor(&self, y: i32) {
            let chunk_pos = ChunkPos::new(0, 0);
            let mut chunk = self
                .state
                .0
                .world
                .get_or_generate_mut(chunk_pos, Dimension::Overworld)
                .expect("Failed to get chunk");
            for x in 0u8..16 {
                for z in 0u8..16 {
                    chunk.set_block(
                        BlockPos::of(i32::from(x), y, i32::from(z)).chunk_block_pos(),
                        block!("stone"),
                    );
                }
            }
        }

        /// Places a single block in the world.
        fn set_block(&self, x: i32, y: i32, z: i32, id: BlockStateId) {
            let pos = BlockPos::of(x, y, z);
            let mut chunk = self
                .state
                .0
                .world
                .get_or_generate_mut(pos.chunk(), Dimension::Overworld)
                .expect("Failed to get chunk");
            chunk.set_block(pos.chunk_block_pos(), id);
        }

        /// Places a vertical wall (stone) along the Z axis at the given x and y.
        fn wall_z(&self, x: i32, y: i32, z_range: std::ops::Range<i32>) {
            for z in z_range {
                self.set_block(x, y, z, block!("stone"));
            }
        }

        /// Fills a rectangular area at the given y with stone.
        fn fill_rect(&self, x_range: std::ops::Range<i32>, y: i32, z_range: std::ops::Range<i32>) {
            for x in x_range {
                for z in z_range.clone() {
                    self.set_block(x, y, z, block!("stone"));
                }
            }
        }

        /// Surrounds a position with stone walls at the given heights.
        fn cage(&self, center_x: i32, center_z: i32, y_range: std::ops::Range<i32>) {
            for dx in -1..=1 {
                for dz in -1..=1 {
                    if dx != 0 || dz != 0 {
                        for y in y_range.clone() {
                            self.set_block(center_x + dx, y, center_z + dz, block!("stone"));
                        }
                    }
                }
            }
        }

        /// Runs pathfinding with a generous node budget.
        fn find(
            &self,
            start: BlockPos,
            goal: BlockPos,
            physical: &PhysicalProperties,
        ) -> Option<Path> {
            find_path(&self.state.0.world, start, goal, 1000, physical)
        }

        /// Runs pathfinding with a specific node budget.
        fn find_with_budget(
            &self,
            start: BlockPos,
            goal: BlockPos,
            max_nodes: usize,
            physical: &PhysicalProperties,
        ) -> Option<Path> {
            find_path(&self.state.0.world, start, goal, max_nodes, physical)
        }
    }
    #[test]
    fn test_straight_line_path() {
        let env = TestEnv::new();
        let start = BlockPos::of(0, 65, 0);
        let goal = BlockPos::of(3, 65, 0);

        let path = env
            .find(start, goal, &pig())
            .expect("Should find a path on flat ground");

        assert_eq!(path.nodes.first(), Some(&start));
        assert_eq!(path.nodes.last(), Some(&goal));
    }

    #[test]
    fn test_diagonal_path() {
        let env = TestEnv::new();
        let start = BlockPos::of(0, 65, 0);
        let goal = BlockPos::of(3, 65, 3);

        let path = env
            .find(start, goal, &pig())
            .expect("Should find a diagonal path");

        // With diagonals: (0,0) -> (1,1) -> (2,2) -> (3,3) = 4 nodes
        assert!(
            path.nodes.len() <= 5,
            "Diagonal path should be efficient, got {} nodes",
            path.nodes.len()
        );
    }

    #[test]
    fn test_path_around_wall() {
        let env = TestEnv::new();
        env.wall_z(5, 65, 0..16);

        let start = BlockPos::of(0, 65, 5);
        let goal = BlockPos::of(10, 65, 5);

        let path = env
            .find(start, goal, &pig())
            .expect("Should find a path around the wall");

        assert!(path.nodes.len() > 10, "Path should be longer due to wall");
    }

    #[test]
    fn test_step_up() {
        let env = TestEnv::new();
        env.set_block(5, 65, 5, block!("stone")); // Step

        let start = BlockPos::of(4, 65, 5);
        let goal = BlockPos::of(5, 66, 5);

        let path = env
            .find(start, goal, &pig())
            .expect("Should find a path stepping up");

        assert_eq!(path.nodes.last(), Some(&goal));
    }

    #[test]
    fn test_step_down() {
        let env = TestEnv::new();
        env.set_block(5, 65, 5, block!("stone")); // Step

        let start = BlockPos::of(5, 66, 5);
        let goal = BlockPos::of(4, 65, 5);

        let path = env
            .find(start, goal, &pig())
            .expect("Should find a path stepping down");

        assert_eq!(path.nodes.last(), Some(&goal));
    }

    #[test]
    fn test_no_path_blocked() {
        let env = TestEnv::new();
        env.cage(2, 2, 65..67); // 2 blocks high cage

        let start = BlockPos::of(2, 65, 2);
        let goal = BlockPos::of(10, 65, 10);

        let path = env.find(start, goal, &pig());

        assert!(
            path.is_none(),
            "Should not find a path when completely blocked"
        );
    }

    #[test]
    fn test_same_start_and_goal() {
        let env = TestEnv::new();
        let pos = BlockPos::of(5, 65, 5);

        let path = env
            .find(pos, pos, &pig())
            .expect("Should return a path for same start and goal");

        assert_eq!(path.nodes.len(), 1);
        assert_eq!(path.nodes[0], pos);
    }

    #[test]
    fn test_tall_entity_blocked_by_low_ceiling() {
        let env = TestEnv::new();
        env.fill_rect(3..8, 66, 0..16); // Low ceiling at y=66

        let start = BlockPos::of(0, 65, 5);
        let goal = BlockPos::of(10, 65, 5);

        // Pig (height < 1 block) should pass
        assert!(
            env.find(start, goal, &pig()).is_some(),
            "Pig should fit under low ceiling"
        );

        // Zombie (height ~2 blocks) should not pass
        assert!(
            env.find(start, goal, &zombie()).is_none(),
            "Zombie should not fit under low ceiling"
        );
    }

    #[test]
    fn test_max_nodes_limit() {
        let env = TestEnv::new();
        let start = BlockPos::of(0, 65, 0);
        let goal = BlockPos::of(15, 65, 15);

        let path = env.find_with_budget(start, goal, 5, &pig());

        assert!(
            path.is_none(),
            "Should not find path with very limited node budget"
        );
    }
}
