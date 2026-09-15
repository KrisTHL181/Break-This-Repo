use bevy_ecs::prelude::*;
use temper_core::pos::BlockPos;

const DEFAULT_BUDGET_PER_TICK: usize = 20;
const DEFAULT_MAX_NODES: usize = 500;

/// Pathfinding component for land mob entities.
/// Set `target` each tick (or periodically) from mob AI. The system `tick_pathfinder`
/// advances the A* search incrementally (budget_per_tick nodes/tick) and updates `path`.
/// The mob AI reads `current_waypoint()` and calls `advance_waypoint()` when it arrives.
#[derive(Component)]
pub struct Pathfinder {
    /// Target block to navigate to. Changing this restarts the search.
    pub target: Option<BlockPos>,
    /// Current computed path (nodes from start to goal).
    pub path: Vec<BlockPos>,
    /// Index of the waypoint the mob is currently heading toward.
    pub waypoint: usize,
    /// A* node expansions allowed per tick.
    pub budget_per_tick: usize,
    /// Maximum total A* expansions before giving up.
    pub max_nodes: usize,
    /// True while a search is in progress. Set by `tick_pathfinder`.
    pub is_searching: bool,
    #[doc(hidden)]
    pub last_target: Option<BlockPos>,
}

impl Pathfinder {
    pub fn new(budget_per_tick: usize, max_nodes: usize) -> Self {
        Self {
            target: None,
            path: Vec::new(),
            waypoint: 0,
            budget_per_tick,
            max_nodes,
            is_searching: false,
            last_target: None,
        }
    }

    /// The block the mob should currently move toward.
    pub fn current_waypoint(&self) -> Option<BlockPos> {
        self.path.get(self.waypoint).copied()
    }

    /// Move to the next waypoint after reaching the current one.
    pub fn advance_waypoint(&mut self) {
        self.waypoint += 1;
    }

    /// True if a path is available and not yet exhausted.
    pub fn has_path(&self) -> bool {
        self.waypoint < self.path.len()
    }

    /// Force a new search on the next tick, even if the target hasn't changed.
    pub fn request_repath(&mut self) {
        self.last_target = None;
        self.is_searching = false;
    }
}

impl Default for Pathfinder {
    fn default() -> Self {
        Self::new(DEFAULT_BUDGET_PER_TICK, DEFAULT_MAX_NODES)
    }
}
