use std::sync::LazyLock;

use crate::block_data::BlockData;
use crate::block_state_id::{BlockStateId, ID2BLOCK};

/// Precomputed solidity for all block states.
/// A block is solid if it has a full collision box that entities cannot walk through.
/// Indexed by `BlockStateId::raw()`.
static SOLID_BLOCKS: LazyLock<Vec<bool>> = LazyLock::new(|| {
    ID2BLOCK
        .get_or_init(|| crate::block_state_id::create_block_mappings().0)
        .iter()
        .map(compute_solid)
        .collect()
});

/// Returns whether a block is solid (has a full collision box).
///
/// This is the single source of truth for solidity, used by both the collision
/// system and the pathfinding system.
#[inline]
pub fn is_solid(id: BlockStateId) -> bool {
    SOLID_BLOCKS
        .get(id.raw() as usize)
        .copied()
        .unwrap_or(false)
}

/// Determine whether a block data entry represents a solid block.
fn compute_solid(data: &BlockData) -> bool {
    let name = data.name.trim_start_matches("minecraft:");

    // Air variants are never solid
    if name.ends_with("air") {
        return false;
    }

    // Liquids
    if matches!(name, "water" | "lava" | "bubble_column") {
        return false;
    }

    // Fire
    if matches!(name, "fire" | "soul_fire") {
        return false;
    }
    if name.ends_with("_campfire") {
        // Campfires are solid blocks you can stand on, but lit ones deal damage
        return true;
    }

    // Doors, fence gates, trapdoors: solid only when closed
    if name.ends_with("_door") || name.ends_with("_fence_gate") || name.ends_with("_trapdoor") {
        let open = data
            .properties
            .as_ref()
            .and_then(|p| p.get("open"))
            .is_some_and(|v| v == "true");
        return !open;
    }

    // Non-solid vegetation and decorations
    if is_non_solid_decoration(name) {
        return false;
    }

    // Default: solid
    true
}

/// Returns true for blocks that have no collision box (decorative, vegetation, etc.)
pub fn is_non_solid_decoration(name: &str) -> bool {
    if matches!(
        name,
        "grass"
            | "short_grass"
            | "tall_grass"
            | "fern"
            | "large_fern"
            | "dead_bush"
            | "snow"
            | "string"
            | "nether_portal"
            | "spore_blossom"
            | "glow_lichen"
            | "dandelion"
            | "poppy"
            | "blue_orchid"
            | "allium"
            | "azure_bluet"
            | "oxeye_daisy"
            | "cornflower"
            | "lily_of_the_valley"
            | "wither_rose"
            | "sunflower"
            | "lilac"
            | "rose_bush"
            | "peony"
            | "torchflower"
            | "pitcher_plant"
            | "pitcher_pod"
            | "sweet_berry_bush"
            | "cobweb"
            | "powder_snow"
            | "redstone_wire"
            | "rail"
            | "powered_rail"
            | "detector_rail"
            | "activator_rail"
            | "tripwire"
            | "tripwire_hook"
            | "structure_void"
    ) {
        return true;
    }

    name.ends_with("_button")
        || name.ends_with("_pressure_plate")
        || name.ends_with("_sign")
        || name.ends_with("_banner")
        || name.ends_with("_carpet")
        || name.ends_with("_torch")
        || name.ends_with("_sapling")
        || name.ends_with("_mushroom")
        || name.ends_with("_flower")
        || name.ends_with("_vine")
        || name.ends_with("_roots")
}
