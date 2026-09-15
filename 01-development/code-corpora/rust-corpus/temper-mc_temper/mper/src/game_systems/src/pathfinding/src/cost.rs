use std::sync::LazyLock;

use temper_core::block_data::BlockData;
use temper_core::block_state_id::{BlockStateId, ID2BLOCK};

/// Sentinel value meaning the block cannot be traversed.
pub const IMPASSABLE: i32 = i32::MIN;

/// Precomputed pathfinding costs for all block states.
/// Indexed by `BlockStateId::raw()`.
static PATHFINDING_COSTS: LazyLock<Vec<i32>> = LazyLock::new(|| {
    ID2BLOCK
        .get_or_init(|| temper_core::block_state_id::create_block_mappings().0)
        .iter()
        .map(compute_cost)
        .collect()
});

/// Returns the pathfinding penalty for a block, following the Minecraft wiki penalty system:
/// - IMPASSABLE: solid blocks, fences, walls, closed doors, cactus, lava, etc.
/// - 0  : air, open trapdoors, lily pads, vegetation
/// - 8  : water, honey blocks, danger zones (near fire/cactus)
/// - 16 : fire, lava, magma, lit campfire
#[inline]
pub fn block_penalty(id: BlockStateId) -> i32 {
    PATHFINDING_COSTS
        .get(id.raw() as usize)
        .copied()
        .unwrap_or(IMPASSABLE)
}

/// Compute the pathfinding cost for a single block data entry.
fn compute_cost(data: &BlockData) -> i32 {
    let name = data.name.trim_start_matches("minecraft:");

    // Air variants
    if name.ends_with("air") {
        return 0;
    }

    // Damage blocks (penalty: 16)
    if matches!(name, "fire" | "soul_fire" | "magma_block") {
        return 16;
    }
    if name.ends_with("_campfire") {
        return 16;
    }

    // Liquids
    if name == "lava" {
        return IMPASSABLE;
    }
    if name == "water" || name == "bubble_column" {
        return 8;
    }

    // Impassable hazards
    if matches!(
        name,
        "cactus" | "sweet_berry_bush" | "cobweb" | "powder_snow"
    ) {
        return IMPASSABLE;
    }

    // Fences, walls
    if name.ends_with("_fence") || name.ends_with("_wall") {
        return IMPASSABLE;
    }

    // Doors and fence gates: passable only when open
    if name.ends_with("_door") || name.ends_with("_fence_gate") {
        let open = data
            .properties
            .as_ref()
            .and_then(|p| p.get("open"))
            .is_some_and(|v| v == "true");
        return if open { 0 } else { IMPASSABLE };
    }

    // Trapdoors: passable only when open
    if name.ends_with("_trapdoor") {
        let open = data
            .properties
            .as_ref()
            .and_then(|p| p.get("open"))
            .is_some_and(|v| v == "true");
        return if open { 0 } else { IMPASSABLE };
    }

    // Known non-solid blocks (shared definition with the collision system)
    // lily_pad, dripleaves: walkable surfaces not in the collision list
    if temper_core::block_properties::is_non_solid_decoration(name)
        || matches!(name, "lily_pad" | "big_dripleaf" | "small_dripleaf")
    {
        return 0;
    }

    // Default: solid/impassable
    IMPASSABLE
}

#[cfg(test)]
mod tests {
    use super::*;
    use temper_macros::block;

    /// Helper to assert a block has the expected penalty.
    fn assert_penalty(id: BlockStateId, expected: i32, name: &str) {
        assert_eq!(
            block_penalty(id),
            expected,
            "{name} should have penalty {expected}"
        );
    }

    #[test]
    fn passable_blocks_have_zero_cost() {
        assert_penalty(block!("air"), 0, "air");
        assert_penalty(block!("short_grass"), 0, "short_grass");
        assert_penalty(block!("wall_torch", { facing: "north" }), 0, "wall_torch");
        assert_penalty(block!("red_carpet"), 0, "red_carpet");
    }

    #[test]
    fn water_has_medium_penalty() {
        assert_penalty(block!("water", { level: 0 }), 8, "water");
    }

    #[test]
    fn damage_blocks_have_high_penalty() {
        assert_penalty(
            block!("fire", { age: 0, east: false, north: false, south: false, up: false, west: false }),
            16,
            "fire",
        );
        assert_penalty(block!("magma_block"), 16, "magma_block");
    }

    #[test]
    fn solid_and_hazard_blocks_are_impassable() {
        assert_penalty(block!("stone"), IMPASSABLE, "stone");
        assert_penalty(block!("lava", { level: 0 }), IMPASSABLE, "lava");
        assert_penalty(block!("cactus", { age: 0 }), IMPASSABLE, "cactus");
        assert_penalty(
            block!("oak_fence", { east: false, north: false, south: false, waterlogged: false, west: false }),
            IMPASSABLE,
            "oak_fence",
        );
    }

    #[test]
    fn doors_depend_on_open_property() {
        assert_penalty(
            block!("oak_door", { open: true, half: "lower", facing: "north", hinge: "left", powered: false }),
            0,
            "oak_door (open)",
        );
        assert_penalty(
            block!("oak_door", { open: false, half: "lower", facing: "north", hinge: "left", powered: false }),
            IMPASSABLE,
            "oak_door (closed)",
        );
    }

    #[test]
    fn trapdoors_depend_on_open_property() {
        assert_penalty(
            block!("oak_trapdoor", { open: true, half: "bottom", facing: "north", powered: false, waterlogged: false }),
            0,
            "oak_trapdoor (open)",
        );
        assert_penalty(
            block!("oak_trapdoor", { open: false, half: "bottom", facing: "north", powered: false, waterlogged: false }),
            IMPASSABLE,
            "oak_trapdoor (closed)",
        );
    }
}
