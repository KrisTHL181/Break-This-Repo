package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.references.BlockIds;
import net.minecraft.references.BlockItemId;
import net.minecraft.references.BlockItemIds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.WeatheringCopperCollection;

public class VanillaBlockTagsProvider extends TagsProvider<Block> {
    public VanillaBlockTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BLOCK, lookupProvider);
    }

    protected BlockItemTagAppender<Block> tag(final TagKey<Block> tag) {
        return new BlockItemTagAppender<Block>(super.tag(tag)) {
            @Override
            protected ResourceKey<Block> convertElement(final BlockItemId element) {
                return element.block();
            }
        };
    }

    @Override
    protected void addTags(final HolderLookup.Provider registries) {
        new VanillaBlockItemTagsProvider(tagId -> BlockItemTagsProvider.wrapForBlocks(this.tag(tagId.block()))).run();
        this.tag(BlockTags.MOB_INTERACTABLE_DOORS).addTag(BlockTags.WOODEN_DOORS).addAll(toIds(BlockItemIds.COPPER_DOOR));
        this.tag(BlockTags.OVERWORLD_NATURAL_LOGS)
            .add(new BlockItemId[]{BlockItemIds.ACACIA_LOG})
            .add(new BlockItemId[]{BlockItemIds.BIRCH_LOG})
            .add(new BlockItemId[]{BlockItemIds.OAK_LOG})
            .add(new BlockItemId[]{BlockItemIds.JUNGLE_LOG})
            .add(new BlockItemId[]{BlockItemIds.SPRUCE_LOG})
            .add(new BlockItemId[]{BlockItemIds.DARK_OAK_LOG})
            .add(new BlockItemId[]{BlockItemIds.PALE_OAK_LOG})
            .add(new BlockItemId[]{BlockItemIds.MANGROVE_LOG})
            .add(new BlockItemId[]{BlockItemIds.CHERRY_LOG});
        this.tag(BlockTags.ENDERMAN_HOLDABLE)
            .addTag(BlockTags.SMALL_FLOWERS)
            .addTag(BlockTags.DIRT)
            .addTag(BlockTags.MUD)
            .addTag(BlockTags.MOSS_BLOCKS)
            .addTag(BlockTags.GRASS_BLOCKS)
            .add(
                BlockItemIds.SAND,
                BlockItemIds.RED_SAND,
                BlockItemIds.GRAVEL,
                BlockItemIds.BROWN_MUSHROOM,
                BlockItemIds.RED_MUSHROOM,
                BlockItemIds.TNT,
                BlockItemIds.CACTUS,
                BlockItemIds.CLAY,
                BlockItemIds.PUMPKIN,
                BlockItemIds.CARVED_PUMPKIN,
                BlockItemIds.MELON,
                BlockItemIds.CRIMSON_FUNGUS,
                BlockItemIds.CRIMSON_NYLIUM,
                BlockItemIds.CRIMSON_ROOTS,
                BlockItemIds.WARPED_FUNGUS,
                BlockItemIds.WARPED_NYLIUM,
                BlockItemIds.WARPED_ROOTS,
                BlockItemIds.CACTUS_FLOWER
            );
        this.tag(BlockTags.FLOWER_POTS)
            .add(new BlockItemId[]{BlockItemIds.FLOWER_POT})
            .add(
                BlockIds.POTTED_OPEN_EYEBLOSSOM,
                BlockIds.POTTED_CLOSED_EYEBLOSSOM,
                BlockIds.POTTED_POPPY,
                BlockIds.POTTED_BLUE_ORCHID,
                BlockIds.POTTED_ALLIUM,
                BlockIds.POTTED_AZURE_BLUET,
                BlockIds.POTTED_RED_TULIP,
                BlockIds.POTTED_ORANGE_TULIP,
                BlockIds.POTTED_WHITE_TULIP,
                BlockIds.POTTED_PINK_TULIP,
                BlockIds.POTTED_OXEYE_DAISY,
                BlockIds.POTTED_DANDELION,
                BlockIds.POTTED_OAK_SAPLING,
                BlockIds.POTTED_SPRUCE_SAPLING,
                BlockIds.POTTED_BIRCH_SAPLING,
                BlockIds.POTTED_JUNGLE_SAPLING,
                BlockIds.POTTED_ACACIA_SAPLING,
                BlockIds.POTTED_DARK_OAK_SAPLING,
                BlockIds.POTTED_PALE_OAK_SAPLING,
                BlockIds.POTTED_RED_MUSHROOM,
                BlockIds.POTTED_BROWN_MUSHROOM,
                BlockIds.POTTED_DEAD_BUSH,
                BlockIds.POTTED_FERN,
                BlockIds.POTTED_CACTUS,
                BlockIds.POTTED_CORNFLOWER,
                BlockIds.POTTED_LILY_OF_THE_VALLEY,
                BlockIds.POTTED_WITHER_ROSE,
                BlockIds.POTTED_BAMBOO,
                BlockIds.POTTED_CRIMSON_FUNGUS,
                BlockIds.POTTED_WARPED_FUNGUS,
                BlockIds.POTTED_CRIMSON_ROOTS,
                BlockIds.POTTED_WARPED_ROOTS,
                BlockIds.POTTED_AZALEA_BUSH,
                BlockIds.POTTED_FLOWERING_AZALEA_BUSH,
                BlockIds.POTTED_MANGROVE_PROPAGULE,
                BlockIds.POTTED_CHERRY_SAPLING,
                BlockIds.POTTED_TORCHFLOWER,
                BlockIds.POTTED_GOLDEN_DANDELION
            );
        this.tag(BlockTags.BANNERS).addAll(toIds(BlockItemIds.BANNER)).addAll(BlockIds.WALL_BANNER);
        this.tag(BlockTags.STONE_PRESSURE_PLATES).add(BlockItemIds.STONE_PRESSURE_PLATE, BlockItemIds.POLISHED_BLACKSTONE_PRESSURE_PLATE);
        this.tag(BlockTags.PRESSURE_PLATES)
            .add(BlockItemIds.LIGHT_WEIGHTED_PRESSURE_PLATE, BlockItemIds.HEAVY_WEIGHTED_PRESSURE_PLATE)
            .addTag(BlockTags.WOODEN_PRESSURE_PLATES)
            .addTag(BlockTags.STONE_PRESSURE_PLATES);
        this.tag(BlockTags.CORAL_PLANTS)
            .add(BlockItemIds.TUBE_CORAL, BlockItemIds.BRAIN_CORAL, BlockItemIds.BUBBLE_CORAL, BlockItemIds.FIRE_CORAL, BlockItemIds.HORN_CORAL);
        this.tag(BlockTags.CORALS)
            .addTag(BlockTags.CORAL_PLANTS)
            .add(
                BlockItemIds.TUBE_CORAL_FAN,
                BlockItemIds.BRAIN_CORAL_FAN,
                BlockItemIds.BUBBLE_CORAL_FAN,
                BlockItemIds.FIRE_CORAL_FAN,
                BlockItemIds.HORN_CORAL_FAN
            );
        this.tag(BlockTags.WALL_CORALS)
            .add(
                BlockIds.TUBE_CORAL_WALL_FAN,
                BlockIds.BRAIN_CORAL_WALL_FAN,
                BlockIds.BUBBLE_CORAL_WALL_FAN,
                BlockIds.FIRE_CORAL_WALL_FAN,
                BlockIds.HORN_CORAL_WALL_FAN
            );
        this.tag(BlockTags.CORAL_BLOCKS)
            .add(
                BlockItemIds.TUBE_CORAL_BLOCK,
                BlockItemIds.BRAIN_CORAL_BLOCK,
                BlockItemIds.BUBBLE_CORAL_BLOCK,
                BlockItemIds.FIRE_CORAL_BLOCK,
                BlockItemIds.HORN_CORAL_BLOCK
            );
        this.tag(BlockTags.ICE).add(BlockItemIds.ICE, BlockItemIds.PACKED_ICE, BlockItemIds.BLUE_ICE).add(BlockIds.FROSTED_ICE);
        this.tag(BlockTags.VALID_SPAWN).add(BlockItemIds.GRASS_BLOCK, BlockItemIds.PODZOL);
        this.tag(BlockTags.IMPERMEABLE).addAll(toIds(BlockItemIds.STAINED_GLASS)).add(BlockItemIds.GLASS, BlockItemIds.TINTED_GLASS, BlockItemIds.BARRIER);
        this.tag(BlockTags.UNDERWATER_BONEMEALS).add(new BlockItemId[]{BlockItemIds.SEAGRASS}).addTag(BlockTags.CORALS).addTag(BlockTags.WALL_CORALS);
        this.tag(BlockTags.WALL_SIGNS)
            .add(
                BlockIds.OAK_WALL_SIGN,
                BlockIds.SPRUCE_WALL_SIGN,
                BlockIds.BIRCH_WALL_SIGN,
                BlockIds.ACACIA_WALL_SIGN,
                BlockIds.JUNGLE_WALL_SIGN,
                BlockIds.DARK_OAK_WALL_SIGN,
                BlockIds.PALE_OAK_WALL_SIGN,
                BlockIds.CRIMSON_WALL_SIGN,
                BlockIds.WARPED_WALL_SIGN,
                BlockIds.MANGROVE_WALL_SIGN,
                BlockIds.BAMBOO_WALL_SIGN,
                BlockIds.CHERRY_WALL_SIGN
            );
        this.tag(BlockTags.SIGNS).addTag(BlockTags.STANDING_SIGNS).addTag(BlockTags.WALL_SIGNS);
        this.tag(BlockTags.WALL_HANGING_SIGNS)
            .add(
                BlockIds.OAK_WALL_HANGING_SIGN,
                BlockIds.SPRUCE_WALL_HANGING_SIGN,
                BlockIds.BIRCH_WALL_HANGING_SIGN,
                BlockIds.ACACIA_WALL_HANGING_SIGN,
                BlockIds.CHERRY_WALL_HANGING_SIGN,
                BlockIds.JUNGLE_WALL_HANGING_SIGN,
                BlockIds.DARK_OAK_WALL_HANGING_SIGN,
                BlockIds.PALE_OAK_WALL_HANGING_SIGN,
                BlockIds.CRIMSON_WALL_HANGING_SIGN,
                BlockIds.WARPED_WALL_HANGING_SIGN,
                BlockIds.MANGROVE_WALL_HANGING_SIGN,
                BlockIds.BAMBOO_WALL_HANGING_SIGN
            );
        this.tag(BlockTags.ALL_HANGING_SIGNS).addTag(BlockTags.CEILING_HANGING_SIGNS).addTag(BlockTags.WALL_HANGING_SIGNS);
        this.tag(BlockTags.ALL_SIGNS).addTag(BlockTags.SIGNS).addTag(BlockTags.ALL_HANGING_SIGNS);
        this.tag(BlockTags.DRAGON_IMMUNE)
            .add(BlockItemIds.BARRIER, BlockItemIds.BEDROCK)
            .add(BlockIds.END_PORTAL)
            .add(new BlockItemId[]{BlockItemIds.END_PORTAL_FRAME})
            .add(BlockIds.END_GATEWAY)
            .add(
                BlockItemIds.COMMAND_BLOCK,
                BlockItemIds.REPEATING_COMMAND_BLOCK,
                BlockItemIds.CHAIN_COMMAND_BLOCK,
                BlockItemIds.STRUCTURE_BLOCK,
                BlockItemIds.JIGSAW
            )
            .add(BlockIds.MOVING_PISTON)
            .add(
                BlockItemIds.OBSIDIAN,
                BlockItemIds.CRYING_OBSIDIAN,
                BlockItemIds.END_STONE,
                BlockItemIds.IRON_BARS,
                BlockItemIds.RESPAWN_ANCHOR,
                BlockItemIds.REINFORCED_DEEPSLATE,
                BlockItemIds.TEST_BLOCK,
                BlockItemIds.TEST_INSTANCE_BLOCK
            );
        this.tag(BlockTags.DRAGON_TRANSPARENT).add(new BlockItemId[]{BlockItemIds.LIGHT}).addTag(BlockTags.FIRE);
        this.tag(BlockTags.WITHER_IMMUNE)
            .add(BlockItemIds.BARRIER, BlockItemIds.BEDROCK)
            .add(BlockIds.END_PORTAL)
            .add(new BlockItemId[]{BlockItemIds.END_PORTAL_FRAME})
            .add(BlockIds.END_GATEWAY)
            .add(
                BlockItemIds.COMMAND_BLOCK,
                BlockItemIds.REPEATING_COMMAND_BLOCK,
                BlockItemIds.CHAIN_COMMAND_BLOCK,
                BlockItemIds.STRUCTURE_BLOCK,
                BlockItemIds.JIGSAW
            )
            .add(BlockIds.MOVING_PISTON)
            .add(BlockItemIds.LIGHT, BlockItemIds.REINFORCED_DEEPSLATE, BlockItemIds.TEST_BLOCK, BlockItemIds.TEST_INSTANCE_BLOCK);
        this.tag(BlockTags.WITHER_SUMMON_BASE_BLOCKS).add(BlockItemIds.SOUL_SAND, BlockItemIds.SOUL_SOIL);
        this.tag(BlockTags.BEEHIVES).add(BlockItemIds.BEE_NEST, BlockItemIds.BEEHIVE);
        this.tag(BlockTags.CROPS)
            .add(
                BlockItemIds.BEETROOT_CROP,
                BlockItemIds.CARROT_CROP,
                BlockItemIds.POTATO_CROP,
                BlockItemIds.WHEAT_CROP,
                BlockItemIds.MELON_CROP,
                BlockItemIds.PUMPKIN_CROP,
                BlockItemIds.TORCHFLOWER_CROP,
                BlockItemIds.PITCHER_CROP
            );
        this.tag(BlockTags.BEE_GROWABLES)
            .addTag(BlockTags.CROPS)
            .add(new BlockItemId[]{BlockItemIds.SWEET_BERRY_CROP})
            .add(new BlockItemId[]{BlockItemIds.GLOW_BERRY_CROP})
            .add(BlockIds.CAVE_VINES_PLANT);
        this.tag(BlockTags.PORTALS).add(BlockIds.NETHER_PORTAL, BlockIds.END_PORTAL, BlockIds.END_GATEWAY);
        this.tag(BlockTags.FIRE).add(BlockIds.FIRE, BlockIds.SOUL_FIRE);
        this.tag(BlockTags.NYLIUM).add(BlockItemIds.CRIMSON_NYLIUM, BlockItemIds.WARPED_NYLIUM);
        this.tag(BlockTags.BEACON_BASE_BLOCKS)
            .add(BlockItemIds.NETHERITE_BLOCK, BlockItemIds.EMERALD_BLOCK, BlockItemIds.DIAMOND_BLOCK, BlockItemIds.GOLD_BLOCK, BlockItemIds.IRON_BLOCK);
        this.tag(BlockTags.SOUL_SPEED_BLOCKS).add(BlockItemIds.SOUL_SAND, BlockItemIds.SOUL_SOIL);
        this.tag(BlockTags.WALL_POST_OVERRIDE)
            .add(BlockItemIds.TORCH, BlockItemIds.SOUL_TORCH, BlockItemIds.REDSTONE_TORCH, BlockItemIds.COPPER_TORCH, BlockItemIds.TRIPWIRE)
            .addTag(BlockTags.SIGNS)
            .addTag(BlockTags.BANNERS)
            .addTag(BlockTags.PRESSURE_PLATES)
            .add(new BlockItemId[]{BlockItemIds.CACTUS_FLOWER});
        this.tag(BlockTags.CLIMBABLE)
            .add(BlockItemIds.LADDER, BlockItemIds.VINE, BlockItemIds.SCAFFOLDING, BlockItemIds.WEEPING_VINES)
            .add(BlockIds.WEEPING_VINES_PLANT)
            .add(new BlockItemId[]{BlockItemIds.TWISTING_VINES})
            .add(BlockIds.TWISTING_VINES_PLANT)
            .add(new BlockItemId[]{BlockItemIds.GLOW_BERRY_CROP})
            .add(BlockIds.CAVE_VINES_PLANT);
        this.tag(BlockTags.FALL_DAMAGE_RESETTING).addTag(BlockTags.CLIMBABLE).add(BlockItemIds.SWEET_BERRY_CROP, BlockItemIds.COBWEB);
        this.tag(BlockTags.PIGLIN_REPELLENTS)
            .add(BlockIds.SOUL_FIRE)
            .add(new BlockItemId[]{BlockItemIds.SOUL_TORCH})
            .add(new BlockItemId[]{BlockItemIds.SOUL_LANTERN})
            .add(BlockIds.SOUL_WALL_TORCH)
            .add(new BlockItemId[]{BlockItemIds.SOUL_CAMPFIRE});
        this.tag(BlockTags.HOGLIN_REPELLENTS)
            .add(new BlockItemId[]{BlockItemIds.WARPED_FUNGUS})
            .add(BlockIds.POTTED_WARPED_FUNGUS)
            .add(BlockIds.NETHER_PORTAL)
            .add(new BlockItemId[]{BlockItemIds.RESPAWN_ANCHOR});
        this.tag(BlockTags.STRIDER_WARM_BLOCKS).add(BlockIds.LAVA);
        this.tag(BlockTags.CAMPFIRES).add(BlockItemIds.CAMPFIRE, BlockItemIds.SOUL_CAMPFIRE);
        this.tag(BlockTags.GUARDED_BY_PIGLINS)
            .addTag(BlockTags.COPPER_CHESTS)
            .add(
                BlockItemIds.GOLD_BLOCK,
                BlockItemIds.BARREL,
                BlockItemIds.CHEST,
                BlockItemIds.ENDER_CHEST,
                BlockItemIds.GILDED_BLACKSTONE,
                BlockItemIds.TRAPPED_CHEST,
                BlockItemIds.RAW_GOLD_BLOCK
            )
            .addTag(BlockTags.SHULKER_BOXES)
            .addTag(BlockTags.GOLD_ORES);
        this.tag(BlockTags.PREVENT_MOB_SPAWNING_INSIDE).addTag(BlockTags.RAILS);
        this.tag(BlockTags.UNSTABLE_BOTTOM_CENTER).addTag(BlockTags.FENCE_GATES);
        this.tag(BlockTags.EDIBLE_FOR_SHEEP)
            .add(new BlockItemId[]{BlockItemIds.SHORT_GRASS})
            .add(new BlockItemId[]{BlockItemIds.SHORT_DRY_GRASS})
            .add(new BlockItemId[]{BlockItemIds.TALL_DRY_GRASS})
            .add(new BlockItemId[]{BlockItemIds.FERN});
        this.tag(BlockTags.CAN_GLIDE_THROUGH)
            .add(new BlockItemId[]{BlockItemIds.VINE})
            .add(new BlockItemId[]{BlockItemIds.TWISTING_VINES})
            .add(BlockIds.TWISTING_VINES_PLANT)
            .add(new BlockItemId[]{BlockItemIds.WEEPING_VINES})
            .add(BlockIds.WEEPING_VINES_PLANT)
            .addTag(BlockTags.CAVE_VINES);
        this.tag(BlockTags.CAUSES_PERIODIC_GEYSER_ERUPTIONS).add(new BlockItemId[]{BlockItemIds.MAGMA_BLOCK});
        this.tag(BlockTags.CAUSES_CONTINUOUS_GEYSER_ERUPTIONS).add(BlockIds.LAVA);
        this.tag(BlockTags.INFINIBURN_OVERWORLD).add(BlockItemIds.NETHERRACK, BlockItemIds.MAGMA_BLOCK);
        this.tag(BlockTags.INFINIBURN_NETHER).addTag(BlockTags.INFINIBURN_OVERWORLD);
        this.tag(BlockTags.INFINIBURN_END).addTag(BlockTags.INFINIBURN_OVERWORLD).add(new BlockItemId[]{BlockItemIds.BEDROCK});
        this.tag(BlockTags.STONE_ORE_REPLACEABLES)
            .add(new BlockItemId[]{BlockItemIds.STONE})
            .add(new BlockItemId[]{BlockItemIds.GRANITE})
            .add(new BlockItemId[]{BlockItemIds.DIORITE})
            .add(new BlockItemId[]{BlockItemIds.ANDESITE});
        this.tag(BlockTags.DEEPSLATE_ORE_REPLACEABLES).add(new BlockItemId[]{BlockItemIds.DEEPSLATE}).add(new BlockItemId[]{BlockItemIds.TUFF});
        this.tag(BlockTags.SUBSTRATE_OVERWORLD).addTag(BlockTags.DIRT).addTag(BlockTags.MUD).addTag(BlockTags.MOSS_BLOCKS).addTag(BlockTags.GRASS_BLOCKS);
        this.tag(BlockTags.BASE_STONE_OVERWORLD)
            .add(new BlockItemId[]{BlockItemIds.STONE})
            .add(new BlockItemId[]{BlockItemIds.GRANITE})
            .add(new BlockItemId[]{BlockItemIds.DIORITE})
            .add(new BlockItemId[]{BlockItemIds.ANDESITE})
            .add(new BlockItemId[]{BlockItemIds.TUFF})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE});
        this.tag(BlockTags.BASE_STONE_NETHER)
            .add(new BlockItemId[]{BlockItemIds.NETHERRACK})
            .add(new BlockItemId[]{BlockItemIds.BASALT})
            .add(new BlockItemId[]{BlockItemIds.BLACKSTONE});
        this.tag(BlockTags.OVERWORLD_CARVER_REPLACEABLES)
            .addTag(BlockTags.BASE_STONE_OVERWORLD)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .addTag(BlockTags.SAND)
            .addTag(BlockTags.TERRACOTTA)
            .addTag(BlockTags.IRON_ORES)
            .addTag(BlockTags.COPPER_ORES)
            .addTag(BlockTags.SNOW)
            .add(BlockIds.WATER)
            .add(
                BlockItemIds.GRAVEL,
                BlockItemIds.SUSPICIOUS_GRAVEL,
                BlockItemIds.SANDSTONE,
                BlockItemIds.RED_SANDSTONE,
                BlockItemIds.CALCITE,
                BlockItemIds.PACKED_ICE,
                BlockItemIds.RAW_IRON_BLOCK,
                BlockItemIds.RAW_COPPER_BLOCK,
                BlockItemIds.CINNABAR,
                BlockItemIds.SULFUR,
                BlockItemIds.POTENT_SULFUR
            );
        this.tag(BlockTags.NETHER_CARVER_REPLACEABLES)
            .addTag(BlockTags.BASE_STONE_OVERWORLD)
            .addTag(BlockTags.BASE_STONE_NETHER)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .addTag(BlockTags.NYLIUM)
            .addTag(BlockTags.WART_BLOCKS)
            .add(BlockItemIds.SOUL_SAND, BlockItemIds.SOUL_SOIL);
        this.tag(BlockTags.BENEATH_TREE_PODZOL_REPLACEABLE).addTag(BlockTags.SUBSTRATE_OVERWORLD);
        this.tag(BlockTags.BENEATH_BAMBOO_PODZOL_REPLACEABLE).addTag(BlockTags.SUBSTRATE_OVERWORLD);
        this.tag(BlockTags.CANNOT_REPLACE_BELOW_TREE_TRUNK)
            .addTag(BlockTags.DIRT)
            .addTag(BlockTags.MUD)
            .addTag(BlockTags.MOSS_BLOCKS)
            .add(new BlockItemId[]{BlockItemIds.PODZOL});
        this.tag(BlockTags.CANDLE_CAKES).add(BlockIds.CANDLE_CAKE).addAll(BlockIds.DYED_CANDLE_CAKE);
        this.tag(BlockTags.CRYSTAL_SOUND_BLOCKS).add(BlockItemIds.AMETHYST_BLOCK, BlockItemIds.BUDDING_AMETHYST);
        this.tag(BlockTags.CAULDRONS)
            .add(new BlockItemId[]{BlockItemIds.CAULDRON})
            .add(BlockIds.WATER_CAULDRON, BlockIds.LAVA_CAULDRON, BlockIds.POWDER_SNOW_CAULDRON);
        this.tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS)
            .add(
                BlockItemIds.POWDER_SNOW,
                BlockItemIds.SCULK_VEIN,
                BlockItemIds.GLOW_LICHEN,
                BlockItemIds.LILY_PAD,
                BlockItemIds.SMALL_AMETHYST_BUD,
                BlockItemIds.PINK_PETALS,
                BlockItemIds.WILDFLOWERS,
                BlockItemIds.LEAF_LITTER
            );
        this.tag(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)
            .addTag(BlockTags.WOOL_CARPETS)
            .add(
                BlockItemIds.MOSS_CARPET,
                BlockItemIds.PALE_MOSS_CARPET,
                BlockItemIds.SNOW,
                BlockItemIds.NETHER_SPROUTS,
                BlockItemIds.WARPED_ROOTS,
                BlockItemIds.CRIMSON_ROOTS,
                BlockItemIds.RESIN_CLUMP
            );
        this.tag(BlockTags.DRIPSTONE_REPLACEABLE).addTag(BlockTags.BASE_STONE_OVERWORLD);
        this.tag(BlockTags.CAVE_VINES).add(BlockIds.CAVE_VINES_PLANT).add(new BlockItemId[]{BlockItemIds.GLOW_BERRY_CROP});
        this.tag(BlockTags.MOSS_REPLACEABLE)
            .addTag(BlockTags.BASE_STONE_OVERWORLD)
            .addTag(BlockTags.CAVE_VINES)
            .addTag(BlockTags.DIRT)
            .addTag(BlockTags.MUD)
            .addTag(BlockTags.MOSS_BLOCKS)
            .addTag(BlockTags.GRASS_BLOCKS);
        this.tag(BlockTags.LUSH_GROUND_REPLACEABLE)
            .addTag(BlockTags.MOSS_REPLACEABLE)
            .add(new BlockItemId[]{BlockItemIds.CLAY})
            .add(new BlockItemId[]{BlockItemIds.GRAVEL})
            .add(new BlockItemId[]{BlockItemIds.SAND});
        this.tag(BlockTags.AZALEA_ROOT_REPLACEABLE)
            .addTag(BlockTags.BASE_STONE_OVERWORLD)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .addTag(BlockTags.TERRACOTTA)
            .add(new BlockItemId[]{BlockItemIds.RED_SAND})
            .add(new BlockItemId[]{BlockItemIds.CLAY})
            .add(new BlockItemId[]{BlockItemIds.GRAVEL})
            .add(new BlockItemId[]{BlockItemIds.SAND})
            .add(new BlockItemId[]{BlockItemIds.SNOW_BLOCK})
            .add(new BlockItemId[]{BlockItemIds.POWDER_SNOW});
        this.tag(BlockTags.ICE_SPIKE_REPLACEABLE)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .add(new BlockItemId[]{BlockItemIds.SNOW_BLOCK})
            .add(new BlockItemId[]{BlockItemIds.ICE});
        this.tag(BlockTags.FOREST_ROCK_CAN_PLACE_ON).addTag(BlockTags.SUBSTRATE_OVERWORLD).addTag(BlockTags.BASE_STONE_OVERWORLD);
        this.tag(BlockTags.HUGE_BROWN_MUSHROOM_CAN_PLACE_ON)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .add(new BlockItemId[]{BlockItemIds.MYCELIUM})
            .add(new BlockItemId[]{BlockItemIds.PODZOL})
            .add(new BlockItemId[]{BlockItemIds.CRIMSON_NYLIUM})
            .add(new BlockItemId[]{BlockItemIds.WARPED_NYLIUM});
        this.tag(BlockTags.HUGE_RED_MUSHROOM_CAN_PLACE_ON)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .add(new BlockItemId[]{BlockItemIds.MYCELIUM})
            .add(new BlockItemId[]{BlockItemIds.PODZOL})
            .add(new BlockItemId[]{BlockItemIds.CRIMSON_NYLIUM})
            .add(new BlockItemId[]{BlockItemIds.WARPED_NYLIUM});
        this.tag(BlockTags.OCCLUDES_VIBRATION_SIGNALS).addTag(BlockTags.WOOL);
        this.tag(BlockTags.SNOW).add(BlockItemIds.SNOW, BlockItemIds.SNOW_BLOCK, BlockItemIds.POWDER_SNOW);
        this.tag(BlockTags.MINEABLE_WITH_AXE)
            .add(BlockItemIds.NOTE_BLOCK, BlockItemIds.BAMBOO, BlockItemIds.BARREL, BlockItemIds.BEE_NEST, BlockItemIds.BEEHIVE)
            .add(BlockIds.BIG_DRIPLEAF_STEM)
            .add(
                BlockItemIds.BIG_DRIPLEAF,
                BlockItemIds.BOOKSHELF,
                BlockItemIds.BROWN_MUSHROOM_BLOCK,
                BlockItemIds.CAMPFIRE,
                BlockItemIds.CARTOGRAPHY_TABLE,
                BlockItemIds.CARVED_PUMPKIN,
                BlockItemIds.CHEST,
                BlockItemIds.CHORUS_FLOWER,
                BlockItemIds.CHORUS_PLANT,
                BlockItemIds.COCOA_CROP,
                BlockItemIds.COMPOSTER,
                BlockItemIds.CRAFTING_TABLE,
                BlockItemIds.DAYLIGHT_DETECTOR,
                BlockItemIds.FLETCHING_TABLE,
                BlockItemIds.GLOW_LICHEN,
                BlockItemIds.JACK_O_LANTERN,
                BlockItemIds.JUKEBOX,
                BlockItemIds.LADDER,
                BlockItemIds.LECTERN,
                BlockItemIds.LOOM,
                BlockItemIds.MELON,
                BlockItemIds.MUSHROOM_STEM,
                BlockItemIds.PUMPKIN,
                BlockItemIds.RED_MUSHROOM_BLOCK,
                BlockItemIds.SMITHING_TABLE,
                BlockItemIds.SOUL_CAMPFIRE,
                BlockItemIds.TRAPPED_CHEST,
                BlockItemIds.VINE
            )
            .addTag(BlockTags.BANNERS)
            .addTag(BlockTags.FENCE_GATES)
            .addTag(BlockTags.LOGS)
            .addTag(BlockTags.PLANKS)
            .addTag(BlockTags.SIGNS)
            .addTag(BlockTags.WOODEN_BUTTONS)
            .addTag(BlockTags.WOODEN_DOORS)
            .addTag(BlockTags.WOODEN_FENCES)
            .addTag(BlockTags.WOODEN_PRESSURE_PLATES)
            .addTag(BlockTags.WOODEN_SLABS)
            .addTag(BlockTags.WOODEN_STAIRS)
            .addTag(BlockTags.WOODEN_TRAPDOORS)
            .add(new BlockItemId[]{BlockItemIds.MANGROVE_ROOTS})
            .addTag(BlockTags.ALL_HANGING_SIGNS)
            .add(BlockItemIds.BAMBOO_MOSAIC, BlockItemIds.BAMBOO_MOSAIC_SLAB, BlockItemIds.BAMBOO_MOSAIC_STAIRS)
            .addTag(BlockTags.BAMBOO_BLOCKS)
            .add(new BlockItemId[]{BlockItemIds.CHISELED_BOOKSHELF})
            .addTag(BlockTags.WOODEN_SHELVES)
            .add(new BlockItemId[]{BlockItemIds.CREAKING_HEART});
        this.tag(BlockTags.MINEABLE_WITH_HOE)
            .addTag(BlockTags.LEAVES)
            .add(
                BlockItemIds.NETHER_WART_BLOCK,
                BlockItemIds.WARPED_WART_BLOCK,
                BlockItemIds.HAY_BLOCK,
                BlockItemIds.DRIED_KELP_BLOCK,
                BlockItemIds.TARGET,
                BlockItemIds.SHROOMLIGHT,
                BlockItemIds.SPONGE,
                BlockItemIds.WET_SPONGE,
                BlockItemIds.SCULK_SENSOR,
                BlockItemIds.CALIBRATED_SCULK_SENSOR,
                BlockItemIds.MOSS_BLOCK,
                BlockItemIds.MOSS_CARPET,
                BlockItemIds.PALE_MOSS_BLOCK,
                BlockItemIds.PALE_MOSS_CARPET,
                BlockItemIds.SCULK,
                BlockItemIds.SCULK_CATALYST,
                BlockItemIds.SCULK_VEIN,
                BlockItemIds.SCULK_SHRIEKER
            );
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .add(
                BlockItemIds.STONE,
                BlockItemIds.GRANITE,
                BlockItemIds.POLISHED_GRANITE,
                BlockItemIds.DIORITE,
                BlockItemIds.POLISHED_DIORITE,
                BlockItemIds.ANDESITE,
                BlockItemIds.POLISHED_ANDESITE,
                BlockItemIds.COBBLESTONE,
                BlockItemIds.GOLD_ORE,
                BlockItemIds.DEEPSLATE_GOLD_ORE,
                BlockItemIds.IRON_ORE,
                BlockItemIds.DEEPSLATE_IRON_ORE,
                BlockItemIds.COAL_ORE,
                BlockItemIds.DEEPSLATE_COAL_ORE,
                BlockItemIds.NETHER_GOLD_ORE,
                BlockItemIds.LAPIS_ORE,
                BlockItemIds.DEEPSLATE_LAPIS_ORE,
                BlockItemIds.LAPIS_BLOCK,
                BlockItemIds.DISPENSER,
                BlockItemIds.SANDSTONE,
                BlockItemIds.CHISELED_SANDSTONE,
                BlockItemIds.CUT_SANDSTONE,
                BlockItemIds.GOLD_BLOCK,
                BlockItemIds.IRON_BLOCK,
                BlockItemIds.BRICKS,
                BlockItemIds.MOSSY_COBBLESTONE,
                BlockItemIds.OBSIDIAN,
                BlockItemIds.SPAWNER,
                BlockItemIds.DIAMOND_ORE,
                BlockItemIds.DEEPSLATE_DIAMOND_ORE,
                BlockItemIds.DIAMOND_BLOCK,
                BlockItemIds.FURNACE,
                BlockItemIds.COBBLESTONE_STAIRS,
                BlockItemIds.STONE_PRESSURE_PLATE,
                BlockItemIds.IRON_DOOR,
                BlockItemIds.REDSTONE_ORE,
                BlockItemIds.DEEPSLATE_REDSTONE_ORE,
                BlockItemIds.NETHERRACK,
                BlockItemIds.BASALT,
                BlockItemIds.POLISHED_BASALT,
                BlockItemIds.STONE_BRICKS,
                BlockItemIds.MOSSY_STONE_BRICKS,
                BlockItemIds.CRACKED_STONE_BRICKS,
                BlockItemIds.CHISELED_STONE_BRICKS,
                BlockItemIds.BRICK_STAIRS,
                BlockItemIds.STONE_BRICK_STAIRS,
                BlockItemIds.NETHER_BRICKS,
                BlockItemIds.NETHER_BRICK_FENCE,
                BlockItemIds.NETHER_BRICK_STAIRS,
                BlockItemIds.ENCHANTING_TABLE,
                BlockItemIds.BREWING_STAND,
                BlockItemIds.END_STONE,
                BlockItemIds.SANDSTONE_STAIRS,
                BlockItemIds.EMERALD_ORE,
                BlockItemIds.DEEPSLATE_EMERALD_ORE,
                BlockItemIds.ENDER_CHEST,
                BlockItemIds.EMERALD_BLOCK,
                BlockItemIds.LIGHT_WEIGHTED_PRESSURE_PLATE,
                BlockItemIds.HEAVY_WEIGHTED_PRESSURE_PLATE,
                BlockItemIds.REDSTONE_BLOCK,
                BlockItemIds.NETHER_QUARTZ_ORE,
                BlockItemIds.HOPPER,
                BlockItemIds.QUARTZ_BLOCK,
                BlockItemIds.CHISELED_QUARTZ_BLOCK,
                BlockItemIds.QUARTZ_PILLAR,
                BlockItemIds.QUARTZ_STAIRS,
                BlockItemIds.DROPPER,
                BlockItemIds.IRON_TRAPDOOR,
                BlockItemIds.PRISMARINE,
                BlockItemIds.PRISMARINE_BRICKS,
                BlockItemIds.DARK_PRISMARINE,
                BlockItemIds.PRISMARINE_STAIRS,
                BlockItemIds.PRISMARINE_BRICK_STAIRS,
                BlockItemIds.DARK_PRISMARINE_STAIRS,
                BlockItemIds.PRISMARINE_SLAB,
                BlockItemIds.PRISMARINE_BRICK_SLAB,
                BlockItemIds.DARK_PRISMARINE_SLAB,
                BlockItemIds.TERRACOTTA,
                BlockItemIds.COAL_BLOCK,
                BlockItemIds.RED_SANDSTONE,
                BlockItemIds.CHISELED_RED_SANDSTONE,
                BlockItemIds.CUT_RED_SANDSTONE,
                BlockItemIds.RED_SANDSTONE_STAIRS,
                BlockItemIds.STONE_SLAB,
                BlockItemIds.SMOOTH_STONE_SLAB,
                BlockItemIds.SANDSTONE_SLAB,
                BlockItemIds.CUT_SANDSTONE_SLAB,
                BlockItemIds.PETRIFIED_OAK_SLAB,
                BlockItemIds.COBBLESTONE_SLAB,
                BlockItemIds.BRICK_SLAB,
                BlockItemIds.STONE_BRICK_SLAB,
                BlockItemIds.NETHER_BRICK_SLAB,
                BlockItemIds.QUARTZ_SLAB,
                BlockItemIds.RED_SANDSTONE_SLAB,
                BlockItemIds.CUT_RED_SANDSTONE_SLAB,
                BlockItemIds.PURPUR_SLAB,
                BlockItemIds.SMOOTH_STONE,
                BlockItemIds.SMOOTH_SANDSTONE,
                BlockItemIds.SMOOTH_QUARTZ,
                BlockItemIds.SMOOTH_RED_SANDSTONE,
                BlockItemIds.PURPUR_BLOCK,
                BlockItemIds.PURPUR_PILLAR,
                BlockItemIds.PURPUR_STAIRS,
                BlockItemIds.END_STONE_BRICKS,
                BlockItemIds.MAGMA_BLOCK,
                BlockItemIds.RED_NETHER_BRICKS,
                BlockItemIds.BONE_BLOCK,
                BlockItemIds.OBSERVER,
                BlockItemIds.DEAD_TUBE_CORAL_BLOCK,
                BlockItemIds.DEAD_BRAIN_CORAL_BLOCK,
                BlockItemIds.DEAD_BUBBLE_CORAL_BLOCK,
                BlockItemIds.DEAD_FIRE_CORAL_BLOCK,
                BlockItemIds.DEAD_HORN_CORAL_BLOCK,
                BlockItemIds.TUBE_CORAL_BLOCK,
                BlockItemIds.BRAIN_CORAL_BLOCK,
                BlockItemIds.BUBBLE_CORAL_BLOCK,
                BlockItemIds.FIRE_CORAL_BLOCK,
                BlockItemIds.HORN_CORAL_BLOCK,
                BlockItemIds.DEAD_TUBE_CORAL,
                BlockItemIds.DEAD_BRAIN_CORAL,
                BlockItemIds.DEAD_BUBBLE_CORAL,
                BlockItemIds.DEAD_FIRE_CORAL,
                BlockItemIds.DEAD_HORN_CORAL,
                BlockItemIds.DEAD_TUBE_CORAL_FAN,
                BlockItemIds.DEAD_BRAIN_CORAL_FAN,
                BlockItemIds.DEAD_BUBBLE_CORAL_FAN,
                BlockItemIds.DEAD_FIRE_CORAL_FAN,
                BlockItemIds.DEAD_HORN_CORAL_FAN
            )
            .add(
                BlockIds.DEAD_TUBE_CORAL_WALL_FAN,
                BlockIds.DEAD_BRAIN_CORAL_WALL_FAN,
                BlockIds.DEAD_BUBBLE_CORAL_WALL_FAN,
                BlockIds.DEAD_FIRE_CORAL_WALL_FAN,
                BlockIds.DEAD_HORN_CORAL_WALL_FAN
            )
            .add(
                BlockItemIds.POLISHED_GRANITE_STAIRS,
                BlockItemIds.SMOOTH_RED_SANDSTONE_STAIRS,
                BlockItemIds.MOSSY_STONE_BRICK_STAIRS,
                BlockItemIds.POLISHED_DIORITE_STAIRS,
                BlockItemIds.MOSSY_COBBLESTONE_STAIRS,
                BlockItemIds.END_STONE_BRICK_STAIRS,
                BlockItemIds.STONE_STAIRS,
                BlockItemIds.SMOOTH_SANDSTONE_STAIRS,
                BlockItemIds.SMOOTH_QUARTZ_STAIRS,
                BlockItemIds.GRANITE_STAIRS,
                BlockItemIds.ANDESITE_STAIRS,
                BlockItemIds.RED_NETHER_BRICK_STAIRS,
                BlockItemIds.POLISHED_ANDESITE_STAIRS,
                BlockItemIds.DIORITE_STAIRS,
                BlockItemIds.POLISHED_GRANITE_SLAB,
                BlockItemIds.SMOOTH_RED_SANDSTONE_SLAB,
                BlockItemIds.MOSSY_STONE_BRICK_SLAB,
                BlockItemIds.POLISHED_DIORITE_SLAB,
                BlockItemIds.MOSSY_COBBLESTONE_SLAB,
                BlockItemIds.END_STONE_BRICK_SLAB,
                BlockItemIds.SMOOTH_SANDSTONE_SLAB,
                BlockItemIds.SMOOTH_QUARTZ_SLAB,
                BlockItemIds.GRANITE_SLAB,
                BlockItemIds.ANDESITE_SLAB,
                BlockItemIds.RED_NETHER_BRICK_SLAB,
                BlockItemIds.POLISHED_ANDESITE_SLAB,
                BlockItemIds.DIORITE_SLAB,
                BlockItemIds.SMOKER,
                BlockItemIds.BLAST_FURNACE,
                BlockItemIds.GRINDSTONE,
                BlockItemIds.STONECUTTER,
                BlockItemIds.BELL,
                BlockItemIds.WARPED_NYLIUM,
                BlockItemIds.CRIMSON_NYLIUM,
                BlockItemIds.NETHERITE_BLOCK,
                BlockItemIds.ANCIENT_DEBRIS,
                BlockItemIds.CRYING_OBSIDIAN,
                BlockItemIds.RESPAWN_ANCHOR,
                BlockItemIds.LODESTONE,
                BlockItemIds.BLACKSTONE,
                BlockItemIds.BLACKSTONE_STAIRS,
                BlockItemIds.BLACKSTONE_SLAB,
                BlockItemIds.POLISHED_BLACKSTONE,
                BlockItemIds.POLISHED_BLACKSTONE_BRICKS,
                BlockItemIds.CRACKED_POLISHED_BLACKSTONE_BRICKS,
                BlockItemIds.CHISELED_POLISHED_BLACKSTONE,
                BlockItemIds.POLISHED_BLACKSTONE_BRICK_SLAB,
                BlockItemIds.POLISHED_BLACKSTONE_BRICK_STAIRS,
                BlockItemIds.GILDED_BLACKSTONE,
                BlockItemIds.POLISHED_BLACKSTONE_STAIRS,
                BlockItemIds.POLISHED_BLACKSTONE_SLAB,
                BlockItemIds.POLISHED_BLACKSTONE_PRESSURE_PLATE,
                BlockItemIds.CHISELED_NETHER_BRICKS,
                BlockItemIds.CRACKED_NETHER_BRICKS,
                BlockItemIds.QUARTZ_BRICKS,
                BlockItemIds.TUFF,
                BlockItemIds.CALCITE,
                BlockItemIds.COPPER_ORE,
                BlockItemIds.DEEPSLATE_COPPER_ORE,
                BlockItemIds.DRIPSTONE_BLOCK,
                BlockItemIds.DEEPSLATE,
                BlockItemIds.COBBLED_DEEPSLATE,
                BlockItemIds.COBBLED_DEEPSLATE_STAIRS,
                BlockItemIds.COBBLED_DEEPSLATE_SLAB,
                BlockItemIds.POLISHED_DEEPSLATE,
                BlockItemIds.POLISHED_DEEPSLATE_STAIRS,
                BlockItemIds.POLISHED_DEEPSLATE_SLAB,
                BlockItemIds.DEEPSLATE_TILES,
                BlockItemIds.DEEPSLATE_TILE_STAIRS,
                BlockItemIds.DEEPSLATE_TILE_SLAB,
                BlockItemIds.DEEPSLATE_BRICKS,
                BlockItemIds.DEEPSLATE_BRICK_STAIRS,
                BlockItemIds.DEEPSLATE_BRICK_SLAB,
                BlockItemIds.CHISELED_DEEPSLATE,
                BlockItemIds.CRACKED_DEEPSLATE_BRICKS,
                BlockItemIds.CRACKED_DEEPSLATE_TILES,
                BlockItemIds.SMOOTH_BASALT,
                BlockItemIds.RAW_IRON_BLOCK,
                BlockItemIds.RAW_COPPER_BLOCK,
                BlockItemIds.RAW_GOLD_BLOCK,
                BlockItemIds.ICE,
                BlockItemIds.PACKED_ICE,
                BlockItemIds.BLUE_ICE,
                BlockItemIds.PISTON,
                BlockItemIds.STICKY_PISTON
            )
            .add(BlockIds.PISTON_HEAD)
            .add(
                BlockItemIds.AMETHYST_CLUSTER,
                BlockItemIds.SMALL_AMETHYST_BUD,
                BlockItemIds.MEDIUM_AMETHYST_BUD,
                BlockItemIds.LARGE_AMETHYST_BUD,
                BlockItemIds.AMETHYST_BLOCK,
                BlockItemIds.BUDDING_AMETHYST,
                BlockItemIds.INFESTED_COBBLESTONE,
                BlockItemIds.INFESTED_CHISELED_STONE_BRICKS,
                BlockItemIds.INFESTED_CRACKED_STONE_BRICKS,
                BlockItemIds.INFESTED_DEEPSLATE,
                BlockItemIds.INFESTED_STONE,
                BlockItemIds.INFESTED_MOSSY_STONE_BRICKS,
                BlockItemIds.INFESTED_STONE_BRICKS
            )
            .addTag(BlockTags.STONE_BUTTONS)
            .addTag(BlockTags.WALLS)
            .addTag(BlockTags.SHULKER_BOXES)
            .addTag(BlockTags.ANVIL)
            .addTag(BlockTags.CAULDRONS)
            .addTag(BlockTags.RAILS)
            .add(new BlockItemId[]{BlockItemIds.CONDUIT})
            .add(new BlockItemId[]{BlockItemIds.MUD_BRICKS})
            .add(new BlockItemId[]{BlockItemIds.MUD_BRICK_STAIRS})
            .add(new BlockItemId[]{BlockItemIds.MUD_BRICK_SLAB})
            .add(new BlockItemId[]{BlockItemIds.PACKED_MUD})
            .add(
                BlockItemIds.CRAFTER,
                BlockItemIds.TUFF_SLAB,
                BlockItemIds.TUFF_STAIRS,
                BlockItemIds.TUFF_WALL,
                BlockItemIds.CHISELED_TUFF,
                BlockItemIds.POLISHED_TUFF,
                BlockItemIds.POLISHED_TUFF_SLAB,
                BlockItemIds.POLISHED_TUFF_STAIRS,
                BlockItemIds.POLISHED_TUFF_WALL,
                BlockItemIds.TUFF_BRICKS,
                BlockItemIds.TUFF_BRICK_SLAB,
                BlockItemIds.TUFF_BRICK_STAIRS,
                BlockItemIds.TUFF_BRICK_WALL,
                BlockItemIds.CHISELED_TUFF_BRICKS,
                BlockItemIds.HEAVY_CORE,
                BlockItemIds.RESIN_BRICKS,
                BlockItemIds.RESIN_BRICK_SLAB,
                BlockItemIds.RESIN_BRICK_WALL,
                BlockItemIds.RESIN_BRICK_STAIRS,
                BlockItemIds.CHISELED_RESIN_BRICKS,
                BlockItemIds.CINNABAR,
                BlockItemIds.CINNABAR_SLAB,
                BlockItemIds.CINNABAR_STAIRS,
                BlockItemIds.CINNABAR_WALL,
                BlockItemIds.POLISHED_CINNABAR,
                BlockItemIds.POLISHED_CINNABAR_SLAB,
                BlockItemIds.POLISHED_CINNABAR_STAIRS,
                BlockItemIds.POLISHED_CINNABAR_WALL,
                BlockItemIds.CINNABAR_BRICKS,
                BlockItemIds.CINNABAR_BRICK_SLAB,
                BlockItemIds.CINNABAR_BRICK_STAIRS,
                BlockItemIds.CINNABAR_BRICK_WALL,
                BlockItemIds.CHISELED_CINNABAR,
                BlockItemIds.SULFUR,
                BlockItemIds.POTENT_SULFUR,
                BlockItemIds.SULFUR_SLAB,
                BlockItemIds.SULFUR_STAIRS,
                BlockItemIds.SULFUR_WALL,
                BlockItemIds.POLISHED_SULFUR,
                BlockItemIds.POLISHED_SULFUR_SLAB,
                BlockItemIds.POLISHED_SULFUR_STAIRS,
                BlockItemIds.POLISHED_SULFUR_WALL,
                BlockItemIds.SULFUR_BRICKS,
                BlockItemIds.SULFUR_BRICK_SLAB,
                BlockItemIds.SULFUR_BRICK_STAIRS,
                BlockItemIds.SULFUR_BRICK_WALL,
                BlockItemIds.CHISELED_SULFUR
            )
            .addTag(BlockTags.COPPER_CHESTS)
            .addTag(BlockTags.COPPER_GOLEM_STATUES)
            .addTag(BlockTags.LIGHTNING_RODS)
            .addTag(BlockTags.LANTERNS)
            .addTag(BlockTags.CHAINS)
            .addTag(BlockTags.BARS)
            .addAll(toIds(BlockItemIds.COPPER_BLOCK))
            .addAll(toIds(BlockItemIds.COPPER_BULB))
            .addAll(toIds(BlockItemIds.CUT_COPPER))
            .addAll(toIds(BlockItemIds.CHISELED_COPPER))
            .addAll(toIds(BlockItemIds.CUT_COPPER_STAIRS))
            .addAll(toIds(BlockItemIds.CUT_COPPER_SLAB))
            .addAll(toIds(BlockItemIds.COPPER_DOOR))
            .addAll(toIds(BlockItemIds.COPPER_TRAPDOOR))
            .addAll(toIds(BlockItemIds.COPPER_GRATE))
            .addAll(toIds(BlockItemIds.GLAZED_TERRACOTTA))
            .addAll(toIds(BlockItemIds.DYED_TERRACOTTA))
            .addAll(toIds(BlockItemIds.CONCRETE))
            .addTag(BlockTags.SPELEOTHEMS);
        this.tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .add(
                BlockItemIds.CLAY,
                BlockItemIds.DIRT,
                BlockItemIds.COARSE_DIRT,
                BlockItemIds.PODZOL,
                BlockItemIds.FARMLAND,
                BlockItemIds.GRASS_BLOCK,
                BlockItemIds.GRAVEL,
                BlockItemIds.MYCELIUM,
                BlockItemIds.SAND,
                BlockItemIds.RED_SAND,
                BlockItemIds.SNOW_BLOCK,
                BlockItemIds.SNOW,
                BlockItemIds.SOUL_SAND,
                BlockItemIds.DIRT_PATH,
                BlockItemIds.SOUL_SOIL,
                BlockItemIds.ROOTED_DIRT,
                BlockItemIds.MUDDY_MANGROVE_ROOTS,
                BlockItemIds.MUD,
                BlockItemIds.SUSPICIOUS_SAND,
                BlockItemIds.SUSPICIOUS_GRAVEL
            )
            .addTag(BlockTags.CONCRETE_POWDERS);
        this.tag(BlockTags.SWORD_EFFICIENT)
            .addTag(BlockTags.LEAVES)
            .add(BlockItemIds.VINE, BlockItemIds.GLOW_LICHEN)
            .add(
                BlockItemIds.PUMPKIN,
                BlockItemIds.CARVED_PUMPKIN,
                BlockItemIds.JACK_O_LANTERN,
                BlockItemIds.MELON,
                BlockItemIds.COCOA_CROP,
                BlockItemIds.BIG_DRIPLEAF
            )
            .add(BlockIds.BIG_DRIPLEAF_STEM)
            .add(BlockItemIds.CHORUS_PLANT, BlockItemIds.CHORUS_FLOWER);
        this.tag(BlockTags.SWORD_INSTANTLY_MINES).add(new BlockItemId[]{BlockItemIds.BAMBOO}).add(BlockIds.BAMBOO_SAPLING);
        this.tag(BlockTags.SHEARS_EXTREME_BREAKING_SPEED).addTag(BlockTags.LEAVES);
        this.tag(BlockTags.SHEARS_MAJOR_BREAKING_SPEED).addTag(BlockTags.WOOL);
        this.tag(BlockTags.SHEARS_MINOR_BREAKING_SPEED).add(BlockItemIds.GLOW_LICHEN, BlockItemIds.VINE);
        this.tag(BlockTags.NEEDS_DIAMOND_TOOL)
            .add(BlockItemIds.OBSIDIAN, BlockItemIds.CRYING_OBSIDIAN, BlockItemIds.NETHERITE_BLOCK, BlockItemIds.RESPAWN_ANCHOR, BlockItemIds.ANCIENT_DEBRIS);
        this.tag(BlockTags.NEEDS_IRON_TOOL)
            .add(
                BlockItemIds.DIAMOND_BLOCK,
                BlockItemIds.DIAMOND_ORE,
                BlockItemIds.DEEPSLATE_DIAMOND_ORE,
                BlockItemIds.EMERALD_ORE,
                BlockItemIds.DEEPSLATE_EMERALD_ORE,
                BlockItemIds.EMERALD_BLOCK,
                BlockItemIds.GOLD_BLOCK,
                BlockItemIds.RAW_GOLD_BLOCK,
                BlockItemIds.GOLD_ORE,
                BlockItemIds.DEEPSLATE_GOLD_ORE,
                BlockItemIds.REDSTONE_ORE,
                BlockItemIds.DEEPSLATE_REDSTONE_ORE
            );
        this.tag(BlockTags.NEEDS_STONE_TOOL)
            .add(
                BlockItemIds.IRON_BLOCK,
                BlockItemIds.RAW_IRON_BLOCK,
                BlockItemIds.IRON_ORE,
                BlockItemIds.DEEPSLATE_IRON_ORE,
                BlockItemIds.LAPIS_BLOCK,
                BlockItemIds.LAPIS_ORE,
                BlockItemIds.DEEPSLATE_LAPIS_ORE,
                BlockItemIds.RAW_COPPER_BLOCK,
                BlockItemIds.COPPER_ORE,
                BlockItemIds.DEEPSLATE_COPPER_ORE,
                BlockItemIds.CRAFTER
            )
            .addTag(BlockTags.COPPER_CHESTS)
            .addTag(BlockTags.LIGHTNING_RODS)
            .addAll(toIds(BlockItemIds.COPPER_BLOCK))
            .addAll(toIds(BlockItemIds.COPPER_BULB))
            .addAll(toIds(BlockItemIds.CUT_COPPER))
            .addAll(toIds(BlockItemIds.CHISELED_COPPER))
            .addAll(toIds(BlockItemIds.CUT_COPPER_STAIRS))
            .addAll(toIds(BlockItemIds.CUT_COPPER_SLAB))
            .addAll(toIds(BlockItemIds.COPPER_TRAPDOOR))
            .addAll(toIds(BlockItemIds.COPPER_GRATE));
        this.tag(BlockTags.INCORRECT_FOR_NETHERITE_TOOL);
        this.tag(BlockTags.INCORRECT_FOR_DIAMOND_TOOL);
        this.tag(BlockTags.INCORRECT_FOR_IRON_TOOL).addTag(BlockTags.NEEDS_DIAMOND_TOOL);
        this.tag(BlockTags.INCORRECT_FOR_STONE_TOOL).addTag(BlockTags.NEEDS_DIAMOND_TOOL).addTag(BlockTags.NEEDS_IRON_TOOL);
        this.tag(BlockTags.INCORRECT_FOR_COPPER_TOOL).addTag(BlockTags.NEEDS_DIAMOND_TOOL).addTag(BlockTags.NEEDS_IRON_TOOL);
        this.tag(BlockTags.INCORRECT_FOR_GOLD_TOOL).addTag(BlockTags.NEEDS_DIAMOND_TOOL).addTag(BlockTags.NEEDS_IRON_TOOL).addTag(BlockTags.NEEDS_STONE_TOOL);
        this.tag(BlockTags.INCORRECT_FOR_WOODEN_TOOL).addTag(BlockTags.NEEDS_DIAMOND_TOOL).addTag(BlockTags.NEEDS_IRON_TOOL).addTag(BlockTags.NEEDS_STONE_TOOL);
        this.tag(BlockTags.FEATURES_CANNOT_REPLACE)
            .add(
                BlockItemIds.BEDROCK,
                BlockItemIds.SPAWNER,
                BlockItemIds.CHEST,
                BlockItemIds.END_PORTAL_FRAME,
                BlockItemIds.REINFORCED_DEEPSLATE,
                BlockItemIds.TRIAL_SPAWNER,
                BlockItemIds.VAULT
            );
        this.tag(BlockTags.LAVA_POOL_STONE_CANNOT_REPLACE).addTag(BlockTags.FEATURES_CANNOT_REPLACE).addTag(BlockTags.LEAVES).addTag(BlockTags.LOGS);
        this.tag(BlockTags.GEODE_INVALID_BLOCKS)
            .add(new BlockItemId[]{BlockItemIds.BEDROCK})
            .add(BlockIds.WATER, BlockIds.LAVA)
            .add(BlockItemIds.ICE, BlockItemIds.PACKED_ICE, BlockItemIds.BLUE_ICE);
        this.tag(BlockTags.ANIMALS_SPAWNABLE_ON).add(new BlockItemId[]{BlockItemIds.GRASS_BLOCK});
        this.tag(BlockTags.ARMADILLO_SPAWNABLE_ON)
            .addTag(BlockTags.ANIMALS_SPAWNABLE_ON)
            .addTag(BlockTags.BADLANDS_TERRACOTTA)
            .add(BlockItemIds.RED_SAND, BlockItemIds.COARSE_DIRT);
        this.tag(BlockTags.AXOLOTLS_SPAWNABLE_ON).add(new BlockItemId[]{BlockItemIds.CLAY});
        this.tag(BlockTags.GOATS_SPAWNABLE_ON)
            .addTag(BlockTags.ANIMALS_SPAWNABLE_ON)
            .add(BlockItemIds.STONE, BlockItemIds.SNOW, BlockItemIds.SNOW_BLOCK, BlockItemIds.PACKED_ICE, BlockItemIds.GRAVEL);
        this.tag(BlockTags.MOOSHROOMS_SPAWNABLE_ON).add(new BlockItemId[]{BlockItemIds.MYCELIUM});
        this.tag(BlockTags.PARROTS_SPAWNABLE_ON).add(BlockItemIds.GRASS_BLOCK, BlockItemIds.AIR).addTag(BlockTags.LEAVES).addTag(BlockTags.LOGS);
        this.tag(BlockTags.POLAR_BEARS_SPAWNABLE_ON_ALTERNATE).add(new BlockItemId[]{BlockItemIds.ICE});
        this.tag(BlockTags.RABBITS_SPAWNABLE_ON).add(BlockItemIds.GRASS_BLOCK, BlockItemIds.SNOW, BlockItemIds.SNOW_BLOCK, BlockItemIds.SAND);
        this.tag(BlockTags.FOXES_SPAWNABLE_ON)
            .add(BlockItemIds.GRASS_BLOCK, BlockItemIds.SNOW, BlockItemIds.SNOW_BLOCK, BlockItemIds.PODZOL, BlockItemIds.COARSE_DIRT);
        this.tag(BlockTags.WOLVES_SPAWNABLE_ON)
            .add(BlockItemIds.GRASS_BLOCK, BlockItemIds.SNOW, BlockItemIds.SNOW_BLOCK, BlockItemIds.COARSE_DIRT, BlockItemIds.PODZOL);
        this.tag(BlockTags.FROGS_SPAWNABLE_ON).add(BlockItemIds.GRASS_BLOCK, BlockItemIds.MUD, BlockItemIds.MANGROVE_ROOTS, BlockItemIds.MUDDY_MANGROVE_ROOTS);
        this.tag(BlockTags.BATS_SPAWNABLE_ON).addTag(BlockTags.BASE_STONE_OVERWORLD);
        this.tag(BlockTags.CAMELS_SPAWNABLE_ON).addTag(BlockTags.SAND);
        this.tag(BlockTags.BADLANDS_TERRACOTTA)
            .add(new BlockItemId[]{BlockItemIds.TERRACOTTA})
            .add(
                BlockItemIds.DYED_TERRACOTTA.white(),
                BlockItemIds.DYED_TERRACOTTA.yellow(),
                BlockItemIds.DYED_TERRACOTTA.orange(),
                BlockItemIds.DYED_TERRACOTTA.red(),
                BlockItemIds.DYED_TERRACOTTA.brown(),
                BlockItemIds.DYED_TERRACOTTA.lightGray()
            );
        this.tag(BlockTags.AZALEA_GROWS_ON)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .addTag(BlockTags.SAND)
            .addTag(BlockTags.TERRACOTTA)
            .add(new BlockItemId[]{BlockItemIds.SNOW_BLOCK})
            .add(new BlockItemId[]{BlockItemIds.POWDER_SNOW});
        this.tag(BlockTags.FROG_PREFER_JUMP_TO).add(BlockItemIds.LILY_PAD, BlockItemIds.BIG_DRIPLEAF);
        this.tag(BlockTags.SCULK_REPLACEABLE)
            .addTag(BlockTags.BASE_STONE_OVERWORLD)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .addTag(BlockTags.TERRACOTTA)
            .addTag(BlockTags.NYLIUM)
            .addTag(BlockTags.BASE_STONE_NETHER)
            .add(BlockItemIds.SAND, BlockItemIds.RED_SAND)
            .add(new BlockItemId[]{BlockItemIds.GRAVEL})
            .add(new BlockItemId[]{BlockItemIds.SOUL_SAND})
            .add(new BlockItemId[]{BlockItemIds.SOUL_SOIL})
            .add(new BlockItemId[]{BlockItemIds.CALCITE})
            .add(new BlockItemId[]{BlockItemIds.SMOOTH_BASALT})
            .add(new BlockItemId[]{BlockItemIds.CLAY})
            .add(new BlockItemId[]{BlockItemIds.DRIPSTONE_BLOCK})
            .add(new BlockItemId[]{BlockItemIds.END_STONE})
            .add(new BlockItemId[]{BlockItemIds.RED_SANDSTONE})
            .add(new BlockItemId[]{BlockItemIds.SANDSTONE})
            .add(BlockItemIds.SULFUR, BlockItemIds.CINNABAR);
        this.tag(BlockTags.SCULK_REPLACEABLE_WORLD_GEN)
            .addTag(BlockTags.SCULK_REPLACEABLE)
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_BRICKS})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_TILES})
            .add(new BlockItemId[]{BlockItemIds.COBBLED_DEEPSLATE})
            .add(new BlockItemId[]{BlockItemIds.CRACKED_DEEPSLATE_BRICKS})
            .add(new BlockItemId[]{BlockItemIds.CRACKED_DEEPSLATE_TILES})
            .add(new BlockItemId[]{BlockItemIds.POLISHED_DEEPSLATE});
        this.tag(BlockTags.VIBRATION_RESONATORS).add(new BlockItemId[]{BlockItemIds.AMETHYST_BLOCK});
        this.tag(BlockTags.CONVERTABLE_TO_MUD).add(BlockItemIds.DIRT, BlockItemIds.COARSE_DIRT, BlockItemIds.ROOTED_DIRT);
        this.tag(BlockTags.ANCIENT_CITY_REPLACEABLE)
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_BRICKS})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_TILES})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_BRICK_SLAB})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_TILE_SLAB})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_BRICK_STAIRS})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_TILE_WALL})
            .add(new BlockItemId[]{BlockItemIds.DEEPSLATE_BRICK_WALL})
            .add(new BlockItemId[]{BlockItemIds.COBBLED_DEEPSLATE})
            .add(new BlockItemId[]{BlockItemIds.CRACKED_DEEPSLATE_BRICKS})
            .add(new BlockItemId[]{BlockItemIds.CRACKED_DEEPSLATE_TILES})
            .add(new BlockItemId[]{BlockItemIds.WOOL.gray()});
        this.tag(BlockTags.MANGROVE_LOGS_CAN_GROW_THROUGH)
            .add(
                BlockItemIds.MUD,
                BlockItemIds.MUDDY_MANGROVE_ROOTS,
                BlockItemIds.MANGROVE_ROOTS,
                BlockItemIds.MANGROVE_LEAVES,
                BlockItemIds.MANGROVE_LOG,
                BlockItemIds.MANGROVE_PROPAGULE,
                BlockItemIds.MOSS_CARPET,
                BlockItemIds.VINE
            );
        this.tag(BlockTags.MANGROVE_ROOTS_CAN_GROW_THROUGH)
            .add(
                BlockItemIds.MUD,
                BlockItemIds.MUDDY_MANGROVE_ROOTS,
                BlockItemIds.MANGROVE_ROOTS,
                BlockItemIds.MOSS_CARPET,
                BlockItemIds.VINE,
                BlockItemIds.MANGROVE_PROPAGULE,
                BlockItemIds.SNOW
            );
        this.tag(BlockTags.REPLACEABLE_BY_TREES)
            .addTag(BlockTags.LEAVES)
            .addTag(BlockTags.SMALL_FLOWERS)
            .add(new BlockItemId[]{BlockItemIds.PALE_MOSS_CARPET})
            .add(
                BlockItemIds.SHORT_GRASS,
                BlockItemIds.FERN,
                BlockItemIds.DEAD_BUSH,
                BlockItemIds.VINE,
                BlockItemIds.GLOW_LICHEN,
                BlockItemIds.SUNFLOWER,
                BlockItemIds.LILAC,
                BlockItemIds.ROSE_BUSH,
                BlockItemIds.PEONY,
                BlockItemIds.TALL_GRASS,
                BlockItemIds.LARGE_FERN,
                BlockItemIds.HANGING_ROOTS,
                BlockItemIds.PITCHER_PLANT
            )
            .add(BlockIds.WATER)
            .add(new BlockItemId[]{BlockItemIds.SEAGRASS})
            .add(BlockIds.TALL_SEAGRASS)
            .add(BlockItemIds.BUSH, BlockItemIds.FIREFLY_BUSH)
            .add(
                BlockItemIds.WARPED_ROOTS,
                BlockItemIds.NETHER_SPROUTS,
                BlockItemIds.CRIMSON_ROOTS,
                BlockItemIds.LEAF_LITTER,
                BlockItemIds.SHORT_DRY_GRASS,
                BlockItemIds.TALL_DRY_GRASS
            );
        this.tag(BlockTags.REPLACEABLE_BY_MUSHROOMS)
            .addTag(BlockTags.LEAVES)
            .addTag(BlockTags.SMALL_FLOWERS)
            .add(new BlockItemId[]{BlockItemIds.PALE_MOSS_CARPET})
            .add(
                BlockItemIds.SHORT_GRASS,
                BlockItemIds.FERN,
                BlockItemIds.DEAD_BUSH,
                BlockItemIds.VINE,
                BlockItemIds.GLOW_LICHEN,
                BlockItemIds.SUNFLOWER,
                BlockItemIds.LILAC,
                BlockItemIds.ROSE_BUSH,
                BlockItemIds.PEONY,
                BlockItemIds.TALL_GRASS,
                BlockItemIds.LARGE_FERN,
                BlockItemIds.HANGING_ROOTS,
                BlockItemIds.PITCHER_PLANT
            )
            .add(BlockIds.WATER)
            .add(new BlockItemId[]{BlockItemIds.SEAGRASS})
            .add(BlockIds.TALL_SEAGRASS)
            .add(BlockItemIds.BROWN_MUSHROOM, BlockItemIds.RED_MUSHROOM, BlockItemIds.BROWN_MUSHROOM_BLOCK, BlockItemIds.RED_MUSHROOM_BLOCK)
            .add(
                BlockItemIds.WARPED_ROOTS,
                BlockItemIds.NETHER_SPROUTS,
                BlockItemIds.CRIMSON_ROOTS,
                BlockItemIds.LEAF_LITTER,
                BlockItemIds.SHORT_DRY_GRASS,
                BlockItemIds.TALL_DRY_GRASS,
                BlockItemIds.BUSH,
                BlockItemIds.FIREFLY_BUSH
            );
        this.tag(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN).add(new BlockItemId[]{BlockItemIds.MAGMA_BLOCK});
        this.tag(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP).add(new BlockItemId[]{BlockItemIds.SOUL_SAND});
        this.tag(BlockTags.PREVENTS_NEARBY_LEAF_DECAY).addTag(BlockTags.LOGS);
        this.tag(BlockTags.SUPPORTS_VEGETATION).addTag(BlockTags.SUBSTRATE_OVERWORLD).add(new BlockItemId[]{BlockItemIds.FARMLAND});
        this.tag(BlockTags.SUPPORTS_DRY_VEGETATION).addTag(BlockTags.SAND).addTag(BlockTags.TERRACOTTA).addTag(BlockTags.SUPPORTS_VEGETATION);
        this.tag(BlockTags.SUPPORTS_CROPS).add(new BlockItemId[]{BlockItemIds.FARMLAND});
        this.tag(BlockTags.SUPPORTS_STEM_CROPS).addTag(BlockTags.SUPPORTS_CROPS);
        this.tag(BlockTags.SUPPORTS_STEM_FRUIT).addTag(BlockTags.SUPPORTS_VEGETATION);
        this.tag(BlockTags.SUPPORTS_PUMPKIN_STEM).addTag(BlockTags.SUPPORTS_STEM_CROPS);
        this.tag(BlockTags.SUPPORTS_MELON_STEM).addTag(BlockTags.SUPPORTS_STEM_CROPS);
        this.tag(BlockTags.SUPPORTS_PUMPKIN_STEM_FRUIT).addTag(BlockTags.SUPPORTS_STEM_FRUIT);
        this.tag(BlockTags.SUPPORTS_MELON_STEM_FRUIT).addTag(BlockTags.SUPPORTS_STEM_FRUIT);
        this.tag(BlockTags.SUPPORTS_SUGAR_CANE).addTag(BlockTags.SUBSTRATE_OVERWORLD).addTag(BlockTags.SAND);
        this.tag(BlockTags.SUPPORTS_SUGAR_CANE_ADJACENTLY).add(BlockIds.FROSTED_ICE);
        this.tag(BlockTags.SUPPORTS_BAMBOO)
            .addTag(BlockTags.SAND)
            .addTag(BlockTags.SUBSTRATE_OVERWORLD)
            .add(new BlockItemId[]{BlockItemIds.BAMBOO})
            .add(BlockIds.BAMBOO_SAPLING)
            .add(BlockItemIds.GRAVEL, BlockItemIds.SUSPICIOUS_GRAVEL);
        this.tag(BlockTags.SUPPORTS_SMALL_DRIPLEAF).add(new BlockItemId[]{BlockItemIds.CLAY}).add(new BlockItemId[]{BlockItemIds.MOSS_BLOCK});
        this.tag(BlockTags.SUPPORTS_BIG_DRIPLEAF)
            .addTag(BlockTags.SUPPORTS_SMALL_DRIPLEAF)
            .add(
                BlockItemIds.DIRT,
                BlockItemIds.GRASS_BLOCK,
                BlockItemIds.PODZOL,
                BlockItemIds.COARSE_DIRT,
                BlockItemIds.MYCELIUM,
                BlockItemIds.ROOTED_DIRT,
                BlockItemIds.MOSS_BLOCK,
                BlockItemIds.MUD,
                BlockItemIds.MUDDY_MANGROVE_ROOTS,
                BlockItemIds.FARMLAND
            );
        this.tag(BlockTags.SUPPORTS_CACTUS).addTag(BlockTags.SAND);
        this.tag(BlockTags.SUPPORTS_CHORUS_PLANT).add(new BlockItemId[]{BlockItemIds.END_STONE});
        this.tag(BlockTags.SUPPORTS_CHORUS_FLOWER).add(new BlockItemId[]{BlockItemIds.END_STONE});
        this.tag(BlockTags.SUPPORTS_NETHER_SPROUTS)
            .addTag(BlockTags.SUPPORTS_VEGETATION)
            .addTag(BlockTags.NYLIUM)
            .add(new BlockItemId[]{BlockItemIds.SOUL_SOIL});
        this.tag(BlockTags.SUPPORTS_AZALEA).addTag(BlockTags.SUPPORTS_VEGETATION).add(new BlockItemId[]{BlockItemIds.CLAY});
        this.tag(BlockTags.SUPPORTS_WARPED_FUNGUS)
            .addTag(BlockTags.SUPPORTS_VEGETATION)
            .addTag(BlockTags.NYLIUM)
            .add(new BlockItemId[]{BlockItemIds.MYCELIUM})
            .add(new BlockItemId[]{BlockItemIds.SOUL_SOIL});
        this.tag(BlockTags.SUPPORTS_CRIMSON_FUNGUS).addTag(BlockTags.SUPPORTS_WARPED_FUNGUS);
        this.tag(BlockTags.SUPPORTS_MANGROVE_PROPAGULE).addTag(BlockTags.SUPPORTS_VEGETATION).add(new BlockItemId[]{BlockItemIds.CLAY});
        this.tag(BlockTags.SUPPORTS_HANGING_MANGROVE_PROPAGULE).add(new BlockItemId[]{BlockItemIds.MANGROVE_LEAVES});
        this.tag(BlockTags.SUPPORTS_NETHER_WART).add(new BlockItemId[]{BlockItemIds.SOUL_SAND});
        this.tag(BlockTags.SUPPORTS_WARPED_ROOTS).addTag(BlockTags.SUPPORTS_VEGETATION).addTag(BlockTags.NYLIUM).add(new BlockItemId[]{BlockItemIds.SOUL_SOIL});
        this.tag(BlockTags.SUPPORTS_CRIMSON_ROOTS).addTag(BlockTags.SUPPORTS_WARPED_ROOTS);
        this.tag(BlockTags.SUPPORTS_WITHER_ROSE)
            .addTag(BlockTags.SUPPORTS_VEGETATION)
            .add(new BlockItemId[]{BlockItemIds.NETHERRACK})
            .add(new BlockItemId[]{BlockItemIds.SOUL_SAND})
            .add(new BlockItemId[]{BlockItemIds.SOUL_SOIL});
        this.tag(BlockTags.SUPPORTS_COCOA).addTag(BlockTags.JUNGLE_LOGS);
        this.tag(BlockTags.SUPPORTS_LILY_PAD).add(new BlockItemId[]{BlockItemIds.ICE}).add(BlockIds.FROSTED_ICE);
        this.tag(BlockTags.SUPPORTS_FROGSPAWN);
        this.tag(BlockTags.SUPPORT_OVERRIDE_CACTUS_FLOWER).add(new BlockItemId[]{BlockItemIds.CACTUS}).add(new BlockItemId[]{BlockItemIds.FARMLAND});
        this.tag(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER)
            .add(new BlockItemId[]{BlockItemIds.HONEY_BLOCK})
            .add(new BlockItemId[]{BlockItemIds.SOUL_SAND})
            .add(new BlockItemId[]{BlockItemIds.MUD});
        this.tag(BlockTags.CANNOT_SUPPORT_SEAGRASS).add(new BlockItemId[]{BlockItemIds.MAGMA_BLOCK});
        this.tag(BlockTags.CANNOT_SUPPORT_KELP).add(new BlockItemId[]{BlockItemIds.MAGMA_BLOCK});
        this.tag(BlockTags.CANNOT_SUPPORT_SNOW_LAYER)
            .add(new BlockItemId[]{BlockItemIds.ICE})
            .add(new BlockItemId[]{BlockItemIds.PACKED_ICE})
            .add(new BlockItemId[]{BlockItemIds.BARRIER});
        this.tag(BlockTags.OVERRIDES_MUSHROOM_LIGHT_REQUIREMENT)
            .add(new BlockItemId[]{BlockItemIds.MYCELIUM})
            .add(new BlockItemId[]{BlockItemIds.PODZOL})
            .add(new BlockItemId[]{BlockItemIds.CRIMSON_NYLIUM})
            .add(new BlockItemId[]{BlockItemIds.WARPED_NYLIUM});
        this.tag(BlockTags.GROWS_CROPS).add(new BlockItemId[]{BlockItemIds.FARMLAND});
        this.tag(BlockTags.SNAPS_GOAT_HORN)
            .addTag(BlockTags.OVERWORLD_NATURAL_LOGS)
            .add(new BlockItemId[]{BlockItemIds.STONE})
            .add(new BlockItemId[]{BlockItemIds.PACKED_ICE})
            .add(new BlockItemId[]{BlockItemIds.IRON_ORE})
            .add(new BlockItemId[]{BlockItemIds.COAL_ORE})
            .add(new BlockItemId[]{BlockItemIds.COPPER_ORE})
            .add(new BlockItemId[]{BlockItemIds.EMERALD_ORE});
        this.tag(BlockTags.INVALID_SPAWN_INSIDE).add(BlockIds.END_PORTAL, BlockIds.END_GATEWAY);
        this.tag(BlockTags.TRAIL_RUINS_REPLACEABLE).add(new BlockItemId[]{BlockItemIds.GRAVEL});
        this.tag(BlockTags.SNIFFER_DIGGABLE_BLOCK)
            .addTag(BlockTags.DIRT)
            .addTag(BlockTags.MUD)
            .addTag(BlockTags.MOSS_BLOCKS)
            .add(BlockItemIds.GRASS_BLOCK, BlockItemIds.PODZOL);
        this.tag(BlockTags.SNIFFER_EGG_HATCH_BOOST).add(new BlockItemId[]{BlockItemIds.MOSS_BLOCK});
        this.tag(BlockTags.REPLACEABLE)
            .addAll(
                registries.lookupOrThrow(Registries.BLOCK).listElements().filter(b -> b.value().defaultBlockState().canBeReplaced()).map(Holder.Reference::key)
            );
        this.tag(BlockTags.ENCHANTMENT_POWER_PROVIDER).add(new BlockItemId[]{BlockItemIds.BOOKSHELF});
        this.tag(BlockTags.ENCHANTMENT_POWER_TRANSMITTER).addTag(BlockTags.REPLACEABLE);
        this.tag(BlockTags.MAINTAINS_FARMLAND)
            .add(new BlockItemId[]{BlockItemIds.PUMPKIN_CROP})
            .add(BlockIds.ATTACHED_PUMPKIN_STEM)
            .add(new BlockItemId[]{BlockItemIds.MELON_CROP})
            .add(BlockIds.ATTACHED_MELON_STEM)
            .add(
                BlockItemIds.BEETROOT_CROP,
                BlockItemIds.CARROT_CROP,
                BlockItemIds.POTATO_CROP,
                BlockItemIds.TORCHFLOWER_CROP,
                BlockItemIds.TORCHFLOWER,
                BlockItemIds.PITCHER_CROP,
                BlockItemIds.WHEAT_CROP
            )
            .add(BlockIds.MOVING_PISTON)
            .addTag(BlockTags.FENCE_GATES);
        this.tag(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS).addTag(BlockTags.SAND).addTag(BlockTags.CONCRETE_POWDERS);
        this.tag(BlockTags.HAPPY_GHAST_AVOIDS)
            .add(BlockItemIds.SWEET_BERRY_CROP, BlockItemIds.CACTUS, BlockItemIds.WITHER_ROSE, BlockItemIds.MAGMA_BLOCK)
            .add(BlockIds.FIRE)
            .addTag(BlockTags.SPELEOTHEMS);
        this.tag(BlockTags.DOES_NOT_BLOCK_HOPPERS).addTag(BlockTags.BEEHIVES);
        this.tag(BlockTags.SUPPRESSES_BOUNCE).add(new BlockItemId[]{BlockItemIds.HONEY_BLOCK});
        this.tag(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).add(BlockItemIds.BARRIER, BlockItemIds.BEDROCK);
        this.tag(BlockTags.TRIGGERS_AMBIENT_DESERT_SAND_BLOCK_SOUNDS).add(BlockItemIds.SAND, BlockItemIds.RED_SAND);
        this.tag(BlockTags.TRIGGERS_AMBIENT_DESERT_DRY_VEGETATION_BLOCK_SOUNDS).addTag(BlockTags.TERRACOTTA).add(BlockItemIds.SAND, BlockItemIds.RED_SAND);
        this.tag(BlockTags.TRIGGERS_AMBIENT_DRIED_GHAST_BLOCK_SOUNDS).add(BlockItemIds.SOUL_SAND, BlockItemIds.SOUL_SOIL);
        this.tag(BlockTags.SPELEOTHEMS).add(BlockItemIds.POINTED_DRIPSTONE, BlockItemIds.SULFUR_SPIKE);
        this.tag(BlockTags.SULFUR_SPIKE_REPLACEABLE).add(BlockItemIds.SULFUR, BlockItemIds.CINNABAR);
        this.tag(BlockTags.FOX_IMMUNE_TO).add(new BlockItemId[]{BlockItemIds.SWEET_BERRY_CROP});
        this.tag(BlockTags.POLAR_BEAR_IMMUNE_TO).add(new BlockItemId[]{BlockItemIds.POWDER_SNOW});
        this.tag(BlockTags.SNOW_GOLEM_IMMUNE_TO).add(new BlockItemId[]{BlockItemIds.POWDER_SNOW});
        this.tag(BlockTags.STRAY_IMMUNE_TO).add(new BlockItemId[]{BlockItemIds.POWDER_SNOW});
        this.tag(BlockTags.WITHER_IMMUNE_TO).add(new BlockItemId[]{BlockItemIds.WITHER_ROSE});
        this.tag(BlockTags.WITHER_SKELETON_IMMUNE_TO).add(new BlockItemId[]{BlockItemIds.WITHER_ROSE});
        this.tag(BlockTags.DEFAULT_IMMUNE_TO);
        this.tag(BlockTags.AIR).add(new BlockItemId[]{BlockItemIds.AIR}).add(BlockIds.VOID_AIR, BlockIds.CAVE_AIR);
    }

    private static ColorCollection<ResourceKey<Block>> toIds(final ColorCollection<BlockItemId> ids) {
        return ids.map(BlockItemId::block);
    }

    private static WeatheringCopperCollection<ResourceKey<Block>> toIds(final WeatheringCopperCollection<BlockItemId> ids) {
        return ids.map(BlockItemId::block);
    }
}