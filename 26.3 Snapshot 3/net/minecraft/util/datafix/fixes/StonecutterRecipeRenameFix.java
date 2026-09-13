package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;

public class StonecutterRecipeRenameFix {
   public static final ImmutableMap<String, String> RECIPE_RENAMES = ImmutableMap.builder()
      .put("minecraft:chiseled_stone_bricks_stone_from_stonecutting", "minecraft:chiseled_stone_bricks_from_stone_stonecutting")
      .put("minecraft:end_stone_brick_slab_from_end_stone_brick_stonecutting", "minecraft:end_stone_brick_slab_from_end_stone_bricks_stonecutting")
      .put("minecraft:end_stone_brick_stairs_from_end_stone_brick_stonecutting", "minecraft:end_stone_brick_stairs_from_end_stone_bricks_stonecutting")
      .put("minecraft:end_stone_brick_wall_from_end_stone_brick_stonecutting", "minecraft:end_stone_brick_wall_from_end_stone_bricks_stonecutting")
      .put("minecraft:mossy_stone_brick_slab_from_mossy_stone_brick_stonecutting", "minecraft:mossy_stone_brick_slab_from_mossy_stone_bricks_stonecutting")
      .put("minecraft:mossy_stone_brick_stairs_from_mossy_stone_brick_stonecutting", "minecraft:mossy_stone_brick_stairs_from_mossy_stone_bricks_stonecutting")
      .put("minecraft:mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting", "minecraft:mossy_stone_brick_wall_from_mossy_stone_bricks_stonecutting")
      .put("minecraft:prismarine_brick_slab_from_prismarine_stonecutting", "minecraft:prismarine_brick_slab_from_prismarine_bricks_stonecutting")
      .put("minecraft:prismarine_brick_stairs_from_prismarine_stonecutting", "minecraft:prismarine_brick_stairs_from_prismarine_bricks_stonecutting")
      .put("minecraft:quartz_slab_from_stonecutting", "minecraft:quartz_slab_from_quartz_block_stonecutting")
      .put("minecraft:stone_brick_walls_from_stone_stonecutting", "minecraft:stone_brick_wall_from_stone_stonecutting")
      .build();
   public static final ImmutableMap<String, String> ADVANCEMENT_RENAMES = ImmutableMap.builder()
      .put(
         "minecraft:recipes/building_blocks/chiseled_stone_bricks_stone_from_stonecutting",
         "minecraft:recipes/building_blocks/chiseled_stone_bricks_from_stone_stonecutting"
      )
      .put(
         "minecraft:recipes/building_blocks/end_stone_brick_slab_from_end_stone_brick_stonecutting",
         "minecraft:recipes/building_blocks/end_stone_brick_slab_from_end_stone_bricks_stonecutting"
      )
      .put(
         "minecraft:recipes/building_blocks/end_stone_brick_stairs_from_end_stone_brick_stonecutting",
         "minecraft:recipes/building_blocks/end_stone_brick_stairs_from_end_stone_bricks_stonecutting"
      )
      .put(
         "minecraft:recipes/decorations/end_stone_brick_wall_from_end_stone_brick_stonecutting",
         "minecraft:recipes/decorations/end_stone_brick_wall_from_end_stone_bricks_stonecutting"
      )
      .put(
         "minecraft:recipes/building_blocks/mossy_stone_brick_slab_from_mossy_stone_brick_stonecutting",
         "minecraft:recipes/building_blocks/mossy_stone_brick_slab_from_mossy_stone_bricks_stonecutting"
      )
      .put(
         "minecraft:recipes/building_blocks/mossy_stone_brick_stairs_from_mossy_stone_brick_stonecutting",
         "minecraft:recipes/building_blocks/mossy_stone_brick_stairs_from_mossy_stone_bricks_stonecutting"
      )
      .put(
         "minecraft:recipes/building_blocks/mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting",
         "minecraft:recipes/decorations/mossy_stone_brick_wall_from_mossy_stone_bricks_stonecutting"
      )
      .put(
         "minecraft:recipes/building_blocks/prismarine_brick_slab_from_prismarine_stonecutting",
         "minecraft:recipes/building_blocks/prismarine_brick_slab_from_prismarine_bricks_stonecutting"
      )
      .put(
         "minecraft:recipes/building_blocks/prismarine_brick_stairs_from_prismarine_stonecutting",
         "minecraft:recipes/building_blocks/prismarine_brick_stairs_from_prismarine_bricks_stonecutting"
      )
      .put("minecraft:recipes/building_blocks/quartz_slab_from_stonecutting", "minecraft:recipes/building_blocks/quartz_slab_from_quartz_block_stonecutting")
      .put("minecraft:recipes/decorations/stone_brick_walls_from_stone_stonecutting", "minecraft:recipes/decorations/stone_brick_wall_from_stone_stonecutting")
      .build();
}
