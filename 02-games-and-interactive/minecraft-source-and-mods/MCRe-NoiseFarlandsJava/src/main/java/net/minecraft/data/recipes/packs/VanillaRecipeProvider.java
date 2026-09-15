package net.minecraft.data.recipes.packs;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.CustomCraftingRecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.data.recipes.TransmuteRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.crafting.BookCloningRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.DecoratedPotRecipe;
import net.minecraft.world.item.crafting.FireworkRocketRecipe;
import net.minecraft.world.item.crafting.FireworkStarFadeRecipe;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.item.crafting.ImbueRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.MapExtendingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.item.crafting.ShieldDecorationRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.WeatheringCopperCollection;

public class VanillaRecipeProvider extends RecipeProvider {
    private static final ImmutableList<ItemLike> COAL_SMELTABLES = ImmutableList.of(Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE);
    private static final ImmutableList<ItemLike> IRON_SMELTABLES = ImmutableList.of(Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON);
    private static final ImmutableList<ItemLike> COPPER_SMELTABLES = ImmutableList.of(Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER);
    private static final ImmutableList<ItemLike> GOLD_SMELTABLES = ImmutableList.of(
        Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE, Items.RAW_GOLD
    );
    private static final ImmutableList<ItemLike> DIAMOND_SMELTABLES = ImmutableList.of(Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE);
    private static final ImmutableList<ItemLike> LAPIS_SMELTABLES = ImmutableList.of(Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE);
    private static final ImmutableList<ItemLike> REDSTONE_SMELTABLES = ImmutableList.of(Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE);
    private static final ImmutableList<ItemLike> EMERALD_SMELTABLES = ImmutableList.of(Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE);

    private VanillaRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        this.output.includeRootAdvancement();
        this.generateForEnabledBlockFamilies(FeatureFlagSet.of(FeatureFlags.VANILLA));
        this.planksFromLog(Blocks.ACACIA_PLANKS, ItemTags.ACACIA_LOGS, 4);
        this.planksFromLogs(Blocks.BIRCH_PLANKS, ItemTags.BIRCH_LOGS, 4);
        this.planksFromLogs(Blocks.CRIMSON_PLANKS, ItemTags.CRIMSON_STEMS, 4);
        this.planksFromLog(Blocks.DARK_OAK_PLANKS, ItemTags.DARK_OAK_LOGS, 4);
        this.planksFromLog(Blocks.PALE_OAK_PLANKS, ItemTags.PALE_OAK_LOGS, 4);
        this.planksFromLogs(Blocks.JUNGLE_PLANKS, ItemTags.JUNGLE_LOGS, 4);
        this.planksFromLogs(Blocks.OAK_PLANKS, ItemTags.OAK_LOGS, 4);
        this.planksFromLogs(Blocks.SPRUCE_PLANKS, ItemTags.SPRUCE_LOGS, 4);
        this.planksFromLogs(Blocks.WARPED_PLANKS, ItemTags.WARPED_STEMS, 4);
        this.planksFromLogs(Blocks.MANGROVE_PLANKS, ItemTags.MANGROVE_LOGS, 4);
        this.woodFromLogs(Blocks.ACACIA_WOOD, Blocks.ACACIA_LOG);
        this.woodFromLogs(Blocks.BIRCH_WOOD, Blocks.BIRCH_LOG);
        this.woodFromLogs(Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LOG);
        this.woodFromLogs(Blocks.PALE_OAK_WOOD, Blocks.PALE_OAK_LOG);
        this.woodFromLogs(Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LOG);
        this.woodFromLogs(Blocks.OAK_WOOD, Blocks.OAK_LOG);
        this.woodFromLogs(Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LOG);
        this.woodFromLogs(Blocks.CRIMSON_HYPHAE, Blocks.CRIMSON_STEM);
        this.woodFromLogs(Blocks.WARPED_HYPHAE, Blocks.WARPED_STEM);
        this.woodFromLogs(Blocks.MANGROVE_WOOD, Blocks.MANGROVE_LOG);
        this.woodFromLogs(Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG);
        this.woodFromLogs(Blocks.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG);
        this.woodFromLogs(Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG);
        this.woodFromLogs(Blocks.STRIPPED_PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_LOG);
        this.woodFromLogs(Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_LOG);
        this.woodFromLogs(Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_OAK_LOG);
        this.woodFromLogs(Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_LOG);
        this.woodFromLogs(Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM);
        this.woodFromLogs(Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM);
        this.woodFromLogs(Blocks.STRIPPED_MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_LOG);
        this.woodenBoat(Items.ACACIA_BOAT, Blocks.ACACIA_PLANKS);
        this.woodenBoat(Items.BIRCH_BOAT, Blocks.BIRCH_PLANKS);
        this.woodenBoat(Items.DARK_OAK_BOAT, Blocks.DARK_OAK_PLANKS);
        this.woodenBoat(Items.PALE_OAK_BOAT, Blocks.PALE_OAK_PLANKS);
        this.woodenBoat(Items.JUNGLE_BOAT, Blocks.JUNGLE_PLANKS);
        this.woodenBoat(Items.OAK_BOAT, Blocks.OAK_PLANKS);
        this.woodenBoat(Items.SPRUCE_BOAT, Blocks.SPRUCE_PLANKS);
        this.woodenBoat(Items.MANGROVE_BOAT, Blocks.MANGROVE_PLANKS);
        this.shelf(Blocks.ACACIA_SHELF, Items.STRIPPED_ACACIA_LOG);
        this.shelf(Blocks.BAMBOO_SHELF, Items.STRIPPED_BAMBOO_BLOCK);
        this.shelf(Blocks.BIRCH_SHELF, Items.STRIPPED_BIRCH_LOG);
        this.shelf(Blocks.CHERRY_SHELF, Items.STRIPPED_CHERRY_LOG);
        this.shelf(Blocks.CRIMSON_SHELF, Items.STRIPPED_CRIMSON_STEM);
        this.shelf(Blocks.DARK_OAK_SHELF, Items.STRIPPED_DARK_OAK_LOG);
        this.shelf(Blocks.JUNGLE_SHELF, Items.STRIPPED_JUNGLE_LOG);
        this.shelf(Blocks.MANGROVE_SHELF, Items.STRIPPED_MANGROVE_LOG);
        this.shelf(Blocks.OAK_SHELF, Items.STRIPPED_OAK_LOG);
        this.shelf(Blocks.PALE_OAK_SHELF, Items.STRIPPED_PALE_OAK_LOG);
        this.shelf(Blocks.SPRUCE_SHELF, Items.STRIPPED_SPRUCE_LOG);
        this.shelf(Blocks.WARPED_SHELF, Items.STRIPPED_WARPED_STEM);
        List<Item> dyes = Items.DYE.asList();
        this.colorItemWithDye(dyes, Items.WOOL.asList(), "wool", RecipeCategory.BUILDING_BLOCKS);
        this.colorItemWithDye(dyes, Items.BED.asList(), "bed_dye", RecipeCategory.DECORATIONS);
        this.colorItemWithDye(dyes, Items.CARPET.asList(), "carpet_dye", RecipeCategory.DECORATIONS);
        this.colorItemWithDye(dyes, Items.HARNESS.asList(), "harness_dye", RecipeCategory.COMBAT);
        ColorCollection.zipApply(Blocks.CARPET, Blocks.WOOL, (x$0, x$1) -> this.carpet(x$0, x$1));
        ColorCollection.zipApply(Items.BED, Blocks.WOOL, (x$0, x$1) -> this.bedFromPlanksAndWool(x$0, x$1));
        ColorCollection.zipApply(Items.BANNER, Blocks.WOOL, (x$0, x$1) -> this.banner(x$0, x$1));
        this.carpet(Blocks.MOSS_CARPET, Blocks.MOSS_BLOCK);
        this.carpet(Blocks.PALE_MOSS_CARPET, Blocks.PALE_MOSS_BLOCK);
        ColorCollection.zipApply(Items.HARNESS, Blocks.WOOL, (x$0, x$1) -> this.harness(x$0, x$1));
        ColorCollection.zipApply(Blocks.STAINED_GLASS, Items.DYE, (x$0, x$1) -> this.stainedGlassFromGlassAndDye(x$0, x$1));
        ColorCollection.zipApply(Blocks.STAINED_GLASS_PANE, Blocks.STAINED_GLASS, (x$0, x$1) -> this.stainedGlassPaneFromStainedGlass(x$0, x$1));
        ColorCollection.zipApply(Blocks.STAINED_GLASS_PANE, Items.DYE, (x$0, x$1) -> this.stainedGlassPaneFromGlassPaneAndDye(x$0, x$1));
        ColorCollection.zipApply(Blocks.DYED_TERRACOTTA, Items.DYE, (x$0, x$1) -> this.coloredTerracottaFromTerracottaAndDye(x$0, x$1));
        ColorCollection.zipApply(Blocks.CONCRETE_POWDER, Items.DYE, (x$0, x$1) -> this.concretePowder(x$0, x$1));
        this.dryGhast(Blocks.DRIED_GHAST);
        this.shaped(RecipeCategory.DECORATIONS, Items.CANDLE)
            .define('S', Items.STRING)
            .define('H', Items.HONEYCOMB)
            .pattern("S")
            .pattern("H")
            .unlockedBy("has_string", this.has(Items.STRING))
            .unlockedBy("has_honeycomb", this.has(Items.HONEYCOMB))
            .save(this.output);
        ColorCollection.zipApply(Blocks.DYED_CANDLE, Items.DYE, (x$0, x$1) -> this.candle(x$0, x$1));
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.PACKED_MUD, 1)
            .requires(Blocks.MUD)
            .requires(Items.WHEAT)
            .unlockedBy("has_mud", this.has(Blocks.MUD))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.MUD_BRICKS, 4)
            .define('#', Blocks.PACKED_MUD)
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_packed_mud", this.has(Blocks.PACKED_MUD))
            .save(this.output);
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MUDDY_MANGROVE_ROOTS, 1)
            .requires(Blocks.MUD)
            .requires(Items.MANGROVE_ROOTS)
            .unlockedBy("has_mangrove_roots", this.has(Blocks.MANGROVE_ROOTS))
            .save(this.output);
        this.shaped(RecipeCategory.TRANSPORTATION, Blocks.ACTIVATOR_RAIL, 6)
            .define('#', Blocks.REDSTONE_TORCH)
            .define('S', Items.STICK)
            .define('X', Items.IRON_INGOT)
            .pattern("XSX")
            .pattern("X#X")
            .pattern("XSX")
            .unlockedBy("has_rail", this.has(Blocks.RAIL))
            .save(this.output);
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.ANDESITE, 2)
            .requires(Blocks.DIORITE)
            .requires(Blocks.COBBLESTONE)
            .unlockedBy("has_stone", this.has(Blocks.DIORITE))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.ANVIL)
            .define('I', Blocks.IRON_BLOCK)
            .define('i', Items.IRON_INGOT)
            .pattern("III")
            .pattern(" i ")
            .pattern("iii")
            .unlockedBy("has_iron_block", this.has(Blocks.IRON_BLOCK))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Items.ARMOR_STAND)
            .define('/', Items.STICK)
            .define('_', Blocks.SMOOTH_STONE_SLAB)
            .pattern("///")
            .pattern(" / ")
            .pattern("/_/")
            .unlockedBy("has_stone_slab", this.has(Blocks.SMOOTH_STONE_SLAB))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.ARROW, 4)
            .define('#', Items.STICK)
            .define('X', Items.FLINT)
            .define('Y', Items.FEATHER)
            .pattern("X")
            .pattern("#")
            .pattern("Y")
            .unlockedBy("has_feather", this.has(Items.FEATHER))
            .unlockedBy("has_flint", this.has(Items.FLINT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.BARREL, 1)
            .define('P', ItemTags.PLANKS)
            .define('S', ItemTags.WOODEN_SLABS)
            .pattern("PSP")
            .pattern("P P")
            .pattern("PSP")
            .unlockedBy("has_planks", this.has(ItemTags.PLANKS))
            .unlockedBy("has_wood_slab", this.has(ItemTags.WOODEN_SLABS))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Blocks.BEACON)
            .define('S', Items.NETHER_STAR)
            .define('G', Blocks.GLASS)
            .define('O', Blocks.OBSIDIAN)
            .pattern("GGG")
            .pattern("GSG")
            .pattern("OOO")
            .unlockedBy("has_nether_star", this.has(Items.NETHER_STAR))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.BEEHIVE)
            .define('P', ItemTags.PLANKS)
            .define('H', Items.HONEYCOMB)
            .pattern("PPP")
            .pattern("HHH")
            .pattern("PPP")
            .unlockedBy("has_honeycomb", this.has(Items.HONEYCOMB))
            .save(this.output);
        this.shapeless(RecipeCategory.FOOD, Items.BEETROOT_SOUP)
            .requires(Items.BOWL)
            .requires(Items.BEETROOT, 6)
            .unlockedBy("has_beetroot", this.has(Items.BEETROOT))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.DYE.black())
            .requires(Items.INK_SAC)
            .group("black_dye")
            .unlockedBy("has_ink_sac", this.has(Items.INK_SAC))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.black(), Blocks.WITHER_ROSE, "black_dye");
        this.shapeless(RecipeCategory.BREWING, Items.BLAZE_POWDER, 2)
            .requires(Items.BLAZE_ROD)
            .unlockedBy("has_blaze_rod", this.has(Items.BLAZE_ROD))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.DYE.blue())
            .requires(Items.LAPIS_LAZULI)
            .group("blue_dye")
            .unlockedBy("has_lapis_lazuli", this.has(Items.LAPIS_LAZULI))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.blue(), Blocks.CORNFLOWER, "blue_dye");
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.BLUE_ICE, Blocks.PACKED_ICE);
        this.shapeless(RecipeCategory.MISC, Items.BONE_MEAL, 3)
            .requires(Items.BONE)
            .group("bonemeal")
            .unlockedBy("has_bone", this.has(Items.BONE))
            .save(this.output);
        this.nineBlockStorageRecipesRecipesWithCustomUnpacking(
            RecipeCategory.MISC, Items.BONE_MEAL, RecipeCategory.BUILDING_BLOCKS, Items.BONE_BLOCK, "bone_meal_from_bone_block", "bonemeal"
        );
        this.shapeless(RecipeCategory.MISC, Items.BOOK)
            .requires(Items.PAPER, 3)
            .requires(Items.LEATHER)
            .unlockedBy("has_paper", this.has(Items.PAPER))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.BOOKSHELF)
            .define('#', ItemTags.PLANKS)
            .define('X', Items.BOOK)
            .pattern("###")
            .pattern("XXX")
            .pattern("###")
            .unlockedBy("has_book", this.has(Items.BOOK))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.BOW)
            .define('#', Items.STICK)
            .define('X', Items.STRING)
            .pattern(" #X")
            .pattern("# X")
            .pattern(" #X")
            .unlockedBy("has_string", this.has(Items.STRING))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Items.BOWL, 4)
            .define('#', ItemTags.PLANKS)
            .pattern("# #")
            .pattern(" # ")
            .unlockedBy("has_brown_mushroom", this.has(Blocks.BROWN_MUSHROOM))
            .unlockedBy("has_red_mushroom", this.has(Blocks.RED_MUSHROOM))
            .unlockedBy("has_mushroom_stew", this.has(Items.MUSHROOM_STEW))
            .save(this.output);
        this.shaped(RecipeCategory.FOOD, Items.BREAD).define('#', Items.WHEAT).pattern("###").unlockedBy("has_wheat", this.has(Items.WHEAT)).save(this.output);
        this.shaped(RecipeCategory.BREWING, Blocks.BREWING_STAND)
            .define('B', Items.BLAZE_ROD)
            .define('#', ItemTags.STONE_CRAFTING_MATERIALS)
            .pattern(" B ")
            .pattern("###")
            .unlockedBy("has_blaze_rod", this.has(Items.BLAZE_ROD))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.BRICKS)
            .define('#', Items.BRICK)
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_brick", this.has(Items.BRICK))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.DYE.brown())
            .requires(Items.COCOA_BEANS)
            .group("brown_dye")
            .unlockedBy("has_cocoa_beans", this.has(Items.COCOA_BEANS))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Items.BUCKET)
            .define('#', Items.IRON_INGOT)
            .pattern("# #")
            .pattern(" # ")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.FOOD, Blocks.CAKE)
            .define('A', Items.MILK_BUCKET)
            .define('B', Items.SUGAR)
            .define('C', Items.WHEAT)
            .define('E', ItemTags.EGGS)
            .pattern("AAA")
            .pattern("BEB")
            .pattern("CCC")
            .unlockedBy("has_egg", this.has(ItemTags.EGGS))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.CAMPFIRE)
            .define('L', ItemTags.LOGS)
            .define('S', Items.STICK)
            .define('C', ItemTags.COALS)
            .pattern(" S ")
            .pattern("SCS")
            .pattern("LLL")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .unlockedBy("has_coal", this.has(ItemTags.COALS))
            .save(this.output);
        this.shaped(RecipeCategory.TRANSPORTATION, Items.CARROT_ON_A_STICK)
            .define('#', Items.FISHING_ROD)
            .define('X', Items.CARROT)
            .pattern("# ")
            .pattern(" X")
            .unlockedBy("has_carrot", this.has(Items.CARROT))
            .save(this.output);
        this.shaped(RecipeCategory.TRANSPORTATION, Items.WARPED_FUNGUS_ON_A_STICK)
            .define('#', Items.FISHING_ROD)
            .define('X', Items.WARPED_FUNGUS)
            .pattern("# ")
            .pattern(" X")
            .unlockedBy("has_warped_fungus", this.has(Items.WARPED_FUNGUS))
            .save(this.output);
        this.shaped(RecipeCategory.BREWING, Blocks.CAULDRON)
            .define('#', Items.IRON_INGOT)
            .pattern("# #")
            .pattern("# #")
            .pattern("###")
            .unlockedBy("has_water_bucket", this.has(Items.WATER_BUCKET))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.COMPOSTER)
            .define('#', ItemTags.WOODEN_SLABS)
            .pattern("# #")
            .pattern("# #")
            .pattern("###")
            .unlockedBy("has_wood_slab", this.has(ItemTags.WOODEN_SLABS))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.CHEST)
            .define('#', ItemTags.PLANKS)
            .pattern("###")
            .pattern("# #")
            .pattern("###")
            .unlockedBy(
                "has_lots_of_items",
                CriteriaTriggers.INVENTORY_CHANGED
                    .createCriterion(
                        new InventoryChangeTrigger.TriggerInstance(
                            Optional.empty(),
                            new InventoryChangeTrigger.TriggerInstance.Slots(MinMaxBounds.Ints.atLeast(10), MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY),
                            List.of()
                        )
                    )
            )
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.COPPER_CHEST.weathering().unaffected())
            .define('#', Items.COPPER_INGOT)
            .define('X', Items.CHEST)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_copper_chest", this.has(Items.COPPER_CHEST.weathering().unaffected()))
            .save(this.output);
        this.shapeless(RecipeCategory.TRANSPORTATION, Items.CHEST_MINECART)
            .requires(Blocks.CHEST)
            .requires(Items.MINECART)
            .unlockedBy("has_minecart", this.has(Items.MINECART))
            .save(this.output);
        this.chestBoat(Items.ACACIA_CHEST_BOAT, Items.ACACIA_BOAT);
        this.chestBoat(Items.BIRCH_CHEST_BOAT, Items.BIRCH_BOAT);
        this.chestBoat(Items.DARK_OAK_CHEST_BOAT, Items.DARK_OAK_BOAT);
        this.chestBoat(Items.PALE_OAK_CHEST_BOAT, Items.PALE_OAK_BOAT);
        this.chestBoat(Items.JUNGLE_CHEST_BOAT, Items.JUNGLE_BOAT);
        this.chestBoat(Items.OAK_CHEST_BOAT, Items.OAK_BOAT);
        this.chestBoat(Items.SPRUCE_CHEST_BOAT, Items.SPRUCE_BOAT);
        this.chestBoat(Items.MANGROVE_CHEST_BOAT, Items.MANGROVE_BOAT);
        this.chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_QUARTZ_BLOCK, Ingredient.of(Blocks.QUARTZ_SLAB))
            .unlockedBy("has_chiseled_quartz_block", this.has(Blocks.CHISELED_QUARTZ_BLOCK))
            .unlockedBy("has_quartz_block", this.has(Blocks.QUARTZ_BLOCK))
            .unlockedBy("has_quartz_pillar", this.has(Blocks.QUARTZ_PILLAR))
            .save(this.output);
        this.chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_STONE_BRICKS, Ingredient.of(Blocks.STONE_BRICK_SLAB))
            .unlockedBy("has_tag", this.has(ItemTags.STONE_BRICKS))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.CLAY, Items.CLAY_BALL);
        this.shaped(RecipeCategory.TOOLS, Items.CLOCK)
            .define('#', Items.GOLD_INGOT)
            .define('X', Items.REDSTONE)
            .pattern(" # ")
            .pattern("#X#")
            .pattern(" # ")
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.COAL, RecipeCategory.BUILDING_BLOCKS, Items.COAL_BLOCK);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.COARSE_DIRT, 4)
            .define('D', Blocks.DIRT)
            .define('G', Blocks.GRAVEL)
            .pattern("DG")
            .pattern("GD")
            .unlockedBy("has_gravel", this.has(Blocks.GRAVEL))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.COMPARATOR)
            .define('#', Blocks.REDSTONE_TORCH)
            .define('X', Items.QUARTZ)
            .define('I', Blocks.STONE)
            .pattern(" # ")
            .pattern("#X#")
            .pattern("III")
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.COMPASS)
            .define('#', Items.IRON_INGOT)
            .define('X', Items.REDSTONE)
            .pattern(" # ")
            .pattern("#X#")
            .pattern(" # ")
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);
        this.shaped(RecipeCategory.FOOD, Items.COOKIE, 8)
            .define('#', Items.WHEAT)
            .define('X', Items.COCOA_BEANS)
            .pattern("#X#")
            .unlockedBy("has_cocoa", this.has(Items.COCOA_BEANS))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.CRAFTING_TABLE)
            .define('#', ItemTags.PLANKS)
            .pattern("##")
            .pattern("##")
            .unlockedBy("unlock_right_away", PlayerTrigger.TriggerInstance.tick())
            .showNotification(false)
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.CROSSBOW)
            .define('~', Items.STRING)
            .define('#', Items.STICK)
            .define('&', Items.IRON_INGOT)
            .define('$', Blocks.TRIPWIRE_HOOK)
            .pattern("#&#")
            .pattern("~$~")
            .pattern(" # ")
            .unlockedBy("has_string", this.has(Items.STRING))
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .unlockedBy("has_tripwire_hook", this.has(Blocks.TRIPWIRE_HOOK))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.LOOM)
            .define('#', ItemTags.PLANKS)
            .define('@', Items.STRING)
            .pattern("@@")
            .pattern("##")
            .unlockedBy("has_string", this.has(Items.STRING))
            .save(this.output);
        this.chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_RED_SANDSTONE, Ingredient.of(Blocks.RED_SANDSTONE_SLAB))
            .unlockedBy("has_red_sandstone", this.has(Blocks.RED_SANDSTONE))
            .unlockedBy("has_chiseled_red_sandstone", this.has(Blocks.CHISELED_RED_SANDSTONE))
            .unlockedBy("has_cut_red_sandstone", this.has(Blocks.CUT_RED_SANDSTONE))
            .save(this.output);
        this.chiseled(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE_SLAB);
        this.nineBlockStorageRecipesRecipesWithCustomUnpacking(
            RecipeCategory.MISC,
            Items.COPPER_INGOT,
            RecipeCategory.BUILDING_BLOCKS,
            Items.COPPER_BLOCK.weathering().unaffected(),
            getSimpleRecipeName(Items.COPPER_INGOT),
            getItemName(Items.COPPER_INGOT)
        );
        this.shapeless(RecipeCategory.MISC, Items.COPPER_INGOT, 9)
            .requires(Blocks.COPPER_BLOCK.waxed().unaffected())
            .group(getItemName(Items.COPPER_INGOT))
            .unlockedBy(getHasName(Blocks.COPPER_BLOCK.waxed().unaffected()), this.has(Blocks.COPPER_BLOCK.waxed().unaffected()))
            .save(this.output, getConversionRecipeName(Items.COPPER_INGOT, Blocks.COPPER_BLOCK.waxed().unaffected()));
        this.waxRecipes(FeatureFlagSet.of(FeatureFlags.VANILLA));
        this.shapeless(RecipeCategory.MISC, Items.DYE.cyan(), 2)
            .requires(Items.DYE.blue())
            .requires(Items.DYE.green())
            .group("cyan_dye")
            .unlockedBy("has_green_dye", this.has(Items.DYE.green()))
            .unlockedBy("has_blue_dye", this.has(Items.DYE.blue()))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.DARK_PRISMARINE)
            .define('S', Items.PRISMARINE_SHARD)
            .define('I', Items.DYE.black())
            .pattern("SSS")
            .pattern("SIS")
            .pattern("SSS")
            .unlockedBy("has_prismarine_shard", this.has(Items.PRISMARINE_SHARD))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.DAYLIGHT_DETECTOR)
            .define('Q', Items.QUARTZ)
            .define('G', Blocks.GLASS)
            .define('W', ItemTags.WOODEN_SLABS)
            .pattern("GGG")
            .pattern("QQQ")
            .pattern("WWW")
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);
        this.shaped(RecipeCategory.TRANSPORTATION, Blocks.DETECTOR_RAIL, 6)
            .define('R', Items.REDSTONE)
            .define('#', Blocks.STONE_PRESSURE_PLATE)
            .define('X', Items.IRON_INGOT)
            .pattern("X X")
            .pattern("X#X")
            .pattern("XRX")
            .unlockedBy("has_rail", this.has(Blocks.RAIL))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.DIAMOND_AXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.DIAMOND_TOOL_MATERIALS)
            .pattern("XX")
            .pattern("X#")
            .pattern(" #")
            .unlockedBy("has_diamond", this.has(ItemTags.DIAMOND_TOOL_MATERIALS))
            .save(this.output);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.DIAMOND, RecipeCategory.BUILDING_BLOCKS, Items.DIAMOND_BLOCK);
        this.shaped(RecipeCategory.COMBAT, Items.DIAMOND_BOOTS)
            .define('X', Items.DIAMOND)
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_diamond", this.has(Items.DIAMOND))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.DIAMOND_CHESTPLATE)
            .define('X', Items.DIAMOND)
            .pattern("X X")
            .pattern("XXX")
            .pattern("XXX")
            .unlockedBy("has_diamond", this.has(Items.DIAMOND))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.DIAMOND_HELMET)
            .define('X', Items.DIAMOND)
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_diamond", this.has(Items.DIAMOND))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.DIAMOND_HOE)
            .define('#', Items.STICK)
            .define('X', ItemTags.DIAMOND_TOOL_MATERIALS)
            .pattern("XX")
            .pattern(" #")
            .pattern(" #")
            .unlockedBy("has_diamond", this.has(ItemTags.DIAMOND_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.DIAMOND_LEGGINGS)
            .define('X', Items.DIAMOND)
            .pattern("XXX")
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_diamond", this.has(Items.DIAMOND))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.DIAMOND_PICKAXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.DIAMOND_TOOL_MATERIALS)
            .pattern("XXX")
            .pattern(" # ")
            .pattern(" # ")
            .unlockedBy("has_diamond", this.has(ItemTags.DIAMOND_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.DIAMOND_SHOVEL)
            .define('#', Items.STICK)
            .define('X', ItemTags.DIAMOND_TOOL_MATERIALS)
            .pattern("X")
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_diamond", this.has(ItemTags.DIAMOND_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.DIAMOND_SWORD)
            .define('#', Items.STICK)
            .define('X', ItemTags.DIAMOND_TOOL_MATERIALS)
            .pattern("X")
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_diamond", this.has(ItemTags.DIAMOND_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.DIAMOND_SPEAR)
            .define('#', Items.STICK)
            .define('X', ItemTags.DIAMOND_TOOL_MATERIALS)
            .pattern("  X")
            .pattern(" # ")
            .pattern("#  ")
            .unlockedBy("has_diamond", this.has(ItemTags.DIAMOND_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.DIORITE, 2)
            .define('Q', Items.QUARTZ)
            .define('C', Blocks.COBBLESTONE)
            .pattern("CQ")
            .pattern("QC")
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.DISPENSER)
            .define('R', Items.REDSTONE)
            .define('#', Blocks.COBBLESTONE)
            .define('X', Items.BOW)
            .pattern("###")
            .pattern("#X#")
            .pattern("#R#")
            .unlockedBy("has_bow", this.has(Items.BOW))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.SULFUR, 1)
            .define('#', Blocks.SULFUR_SPIKE)
            .pattern("##")
            .pattern("##")
            .unlockedBy(getHasName(Blocks.SULFUR_SPIKE), this.has(Blocks.SULFUR_SPIKE))
            .save(this.output, "sulfur_from_sulfur_spikes");
        this.shaped(RecipeCategory.REDSTONE, Blocks.DROPPER)
            .define('R', Items.REDSTONE)
            .define('#', Blocks.COBBLESTONE)
            .pattern("###")
            .pattern("# #")
            .pattern("#R#")
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.EMERALD, RecipeCategory.BUILDING_BLOCKS, Items.EMERALD_BLOCK);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.ENCHANTING_TABLE)
            .define('B', Items.BOOK)
            .define('#', Blocks.OBSIDIAN)
            .define('D', Items.DIAMOND)
            .pattern(" B ")
            .pattern("D#D")
            .pattern("###")
            .unlockedBy("has_obsidian", this.has(Blocks.OBSIDIAN))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.ENDER_CHEST)
            .define('#', Blocks.OBSIDIAN)
            .define('E', Items.ENDER_EYE)
            .pattern("###")
            .pattern("#E#")
            .pattern("###")
            .unlockedBy("has_ender_eye", this.has(Items.ENDER_EYE))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.ENDER_EYE)
            .requires(Items.ENDER_PEARL)
            .requires(Items.BLAZE_POWDER)
            .unlockedBy("has_blaze_powder", this.has(Items.BLAZE_POWDER))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Items.END_CRYSTAL)
            .define('T', Items.GHAST_TEAR)
            .define('E', Items.ENDER_EYE)
            .define('G', Blocks.GLASS)
            .pattern("GGG")
            .pattern("GEG")
            .pattern("GTG")
            .unlockedBy("has_ender_eye", this.has(Items.ENDER_EYE))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.END_ROD, 4)
            .define('#', Items.POPPED_CHORUS_FRUIT)
            .define('/', Items.BLAZE_ROD)
            .pattern("/")
            .pattern("#")
            .unlockedBy("has_chorus_fruit_popped", this.has(Items.POPPED_CHORUS_FRUIT))
            .save(this.output);
        this.shapeless(RecipeCategory.BREWING, Items.FERMENTED_SPIDER_EYE)
            .requires(Items.SPIDER_EYE)
            .requires(Blocks.BROWN_MUSHROOM)
            .requires(Items.SUGAR)
            .unlockedBy("has_spider_eye", this.has(Items.SPIDER_EYE))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.FIRE_CHARGE, 3)
            .requires(Items.GUNPOWDER)
            .requires(Items.BLAZE_POWDER)
            .requires(Ingredient.of(Items.COAL, Items.CHARCOAL))
            .unlockedBy("has_blaze_powder", this.has(Items.BLAZE_POWDER))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.FIREWORK_ROCKET, 3)
            .requires(Items.GUNPOWDER)
            .requires(Items.PAPER)
            .unlockedBy("has_gunpowder", this.has(Items.GUNPOWDER))
            .save(this.output, "firework_rocket_simple");
        this.shaped(RecipeCategory.TOOLS, Items.FISHING_ROD)
            .define('#', Items.STICK)
            .define('X', Items.STRING)
            .pattern("  #")
            .pattern(" #X")
            .pattern("# X")
            .unlockedBy("has_string", this.has(Items.STRING))
            .save(this.output);
        this.shapeless(RecipeCategory.TOOLS, Items.FLINT_AND_STEEL)
            .requires(Items.IRON_INGOT)
            .requires(Items.FLINT)
            .unlockedBy("has_flint", this.has(Items.FLINT))
            .unlockedBy("has_obsidian", this.has(Blocks.OBSIDIAN))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.FLOWER_POT)
            .define('#', Items.BRICK)
            .pattern("# #")
            .pattern(" # ")
            .unlockedBy("has_brick", this.has(Items.BRICK))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.FURNACE)
            .define('#', ItemTags.STONE_CRAFTING_MATERIALS)
            .pattern("###")
            .pattern("# #")
            .pattern("###")
            .unlockedBy("has_cobblestone", this.has(ItemTags.STONE_CRAFTING_MATERIALS))
            .save(this.output);
        this.shapeless(RecipeCategory.TRANSPORTATION, Items.FURNACE_MINECART)
            .requires(Blocks.FURNACE)
            .requires(Items.MINECART)
            .unlockedBy("has_minecart", this.has(Items.MINECART))
            .save(this.output);
        this.shaped(RecipeCategory.BREWING, Items.GLASS_BOTTLE, 3)
            .define('#', Blocks.GLASS)
            .pattern("# #")
            .pattern(" # ")
            .unlockedBy("has_glass", this.has(Blocks.GLASS))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.GLASS_PANE, 16)
            .define('#', Blocks.GLASS)
            .pattern("###")
            .pattern("###")
            .unlockedBy("has_glass", this.has(Blocks.GLASS))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.GLOWSTONE, Items.GLOWSTONE_DUST);
        this.shapeless(RecipeCategory.DECORATIONS, Items.GLOW_ITEM_FRAME)
            .requires(Items.ITEM_FRAME)
            .requires(Items.GLOW_INK_SAC)
            .unlockedBy("has_item_frame", this.has(Items.ITEM_FRAME))
            .unlockedBy("has_glow_ink_sac", this.has(Items.GLOW_INK_SAC))
            .save(this.output);
        this.shaped(RecipeCategory.FOOD, Items.GOLDEN_APPLE)
            .define('#', Items.GOLD_INGOT)
            .define('X', Items.APPLE)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_gold_ingot", this.has(Items.GOLD_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.GOLDEN_AXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.GOLD_TOOL_MATERIALS)
            .pattern("XX")
            .pattern("X#")
            .pattern(" #")
            .unlockedBy("has_gold_ingot", this.has(ItemTags.GOLD_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.GOLDEN_BOOTS)
            .define('X', Items.GOLD_INGOT)
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_gold_ingot", this.has(Items.GOLD_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.BREWING, Items.GOLDEN_CARROT)
            .define('#', Items.GOLD_NUGGET)
            .define('X', Items.CARROT)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_gold_nugget", this.has(Items.GOLD_NUGGET))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.GOLDEN_CHESTPLATE)
            .define('X', Items.GOLD_INGOT)
            .pattern("X X")
            .pattern("XXX")
            .pattern("XXX")
            .unlockedBy("has_gold_ingot", this.has(Items.GOLD_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.GOLDEN_HELMET)
            .define('X', Items.GOLD_INGOT)
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_gold_ingot", this.has(Items.GOLD_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.GOLDEN_HOE)
            .define('#', Items.STICK)
            .define('X', ItemTags.GOLD_TOOL_MATERIALS)
            .pattern("XX")
            .pattern(" #")
            .pattern(" #")
            .unlockedBy("has_gold_ingot", this.has(ItemTags.GOLD_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.GOLDEN_LEGGINGS)
            .define('X', Items.GOLD_INGOT)
            .pattern("XXX")
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_gold_ingot", this.has(Items.GOLD_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.GOLDEN_PICKAXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.GOLD_TOOL_MATERIALS)
            .pattern("XXX")
            .pattern(" # ")
            .pattern(" # ")
            .unlockedBy("has_gold_ingot", this.has(ItemTags.GOLD_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TRANSPORTATION, Blocks.POWERED_RAIL, 6)
            .define('R', Items.REDSTONE)
            .define('#', Items.STICK)
            .define('X', Items.GOLD_INGOT)
            .pattern("X X")
            .pattern("X#X")
            .pattern("XRX")
            .unlockedBy("has_rail", this.has(Blocks.RAIL))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.GOLDEN_SHOVEL)
            .define('#', Items.STICK)
            .define('X', ItemTags.GOLD_TOOL_MATERIALS)
            .pattern("X")
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_gold_ingot", this.has(ItemTags.GOLD_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.GOLDEN_SWORD)
            .define('#', Items.STICK)
            .define('X', ItemTags.GOLD_TOOL_MATERIALS)
            .pattern("X")
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_gold_ingot", this.has(ItemTags.GOLD_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.GOLDEN_SPEAR)
            .define('#', Items.STICK)
            .define('X', ItemTags.GOLD_TOOL_MATERIALS)
            .pattern("  X")
            .pattern(" # ")
            .pattern("#  ")
            .unlockedBy("has_gold_ingot", this.has(ItemTags.GOLD_TOOL_MATERIALS))
            .save(this.output);
        this.nineBlockStorageRecipesRecipesWithCustomUnpacking(
            RecipeCategory.MISC, Items.GOLD_INGOT, RecipeCategory.BUILDING_BLOCKS, Items.GOLD_BLOCK, "gold_ingot_from_gold_block", "gold_ingot"
        );
        this.nineBlockStorageRecipesWithCustomPacking(
            RecipeCategory.MISC, Items.GOLD_NUGGET, RecipeCategory.MISC, Items.GOLD_INGOT, "gold_ingot_from_nuggets", "gold_ingot"
        );
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.GRANITE)
            .requires(Blocks.DIORITE)
            .requires(Items.QUARTZ)
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.DYE.gray(), 2)
            .requires(Items.DYE.black())
            .requires(Items.DYE.white())
            .group("gray_dye")
            .unlockedBy("has_white_dye", this.has(Items.DYE.white()))
            .unlockedBy("has_black_dye", this.has(Items.DYE.black()))
            .save(this.output);
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.HAY_BLOCK, Items.WHEAT);
        this.pressurePlate(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.IRON_INGOT);
        this.shapeless(RecipeCategory.FOOD, Items.HONEY_BOTTLE, 4)
            .requires(Items.HONEY_BLOCK)
            .requires(Items.GLASS_BOTTLE, 4)
            .unlockedBy("has_honey_block", this.has(Blocks.HONEY_BLOCK))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.REDSTONE, Blocks.HONEY_BLOCK, Items.HONEY_BOTTLE);
        this.twoByTwoPacker(RecipeCategory.DECORATIONS, Blocks.HONEYCOMB_BLOCK, Items.HONEYCOMB);
        this.shaped(RecipeCategory.REDSTONE, Blocks.HOPPER)
            .define('C', Blocks.CHEST)
            .define('I', Items.IRON_INGOT)
            .pattern("I I")
            .pattern("ICI")
            .pattern(" I ")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shapeless(RecipeCategory.TRANSPORTATION, Items.HOPPER_MINECART)
            .requires(Blocks.HOPPER)
            .requires(Items.MINECART)
            .unlockedBy("has_minecart", this.has(Items.MINECART))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.IRON_AXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.IRON_TOOL_MATERIALS)
            .pattern("XX")
            .pattern("X#")
            .pattern(" #")
            .unlockedBy("has_iron_ingot", this.has(ItemTags.IRON_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.IRON_BARS, 16)
            .define('#', Items.IRON_INGOT)
            .pattern("###")
            .pattern("###")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.COPPER_BARS.weathering().unaffected(), 16)
            .define('#', Items.COPPER_INGOT)
            .pattern("###")
            .pattern("###")
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.IRON_BOOTS)
            .define('X', Items.IRON_INGOT)
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.IRON_CHESTPLATE)
            .define('X', Items.IRON_INGOT)
            .pattern("X X")
            .pattern("XXX")
            .pattern("XXX")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.doorBuilder(Blocks.IRON_DOOR, Ingredient.of(Items.IRON_INGOT))
            .unlockedBy(getHasName(Items.IRON_INGOT), this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.IRON_HELMET)
            .define('X', Items.IRON_INGOT)
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.IRON_HOE)
            .define('#', Items.STICK)
            .define('X', ItemTags.IRON_TOOL_MATERIALS)
            .pattern("XX")
            .pattern(" #")
            .pattern(" #")
            .unlockedBy("has_iron_ingot", this.has(ItemTags.IRON_TOOL_MATERIALS))
            .save(this.output);
        this.nineBlockStorageRecipesRecipesWithCustomUnpacking(
            RecipeCategory.MISC, Items.IRON_INGOT, RecipeCategory.BUILDING_BLOCKS, Items.IRON_BLOCK, "iron_ingot_from_iron_block", "iron_ingot"
        );
        this.nineBlockStorageRecipesWithCustomPacking(
            RecipeCategory.MISC, Items.IRON_NUGGET, RecipeCategory.MISC, Items.IRON_INGOT, "iron_ingot_from_nuggets", "iron_ingot"
        );
        this.shaped(RecipeCategory.COMBAT, Items.IRON_LEGGINGS)
            .define('X', Items.IRON_INGOT)
            .pattern("XXX")
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.IRON_PICKAXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.IRON_TOOL_MATERIALS)
            .pattern("XXX")
            .pattern(" # ")
            .pattern(" # ")
            .unlockedBy("has_iron_ingot", this.has(ItemTags.IRON_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.IRON_SHOVEL)
            .define('#', Items.STICK)
            .define('X', ItemTags.IRON_TOOL_MATERIALS)
            .pattern("X")
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_iron_ingot", this.has(ItemTags.IRON_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.IRON_SWORD)
            .define('#', Items.STICK)
            .define('X', ItemTags.IRON_TOOL_MATERIALS)
            .pattern("X")
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_iron_ingot", this.has(ItemTags.IRON_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.IRON_SPEAR)
            .define('#', Items.STICK)
            .define('X', ItemTags.IRON_TOOL_MATERIALS)
            .pattern("  X")
            .pattern(" # ")
            .pattern("#  ")
            .unlockedBy("has_iron_ingot", this.has(ItemTags.IRON_TOOL_MATERIALS))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.REDSTONE, Blocks.IRON_TRAPDOOR, Items.IRON_INGOT);
        this.shaped(RecipeCategory.DECORATIONS, Items.ITEM_FRAME)
            .define('#', Items.STICK)
            .define('X', Items.LEATHER)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_leather", this.has(Items.LEATHER))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.JUKEBOX)
            .define('#', ItemTags.PLANKS)
            .define('X', Items.DIAMOND)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_diamond", this.has(Items.DIAMOND))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.LADDER, 3)
            .define('#', Items.STICK)
            .pattern("# #")
            .pattern("###")
            .pattern("# #")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .save(this.output);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.LAPIS_LAZULI, RecipeCategory.BUILDING_BLOCKS, Items.LAPIS_BLOCK);
        this.shaped(RecipeCategory.TOOLS, Items.LEAD, 2)
            .define('~', Items.STRING)
            .pattern("~~ ")
            .pattern("~~ ")
            .pattern("  ~")
            .unlockedBy("has_string", this.has(Items.STRING))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.NAME_TAG)
            .define('X', ItemTags.METAL_NUGGETS)
            .define('#', Items.PAPER)
            .pattern(" X")
            .pattern("# ")
            .unlockedBy("has_metal_nugget", this.has(ItemTags.METAL_NUGGETS))
            .unlockedBy("has_paper", this.has(Items.PAPER))
            .unlockedBy("has_name_tag", this.has(Items.NAME_TAG))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.MISC, Items.LEATHER, Items.RABBIT_HIDE);
        this.shaped(RecipeCategory.COMBAT, Items.LEATHER_BOOTS)
            .define('X', Items.LEATHER)
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_leather", this.has(Items.LEATHER))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.LEATHER_CHESTPLATE)
            .define('X', Items.LEATHER)
            .pattern("X X")
            .pattern("XXX")
            .pattern("XXX")
            .unlockedBy("has_leather", this.has(Items.LEATHER))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.LEATHER_HELMET)
            .define('X', Items.LEATHER)
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_leather", this.has(Items.LEATHER))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.LEATHER_LEGGINGS)
            .define('X', Items.LEATHER)
            .pattern("XXX")
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_leather", this.has(Items.LEATHER))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Items.LEATHER_HORSE_ARMOR)
            .define('X', Items.LEATHER)
            .pattern("X X")
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_leather", this.has(Items.LEATHER))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.SADDLE)
            .define('X', Items.LEATHER)
            .define('#', Items.IRON_INGOT)
            .pattern(" X ")
            .pattern("X#X")
            .unlockedBy("has_leather", this.has(Items.LEATHER))
            .save(this.output);
        this.nineBlockStorageRecipesWithCustomPacking(
            RecipeCategory.MISC, Items.COPPER_NUGGET, RecipeCategory.MISC, Items.COPPER_INGOT, "copper_ingot_from_nuggets", "copper_ingot"
        );
        this.shaped(RecipeCategory.TOOLS, Items.COPPER_AXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.COPPER_TOOL_MATERIALS)
            .pattern("XX")
            .pattern("X#")
            .pattern(" #")
            .unlockedBy("has_copper_ingot", this.has(ItemTags.COPPER_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.COPPER_HOE)
            .define('#', Items.STICK)
            .define('X', ItemTags.COPPER_TOOL_MATERIALS)
            .pattern("XX")
            .pattern(" #")
            .pattern(" #")
            .unlockedBy("has_copper_ingot", this.has(ItemTags.COPPER_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.COPPER_PICKAXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.COPPER_TOOL_MATERIALS)
            .pattern("XXX")
            .pattern(" # ")
            .pattern(" # ")
            .unlockedBy("has_copper_ingot", this.has(ItemTags.COPPER_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.COPPER_SHOVEL)
            .define('#', Items.STICK)
            .define('X', ItemTags.COPPER_TOOL_MATERIALS)
            .pattern("X")
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_copper_ingot", this.has(ItemTags.COPPER_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.COPPER_SWORD)
            .define('#', Items.STICK)
            .define('X', ItemTags.COPPER_TOOL_MATERIALS)
            .pattern("X")
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_copper_ingot", this.has(ItemTags.COPPER_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.COPPER_SPEAR)
            .define('#', Items.STICK)
            .define('X', ItemTags.COPPER_TOOL_MATERIALS)
            .pattern("  X")
            .pattern(" # ")
            .pattern("#  ")
            .unlockedBy("has_copper_ingot", this.has(ItemTags.COPPER_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.COPPER_BOOTS)
            .define('X', Items.COPPER_INGOT)
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.COPPER_CHESTPLATE)
            .define('X', Items.COPPER_INGOT)
            .pattern("X X")
            .pattern("XXX")
            .pattern("XXX")
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.COPPER_HELMET)
            .define('X', Items.COPPER_INGOT)
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.COPPER_LEGGINGS)
            .define('X', Items.COPPER_INGOT)
            .pattern("XXX")
            .pattern("X X")
            .pattern("X X")
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.LECTERN)
            .define('S', ItemTags.WOODEN_SLABS)
            .define('B', Blocks.BOOKSHELF)
            .pattern("SSS")
            .pattern(" B ")
            .pattern(" S ")
            .unlockedBy("has_book", this.has(Items.BOOK))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.LEVER)
            .define('#', Blocks.COBBLESTONE)
            .define('X', Items.STICK)
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_cobblestone", this.has(Blocks.COBBLESTONE))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.lightBlue(), Blocks.BLUE_ORCHID, "light_blue_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.lightBlue(), 2)
            .requires(Items.DYE.blue())
            .requires(Items.DYE.white())
            .group("light_blue_dye")
            .unlockedBy("has_blue_dye", this.has(Items.DYE.blue()))
            .unlockedBy("has_white_dye", this.has(Items.DYE.white()))
            .save(this.output, "light_blue_dye_from_blue_white_dye");
        this.oneToOneConversionRecipe(Items.DYE.lightGray(), Blocks.AZURE_BLUET, "light_gray_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.lightGray(), 2)
            .requires(Items.DYE.gray())
            .requires(Items.DYE.white())
            .group("light_gray_dye")
            .unlockedBy("has_gray_dye", this.has(Items.DYE.gray()))
            .unlockedBy("has_white_dye", this.has(Items.DYE.white()))
            .save(this.output, "light_gray_dye_from_gray_white_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.lightGray(), 3)
            .requires(Items.DYE.black())
            .requires(Items.DYE.white(), 2)
            .group("light_gray_dye")
            .unlockedBy("has_white_dye", this.has(Items.DYE.white()))
            .unlockedBy("has_black_dye", this.has(Items.DYE.black()))
            .save(this.output, "light_gray_dye_from_black_white_dye");
        this.oneToOneConversionRecipe(Items.DYE.lightGray(), Blocks.OXEYE_DAISY, "light_gray_dye");
        this.oneToOneConversionRecipe(Items.DYE.lightGray(), Blocks.WHITE_TULIP, "light_gray_dye");
        this.pressurePlate(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.GOLD_INGOT);
        this.shaped(RecipeCategory.REDSTONE, Blocks.LIGHTNING_ROD.weathering().unaffected())
            .define('#', Items.COPPER_INGOT)
            .pattern("#")
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.DYE.lime(), 2)
            .requires(Items.DYE.green())
            .requires(Items.DYE.white())
            .unlockedBy("has_green_dye", this.has(Items.DYE.green()))
            .unlockedBy("has_white_dye", this.has(Items.DYE.white()))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.JACK_O_LANTERN)
            .define('A', Blocks.CARVED_PUMPKIN)
            .define('B', Blocks.TORCH)
            .pattern("A")
            .pattern("B")
            .unlockedBy("has_carved_pumpkin", this.has(Blocks.CARVED_PUMPKIN))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.magenta(), Blocks.ALLIUM, "magenta_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.magenta(), 4)
            .requires(Items.DYE.blue())
            .requires(Items.DYE.red(), 2)
            .requires(Items.DYE.white())
            .group("magenta_dye")
            .unlockedBy("has_blue_dye", this.has(Items.DYE.blue()))
            .unlockedBy("has_rose_red", this.has(Items.DYE.red()))
            .unlockedBy("has_white_dye", this.has(Items.DYE.white()))
            .save(this.output, "magenta_dye_from_blue_red_white_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.magenta(), 3)
            .requires(Items.DYE.blue())
            .requires(Items.DYE.red())
            .requires(Items.DYE.pink())
            .group("magenta_dye")
            .unlockedBy("has_pink_dye", this.has(Items.DYE.pink()))
            .unlockedBy("has_blue_dye", this.has(Items.DYE.blue()))
            .unlockedBy("has_red_dye", this.has(Items.DYE.red()))
            .save(this.output, "magenta_dye_from_blue_red_pink");
        this.oneToOneConversionRecipe(Items.DYE.magenta(), Blocks.LILAC, "magenta_dye", 2);
        this.shapeless(RecipeCategory.MISC, Items.DYE.magenta(), 2)
            .requires(Items.DYE.purple())
            .requires(Items.DYE.pink())
            .group("magenta_dye")
            .unlockedBy("has_pink_dye", this.has(Items.DYE.pink()))
            .unlockedBy("has_purple_dye", this.has(Items.DYE.purple()))
            .save(this.output, "magenta_dye_from_purple_and_pink");
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.MAGMA_BLOCK, Items.MAGMA_CREAM);
        this.shapeless(RecipeCategory.BREWING, Items.MAGMA_CREAM)
            .requires(Items.BLAZE_POWDER)
            .requires(Items.SLIME_BALL)
            .unlockedBy("has_blaze_powder", this.has(Items.BLAZE_POWDER))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Items.MAP)
            .define('#', Items.PAPER)
            .define('X', Items.COMPASS)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_compass", this.has(Items.COMPASS))
            .save(this.output);
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.MELON, Items.MELON_SLICE, "has_melon");
        this.shapeless(RecipeCategory.MISC, Items.MELON_SEEDS)
            .requires(Items.MELON_SLICE)
            .unlockedBy("has_melon", this.has(Items.MELON_SLICE))
            .save(this.output);
        this.shaped(RecipeCategory.TRANSPORTATION, Items.MINECART)
            .define('#', Items.IRON_INGOT)
            .pattern("# #")
            .pattern("###")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_COBBLESTONE)
            .requires(Blocks.COBBLESTONE)
            .requires(Blocks.VINE)
            .group("mossy_cobblestone")
            .unlockedBy("has_vine", this.has(Blocks.VINE))
            .save(this.output, getConversionRecipeName(Blocks.MOSSY_COBBLESTONE, Blocks.VINE));
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_STONE_BRICKS)
            .requires(Blocks.STONE_BRICKS)
            .requires(Blocks.VINE)
            .group("mossy_stone_bricks")
            .unlockedBy("has_vine", this.has(Blocks.VINE))
            .save(this.output, getConversionRecipeName(Blocks.MOSSY_STONE_BRICKS, Blocks.VINE));
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_COBBLESTONE)
            .requires(Blocks.COBBLESTONE)
            .requires(Blocks.MOSS_BLOCK)
            .group("mossy_cobblestone")
            .unlockedBy("has_moss_block", this.has(Blocks.MOSS_BLOCK))
            .save(this.output, getConversionRecipeName(Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK));
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, Blocks.MOSSY_STONE_BRICKS)
            .requires(Blocks.STONE_BRICKS)
            .requires(Blocks.MOSS_BLOCK)
            .group("mossy_stone_bricks")
            .unlockedBy("has_moss_block", this.has(Blocks.MOSS_BLOCK))
            .save(this.output, getConversionRecipeName(Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_BLOCK));
        this.shapeless(RecipeCategory.FOOD, Items.MUSHROOM_STEW)
            .requires(Blocks.BROWN_MUSHROOM)
            .requires(Blocks.RED_MUSHROOM)
            .requires(Items.BOWL)
            .unlockedBy("has_mushroom_stew", this.has(Items.MUSHROOM_STEW))
            .unlockedBy("has_bowl", this.has(Items.BOWL))
            .unlockedBy("has_brown_mushroom", this.has(Blocks.BROWN_MUSHROOM))
            .unlockedBy("has_red_mushroom", this.has(Blocks.RED_MUSHROOM))
            .save(this.output);
        BuiltInRegistries.ITEM.stream().forEach(item -> {
            SuspiciousEffectHolder effectHolder = SuspiciousEffectHolder.tryGet(item);
            if (effectHolder != null) {
                this.suspiciousStew(item, effectHolder);
            }
        });
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.NETHER_BRICKS, Items.NETHER_BRICK);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.RESIN_BRICKS, Items.RESIN_BRICK);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.RESIN_CLUMP, RecipeCategory.BUILDING_BLOCKS, Items.RESIN_BLOCK);
        this.shaped(RecipeCategory.MISC, Blocks.CREAKING_HEART)
            .define('R', Items.RESIN_BLOCK)
            .define('L', Blocks.PALE_OAK_LOG)
            .pattern(" L ")
            .pattern(" R ")
            .pattern(" L ")
            .unlockedBy("has_resin_block", this.has(Items.RESIN_BLOCK))
            .save(this.output);
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.NETHER_WART_BLOCK, Items.NETHER_WART);
        this.shaped(RecipeCategory.REDSTONE, Blocks.NOTE_BLOCK)
            .define('#', ItemTags.PLANKS)
            .define('X', Items.REDSTONE)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.OBSERVER)
            .define('Q', Items.QUARTZ)
            .define('R', Items.REDSTONE)
            .define('#', Blocks.COBBLESTONE)
            .pattern("###")
            .pattern("RRQ")
            .pattern("###")
            .unlockedBy("has_quartz", this.has(Items.QUARTZ))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.orange(), Blocks.ORANGE_TULIP, "orange_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.orange(), 2)
            .requires(Items.DYE.red())
            .requires(Items.DYE.yellow())
            .group("orange_dye")
            .unlockedBy("has_red_dye", this.has(Items.DYE.red()))
            .unlockedBy("has_yellow_dye", this.has(Items.DYE.yellow()))
            .save(this.output, "orange_dye_from_red_yellow");
        this.shaped(RecipeCategory.DECORATIONS, Items.PAINTING)
            .define('#', Items.STICK)
            .define('X', ItemTags.WOOL)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_wool", this.has(ItemTags.WOOL))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Items.PAPER, 3)
            .define('#', Blocks.SUGAR_CANE)
            .pattern("###")
            .unlockedBy("has_reeds", this.has(Blocks.SUGAR_CANE))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_PILLAR, 2)
            .define('#', Blocks.QUARTZ_BLOCK)
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_chiseled_quartz_block", this.has(Blocks.CHISELED_QUARTZ_BLOCK))
            .unlockedBy("has_quartz_block", this.has(Blocks.QUARTZ_BLOCK))
            .unlockedBy("has_quartz_pillar", this.has(Blocks.QUARTZ_PILLAR))
            .save(this.output);
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.PACKED_ICE, Blocks.ICE);
        this.oneToOneConversionRecipe(Items.DYE.pink(), Blocks.PEONY, "pink_dye", 2);
        this.oneToOneConversionRecipe(Items.DYE.pink(), Blocks.PINK_TULIP, "pink_dye");
        this.oneToOneConversionRecipe(Items.DYE.pink(), Blocks.CACTUS_FLOWER, "pink_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.pink(), 2)
            .requires(Items.DYE.red())
            .requires(Items.DYE.white())
            .group("pink_dye")
            .unlockedBy("has_white_dye", this.has(Items.DYE.white()))
            .unlockedBy("has_red_dye", this.has(Items.DYE.red()))
            .save(this.output, "pink_dye_from_red_white_dye");
        this.shaped(RecipeCategory.REDSTONE, Blocks.PISTON)
            .define('R', Items.REDSTONE)
            .define('#', Blocks.COBBLESTONE)
            .define('T', ItemTags.PLANKS)
            .define('X', Items.IRON_INGOT)
            .pattern("TTT")
            .pattern("#X#")
            .pattern("#R#")
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);
        this.polished(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_BASALT, Blocks.BASALT);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.PRISMARINE, Items.PRISMARINE_SHARD);
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.PRISMARINE_BRICKS, Items.PRISMARINE_SHARD);
        this.shapeless(RecipeCategory.FOOD, Items.PUMPKIN_PIE)
            .requires(Blocks.PUMPKIN)
            .requires(Items.SUGAR)
            .requires(ItemTags.EGGS)
            .unlockedBy("has_carved_pumpkin", this.has(Blocks.CARVED_PUMPKIN))
            .unlockedBy("has_pumpkin", this.has(Blocks.PUMPKIN))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.PUMPKIN_SEEDS, 4)
            .requires(Blocks.PUMPKIN)
            .unlockedBy("has_pumpkin", this.has(Blocks.PUMPKIN))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.DYE.purple(), 2)
            .requires(Items.DYE.blue())
            .requires(Items.DYE.red())
            .unlockedBy("has_blue_dye", this.has(Items.DYE.blue()))
            .unlockedBy("has_red_dye", this.has(Items.DYE.red()))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SHULKER_BOX)
            .define('#', Blocks.CHEST)
            .define('-', Items.SHULKER_SHELL)
            .pattern("-")
            .pattern("#")
            .pattern("-")
            .unlockedBy("has_shulker_shell", this.has(Items.SHULKER_SHELL))
            .save(this.output);
        ColorCollection.zipApply(Items.DYE, Items.DYED_SHULKER_BOX, (x$0, x$1) -> this.dyedShulkerBoxRecipe(x$0, x$1));
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_BLOCK, 4)
            .define('F', Items.POPPED_CHORUS_FRUIT)
            .pattern("FF")
            .pattern("FF")
            .unlockedBy("has_chorus_fruit_popped", this.has(Items.POPPED_CHORUS_FRUIT))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_PILLAR)
            .define('#', Blocks.PURPUR_SLAB)
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_purpur_block", this.has(Blocks.PURPUR_BLOCK))
            .save(this.output);
        this.slabBuilder(RecipeCategory.BUILDING_BLOCKS, Blocks.PURPUR_SLAB, Ingredient.of(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR))
            .unlockedBy("has_purpur_block", this.has(Blocks.PURPUR_BLOCK))
            .save(this.output);
        this.stairBuilder(Blocks.PURPUR_STAIRS, Ingredient.of(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR))
            .unlockedBy("has_purpur_block", this.has(Blocks.PURPUR_BLOCK))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_BLOCK, Items.QUARTZ);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_BRICKS, 4)
            .define('#', Blocks.QUARTZ_BLOCK)
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_quartz_block", this.has(Blocks.QUARTZ_BLOCK))
            .save(this.output);
        this.slabBuilder(
                RecipeCategory.BUILDING_BLOCKS, Blocks.QUARTZ_SLAB, Ingredient.of(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR)
            )
            .unlockedBy("has_chiseled_quartz_block", this.has(Blocks.CHISELED_QUARTZ_BLOCK))
            .unlockedBy("has_quartz_block", this.has(Blocks.QUARTZ_BLOCK))
            .unlockedBy("has_quartz_pillar", this.has(Blocks.QUARTZ_PILLAR))
            .save(this.output);
        this.stairBuilder(Blocks.QUARTZ_STAIRS, Ingredient.of(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR))
            .unlockedBy("has_chiseled_quartz_block", this.has(Blocks.CHISELED_QUARTZ_BLOCK))
            .unlockedBy("has_quartz_block", this.has(Blocks.QUARTZ_BLOCK))
            .unlockedBy("has_quartz_pillar", this.has(Blocks.QUARTZ_PILLAR))
            .save(this.output);
        this.shapeless(RecipeCategory.FOOD, Items.RABBIT_STEW)
            .requires(Items.BAKED_POTATO)
            .requires(Items.COOKED_RABBIT)
            .requires(Items.BOWL)
            .requires(Items.CARROT)
            .requires(Blocks.BROWN_MUSHROOM)
            .group("rabbit_stew")
            .unlockedBy("has_cooked_rabbit", this.has(Items.COOKED_RABBIT))
            .save(this.output, getConversionRecipeName(Items.RABBIT_STEW, Items.BROWN_MUSHROOM));
        this.shapeless(RecipeCategory.FOOD, Items.RABBIT_STEW)
            .requires(Items.BAKED_POTATO)
            .requires(Items.COOKED_RABBIT)
            .requires(Items.BOWL)
            .requires(Items.CARROT)
            .requires(Blocks.RED_MUSHROOM)
            .group("rabbit_stew")
            .unlockedBy("has_cooked_rabbit", this.has(Items.COOKED_RABBIT))
            .save(this.output, getConversionRecipeName(Items.RABBIT_STEW, Items.RED_MUSHROOM));
        this.shaped(RecipeCategory.TRANSPORTATION, Blocks.RAIL, 16)
            .define('#', Items.STICK)
            .define('X', Items.IRON_INGOT)
            .pattern("X X")
            .pattern("X#X")
            .pattern("X X")
            .unlockedBy("has_minecart", this.has(Items.MINECART))
            .save(this.output);
        this.nineBlockStorageRecipes(RecipeCategory.REDSTONE, Items.REDSTONE, RecipeCategory.REDSTONE, Items.REDSTONE_BLOCK);
        this.shaped(RecipeCategory.REDSTONE, Blocks.REDSTONE_LAMP)
            .define('R', Items.REDSTONE)
            .define('G', Blocks.GLOWSTONE)
            .pattern(" R ")
            .pattern("RGR")
            .pattern(" R ")
            .unlockedBy("has_glowstone", this.has(Blocks.GLOWSTONE))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.REDSTONE_TORCH)
            .define('#', Items.STICK)
            .define('X', Items.REDSTONE)
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.red(), Items.BEETROOT, "red_dye");
        this.oneToOneConversionRecipe(Items.DYE.red(), Blocks.POPPY, "red_dye");
        this.oneToOneConversionRecipe(Items.DYE.red(), Blocks.ROSE_BUSH, "red_dye", 2);
        this.oneToOneConversionRecipe(Items.DYE.orange(), Blocks.OPEN_EYEBLOSSOM, "orange_dye");
        this.oneToOneConversionRecipe(Items.DYE.gray(), Blocks.CLOSED_EYEBLOSSOM, "gray_dye");
        this.shapeless(RecipeCategory.MISC, Items.DYE.red())
            .requires(Blocks.RED_TULIP)
            .group("red_dye")
            .unlockedBy("has_red_flower", this.has(Blocks.RED_TULIP))
            .save(this.output, "red_dye_from_tulip");
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.RED_NETHER_BRICKS)
            .define('W', Items.NETHER_WART)
            .define('N', Items.NETHER_BRICK)
            .pattern("NW")
            .pattern("WN")
            .unlockedBy("has_nether_wart", this.has(Items.NETHER_WART))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.RED_SANDSTONE)
            .define('#', Blocks.RED_SAND)
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_sand", this.has(Blocks.RED_SAND))
            .save(this.output);
        this.slabBuilder(RecipeCategory.BUILDING_BLOCKS, Blocks.RED_SANDSTONE_SLAB, Ingredient.of(Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE))
            .unlockedBy("has_red_sandstone", this.has(Blocks.RED_SANDSTONE))
            .unlockedBy("has_chiseled_red_sandstone", this.has(Blocks.CHISELED_RED_SANDSTONE))
            .save(this.output);
        this.stairBuilder(Blocks.RED_SANDSTONE_STAIRS, Ingredient.of(Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE))
            .unlockedBy("has_red_sandstone", this.has(Blocks.RED_SANDSTONE))
            .unlockedBy("has_chiseled_red_sandstone", this.has(Blocks.CHISELED_RED_SANDSTONE))
            .unlockedBy("has_cut_red_sandstone", this.has(Blocks.CUT_RED_SANDSTONE))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.REPEATER)
            .define('#', Blocks.REDSTONE_TORCH)
            .define('X', Items.REDSTONE)
            .define('I', Blocks.STONE)
            .pattern("#X#")
            .pattern("III")
            .unlockedBy("has_redstone_torch", this.has(Blocks.REDSTONE_TORCH))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.SANDSTONE, Blocks.SAND);
        this.slabBuilder(RecipeCategory.BUILDING_BLOCKS, Blocks.SANDSTONE_SLAB, Ingredient.of(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE))
            .unlockedBy("has_sandstone", this.has(Blocks.SANDSTONE))
            .unlockedBy("has_chiseled_sandstone", this.has(Blocks.CHISELED_SANDSTONE))
            .save(this.output);
        this.stairBuilder(Blocks.SANDSTONE_STAIRS, Ingredient.of(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE))
            .unlockedBy("has_sandstone", this.has(Blocks.SANDSTONE))
            .unlockedBy("has_chiseled_sandstone", this.has(Blocks.CHISELED_SANDSTONE))
            .unlockedBy("has_cut_sandstone", this.has(Blocks.CUT_SANDSTONE))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.SEA_LANTERN)
            .define('S', Items.PRISMARINE_SHARD)
            .define('C', Items.PRISMARINE_CRYSTALS)
            .pattern("SCS")
            .pattern("CCC")
            .pattern("SCS")
            .unlockedBy("has_prismarine_crystals", this.has(Items.PRISMARINE_CRYSTALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.SHEARS)
            .define('#', Items.IRON_INGOT)
            .pattern(" #")
            .pattern("# ")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.SHIELD)
            .define('W', ItemTags.WOODEN_TOOL_MATERIALS)
            .define('o', Items.IRON_INGOT)
            .pattern("WoW")
            .pattern("WWW")
            .pattern(" W ")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.SLIME_BALL, RecipeCategory.REDSTONE, Items.SLIME_BLOCK);
        this.cut(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE);
        this.cut(RecipeCategory.BUILDING_BLOCKS, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.SNOW_BLOCK, Items.SNOWBALL);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SNOW, 6)
            .define('#', Blocks.SNOW_BLOCK)
            .pattern("###")
            .unlockedBy("has_snowball", this.has(Items.SNOWBALL))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SOUL_CAMPFIRE)
            .define('L', ItemTags.LOGS)
            .define('S', Items.STICK)
            .define('#', ItemTags.SOUL_FIRE_BASE_BLOCKS)
            .pattern(" S ")
            .pattern("S#S")
            .pattern("LLL")
            .unlockedBy("has_soul_sand", this.has(ItemTags.SOUL_FIRE_BASE_BLOCKS))
            .save(this.output);
        this.shaped(RecipeCategory.BREWING, Items.GLISTERING_MELON_SLICE)
            .define('#', Items.GOLD_NUGGET)
            .define('X', Items.MELON_SLICE)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_melon", this.has(Items.MELON_SLICE))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.SPECTRAL_ARROW, 2)
            .define('#', Items.GLOWSTONE_DUST)
            .define('X', Items.ARROW)
            .pattern(" # ")
            .pattern("#X#")
            .pattern(" # ")
            .unlockedBy("has_glowstone_dust", this.has(Items.GLOWSTONE_DUST))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.SPYGLASS)
            .define('#', Items.AMETHYST_SHARD)
            .define('X', Items.COPPER_INGOT)
            .pattern(" # ")
            .pattern(" X ")
            .pattern(" X ")
            .unlockedBy("has_amethyst_shard", this.has(Items.AMETHYST_SHARD))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Items.STICK, 4)
            .define('#', ItemTags.PLANKS)
            .pattern("#")
            .pattern("#")
            .group("sticks")
            .unlockedBy("has_planks", this.has(ItemTags.PLANKS))
            .save(this.output);
        this.shaped(RecipeCategory.MISC, Items.STICK, 1)
            .define('#', Blocks.BAMBOO)
            .pattern("#")
            .pattern("#")
            .group("sticks")
            .unlockedBy("has_bamboo", this.has(Blocks.BAMBOO))
            .save(this.output, "stick_from_bamboo_item");
        this.shaped(RecipeCategory.REDSTONE, Blocks.STICKY_PISTON)
            .define('P', Blocks.PISTON)
            .define('S', Items.SLIME_BALL)
            .pattern("S")
            .pattern("P")
            .unlockedBy("has_slime_ball", this.has(Items.SLIME_BALL))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.STONE_AXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.STONE_TOOL_MATERIALS)
            .pattern("XX")
            .pattern("X#")
            .pattern(" #")
            .unlockedBy("has_cobblestone", this.has(ItemTags.STONE_TOOL_MATERIALS))
            .save(this.output);
        this.slabBuilder(RecipeCategory.BUILDING_BLOCKS, Blocks.STONE_BRICK_SLAB, Ingredient.of(Blocks.STONE_BRICKS))
            .unlockedBy("has_stone_bricks", this.has(ItemTags.STONE_BRICKS))
            .save(this.output);
        this.stairBuilder(Blocks.STONE_BRICK_STAIRS, Ingredient.of(Blocks.STONE_BRICKS))
            .unlockedBy("has_stone_bricks", this.has(ItemTags.STONE_BRICKS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.STONE_HOE)
            .define('#', Items.STICK)
            .define('X', ItemTags.STONE_TOOL_MATERIALS)
            .pattern("XX")
            .pattern(" #")
            .pattern(" #")
            .unlockedBy("has_cobblestone", this.has(ItemTags.STONE_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.STONE_PICKAXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.STONE_TOOL_MATERIALS)
            .pattern("XXX")
            .pattern(" # ")
            .pattern(" # ")
            .unlockedBy("has_cobblestone", this.has(ItemTags.STONE_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.STONE_SHOVEL)
            .define('#', Items.STICK)
            .define('X', ItemTags.STONE_TOOL_MATERIALS)
            .pattern("X")
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_cobblestone", this.has(ItemTags.STONE_TOOL_MATERIALS))
            .save(this.output);
        this.slab(RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE);
        this.shaped(RecipeCategory.COMBAT, Items.STONE_SWORD)
            .define('#', Items.STICK)
            .define('X', ItemTags.STONE_TOOL_MATERIALS)
            .pattern("X")
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_cobblestone", this.has(ItemTags.STONE_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.STONE_SPEAR)
            .define('#', Items.STICK)
            .define('X', ItemTags.STONE_TOOL_MATERIALS)
            .pattern("  X")
            .pattern(" # ")
            .pattern("#  ")
            .unlockedBy("has_cobblestone", this.has(ItemTags.STONE_TOOL_MATERIALS))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.WOOL.white())
            .define('#', Items.STRING)
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_string", this.has(Items.STRING))
            .save(this.output, getConversionRecipeName(Blocks.WOOL.white(), Items.STRING));
        this.oneToOneConversionRecipe(Items.SUGAR, Blocks.SUGAR_CANE, "sugar");
        this.shapeless(RecipeCategory.MISC, Items.SUGAR, 3)
            .requires(Items.HONEY_BOTTLE)
            .group("sugar")
            .unlockedBy("has_honey_bottle", this.has(Items.HONEY_BOTTLE))
            .save(this.output, getConversionRecipeName(Items.SUGAR, Items.HONEY_BOTTLE));
        this.shaped(RecipeCategory.REDSTONE, Blocks.TARGET)
            .define('H', Items.HAY_BLOCK)
            .define('R', Items.REDSTONE)
            .pattern(" R ")
            .pattern("RHR")
            .pattern(" R ")
            .unlockedBy("has_redstone", this.has(Items.REDSTONE))
            .unlockedBy("has_hay_block", this.has(Blocks.HAY_BLOCK))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.TNT)
            .define('#', Ingredient.of(Blocks.SAND, Blocks.RED_SAND))
            .define('X', Items.GUNPOWDER)
            .pattern("X#X")
            .pattern("#X#")
            .pattern("X#X")
            .unlockedBy("has_gunpowder", this.has(Items.GUNPOWDER))
            .save(this.output);
        this.shapeless(RecipeCategory.TRANSPORTATION, Items.TNT_MINECART)
            .requires(Blocks.TNT)
            .requires(Items.MINECART)
            .unlockedBy("has_minecart", this.has(Items.MINECART))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.TORCH, 4)
            .define('#', Items.STICK)
            .define('X', Ingredient.of(Items.COAL, Items.CHARCOAL))
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_stone_pickaxe", this.has(Items.STONE_PICKAXE))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SOUL_TORCH, 4)
            .define('X', Ingredient.of(Items.COAL, Items.CHARCOAL))
            .define('#', Items.STICK)
            .define('S', ItemTags.SOUL_FIRE_BASE_BLOCKS)
            .pattern("X")
            .pattern("#")
            .pattern("S")
            .unlockedBy("has_soul_sand", this.has(ItemTags.SOUL_FIRE_BASE_BLOCKS))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.COPPER_TORCH, 4)
            .define('X', Ingredient.of(Items.COAL, Items.CHARCOAL))
            .define('#', Items.STICK)
            .define('C', Items.COPPER_NUGGET)
            .pattern("C")
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_copper_nugget", this.has(Items.COPPER_NUGGET))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.LANTERN)
            .define('#', Items.TORCH)
            .define('X', Items.IRON_NUGGET)
            .pattern("XXX")
            .pattern("X#X")
            .pattern("XXX")
            .unlockedBy("has_iron_nugget", this.has(Items.IRON_NUGGET))
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SOUL_LANTERN)
            .define('#', Items.SOUL_TORCH)
            .define('X', Items.IRON_NUGGET)
            .pattern("XXX")
            .pattern("X#X")
            .pattern("XXX")
            .unlockedBy("has_soul_torch", this.has(Items.SOUL_TORCH))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.COPPER_LANTERN.weathering().unaffected())
            .define('#', Items.COPPER_TORCH)
            .define('X', Items.COPPER_NUGGET)
            .pattern("XXX")
            .pattern("X#X")
            .pattern("XXX")
            .unlockedBy("has_copper_torch", this.has(Items.COPPER_TORCH))
            .save(this.output);
        this.shapeless(RecipeCategory.REDSTONE, Blocks.TRAPPED_CHEST)
            .requires(Blocks.CHEST)
            .requires(Blocks.TRIPWIRE_HOOK)
            .unlockedBy("has_tripwire_hook", this.has(Blocks.TRIPWIRE_HOOK))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Blocks.TRIPWIRE_HOOK, 2)
            .define('#', ItemTags.PLANKS)
            .define('S', Items.STICK)
            .define('I', Items.IRON_INGOT)
            .pattern("I")
            .pattern("S")
            .pattern("#")
            .unlockedBy("has_string", this.has(Items.STRING))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.TURTLE_HELMET)
            .define('X', Items.TURTLE_SCUTE)
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_turtle_scute", this.has(Items.TURTLE_SCUTE))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.WOLF_ARMOR)
            .define('X', Items.ARMADILLO_SCUTE)
            .pattern("X  ")
            .pattern("XXX")
            .pattern("X X")
            .unlockedBy("has_armadillo_scute", this.has(Items.ARMADILLO_SCUTE))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.WHEAT, 9)
            .requires(Blocks.HAY_BLOCK)
            .unlockedBy("has_hay_block", this.has(Blocks.HAY_BLOCK))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.DYE.white())
            .requires(Items.BONE_MEAL)
            .group("white_dye")
            .unlockedBy("has_bone_meal", this.has(Items.BONE_MEAL))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.white(), Blocks.LILY_OF_THE_VALLEY, "white_dye");
        this.shaped(RecipeCategory.TOOLS, Items.WOODEN_AXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.WOODEN_TOOL_MATERIALS)
            .pattern("XX")
            .pattern("X#")
            .pattern(" #")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.WOODEN_HOE)
            .define('#', Items.STICK)
            .define('X', ItemTags.WOODEN_TOOL_MATERIALS)
            .pattern("XX")
            .pattern(" #")
            .pattern(" #")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.WOODEN_PICKAXE)
            .define('#', Items.STICK)
            .define('X', ItemTags.WOODEN_TOOL_MATERIALS)
            .pattern("XXX")
            .pattern(" # ")
            .pattern(" # ")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .save(this.output);
        this.shaped(RecipeCategory.TOOLS, Items.WOODEN_SHOVEL)
            .define('#', Items.STICK)
            .define('X', ItemTags.WOODEN_TOOL_MATERIALS)
            .pattern("X")
            .pattern("#")
            .pattern("#")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.WOODEN_SWORD)
            .define('#', Items.STICK)
            .define('X', ItemTags.WOODEN_TOOL_MATERIALS)
            .pattern("X")
            .pattern("X")
            .pattern("#")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.WOODEN_SPEAR)
            .define('#', Items.STICK)
            .define('X', ItemTags.WOODEN_TOOL_MATERIALS)
            .pattern("  X")
            .pattern(" # ")
            .pattern("#  ")
            .unlockedBy("has_stick", this.has(Items.STICK))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.WRITABLE_BOOK)
            .requires(Items.BOOK)
            .requires(Items.INK_SAC)
            .requires(Items.FEATHER)
            .unlockedBy("has_book", this.has(Items.BOOK))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.yellow(), Blocks.DANDELION, "yellow_dye");
        this.oneToOneConversionRecipe(Items.DYE.yellow(), Blocks.GOLDEN_DANDELION, "yellow_dye");
        this.oneToOneConversionRecipe(Items.DYE.yellow(), Blocks.SUNFLOWER, "yellow_dye", 2);
        this.oneToOneConversionRecipe(Items.DYE.yellow(), Blocks.WILDFLOWERS, "yellow_dye");
        this.nineBlockStorageRecipes(RecipeCategory.FOOD, Items.DRIED_KELP, RecipeCategory.BUILDING_BLOCKS, Items.DRIED_KELP_BLOCK);
        this.shaped(RecipeCategory.MISC, Blocks.CONDUIT)
            .define('#', Items.NAUTILUS_SHELL)
            .define('X', Items.HEART_OF_THE_SEA)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .unlockedBy("has_nautilus_core", this.has(Items.HEART_OF_THE_SEA))
            .unlockedBy("has_nautilus_shell", this.has(Items.NAUTILUS_SHELL))
            .save(this.output);
        this.wall(RecipeCategory.DECORATIONS, Blocks.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE);
        this.wall(RecipeCategory.DECORATIONS, Blocks.STONE_BRICK_WALL, Blocks.STONE_BRICKS);
        this.wall(RecipeCategory.DECORATIONS, Blocks.SANDSTONE_WALL, Blocks.SANDSTONE);
        this.shapeless(RecipeCategory.MISC, Items.FIELD_MASONED_BANNER_PATTERN)
            .requires(Items.PAPER)
            .requires(Blocks.BRICKS)
            .unlockedBy("has_bricks", this.has(Blocks.BRICKS))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.BORDURE_INDENTED_BANNER_PATTERN)
            .requires(Items.PAPER)
            .requires(Blocks.VINE)
            .unlockedBy("has_vines", this.has(Blocks.VINE))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.CREEPER_BANNER_PATTERN)
            .requires(Items.PAPER)
            .requires(Items.CREEPER_HEAD)
            .unlockedBy("has_creeper_head", this.has(Items.CREEPER_HEAD))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.SKULL_BANNER_PATTERN)
            .requires(Items.PAPER)
            .requires(Items.WITHER_SKELETON_SKULL)
            .unlockedBy("has_wither_skeleton_skull", this.has(Items.WITHER_SKELETON_SKULL))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.FLOWER_BANNER_PATTERN)
            .requires(Items.PAPER)
            .requires(Blocks.OXEYE_DAISY)
            .unlockedBy("has_oxeye_daisy", this.has(Blocks.OXEYE_DAISY))
            .save(this.output);
        this.shapeless(RecipeCategory.MISC, Items.MOJANG_BANNER_PATTERN)
            .requires(Items.PAPER)
            .requires(Items.ENCHANTED_GOLDEN_APPLE)
            .unlockedBy("has_enchanted_golden_apple", this.has(Items.ENCHANTED_GOLDEN_APPLE))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SCAFFOLDING, 6)
            .define('~', Items.STRING)
            .define('I', Blocks.BAMBOO)
            .pattern("I~I")
            .pattern("I I")
            .pattern("I I")
            .unlockedBy("has_bamboo", this.has(Blocks.BAMBOO))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.GRINDSTONE)
            .define('I', Items.STICK)
            .define('-', Blocks.STONE_SLAB)
            .define('#', ItemTags.PLANKS)
            .pattern("I-I")
            .pattern("# #")
            .unlockedBy("has_stone_slab", this.has(Blocks.STONE_SLAB))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.BLAST_FURNACE)
            .define('#', Blocks.SMOOTH_STONE)
            .define('X', Blocks.FURNACE)
            .define('I', Items.IRON_INGOT)
            .pattern("III")
            .pattern("IXI")
            .pattern("###")
            .unlockedBy("has_smooth_stone", this.has(Blocks.SMOOTH_STONE))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SMOKER)
            .define('#', ItemTags.LOGS)
            .define('X', Blocks.FURNACE)
            .pattern(" # ")
            .pattern("#X#")
            .pattern(" # ")
            .unlockedBy("has_furnace", this.has(Blocks.FURNACE))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.CARTOGRAPHY_TABLE)
            .define('#', ItemTags.PLANKS)
            .define('@', Items.PAPER)
            .pattern("@@")
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_paper", this.has(Items.PAPER))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.SMITHING_TABLE)
            .define('#', ItemTags.PLANKS)
            .define('@', Items.IRON_INGOT)
            .pattern("@@")
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.FLETCHING_TABLE)
            .define('#', ItemTags.PLANKS)
            .define('@', Items.FLINT)
            .pattern("@@")
            .pattern("##")
            .pattern("##")
            .unlockedBy("has_flint", this.has(Items.FLINT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.STONECUTTER)
            .define('I', Items.IRON_INGOT)
            .define('#', Blocks.STONE)
            .pattern(" I ")
            .pattern("###")
            .unlockedBy("has_stone", this.has(Blocks.STONE))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.LODESTONE)
            .define('S', Items.CHISELED_STONE_BRICKS)
            .define('#', Items.IRON_INGOT)
            .pattern("SSS")
            .pattern("S#S")
            .pattern("SSS")
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .unlockedBy("has_lodestone", this.has(Items.LODESTONE))
            .save(this.output);
        this.nineBlockStorageRecipesRecipesWithCustomUnpacking(
            RecipeCategory.MISC,
            Items.NETHERITE_INGOT,
            RecipeCategory.BUILDING_BLOCKS,
            Items.NETHERITE_BLOCK,
            "netherite_ingot_from_netherite_block",
            "netherite_ingot"
        );
        this.shapeless(RecipeCategory.MISC, Items.NETHERITE_INGOT)
            .requires(Items.NETHERITE_SCRAP, 4)
            .requires(Items.GOLD_INGOT, 4)
            .group("netherite_ingot")
            .unlockedBy("has_netherite_scrap", this.has(Items.NETHERITE_SCRAP))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.RESPAWN_ANCHOR)
            .define('O', Blocks.CRYING_OBSIDIAN)
            .define('G', Blocks.GLOWSTONE)
            .pattern("OOO")
            .pattern("GGG")
            .pattern("OOO")
            .unlockedBy("has_obsidian", this.has(Blocks.CRYING_OBSIDIAN))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.IRON_CHAIN)
            .define('I', Items.IRON_INGOT)
            .define('N', Items.IRON_NUGGET)
            .pattern("N")
            .pattern("I")
            .pattern("N")
            .unlockedBy("has_iron_nugget", this.has(Items.IRON_NUGGET))
            .unlockedBy("has_iron_ingot", this.has(Items.IRON_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.COPPER_CHAIN.weathering().unaffected())
            .define('I', Items.COPPER_INGOT)
            .define('N', Items.COPPER_NUGGET)
            .pattern("N")
            .pattern("I")
            .pattern("N")
            .unlockedBy("has_copper_nugget", this.has(Items.COPPER_NUGGET))
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Blocks.GOLDEN_DANDELION)
            .define('I', Items.DANDELION)
            .define('#', Items.GOLD_NUGGET)
            .pattern("###")
            .pattern("#I#")
            .pattern("###")
            .unlockedBy("has_gold_nugget", this.has(Items.GOLD_NUGGET))
            .unlockedBy("has_dandelion", this.has(Items.DANDELION))
            .save(this.output);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.TINTED_GLASS, 2)
            .define('G', Blocks.GLASS)
            .define('S', Items.AMETHYST_SHARD)
            .pattern(" S ")
            .pattern("SGS")
            .pattern(" S ")
            .unlockedBy("has_amethyst_shard", this.has(Items.AMETHYST_SHARD))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.BUILDING_BLOCKS, Blocks.AMETHYST_BLOCK, Items.AMETHYST_SHARD);
        this.shaped(RecipeCategory.TOOLS, Items.RECOVERY_COMPASS)
            .define('C', Items.COMPASS)
            .define('S', Items.ECHO_SHARD)
            .pattern("SSS")
            .pattern("SCS")
            .pattern("SSS")
            .unlockedBy("has_echo_shard", this.has(Items.ECHO_SHARD))
            .save(this.output);
        this.shaped(RecipeCategory.REDSTONE, Items.CALIBRATED_SCULK_SENSOR)
            .define('#', Items.AMETHYST_SHARD)
            .define('X', Items.SCULK_SENSOR)
            .pattern(" # ")
            .pattern("#X#")
            .unlockedBy("has_amethyst_shard", this.has(Items.AMETHYST_SHARD))
            .save(this.output);
        this.threeByThreePacker(RecipeCategory.MISC, Items.MUSIC_DISC_5, Items.DISC_FRAGMENT_5);
        this.dyedItem(Items.LEATHER_HELMET, "dyed_armor");
        this.dyedItem(Items.LEATHER_CHESTPLATE, "dyed_armor");
        this.dyedItem(Items.LEATHER_LEGGINGS, "dyed_armor");
        this.dyedItem(Items.LEATHER_BOOTS, "dyed_armor");
        this.dyedItem(Items.LEATHER_HORSE_ARMOR, "dyed_armor");
        this.dyedItem(Items.WOLF_ARMOR, "dyed_armor");
        CustomCraftingRecipeBuilder.customCrafting(
                RecipeCategory.MISC,
                (commonInfo, bookInfo) -> new ImbueRecipe(
                    commonInfo, bookInfo, Ingredient.of(Items.LINGERING_POTION), Ingredient.of(Items.ARROW), new ItemStackTemplate(Items.TIPPED_ARROW, 8)
                )
            )
            .unlockedBy("has_lingering_potion", this.has(Items.LINGERING_POTION))
            .save(this.output, "tipped_arrow");
        SpecialRecipeBuilder.special(
                () -> new BookCloningRecipe(
                    Ingredient.of(Items.WRITTEN_BOOK),
                    this.tag(ItemTags.BOOK_CLONING_TARGET),
                    BookCloningRecipe.DEFAULT_BOOK_GENERATION_RANGES,
                    new ItemStackTemplate(Items.WRITTEN_BOOK)
                )
            )
            .save(this.output, "book_cloning");
        SpecialRecipeBuilder.special(
                () -> new FireworkRocketRecipe(
                    Ingredient.of(Items.PAPER),
                    Ingredient.of(Items.GUNPOWDER),
                    Ingredient.of(Items.FIREWORK_STAR),
                    new ItemStackTemplate(Items.FIREWORK_ROCKET, 3)
                )
            )
            .save(this.output, "firework_rocket");
        SpecialRecipeBuilder.special(
                () -> new FireworkStarRecipe(
                    Map.of(
                        FireworkExplosion.Shape.LARGE_BALL,
                        Ingredient.of(Items.FIRE_CHARGE),
                        FireworkExplosion.Shape.BURST,
                        Ingredient.of(Items.FEATHER),
                        FireworkExplosion.Shape.STAR,
                        Ingredient.of(Items.GOLD_NUGGET),
                        FireworkExplosion.Shape.CREEPER,
                        this.tag(ItemTags.SKULLS)
                    ),
                    Ingredient.of(Items.DIAMOND),
                    Ingredient.of(Items.GLOWSTONE_DUST),
                    Ingredient.of(Items.GUNPOWDER),
                    this.tag(ItemTags.DYES),
                    new ItemStackTemplate(Items.FIREWORK_STAR)
                )
            )
            .save(this.output, "firework_star");
        SpecialRecipeBuilder.special(
                () -> new FireworkStarFadeRecipe(Ingredient.of(Items.FIREWORK_STAR), this.tag(ItemTags.DYES), new ItemStackTemplate(Items.FIREWORK_STAR))
            )
            .save(this.output, "firework_star_fade");
        TransmuteRecipeBuilder.transmute(
                RecipeCategory.MISC, Ingredient.of(Items.FILLED_MAP), Ingredient.of(Items.MAP), new ItemStackTemplate(Items.FILLED_MAP)
            )
            .addMaterialCountToOutput()
            .setMaterialCount(TransmuteRecipe.FULL_RANGE_MATERIAL_COUNT)
            .group("map_cloning")
            .unlockedBy("has_filled_map", this.has(Items.FILLED_MAP))
            .save(this.output, "map_cloning");
        SpecialRecipeBuilder.special(
                () -> new MapExtendingRecipe(Ingredient.of(Items.FILLED_MAP), Ingredient.of(Items.PAPER), new ItemStackTemplate(Items.FILLED_MAP))
            )
            .save(this.output, "map_extending");
        SpecialRecipeBuilder.special(RepairItemRecipe::new).save(this.output, "repair_item");
        SpecialRecipeBuilder.special(
                () -> new ShieldDecorationRecipe(this.tag(ItemTags.BANNERS), Ingredient.of(Items.SHIELD), new ItemStackTemplate(Items.SHIELD))
            )
            .save(this.output, "shield_decoration");
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.POTATO), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.BAKED_POTATO, 0.35F, 200)
            .unlockedBy("has_potato", this.has(Items.POTATO))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.CLAY_BALL), RecipeCategory.MISC, CookingBookCategory.MISC, Items.BRICK, 0.3F, 200)
            .unlockedBy("has_clay_ball", this.has(Items.CLAY_BALL))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(this.tag(ItemTags.LOGS_THAT_BURN), RecipeCategory.MISC, CookingBookCategory.MISC, Items.CHARCOAL, 0.15F, 200)
            .unlockedBy("has_log", this.has(ItemTags.LOGS_THAT_BURN))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Items.CHORUS_FRUIT), RecipeCategory.MISC, CookingBookCategory.MISC, Items.POPPED_CHORUS_FRUIT, 0.1F, 200
            )
            .unlockedBy("has_chorus_fruit", this.has(Items.CHORUS_FRUIT))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.BEEF), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.COOKED_BEEF, 0.35F, 200)
            .unlockedBy("has_beef", this.has(Items.BEEF))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.CHICKEN), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.COOKED_CHICKEN, 0.35F, 200)
            .unlockedBy("has_chicken", this.has(Items.CHICKEN))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.COD), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.COOKED_COD, 0.35F, 200)
            .unlockedBy("has_cod", this.has(Items.COD))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Blocks.KELP), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.DRIED_KELP, 0.1F, 200)
            .unlockedBy("has_kelp", this.has(Blocks.KELP))
            .save(this.output, getSmeltingRecipeName(Items.DRIED_KELP));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.SALMON), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.COOKED_SALMON, 0.35F, 200)
            .unlockedBy("has_salmon", this.has(Items.SALMON))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.MUTTON), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.COOKED_MUTTON, 0.35F, 200)
            .unlockedBy("has_mutton", this.has(Items.MUTTON))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.PORKCHOP), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.COOKED_PORKCHOP, 0.35F, 200)
            .unlockedBy("has_porkchop", this.has(Items.PORKCHOP))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.RABBIT), RecipeCategory.FOOD, CookingBookCategory.FOOD, Items.COOKED_RABBIT, 0.35F, 200)
            .unlockedBy("has_rabbit", this.has(Items.RABBIT))
            .save(this.output);
        this.oreSmelting(COAL_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.COAL, 0.1F, 200, "coal");
        this.oreSmelting(IRON_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.IRON_INGOT, 0.7F, 200, "iron_ingot");
        this.oreSmelting(COPPER_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.COPPER_INGOT, 0.7F, 200, "copper_ingot");
        this.oreSmelting(GOLD_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.GOLD_INGOT, 1.0F, 200, "gold_ingot");
        this.oreSmelting(DIAMOND_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.DIAMOND, 1.0F, 200, "diamond");
        this.oreSmelting(LAPIS_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.LAPIS_LAZULI, 0.2F, 200, "lapis_lazuli");
        this.oreSmelting(REDSTONE_SMELTABLES, RecipeCategory.REDSTONE, CookingBookCategory.BLOCKS, Items.REDSTONE, 0.7F, 200, "redstone");
        this.oreSmelting(EMERALD_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.EMERALD, 1.0F, 200, "emerald");
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.RAW_IRON, RecipeCategory.BUILDING_BLOCKS, Items.RAW_IRON_BLOCK);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.RAW_COPPER, RecipeCategory.BUILDING_BLOCKS, Items.RAW_COPPER_BLOCK);
        this.nineBlockStorageRecipes(RecipeCategory.MISC, Items.RAW_GOLD, RecipeCategory.BUILDING_BLOCKS, Items.RAW_GOLD_BLOCK);
        SimpleCookingRecipeBuilder.smelting(
                this.tag(ItemTags.SMELTS_TO_GLASS), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.GLASS, 0.1F, 200
            )
            .unlockedBy("has_smelts_to_glass", this.has(ItemTags.SMELTS_TO_GLASS))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Blocks.SEA_PICKLE), RecipeCategory.MISC, CookingBookCategory.MISC, Items.DYE.lime(), 0.1F, 200)
            .unlockedBy("has_sea_pickle", this.has(Blocks.SEA_PICKLE))
            .save(this.output, getSmeltingRecipeName(Items.DYE.lime()));
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.CACTUS), RecipeCategory.MISC, CookingBookCategory.MISC, Items.DYE.green(), 1.0F, 200)
            .unlockedBy("has_cactus", this.has(Blocks.CACTUS))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(
                    Items.GOLDEN_PICKAXE,
                    Items.GOLDEN_SHOVEL,
                    Items.GOLDEN_AXE,
                    Items.GOLDEN_HOE,
                    Items.GOLDEN_SWORD,
                    Items.GOLDEN_SPEAR,
                    Items.GOLDEN_HELMET,
                    Items.GOLDEN_CHESTPLATE,
                    Items.GOLDEN_LEGGINGS,
                    Items.GOLDEN_BOOTS,
                    Items.GOLDEN_HORSE_ARMOR,
                    Items.GOLDEN_NAUTILUS_ARMOR
                ),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.GOLD_NUGGET,
                0.1F,
                200
            )
            .unlockedBy("has_golden_pickaxe", this.has(Items.GOLDEN_PICKAXE))
            .unlockedBy("has_golden_shovel", this.has(Items.GOLDEN_SHOVEL))
            .unlockedBy("has_golden_axe", this.has(Items.GOLDEN_AXE))
            .unlockedBy("has_golden_hoe", this.has(Items.GOLDEN_HOE))
            .unlockedBy("has_golden_sword", this.has(Items.GOLDEN_SWORD))
            .unlockedBy("has_golden_spear", this.has(Items.GOLDEN_SPEAR))
            .unlockedBy("has_golden_helmet", this.has(Items.GOLDEN_HELMET))
            .unlockedBy("has_golden_chestplate", this.has(Items.GOLDEN_CHESTPLATE))
            .unlockedBy("has_golden_leggings", this.has(Items.GOLDEN_LEGGINGS))
            .unlockedBy("has_golden_boots", this.has(Items.GOLDEN_BOOTS))
            .unlockedBy("has_golden_horse_armor", this.has(Items.GOLDEN_HORSE_ARMOR))
            .unlockedBy("has_golden_nautilus_armor", this.has(Items.GOLDEN_NAUTILUS_ARMOR))
            .save(this.output, getSmeltingRecipeName(Items.GOLD_NUGGET));
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(
                    Items.COPPER_PICKAXE,
                    Items.COPPER_SHOVEL,
                    Items.COPPER_AXE,
                    Items.COPPER_HOE,
                    Items.COPPER_SWORD,
                    Items.COPPER_SPEAR,
                    Items.COPPER_HELMET,
                    Items.COPPER_CHESTPLATE,
                    Items.COPPER_LEGGINGS,
                    Items.COPPER_BOOTS,
                    Items.COPPER_HORSE_ARMOR,
                    Items.COPPER_NAUTILUS_ARMOR
                ),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.COPPER_NUGGET,
                0.1F,
                200
            )
            .unlockedBy("has_copper_pickaxe", this.has(Items.COPPER_PICKAXE))
            .unlockedBy("has_copper_shovel", this.has(Items.COPPER_SHOVEL))
            .unlockedBy("has_copper_axe", this.has(Items.COPPER_AXE))
            .unlockedBy("has_copper_hoe", this.has(Items.COPPER_HOE))
            .unlockedBy("has_copper_sword", this.has(Items.COPPER_SWORD))
            .unlockedBy("has_copper_spear", this.has(Items.COPPER_SPEAR))
            .unlockedBy("has_copper_helmet", this.has(Items.COPPER_HELMET))
            .unlockedBy("has_copper_chestplate", this.has(Items.COPPER_CHESTPLATE))
            .unlockedBy("has_copper_leggings", this.has(Items.COPPER_LEGGINGS))
            .unlockedBy("has_copper_boots", this.has(Items.COPPER_BOOTS))
            .unlockedBy("has_copper_horse_armor", this.has(Items.COPPER_HORSE_ARMOR))
            .unlockedBy("has_copper_nautilus_armor", this.has(Items.COPPER_NAUTILUS_ARMOR))
            .save(this.output, getSmeltingRecipeName(Items.COPPER_NUGGET));
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(
                    Items.IRON_PICKAXE,
                    Items.IRON_SHOVEL,
                    Items.IRON_AXE,
                    Items.IRON_HOE,
                    Items.IRON_SWORD,
                    Items.IRON_SPEAR,
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS,
                    Items.IRON_HORSE_ARMOR,
                    Items.CHAINMAIL_HELMET,
                    Items.CHAINMAIL_CHESTPLATE,
                    Items.CHAINMAIL_LEGGINGS,
                    Items.CHAINMAIL_BOOTS,
                    Items.IRON_NAUTILUS_ARMOR
                ),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.IRON_NUGGET,
                0.1F,
                200
            )
            .unlockedBy("has_iron_pickaxe", this.has(Items.IRON_PICKAXE))
            .unlockedBy("has_iron_shovel", this.has(Items.IRON_SHOVEL))
            .unlockedBy("has_iron_axe", this.has(Items.IRON_AXE))
            .unlockedBy("has_iron_hoe", this.has(Items.IRON_HOE))
            .unlockedBy("has_iron_sword", this.has(Items.IRON_SWORD))
            .unlockedBy("has_iron_spear", this.has(Items.IRON_SPEAR))
            .unlockedBy("has_iron_helmet", this.has(Items.IRON_HELMET))
            .unlockedBy("has_iron_chestplate", this.has(Items.IRON_CHESTPLATE))
            .unlockedBy("has_iron_leggings", this.has(Items.IRON_LEGGINGS))
            .unlockedBy("has_iron_boots", this.has(Items.IRON_BOOTS))
            .unlockedBy("has_iron_horse_armor", this.has(Items.IRON_HORSE_ARMOR))
            .unlockedBy("has_chainmail_helmet", this.has(Items.CHAINMAIL_HELMET))
            .unlockedBy("has_chainmail_chestplate", this.has(Items.CHAINMAIL_CHESTPLATE))
            .unlockedBy("has_chainmail_leggings", this.has(Items.CHAINMAIL_LEGGINGS))
            .unlockedBy("has_chainmail_boots", this.has(Items.CHAINMAIL_BOOTS))
            .unlockedBy("has_iron_nautilus_armor", this.has(Items.IRON_NAUTILUS_ARMOR))
            .save(this.output, getSmeltingRecipeName(Items.IRON_NUGGET));
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.CLAY), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.TERRACOTTA, 0.35F, 200
            )
            .unlockedBy("has_clay_block", this.has(Blocks.CLAY))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Blocks.NETHERRACK), RecipeCategory.MISC, CookingBookCategory.MISC, Items.NETHER_BRICK, 0.1F, 200)
            .unlockedBy("has_netherrack", this.has(Blocks.NETHERRACK))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Items.RESIN_CLUMP), RecipeCategory.MISC, CookingBookCategory.MISC, Items.RESIN_BRICK, 0.1F, 200)
            .unlockedBy("has_resin_clump", this.has(Blocks.RESIN_CLUMP))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(Blocks.NETHER_QUARTZ_ORE), RecipeCategory.MISC, CookingBookCategory.MISC, Items.QUARTZ, 0.2F, 200)
            .unlockedBy("has_nether_quartz_ore", this.has(Blocks.NETHER_QUARTZ_ORE))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.WET_SPONGE), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.SPONGE, 0.15F, 200
            )
            .unlockedBy("has_wet_sponge", this.has(Blocks.WET_SPONGE))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.STONE), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.SMOOTH_STONE, 0.1F, 200
            )
            .unlockedBy("has_stone", this.has(Blocks.STONE))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.SANDSTONE), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.SMOOTH_SANDSTONE, 0.1F, 200
            )
            .unlockedBy("has_sandstone", this.has(Blocks.SANDSTONE))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.RED_SANDSTONE), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.SMOOTH_RED_SANDSTONE, 0.1F, 200
            )
            .unlockedBy("has_red_sandstone", this.has(Blocks.RED_SANDSTONE))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.QUARTZ_BLOCK), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.SMOOTH_QUARTZ, 0.1F, 200
            )
            .unlockedBy("has_quartz_block", this.has(Blocks.QUARTZ_BLOCK))
            .save(this.output);
        DyeColor.VALUES
            .forEach(
                dyeColor -> SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(Blocks.DYED_TERRACOTTA.pick(dyeColor)),
                        RecipeCategory.DECORATIONS,
                        CookingBookCategory.BLOCKS,
                        Items.GLAZED_TERRACOTTA.pick(dyeColor),
                        0.1F,
                        200
                    )
                    .unlockedBy("has_" + dyeColor.getName() + "_terracotta", this.has(Blocks.DYED_TERRACOTTA.pick(dyeColor)))
                    .save(this.output)
            );
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.ANCIENT_DEBRIS), RecipeCategory.MISC, CookingBookCategory.MISC, Items.NETHERITE_SCRAP, 2.0F, 200
            )
            .unlockedBy("has_ancient_debris", this.has(Blocks.ANCIENT_DEBRIS))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(
                Ingredient.of(Blocks.BASALT), RecipeCategory.BUILDING_BLOCKS, CookingBookCategory.BLOCKS, Items.SMOOTH_BASALT, 0.1F, 200
            )
            .unlockedBy("has_basalt", this.has(Blocks.BASALT))
            .save(this.output);
        SimpleCookingRecipeBuilder.smelting(this.tag(ItemTags.LEAVES), RecipeCategory.MISC, CookingBookCategory.BLOCKS, Items.LEAF_LITTER, 0.1F, 200)
            .unlockedBy("has_leaves", this.has(ItemTags.LEAVES))
            .save(this.output);
        this.oreBlasting(COAL_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.COAL, 0.1F, 100, "coal");
        this.oreBlasting(IRON_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.IRON_INGOT, 0.7F, 100, "iron_ingot");
        this.oreBlasting(COPPER_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.COPPER_INGOT, 0.7F, 100, "copper_ingot");
        this.oreBlasting(GOLD_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.GOLD_INGOT, 1.0F, 100, "gold_ingot");
        this.oreBlasting(DIAMOND_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.DIAMOND, 1.0F, 100, "diamond");
        this.oreBlasting(LAPIS_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.LAPIS_LAZULI, 0.2F, 100, "lapis_lazuli");
        this.oreBlasting(REDSTONE_SMELTABLES, RecipeCategory.REDSTONE, CookingBookCategory.BLOCKS, Items.REDSTONE, 0.7F, 100, "redstone");
        this.oreBlasting(EMERALD_SMELTABLES, RecipeCategory.MISC, CookingBookCategory.MISC, Items.EMERALD, 1.0F, 100, "emerald");
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(Blocks.NETHER_QUARTZ_ORE), RecipeCategory.MISC, CookingBookCategory.MISC, Items.QUARTZ, 0.2F, 100)
            .unlockedBy("has_nether_quartz_ore", this.has(Blocks.NETHER_QUARTZ_ORE))
            .save(this.output, getBlastingRecipeName(Items.QUARTZ));
        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(
                    Items.GOLDEN_PICKAXE,
                    Items.GOLDEN_SHOVEL,
                    Items.GOLDEN_AXE,
                    Items.GOLDEN_HOE,
                    Items.GOLDEN_SWORD,
                    Items.GOLDEN_SPEAR,
                    Items.GOLDEN_HELMET,
                    Items.GOLDEN_CHESTPLATE,
                    Items.GOLDEN_LEGGINGS,
                    Items.GOLDEN_BOOTS,
                    Items.GOLDEN_HORSE_ARMOR,
                    Items.GOLDEN_NAUTILUS_ARMOR
                ),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.GOLD_NUGGET,
                0.1F,
                100
            )
            .unlockedBy("has_golden_pickaxe", this.has(Items.GOLDEN_PICKAXE))
            .unlockedBy("has_golden_shovel", this.has(Items.GOLDEN_SHOVEL))
            .unlockedBy("has_golden_axe", this.has(Items.GOLDEN_AXE))
            .unlockedBy("has_golden_hoe", this.has(Items.GOLDEN_HOE))
            .unlockedBy("has_golden_sword", this.has(Items.GOLDEN_SWORD))
            .unlockedBy("has_golden_spear", this.has(Items.GOLDEN_SPEAR))
            .unlockedBy("has_golden_helmet", this.has(Items.GOLDEN_HELMET))
            .unlockedBy("has_golden_chestplate", this.has(Items.GOLDEN_CHESTPLATE))
            .unlockedBy("has_golden_leggings", this.has(Items.GOLDEN_LEGGINGS))
            .unlockedBy("has_golden_boots", this.has(Items.GOLDEN_BOOTS))
            .unlockedBy("has_golden_horse_armor", this.has(Items.GOLDEN_HORSE_ARMOR))
            .unlockedBy("has_golden_nautilus_armor", this.has(Items.GOLDEN_NAUTILUS_ARMOR))
            .save(this.output, getBlastingRecipeName(Items.GOLD_NUGGET));
        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(
                    Items.COPPER_PICKAXE,
                    Items.COPPER_SHOVEL,
                    Items.COPPER_AXE,
                    Items.COPPER_HOE,
                    Items.COPPER_SWORD,
                    Items.COPPER_SPEAR,
                    Items.COPPER_HELMET,
                    Items.COPPER_CHESTPLATE,
                    Items.COPPER_LEGGINGS,
                    Items.COPPER_BOOTS,
                    Items.COPPER_HORSE_ARMOR,
                    Items.COPPER_NAUTILUS_ARMOR
                ),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.COPPER_NUGGET,
                0.1F,
                100
            )
            .unlockedBy("has_copper_pickaxe", this.has(Items.COPPER_PICKAXE))
            .unlockedBy("has_copper_shovel", this.has(Items.COPPER_SHOVEL))
            .unlockedBy("has_copper_axe", this.has(Items.COPPER_AXE))
            .unlockedBy("has_copper_hoe", this.has(Items.COPPER_HOE))
            .unlockedBy("has_copper_sword", this.has(Items.COPPER_SWORD))
            .unlockedBy("has_copper_spear", this.has(Items.COPPER_SPEAR))
            .unlockedBy("has_copper_helmet", this.has(Items.COPPER_HELMET))
            .unlockedBy("has_copper_chestplate", this.has(Items.COPPER_CHESTPLATE))
            .unlockedBy("has_copper_leggings", this.has(Items.COPPER_LEGGINGS))
            .unlockedBy("has_copper_boots", this.has(Items.COPPER_BOOTS))
            .unlockedBy("has_copper_horse_armor", this.has(Items.COPPER_HORSE_ARMOR))
            .unlockedBy("has_copper_nautilus_armor", this.has(Items.COPPER_NAUTILUS_ARMOR))
            .save(this.output, getBlastingRecipeName(Items.COPPER_NUGGET));
        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(
                    Items.IRON_PICKAXE,
                    Items.IRON_SHOVEL,
                    Items.IRON_AXE,
                    Items.IRON_HOE,
                    Items.IRON_SWORD,
                    Items.IRON_SPEAR,
                    Items.IRON_HELMET,
                    Items.IRON_CHESTPLATE,
                    Items.IRON_LEGGINGS,
                    Items.IRON_BOOTS,
                    Items.IRON_HORSE_ARMOR,
                    Items.IRON_NAUTILUS_ARMOR,
                    Items.CHAINMAIL_HELMET,
                    Items.CHAINMAIL_CHESTPLATE,
                    Items.CHAINMAIL_LEGGINGS,
                    Items.CHAINMAIL_BOOTS
                ),
                RecipeCategory.MISC,
                CookingBookCategory.MISC,
                Items.IRON_NUGGET,
                0.1F,
                100
            )
            .unlockedBy("has_iron_pickaxe", this.has(Items.IRON_PICKAXE))
            .unlockedBy("has_iron_shovel", this.has(Items.IRON_SHOVEL))
            .unlockedBy("has_iron_axe", this.has(Items.IRON_AXE))
            .unlockedBy("has_iron_hoe", this.has(Items.IRON_HOE))
            .unlockedBy("has_iron_sword", this.has(Items.IRON_SWORD))
            .unlockedBy("has_iron_spear", this.has(Items.IRON_SPEAR))
            .unlockedBy("has_iron_helmet", this.has(Items.IRON_HELMET))
            .unlockedBy("has_iron_chestplate", this.has(Items.IRON_CHESTPLATE))
            .unlockedBy("has_iron_leggings", this.has(Items.IRON_LEGGINGS))
            .unlockedBy("has_iron_boots", this.has(Items.IRON_BOOTS))
            .unlockedBy("has_iron_horse_armor", this.has(Items.IRON_HORSE_ARMOR))
            .unlockedBy("has_chainmail_helmet", this.has(Items.CHAINMAIL_HELMET))
            .unlockedBy("has_chainmail_chestplate", this.has(Items.CHAINMAIL_CHESTPLATE))
            .unlockedBy("has_chainmail_leggings", this.has(Items.CHAINMAIL_LEGGINGS))
            .unlockedBy("has_chainmail_boots", this.has(Items.CHAINMAIL_BOOTS))
            .unlockedBy("has_iron_nautilus_armor", this.has(Items.IRON_NAUTILUS_ARMOR))
            .save(this.output, getBlastingRecipeName(Items.IRON_NUGGET));
        SimpleCookingRecipeBuilder.blasting(
                Ingredient.of(Blocks.ANCIENT_DEBRIS), RecipeCategory.MISC, CookingBookCategory.MISC, Items.NETHERITE_SCRAP, 2.0F, 100
            )
            .unlockedBy("has_ancient_debris", this.has(Blocks.ANCIENT_DEBRIS))
            .save(this.output, getBlastingRecipeName(Items.NETHERITE_SCRAP));
        this.cookRecipes("smoking", SmokingRecipe::new, 100);
        this.cookRecipes("campfire_cooking", CampfireCookingRecipe::new, 600);
        this.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, Blocks.POLISHED_BASALT, Blocks.BASALT);
        this.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE, 2);
        WeatheringCopperCollection.zipApply(
            Blocks.CUT_COPPER,
            Blocks.COPPER_BLOCK,
            (cutBlock, material) -> this.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, cutBlock, material, 4)
        );
        WeatheringCopperCollection.zipApply(
            Blocks.CUT_COPPER_STAIRS,
            Blocks.COPPER_BLOCK,
            (cutStairs, material) -> this.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, cutStairs, material, 4)
        );
        WeatheringCopperCollection.zipApply(
            Blocks.CUT_COPPER_SLAB,
            Blocks.COPPER_BLOCK,
            (cutSlab, material) -> this.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, cutSlab, material, 8)
        );
        smithingTrims().forEach(trim -> this.trimSmithing(trim.template(), trim.patternId(), trim.recipeId()));
        this.netheriteSmithing(Items.DIAMOND_CHESTPLATE, RecipeCategory.COMBAT, Items.NETHERITE_CHESTPLATE);
        this.netheriteSmithing(Items.DIAMOND_LEGGINGS, RecipeCategory.COMBAT, Items.NETHERITE_LEGGINGS);
        this.netheriteSmithing(Items.DIAMOND_HELMET, RecipeCategory.COMBAT, Items.NETHERITE_HELMET);
        this.netheriteSmithing(Items.DIAMOND_BOOTS, RecipeCategory.COMBAT, Items.NETHERITE_BOOTS);
        this.netheriteSmithing(Items.DIAMOND_NAUTILUS_ARMOR, RecipeCategory.COMBAT, Items.NETHERITE_NAUTILUS_ARMOR);
        this.netheriteSmithing(Items.DIAMOND_HORSE_ARMOR, RecipeCategory.COMBAT, Items.NETHERITE_HORSE_ARMOR);
        this.netheriteSmithing(Items.DIAMOND_SWORD, RecipeCategory.COMBAT, Items.NETHERITE_SWORD);
        this.netheriteSmithing(Items.DIAMOND_SPEAR, RecipeCategory.COMBAT, Items.NETHERITE_SPEAR);
        this.netheriteSmithing(Items.DIAMOND_AXE, RecipeCategory.TOOLS, Items.NETHERITE_AXE);
        this.netheriteSmithing(Items.DIAMOND_PICKAXE, RecipeCategory.TOOLS, Items.NETHERITE_PICKAXE);
        this.netheriteSmithing(Items.DIAMOND_HOE, RecipeCategory.TOOLS, Items.NETHERITE_HOE);
        this.netheriteSmithing(Items.DIAMOND_SHOVEL, RecipeCategory.TOOLS, Items.NETHERITE_SHOVEL);
        this.copySmithingTemplate(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, Items.NETHERRACK);
        this.copySmithingTemplate(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLESTONE);
        this.copySmithingTemplate(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.SANDSTONE);
        this.copySmithingTemplate(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLESTONE);
        this.copySmithingTemplate(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.MOSSY_COBBLESTONE);
        this.copySmithingTemplate(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLED_DEEPSLATE);
        this.copySmithingTemplate(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.END_STONE);
        this.copySmithingTemplate(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLESTONE);
        this.copySmithingTemplate(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.PRISMARINE);
        this.copySmithingTemplate(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BLACKSTONE);
        this.copySmithingTemplate(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, Items.NETHERRACK);
        this.copySmithingTemplate(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.PURPUR_BLOCK);
        this.copySmithingTemplate(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, Items.COBBLED_DEEPSLATE);
        this.copySmithingTemplate(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
        this.copySmithingTemplate(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
        this.copySmithingTemplate(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
        this.copySmithingTemplate(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.TERRACOTTA);
        this.copySmithingTemplate(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BREEZE_ROD);
        this.copySmithingTemplate(
            Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, Ingredient.of(Items.COPPER_BLOCK.weathering().unaffected(), Items.COPPER_BLOCK.waxed().unaffected())
        );
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.BAMBOO_BLOCK, Items.BAMBOO);
        this.planksFromLogs(Blocks.BAMBOO_PLANKS, ItemTags.BAMBOO_BLOCKS, 2);
        this.mosaicBuilder(RecipeCategory.DECORATIONS, Blocks.BAMBOO_MOSAIC, Blocks.BAMBOO_SLAB);
        this.woodenBoat(Items.BAMBOO_RAFT, Blocks.BAMBOO_PLANKS);
        this.chestBoat(Items.BAMBOO_CHEST_RAFT, Items.BAMBOO_RAFT);
        this.shaped(RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_BOOKSHELF)
            .define('#', ItemTags.PLANKS)
            .define('X', ItemTags.WOODEN_SLABS)
            .pattern("###")
            .pattern("XXX")
            .pattern("###")
            .unlockedBy("has_book", this.has(Items.BOOK))
            .save(this.output);
        this.oneToOneConversionRecipe(Items.DYE.orange(), Blocks.TORCHFLOWER, "orange_dye");
        this.oneToOneConversionRecipe(Items.DYE.cyan(), Blocks.PITCHER_PLANT, "cyan_dye", 2);
        this.planksFromLog(Blocks.CHERRY_PLANKS, ItemTags.CHERRY_LOGS, 4);
        this.woodFromLogs(Blocks.CHERRY_WOOD, Blocks.CHERRY_LOG);
        this.woodFromLogs(Blocks.STRIPPED_CHERRY_WOOD, Blocks.STRIPPED_CHERRY_LOG);
        this.woodenBoat(Items.CHERRY_BOAT, Blocks.CHERRY_PLANKS);
        this.chestBoat(Items.CHERRY_CHEST_BOAT, Items.CHERRY_BOAT);
        this.oneToOneConversionRecipe(Items.DYE.pink(), Items.PINK_PETALS, "pink_dye", 1);
        this.shaped(RecipeCategory.TOOLS, Items.BRUSH)
            .define('X', Items.FEATHER)
            .define('#', Items.COPPER_INGOT)
            .define('I', Items.STICK)
            .pattern("X")
            .pattern("#")
            .pattern("I")
            .unlockedBy("has_copper_ingot", this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.shaped(RecipeCategory.DECORATIONS, Items.DECORATED_POT)
            .define('#', Items.BRICK)
            .pattern(" # ")
            .pattern("# #")
            .pattern(" # ")
            .unlockedBy("has_brick", this.has(ItemTags.DECORATED_POT_INGREDIENTS))
            .save(this.output, "decorated_pot_simple");
        SpecialRecipeBuilder.special(() -> new DecoratedPotRecipe(this.tag(ItemTags.DECORATED_POT_INGREDIENTS), new ItemStackTemplate(Items.DECORATED_POT)))
            .save(this.output, "decorated_pot");
        this.shaped(RecipeCategory.REDSTONE, Blocks.CRAFTER)
            .define('#', Items.IRON_INGOT)
            .define('C', Items.CRAFTING_TABLE)
            .define('R', Items.REDSTONE)
            .define('D', Items.DROPPER)
            .pattern("###")
            .pattern("#C#")
            .pattern("RDR")
            .unlockedBy("has_dropper", this.has(Items.DROPPER))
            .save(this.output);
        WeatheringCopper.WeatherState.forEach(
            state -> {
                this.stonecutterResultFromBase(
                    RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_COPPER.weathering().pick(state), Blocks.COPPER_BLOCK.weathering().pick(state), 4
                );
                this.stonecutterResultFromBase(
                    RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_COPPER.waxed().pick(state), Blocks.COPPER_BLOCK.waxed().pick(state), 4
                );
                this.stonecutterResultFromBase(
                    RecipeCategory.BUILDING_BLOCKS, Blocks.CHISELED_COPPER.waxed().pick(state), Blocks.CUT_COPPER.waxed().pick(state), 1
                );
            }
        );
        WeatheringCopperCollection.zipApply(Blocks.COPPER_GRATE, Blocks.COPPER_BLOCK, (x$0, x$1) -> this.grate(x$0, x$1));
        WeatheringCopperCollection.zipApply(Blocks.COPPER_BULB, Blocks.COPPER_BLOCK, (x$0, x$1) -> this.copperBulb(x$0, x$1));
        WeatheringCopper.WeatherState.forEach(
            state -> this.waxedChiseled(Blocks.CHISELED_COPPER.waxed().pick(state), Blocks.CUT_COPPER_SLAB.waxed().pick(state))
        );
        WeatheringCopperCollection.zipApply(
            Blocks.COPPER_GRATE, Blocks.COPPER_BLOCK, (grate, block) -> this.stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, grate, block, 4)
        );
        this.shapeless(RecipeCategory.MISC, Items.WIND_CHARGE, 4)
            .requires(Items.BREEZE_ROD)
            .unlockedBy("has_breeze_rod", this.has(Items.BREEZE_ROD))
            .save(this.output);
        this.shaped(RecipeCategory.COMBAT, Items.MACE, 1)
            .define('I', Items.BREEZE_ROD)
            .define('#', Blocks.HEAVY_CORE)
            .pattern(" # ")
            .pattern(" I ")
            .unlockedBy("has_breeze_rod", this.has(Items.BREEZE_ROD))
            .unlockedBy("has_heavy_core", this.has(Blocks.HEAVY_CORE))
            .save(this.output);
        this.doorBuilder(Blocks.COPPER_DOOR.weathering().unaffected(), Ingredient.of(Items.COPPER_INGOT))
            .unlockedBy(getHasName(Items.COPPER_INGOT), this.has(Items.COPPER_INGOT))
            .save(this.output);
        this.twoByTwoPacker(RecipeCategory.REDSTONE, Blocks.COPPER_TRAPDOOR.weathering().unaffected(), Items.COPPER_INGOT);
        this.shaped(RecipeCategory.TOOLS, Items.BUNDLE)
            .define('-', Items.STRING)
            .define('#', Items.LEATHER)
            .pattern("-")
            .pattern("#")
            .unlockedBy("has_string", this.has(Items.STRING))
            .save(this.output);
        this.threeByThreePacker(RecipeCategory.BUILDING_BLOCKS, Blocks.POTENT_SULFUR, Items.SULFUR);
        ColorCollection.zipApply(Items.DYE, Items.DYED_BUNDLE, (x$0, x$1) -> this.dyedBundleRecipe(x$0, x$1));
    }

    public static Stream<VanillaRecipeProvider.TrimTemplate> smithingTrims() {
        return Stream.of(
                Pair.of(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.BOLT),
                Pair.of(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.COAST),
                Pair.of(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.DUNE),
                Pair.of(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.EYE),
                Pair.of(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.FLOW),
                Pair.of(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.HOST),
                Pair.of(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.RAISER),
                Pair.of(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.RIB),
                Pair.of(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.SENTRY),
                Pair.of(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.SHAPER),
                Pair.of(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.SILENCE),
                Pair.of(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.SNOUT),
                Pair.of(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.SPIRE),
                Pair.of(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.TIDE),
                Pair.of(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.VEX),
                Pair.of(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.WARD),
                Pair.of(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.WAYFINDER),
                Pair.of(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, TrimPatterns.WILD)
            )
            .map(itemAndPattern -> {
                Item item = (Item)itemAndPattern.getFirst();
                ResourceKey<TrimPattern> patternId = (ResourceKey<TrimPattern>)itemAndPattern.getSecond();
                ResourceKey<Recipe<?>> recipeId = ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace(getItemName(item) + "_smithing_trim"));
                return new VanillaRecipeProvider.TrimTemplate(item, patternId, recipeId);
            });
    }

    public static class Runner extends RecipeProvider.Runner {
        public Runner(final PackOutput packOutput, final CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
            return new VanillaRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "Vanilla Recipes";
        }
    }

    public record TrimTemplate(Item template, ResourceKey<TrimPattern> patternId, ResourceKey<Recipe<?>> recipeId) {
    }
}