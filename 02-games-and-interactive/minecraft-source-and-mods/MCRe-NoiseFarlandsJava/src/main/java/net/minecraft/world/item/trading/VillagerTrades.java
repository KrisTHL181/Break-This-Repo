package net.minecraft.world.item.trading;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.component.predicates.VillagerTypePredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.PotionTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.DiscardItem;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.functions.ExplorationMapFunction;
import net.minecraft.world.level.storage.loot.functions.FilteredFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;
import net.minecraft.world.level.storage.loot.functions.SetRandomDyesFunction;
import net.minecraft.world.level.storage.loot.functions.SetRandomPotionFunction;
import net.minecraft.world.level.storage.loot.functions.SetStewEffectFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.Sum;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class VillagerTrades {
    public static final ResourceKey<VillagerTrade> FARMER_1_WHEAT_EMERALD = resourceKey("farmer/1/wheat_emerald");
    public static final ResourceKey<VillagerTrade> FARMER_1_POTATO_EMERALD = resourceKey("farmer/1/potato_emerald");
    public static final ResourceKey<VillagerTrade> FARMER_1_CARROT_EMERALD = resourceKey("farmer/1/carrot_emerald");
    public static final ResourceKey<VillagerTrade> FARMER_1_BEETROOT_EMERALD = resourceKey("farmer/1/beetroot_emerald");
    public static final ResourceKey<VillagerTrade> FARMER_1_EMERALD_BREAD = resourceKey("farmer/1/emerald_bread");
    public static final ResourceKey<VillagerTrade> FARMER_2_PUMPKIN_EMERALD = resourceKey("farmer/2/pumpkin_emerald");
    public static final ResourceKey<VillagerTrade> FARMER_2_EMERALD_PUMPKIN_PIE = resourceKey("farmer/2/emerald_pumpkin_pie");
    public static final ResourceKey<VillagerTrade> FARMER_2_EMERALD_APPLE = resourceKey("farmer/2/emerald_apple");
    public static final ResourceKey<VillagerTrade> FARMER_3_EMERALD_COOKIE = resourceKey("farmer/3/emerald_cookie");
    public static final ResourceKey<VillagerTrade> FARMER_3_MELON_EMERALD = resourceKey("farmer/3/melon_emerald");
    public static final ResourceKey<VillagerTrade> FARMER_4_EMERALD_CAKE = resourceKey("farmer/4/emerald_cake");
    public static final ResourceKey<VillagerTrade> FARMER_4_EMERALD_SUSPICIOUS_STEW = resourceKey("farmer/4/emerald_suspicious_stew");
    public static final ResourceKey<VillagerTrade> FARMER_5_EMERALD_GOLDEN_CARROT = resourceKey("farmer/5/emerald_golden_carrot");
    public static final ResourceKey<VillagerTrade> FARMER_5_EMERALD_GLISTENING_MELON_SLICE = resourceKey("farmer/5/emerald_glistening_melon_slice");
    public static final ResourceKey<VillagerTrade> FISHERMAN_1_STRING_EMERALD = resourceKey("fisherman/1/string_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_1_COAL_EMERALD = resourceKey("fisherman/1/coal_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_1_RAW_COD_AND_EMERALD_COOKED_COD = resourceKey("fisherman/1/raw_cod_and_emerald_cooked_cod");
    public static final ResourceKey<VillagerTrade> FISHERMAN_1_EMERALD_COD_BUCKET = resourceKey("fisherman/1/emerald_cod_bucket");
    public static final ResourceKey<VillagerTrade> FISHERMAN_2_COD_EMERALD = resourceKey("fisherman/2/cod_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_2_SALMON_AND_EMERALD_COOKED_SALMON = resourceKey("fisherman/2/salmon_and_emerald_cooked_salmon");
    public static final ResourceKey<VillagerTrade> FISHERMAN_2_EMERALD_CAMPFIRE = resourceKey("fisherman/2/emerald_campfire");
    public static final ResourceKey<VillagerTrade> FISHERMAN_3_SALMON_EMERALD = resourceKey("fisherman/3/salmon_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_3_EMERALD_ENCHANTED_FISHING_ROD = resourceKey("fisherman/3/emerald_enchanted_fishing_rod");
    public static final ResourceKey<VillagerTrade> FISHERMAN_4_TROPICAL_FISH_EMERALD = resourceKey("fisherman/4/tropical_fish_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_5_PUFFERFISH_EMERALD = resourceKey("fisherman/5/pufferfish_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_5_OAK_BOAT_EMERALD = resourceKey("fisherman/5/oak_boat_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_5_SPRUCE_BOAT_EMERALD = resourceKey("fisherman/5/spruce_boat_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_5_JUNGLE_BOAT_EMERALD = resourceKey("fisherman/5/jungle_boat_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_5_ACACIA_BOAT_EMERALD = resourceKey("fisherman/5/acacia_boat_emerald");
    public static final ResourceKey<VillagerTrade> FISHERMAN_5_DARK_OAK_BOAT_EMERALD = resourceKey("fisherman/5/dark_oak_boat_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_1_WHITE_WOOL_EMERALD = resourceKey("shepherd/1/white_wool_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_1_BROWN_WOOL_EMERALD = resourceKey("shepherd/1/brown_wool_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_1_GRAY_WOOL_EMERALD = resourceKey("shepherd/1/gray_wool_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_1_BLACK_WOOL_EMERALD = resourceKey("shepherd/1/black_wool_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_1_EMERALD_SHEARS = resourceKey("shepherd/1/emerald_shears");
    public static final ResourceKey<VillagerTrade> SHEPHERD_2_WHITE_DYE_EMERALD = resourceKey("shepherd/2/white_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_2_GRAY_DYE_EMERALD = resourceKey("shepherd/2/gray_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_2_BLACK_DYE_EMERALD = resourceKey("shepherd/2/black_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_2_LIGHT_BLUE_DYE_EMERALD = resourceKey("shepherd/2/light_blue_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_2_LIME_DYE_EMERALD = resourceKey("shepherd/2/lime_dye_emerald");
    public static final ColorCollection<ResourceKey<VillagerTrade>> SHEPHERD_2_EMERALD_WOOL = ColorCollection.NAMES
        .map(color -> resourceKey("shepherd/2/emerald_" + color + "_wool"));
    public static final ColorCollection<ResourceKey<VillagerTrade>> SHEPHERD_2_EMERALD_CARPETS = ColorCollection.NAMES
        .map(color -> resourceKey("shepherd/2/emerald_" + color + "_carpet"));
    public static final ResourceKey<VillagerTrade> SHEPHERD_3_YELLOW_DYE_EMERALD = resourceKey("shepherd/3/yellow_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_3_LIGHT_GRAY_DYE_EMERALD = resourceKey("shepherd/3/light_gray_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_3_ORANGE_DYE_EMERALD = resourceKey("shepherd/3/orange_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_3_RED_DYE_EMERALD = resourceKey("shepherd/3/red_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_3_PINK_DYE_EMERALD = resourceKey("shepherd/3/pink_dye_emerald");
    public static final ColorCollection<ResourceKey<VillagerTrade>> SHEPHERD_3_EMERALD_BED = ColorCollection.NAMES
        .map(color -> resourceKey("shepherd/3/emerald_" + color + "_bed"));
    public static final ResourceKey<VillagerTrade> SHEPHERD_4_BROWN_DYE_EMERALD = resourceKey("shepherd/4/brown_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_4_PURPLE_DYE_EMERALD = resourceKey("shepherd/4/purple_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_4_BLUE_DYE_EMERALD = resourceKey("shepherd/4/blue_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_4_GREEN_DYE_EMERALD = resourceKey("shepherd/4/green_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_4_MAGENTA_DYE_EMERALD = resourceKey("shepherd/4/magenta_dye_emerald");
    public static final ResourceKey<VillagerTrade> SHEPHERD_4_CYAN_DYE_EMERALD = resourceKey("shepherd/4/cyan_dye_emerald");
    public static final ColorCollection<ResourceKey<VillagerTrade>> SHEPHERD_4_EMERALD_BANNER = ColorCollection.NAMES
        .map(color -> resourceKey("shepherd/4/emerald_" + color + "_banner"));
    public static final ResourceKey<VillagerTrade> SHEPHERD_5_EMERALD_PAINTING = resourceKey("shepherd/5/emerald_painting");
    public static final ResourceKey<VillagerTrade> FLETCHER_1_STICK_EMERALD = resourceKey("fletcher/1/stick_emerald");
    public static final ResourceKey<VillagerTrade> FLETCHER_1_EMERALD_ARROW = resourceKey("fletcher/1/emerald_arrow");
    public static final ResourceKey<VillagerTrade> FLETCHER_1_GRAVEL_AND_EMERALD_FLINT = resourceKey("fletcher/1/gravel_and_emerald_flint");
    public static final ResourceKey<VillagerTrade> FLETCHER_2_FLINT_EMERALD = resourceKey("fletcher/2/flint_emerald");
    public static final ResourceKey<VillagerTrade> FLETCHER_2_EMERALD_BOW = resourceKey("fletcher/2/emerald_bow");
    public static final ResourceKey<VillagerTrade> FLETCHER_3_STRING_EMERALD = resourceKey("fletcher/3/string_emerald");
    public static final ResourceKey<VillagerTrade> FLETCHER_3_EMERALD_CROSSBOW = resourceKey("fletcher/3/emerald_crossbow");
    public static final ResourceKey<VillagerTrade> FLETCHER_4_FEATHER_EMERALD = resourceKey("fletcher/4/feather_emerald");
    public static final ResourceKey<VillagerTrade> FLETCHER_4_EMERALD_ENCHANTED_BOW = resourceKey("fletcher/4/emerald_enchanted_bow");
    public static final ResourceKey<VillagerTrade> FLETCHER_5_TRIPWIRE_HOOK_EMERALD = resourceKey("fletcher/5/tripwire_hook_emerald");
    public static final ResourceKey<VillagerTrade> FLETCHER_5_EMERALD_ENCHANTED_CROSSBOW = resourceKey("fletcher/5/emerald_enchanted_crossbow");
    public static final ResourceKey<VillagerTrade> FLETCHER_5_ARROW_AND_EMERALD_TIPPED_ARROW = resourceKey("fletcher/5/arrow_and_emerald_tipped_arrow");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_1_PAPER_EMERALD = resourceKey("librarian/1/paper_emerald");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_AND_BOOK_ENCHANTED_BOOK = resourceKey("librarian/1/emerald_and_book_enchanted_book");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_1_EMERALD_BOOKSHELF = resourceKey("librarian/1/emerald_bookshelf");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_2_BOOK_EMERALD = resourceKey("librarian/2/book_emerald");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_AND_BOOK_ENCHANTED_BOOK = resourceKey("librarian/2/emerald_and_book_enchanted_book");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_2_EMERALD_LANTERN = resourceKey("librarian/2/emerald_lantern");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_3_INK_SAC_EMERALD = resourceKey("librarian/3/ink_sac_emerald");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_AND_BOOK_ENCHANTED_BOOK = resourceKey("librarian/3/emerald_and_book_enchanted_book");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_3_EMERALD_GLASS = resourceKey("librarian/3/emerald_glass");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_4_WRITABLE_BOOK_EMERALD = resourceKey("librarian/4/writable_book_emerald");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_4_EMERALD_AND_BOOK_ENCHANTED_BOOK = resourceKey("librarian/4/emerald_book_and_enchanted_book");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_4_EMERALD_CLOCK = resourceKey("librarian/4/emerald_clock");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_4_EMERALD_COMPASS = resourceKey("librarian/4/emerald_compass");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_YELLOW_CANDLE = resourceKey("librarian/5/emerald_yellow_candle");
    public static final ResourceKey<VillagerTrade> LIBRARIAN_5_EMERALD_RED_CANDLE = resourceKey("librarian/5/emerald_red_candle");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_1_PAPER_EMERALD = resourceKey("cartographer/1/paper_emerald");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_1_EMERALD_MAP = resourceKey("cartographer/1/emerald_map");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_GLASS_PANE_EMERALD = resourceKey("cartographer/2/glass_pane_emerald");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_TAIGA_MAP = resourceKey(
        "cartographer/2/emerald_and_compass_village_taiga_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_EMERALD_AND_COMPASS_EXPLORER_SWAMP_MAP = resourceKey(
        "cartographer/2/emerald_and_compass_explorer_swamp_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_SNOWY_MAP = resourceKey(
        "cartographer/2/emerald_and_compass_village_snowy_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_SAVANNA_MAP = resourceKey(
        "cartographer/2/emerald_and_compass_village_savanna_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_PLAINS_MAP = resourceKey(
        "cartographer/2/emerald_and_compass_village_plains_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_EMERALD_AND_COMPASS_EXPLORER_JUNGLE_MAP = resourceKey(
        "cartographer/2/emerald_and_compass_explorer_jungle_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_DESERT_MAP = resourceKey(
        "cartographer/2/emerald_and_compass_village_desert_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_3_COMPASS_EMERALD = resourceKey("cartographer/3/compass_emerald");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_3_EMERALD_AND_COMPASS_OCEAN_EXPLORER_MAP = resourceKey(
        "cartographer/3/emerald_and_compass_ocean_explorer_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_3_EMERALD_AND_COMPASS_TRIAL_CHAMBER_MAP = resourceKey(
        "cartographer/3/emerald_and_compass_trial_chamber_map"
    );
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_ITEM_FRAME = resourceKey("cartographer/4/emerald_item_frame");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_WHITE_BANNER = resourceKey("cartographer/4/emerald_white_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_ORANGE_BANNER = resourceKey("cartographer/4/emerald_orange_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_MAGENTA_BANNER = resourceKey("cartographer/4/emerald_magenta_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_BLUE_BANNER = resourceKey("cartographer/4/emerald_blue_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_LIGHT_BLUE_BANNER = resourceKey("cartographer/4/emerald_light_blue_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_YELLOW_BANNER = resourceKey("cartographer/4/emerald_yellow_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_LIME_BANNER = resourceKey("cartographer/4/emerald_lime_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_PINK_BANNER = resourceKey("cartographer/4/emerald_pink_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_GRAY_BANNER = resourceKey("cartographer/4/emerald_gray_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_CYAN_BANNER = resourceKey("cartographer/4/emerald_cyan_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_PURPLE_BANNER = resourceKey("cartographer/4/emerald_purple_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_BROWN_BANNER = resourceKey("cartographer/4/emerald_brown_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_GREEN_BANNER = resourceKey("cartographer/4/emerald_green_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_RED_BANNER = resourceKey("cartographer/4/emerald_red_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_4_EMERALD_BLACK_BANNER = resourceKey("cartographer/4/emerald_black_banner");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_5_EMERALD_GLOBE_BANNER_PATTERN = resourceKey("cartographer/5/emerald_globe_banner_pattern");
    public static final ResourceKey<VillagerTrade> CARTOGRAPHER_5_EMERALD_AND_COMPASS_WOODLAND_MANSION_MAP = resourceKey(
        "cartographer/5/emerald_and_compass_woodland_mansion_map"
    );
    public static final ResourceKey<VillagerTrade> CLERIC_1_ROTTEN_FLESH_EMERALD = resourceKey("cleric/1/rotten_flesh_emerald");
    public static final ResourceKey<VillagerTrade> CLERIC_1_EMERALD_REDSTONE = resourceKey("cleric/1/emerald_redstone");
    public static final ResourceKey<VillagerTrade> CLERIC_2_GOLD_INGOT_EMERALD = resourceKey("cleric/2/gold_ingot_emerald");
    public static final ResourceKey<VillagerTrade> CLERIC_2_EMERALD_LAPIS_LAZULI = resourceKey("cleric/2/emerald_lapis_lazuli");
    public static final ResourceKey<VillagerTrade> CLERIC_3_RABBIT_FOOT_EMERALD = resourceKey("cleric/3/rabbit_foot_emerald");
    public static final ResourceKey<VillagerTrade> CLERIC_3_EMERALD_GLOWSTONE = resourceKey("cleric/3/emerald_glowstone");
    public static final ResourceKey<VillagerTrade> CLERIC_4_TURTLE_SCUTE_EMERALD = resourceKey("cleric/4/turtle_scute_emerald");
    public static final ResourceKey<VillagerTrade> CLERIC_4_GLASS_BOTTLE_EMERALD = resourceKey("cleric/4/glass_bottle_emerald");
    public static final ResourceKey<VillagerTrade> CLERIC_4_EMERALD_ENDER_PEARL = resourceKey("cleric/4/emerald_ender_pearl");
    public static final ResourceKey<VillagerTrade> CLERIC_5_NETHER_WART_EMERALD = resourceKey("cleric/5/nether_wart_emerald");
    public static final ResourceKey<VillagerTrade> CLERIC_5_EMERALD_EXPERIENCE_BOTTLE = resourceKey("cleric/5/emerald_experience_bottle");
    public static final ResourceKey<VillagerTrade> COMMON_SMITH_1_COAL_EMERALD = resourceKey("smith/1/coal_emerald");
    public static final ResourceKey<VillagerTrade> COMMON_SMITH_2_IRON_INGOT_EMERALD = resourceKey("smith/2/iron_ingot_emerald");
    public static final ResourceKey<VillagerTrade> COMMON_SMITH_2_EMERALD_BELL = resourceKey("smith/2/emerald_bell");
    public static final ResourceKey<VillagerTrade> ARMORER_1_EMERALD_IRON_LEGGINGS = resourceKey("armorer/1/emerald_iron_leggings");
    public static final ResourceKey<VillagerTrade> ARMORER_1_EMERALD_IRON_BOOTS = resourceKey("armorer/1/emerald_iron_boots");
    public static final ResourceKey<VillagerTrade> ARMORER_1_EMERALD_IRON_HELMET = resourceKey("armorer/1/emerald_iron_helmet");
    public static final ResourceKey<VillagerTrade> ARMORER_1_EMERALD_IRON_CHESTPLATE = resourceKey("armorer/1/emerald_iron_chestplate");
    public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_CHAINMAIL_BOOTS = resourceKey("armorer/2/emerald_chainmail_boots");
    public static final ResourceKey<VillagerTrade> ARMORER_2_EMERALD_CHAINMAIL_LEGGINGS = resourceKey("armorer/2/emerald_chainmail_leggings");
    public static final ResourceKey<VillagerTrade> ARMORER_3_LAVA_BUCKET_EMERALD = resourceKey("armorer/3/lava_bucket_emerald");
    public static final ResourceKey<VillagerTrade> ARMORER_3_EMERALD_CHAINMAIL_HELMET = resourceKey("armorer/3/emerald_chainmail_helmet");
    public static final ResourceKey<VillagerTrade> ARMORER_3_EMERALD_CHAINMAIL_CHESTPLATE = resourceKey("armorer/3/emerald_chainmail_chestplate");
    public static final ResourceKey<VillagerTrade> ARMORER_3_EMERALD_SHIELD = resourceKey("armorer/3/emerald_shield");
    public static final ResourceKey<VillagerTrade> ARMORER_3_DIAMOND_EMERALD = resourceKey("armorer/3/diamond_emerald");
    public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_DIAMOND_LEGGINGS = resourceKey("armorer/4/emerald_enchanted_diamond_leggings");
    public static final ResourceKey<VillagerTrade> ARMORER_4_EMERALD_ENCHANTED_DIAMOND_BOOTS = resourceKey("armorer/4/emerald_enchanted_diamond_boots");
    public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_ENCHANTED_DIAMOND_HELMET = resourceKey("armorer/5/emerald_enchanted_diamond_helmet");
    public static final ResourceKey<VillagerTrade> ARMORER_5_EMERALD_ENCHANTED_DIAMOND_CHESTPLATE = resourceKey(
        "armorer/5/emerald_enchanted_diamond_chestplate"
    );
    public static final ResourceKey<VillagerTrade> WEAPONSMITH_1_EMERALD_IRON_AXE = resourceKey("weaponsmith/1/emerald_iron_axe");
    public static final ResourceKey<VillagerTrade> WEAPONSMITH_1_EMERALD_ENCHANTED_IRON_SWORD = resourceKey("weaponsmith/1/emerald_enchanted_iron_sword");
    public static final ResourceKey<VillagerTrade> WEAPONSMITH_3_FLINT_EMERALD = resourceKey("weaponsmith/3/flint_emerald");
    public static final ResourceKey<VillagerTrade> WEAPONSMITH_4_DIAMOND_EMERALD = resourceKey("weaponsmith/4/diamond_emerald");
    public static final ResourceKey<VillagerTrade> WEAPONSMITH_4_EMERALD_ENCHANTED_DIAMOND_AXE = resourceKey("weaponsmith/4/emerald_enchanted_diamond_axe");
    public static final ResourceKey<VillagerTrade> WEAPONSMITH_5_EMERALD_ENCHANTED_DIAMOND_SWORD = resourceKey("weaponsmith/5/emerald_enchanted_diamond_sword");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_1_EMERALD_STONE_AXE = resourceKey("toolsmith/1/emerald_stone_axe");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_1_EMERALD_STONE_SHOVEL = resourceKey("toolsmith/1/emerald_stone_shovel");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_1_EMERALD_STONE_PICKAXE = resourceKey("toolsmith/1/emerald_stone_pickaxe");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_1_EMERALD_STONE_HOE = resourceKey("toolsmith/1/emerald_stone_hoe");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_3_FLINT_EMERALD = resourceKey("toolsmith/3/flint_emerald");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_3_EMERALD_IRON_AXE = resourceKey("toolsmith/3/emerald_enchanted_iron_axe");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_3_EMERALD_IRON_SHOVEL = resourceKey("toolsmith/3/emerald_enchanted_iron_shovel");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_3_EMERALD_IRON_PICKAXE = resourceKey("toolsmith/3/emerald_enchanted_iron_pickaxe");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_3_EMERALD_DIAMOND_HOE = resourceKey("toolsmith/3/emerald_diamond_hoe");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_4_DIAMOND_EMERALD = resourceKey("toolsmith/4/diamond_emerald");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_4_EMERALD_DIAMOND_AXE = resourceKey("toolsmith/4/emerald_enchanted_diamond_axe");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_4_EMERALD_DIAMOND_SHOVEL = resourceKey("toolsmith/4/emerald_enchanted_diamond_shovel");
    public static final ResourceKey<VillagerTrade> TOOLSMITH_5_EMERALD_DIAMOND_PICKAXE = resourceKey("toolsmith/5/emerald_enchanted_diamond_pickaxe");
    public static final ResourceKey<VillagerTrade> BUTCHER_1_CHICKEN_EMERALD = resourceKey("butcher/1/chicken_emerald");
    public static final ResourceKey<VillagerTrade> BUTCHER_1_PORKCHOP_EMERALD = resourceKey("butcher/1/porkchop_emerald");
    public static final ResourceKey<VillagerTrade> BUTCHER_1_RABBIT_EMERALD = resourceKey("butcher/1/rabbit_emerald");
    public static final ResourceKey<VillagerTrade> BUTCHER_1_EMERALD_RABBIT_STEW = resourceKey("butcher/1/emerald_rabbit_stew");
    public static final ResourceKey<VillagerTrade> BUTCHER_2_COAL_EMERALD = resourceKey("butcher/2/coal_emerald");
    public static final ResourceKey<VillagerTrade> BUTCHER_2_EMERALD_COOKED_PORKCHOP = resourceKey("butcher/2/emerald_cooked_porkchop");
    public static final ResourceKey<VillagerTrade> BUTCHER_2_EMERALD_COOKED_CHICKEN = resourceKey("butcher/2/emerald_cooked_chicken");
    public static final ResourceKey<VillagerTrade> BUTCHER_3_MUTTON_EMERALD = resourceKey("butcher/3/mutton_emerald");
    public static final ResourceKey<VillagerTrade> BUTCHER_3_BEEF_EMERALD = resourceKey("butcher/3/beef_emerald");
    public static final ResourceKey<VillagerTrade> BUTCHER_4_DRIED_KELP_BLOCK_EMERALD = resourceKey("butcher/4/dried_kelp_block_emerald");
    public static final ResourceKey<VillagerTrade> BUTCHER_5_SWEET_BERRIES_EMERALD = resourceKey("butcher/5/sweet_berries_emerald");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_1_LEATHER_EMERALD = resourceKey("leatherworker/1/leather_emerald");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_1_EMERALD_DYED_LEATHER_LEGGINGS = resourceKey("leatherworker/1/emerald_dyed_leather_leggings");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_1_EMERALD_DYED_LEATHER_CHESTPLATE = resourceKey(
        "leatherworker/1/emerald_dyed_leather_chestplate"
    );
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_2_FLINT_EMERALD = resourceKey("leatherworker/2/flint_emerald");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_2_EMERALD_DYED_LEATHER_HELMET = resourceKey("leatherworker/2/emerald_dyed_leather_helmet");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_2_EMERALD_DYED_LEATHER_BOOTS = resourceKey("leatherworker/2/emerald_dyed_leather_boots");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_3_RABBIT_HIDE_EMERALD = resourceKey("leatherworker/3/rabbit_hide_emerald");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_3_EMERALD_DYED_LEATHER_CHESTPLATE = resourceKey(
        "leatherworker/3/emerald_dyed_leather_chestplate"
    );
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_4_TURTLE_SCUTE_EMERALD = resourceKey("leatherworker/4/turtle_scute_emerald");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_4_EMERALD_DYED_LEATHER_HORSE_ARMOR = resourceKey(
        "leatherworker/4/emerald_dyed_leather_horse_armor"
    );
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_5_EMERALD_SADDLE = resourceKey("leatherworker/5/emerald_saddle");
    public static final ResourceKey<VillagerTrade> LEATHERWORKER_5_EMERALD_DYED_LEATHER_HELMET = resourceKey("leatherworker/5/emerald_dyed_leather_helmet");
    public static final ResourceKey<VillagerTrade> MASON_1_CLAY_BALL_EMERALD = resourceKey("mason/1/clay_ball_emerald");
    public static final ResourceKey<VillagerTrade> MASON_1_EMERALD_BRICK = resourceKey("mason/1/emerald_brick");
    public static final ResourceKey<VillagerTrade> MASON_2_STONE_EMERALD = resourceKey("mason/2/stone_emerald");
    public static final ResourceKey<VillagerTrade> MASON_2_EMERALD_CHISELED_STONE_BRICKS = resourceKey("mason/2/emerald_chiseled_stone_bricks");
    public static final ResourceKey<VillagerTrade> MASON_3_GRANITE_EMERALD = resourceKey("mason/3/granite_emerald");
    public static final ResourceKey<VillagerTrade> MASON_3_ANDESITE_EMERALD = resourceKey("mason/3/andesite_emerald");
    public static final ResourceKey<VillagerTrade> MASON_3_DIORITE_EMERALD = resourceKey("mason/3/diorite_emerald");
    public static final ResourceKey<VillagerTrade> MASON_3_EMERALD_DRIPSTONE_BLOCK = resourceKey("mason/3/emerald_dripstone_block");
    public static final ResourceKey<VillagerTrade> MASON_3_EMERALD_POLISHED_ANDESITE = resourceKey("mason/3/emerald_polished_andesite");
    public static final ResourceKey<VillagerTrade> MASON_3_EMERALD_POLISHED_DIORITE = resourceKey("mason/3/emerald_polished_diorite");
    public static final ResourceKey<VillagerTrade> MASON_3_EMERALD_POLISHED_GRANTITE = resourceKey("mason/3/emerald_polished_granite");
    public static final ResourceKey<VillagerTrade> MASON_4_QUARTZ_EMERALD = resourceKey("mason/4/quartz_emerald");
    public static final ColorCollection<ResourceKey<VillagerTrade>> MASON_4_EMERALD_TERRACOTTA = ColorCollection.NAMES
        .map(color -> resourceKey("mason/4/emerald_" + color + "_terracotta"));
    public static final ColorCollection<ResourceKey<VillagerTrade>> MASON_4_EMERALD_GLAZED_TERRACOTTA = ColorCollection.NAMES
        .map(color -> resourceKey("mason/4/emerald_" + color + "_glazed_terracotta"));
    public static final ResourceKey<VillagerTrade> MASON_5_EMERALD_QUARTZ_PILLAR = resourceKey("mason/5/emerald_quartz_pillar");
    public static final ResourceKey<VillagerTrade> MASON_5_EMERALD_QUARTZ_BLOCK = resourceKey("mason/5/emerald_quartz_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_WATER_BOTTLE_EMERALD = resourceKey("wandering_trader/water_bottle_emerald");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_WATER_BUCKET_EMERALD = resourceKey("wandering_trader/water_bucket_emerald");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_MILK_BUCKET_EMERALD = resourceKey("wandering_trader/milk_bucket_emerald");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_FERMENTED_SPIDER_EYE_EMERALD = resourceKey("wandering_trader/fermented_spider_eye_emerald");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_BAKED_POTATO_EMERALD = resourceKey("wandering_trader/baked_potato_emerald");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_HAY_BLOCK_EMERALD = resourceKey("wandering_trader/hay_block_emerald");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PACKED_ICE = resourceKey("wandering_trader/emerald_packed_ice");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BLUE_ICE = resourceKey("wandering_trader/emerald_blue_ice");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_GUNPOWDER = resourceKey("wandering_trader/emerald_gunpowder");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PODZOL = resourceKey("wandering_trader/emerald_podzol");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_ACACIA_LOG = resourceKey("wandering_trader/emerald_acacia_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BIRCH_LOG = resourceKey("wandering_trader/emerald_birch_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_DARK_OAK_LOG = resourceKey("wandering_trader/emerald_dark_oak_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_JUNGLE_LOG = resourceKey("wandering_trader/emerald_jungle_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_OAK_LOG = resourceKey("wandering_trader/emerald_oak_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SPRUCE_LOG = resourceKey("wandering_trader/emerald_spruce_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_CHERRY_LOG = resourceKey("wandering_trader/emerald_cherry_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_MANGROVE_LOG = resourceKey("wandering_trader/emerald_mangrove_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PALE_OAK_LOG = resourceKey("wandering_trader/emerald_pale_oak_log");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_ENCHANTED_IRON_PICKAXE = resourceKey(
        "wandering_trader/emerald_enchanted_iron_pickaxe"
    );
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_LONG_INVISIBILITY_POTION = resourceKey(
        "wandering_trader/emerald_long_invisibility_potion"
    );
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_TROPICAL_FISH_BUCKET = resourceKey("wandering_trader/emerald_fish_bucket");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PUFFERFISH_BUCKET = resourceKey("wandering_trader/emerald_pufferfish_bucket");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SEA_PICKLE = resourceKey("wandering_trader/emerald_sea_pickle");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SLIME_BALL = resourceKey("wandering_trader/emerald_slime_ball");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_GLOWSTONE = resourceKey("wandering_trader/emerald_glowstone");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_NAUTILUS_SHELL = resourceKey("wandering_trader/emerald_nautilus_shell");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_FERN = resourceKey("wandering_trader/emerald_fern");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SUGAR_CANE = resourceKey("wandering_trader/emerald_sugar_cane");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PUMPKIN = resourceKey("wandering_trader/emerald_pumpkin");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_KELP = resourceKey("wandering_trader/emerald_kelp");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_CACTUS = resourceKey("wandering_trader/emerald_cactus");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_DANDELION = resourceKey("wandering_trader/emerald_dandelion");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_GOLDEN_DANDELION = resourceKey("wandering_trader/emerald_golden_dandelion");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_POPPY = resourceKey("wandering_trader/emerald_poppy");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BLUE_ORCHID = resourceKey("wandering_trader/emerald_blue_orchid");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_ALLIUM = resourceKey("wandering_trader/emerald_allium");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_AZURE_BLUET = resourceKey("wandering_trader/emerald_azure_bluet");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_RED_TULIP = resourceKey("wandering_trader/emerald_red_tulip");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_ORANGE_TULIP = resourceKey("wandering_trader/emerald_orange_tulip");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_WHITE_TULIP = resourceKey("wandering_trader/emerald_white_tulip");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PINK_TULIP = resourceKey("wandering_trader/emerald_pink_tulip");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_OXEYE_DAISY = resourceKey("wandering_trader/emerald_oxeye_daisy");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_CORNFLOWER = resourceKey("wandering_trader/emerald_cornflower");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_LILY_OF_THE_VALLEY = resourceKey("wandering_trader/emerald_lily_of_the_valley");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_OPEN_EYEBLOSSOM = resourceKey("wandering_trader/emerald_open_eyeblossom");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_WHEAT_SEEDS = resourceKey("wandering_trader/emerald_wheat_seeds");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BEETROOT_SEEDS = resourceKey("wandering_trader/emerald_beetroot_seeds");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PUMPKIN_SEEDS = resourceKey("wandering_trader/emerald_pumpkin_seeds");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_MELON_SEEDS = resourceKey("wandering_trader/emerald_melon_seeds");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_ACACIA_SAPLING = resourceKey("wandering_trader/emerald_acacia_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BIRCH_SAPLING = resourceKey("wandering_trader/emerald_birch_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_DARK_OAK_SAPLING = resourceKey("wandering_trader/emerald_dark_oak_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_JUNGLE_SAPLING = resourceKey("wandering_trader/emerald_jungle_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_OAK_SAPLING = resourceKey("wandering_trader/emerald_oak_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SPRUCE_SAPLING = resourceKey("wandering_trader/emerald_spruce_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_CHERRY_SAPLING = resourceKey("wandering_trader/emerald_cherry_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PALE_OAK_SAPLING = resourceKey("wandering_trader/emerald_pale_oak_sapling");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_MANGROVE_PROPAGULE = resourceKey("wandering_trader/emerald_mangrove_propagule");
    public static final ColorCollection<ResourceKey<VillagerTrade>> WANDERING_TRADER_EMERALD_DYE = ColorCollection.NAMES
        .map(color -> resourceKey("wandering_trader/emerald_" + color + "_dye"));
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BRAIN_CORAL_BLOCK = resourceKey("wandering_trader/emerald_brain_coral_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BUBBLE_CORAL_BLOCK = resourceKey("wandering_trader/emerald_bubble_coral_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_FIRE_CORAL_BLOCK = resourceKey("wandering_trader/emerald_fire_coral_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_HORN_CORAL_BLOCK = resourceKey("wandering_trader/emerald_horn_coral_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_TUBE_CORAL_BLOCK = resourceKey("wandering_trader/emerald_tube_coral_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_VINE = resourceKey("wandering_trader/emerald_vine");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PALE_HANGING_MOSS = resourceKey("wandering_trader/emerald_pale_hanging_moss");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_BROWN_MUSHROOM = resourceKey("wandering_trader/emerald_brown_mushroom");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_RED_MUSHROOM = resourceKey("wandering_trader/emerald_red_mushroom");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_LILY_PAD = resourceKey("wandering_trader/emerald_lily_pad");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SMALL_DRIPLEAF = resourceKey("wandering_trader/emerald_small_dripleaf");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SAND = resourceKey("wandering_trader/emerald_sand");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_RED_SAND = resourceKey("wandering_trader/emerald_red_sand");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_POINTED_DRIPSTONE = resourceKey("wandering_trader/emerald_pointed_dripstone");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_SULFUR_SPIKE = resourceKey("wandering_trader/emerald_sulfur_spike");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_ROOTED_DIRT = resourceKey("wandering_trader/emerald_rooted_dirt");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_MOSS_BLOCK = resourceKey("wandering_trader/emerald_moss_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_PALE_MOSS_BLOCK = resourceKey("wandering_trader/emerald_pale_moss_block");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_WILDFLOWERS = resourceKey("wandering_trader/emerald_wildflowers");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_DRY_TALL_GRASS = resourceKey("wandering_trader/emerald_dry_tall_grass");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_FIREFLY_BUSH = resourceKey("wandering_trader/emerald_firefly_bush");
    public static final ResourceKey<VillagerTrade> WANDERING_TRADER_EMERALD_NAME_TAG = resourceKey("wandering_trader/emerald_name_tag");

    public static Holder<VillagerTrade> bootstrap(final BootstrapContext<VillagerTrade> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
        HolderSet<Enchantment> enchantmentsForTradedEquipment = enchantments.getOrThrow(EnchantmentTags.ON_TRADED_EQUIPMENT);
        HolderSet<Enchantment> enchantmentsForBooks = enchantments.getOrThrow(EnchantmentTags.TRADEABLE);
        HolderSet<Enchantment> doubleTradePrice = enchantments.getOrThrow(EnchantmentTags.DOUBLE_TRADE_PRICE);
        HolderGetter<Potion> potions = context.lookup(Registries.POTION);
        HolderSet<Potion> potionsForTippedArrows = potions.getOrThrow(PotionTags.TRADEABLE);
        HolderGetter<VillagerType> villagerVariants = context.lookup(Registries.VILLAGER_TYPE);
        register(
            context,
            FARMER_1_WHEAT_EMERALD,
            new VillagerTrade(new TradeCost(Items.WHEAT, 20), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_1_POTATO_EMERALD,
            new VillagerTrade(new TradeCost(Items.POTATO, 26), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_1_CARROT_EMERALD,
            new VillagerTrade(new TradeCost(Items.CARROT, 22), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_1_BEETROOT_EMERALD,
            new VillagerTrade(new TradeCost(Items.BEETROOT, 15), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_1_EMERALD_BREAD,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.BREAD, 6), 16, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_2_PUMPKIN_EMERALD,
            new VillagerTrade(new TradeCost(Items.PUMPKIN, 6), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_2_EMERALD_PUMPKIN_PIE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.PUMPKIN_PIE, 4), 12, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_2_EMERALD_APPLE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.APPLE, 4), 16, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_3_EMERALD_COOKIE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.COOKIE, 18), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_3_MELON_EMERALD,
            new VillagerTrade(new TradeCost(Items.MELON, 4), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_4_EMERALD_CAKE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.CAKE), 12, 15, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_4_EMERALD_SUSPICIOUS_STEW,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 1),
                new ItemStackTemplate(Items.SUSPICIOUS_STEW),
                12,
                15,
                0.05F,
                Optional.empty(),
                List.of(
                    new SetStewEffectFunction.Builder()
                        .withEffect(MobEffects.NIGHT_VISION, new ConstantValue(5.0F))
                        .withEffect(MobEffects.JUMP_BOOST, new ConstantValue(8.0F))
                        .withEffect(MobEffects.WEAKNESS, new ConstantValue(7.0F))
                        .withEffect(MobEffects.BLINDNESS, new ConstantValue(6.0F))
                        .withEffect(MobEffects.POISON, new ConstantValue(14.0F))
                        .withEffect(MobEffects.SATURATION, new ConstantValue(7.0F))
                        .build()
                )
            )
        );
        register(
            context,
            FARMER_5_EMERALD_GOLDEN_CARROT,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.GOLDEN_CARROT, 3), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FARMER_5_EMERALD_GLISTENING_MELON_SLICE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 4), new ItemStackTemplate(Items.GLISTERING_MELON_SLICE, 3), 12, 30, 0.05F, Optional.empty(), List.of()
            )
        );
        register(
            context,
            FISHERMAN_1_STRING_EMERALD,
            new VillagerTrade(new TradeCost(Items.STRING, 20), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FISHERMAN_1_COAL_EMERALD,
            new VillagerTrade(new TradeCost(Items.COAL, 10), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FISHERMAN_1_RAW_COD_AND_EMERALD_COOKED_COD,
            new VillagerTrade(
                new TradeCost(Items.COD, 6),
                Optional.of(new TradeCost(Items.EMERALD, 1)),
                new ItemStackTemplate(Items.COOKED_COD, 6),
                16,
                1,
                0.05F,
                Optional.empty(),
                List.of()
            )
        );
        register(
            context,
            FISHERMAN_1_EMERALD_COD_BUCKET,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.COD_BUCKET), 16, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FISHERMAN_2_COD_EMERALD,
            new VillagerTrade(new TradeCost(Items.COD, 15), new ItemStackTemplate(Items.EMERALD), 16, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FISHERMAN_2_SALMON_AND_EMERALD_COOKED_SALMON,
            new VillagerTrade(
                new TradeCost(Items.SALMON, 6),
                Optional.of(new TradeCost(Items.EMERALD, 1)),
                new ItemStackTemplate(Items.COOKED_SALMON, 6),
                16,
                5,
                0.05F,
                Optional.empty(),
                List.of()
            )
        );
        register(
            context,
            FISHERMAN_2_EMERALD_CAMPFIRE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 2), new ItemStackTemplate(Items.CAMPFIRE), 12, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FISHERMAN_3_SALMON_EMERALD,
            new VillagerTrade(new TradeCost(Items.SALMON, 13), new ItemStackTemplate(Items.EMERALD), 16, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FISHERMAN_3_EMERALD_ENCHANTED_FISHING_ROD,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 3),
                new ItemStackTemplate(Items.FISHING_ROD),
                3,
                10,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.FISHING_ROD)
            )
        );
        register(
            context,
            FISHERMAN_4_TROPICAL_FISH_EMERALD,
            new VillagerTrade(new TradeCost(Items.TROPICAL_FISH, 6), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FISHERMAN_5_PUFFERFISH_EMERALD,
            new VillagerTrade(new TradeCost(Items.PUFFERFISH, 4), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        registerBoatTrades(context, villagerVariants);
        registerShepherdWoolSales(context);
        register(
            context,
            SHEPHERD_1_EMERALD_SHEARS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 2), new ItemStackTemplate(Items.SHEARS), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        registerShepherdLevelTwoDyeTrades(context);
        registerWoolPurchases(context);
        registerCarpetPurchases(context);
        registerShepherdLevelThreeDyeTrades(context);
        registerBedTrades(context);
        registerLevelFourDyeTrades(context);
        registerShepherdBannerTrades(context);
        register(
            context,
            SHEPHERD_5_EMERALD_PAINTING,
            new VillagerTrade(new TradeCost(Items.EMERALD, 2), new ItemStackTemplate(Items.PAINTING, 3), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_1_STICK_EMERALD,
            new VillagerTrade(new TradeCost(Items.STICK, 32), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_1_EMERALD_ARROW,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.ARROW, 16), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_1_GRAVEL_AND_EMERALD_FLINT,
            new VillagerTrade(
                new TradeCost(Items.GRAVEL, 10),
                Optional.of(new TradeCost(Items.EMERALD, 1)),
                new ItemStackTemplate(Items.FLINT, 10),
                12,
                1,
                0.05F,
                Optional.empty(),
                List.of()
            )
        );
        register(
            context,
            FLETCHER_2_FLINT_EMERALD,
            new VillagerTrade(new TradeCost(Items.FLINT, 26), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_2_EMERALD_BOW,
            new VillagerTrade(new TradeCost(Items.EMERALD, 2), new ItemStackTemplate(Items.BOW), 12, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_3_STRING_EMERALD,
            new VillagerTrade(new TradeCost(Items.STRING, 14), new ItemStackTemplate(Items.EMERALD), 16, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_3_EMERALD_CROSSBOW,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.CROSSBOW), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_4_FEATHER_EMERALD,
            new VillagerTrade(new TradeCost(Items.FEATHER, 24), new ItemStackTemplate(Items.EMERALD), 16, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_4_EMERALD_ENCHANTED_BOW,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 2),
                new ItemStackTemplate(Items.BOW),
                3,
                15,
                0.05F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.BOW)
            )
        );
        register(
            context,
            FLETCHER_5_TRIPWIRE_HOOK_EMERALD,
            new VillagerTrade(new TradeCost(Items.TRIPWIRE_HOOK, 8), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            FLETCHER_5_EMERALD_ENCHANTED_CROSSBOW,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 3),
                new ItemStackTemplate(Items.CROSSBOW),
                3,
                15,
                0.05F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.CROSSBOW)
            )
        );
        register(
            context,
            FLETCHER_5_ARROW_AND_EMERALD_TIPPED_ARROW,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 2),
                Optional.of(new TradeCost(Items.ARROW, 5)),
                new ItemStackTemplate(Items.TIPPED_ARROW, 5),
                12,
                30,
                0.05F,
                Optional.empty(),
                List.of(SetRandomPotionFunction.fromTagKey(potionsForTippedArrows).build())
            )
        );
        register(
            context,
            LIBRARIAN_1_PAPER_EMERALD,
            new VillagerTrade(new TradeCost(Items.PAPER, 24), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_1_EMERALD_AND_BOOK_ENCHANTED_BOOK,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 0),
                Optional.of(new TradeCost(Items.BOOK, 1)),
                new ItemStackTemplate(Items.ENCHANTED_BOOK),
                12,
                1,
                0.2F,
                Optional.empty(),
                enchantedBook(items, enchantmentsForBooks),
                doubleTradePrice
            )
        );
        register(
            context,
            LIBRARIAN_1_EMERALD_BOOKSHELF,
            new VillagerTrade(new TradeCost(Items.EMERALD, 9), new ItemStackTemplate(Items.BOOKSHELF), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_2_BOOK_EMERALD,
            new VillagerTrade(new TradeCost(Items.BOOK, 4), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_2_EMERALD_AND_BOOK_ENCHANTED_BOOK,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 0),
                Optional.of(new TradeCost(Items.BOOK, 1)),
                new ItemStackTemplate(Items.ENCHANTED_BOOK),
                12,
                5,
                0.2F,
                Optional.empty(),
                enchantedBook(items, enchantmentsForBooks),
                doubleTradePrice
            )
        );
        register(
            context,
            LIBRARIAN_2_EMERALD_LANTERN,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.LANTERN), 12, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_3_INK_SAC_EMERALD,
            new VillagerTrade(new TradeCost(Items.INK_SAC, 5), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_3_EMERALD_AND_BOOK_ENCHANTED_BOOK,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 0),
                Optional.of(new TradeCost(Items.BOOK, 1)),
                new ItemStackTemplate(Items.ENCHANTED_BOOK),
                12,
                10,
                0.2F,
                Optional.empty(),
                enchantedBook(items, enchantmentsForBooks),
                doubleTradePrice
            )
        );
        register(
            context,
            LIBRARIAN_3_EMERALD_GLASS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.GLASS, 4), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_4_WRITABLE_BOOK_EMERALD,
            new VillagerTrade(new TradeCost(Items.WRITABLE_BOOK, 2), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_4_EMERALD_AND_BOOK_ENCHANTED_BOOK,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 0),
                Optional.of(new TradeCost(Items.BOOK, 1)),
                new ItemStackTemplate(Items.ENCHANTED_BOOK),
                12,
                15,
                0.2F,
                Optional.empty(),
                enchantedBook(items, enchantmentsForBooks),
                doubleTradePrice
            )
        );
        register(
            context,
            LIBRARIAN_4_EMERALD_CLOCK,
            new VillagerTrade(new TradeCost(Items.EMERALD, 5), new ItemStackTemplate(Items.CLOCK), 12, 15, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_4_EMERALD_COMPASS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 4), new ItemStackTemplate(Items.COMPASS), 12, 15, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_5_EMERALD_YELLOW_CANDLE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.DYED_CANDLE.yellow()), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LIBRARIAN_5_EMERALD_RED_CANDLE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.DYED_CANDLE.red()), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CARTOGRAPHER_1_PAPER_EMERALD,
            new VillagerTrade(new TradeCost(Items.PAPER, 24), new ItemStackTemplate(Items.EMERALD), 12, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CARTOGRAPHER_1_EMERALD_MAP,
            new VillagerTrade(new TradeCost(Items.EMERALD, 7), new ItemStackTemplate(Items.MAP), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CARTOGRAPHER_2_GLASS_PANE_EMERALD,
            new VillagerTrade(new TradeCost(Items.GLASS_PANE, 11), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        registerBasicExplorerMapTrades(context, items, villagerVariants);
        register(
            context,
            CARTOGRAPHER_3_COMPASS_EMERALD,
            new VillagerTrade(new TradeCost(Items.COMPASS, 1), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CARTOGRAPHER_3_EMERALD_AND_COMPASS_OCEAN_EXPLORER_MAP,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 13),
                Optional.of(new TradeCost(Items.COMPASS, 1)),
                new ItemStackTemplate(Items.MAP),
                12,
                10,
                0.2F,
                Optional.empty(),
                List.of(
                    ExplorationMapFunction.makeExplorationMap()
                        .setDestination(StructureTags.ON_OCEAN_EXPLORER_MAPS)
                        .setMapDecoration(MapDecorationTypes.OCEAN_MONUMENT)
                        .setSearchRadius(100)
                        .setSkipKnownStructures(true)
                        .build(),
                    SetNameFunction.setName(Component.translatable("filled_map.monument"), SetNameFunction.Target.ITEM_NAME).build(),
                    FilteredFunction.filtered(
                            new ItemPredicate.Builder()
                                .of(items, Items.FILLED_MAP)
                                .withComponents(DataComponentMatchers.Builder.components().any(DataComponents.MAP_ID).build())
                                .build()
                        )
                        .onFail(Optional.of(DiscardItem.discardItem().build()))
                        .build()
                )
            )
        );
        register(
            context,
            CARTOGRAPHER_3_EMERALD_AND_COMPASS_TRIAL_CHAMBER_MAP,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 12),
                Optional.of(new TradeCost(Items.COMPASS, 1)),
                new ItemStackTemplate(Items.MAP),
                12,
                10,
                0.2F,
                Optional.empty(),
                List.of(
                    ExplorationMapFunction.makeExplorationMap()
                        .setDestination(StructureTags.ON_TRIAL_CHAMBERS_MAPS)
                        .setMapDecoration(MapDecorationTypes.TRIAL_CHAMBERS)
                        .setSearchRadius(100)
                        .setSkipKnownStructures(true)
                        .build(),
                    SetNameFunction.setName(Component.translatable("filled_map.trial_chambers"), SetNameFunction.Target.ITEM_NAME).build(),
                    FilteredFunction.filtered(
                            new ItemPredicate.Builder()
                                .of(items, Items.FILLED_MAP)
                                .withComponents(DataComponentMatchers.Builder.components().any(DataComponents.MAP_ID).build())
                                .build()
                        )
                        .onFail(Optional.of(DiscardItem.discardItem().build()))
                        .build()
                )
            )
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_ITEM_FRAME,
            new VillagerTrade(new TradeCost(Items.EMERALD, 7), new ItemStackTemplate(Items.ITEM_FRAME), 12, 15, 0.05F, Optional.empty(), List.of())
        );
        registerCartographerBannerTrades(context, villagerVariants);
        register(
            context,
            CARTOGRAPHER_5_EMERALD_GLOBE_BANNER_PATTERN,
            new VillagerTrade(new TradeCost(Items.EMERALD, 8), new ItemStackTemplate(Items.GLOBE_BANNER_PATTERN), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CARTOGRAPHER_5_EMERALD_AND_COMPASS_WOODLAND_MANSION_MAP,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 14),
                Optional.of(new TradeCost(Items.COMPASS, 1)),
                new ItemStackTemplate(Items.MAP),
                12,
                30,
                0.2F,
                Optional.empty(),
                List.of(
                    ExplorationMapFunction.makeExplorationMap()
                        .setDestination(StructureTags.ON_WOODLAND_EXPLORER_MAPS)
                        .setMapDecoration(MapDecorationTypes.WOODLAND_MANSION)
                        .setSearchRadius(100)
                        .setSkipKnownStructures(true)
                        .build(),
                    SetNameFunction.setName(Component.translatable("filled_map.mansion"), SetNameFunction.Target.ITEM_NAME).build(),
                    FilteredFunction.filtered(
                            new ItemPredicate.Builder()
                                .of(items, Items.FILLED_MAP)
                                .withComponents(DataComponentMatchers.Builder.components().any(DataComponents.MAP_ID).build())
                                .build()
                        )
                        .onFail(Optional.of(DiscardItem.discardItem().build()))
                        .build()
                )
            )
        );
        register(
            context,
            CLERIC_1_ROTTEN_FLESH_EMERALD,
            new VillagerTrade(new TradeCost(Items.ROTTEN_FLESH, 32), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_1_EMERALD_REDSTONE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.REDSTONE, 2), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_2_GOLD_INGOT_EMERALD,
            new VillagerTrade(new TradeCost(Items.GOLD_INGOT, 3), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_2_EMERALD_LAPIS_LAZULI,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.LAPIS_LAZULI), 12, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_3_RABBIT_FOOT_EMERALD,
            new VillagerTrade(new TradeCost(Items.RABBIT_FOOT, 2), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_3_EMERALD_GLOWSTONE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 4), new ItemStackTemplate(Items.GLOWSTONE), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_4_TURTLE_SCUTE_EMERALD,
            new VillagerTrade(new TradeCost(Items.TURTLE_SCUTE, 4), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_4_GLASS_BOTTLE_EMERALD,
            new VillagerTrade(new TradeCost(Items.GLASS_BOTTLE, 9), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_4_EMERALD_ENDER_PEARL,
            new VillagerTrade(new TradeCost(Items.EMERALD, 5), new ItemStackTemplate(Items.ENDER_PEARL), 12, 15, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_5_NETHER_WART_EMERALD,
            new VillagerTrade(new TradeCost(Items.NETHER_WART, 22), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            CLERIC_5_EMERALD_EXPERIENCE_BOTTLE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.EXPERIENCE_BOTTLE), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            COMMON_SMITH_1_COAL_EMERALD,
            new VillagerTrade(new TradeCost(Items.COAL, 15), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            COMMON_SMITH_2_IRON_INGOT_EMERALD,
            new VillagerTrade(new TradeCost(Items.IRON_INGOT, 4), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            COMMON_SMITH_2_EMERALD_BELL,
            new VillagerTrade(new TradeCost(Items.EMERALD, 36), new ItemStackTemplate(Items.BELL), 12, 5, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_1_EMERALD_IRON_LEGGINGS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 7), new ItemStackTemplate(Items.IRON_LEGGINGS), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_1_EMERALD_IRON_BOOTS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 4), new ItemStackTemplate(Items.IRON_BOOTS), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_1_EMERALD_IRON_HELMET,
            new VillagerTrade(new TradeCost(Items.EMERALD, 5), new ItemStackTemplate(Items.IRON_HELMET), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_1_EMERALD_IRON_CHESTPLATE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 9), new ItemStackTemplate(Items.IRON_CHESTPLATE), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_2_EMERALD_CHAINMAIL_BOOTS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.CHAINMAIL_BOOTS), 12, 5, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_2_EMERALD_CHAINMAIL_LEGGINGS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.CHAINMAIL_LEGGINGS), 12, 5, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_3_LAVA_BUCKET_EMERALD,
            new VillagerTrade(new TradeCost(Items.LAVA_BUCKET, 1), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_3_EMERALD_CHAINMAIL_HELMET,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.CHAINMAIL_HELMET), 12, 10, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_3_EMERALD_CHAINMAIL_CHESTPLATE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 4), new ItemStackTemplate(Items.CHAINMAIL_CHESTPLATE), 12, 10, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_3_EMERALD_SHIELD,
            new VillagerTrade(new TradeCost(Items.EMERALD, 5), new ItemStackTemplate(Items.SHIELD), 12, 10, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_3_DIAMOND_EMERALD,
            new VillagerTrade(new TradeCost(Items.DIAMOND, 1), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            ARMORER_4_EMERALD_ENCHANTED_DIAMOND_LEGGINGS,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 14),
                new ItemStackTemplate(Items.DIAMOND_LEGGINGS),
                3,
                15,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_LEGGINGS)
            )
        );
        register(
            context,
            ARMORER_4_EMERALD_ENCHANTED_DIAMOND_BOOTS,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 8),
                new ItemStackTemplate(Items.DIAMOND_BOOTS),
                3,
                15,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_BOOTS)
            )
        );
        register(
            context,
            ARMORER_5_EMERALD_ENCHANTED_DIAMOND_HELMET,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 8),
                new ItemStackTemplate(Items.DIAMOND_HELMET),
                3,
                30,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_HELMET)
            )
        );
        register(
            context,
            ARMORER_5_EMERALD_ENCHANTED_DIAMOND_CHESTPLATE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 16),
                new ItemStackTemplate(Items.DIAMOND_CHESTPLATE),
                3,
                30,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_CHESTPLATE)
            )
        );
        register(
            context,
            WEAPONSMITH_1_EMERALD_IRON_AXE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.IRON_AXE), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            WEAPONSMITH_1_EMERALD_ENCHANTED_IRON_SWORD,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 2),
                new ItemStackTemplate(Items.IRON_SWORD),
                12,
                1,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.IRON_SWORD)
            )
        );
        register(
            context,
            WEAPONSMITH_3_FLINT_EMERALD,
            new VillagerTrade(new TradeCost(Items.FLINT, 24), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            WEAPONSMITH_4_EMERALD_ENCHANTED_DIAMOND_AXE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 12),
                new ItemStackTemplate(Items.DIAMOND_AXE),
                3,
                15,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_AXE)
            )
        );
        register(
            context,
            WEAPONSMITH_4_DIAMOND_EMERALD,
            new VillagerTrade(new TradeCost(Items.DIAMOND, 1), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            WEAPONSMITH_5_EMERALD_ENCHANTED_DIAMOND_SWORD,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 8),
                new ItemStackTemplate(Items.DIAMOND_SWORD),
                3,
                30,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_SWORD)
            )
        );
        register(
            context,
            TOOLSMITH_1_EMERALD_STONE_AXE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.STONE_AXE), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            TOOLSMITH_1_EMERALD_STONE_SHOVEL,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.STONE_SHOVEL), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            TOOLSMITH_1_EMERALD_STONE_PICKAXE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.STONE_PICKAXE), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            TOOLSMITH_1_EMERALD_STONE_HOE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.STONE_HOE), 12, 1, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            TOOLSMITH_3_FLINT_EMERALD,
            new VillagerTrade(new TradeCost(Items.FLINT, 30), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            TOOLSMITH_3_EMERALD_IRON_AXE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 1),
                new ItemStackTemplate(Items.IRON_AXE),
                3,
                10,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.IRON_AXE)
            )
        );
        register(
            context,
            TOOLSMITH_3_EMERALD_IRON_SHOVEL,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 2),
                new ItemStackTemplate(Items.IRON_SHOVEL),
                3,
                10,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.IRON_SHOVEL)
            )
        );
        register(
            context,
            TOOLSMITH_3_EMERALD_IRON_PICKAXE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 3),
                new ItemStackTemplate(Items.IRON_PICKAXE),
                3,
                10,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.IRON_PICKAXE)
            )
        );
        register(
            context,
            TOOLSMITH_3_EMERALD_DIAMOND_HOE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 4), new ItemStackTemplate(Items.DIAMOND_HOE), 3, 10, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            TOOLSMITH_4_EMERALD_DIAMOND_AXE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 12),
                new ItemStackTemplate(Items.DIAMOND_AXE),
                3,
                15,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_AXE)
            )
        );
        register(
            context,
            TOOLSMITH_4_EMERALD_DIAMOND_SHOVEL,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 5),
                new ItemStackTemplate(Items.DIAMOND_SHOVEL),
                3,
                15,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_SHOVEL)
            )
        );
        register(
            context,
            TOOLSMITH_4_DIAMOND_EMERALD,
            new VillagerTrade(new TradeCost(Items.DIAMOND, 1), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            TOOLSMITH_5_EMERALD_DIAMOND_PICKAXE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 13),
                new ItemStackTemplate(Items.DIAMOND_PICKAXE),
                3,
                30,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.DIAMOND_PICKAXE)
            )
        );
        register(
            context,
            BUTCHER_1_CHICKEN_EMERALD,
            new VillagerTrade(new TradeCost(Items.CHICKEN, 14), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_1_PORKCHOP_EMERALD,
            new VillagerTrade(new TradeCost(Items.PORKCHOP, 7), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_1_RABBIT_EMERALD,
            new VillagerTrade(new TradeCost(Items.RABBIT, 4), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_1_EMERALD_RABBIT_STEW,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.RABBIT_STEW), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_2_COAL_EMERALD,
            new VillagerTrade(new TradeCost(Items.COAL, 15), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_2_EMERALD_COOKED_PORKCHOP,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.COOKED_PORKCHOP, 5), 16, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_2_EMERALD_COOKED_CHICKEN,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.COOKED_CHICKEN, 8), 16, 5, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_3_MUTTON_EMERALD,
            new VillagerTrade(new TradeCost(Items.MUTTON, 7), new ItemStackTemplate(Items.EMERALD), 16, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_3_BEEF_EMERALD,
            new VillagerTrade(new TradeCost(Items.BEEF, 10), new ItemStackTemplate(Items.EMERALD), 16, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_4_DRIED_KELP_BLOCK_EMERALD,
            new VillagerTrade(new TradeCost(Items.DRIED_KELP_BLOCK, 10), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            BUTCHER_5_SWEET_BERRIES_EMERALD,
            new VillagerTrade(new TradeCost(Items.SWEET_BERRIES, 10), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LEATHERWORKER_1_LEATHER_EMERALD,
            new VillagerTrade(new TradeCost(Items.LEATHER, 6), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LEATHERWORKER_1_EMERALD_DYED_LEATHER_LEGGINGS,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 3),
                new ItemStackTemplate(Items.LEATHER_LEGGINGS),
                12,
                1,
                0.2F,
                Optional.empty(),
                dyedItem(items, Items.LEATHER_LEGGINGS)
            )
        );
        register(
            context,
            LEATHERWORKER_1_EMERALD_DYED_LEATHER_CHESTPLATE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 7),
                new ItemStackTemplate(Items.LEATHER_CHESTPLATE),
                12,
                1,
                0.2F,
                Optional.empty(),
                dyedItem(items, Items.LEATHER_CHESTPLATE)
            )
        );
        register(
            context,
            LEATHERWORKER_2_FLINT_EMERALD,
            new VillagerTrade(new TradeCost(Items.FLINT, 26), new ItemStackTemplate(Items.EMERALD), 12, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LEATHERWORKER_2_EMERALD_DYED_LEATHER_HELMET,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 5),
                new ItemStackTemplate(Items.LEATHER_HELMET),
                12,
                5,
                0.2F,
                Optional.empty(),
                dyedItem(items, Items.LEATHER_HELMET)
            )
        );
        register(
            context,
            LEATHERWORKER_2_EMERALD_DYED_LEATHER_BOOTS,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 4),
                new ItemStackTemplate(Items.LEATHER_BOOTS),
                12,
                5,
                0.2F,
                Optional.empty(),
                dyedItem(items, Items.LEATHER_BOOTS)
            )
        );
        register(
            context,
            LEATHERWORKER_3_RABBIT_HIDE_EMERALD,
            new VillagerTrade(new TradeCost(Items.RABBIT_HIDE, 9), new ItemStackTemplate(Items.EMERALD), 12, 20, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LEATHERWORKER_3_EMERALD_DYED_LEATHER_CHESTPLATE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 7),
                new ItemStackTemplate(Items.LEATHER_CHESTPLATE),
                12,
                1,
                0.2F,
                Optional.empty(),
                dyedItem(items, Items.LEATHER_CHESTPLATE)
            )
        );
        register(
            context,
            LEATHERWORKER_4_TURTLE_SCUTE_EMERALD,
            new VillagerTrade(new TradeCost(Items.TURTLE_SCUTE, 4), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            LEATHERWORKER_4_EMERALD_DYED_LEATHER_HORSE_ARMOR,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 6),
                new ItemStackTemplate(Items.LEATHER_HORSE_ARMOR),
                12,
                15,
                0.2F,
                Optional.empty(),
                dyedItem(items, Items.LEATHER_HORSE_ARMOR)
            )
        );
        register(
            context,
            LEATHERWORKER_5_EMERALD_SADDLE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 6), new ItemStackTemplate(Items.SADDLE), 12, 30, 0.2F, Optional.empty(), List.of())
        );
        register(
            context,
            LEATHERWORKER_5_EMERALD_DYED_LEATHER_HELMET,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 5),
                new ItemStackTemplate(Items.LEATHER_HELMET),
                12,
                5,
                0.2F,
                Optional.empty(),
                dyedItem(items, Items.LEATHER_HELMET)
            )
        );
        register(
            context,
            MASON_1_CLAY_BALL_EMERALD,
            new VillagerTrade(new TradeCost(Items.CLAY_BALL, 10), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            MASON_1_EMERALD_BRICK,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.BRICK, 10), 16, 1, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            MASON_2_STONE_EMERALD,
            new VillagerTrade(new TradeCost(Items.STONE, 20), new ItemStackTemplate(Items.EMERALD), 16, 10, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            MASON_2_EMERALD_CHISELED_STONE_BRICKS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.CHISELED_STONE_BRICKS, 4), 16, 5, 0.05F, Optional.empty(), List.of())
        );
        registerMasonLevelThreeStones(context);
        registerMasonLevelThreeBlocks(context);
        register(
            context,
            MASON_4_QUARTZ_EMERALD,
            new VillagerTrade(new TradeCost(Items.QUARTZ, 12), new ItemStackTemplate(Items.EMERALD), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        registerMasonLevelFourTerracotta(context);
        register(
            context,
            MASON_5_EMERALD_QUARTZ_PILLAR,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.QUARTZ_PILLAR), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        register(
            context,
            MASON_5_EMERALD_QUARTZ_BLOCK,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.QUARTZ_BLOCK), 12, 30, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_WATER_BOTTLE_EMERALD,
            new VillagerTrade(
                new TradeCost(
                    Items.POTION.builtInRegistryHolder(),
                    ConstantValue.exactly(1.0F),
                    DataComponentExactPredicate.expect(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER))
                ),
                new ItemStackTemplate(Items.EMERALD),
                2,
                1,
                0.05F,
                Optional.empty(),
                List.of()
            )
        );
        context.register(
            WANDERING_TRADER_WATER_BUCKET_EMERALD,
            new VillagerTrade(new TradeCost(Items.WATER_BUCKET, 1), new ItemStackTemplate(Items.EMERALD, 2), 2, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_MILK_BUCKET_EMERALD,
            new VillagerTrade(new TradeCost(Items.MILK_BUCKET, 1), new ItemStackTemplate(Items.EMERALD, 2), 2, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_FERMENTED_SPIDER_EYE_EMERALD,
            new VillagerTrade(new TradeCost(Items.FERMENTED_SPIDER_EYE, 1), new ItemStackTemplate(Items.EMERALD, 3), 2, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_BAKED_POTATO_EMERALD,
            new VillagerTrade(new TradeCost(Items.BAKED_POTATO, 4), new ItemStackTemplate(Items.EMERALD, 1), 2, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_HAY_BLOCK_EMERALD,
            new VillagerTrade(new TradeCost(Items.HAY_BLOCK, 1), new ItemStackTemplate(Items.EMERALD), 2, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_PACKED_ICE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.PACKED_ICE), 6, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_BLUE_ICE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 6), new ItemStackTemplate(Items.BLUE_ICE), 6, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_GUNPOWDER,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.GUNPOWDER, 4), 2, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_PODZOL,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.PODZOL, 3), 6, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(WANDERING_TRADER_EMERALD_ACACIA_LOG, createWanderingTraderLogSell(Items.ACACIA_LOG));
        context.register(WANDERING_TRADER_EMERALD_BIRCH_LOG, createWanderingTraderLogSell(Items.BIRCH_LOG));
        context.register(WANDERING_TRADER_EMERALD_DARK_OAK_LOG, createWanderingTraderLogSell(Items.DARK_OAK_LOG));
        context.register(WANDERING_TRADER_EMERALD_JUNGLE_LOG, createWanderingTraderLogSell(Items.JUNGLE_LOG));
        context.register(WANDERING_TRADER_EMERALD_OAK_LOG, createWanderingTraderLogSell(Items.OAK_LOG));
        context.register(WANDERING_TRADER_EMERALD_SPRUCE_LOG, createWanderingTraderLogSell(Items.SPRUCE_LOG));
        context.register(WANDERING_TRADER_EMERALD_CHERRY_LOG, createWanderingTraderLogSell(Items.CHERRY_LOG));
        context.register(WANDERING_TRADER_EMERALD_MANGROVE_LOG, createWanderingTraderLogSell(Items.MANGROVE_LOG));
        context.register(WANDERING_TRADER_EMERALD_PALE_OAK_LOG, createWanderingTraderLogSell(Items.PALE_OAK_LOG));
        context.register(
            WANDERING_TRADER_EMERALD_ENCHANTED_IRON_PICKAXE,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 1),
                new ItemStackTemplate(Items.IRON_PICKAXE),
                1,
                1,
                0.2F,
                Optional.empty(),
                enchantedItem(items, enchantmentsForTradedEquipment, Items.IRON_PICKAXE)
            )
        );
        context.register(
            WANDERING_TRADER_EMERALD_LONG_INVISIBILITY_POTION,
            new VillagerTrade(
                new TradeCost(Items.EMERALD, 5),
                new ItemStackTemplate(Items.POTION),
                1,
                1,
                0.05F,
                Optional.empty(),
                List.of(SetPotionFunction.setPotion(Potions.LONG_INVISIBILITY).build())
            )
        );
        context.register(
            WANDERING_TRADER_EMERALD_TROPICAL_FISH_BUCKET,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.TROPICAL_FISH_BUCKET), 4, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_PUFFERFISH_BUCKET,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.PUFFERFISH_BUCKET), 4, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_SEA_PICKLE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 2), new ItemStackTemplate(Items.SEA_PICKLE), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_SLIME_BALL,
            new VillagerTrade(new TradeCost(Items.EMERALD, 4), new ItemStackTemplate(Items.SLIME_BALL), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_GLOWSTONE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 2), new ItemStackTemplate(Items.GLOWSTONE), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_NAUTILUS_SHELL,
            new VillagerTrade(new TradeCost(Items.EMERALD, 5), new ItemStackTemplate(Items.NAUTILUS_SHELL), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_FERN,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.FERN), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_SUGAR_CANE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.SUGAR_CANE), 8, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_PUMPKIN,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.PUMPKIN), 4, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_KELP,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.KELP), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_CACTUS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.CACTUS), 8, 1, 0.05F, Optional.empty(), List.of())
        );
        registerWanderingTraderFlowers(context);
        registerWanderingTraderSeeds(context);
        registerWanderingTraderSaplings(context);
        registerWanderingTraderDyes(context);
        context.register(WANDERING_TRADER_EMERALD_BRAIN_CORAL_BLOCK, createWanderingTraderCoralBlockSell(Items.BRAIN_CORAL_BLOCK));
        context.register(WANDERING_TRADER_EMERALD_BUBBLE_CORAL_BLOCK, createWanderingTraderCoralBlockSell(Items.BUBBLE_CORAL_BLOCK));
        context.register(WANDERING_TRADER_EMERALD_FIRE_CORAL_BLOCK, createWanderingTraderCoralBlockSell(Items.FIRE_CORAL_BLOCK));
        context.register(WANDERING_TRADER_EMERALD_HORN_CORAL_BLOCK, createWanderingTraderCoralBlockSell(Items.HORN_CORAL_BLOCK));
        context.register(WANDERING_TRADER_EMERALD_TUBE_CORAL_BLOCK, createWanderingTraderCoralBlockSell(Items.TUBE_CORAL_BLOCK));
        context.register(
            WANDERING_TRADER_EMERALD_VINE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.VINE, 3), 4, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_PALE_HANGING_MOSS,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.PALE_HANGING_MOSS, 3), 4, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_BROWN_MUSHROOM,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.BROWN_MUSHROOM, 3), 4, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_RED_MUSHROOM,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.RED_MUSHROOM, 3), 4, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_LILY_PAD,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.LILY_PAD, 5), 2, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_SMALL_DRIPLEAF,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.SMALL_DRIPLEAF, 2), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_SAND,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.SAND, 8), 8, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_RED_SAND,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.RED_SAND, 4), 6, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_POINTED_DRIPSTONE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.POINTED_DRIPSTONE, 2), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_SULFUR_SPIKE,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.SULFUR_SPIKE, 2), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_ROOTED_DIRT,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.ROOTED_DIRT, 2), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_MOSS_BLOCK,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.MOSS_BLOCK, 2), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_PALE_MOSS_BLOCK,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.PALE_MOSS_BLOCK, 2), 5, 1, 0.05F, Optional.empty(), List.of())
        );
        context.register(
            WANDERING_TRADER_EMERALD_FIREFLY_BUSH,
            new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(Items.FIREFLY_BUSH), 12, 1, 0.05F, Optional.empty(), List.of())
        );
        return context.register(
            WANDERING_TRADER_EMERALD_NAME_TAG,
            new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(Items.NAME_TAG), 5, 1, 0.05F, Optional.empty(), List.of())
        );
    }

    private static VillagerTrade createWanderingTraderCoralBlockSell(final Item item) {
        return new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(item), 8, 1, 0.05F, Optional.empty(), List.of());
    }

    private static VillagerTrade createWanderingTraderLogSell(final Item item) {
        return new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(item, 8), 4, 1, 0.05F, Optional.empty(), List.of());
    }

    private static void registerWanderingTraderFlowers(final BootstrapContext<VillagerTrade> context) {
        context.register(WANDERING_TRADER_EMERALD_BLUE_ORCHID, createWanderingTraderFlowerSell(Items.BLUE_ORCHID, 8));
        context.register(WANDERING_TRADER_EMERALD_DANDELION, createWanderingTraderFlowerSell(Items.DANDELION, 12));
        context.register(WANDERING_TRADER_EMERALD_POPPY, createWanderingTraderFlowerSell(Items.POPPY, 12));
        context.register(WANDERING_TRADER_EMERALD_ALLIUM, createWanderingTraderFlowerSell(Items.ALLIUM, 12));
        context.register(WANDERING_TRADER_EMERALD_AZURE_BLUET, createWanderingTraderFlowerSell(Items.AZURE_BLUET, 12));
        context.register(WANDERING_TRADER_EMERALD_RED_TULIP, createWanderingTraderFlowerSell(Items.RED_TULIP, 12));
        context.register(WANDERING_TRADER_EMERALD_ORANGE_TULIP, createWanderingTraderFlowerSell(Items.ORANGE_TULIP, 12));
        context.register(WANDERING_TRADER_EMERALD_WHITE_TULIP, createWanderingTraderFlowerSell(Items.WHITE_TULIP, 12));
        context.register(WANDERING_TRADER_EMERALD_PINK_TULIP, createWanderingTraderFlowerSell(Items.PINK_TULIP, 12));
        context.register(WANDERING_TRADER_EMERALD_OXEYE_DAISY, createWanderingTraderFlowerSell(Items.OXEYE_DAISY, 12));
        context.register(WANDERING_TRADER_EMERALD_CORNFLOWER, createWanderingTraderFlowerSell(Items.CORNFLOWER, 12));
        context.register(WANDERING_TRADER_EMERALD_WILDFLOWERS, createWanderingTraderFlowerSell(Items.WILDFLOWERS, 12));
        context.register(WANDERING_TRADER_EMERALD_DRY_TALL_GRASS, createWanderingTraderFlowerSell(Items.DRY_TALL_GRASS, 12));
        context.register(WANDERING_TRADER_EMERALD_LILY_OF_THE_VALLEY, createWanderingTraderFlowerSell(Items.LILY_OF_THE_VALLEY, 7));
        context.register(WANDERING_TRADER_EMERALD_OPEN_EYEBLOSSOM, createWanderingTraderFlowerSell(Items.OPEN_EYEBLOSSOM, 7));
        context.register(
            WANDERING_TRADER_EMERALD_GOLDEN_DANDELION,
            new VillagerTrade(new TradeCost(Items.EMERALD, 2), new ItemStackTemplate(Items.GOLDEN_DANDELION), 12, 1, 0.05F, Optional.empty(), List.of())
        );
    }

    private static VillagerTrade createWanderingTraderFlowerSell(final Item item, final int maxUses) {
        return new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(item), maxUses, 1, 0.05F, Optional.empty(), List.of());
    }

    private static void registerWanderingTraderSeeds(final BootstrapContext<VillagerTrade> context) {
        context.register(WANDERING_TRADER_EMERALD_WHEAT_SEEDS, createWanderingTraderSeedSell(Items.WHEAT_SEEDS));
        context.register(WANDERING_TRADER_EMERALD_BEETROOT_SEEDS, createWanderingTraderSeedSell(Items.BEETROOT_SEEDS));
        context.register(WANDERING_TRADER_EMERALD_PUMPKIN_SEEDS, createWanderingTraderSeedSell(Items.PUMPKIN_SEEDS));
        context.register(WANDERING_TRADER_EMERALD_MELON_SEEDS, createWanderingTraderSeedSell(Items.MELON_SEEDS));
    }

    private static VillagerTrade createWanderingTraderSeedSell(final Item item) {
        return new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(item), 12, 1, 0.05F, Optional.empty(), List.of());
    }

    private static void registerWanderingTraderSaplings(final BootstrapContext<VillagerTrade> context) {
        context.register(WANDERING_TRADER_EMERALD_ACACIA_SAPLING, createWanderingTraderSaplingSell(Items.ACACIA_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_BIRCH_SAPLING, createWanderingTraderSaplingSell(Items.BIRCH_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_DARK_OAK_SAPLING, createWanderingTraderSaplingSell(Items.DARK_OAK_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_JUNGLE_SAPLING, createWanderingTraderSaplingSell(Items.JUNGLE_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_OAK_SAPLING, createWanderingTraderSaplingSell(Items.OAK_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_SPRUCE_SAPLING, createWanderingTraderSaplingSell(Items.SPRUCE_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_CHERRY_SAPLING, createWanderingTraderSaplingSell(Items.CHERRY_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_PALE_OAK_SAPLING, createWanderingTraderSaplingSell(Items.PALE_OAK_SAPLING));
        context.register(WANDERING_TRADER_EMERALD_MANGROVE_PROPAGULE, createWanderingTraderSaplingSell(Items.MANGROVE_PROPAGULE));
    }

    private static VillagerTrade createWanderingTraderSaplingSell(final Item item) {
        return new VillagerTrade(new TradeCost(Items.EMERALD, 5), new ItemStackTemplate(item), 8, 1, 0.05F, Optional.empty(), List.of());
    }

    private static void registerWanderingTraderDyes(final BootstrapContext<VillagerTrade> context) {
        ColorCollection.zipApply(
            WANDERING_TRADER_EMERALD_DYE,
            Items.DYE,
            (name, dye) -> context.register(
                name, new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(dye, 3), 12, 1, 0.05F, Optional.empty(), List.of())
            )
        );
    }

    private static void registerMasonLevelFourTerracotta(final BootstrapContext<VillagerTrade> context) {
        registerWanderingTraderTerracottaSellTrades(context, MASON_4_EMERALD_TERRACOTTA, Items.DYED_TERRACOTTA);
        registerWanderingTraderTerracottaSellTrades(context, MASON_4_EMERALD_GLAZED_TERRACOTTA, Items.GLAZED_TERRACOTTA);
    }

    private static void registerWanderingTraderTerracottaSellTrades(
        final BootstrapContext<VillagerTrade> context, final ColorCollection<ResourceKey<VillagerTrade>> trades, final ColorCollection<Item> items
    ) {
        ColorCollection.zipApply(
            trades,
            items,
            (trade, item) -> register(
                context, trade, new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(item), 12, 15, 0.05F, Optional.empty(), List.of())
            )
        );
    }

    private static void registerMasonLevelThreeBlocks(final BootstrapContext<VillagerTrade> context) {
        register(context, MASON_3_EMERALD_DRIPSTONE_BLOCK, createMasonStoneSell(Items.DRIPSTONE_BLOCK));
        register(context, MASON_3_EMERALD_POLISHED_ANDESITE, createMasonStoneSell(Items.POLISHED_ANDESITE));
        register(context, MASON_3_EMERALD_POLISHED_DIORITE, createMasonStoneSell(Items.POLISHED_DIORITE));
        register(context, MASON_3_EMERALD_POLISHED_GRANTITE, createMasonStoneSell(Items.POLISHED_GRANITE));
    }

    private static VillagerTrade createMasonStoneSell(final Item item) {
        return new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(item, 4), 16, 10, 0.05F, Optional.empty(), List.of());
    }

    private static void registerMasonLevelThreeStones(final BootstrapContext<VillagerTrade> context) {
        register(context, MASON_3_GRANITE_EMERALD, createMasonStoneBuy(Items.GRANITE));
        register(context, MASON_3_ANDESITE_EMERALD, createMasonStoneBuy(Items.ANDESITE));
        register(context, MASON_3_DIORITE_EMERALD, createMasonStoneBuy(Items.DIORITE));
    }

    private static VillagerTrade createMasonStoneBuy(final Item item) {
        return new VillagerTrade(new TradeCost(item, 16), new ItemStackTemplate(Items.EMERALD), 16, 20, 0.05F, Optional.empty(), List.of());
    }

    private static void registerBoatTrades(final BootstrapContext<VillagerTrade> context, final HolderGetter<VillagerType> villagerVariants) {
        register(context, FISHERMAN_5_OAK_BOAT_EMERALD, createMasonBoatBuyTrade(villagerVariants, Items.OAK_BOAT, List.of(VillagerType.PLAINS)));
        register(
            context,
            FISHERMAN_5_SPRUCE_BOAT_EMERALD,
            createMasonBoatBuyTrade(villagerVariants, Items.SPRUCE_BOAT, List.of(VillagerType.TAIGA, VillagerType.SNOW))
        );
        register(
            context,
            FISHERMAN_5_JUNGLE_BOAT_EMERALD,
            createMasonBoatBuyTrade(villagerVariants, Items.JUNGLE_BOAT, List.of(VillagerType.DESERT, VillagerType.JUNGLE))
        );
        register(context, FISHERMAN_5_ACACIA_BOAT_EMERALD, createMasonBoatBuyTrade(villagerVariants, Items.ACACIA_BOAT, List.of(VillagerType.SAVANNA)));
        register(context, FISHERMAN_5_DARK_OAK_BOAT_EMERALD, createMasonBoatBuyTrade(villagerVariants, Items.DARK_OAK_BOAT, List.of(VillagerType.SWAMP)));
    }

    private static VillagerTrade createMasonBoatBuyTrade(
        final HolderGetter<VillagerType> villagerVariants, final Item item, final List<ResourceKey<VillagerType>> villagerTypes
    ) {
        return new VillagerTrade(
            new TradeCost(item, 1),
            new ItemStackTemplate(Items.EMERALD),
            12,
            30,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, villagerTypes)),
            List.of()
        );
    }

    private static void registerShepherdWoolSales(final BootstrapContext<VillagerTrade> context) {
        register(context, SHEPHERD_1_WHITE_WOOL_EMERALD, createShepherdWoolBuy(Items.WOOL.white()));
        register(context, SHEPHERD_1_BROWN_WOOL_EMERALD, createShepherdWoolBuy(Items.WOOL.brown()));
        register(context, SHEPHERD_1_GRAY_WOOL_EMERALD, createShepherdWoolBuy(Items.WOOL.gray()));
        register(context, SHEPHERD_1_BLACK_WOOL_EMERALD, createShepherdWoolBuy(Items.WOOL.black()));
    }

    private static VillagerTrade createShepherdWoolBuy(final Item item) {
        return new VillagerTrade(new TradeCost(item, 18), new ItemStackTemplate(Items.EMERALD), 16, 2, 0.05F, Optional.empty(), List.of());
    }

    private static void registerShepherdLevelTwoDyeTrades(final BootstrapContext<VillagerTrade> context) {
        register(context, SHEPHERD_2_WHITE_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.white(), 10));
        register(context, SHEPHERD_2_GRAY_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.gray(), 10));
        register(context, SHEPHERD_2_BLACK_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.black(), 10));
        register(context, SHEPHERD_2_LIGHT_BLUE_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.lightBlue(), 10));
        register(context, SHEPHERD_2_LIME_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.lime(), 10));
    }

    private static void registerShepherdLevelThreeDyeTrades(final BootstrapContext<VillagerTrade> context) {
        register(context, SHEPHERD_3_YELLOW_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.yellow(), 20));
        register(context, SHEPHERD_3_LIGHT_GRAY_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.lightGray(), 20));
        register(context, SHEPHERD_3_ORANGE_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.orange(), 20));
        register(context, SHEPHERD_3_RED_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.red(), 20));
        register(context, SHEPHERD_3_PINK_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.pink(), 20));
    }

    private static void registerLevelFourDyeTrades(final BootstrapContext<VillagerTrade> context) {
        register(context, SHEPHERD_4_BROWN_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.brown(), 30));
        register(context, SHEPHERD_4_PURPLE_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.purple(), 30));
        register(context, SHEPHERD_4_BLUE_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.blue(), 30));
        register(context, SHEPHERD_4_GREEN_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.green(), 30));
        register(context, SHEPHERD_4_MAGENTA_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.magenta(), 30));
        register(context, SHEPHERD_4_CYAN_DYE_EMERALD, createShepherdDyeBuy(Items.DYE.cyan(), 30));
    }

    private static VillagerTrade createShepherdDyeBuy(final Item item, final int xp) {
        return new VillagerTrade(new TradeCost(item, 12), new ItemStackTemplate(Items.EMERALD), 16, xp, 0.05F, Optional.empty(), List.of());
    }

    private static void registerWoolPurchases(final BootstrapContext<VillagerTrade> context) {
        ColorCollection.zipApply(
            SHEPHERD_2_EMERALD_WOOL,
            Items.WOOL,
            (name, wool) -> register(
                context, name, new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(wool), 16, 5, 0.05F, Optional.empty(), List.of())
            )
        );
    }

    private static void registerCarpetPurchases(final BootstrapContext<VillagerTrade> context) {
        ColorCollection.zipApply(
            SHEPHERD_2_EMERALD_CARPETS,
            Items.CARPET,
            (name, carpet) -> register(
                context, name, new VillagerTrade(new TradeCost(Items.EMERALD, 1), new ItemStackTemplate(carpet, 4), 16, 5, 0.05F, Optional.empty(), List.of())
            )
        );
    }

    private static void registerBedTrades(final BootstrapContext<VillagerTrade> context) {
        ColorCollection.zipApply(
            SHEPHERD_3_EMERALD_BED,
            Items.BED,
            (name, bed) -> register(
                context, name, new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(bed), 12, 10, 0.05F, Optional.empty(), List.of())
            )
        );
    }

    private static void registerShepherdBannerTrades(final BootstrapContext<VillagerTrade> context) {
        ColorCollection.zipApply(
            SHEPHERD_4_EMERALD_BANNER,
            Items.BANNER,
            (name, banner) -> register(
                context, name, new VillagerTrade(new TradeCost(Items.EMERALD, 3), new ItemStackTemplate(banner), 12, 15, 0.05F, Optional.empty(), List.of())
            )
        );
    }

    private static void registerCartographerBannerTrades(final BootstrapContext<VillagerTrade> context, final HolderGetter<VillagerType> villagerVariants) {
        register(
            context,
            CARTOGRAPHER_4_EMERALD_WHITE_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.white(), List.of(VillagerType.SNOW, VillagerType.PLAINS))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_ORANGE_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.orange(), List.of(VillagerType.SAVANNA, VillagerType.DESERT))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_MAGENTA_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.magenta(), List.of(VillagerType.SAVANNA))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_BLUE_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.blue(), List.of(VillagerType.SNOW, VillagerType.TAIGA))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_LIGHT_BLUE_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.lightBlue(), List.of(VillagerType.SNOW, VillagerType.SWAMP))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_YELLOW_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.yellow(), List.of(VillagerType.PLAINS, VillagerType.JUNGLE))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_LIME_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.lime(), List.of(VillagerType.DESERT, VillagerType.TAIGA))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_PINK_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.pink(), List.of(VillagerType.TAIGA, VillagerType.PLAINS))
        );
        register(context, CARTOGRAPHER_4_EMERALD_GRAY_BANNER, createCartographerBannerSell(villagerVariants, Items.BANNER.gray(), List.of(VillagerType.DESERT)));
        register(
            context,
            CARTOGRAPHER_4_EMERALD_CYAN_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.cyan(), List.of(VillagerType.DESERT, VillagerType.SNOW))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_PURPLE_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.purple(), List.of(VillagerType.TAIGA, VillagerType.SWAMP))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_BROWN_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.brown(), List.of(VillagerType.PLAINS, VillagerType.JUNGLE))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_GREEN_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.green(), List.of(VillagerType.DESERT, VillagerType.SAVANNA, VillagerType.JUNGLE))
        );
        register(
            context,
            CARTOGRAPHER_4_EMERALD_RED_BANNER,
            createCartographerBannerSell(villagerVariants, Items.BANNER.red(), List.of(VillagerType.SNOW, VillagerType.SAVANNA))
        );
        register(
            context, CARTOGRAPHER_4_EMERALD_BLACK_BANNER, createCartographerBannerSell(villagerVariants, Items.BANNER.black(), List.of(VillagerType.SWAMP))
        );
    }

    private static VillagerTrade createCartographerBannerSell(
        final HolderGetter<VillagerType> villagerVariants, final Item item, final List<ResourceKey<VillagerType>> villagerTypes
    ) {
        return new VillagerTrade(
            new TradeCost(Items.EMERALD, 2),
            new ItemStackTemplate(item),
            12,
            15,
            0.05F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, villagerTypes)),
            List.of()
        );
    }

    private static void registerBasicExplorerMapTrades(
        final BootstrapContext<VillagerTrade> context, final HolderGetter<Item> items, final HolderGetter<VillagerType> villagerVariants
    ) {
        register(
            context,
            CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_TAIGA_MAP,
            createBasicExplorerMapTrades(
                items,
                villagerVariants,
                StructureTags.ON_TAIGA_VILLAGE_MAPS,
                MapDecorationTypes.TAIGA_VILLAGE,
                "village_taiga",
                List.of(VillagerType.SWAMP, VillagerType.SNOW, VillagerType.PLAINS)
            )
        );
        register(
            context,
            CARTOGRAPHER_2_EMERALD_AND_COMPASS_EXPLORER_SWAMP_MAP,
            createBasicExplorerMapTrades(
                items,
                villagerVariants,
                StructureTags.ON_SWAMP_EXPLORER_MAPS,
                MapDecorationTypes.SWAMP_HUT,
                "explorer_swamp",
                List.of(VillagerType.TAIGA, VillagerType.SNOW, VillagerType.JUNGLE)
            )
        );
        register(
            context,
            CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_SNOWY_MAP,
            createBasicExplorerMapTrades(
                items,
                villagerVariants,
                StructureTags.ON_SNOWY_VILLAGE_MAPS,
                MapDecorationTypes.SNOWY_VILLAGE,
                "village_snowy",
                List.of(VillagerType.TAIGA, VillagerType.SWAMP)
            )
        );
        register(
            context,
            CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_SAVANNA_MAP,
            createBasicExplorerMapTrades(
                items,
                villagerVariants,
                StructureTags.ON_SAVANNA_VILLAGE_MAPS,
                MapDecorationTypes.SAVANNA_VILLAGE,
                "village_savanna",
                List.of(VillagerType.PLAINS, VillagerType.JUNGLE, VillagerType.DESERT)
            )
        );
        register(
            context,
            CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_PLAINS_MAP,
            createBasicExplorerMapTrades(
                items,
                villagerVariants,
                StructureTags.ON_PLAINS_VILLAGE_MAPS,
                MapDecorationTypes.PLAINS_VILLAGE,
                "village_plains",
                List.of(VillagerType.TAIGA, VillagerType.SNOW, VillagerType.SAVANNA, VillagerType.DESERT)
            )
        );
        register(
            context,
            CARTOGRAPHER_2_EMERALD_AND_COMPASS_EXPLORER_JUNGLE_MAP,
            createBasicExplorerMapTrades(
                items,
                villagerVariants,
                StructureTags.ON_JUNGLE_EXPLORER_MAPS,
                MapDecorationTypes.JUNGLE_TEMPLE,
                "explorer_jungle",
                List.of(VillagerType.SWAMP, VillagerType.SAVANNA, VillagerType.DESERT)
            )
        );
        register(
            context,
            CARTOGRAPHER_2_EMERALD_AND_COMPASS_VILLAGE_DESERT_MAP,
            createBasicExplorerMapTrades(
                items,
                villagerVariants,
                StructureTags.ON_DESERT_VILLAGE_MAPS,
                MapDecorationTypes.DESERT_VILLAGE,
                "village_desert",
                List.of(VillagerType.SAVANNA, VillagerType.JUNGLE)
            )
        );
    }

    private static VillagerTrade createBasicExplorerMapTrades(
        final HolderGetter<Item> items,
        final HolderGetter<VillagerType> villagerVariants,
        final TagKey<Structure> structureTagKey,
        final Holder<MapDecorationType> mapDecorationType,
        final String translationKey,
        final List<ResourceKey<VillagerType>> villagerTypes
    ) {
        return new VillagerTrade(
            new TradeCost(Items.EMERALD, 8),
            Optional.of(new TradeCost(Items.COMPASS, 1)),
            new ItemStackTemplate(Items.MAP),
            12,
            5,
            0.2F,
            villagerTypeRestriction(villagerTypeHolderSet(villagerVariants, villagerTypes)),
            List.of(
                ExplorationMapFunction.makeExplorationMap()
                    .setDestination(structureTagKey)
                    .setMapDecoration(mapDecorationType)
                    .setSearchRadius(100)
                    .setSkipKnownStructures(true)
                    .build(),
                SetNameFunction.setName(Component.translatable("filled_map." + translationKey), SetNameFunction.Target.ITEM_NAME).build(),
                FilteredFunction.filtered(
                        new ItemPredicate.Builder()
                            .of(items, Items.FILLED_MAP)
                            .withComponents(DataComponentMatchers.Builder.components().any(DataComponents.MAP_ID).build())
                            .build()
                    )
                    .onFail(Optional.of(DiscardItem.discardItem().build()))
                    .build()
            )
        );
    }

    public static Holder.Reference<VillagerTrade> register(
        final BootstrapContext<VillagerTrade> context, final ResourceKey<VillagerTrade> resourceKey, final VillagerTrade villagerTrade
    ) {
        return context.register(resourceKey, villagerTrade);
    }

    public static ResourceKey<VillagerTrade> resourceKey(final String path) {
        return ResourceKey.create(Registries.VILLAGER_TRADE, Identifier.withDefaultNamespace(path));
    }

    public static HolderSet<VillagerType> villagerTypeHolderSet(final HolderGetter<VillagerType> villagerVariants, final ResourceKey<VillagerType> resourceKey) {
        return HolderSet.direct(villagerVariants.getOrThrow(resourceKey));
    }

    public static HolderSet<VillagerType> villagerTypeHolderSet(
        final HolderGetter<VillagerType> villagerVariants, final List<ResourceKey<VillagerType>> resourceKeys
    ) {
        List<Holder<VillagerType>> villagerTypes = new ArrayList<>();

        for (ResourceKey<VillagerType> resourceKey : resourceKeys) {
            villagerTypes.add(villagerVariants.getOrThrow(resourceKey));
        }

        return HolderSet.direct(villagerTypes);
    }

    public static Optional<LootItemCondition> villagerTypeRestriction(final HolderSet<VillagerType> villagerTypes) {
        return Optional.of(
            new LootItemEntityPropertyCondition(
                Optional.of(
                    EntityPredicate.Builder.entity()
                        .components(
                            DataComponentMatchers.Builder.components()
                                .partial(DataComponentPredicates.VILLAGER_VARIANT, VillagerTypePredicate.villagerTypes(villagerTypes))
                                .build()
                        )
                        .build()
                ),
                LootContext.EntityTarget.THIS
            )
        );
    }

    private static List<LootItemFunction> dyedItem(final HolderGetter<Item> items, final Item expectedItem) {
        return List.of(
            addRandomDye(),
            FilteredFunction.filtered(
                    new ItemPredicate.Builder()
                        .of(items, expectedItem)
                        .withComponents(DataComponentMatchers.Builder.components().any(DataComponents.DYED_COLOR).build())
                        .build()
                )
                .onFail(Optional.of(DiscardItem.discardItem().build()))
                .build()
        );
    }

    private static LootItemFunction addRandomDye() {
        return SetRandomDyesFunction.withCount(
                Sum.sum(ConstantValue.exactly(1.0F), new BinomialDistributionGenerator(ConstantValue.exactly(2.0F), ConstantValue.exactly(0.75F)))
            )
            .build();
    }

    public static List<LootItemFunction> enchantedBook(final HolderGetter<Item> items, final HolderSet<Enchantment> options) {
        return List.of(
            new EnchantRandomlyFunction.Builder().withOptions(options).allowingIncompatibleEnchantments().includeAdditionalCostComponent().build(),
            FilteredFunction.filtered(
                    new ItemPredicate.Builder()
                        .of(items, Items.ENCHANTED_BOOK)
                        .withComponents(
                            DataComponentMatchers.Builder.components()
                                .partial(
                                    DataComponentPredicates.STORED_ENCHANTMENTS,
                                    EnchantmentsPredicate.storedEnchantments(List.of(new EnchantmentPredicate(Optional.empty(), MinMaxBounds.Ints.ANY)))
                                )
                                .build()
                        )
                        .build()
                )
                .onFail(Optional.of(DiscardItem.discardItem().build()))
                .build()
        );
    }

    public static List<LootItemFunction> enchantedBook(final HolderGetter<Item> items, final Holder<Enchantment> enchantment, final int level) {
        return List.of(
            new SetEnchantmentsFunction.Builder().withEnchantment(enchantment, ConstantValue.exactly(level)).build(),
            FilteredFunction.filtered(
                    new ItemPredicate.Builder()
                        .of(items, Items.ENCHANTED_BOOK)
                        .withComponents(
                            DataComponentMatchers.Builder.components()
                                .partial(
                                    DataComponentPredicates.STORED_ENCHANTMENTS,
                                    EnchantmentsPredicate.storedEnchantments(
                                        List.of(new EnchantmentPredicate(Optional.empty(), MinMaxBounds.Ints.exactly(level)))
                                    )
                                )
                                .build()
                        )
                        .build()
                )
                .onFail(Optional.of(DiscardItem.discardItem().build()))
                .build()
        );
    }

    public static List<LootItemFunction> enchantedItem(final HolderGetter<Item> items, final HolderSet<Enchantment> options, final Item expectedItem) {
        return List.of(
            new EnchantWithLevelsFunction.Builder(UniformGenerator.between(5.0F, 19.0F)).withOptions(options).includeAdditionalCostComponent().build(),
            FilteredFunction.filtered(
                    new ItemPredicate.Builder()
                        .of(items, expectedItem)
                        .withComponents(
                            DataComponentMatchers.Builder.components()
                                .partial(
                                    DataComponentPredicates.ENCHANTMENTS,
                                    EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(Optional.empty(), MinMaxBounds.Ints.ANY)))
                                )
                                .build()
                        )
                        .build()
                )
                .onFail(Optional.of(DiscardItem.discardItem().build()))
                .build()
        );
    }

    public static List<LootItemFunction> enchantedItem(
        final HolderGetter<Item> items, final Holder<Enchantment> enchantment, final int level, final Item expectedItem
    ) {
        return List.of(
            new SetEnchantmentsFunction.Builder().withEnchantment(enchantment, ConstantValue.exactly(level)).build(),
            FilteredFunction.filtered(
                    new ItemPredicate.Builder()
                        .of(items, expectedItem)
                        .withComponents(
                            DataComponentMatchers.Builder.components()
                                .partial(
                                    DataComponentPredicates.ENCHANTMENTS,
                                    EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(Optional.empty(), MinMaxBounds.Ints.exactly(level))))
                                )
                                .build()
                        )
                        .build()
                )
                .onFail(Optional.of(DiscardItem.discardItem().build()))
                .build()
        );
    }
}