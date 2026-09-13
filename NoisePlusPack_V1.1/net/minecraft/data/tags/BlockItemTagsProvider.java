package net.minecraft.data.tags;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public abstract class BlockItemTagsProvider {
   protected void run() {
      Block[] ablock = new Block[]{
         Blocks.DANDELION,
         Blocks.OPEN_EYEBLOSSOM,
         Blocks.POPPY,
         Blocks.BLUE_ORCHID,
         Blocks.ALLIUM,
         Blocks.AZURE_BLUET,
         Blocks.RED_TULIP,
         Blocks.ORANGE_TULIP,
         Blocks.WHITE_TULIP,
         Blocks.PINK_TULIP,
         Blocks.OXEYE_DAISY,
         Blocks.CORNFLOWER,
         Blocks.LILY_OF_THE_VALLEY,
         Blocks.WITHER_ROSE,
         Blocks.TORCHFLOWER
      };
      Block[] ablock1 = new Block[]{
         Blocks.SUNFLOWER,
         Blocks.LILAC,
         Blocks.PEONY,
         Blocks.ROSE_BUSH,
         Blocks.PITCHER_PLANT,
         Blocks.FLOWERING_AZALEA_LEAVES,
         Blocks.FLOWERING_AZALEA,
         Blocks.MANGROVE_PROPAGULE,
         Blocks.CHERRY_LEAVES,
         Blocks.PINK_PETALS,
         Blocks.WILDFLOWERS,
         Blocks.CHORUS_FLOWER,
         Blocks.SPORE_BLOSSOM,
         Blocks.CACTUS_FLOWER
      };
      this.tag(BlockTags.WOOL, ItemTags.WOOL)
         .add(
            Blocks.WHITE_WOOL,
            Blocks.ORANGE_WOOL,
            Blocks.MAGENTA_WOOL,
            Blocks.LIGHT_BLUE_WOOL,
            Blocks.YELLOW_WOOL,
            Blocks.LIME_WOOL,
            Blocks.PINK_WOOL,
            Blocks.GRAY_WOOL,
            Blocks.LIGHT_GRAY_WOOL,
            Blocks.CYAN_WOOL,
            Blocks.PURPLE_WOOL,
            Blocks.BLUE_WOOL,
            Blocks.BROWN_WOOL,
            Blocks.GREEN_WOOL,
            Blocks.RED_WOOL,
            Blocks.BLACK_WOOL
         );
      this.tag(BlockTags.PLANKS, ItemTags.PLANKS)
         .add(
            Blocks.OAK_PLANKS,
            Blocks.SPRUCE_PLANKS,
            Blocks.BIRCH_PLANKS,
            Blocks.JUNGLE_PLANKS,
            Blocks.ACACIA_PLANKS,
            Blocks.DARK_OAK_PLANKS,
            Blocks.PALE_OAK_PLANKS,
            Blocks.CRIMSON_PLANKS,
            Blocks.WARPED_PLANKS,
            Blocks.MANGROVE_PLANKS,
            Blocks.BAMBOO_PLANKS,
            Blocks.CHERRY_PLANKS
         );
      this.tag(BlockTags.STONE_BRICKS, ItemTags.STONE_BRICKS)
         .add(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS);
      this.tag(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS)
         .add(
            Blocks.OAK_BUTTON,
            Blocks.SPRUCE_BUTTON,
            Blocks.BIRCH_BUTTON,
            Blocks.JUNGLE_BUTTON,
            Blocks.ACACIA_BUTTON,
            Blocks.DARK_OAK_BUTTON,
            Blocks.PALE_OAK_BUTTON,
            Blocks.CRIMSON_BUTTON,
            Blocks.WARPED_BUTTON,
            Blocks.MANGROVE_BUTTON,
            Blocks.BAMBOO_BUTTON,
            Blocks.CHERRY_BUTTON
         );
      this.tag(BlockTags.STONE_BUTTONS, ItemTags.STONE_BUTTONS).add(Blocks.STONE_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON);
      this.tag(BlockTags.BUTTONS, ItemTags.BUTTONS).addTag(BlockTags.WOODEN_BUTTONS).addTag(BlockTags.STONE_BUTTONS);
      this.tag(BlockTags.WOOL_CARPETS, ItemTags.WOOL_CARPETS)
         .add(
            Blocks.WHITE_CARPET,
            Blocks.ORANGE_CARPET,
            Blocks.MAGENTA_CARPET,
            Blocks.LIGHT_BLUE_CARPET,
            Blocks.YELLOW_CARPET,
            Blocks.LIME_CARPET,
            Blocks.PINK_CARPET,
            Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET,
            Blocks.CYAN_CARPET,
            Blocks.PURPLE_CARPET,
            Blocks.BLUE_CARPET,
            Blocks.BROWN_CARPET,
            Blocks.GREEN_CARPET,
            Blocks.RED_CARPET,
            Blocks.BLACK_CARPET
         );
      this.tag(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS)
         .add(
            Blocks.OAK_DOOR,
            Blocks.SPRUCE_DOOR,
            Blocks.BIRCH_DOOR,
            Blocks.JUNGLE_DOOR,
            Blocks.ACACIA_DOOR,
            Blocks.DARK_OAK_DOOR,
            Blocks.PALE_OAK_DOOR,
            Blocks.CRIMSON_DOOR,
            Blocks.WARPED_DOOR,
            Blocks.MANGROVE_DOOR,
            Blocks.BAMBOO_DOOR,
            Blocks.CHERRY_DOOR
         );
      this.tag(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS)
         .add(
            Blocks.OAK_STAIRS,
            Blocks.SPRUCE_STAIRS,
            Blocks.BIRCH_STAIRS,
            Blocks.JUNGLE_STAIRS,
            Blocks.ACACIA_STAIRS,
            Blocks.DARK_OAK_STAIRS,
            Blocks.PALE_OAK_STAIRS,
            Blocks.CRIMSON_STAIRS,
            Blocks.WARPED_STAIRS,
            Blocks.MANGROVE_STAIRS,
            Blocks.BAMBOO_STAIRS,
            Blocks.CHERRY_STAIRS
         );
      this.tag(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS)
         .add(
            Blocks.OAK_SLAB,
            Blocks.SPRUCE_SLAB,
            Blocks.BIRCH_SLAB,
            Blocks.JUNGLE_SLAB,
            Blocks.ACACIA_SLAB,
            Blocks.DARK_OAK_SLAB,
            Blocks.PALE_OAK_SLAB,
            Blocks.CRIMSON_SLAB,
            Blocks.WARPED_SLAB,
            Blocks.MANGROVE_SLAB,
            Blocks.BAMBOO_SLAB,
            Blocks.CHERRY_SLAB
         );
      this.tag(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES)
         .add(
            Blocks.OAK_FENCE,
            Blocks.ACACIA_FENCE,
            Blocks.DARK_OAK_FENCE,
            Blocks.PALE_OAK_FENCE,
            Blocks.SPRUCE_FENCE,
            Blocks.BIRCH_FENCE,
            Blocks.JUNGLE_FENCE,
            Blocks.CRIMSON_FENCE,
            Blocks.WARPED_FENCE,
            Blocks.MANGROVE_FENCE,
            Blocks.BAMBOO_FENCE,
            Blocks.CHERRY_FENCE
         );
      this.tag(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES)
         .add(
            Blocks.ACACIA_FENCE_GATE,
            Blocks.BIRCH_FENCE_GATE,
            Blocks.DARK_OAK_FENCE_GATE,
            Blocks.PALE_OAK_FENCE_GATE,
            Blocks.JUNGLE_FENCE_GATE,
            Blocks.OAK_FENCE_GATE,
            Blocks.SPRUCE_FENCE_GATE,
            Blocks.CRIMSON_FENCE_GATE,
            Blocks.WARPED_FENCE_GATE,
            Blocks.MANGROVE_FENCE_GATE,
            Blocks.BAMBOO_FENCE_GATE,
            Blocks.CHERRY_FENCE_GATE
         );
      this.tag(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES)
         .add(
            Blocks.OAK_PRESSURE_PLATE,
            Blocks.SPRUCE_PRESSURE_PLATE,
            Blocks.BIRCH_PRESSURE_PLATE,
            Blocks.JUNGLE_PRESSURE_PLATE,
            Blocks.ACACIA_PRESSURE_PLATE,
            Blocks.DARK_OAK_PRESSURE_PLATE,
            Blocks.PALE_OAK_PRESSURE_PLATE,
            Blocks.CRIMSON_PRESSURE_PLATE,
            Blocks.WARPED_PRESSURE_PLATE,
            Blocks.MANGROVE_PRESSURE_PLATE,
            Blocks.BAMBOO_PRESSURE_PLATE,
            Blocks.CHERRY_PRESSURE_PLATE
         );
      this.tag(BlockTags.DOORS, ItemTags.DOORS)
         .addTag(BlockTags.WOODEN_DOORS)
         .add(
            Blocks.COPPER_DOOR,
            Blocks.EXPOSED_COPPER_DOOR,
            Blocks.WEATHERED_COPPER_DOOR,
            Blocks.OXIDIZED_COPPER_DOOR,
            Blocks.WAXED_COPPER_DOOR,
            Blocks.WAXED_EXPOSED_COPPER_DOOR,
            Blocks.WAXED_WEATHERED_COPPER_DOOR,
            Blocks.WAXED_OXIDIZED_COPPER_DOOR,
            Blocks.IRON_DOOR
         );
      this.tag(BlockTags.SAPLINGS, ItemTags.SAPLINGS)
         .add(
            Blocks.OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.DARK_OAK_SAPLING,
            Blocks.PALE_OAK_SAPLING,
            Blocks.AZALEA,
            Blocks.FLOWERING_AZALEA,
            Blocks.MANGROVE_PROPAGULE,
            Blocks.CHERRY_SAPLING
         );
      this.tag(BlockTags.BAMBOO_BLOCKS, ItemTags.BAMBOO_BLOCKS).add(Blocks.BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK);
      this.tag(BlockTags.OAK_LOGS, ItemTags.OAK_LOGS).add(Blocks.OAK_LOG, Blocks.OAK_WOOD, Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_OAK_WOOD);
      this.tag(BlockTags.DARK_OAK_LOGS, ItemTags.DARK_OAK_LOGS)
         .add(Blocks.DARK_OAK_LOG, Blocks.DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_WOOD);
      this.tag(BlockTags.PALE_OAK_LOGS, ItemTags.PALE_OAK_LOGS)
         .add(Blocks.PALE_OAK_LOG, Blocks.PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_WOOD);
      this.tag(BlockTags.BIRCH_LOGS, ItemTags.BIRCH_LOGS).add(Blocks.BIRCH_LOG, Blocks.BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_WOOD);
      this.tag(BlockTags.ACACIA_LOGS, ItemTags.ACACIA_LOGS).add(Blocks.ACACIA_LOG, Blocks.ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_ACACIA_WOOD);
      this.tag(BlockTags.SPRUCE_LOGS, ItemTags.SPRUCE_LOGS).add(Blocks.SPRUCE_LOG, Blocks.SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_WOOD);
      this.tag(BlockTags.MANGROVE_LOGS, ItemTags.MANGROVE_LOGS)
         .add(Blocks.MANGROVE_LOG, Blocks.MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_WOOD);
      this.tag(BlockTags.JUNGLE_LOGS, ItemTags.JUNGLE_LOGS).add(Blocks.JUNGLE_LOG, Blocks.JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_WOOD);
      this.tag(BlockTags.CHERRY_LOGS, ItemTags.CHERRY_LOGS).add(Blocks.CHERRY_LOG, Blocks.CHERRY_WOOD, Blocks.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_WOOD);
      this.tag(BlockTags.CRIMSON_STEMS, ItemTags.CRIMSON_STEMS)
         .add(Blocks.CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM, Blocks.CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE);
      this.tag(BlockTags.WARPED_STEMS, ItemTags.WARPED_STEMS)
         .add(Blocks.WARPED_STEM, Blocks.STRIPPED_WARPED_STEM, Blocks.WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE);
      this.tag(BlockTags.WART_BLOCKS, ItemTags.WART_BLOCKS).add(Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK);
      this.tag(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN)
         .addTag(BlockTags.DARK_OAK_LOGS)
         .addTag(BlockTags.PALE_OAK_LOGS)
         .addTag(BlockTags.OAK_LOGS)
         .addTag(BlockTags.ACACIA_LOGS)
         .addTag(BlockTags.BIRCH_LOGS)
         .addTag(BlockTags.JUNGLE_LOGS)
         .addTag(BlockTags.SPRUCE_LOGS)
         .addTag(BlockTags.MANGROVE_LOGS)
         .addTag(BlockTags.CHERRY_LOGS);
      this.tag(BlockTags.LOGS, ItemTags.LOGS).addTag(BlockTags.LOGS_THAT_BURN).addTag(BlockTags.CRIMSON_STEMS).addTag(BlockTags.WARPED_STEMS);
      this.tag(BlockTags.SAND, ItemTags.SAND).add(Blocks.SAND, Blocks.RED_SAND, Blocks.SUSPICIOUS_SAND);
      this.tag(BlockTags.SMELTS_TO_GLASS, ItemTags.SMELTS_TO_GLASS).add(Blocks.SAND, Blocks.RED_SAND);
      this.tag(BlockTags.SLABS, ItemTags.SLABS)
         .addTag(BlockTags.WOODEN_SLABS)
         .add(Blocks.BAMBOO_MOSAIC_SLAB)
         .add(
            Blocks.STONE_SLAB,
            Blocks.SMOOTH_STONE_SLAB,
            Blocks.STONE_BRICK_SLAB,
            Blocks.SANDSTONE_SLAB,
            Blocks.PURPUR_SLAB,
            Blocks.QUARTZ_SLAB,
            Blocks.RED_SANDSTONE_SLAB,
            Blocks.BRICK_SLAB,
            Blocks.COBBLESTONE_SLAB,
            Blocks.NETHER_BRICK_SLAB,
            Blocks.PETRIFIED_OAK_SLAB,
            Blocks.PRISMARINE_SLAB,
            Blocks.PRISMARINE_BRICK_SLAB,
            Blocks.DARK_PRISMARINE_SLAB,
            Blocks.POLISHED_GRANITE_SLAB,
            Blocks.SMOOTH_RED_SANDSTONE_SLAB,
            Blocks.MOSSY_STONE_BRICK_SLAB,
            Blocks.POLISHED_DIORITE_SLAB,
            Blocks.MOSSY_COBBLESTONE_SLAB,
            Blocks.END_STONE_BRICK_SLAB,
            Blocks.SMOOTH_SANDSTONE_SLAB,
            Blocks.SMOOTH_QUARTZ_SLAB,
            Blocks.GRANITE_SLAB,
            Blocks.ANDESITE_SLAB,
            Blocks.RED_NETHER_BRICK_SLAB,
            Blocks.POLISHED_ANDESITE_SLAB,
            Blocks.DIORITE_SLAB,
            Blocks.CUT_SANDSTONE_SLAB,
            Blocks.CUT_RED_SANDSTONE_SLAB,
            Blocks.BLACKSTONE_SLAB,
            Blocks.POLISHED_BLACKSTONE_BRICK_SLAB,
            Blocks.POLISHED_BLACKSTONE_SLAB,
            Blocks.COBBLED_DEEPSLATE_SLAB,
            Blocks.POLISHED_DEEPSLATE_SLAB,
            Blocks.DEEPSLATE_TILE_SLAB,
            Blocks.DEEPSLATE_BRICK_SLAB,
            Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
            Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB,
            Blocks.WAXED_CUT_COPPER_SLAB,
            Blocks.OXIDIZED_CUT_COPPER_SLAB,
            Blocks.WEATHERED_CUT_COPPER_SLAB,
            Blocks.EXPOSED_CUT_COPPER_SLAB,
            Blocks.CUT_COPPER_SLAB,
            Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB,
            Blocks.MUD_BRICK_SLAB,
            Blocks.TUFF_SLAB,
            Blocks.POLISHED_TUFF_SLAB,
            Blocks.TUFF_BRICK_SLAB,
            Blocks.RESIN_BRICK_SLAB
         );
      this.tag(BlockTags.WALLS, ItemTags.WALLS)
         .add(
            Blocks.COBBLESTONE_WALL,
            Blocks.MOSSY_COBBLESTONE_WALL,
            Blocks.BRICK_WALL,
            Blocks.PRISMARINE_WALL,
            Blocks.RED_SANDSTONE_WALL,
            Blocks.MOSSY_STONE_BRICK_WALL,
            Blocks.GRANITE_WALL,
            Blocks.STONE_BRICK_WALL,
            Blocks.NETHER_BRICK_WALL,
            Blocks.ANDESITE_WALL,
            Blocks.RED_NETHER_BRICK_WALL,
            Blocks.SANDSTONE_WALL,
            Blocks.END_STONE_BRICK_WALL,
            Blocks.DIORITE_WALL,
            Blocks.BLACKSTONE_WALL,
            Blocks.POLISHED_BLACKSTONE_BRICK_WALL,
            Blocks.POLISHED_BLACKSTONE_WALL,
            Blocks.COBBLED_DEEPSLATE_WALL,
            Blocks.POLISHED_DEEPSLATE_WALL,
            Blocks.DEEPSLATE_TILE_WALL,
            Blocks.DEEPSLATE_BRICK_WALL,
            Blocks.MUD_BRICK_WALL,
            Blocks.TUFF_WALL,
            Blocks.POLISHED_TUFF_WALL,
            Blocks.TUFF_BRICK_WALL,
            Blocks.RESIN_BRICK_WALL
         );
      this.tag(BlockTags.STAIRS, ItemTags.STAIRS)
         .addTag(BlockTags.WOODEN_STAIRS)
         .add(Blocks.BAMBOO_MOSAIC_STAIRS)
         .add(
            Blocks.COBBLESTONE_STAIRS,
            Blocks.SANDSTONE_STAIRS,
            Blocks.NETHER_BRICK_STAIRS,
            Blocks.STONE_BRICK_STAIRS,
            Blocks.BRICK_STAIRS,
            Blocks.PURPUR_STAIRS,
            Blocks.QUARTZ_STAIRS,
            Blocks.RED_SANDSTONE_STAIRS,
            Blocks.PRISMARINE_BRICK_STAIRS,
            Blocks.PRISMARINE_STAIRS,
            Blocks.DARK_PRISMARINE_STAIRS,
            Blocks.POLISHED_GRANITE_STAIRS,
            Blocks.SMOOTH_RED_SANDSTONE_STAIRS,
            Blocks.MOSSY_STONE_BRICK_STAIRS,
            Blocks.POLISHED_DIORITE_STAIRS,
            Blocks.MOSSY_COBBLESTONE_STAIRS,
            Blocks.END_STONE_BRICK_STAIRS,
            Blocks.STONE_STAIRS,
            Blocks.SMOOTH_SANDSTONE_STAIRS,
            Blocks.SMOOTH_QUARTZ_STAIRS,
            Blocks.GRANITE_STAIRS,
            Blocks.ANDESITE_STAIRS,
            Blocks.RED_NETHER_BRICK_STAIRS,
            Blocks.POLISHED_ANDESITE_STAIRS,
            Blocks.DIORITE_STAIRS,
            Blocks.BLACKSTONE_STAIRS,
            Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Blocks.POLISHED_BLACKSTONE_STAIRS,
            Blocks.COBBLED_DEEPSLATE_STAIRS,
            Blocks.POLISHED_DEEPSLATE_STAIRS,
            Blocks.DEEPSLATE_TILE_STAIRS,
            Blocks.DEEPSLATE_BRICK_STAIRS,
            Blocks.OXIDIZED_CUT_COPPER_STAIRS,
            Blocks.WEATHERED_CUT_COPPER_STAIRS,
            Blocks.EXPOSED_CUT_COPPER_STAIRS,
            Blocks.CUT_COPPER_STAIRS,
            Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
            Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS,
            Blocks.WAXED_CUT_COPPER_STAIRS,
            Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS,
            Blocks.MUD_BRICK_STAIRS,
            Blocks.TUFF_STAIRS,
            Blocks.POLISHED_TUFF_STAIRS,
            Blocks.TUFF_BRICK_STAIRS,
            Blocks.RESIN_BRICK_STAIRS
         );
      this.tag(BlockTags.ANVIL, ItemTags.ANVIL).add(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL);
      this.tag(BlockTags.RAILS, ItemTags.RAILS).add(Blocks.RAIL, Blocks.POWERED_RAIL, Blocks.DETECTOR_RAIL, Blocks.ACTIVATOR_RAIL);
      this.tag(BlockTags.LEAVES, ItemTags.LEAVES)
         .add(
            Blocks.JUNGLE_LEAVES,
            Blocks.OAK_LEAVES,
            Blocks.SPRUCE_LEAVES,
            Blocks.PALE_OAK_LEAVES,
            Blocks.DARK_OAK_LEAVES,
            Blocks.ACACIA_LEAVES,
            Blocks.BIRCH_LEAVES,
            Blocks.AZALEA_LEAVES,
            Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.MANGROVE_LEAVES,
            Blocks.CHERRY_LEAVES
         );
      this.tag(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS)
         .add(
            Blocks.ACACIA_TRAPDOOR,
            Blocks.BIRCH_TRAPDOOR,
            Blocks.DARK_OAK_TRAPDOOR,
            Blocks.PALE_OAK_TRAPDOOR,
            Blocks.JUNGLE_TRAPDOOR,
            Blocks.OAK_TRAPDOOR,
            Blocks.SPRUCE_TRAPDOOR,
            Blocks.CRIMSON_TRAPDOOR,
            Blocks.WARPED_TRAPDOOR,
            Blocks.MANGROVE_TRAPDOOR,
            Blocks.BAMBOO_TRAPDOOR,
            Blocks.CHERRY_TRAPDOOR
         );
      this.tag(BlockTags.TRAPDOORS, ItemTags.TRAPDOORS)
         .addTag(BlockTags.WOODEN_TRAPDOORS)
         .add(
            Blocks.IRON_TRAPDOOR,
            Blocks.COPPER_TRAPDOOR,
            Blocks.EXPOSED_COPPER_TRAPDOOR,
            Blocks.WEATHERED_COPPER_TRAPDOOR,
            Blocks.OXIDIZED_COPPER_TRAPDOOR,
            Blocks.WAXED_COPPER_TRAPDOOR,
            Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR,
            Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR,
            Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR
         );
      this.tag(BlockTags.SMALL_FLOWERS, ItemTags.SMALL_FLOWERS).add(ablock).add(Blocks.CLOSED_EYEBLOSSOM);
      this.tag(BlockTags.FLOWERS, ItemTags.FLOWERS).addTag(BlockTags.SMALL_FLOWERS).add(ablock1);
      this.tag(BlockTags.BEDS, ItemTags.BEDS)
         .add(
            Blocks.RED_BED,
            Blocks.BLACK_BED,
            Blocks.BLUE_BED,
            Blocks.BROWN_BED,
            Blocks.CYAN_BED,
            Blocks.GRAY_BED,
            Blocks.GREEN_BED,
            Blocks.LIGHT_BLUE_BED,
            Blocks.LIGHT_GRAY_BED,
            Blocks.LIME_BED,
            Blocks.MAGENTA_BED,
            Blocks.ORANGE_BED,
            Blocks.PINK_BED,
            Blocks.PURPLE_BED,
            Blocks.WHITE_BED,
            Blocks.YELLOW_BED
         );
      this.tag(BlockTags.FENCES, ItemTags.FENCES).addTag(BlockTags.WOODEN_FENCES).add(Blocks.NETHER_BRICK_FENCE);
      this.tag(BlockTags.SOUL_FIRE_BASE_BLOCKS, ItemTags.SOUL_FIRE_BASE_BLOCKS).add(Blocks.SOUL_SAND, Blocks.SOUL_SOIL);
      this.tag(BlockTags.CANDLES, ItemTags.CANDLES)
         .add(
            Blocks.CANDLE,
            Blocks.WHITE_CANDLE,
            Blocks.ORANGE_CANDLE,
            Blocks.MAGENTA_CANDLE,
            Blocks.LIGHT_BLUE_CANDLE,
            Blocks.YELLOW_CANDLE,
            Blocks.LIME_CANDLE,
            Blocks.PINK_CANDLE,
            Blocks.GRAY_CANDLE,
            Blocks.LIGHT_GRAY_CANDLE,
            Blocks.CYAN_CANDLE,
            Blocks.PURPLE_CANDLE,
            Blocks.BLUE_CANDLE,
            Blocks.BROWN_CANDLE,
            Blocks.GREEN_CANDLE,
            Blocks.RED_CANDLE,
            Blocks.BLACK_CANDLE
         );
      this.tag(BlockTags.DAMPENS_VIBRATIONS, ItemTags.DAMPENS_VIBRATIONS).addTag(BlockTags.WOOL).addTag(BlockTags.WOOL_CARPETS);
      this.tag(BlockTags.GOLD_ORES, ItemTags.GOLD_ORES).add(Blocks.GOLD_ORE, Blocks.NETHER_GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
      this.tag(BlockTags.IRON_ORES, ItemTags.IRON_ORES).add(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
      this.tag(BlockTags.DIAMOND_ORES, ItemTags.DIAMOND_ORES).add(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
      this.tag(BlockTags.REDSTONE_ORES, ItemTags.REDSTONE_ORES).add(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
      this.tag(BlockTags.LAPIS_ORES, ItemTags.LAPIS_ORES).add(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
      this.tag(BlockTags.COAL_ORES, ItemTags.COAL_ORES).add(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
      this.tag(BlockTags.EMERALD_ORES, ItemTags.EMERALD_ORES).add(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
      this.tag(BlockTags.COPPER_ORES, ItemTags.COPPER_ORES).add(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
      this.tag(BlockTags.DIRT, ItemTags.DIRT)
         .add(
            Blocks.DIRT,
            Blocks.GRASS_BLOCK,
            Blocks.PODZOL,
            Blocks.COARSE_DIRT,
            Blocks.MYCELIUM,
            Blocks.ROOTED_DIRT,
            Blocks.MOSS_BLOCK,
            Blocks.PALE_MOSS_BLOCK,
            Blocks.MUD,
            Blocks.MUDDY_MANGROVE_ROOTS
         );
      this.tag(BlockTags.TERRACOTTA, ItemTags.TERRACOTTA)
         .add(
            Blocks.TERRACOTTA,
            Blocks.WHITE_TERRACOTTA,
            Blocks.ORANGE_TERRACOTTA,
            Blocks.MAGENTA_TERRACOTTA,
            Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA,
            Blocks.LIME_TERRACOTTA,
            Blocks.PINK_TERRACOTTA,
            Blocks.GRAY_TERRACOTTA,
            Blocks.LIGHT_GRAY_TERRACOTTA,
            Blocks.CYAN_TERRACOTTA,
            Blocks.PURPLE_TERRACOTTA,
            Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA,
            Blocks.GREEN_TERRACOTTA,
            Blocks.RED_TERRACOTTA,
            Blocks.BLACK_TERRACOTTA
         );
      this.tag(BlockTags.COMPLETES_FIND_TREE_TUTORIAL, ItemTags.COMPLETES_FIND_TREE_TUTORIAL)
         .addTag(BlockTags.LOGS)
         .addTag(BlockTags.LEAVES)
         .addTag(BlockTags.WART_BLOCKS);
      this.tag(BlockTags.SHULKER_BOXES, ItemTags.SHULKER_BOXES)
         .add(
            Blocks.SHULKER_BOX,
            Blocks.BLACK_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX,
            Blocks.BROWN_SHULKER_BOX,
            Blocks.CYAN_SHULKER_BOX,
            Blocks.GRAY_SHULKER_BOX,
            Blocks.GREEN_SHULKER_BOX,
            Blocks.LIGHT_BLUE_SHULKER_BOX,
            Blocks.LIGHT_GRAY_SHULKER_BOX,
            Blocks.LIME_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX,
            Blocks.ORANGE_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX,
            Blocks.PURPLE_SHULKER_BOX,
            Blocks.RED_SHULKER_BOX,
            Blocks.WHITE_SHULKER_BOX,
            Blocks.YELLOW_SHULKER_BOX
         );
      this.tag(BlockTags.COPPER_CHESTS, ItemTags.COPPER_CHESTS)
         .add(Blocks.COPPER_CHEST)
         .add(Blocks.EXPOSED_COPPER_CHEST)
         .add(Blocks.WEATHERED_COPPER_CHEST)
         .add(Blocks.OXIDIZED_COPPER_CHEST)
         .add(Blocks.WAXED_COPPER_CHEST)
         .add(Blocks.WAXED_EXPOSED_COPPER_CHEST)
         .add(Blocks.WAXED_WEATHERED_COPPER_CHEST)
         .add(Blocks.WAXED_OXIDIZED_COPPER_CHEST);
      this.tag(BlockTags.LIGHTNING_RODS, ItemTags.LIGHTNING_RODS)
         .add(Blocks.LIGHTNING_ROD)
         .add(Blocks.EXPOSED_LIGHTNING_ROD)
         .add(Blocks.WEATHERED_LIGHTNING_ROD)
         .add(Blocks.OXIDIZED_LIGHTNING_ROD)
         .add(Blocks.WAXED_LIGHTNING_ROD)
         .add(Blocks.WAXED_EXPOSED_LIGHTNING_ROD)
         .add(Blocks.WAXED_WEATHERED_LIGHTNING_ROD)
         .add(Blocks.WAXED_OXIDIZED_LIGHTNING_ROD);
      this.tag(BlockTags.COPPER_GOLEM_STATUES, ItemTags.COPPER_GOLEM_STATUES)
         .add(Blocks.COPPER_GOLEM_STATUE)
         .add(Blocks.EXPOSED_COPPER_GOLEM_STATUE)
         .add(Blocks.WEATHERED_COPPER_GOLEM_STATUE)
         .add(Blocks.OXIDIZED_COPPER_GOLEM_STATUE)
         .add(Blocks.WAXED_COPPER_GOLEM_STATUE)
         .add(Blocks.WAXED_EXPOSED_COPPER_GOLEM_STATUE)
         .add(Blocks.WAXED_WEATHERED_COPPER_GOLEM_STATUE)
         .add(Blocks.WAXED_OXIDIZED_COPPER_GOLEM_STATUE);
      this.tag(BlockTags.COPPER, ItemTags.COPPER)
         .add(Blocks.COPPER_BLOCK)
         .add(Blocks.EXPOSED_COPPER)
         .add(Blocks.WEATHERED_COPPER)
         .add(Blocks.OXIDIZED_COPPER)
         .add(Blocks.WAXED_COPPER_BLOCK)
         .add(Blocks.WAXED_EXPOSED_COPPER)
         .add(Blocks.WAXED_WEATHERED_COPPER)
         .add(Blocks.WAXED_OXIDIZED_COPPER);
      this.tag(BlockTags.CHAINS, ItemTags.CHAINS).add(Blocks.IRON_CHAIN).addAll(Blocks.COPPER_CHAIN.asList());
      this.tag(BlockTags.WOODEN_SHELVES, ItemTags.WOODEN_SHELVES)
         .add(
            Blocks.ACACIA_SHELF,
            Blocks.BAMBOO_SHELF,
            Blocks.BIRCH_SHELF,
            Blocks.CHERRY_SHELF,
            Blocks.CRIMSON_SHELF,
            Blocks.DARK_OAK_SHELF,
            Blocks.JUNGLE_SHELF,
            Blocks.MANGROVE_SHELF,
            Blocks.OAK_SHELF,
            Blocks.PALE_OAK_SHELF,
            Blocks.SPRUCE_SHELF,
            Blocks.WARPED_SHELF
         );
      this.tag(BlockTags.LANTERNS, ItemTags.LANTERNS).add(Blocks.LANTERN, Blocks.SOUL_LANTERN).addAll(Blocks.COPPER_LANTERN.asList());
      this.tag(BlockTags.BARS, ItemTags.BARS).add(Blocks.IRON_BARS).addAll(Blocks.COPPER_BARS.asList());
      this.tag(BlockTags.STANDING_SIGNS, ItemTags.SIGNS)
         .add(
            Blocks.OAK_SIGN,
            Blocks.SPRUCE_SIGN,
            Blocks.BIRCH_SIGN,
            Blocks.ACACIA_SIGN,
            Blocks.JUNGLE_SIGN,
            Blocks.DARK_OAK_SIGN,
            Blocks.PALE_OAK_SIGN,
            Blocks.CRIMSON_SIGN,
            Blocks.WARPED_SIGN,
            Blocks.MANGROVE_SIGN,
            Blocks.BAMBOO_SIGN,
            Blocks.CHERRY_SIGN
         );
      this.tag(BlockTags.CEILING_HANGING_SIGNS, ItemTags.HANGING_SIGNS)
         .add(
            Blocks.OAK_HANGING_SIGN,
            Blocks.SPRUCE_HANGING_SIGN,
            Blocks.BIRCH_HANGING_SIGN,
            Blocks.ACACIA_HANGING_SIGN,
            Blocks.CHERRY_HANGING_SIGN,
            Blocks.JUNGLE_HANGING_SIGN,
            Blocks.DARK_OAK_HANGING_SIGN,
            Blocks.PALE_OAK_HANGING_SIGN,
            Blocks.CRIMSON_HANGING_SIGN,
            Blocks.WARPED_HANGING_SIGN,
            Blocks.MANGROVE_HANGING_SIGN,
            Blocks.BAMBOO_HANGING_SIGN
         );
      this.tag(BlockTags.BEE_ATTRACTIVE, ItemTags.BEE_FOOD).add(ablock).add(ablock1);
   }

   protected abstract TagAppender<Block, Block> tag(TagKey<Block> var1, TagKey<Item> var2);
}
