package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.VillagerTradeTags;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.item.trading.VillagerTrades;

public class VillagerTradesTagsProvider extends TagsProvider<VillagerTrade> {
   public VillagerTradesTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookupProvider) {
      super(output, Registries.VILLAGER_TRADE, lookupProvider);
   }

   @Override
   protected void addTags(final HolderLookup.Provider registries) {
      this.tag(VillagerTradeTags.FARMER_LEVEL_1)
         .add(
            VillagerTrades.FARMER_1_WHEAT_EMERALD,
            VillagerTrades.FARMER_1_POTATO_EMERALD,
            VillagerTrades.FARMER_1_CARROT_EMERALD,
            VillagerTrades.FARMER_1_BEETROOT_EMERALD,
            VillagerTrades.FARMER_1_EMERALD_BREAD
         );
      this.tag(VillagerTradeTags.FARMER_LEVEL_2)
         .add(VillagerTrades.FARMER_2_PUMPKIN_EMERALD, VillagerTrades.FARMER_2_EMERALD_PUMPKIN_PIE, VillagerTrades.FARMER_2_EMERALD_APPLE);
      this.tag(VillagerTradeTags.FARMER_LEVEL_3).add(VillagerTrades.FARMER_3_EMERALD_COOKIE, VillagerTrades.FARMER_3_MELON_EMERALD);
      this.tag(VillagerTradeTags.FARMER_LEVEL_4).add(VillagerTrades.FARMER_4_EMERALD_CAKE, VillagerTrades.FARMER_4_EMERALD_SUSPICIOUS_STEW);
      this.tag(VillagerTradeTags.FARMER_LEVEL_5).add(VillagerTrades.FARMER_5_EMERALD_GOLDEN_CARROT, VillagerTrades.FARMER_5_EMERALD_GLISTENING_MELON_SLICE);
      this.tag(VillagerTradeTags.FISHERMAN_LEVEL_1)
         .add(
            VillagerTrades.FISHERMAN_1_STRING_EMERALD,
            VillagerTrades.FISHERMAN_1_COAL_EMERALD,
            VillagerTrades.FISHERMAN_1_RAW_COD_AND_EMERALD_COOKED_COD,
            VillagerTrades.FISHERMAN_1_EMERALD_COD_BUCKET
         );
      this.tag(VillagerTradeTags.FISHERMAN_LEVEL_2)
         .add(VillagerTrades.FISHERMAN_2_COD_EMERALD, VillagerTrades.FISHERMAN_2_SALMON_AND_EMERALD_COOKED_SALMON, VillagerTrades.FISHERMAN_2_EMERALD_CAMPFIRE);
      this.tag(VillagerTradeTags.FISHERMAN_LEVEL_3).add(VillagerTrades.FISHERMAN_3_SALMON_EMERALD, VillagerTrades.FISHERMAN_3_EMERALD_ENCHANTED_FISHING_ROD);
      this.tag(VillagerTradeTags.FISHERMAN_LEVEL_4).add(VillagerTrades.FISHERMAN_4_TROPICAL_FISH_EMERALD);
      this.tag(VillagerTradeTags.FISHERMAN_LEVEL_5)
         .add(
            VillagerTrades.FISHERMAN_5_PUFFERFISH_EMERALD,
            VillagerTrades.FISHERMAN_5_OAK_BOAT_EMERALD,
            VillagerTrades.FISHERMAN_5_SPRUCE_BOAT_EMERALD,
            VillagerTrades.FISHERMAN_5_JUNGLE_BOAT_EMERALD,
            VillagerTrades.FISHERMAN_5_ACACIA_BOAT_EMERALD,
            VillagerTrades.FISHERMAN_5_DARK_OAK_BOAT_EMERALD
         );
      this.tag(VillagerTradeTags.SHEPHERD_LEVEL_1)
         .add(
            VillagerTrades.SHEPHERD_1_WHITE_WOOL_EMERALD,
            VillagerTrades.SHEPHERD_1_BROWN_WOOL_EMERALD,
            VillagerTrades.SHEPHERD_1_GRAY_WOOL_EMERALD,
            VillagerTrades.SHEPHERD_1_BLACK_WOOL_EMERALD,
            VillagerTrades.SHEPHERD_1_EMERALD_SHEARS
         );
      this.tag(VillagerTradeTags.SHEPHERD_LEVEL_2)
         .add(
            VillagerTrades.SHEPHERD_2_WHITE_DYE_EMERALD,
            VillagerTrades.SHEPHERD_2_GRAY_DYE_EMERALD,
            VillagerTrades.SHEPHERD_2_BLACK_DYE_EMERALD,
            VillagerTrades.SHEPHERD_2_LIGHT_BLUE_DYE_EMERALD,
            VillagerTrades.SHEPHERD_2_LIME_DYE_EMERALD
         )
         .addAll(VillagerTrades.SHEPHERD_2_EMERALD_WOOL.asList())
         .addAll(VillagerTrades.SHEPHERD_2_EMERALD_CARPETS.asList());
      this.tag(VillagerTradeTags.SHEPHERD_LEVEL_3)
         .add(
            VillagerTrades.SHEPHERD_3_YELLOW_DYE_EMERALD,
            VillagerTrades.SHEPHERD_3_LIGHT_GRAY_DYE_EMERALD,
            VillagerTrades.SHEPHERD_3_ORANGE_DYE_EMERALD,
            VillagerTrades.SHEPHERD_3_RED_DYE_EMERALD,
            VillagerTrades.SHEPHERD_3_PINK_DYE_EMERALD
         )
         .addAll(VillagerTrades.SHEPHERD_3_EMERALD_BED.asList());
      this.tag(VillagerTradeTags.SHEPHERD_LEVEL_4)
         .add(
            VillagerTrades.SHEPHERD_4_BROWN_DYE_EMERALD,
            VillagerTrades.SHEPHERD_4_PURPLE_DYE_EMERALD,
            VillagerTrades.SHEPHERD_4_BLUE_DYE_EMERALD,
            VillagerTrades.SHEPHERD_4_GREEN_DYE_EMERALD,
            VillagerTrades.SHEPHERD_4_MAGENTA_DYE_EMERALD,
            VillagerTrades.SHEPHERD_4_CYAN_DYE_EMERALD
         )
         .addAll(VillagerTrades.SHEPHERD_4_EMERALD_BANNER.asList());
      this.tag(VillagerTradeTags.SHEPHERD_LEVEL_5).add(VillagerTrades.SHEPHERD_5_EMERALD_PAINTING);
      this.tag(VillagerTradeTags.FLETCHER_LEVEL_1)
         .add(VillagerTrades.FLETCHER_1_STICK_EMERALD, VillagerTrades.FLETCHER_1_EMERALD_ARROW, VillagerTrades.FLETCHER_1_GRAVEL_AND_EMERALD_FLINT);
      this.tag(VillagerTradeTags.FLETCHER_LEVEL_2).add(VillagerTrades.FLETCHER_2_FLINT_EMERALD, VillagerTrades.FLETCHER_2_EMERALD_BOW);
      this.tag(VillagerTradeTags.FLETCHER_LEVEL_3).add(VillagerTrades.FLETCHER_3_STRING_EMERALD, VillagerTrades.FLETCHER_3_EMERALD_CROSSBOW);
      this.tag(VillagerTradeTags.FLETCHER_LEVEL_4).add(VillagerTrades.FLETCHER_4_FEATHER_EMERALD, VillagerTrades.FLETCHER_4_EMERALD_ENCHANTED_BOW);
      this.tag(VillagerTradeTags.FLETCHER_LEVEL_5)
         .add(
            VillagerTrades.FLETCHER_5_TRIPWIRE_HOOK_EMERALD,
            VillagerTrades.FLETCHER_5_EMERALD_ENCHANTED_CROSSBOW,
            VillagerTrades.FLETCHER_5_ARROW_AND_EMERALD_TIPPED_ARROW
         );
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_1)
         .add(
            VillagerTrades.LIBRARIAN_1_PAPER_EMERALD, VillagerTrades.LIBRARIAN_1_EMERALD_AND_BOOK_ENCHANTED_BOOK, VillagerTrades.LIBRARIAN_1_EMERALD_BOOKSHELF
         );
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_2)
         .add(VillagerTrades.LIBRARIAN_2_BOOK_EMERALD, VillagerTrades.LIBRARIAN_2_EMERALD_AND_BOOK_ENCHANTED_BOOK, VillagerTrades.LIBRARIAN_2_EMERALD_LANTERN);
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_3)
         .add(VillagerTrades.LIBRARIAN_3_INK_SAC_EMERALD, VillagerTrades.LIBRARIAN_3_EMERALD_AND_BOOK_ENCHANTED_BOOK, VillagerTrades.LIBRARIAN_3_EMERALD_GLASS);
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_4)
         .add(
            VillagerTrades.LIBRARIAN_4_WRITABLE_BOOK_EMERALD,
            VillagerTrades.LIBRARIAN_4_EMERALD_AND_BOOK_ENCHANTED_BOOK,
            VillagerTrades.LIBRARIAN_4_EMERALD_CLOCK,
            VillagerTrades.LIBRARIAN_4_EMERALD_COMPASS
         );
      this.tag(VillagerTradeTags.LIBRARIAN_LEVEL_5).add(VillagerTrades.LIBRARIAN_5_EMERALD_YELLOW_CANDLE, VillagerTrades.LIBRARIAN_5_EMERALD_RED_CANDLE);
      this.tag(VillagerTradeTags.CARTOGRAPHER_LEVEL_1).add(VillagerTrades.CARTOGRAPHER_1_PAPER_EMERALD, VillagerTrades.CARTOGRAPHER_1_EMERALD_MAP);
      this.tag(VillagerTradeTags.CARTOGRAPHER_LEVEL_2)
         .add(
            VillagerTrades.CARTOGRAPHER_2_GLASS_PANE_EMERALD,
            VillagerTrades.CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_TAIGA_MAP,
            VillagerTrades.CARTOGRAPHER_2_EMERALD_AND_COMPASS_EXPLORER_SWAMP_MAP,
            VillagerTrades.CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_SNOWY_MAP,
            VillagerTrades.CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_SAVANNA_MAP,
            VillagerTrades.CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_PLAINS_MAP,
            VillagerTrades.CARTOGRAPHER_2_EMERALD_AND_COMPASS_EXPLORER_JUNGLE_MAP,
            VillagerTrades.CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_DESERT_MAP
         );
      this.tag(VillagerTradeTags.CARTOGRAPHER_LEVEL_3)
         .add(
            VillagerTrades.CARTOGRAPHER_3_COMPASS_EMERALD,
            VillagerTrades.CARTOGRAPHER_3_EMERALD_AND_COMPASS_OCEAN_EXPLORER_MAP,
            VillagerTrades.CARTOGRAPHER_3_EMERALD_AND_COMPASS_TRIAL_CHAMBER_MAP
         );
      this.tag(VillagerTradeTags.CARTOGRAPHER_LEVEL_4)
         .add(
            VillagerTrades.CARTOGRAPHER_4_EMERALD_ITEM_FRAME,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_WHITE_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_ORANGE_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_MAGENTA_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_BLUE_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_LIGHT_BLUE_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_YELLOW_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_LIME_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_PINK_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_GRAY_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_CYAN_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_PURPLE_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_BROWN_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_GREEN_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_RED_BANNER,
            VillagerTrades.CARTOGRAPHER_4_EMERALD_BLACK_BANNER
         );
      this.tag(VillagerTradeTags.CARTOGRAPHER_LEVEL_5)
         .add(VillagerTrades.CARTOGRAPHER_5_EMERALD_GLOBE_BANNER_PATTERN, VillagerTrades.CARTOGRAPHER_5_EMERALD_AND_COMPASS_WOODLAND_MANSION_MAP);
      this.tag(VillagerTradeTags.CLERIC_LEVEL_1).add(VillagerTrades.CLERIC_1_ROTTEN_FLESH_EMERALD, VillagerTrades.CLERIC_1_EMERALD_REDSTONE);
      this.tag(VillagerTradeTags.CLERIC_LEVEL_2).add(VillagerTrades.CLERIC_2_GOLD_INGOT_EMERALD, VillagerTrades.CLERIC_2_EMERALD_LAPIS_LAZULI);
      this.tag(VillagerTradeTags.CLERIC_LEVEL_3).add(VillagerTrades.CLERIC_3_RABBIT_FOOT_EMERALD, VillagerTrades.CLERIC_3_EMERALD_GLOWSTONE);
      this.tag(VillagerTradeTags.CLERIC_LEVEL_4)
         .add(VillagerTrades.CLERIC_4_TURTLE_SCUTE_EMERALD, VillagerTrades.CLERIC_4_GLASS_BOTTLE_EMERALD, VillagerTrades.CLERIC_4_EMERALD_ENDER_PEARL);
      this.tag(VillagerTradeTags.CLERIC_LEVEL_5).add(VillagerTrades.CLERIC_5_NETHER_WART_EMERALD, VillagerTrades.CLERIC_5_EMERALD_EXPERIENCE_BOTTLE);
      this.tag(VillagerTradeTags.COMMON_SMITH_LEVEL_1).add(VillagerTrades.COMMON_SMITH_1_COAL_EMERALD);
      this.tag(VillagerTradeTags.COMMON_SMITH_LEVEL_2).add(VillagerTrades.COMMON_SMITH_2_IRON_INGOT_EMERALD, VillagerTrades.COMMON_SMITH_2_EMERALD_BELL);
      this.tag(VillagerTradeTags.COMMON_SMITH_LEVEL_3);
      this.tag(VillagerTradeTags.COMMON_SMITH_LEVEL_4);
      this.tag(VillagerTradeTags.COMMON_SMITH_LEVEL_5);
      this.tag(VillagerTradeTags.ARMORER_LEVEL_1)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_1)
         .add(
            VillagerTrades.ARMORER_1_EMERALD_IRON_LEGGINGS,
            VillagerTrades.ARMORER_1_EMERALD_IRON_BOOTS,
            VillagerTrades.ARMORER_1_EMERALD_IRON_HELMET,
            VillagerTrades.ARMORER_1_EMERALD_IRON_CHESTPLATE
         );
      this.tag(VillagerTradeTags.ARMORER_LEVEL_2)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_2)
         .add(VillagerTrades.ARMORER_2_EMERALD_CHAINMAIL_BOOTS, VillagerTrades.ARMORER_2_EMERALD_CHAINMAIL_LEGGINGS);
      this.tag(VillagerTradeTags.ARMORER_LEVEL_3)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_3)
         .add(
            VillagerTrades.ARMORER_3_LAVA_BUCKET_EMERALD,
            VillagerTrades.ARMORER_3_EMERALD_CHAINMAIL_HELMET,
            VillagerTrades.ARMORER_3_EMERALD_CHAINMAIL_CHESTPLATE,
            VillagerTrades.ARMORER_3_EMERALD_SHIELD,
            VillagerTrades.ARMORER_3_DIAMOND_EMERALD
         );
      this.tag(VillagerTradeTags.ARMORER_LEVEL_4)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_4)
         .add(VillagerTrades.ARMORER_4_EMERALD_ENCHANTED_DIAMOND_LEGGINGS, VillagerTrades.ARMORER_4_EMERALD_ENCHANTED_DIAMOND_BOOTS);
      this.tag(VillagerTradeTags.ARMORER_LEVEL_5)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_5)
         .add(VillagerTrades.ARMORER_5_EMERALD_ENCHANTED_DIAMOND_HELMET, VillagerTrades.ARMORER_5_EMERALD_ENCHANTED_DIAMOND_CHESTPLATE);
      this.tag(VillagerTradeTags.WEAPONSMITH_LEVEL_1)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_1)
         .add(VillagerTrades.WEAPONSMITH_1_EMERALD_IRON_AXE, VillagerTrades.WEAPONSMITH_1_EMERALD_ENCHANTED_IRON_SWORD);
      this.tag(VillagerTradeTags.WEAPONSMITH_LEVEL_2).addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_2);
      this.tag(VillagerTradeTags.WEAPONSMITH_LEVEL_3).addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_3).add(VillagerTrades.WEAPONSMITH_3_FLINT_EMERALD);
      this.tag(VillagerTradeTags.WEAPONSMITH_LEVEL_4)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_4)
         .add(VillagerTrades.WEAPONSMITH_4_EMERALD_ENCHANTED_DIAMOND_AXE, VillagerTrades.WEAPONSMITH_4_DIAMOND_EMERALD);
      this.tag(VillagerTradeTags.WEAPONSMITH_LEVEL_5)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_5)
         .add(VillagerTrades.WEAPONSMITH_5_EMERALD_ENCHANTED_DIAMOND_SWORD);
      this.tag(VillagerTradeTags.TOOLSMITH_LEVEL_1)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_1)
         .add(
            VillagerTrades.TOOLSMITH_1_EMERALD_STONE_AXE,
            VillagerTrades.TOOLSMITH_1_EMERALD_STONE_SHOVEL,
            VillagerTrades.TOOLSMITH_1_EMERALD_STONE_PICKAXE,
            VillagerTrades.TOOLSMITH_1_EMERALD_STONE_HOE
         );
      this.tag(VillagerTradeTags.TOOLSMITH_LEVEL_2).addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_2);
      this.tag(VillagerTradeTags.TOOLSMITH_LEVEL_3)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_3)
         .add(
            VillagerTrades.TOOLSMITH_3_FLINT_EMERALD,
            VillagerTrades.TOOLSMITH_3_EMERALD_IRON_AXE,
            VillagerTrades.TOOLSMITH_3_EMERALD_IRON_SHOVEL,
            VillagerTrades.TOOLSMITH_3_EMERALD_IRON_PICKAXE,
            VillagerTrades.TOOLSMITH_3_EMERALD_DIAMOND_HOE
         );
      this.tag(VillagerTradeTags.TOOLSMITH_LEVEL_4)
         .addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_4)
         .add(VillagerTrades.TOOLSMITH_4_EMERALD_DIAMOND_AXE, VillagerTrades.TOOLSMITH_4_EMERALD_DIAMOND_SHOVEL, VillagerTrades.TOOLSMITH_4_DIAMOND_EMERALD);
      this.tag(VillagerTradeTags.TOOLSMITH_LEVEL_5).addTag(VillagerTradeTags.COMMON_SMITH_LEVEL_5).add(VillagerTrades.TOOLSMITH_5_EMERALD_DIAMOND_PICKAXE);
      this.tag(VillagerTradeTags.BUTCHER_LEVEL_1)
         .add(
            VillagerTrades.BUTCHER_1_CHICKEN_EMERALD,
            VillagerTrades.BUTCHER_1_PORKCHOP_EMERALD,
            VillagerTrades.BUTCHER_1_RABBIT_EMERALD,
            VillagerTrades.BUTCHER_1_EMERALD_RABBIT_STEW
         );
      this.tag(VillagerTradeTags.BUTCHER_LEVEL_2)
         .add(VillagerTrades.BUTCHER_2_COAL_EMERALD, VillagerTrades.BUTCHER_2_EMERALD_COOKED_PORKCHOP, VillagerTrades.BUTCHER_2_EMERALD_COOKED_CHICKEN);
      this.tag(VillagerTradeTags.BUTCHER_LEVEL_3).add(VillagerTrades.BUTCHER_3_MUTTON_EMERALD, VillagerTrades.BUTCHER_3_BEEF_EMERALD);
      this.tag(VillagerTradeTags.BUTCHER_LEVEL_4).add(VillagerTrades.BUTCHER_4_DRIED_KELP_BLOCK_EMERALD);
      this.tag(VillagerTradeTags.BUTCHER_LEVEL_5).add(VillagerTrades.BUTCHER_5_SWEET_BERRIES_EMERALD);
      this.tag(VillagerTradeTags.LEATHERWORKER_LEVEL_1)
         .add(
            VillagerTrades.LEATHERWORKER_1_LEATHER_EMERALD,
            VillagerTrades.LEATHERWORKER_1_EMERALD_DYED_LEATHER_LEGGINGS,
            VillagerTrades.LEATHERWORKER_1_EMERALD_DYED_LEATHER_CHESTPLATE
         );
      this.tag(VillagerTradeTags.LEATHERWORKER_LEVEL_2)
         .add(
            VillagerTrades.LEATHERWORKER_2_FLINT_EMERALD,
            VillagerTrades.LEATHERWORKER_2_EMERALD_DYED_LEATHER_HELMET,
            VillagerTrades.LEATHERWORKER_2_EMERALD_DYED_LEATHER_BOOTS
         );
      this.tag(VillagerTradeTags.LEATHERWORKER_LEVEL_3)
         .add(VillagerTrades.LEATHERWORKER_3_RABBIT_HIDE_EMERALD, VillagerTrades.LEATHERWORKER_3_EMERALD_DYED_LEATHER_CHESTPLATE);
      this.tag(VillagerTradeTags.LEATHERWORKER_LEVEL_4)
         .add(VillagerTrades.LEATHERWORKER_4_TURTLE_SCUTE_EMERALD, VillagerTrades.LEATHERWORKER_4_EMERALD_DYED_LEATHER_HORSE_ARMOR);
      this.tag(VillagerTradeTags.LEATHERWORKER_LEVEL_5)
         .add(VillagerTrades.LEATHERWORKER_5_EMERALD_SADDLE, VillagerTrades.LEATHERWORKER_5_EMERALD_DYED_LEATHER_HELMET);
      this.tag(VillagerTradeTags.MASON_LEVEL_1).add(VillagerTrades.MASON_1_CLAY_BALL_EMERALD, VillagerTrades.MASON_1_EMERALD_BRICK);
      this.tag(VillagerTradeTags.MASON_LEVEL_2).add(VillagerTrades.MASON_2_STONE_EMERALD, VillagerTrades.MASON_2_EMERALD_CHISELED_STONE_BRICKS);
      this.tag(VillagerTradeTags.MASON_LEVEL_3)
         .add(
            VillagerTrades.MASON_3_GRANITE_EMERALD,
            VillagerTrades.MASON_3_ANDESITE_EMERALD,
            VillagerTrades.MASON_3_DIORITE_EMERALD,
            VillagerTrades.MASON_3_EMERALD_DRIPSTONE_BLOCK,
            VillagerTrades.MASON_3_EMERALD_POLISHED_ANDESITE,
            VillagerTrades.MASON_3_EMERALD_POLISHED_DIORITE,
            VillagerTrades.MASON_3_EMERALD_POLISHED_GRANTITE
         );
      this.tag(VillagerTradeTags.MASON_LEVEL_4)
         .add(VillagerTrades.MASON_4_QUARTZ_EMERALD)
         .addAll(VillagerTrades.MASON_4_EMERALD_TERRACOTTA.asList())
         .addAll(VillagerTrades.MASON_4_EMERALD_GLAZED_TERRACOTTA.asList());
      this.tag(VillagerTradeTags.MASON_LEVEL_5).add(VillagerTrades.MASON_5_EMERALD_QUARTZ_PILLAR, VillagerTrades.MASON_5_EMERALD_QUARTZ_BLOCK);
      this.tag(VillagerTradeTags.WANDERING_TRADER_BUYING)
         .add(
            VillagerTrades.WANDERING_TRADER_WATER_BOTTLE_EMERALD,
            VillagerTrades.WANDERING_TRADER_WATER_BUCKET_EMERALD,
            VillagerTrades.WANDERING_TRADER_MILK_BUCKET_EMERALD,
            VillagerTrades.WANDERING_TRADER_FERMENTED_SPIDER_EYE_EMERALD,
            VillagerTrades.WANDERING_TRADER_BAKED_POTATO_EMERALD,
            VillagerTrades.WANDERING_TRADER_HAY_BLOCK_EMERALD
         );
      this.tag(VillagerTradeTags.WANDERING_TRADER_UNCOMMON)
         .add(
            VillagerTrades.WANDERING_TRADER_EMERALD_PACKED_ICE,
            VillagerTrades.WANDERING_TRADER_EMERALD_BLUE_ICE,
            VillagerTrades.WANDERING_TRADER_EMERALD_GUNPOWDER,
            VillagerTrades.WANDERING_TRADER_EMERALD_PODZOL,
            VillagerTrades.WANDERING_TRADER_EMERALD_ACACIA_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_BIRCH_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_DARK_OAK_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_JUNGLE_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_OAK_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_SPRUCE_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_CHERRY_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_MANGROVE_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_PALE_OAK_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_POPLAR_LOG,
            VillagerTrades.WANDERING_TRADER_EMERALD_ENCHANTED_IRON_PICKAXE,
            VillagerTrades.WANDERING_TRADER_EMERALD_LONG_INVISIBILITY_POTION
         );
      this.tag(VillagerTradeTags.WANDERING_TRADER_COMMON)
         .addAll(VillagerTrades.WANDERING_TRADER_EMERALD_DYE.asList())
         .add(
            VillagerTrades.WANDERING_TRADER_EMERALD_TROPICAL_FISH_BUCKET,
            VillagerTrades.WANDERING_TRADER_EMERALD_PUFFERFISH_BUCKET,
            VillagerTrades.WANDERING_TRADER_EMERALD_SEA_PICKLE,
            VillagerTrades.WANDERING_TRADER_EMERALD_SLIME_BALL,
            VillagerTrades.WANDERING_TRADER_EMERALD_GLOWSTONE,
            VillagerTrades.WANDERING_TRADER_EMERALD_NAUTILUS_SHELL,
            VillagerTrades.WANDERING_TRADER_EMERALD_FERN,
            VillagerTrades.WANDERING_TRADER_EMERALD_SUGAR_CANE,
            VillagerTrades.WANDERING_TRADER_EMERALD_PUMPKIN,
            VillagerTrades.WANDERING_TRADER_EMERALD_KELP,
            VillagerTrades.WANDERING_TRADER_EMERALD_CACTUS,
            VillagerTrades.WANDERING_TRADER_EMERALD_DANDELION,
            VillagerTrades.WANDERING_TRADER_EMERALD_POPPY,
            VillagerTrades.WANDERING_TRADER_EMERALD_BLUE_ORCHID,
            VillagerTrades.WANDERING_TRADER_EMERALD_ALLIUM,
            VillagerTrades.WANDERING_TRADER_EMERALD_AZURE_BLUET,
            VillagerTrades.WANDERING_TRADER_EMERALD_RED_TULIP,
            VillagerTrades.WANDERING_TRADER_EMERALD_ORANGE_TULIP,
            VillagerTrades.WANDERING_TRADER_EMERALD_WHITE_TULIP,
            VillagerTrades.WANDERING_TRADER_EMERALD_PINK_TULIP,
            VillagerTrades.WANDERING_TRADER_EMERALD_OXEYE_DAISY,
            VillagerTrades.WANDERING_TRADER_EMERALD_CORNFLOWER,
            VillagerTrades.WANDERING_TRADER_EMERALD_LILY_OF_THE_VALLEY,
            VillagerTrades.WANDERING_TRADER_EMERALD_OPEN_EYEBLOSSOM,
            VillagerTrades.WANDERING_TRADER_EMERALD_WHEAT_SEEDS,
            VillagerTrades.WANDERING_TRADER_EMERALD_BEETROOT_SEEDS,
            VillagerTrades.WANDERING_TRADER_EMERALD_PUMPKIN_SEEDS,
            VillagerTrades.WANDERING_TRADER_EMERALD_MELON_SEEDS,
            VillagerTrades.WANDERING_TRADER_EMERALD_ACACIA_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_BIRCH_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_DARK_OAK_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_JUNGLE_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_OAK_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_SPRUCE_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_CHERRY_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_PALE_OAK_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_MANGROVE_PROPAGULE,
            VillagerTrades.WANDERING_TRADER_EMERALD_POPLAR_SAPLING,
            VillagerTrades.WANDERING_TRADER_EMERALD_BRAIN_CORAL_BLOCK,
            VillagerTrades.WANDERING_TRADER_EMERALD_BUBBLE_CORAL_BLOCK,
            VillagerTrades.WANDERING_TRADER_EMERALD_FIRE_CORAL_BLOCK,
            VillagerTrades.WANDERING_TRADER_EMERALD_HORN_CORAL_BLOCK,
            VillagerTrades.WANDERING_TRADER_EMERALD_TUBE_CORAL_BLOCK,
            VillagerTrades.WANDERING_TRADER_EMERALD_VINE,
            VillagerTrades.WANDERING_TRADER_EMERALD_PALE_HANGING_MOSS,
            VillagerTrades.WANDERING_TRADER_EMERALD_BROWN_MUSHROOM,
            VillagerTrades.WANDERING_TRADER_EMERALD_RED_MUSHROOM,
            VillagerTrades.WANDERING_TRADER_EMERALD_LILY_PAD,
            VillagerTrades.WANDERING_TRADER_EMERALD_SMALL_DRIPLEAF,
            VillagerTrades.WANDERING_TRADER_EMERALD_SAND,
            VillagerTrades.WANDERING_TRADER_EMERALD_RED_SAND,
            VillagerTrades.WANDERING_TRADER_EMERALD_POINTED_DRIPSTONE,
            VillagerTrades.WANDERING_TRADER_EMERALD_SULFUR_SPIKE,
            VillagerTrades.WANDERING_TRADER_EMERALD_ROOTED_DIRT,
            VillagerTrades.WANDERING_TRADER_EMERALD_MOSS_BLOCK,
            VillagerTrades.WANDERING_TRADER_EMERALD_PALE_MOSS_BLOCK,
            VillagerTrades.WANDERING_TRADER_EMERALD_WILDFLOWERS,
            VillagerTrades.WANDERING_TRADER_EMERALD_DRY_TALL_GRASS,
            VillagerTrades.WANDERING_TRADER_EMERALD_FIREFLY_BUSH,
            VillagerTrades.WANDERING_TRADER_EMERALD_GOLDEN_DANDELION,
            VillagerTrades.WANDERING_TRADER_EMERALD_NAME_TAG,
            VillagerTrades.WANDERING_TRADER_EMERALD_SHELF_MUSHROOM
         );
   }
}
