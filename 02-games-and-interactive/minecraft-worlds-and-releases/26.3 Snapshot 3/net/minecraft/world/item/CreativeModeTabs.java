package net.minecraft.world.item;

import com.mojang.datafixers.util.Pair;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.InstrumentTags;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import org.jspecify.annotations.Nullable;

public class CreativeModeTabs {
   private static final Identifier INVENTORY_BACKGROUND = CreativeModeTab.createTextureLocation("inventory");
   private static final Identifier SEARCH_BACKGROUND = CreativeModeTab.createTextureLocation("item_search");
   private static final ResourceKey<CreativeModeTab> BUILDING_BLOCKS = createKey("building_blocks");
   private static final ResourceKey<CreativeModeTab> COLORED_BLOCKS = createKey("colored_blocks");
   private static final ResourceKey<CreativeModeTab> NATURAL_BLOCKS = createKey("natural_blocks");
   private static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = createKey("functional_blocks");
   private static final ResourceKey<CreativeModeTab> REDSTONE_BLOCKS = createKey("redstone_blocks");
   private static final ResourceKey<CreativeModeTab> HOTBAR = createKey("hotbar");
   private static final ResourceKey<CreativeModeTab> SEARCH = createKey("search");
   private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES = createKey("tools_and_utilities");
   private static final ResourceKey<CreativeModeTab> COMBAT = createKey("combat");
   private static final ResourceKey<CreativeModeTab> FOOD_AND_DRINKS = createKey("food_and_drinks");
   private static final ResourceKey<CreativeModeTab> INGREDIENTS = createKey("ingredients");
   private static final ResourceKey<CreativeModeTab> SPAWN_EGGS = createKey("spawn_eggs");
   private static final ResourceKey<CreativeModeTab> OP_BLOCKS = createKey("op_blocks");
   private static final ResourceKey<CreativeModeTab> INVENTORY = createKey("inventory");
   private static final Comparator<Holder<PaintingVariant>> PAINTING_COMPARATOR = Comparator.comparing(
      Holder::value, Comparator.comparingInt(PaintingVariant::area).thenComparing(PaintingVariant::width)
   );
   private static CreativeModeTab.@Nullable ItemDisplayParameters CACHED_PARAMETERS;

   private static ResourceKey<CreativeModeTab> createKey(final String id) {
      return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(id));
   }

   public static CreativeModeTab bootstrap(final Registry<CreativeModeTab> registry) {
      Registry.register(
         registry,
         BUILDING_BLOCKS,
         CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.buildingBlocks"))
            .icon(() -> new ItemStack(Blocks.BRICKS))
            .displayItems((parameters, buildingBlocks) -> {
               buildingBlocks.accept(Items.OAK_LOG);
               buildingBlocks.accept(Items.OAK_WOOD);
               buildingBlocks.accept(Items.STRIPPED_OAK_LOG);
               buildingBlocks.accept(Items.STRIPPED_OAK_WOOD);
               buildingBlocks.accept(Items.OAK_PLANKS);
               buildingBlocks.accept(Items.OAK_STAIRS);
               buildingBlocks.accept(Items.OAK_SLAB);
               buildingBlocks.accept(Items.OAK_FENCE);
               buildingBlocks.accept(Items.OAK_FENCE_GATE);
               buildingBlocks.accept(Items.OAK_DOOR);
               buildingBlocks.accept(Items.OAK_TRAPDOOR);
               buildingBlocks.accept(Items.OAK_PRESSURE_PLATE);
               buildingBlocks.accept(Items.OAK_BUTTON);
               buildingBlocks.accept(Items.SPRUCE_LOG);
               buildingBlocks.accept(Items.SPRUCE_WOOD);
               buildingBlocks.accept(Items.STRIPPED_SPRUCE_LOG);
               buildingBlocks.accept(Items.STRIPPED_SPRUCE_WOOD);
               buildingBlocks.accept(Items.SPRUCE_PLANKS);
               buildingBlocks.accept(Items.SPRUCE_STAIRS);
               buildingBlocks.accept(Items.SPRUCE_SLAB);
               buildingBlocks.accept(Items.SPRUCE_FENCE);
               buildingBlocks.accept(Items.SPRUCE_FENCE_GATE);
               buildingBlocks.accept(Items.SPRUCE_DOOR);
               buildingBlocks.accept(Items.SPRUCE_TRAPDOOR);
               buildingBlocks.accept(Items.SPRUCE_PRESSURE_PLATE);
               buildingBlocks.accept(Items.SPRUCE_BUTTON);
               buildingBlocks.accept(Items.BIRCH_LOG);
               buildingBlocks.accept(Items.BIRCH_WOOD);
               buildingBlocks.accept(Items.STRIPPED_BIRCH_LOG);
               buildingBlocks.accept(Items.STRIPPED_BIRCH_WOOD);
               buildingBlocks.accept(Items.BIRCH_PLANKS);
               buildingBlocks.accept(Items.BIRCH_STAIRS);
               buildingBlocks.accept(Items.BIRCH_SLAB);
               buildingBlocks.accept(Items.BIRCH_FENCE);
               buildingBlocks.accept(Items.BIRCH_FENCE_GATE);
               buildingBlocks.accept(Items.BIRCH_DOOR);
               buildingBlocks.accept(Items.BIRCH_TRAPDOOR);
               buildingBlocks.accept(Items.BIRCH_PRESSURE_PLATE);
               buildingBlocks.accept(Items.BIRCH_BUTTON);
               buildingBlocks.accept(Items.JUNGLE_LOG);
               buildingBlocks.accept(Items.JUNGLE_WOOD);
               buildingBlocks.accept(Items.STRIPPED_JUNGLE_LOG);
               buildingBlocks.accept(Items.STRIPPED_JUNGLE_WOOD);
               buildingBlocks.accept(Items.JUNGLE_PLANKS);
               buildingBlocks.accept(Items.JUNGLE_STAIRS);
               buildingBlocks.accept(Items.JUNGLE_SLAB);
               buildingBlocks.accept(Items.JUNGLE_FENCE);
               buildingBlocks.accept(Items.JUNGLE_FENCE_GATE);
               buildingBlocks.accept(Items.JUNGLE_DOOR);
               buildingBlocks.accept(Items.JUNGLE_TRAPDOOR);
               buildingBlocks.accept(Items.JUNGLE_PRESSURE_PLATE);
               buildingBlocks.accept(Items.JUNGLE_BUTTON);
               buildingBlocks.accept(Items.ACACIA_LOG);
               buildingBlocks.accept(Items.ACACIA_WOOD);
               buildingBlocks.accept(Items.STRIPPED_ACACIA_LOG);
               buildingBlocks.accept(Items.STRIPPED_ACACIA_WOOD);
               buildingBlocks.accept(Items.ACACIA_PLANKS);
               buildingBlocks.accept(Items.ACACIA_STAIRS);
               buildingBlocks.accept(Items.ACACIA_SLAB);
               buildingBlocks.accept(Items.ACACIA_FENCE);
               buildingBlocks.accept(Items.ACACIA_FENCE_GATE);
               buildingBlocks.accept(Items.ACACIA_DOOR);
               buildingBlocks.accept(Items.ACACIA_TRAPDOOR);
               buildingBlocks.accept(Items.ACACIA_PRESSURE_PLATE);
               buildingBlocks.accept(Items.ACACIA_BUTTON);
               buildingBlocks.accept(Items.DARK_OAK_LOG);
               buildingBlocks.accept(Items.DARK_OAK_WOOD);
               buildingBlocks.accept(Items.STRIPPED_DARK_OAK_LOG);
               buildingBlocks.accept(Items.STRIPPED_DARK_OAK_WOOD);
               buildingBlocks.accept(Items.DARK_OAK_PLANKS);
               buildingBlocks.accept(Items.DARK_OAK_STAIRS);
               buildingBlocks.accept(Items.DARK_OAK_SLAB);
               buildingBlocks.accept(Items.DARK_OAK_FENCE);
               buildingBlocks.accept(Items.DARK_OAK_FENCE_GATE);
               buildingBlocks.accept(Items.DARK_OAK_DOOR);
               buildingBlocks.accept(Items.DARK_OAK_TRAPDOOR);
               buildingBlocks.accept(Items.DARK_OAK_PRESSURE_PLATE);
               buildingBlocks.accept(Items.DARK_OAK_BUTTON);
               buildingBlocks.accept(Items.MANGROVE_LOG);
               buildingBlocks.accept(Items.MANGROVE_WOOD);
               buildingBlocks.accept(Items.STRIPPED_MANGROVE_LOG);
               buildingBlocks.accept(Items.STRIPPED_MANGROVE_WOOD);
               buildingBlocks.accept(Items.MANGROVE_PLANKS);
               buildingBlocks.accept(Items.MANGROVE_STAIRS);
               buildingBlocks.accept(Items.MANGROVE_SLAB);
               buildingBlocks.accept(Items.MANGROVE_FENCE);
               buildingBlocks.accept(Items.MANGROVE_FENCE_GATE);
               buildingBlocks.accept(Items.MANGROVE_DOOR);
               buildingBlocks.accept(Items.MANGROVE_TRAPDOOR);
               buildingBlocks.accept(Items.MANGROVE_PRESSURE_PLATE);
               buildingBlocks.accept(Items.MANGROVE_BUTTON);
               buildingBlocks.accept(Items.CHERRY_LOG);
               buildingBlocks.accept(Items.CHERRY_WOOD);
               buildingBlocks.accept(Items.STRIPPED_CHERRY_LOG);
               buildingBlocks.accept(Items.STRIPPED_CHERRY_WOOD);
               buildingBlocks.accept(Items.CHERRY_PLANKS);
               buildingBlocks.accept(Items.CHERRY_STAIRS);
               buildingBlocks.accept(Items.CHERRY_SLAB);
               buildingBlocks.accept(Items.CHERRY_FENCE);
               buildingBlocks.accept(Items.CHERRY_FENCE_GATE);
               buildingBlocks.accept(Items.CHERRY_DOOR);
               buildingBlocks.accept(Items.CHERRY_TRAPDOOR);
               buildingBlocks.accept(Items.CHERRY_PRESSURE_PLATE);
               buildingBlocks.accept(Items.CHERRY_BUTTON);
               buildingBlocks.accept(Items.PALE_OAK_LOG);
               buildingBlocks.accept(Items.PALE_OAK_WOOD);
               buildingBlocks.accept(Items.STRIPPED_PALE_OAK_LOG);
               buildingBlocks.accept(Items.STRIPPED_PALE_OAK_WOOD);
               buildingBlocks.accept(Items.PALE_OAK_PLANKS);
               buildingBlocks.accept(Items.PALE_OAK_STAIRS);
               buildingBlocks.accept(Items.PALE_OAK_SLAB);
               buildingBlocks.accept(Items.PALE_OAK_FENCE);
               buildingBlocks.accept(Items.PALE_OAK_FENCE_GATE);
               buildingBlocks.accept(Items.PALE_OAK_DOOR);
               buildingBlocks.accept(Items.PALE_OAK_TRAPDOOR);
               buildingBlocks.accept(Items.PALE_OAK_PRESSURE_PLATE);
               buildingBlocks.accept(Items.PALE_OAK_BUTTON);
               buildingBlocks.accept(Items.POPLAR_LOG);
               buildingBlocks.accept(Items.POPLAR_WOOD);
               buildingBlocks.accept(Items.STRIPPED_POPLAR_LOG);
               buildingBlocks.accept(Items.STRIPPED_POPLAR_WOOD);
               buildingBlocks.accept(Items.POPLAR_PLANKS);
               buildingBlocks.accept(Items.POPLAR_STAIRS);
               buildingBlocks.accept(Items.POPLAR_SLAB);
               buildingBlocks.accept(Items.POPLAR_FENCE);
               buildingBlocks.accept(Items.POPLAR_FENCE_GATE);
               buildingBlocks.accept(Items.POPLAR_DOOR);
               buildingBlocks.accept(Items.POPLAR_TRAPDOOR);
               buildingBlocks.accept(Items.POPLAR_PRESSURE_PLATE);
               buildingBlocks.accept(Items.POPLAR_BUTTON);
               buildingBlocks.accept(Items.BAMBOO_BLOCK);
               buildingBlocks.accept(Items.STRIPPED_BAMBOO_BLOCK);
               buildingBlocks.accept(Items.BAMBOO_PLANKS);
               buildingBlocks.accept(Items.BAMBOO_MOSAIC);
               buildingBlocks.accept(Items.BAMBOO_STAIRS);
               buildingBlocks.accept(Items.BAMBOO_MOSAIC_STAIRS);
               buildingBlocks.accept(Items.BAMBOO_SLAB);
               buildingBlocks.accept(Items.BAMBOO_MOSAIC_SLAB);
               buildingBlocks.accept(Items.BAMBOO_FENCE);
               buildingBlocks.accept(Items.BAMBOO_FENCE_GATE);
               buildingBlocks.accept(Items.BAMBOO_DOOR);
               buildingBlocks.accept(Items.BAMBOO_TRAPDOOR);
               buildingBlocks.accept(Items.BAMBOO_PRESSURE_PLATE);
               buildingBlocks.accept(Items.BAMBOO_BUTTON);
               buildingBlocks.accept(Items.CRIMSON_STEM);
               buildingBlocks.accept(Items.CRIMSON_HYPHAE);
               buildingBlocks.accept(Items.STRIPPED_CRIMSON_STEM);
               buildingBlocks.accept(Items.STRIPPED_CRIMSON_HYPHAE);
               buildingBlocks.accept(Items.CRIMSON_PLANKS);
               buildingBlocks.accept(Items.CRIMSON_STAIRS);
               buildingBlocks.accept(Items.CRIMSON_SLAB);
               buildingBlocks.accept(Items.CRIMSON_FENCE);
               buildingBlocks.accept(Items.CRIMSON_FENCE_GATE);
               buildingBlocks.accept(Items.CRIMSON_DOOR);
               buildingBlocks.accept(Items.CRIMSON_TRAPDOOR);
               buildingBlocks.accept(Items.CRIMSON_PRESSURE_PLATE);
               buildingBlocks.accept(Items.CRIMSON_BUTTON);
               buildingBlocks.accept(Items.WARPED_STEM);
               buildingBlocks.accept(Items.WARPED_HYPHAE);
               buildingBlocks.accept(Items.STRIPPED_WARPED_STEM);
               buildingBlocks.accept(Items.STRIPPED_WARPED_HYPHAE);
               buildingBlocks.accept(Items.WARPED_PLANKS);
               buildingBlocks.accept(Items.WARPED_STAIRS);
               buildingBlocks.accept(Items.WARPED_SLAB);
               buildingBlocks.accept(Items.WARPED_FENCE);
               buildingBlocks.accept(Items.WARPED_FENCE_GATE);
               buildingBlocks.accept(Items.WARPED_DOOR);
               buildingBlocks.accept(Items.WARPED_TRAPDOOR);
               buildingBlocks.accept(Items.WARPED_PRESSURE_PLATE);
               buildingBlocks.accept(Items.WARPED_BUTTON);
               buildingBlocks.accept(Items.STONE);
               buildingBlocks.accept(Items.STONE_STAIRS);
               buildingBlocks.accept(Items.STONE_SLAB);
               buildingBlocks.accept(Items.STONE_PRESSURE_PLATE);
               buildingBlocks.accept(Items.STONE_BUTTON);
               buildingBlocks.accept(Items.COBBLESTONE);
               buildingBlocks.accept(Items.COBBLESTONE_STAIRS);
               buildingBlocks.accept(Items.COBBLESTONE_SLAB);
               buildingBlocks.accept(Items.COBBLESTONE_WALL);
               buildingBlocks.accept(Items.MOSSY_COBBLESTONE);
               buildingBlocks.accept(Items.MOSSY_COBBLESTONE_STAIRS);
               buildingBlocks.accept(Items.MOSSY_COBBLESTONE_SLAB);
               buildingBlocks.accept(Items.MOSSY_COBBLESTONE_WALL);
               buildingBlocks.accept(Items.SMOOTH_STONE);
               buildingBlocks.accept(Items.SMOOTH_STONE_SLAB);
               buildingBlocks.accept(Items.STONE_BRICKS);
               buildingBlocks.accept(Items.CRACKED_STONE_BRICKS);
               buildingBlocks.accept(Items.STONE_BRICK_STAIRS);
               buildingBlocks.accept(Items.STONE_BRICK_SLAB);
               buildingBlocks.accept(Items.STONE_BRICK_WALL);
               buildingBlocks.accept(Items.CHISELED_STONE_BRICKS);
               buildingBlocks.accept(Items.MOSSY_STONE_BRICKS);
               buildingBlocks.accept(Items.MOSSY_STONE_BRICK_STAIRS);
               buildingBlocks.accept(Items.MOSSY_STONE_BRICK_SLAB);
               buildingBlocks.accept(Items.MOSSY_STONE_BRICK_WALL);
               buildingBlocks.accept(Items.GRANITE);
               buildingBlocks.accept(Items.GRANITE_STAIRS);
               buildingBlocks.accept(Items.GRANITE_SLAB);
               buildingBlocks.accept(Items.GRANITE_WALL);
               buildingBlocks.accept(Items.POLISHED_GRANITE);
               buildingBlocks.accept(Items.POLISHED_GRANITE_STAIRS);
               buildingBlocks.accept(Items.POLISHED_GRANITE_SLAB);
               buildingBlocks.accept(Items.DIORITE);
               buildingBlocks.accept(Items.DIORITE_STAIRS);
               buildingBlocks.accept(Items.DIORITE_SLAB);
               buildingBlocks.accept(Items.DIORITE_WALL);
               buildingBlocks.accept(Items.POLISHED_DIORITE);
               buildingBlocks.accept(Items.POLISHED_DIORITE_STAIRS);
               buildingBlocks.accept(Items.POLISHED_DIORITE_SLAB);
               buildingBlocks.accept(Items.ANDESITE);
               buildingBlocks.accept(Items.ANDESITE_STAIRS);
               buildingBlocks.accept(Items.ANDESITE_SLAB);
               buildingBlocks.accept(Items.ANDESITE_WALL);
               buildingBlocks.accept(Items.POLISHED_ANDESITE);
               buildingBlocks.accept(Items.POLISHED_ANDESITE_STAIRS);
               buildingBlocks.accept(Items.POLISHED_ANDESITE_SLAB);
               buildingBlocks.accept(Items.DEEPSLATE);
               buildingBlocks.accept(Items.COBBLED_DEEPSLATE);
               buildingBlocks.accept(Items.COBBLED_DEEPSLATE_STAIRS);
               buildingBlocks.accept(Items.COBBLED_DEEPSLATE_SLAB);
               buildingBlocks.accept(Items.COBBLED_DEEPSLATE_WALL);
               buildingBlocks.accept(Items.CHISELED_DEEPSLATE);
               buildingBlocks.accept(Items.POLISHED_DEEPSLATE);
               buildingBlocks.accept(Items.POLISHED_DEEPSLATE_STAIRS);
               buildingBlocks.accept(Items.POLISHED_DEEPSLATE_SLAB);
               buildingBlocks.accept(Items.POLISHED_DEEPSLATE_WALL);
               buildingBlocks.accept(Items.DEEPSLATE_BRICKS);
               buildingBlocks.accept(Items.CRACKED_DEEPSLATE_BRICKS);
               buildingBlocks.accept(Items.DEEPSLATE_BRICK_STAIRS);
               buildingBlocks.accept(Items.DEEPSLATE_BRICK_SLAB);
               buildingBlocks.accept(Items.DEEPSLATE_BRICK_WALL);
               buildingBlocks.accept(Items.DEEPSLATE_TILES);
               buildingBlocks.accept(Items.CRACKED_DEEPSLATE_TILES);
               buildingBlocks.accept(Items.DEEPSLATE_TILE_STAIRS);
               buildingBlocks.accept(Items.DEEPSLATE_TILE_SLAB);
               buildingBlocks.accept(Items.DEEPSLATE_TILE_WALL);
               buildingBlocks.accept(Items.REINFORCED_DEEPSLATE);
               buildingBlocks.accept(Items.TUFF);
               buildingBlocks.accept(Items.TUFF_STAIRS);
               buildingBlocks.accept(Items.TUFF_SLAB);
               buildingBlocks.accept(Items.TUFF_WALL);
               buildingBlocks.accept(Items.CHISELED_TUFF);
               buildingBlocks.accept(Items.POLISHED_TUFF);
               buildingBlocks.accept(Items.POLISHED_TUFF_STAIRS);
               buildingBlocks.accept(Items.POLISHED_TUFF_SLAB);
               buildingBlocks.accept(Items.POLISHED_TUFF_WALL);
               buildingBlocks.accept(Items.TUFF_BRICKS);
               buildingBlocks.accept(Items.TUFF_BRICK_STAIRS);
               buildingBlocks.accept(Items.TUFF_BRICK_SLAB);
               buildingBlocks.accept(Items.TUFF_BRICK_WALL);
               buildingBlocks.accept(Items.CHISELED_TUFF_BRICKS);
               buildingBlocks.accept(Items.BRICKS);
               buildingBlocks.accept(Items.BRICK_STAIRS);
               buildingBlocks.accept(Items.BRICK_SLAB);
               buildingBlocks.accept(Items.BRICK_WALL);
               buildingBlocks.accept(Items.PACKED_MUD);
               buildingBlocks.accept(Items.MUD_BRICKS);
               buildingBlocks.accept(Items.MUD_BRICK_STAIRS);
               buildingBlocks.accept(Items.MUD_BRICK_SLAB);
               buildingBlocks.accept(Items.MUD_BRICK_WALL);
               buildingBlocks.accept(Items.RESIN_BRICKS);
               buildingBlocks.accept(Items.RESIN_BRICK_STAIRS);
               buildingBlocks.accept(Items.RESIN_BRICK_SLAB);
               buildingBlocks.accept(Items.RESIN_BRICK_WALL);
               buildingBlocks.accept(Items.CHISELED_RESIN_BRICKS);
               buildingBlocks.accept(Items.SANDSTONE);
               buildingBlocks.accept(Items.SANDSTONE_STAIRS);
               buildingBlocks.accept(Items.SANDSTONE_SLAB);
               buildingBlocks.accept(Items.SANDSTONE_WALL);
               buildingBlocks.accept(Items.CHISELED_SANDSTONE);
               buildingBlocks.accept(Items.SMOOTH_SANDSTONE);
               buildingBlocks.accept(Items.SMOOTH_SANDSTONE_STAIRS);
               buildingBlocks.accept(Items.SMOOTH_SANDSTONE_SLAB);
               buildingBlocks.accept(Items.CUT_SANDSTONE);
               buildingBlocks.accept(Items.CUT_STANDSTONE_SLAB);
               buildingBlocks.accept(Items.RED_SANDSTONE);
               buildingBlocks.accept(Items.RED_SANDSTONE_STAIRS);
               buildingBlocks.accept(Items.RED_SANDSTONE_SLAB);
               buildingBlocks.accept(Items.RED_SANDSTONE_WALL);
               buildingBlocks.accept(Items.CHISELED_RED_SANDSTONE);
               buildingBlocks.accept(Items.SMOOTH_RED_SANDSTONE);
               buildingBlocks.accept(Items.SMOOTH_RED_SANDSTONE_STAIRS);
               buildingBlocks.accept(Items.SMOOTH_RED_SANDSTONE_SLAB);
               buildingBlocks.accept(Items.CUT_RED_SANDSTONE);
               buildingBlocks.accept(Items.CUT_RED_SANDSTONE_SLAB);
               buildingBlocks.accept(Items.CINNABAR);
               buildingBlocks.accept(Items.CINNABAR_STAIRS);
               buildingBlocks.accept(Items.CINNABAR_SLAB);
               buildingBlocks.accept(Items.CINNABAR_WALL);
               buildingBlocks.accept(Items.CHISELED_CINNABAR);
               buildingBlocks.accept(Items.POLISHED_CINNABAR);
               buildingBlocks.accept(Items.POLISHED_CINNABAR_STAIRS);
               buildingBlocks.accept(Items.POLISHED_CINNABAR_SLAB);
               buildingBlocks.accept(Items.POLISHED_CINNABAR_WALL);
               buildingBlocks.accept(Items.CINNABAR_BRICKS);
               buildingBlocks.accept(Items.CINNABAR_BRICK_STAIRS);
               buildingBlocks.accept(Items.CINNABAR_BRICK_SLAB);
               buildingBlocks.accept(Items.CINNABAR_BRICK_WALL);
               buildingBlocks.accept(Items.SULFUR);
               buildingBlocks.accept(Items.SULFUR_STAIRS);
               buildingBlocks.accept(Items.SULFUR_SLAB);
               buildingBlocks.accept(Items.SULFUR_WALL);
               buildingBlocks.accept(Items.CHISELED_SULFUR);
               buildingBlocks.accept(Items.POLISHED_SULFUR);
               buildingBlocks.accept(Items.POLISHED_SULFUR_STAIRS);
               buildingBlocks.accept(Items.POLISHED_SULFUR_SLAB);
               buildingBlocks.accept(Items.POLISHED_SULFUR_WALL);
               buildingBlocks.accept(Items.SULFUR_BRICKS);
               buildingBlocks.accept(Items.SULFUR_BRICK_STAIRS);
               buildingBlocks.accept(Items.SULFUR_BRICK_SLAB);
               buildingBlocks.accept(Items.SULFUR_BRICK_WALL);
               buildingBlocks.accept(Items.SEA_LANTERN);
               buildingBlocks.accept(Items.PRISMARINE);
               buildingBlocks.accept(Items.PRISMARINE_STAIRS);
               buildingBlocks.accept(Items.PRISMARINE_SLAB);
               buildingBlocks.accept(Items.PRISMARINE_WALL);
               buildingBlocks.accept(Items.PRISMARINE_BRICKS);
               buildingBlocks.accept(Items.PRISMARINE_BRICK_STAIRS);
               buildingBlocks.accept(Items.PRISMARINE_BRICK_SLAB);
               buildingBlocks.accept(Items.DARK_PRISMARINE);
               buildingBlocks.accept(Items.DARK_PRISMARINE_STAIRS);
               buildingBlocks.accept(Items.DARK_PRISMARINE_SLAB);
               buildingBlocks.accept(Items.NETHERRACK);
               buildingBlocks.accept(Items.NETHER_BRICKS);
               buildingBlocks.accept(Items.CRACKED_NETHER_BRICKS);
               buildingBlocks.accept(Items.NETHER_BRICK_STAIRS);
               buildingBlocks.accept(Items.NETHER_BRICK_SLAB);
               buildingBlocks.accept(Items.NETHER_BRICK_WALL);
               buildingBlocks.accept(Items.NETHER_BRICK_FENCE);
               buildingBlocks.accept(Items.CHISELED_NETHER_BRICKS);
               buildingBlocks.accept(Items.RED_NETHER_BRICKS);
               buildingBlocks.accept(Items.RED_NETHER_BRICK_STAIRS);
               buildingBlocks.accept(Items.RED_NETHER_BRICK_SLAB);
               buildingBlocks.accept(Items.RED_NETHER_BRICK_WALL);
               buildingBlocks.accept(Items.BASALT);
               buildingBlocks.accept(Items.SMOOTH_BASALT);
               buildingBlocks.accept(Items.POLISHED_BASALT);
               buildingBlocks.accept(Items.BLACKSTONE);
               buildingBlocks.accept(Items.GILDED_BLACKSTONE);
               buildingBlocks.accept(Items.BLACKSTONE_STAIRS);
               buildingBlocks.accept(Items.BLACKSTONE_SLAB);
               buildingBlocks.accept(Items.BLACKSTONE_WALL);
               buildingBlocks.accept(Items.CHISELED_POLISHED_BLACKSTONE);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_STAIRS);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_SLAB);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_WALL);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_PRESSURE_PLATE);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_BUTTON);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_BRICKS);
               buildingBlocks.accept(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_BRICK_STAIRS);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_BRICK_SLAB);
               buildingBlocks.accept(Items.POLISHED_BLACKSTONE_BRICK_WALL);
               buildingBlocks.accept(Items.END_STONE);
               buildingBlocks.accept(Items.END_STONE_BRICKS);
               buildingBlocks.accept(Items.END_STONE_BRICK_STAIRS);
               buildingBlocks.accept(Items.END_STONE_BRICK_SLAB);
               buildingBlocks.accept(Items.END_STONE_BRICK_WALL);
               buildingBlocks.accept(Items.PURPUR_BLOCK);
               buildingBlocks.accept(Items.PURPUR_PILLAR);
               buildingBlocks.accept(Items.PURPUR_STAIRS);
               buildingBlocks.accept(Items.PURPUR_SLAB);
               buildingBlocks.accept(Items.COAL_BLOCK);
               buildingBlocks.accept(Items.IRON_BLOCK);
               buildingBlocks.accept(Items.IRON_BARS);
               buildingBlocks.accept(Items.IRON_DOOR);
               buildingBlocks.accept(Items.IRON_TRAPDOOR);
               buildingBlocks.accept(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
               buildingBlocks.accept(Items.IRON_CHAIN);
               buildingBlocks.accept(Items.GOLD_BLOCK);
               buildingBlocks.accept(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
               buildingBlocks.accept(Items.REDSTONE_BLOCK);
               buildingBlocks.accept(Items.EMERALD_BLOCK);
               buildingBlocks.accept(Items.LAPIS_BLOCK);
               buildingBlocks.accept(Items.DIAMOND_BLOCK);
               buildingBlocks.accept(Items.NETHERITE_BLOCK);
               buildingBlocks.accept(Items.QUARTZ_BLOCK);
               buildingBlocks.accept(Items.QUARTZ_STAIRS);
               buildingBlocks.accept(Items.QUARTZ_SLAB);
               buildingBlocks.accept(Items.CHISELED_QUARTZ_BLOCK);
               buildingBlocks.accept(Items.QUARTZ_BRICKS);
               buildingBlocks.accept(Items.QUARTZ_PILLAR);
               buildingBlocks.accept(Items.SMOOTH_QUARTZ);
               buildingBlocks.accept(Items.SMOOTH_QUARTZ_STAIRS);
               buildingBlocks.accept(Items.SMOOTH_QUARTZ_SLAB);
               buildingBlocks.accept(Items.AMETHYST_BLOCK);
               copperBlockFamilies(family -> family.weathering().forEach(buildingBlocks::accept));
               copperBlockFamilies(family -> family.waxed().forEach(buildingBlocks::accept));
            })
            .build()
      );
      List<DyeColor> gameplayColorOrder = List.of(
         DyeColor.WHITE,
         DyeColor.LIGHT_GRAY,
         DyeColor.GRAY,
         DyeColor.BLACK,
         DyeColor.BROWN,
         DyeColor.RED,
         DyeColor.ORANGE,
         DyeColor.YELLOW,
         DyeColor.LIME,
         DyeColor.GREEN,
         DyeColor.CYAN,
         DyeColor.LIGHT_BLUE,
         DyeColor.BLUE,
         DyeColor.PURPLE,
         DyeColor.MAGENTA,
         DyeColor.PINK
      );
      Registry.register(
         registry,
         COLORED_BLOCKS,
         CreativeModeTab.builder(CreativeModeTab.Row.TOP, 1)
            .title(Component.translatable("itemGroup.coloredBlocks"))
            .icon(() -> new ItemStack(Blocks.WOOL.cyan()))
            .displayItems((parameters, coloredBlocks) -> {
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.WOOL);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.WOOL_STAIRS);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.WOOL_SLAB);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.CARPET);
               coloredBlocks.accept(Items.TERRACOTTA);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.DYED_TERRACOTTA);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.CONCRETE);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.CONCRETE_POWDER);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.GLAZED_TERRACOTTA);
               coloredBlocks.accept(Items.GLASS);
               coloredBlocks.accept(Items.TINTED_GLASS);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.STAINED_GLASS);
               coloredBlocks.accept(Items.GLASS_PANE);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.STAINED_GLASS_PANE);
               coloredBlocks.accept(Items.SHULKER_BOX);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.DYED_SHULKER_BOX);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.BED);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.CUSHION);
               coloredBlocks.accept(Items.CANDLE);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.DYED_CANDLE);
               registerColoredItems(coloredBlocks, gameplayColorOrder, Items.BANNER);
            })
            .build()
      );
      Registry.register(
         registry,
         NATURAL_BLOCKS,
         CreativeModeTab.builder(CreativeModeTab.Row.TOP, 2)
            .title(Component.translatable("itemGroup.natural"))
            .icon(() -> new ItemStack(Blocks.GRASS_BLOCK))
            .displayItems((parameters, naturalBlocks) -> {
               naturalBlocks.accept(Items.GRASS_BLOCK);
               naturalBlocks.accept(Items.PODZOL);
               naturalBlocks.accept(Items.MYCELIUM);
               naturalBlocks.accept(Items.DIRT_PATH);
               naturalBlocks.accept(Items.DIRT);
               naturalBlocks.accept(Items.COARSE_DIRT);
               naturalBlocks.accept(Items.ROOTED_DIRT);
               naturalBlocks.accept(Items.FARMLAND);
               naturalBlocks.accept(Items.MUD);
               naturalBlocks.accept(Items.CLAY);
               naturalBlocks.accept(Items.GRAVEL);
               naturalBlocks.accept(Items.SAND);
               naturalBlocks.accept(Items.SANDSTONE);
               naturalBlocks.accept(Items.RED_SAND);
               naturalBlocks.accept(Items.RED_SANDSTONE);
               naturalBlocks.accept(Items.ICE);
               naturalBlocks.accept(Items.PACKED_ICE);
               naturalBlocks.accept(Items.BLUE_ICE);
               naturalBlocks.accept(Items.SNOW_BLOCK);
               naturalBlocks.accept(Items.SNOW);
               naturalBlocks.accept(Items.MOSS_BLOCK);
               naturalBlocks.accept(Items.MOSS_CARPET);
               naturalBlocks.accept(Items.PALE_MOSS_BLOCK);
               naturalBlocks.accept(Items.PALE_MOSS_CARPET);
               naturalBlocks.accept(Items.PALE_HANGING_MOSS);
               naturalBlocks.accept(Items.STONE);
               naturalBlocks.accept(Items.DEEPSLATE);
               naturalBlocks.accept(Items.GRANITE);
               naturalBlocks.accept(Items.DIORITE);
               naturalBlocks.accept(Items.ANDESITE);
               naturalBlocks.accept(Items.CALCITE);
               naturalBlocks.accept(Items.TUFF);
               naturalBlocks.accept(Items.DRIPSTONE_BLOCK);
               naturalBlocks.accept(Items.POINTED_DRIPSTONE);
               naturalBlocks.accept(Items.PRISMARINE);
               naturalBlocks.accept(Items.CINNABAR);
               naturalBlocks.accept(Items.SULFUR);
               naturalBlocks.accept(Items.SULFUR_SPIKE);
               naturalBlocks.accept(Items.POTENT_SULFUR);
               naturalBlocks.accept(Items.MAGMA_BLOCK);
               naturalBlocks.accept(Items.OBSIDIAN);
               naturalBlocks.accept(Items.CRYING_OBSIDIAN);
               naturalBlocks.accept(Items.NETHERRACK);
               naturalBlocks.accept(Items.CRIMSON_NYLIUM);
               naturalBlocks.accept(Items.WARPED_NYLIUM);
               naturalBlocks.accept(Items.SOUL_SAND);
               naturalBlocks.accept(Items.SOUL_SOIL);
               naturalBlocks.accept(Items.BONE_BLOCK);
               naturalBlocks.accept(Items.BLACKSTONE);
               naturalBlocks.accept(Items.BASALT);
               naturalBlocks.accept(Items.SMOOTH_BASALT);
               naturalBlocks.accept(Items.END_STONE);
               naturalBlocks.accept(Items.COAL_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_COAL_ORE);
               naturalBlocks.accept(Items.IRON_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_IRON_ORE);
               naturalBlocks.accept(Items.COPPER_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_COPPER_ORE);
               naturalBlocks.accept(Items.GOLD_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_GOLD_ORE);
               naturalBlocks.accept(Items.REDSTONE_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_REDSTONE_ORE);
               naturalBlocks.accept(Items.EMERALD_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_EMERALD_ORE);
               naturalBlocks.accept(Items.LAPIS_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_LAPIS_ORE);
               naturalBlocks.accept(Items.DIAMOND_ORE);
               naturalBlocks.accept(Items.DEEPSLATE_DIAMOND_ORE);
               naturalBlocks.accept(Items.NETHER_GOLD_ORE);
               naturalBlocks.accept(Items.NETHER_QUARTZ_ORE);
               naturalBlocks.accept(Items.ANCIENT_DEBRIS);
               naturalBlocks.accept(Items.RAW_IRON_BLOCK);
               naturalBlocks.accept(Items.RAW_COPPER_BLOCK);
               naturalBlocks.accept(Items.RAW_GOLD_BLOCK);
               naturalBlocks.accept(Items.GLOWSTONE);
               naturalBlocks.accept(Items.AMETHYST_BLOCK);
               naturalBlocks.accept(Items.BUDDING_AMETHYST);
               naturalBlocks.accept(Items.SMALL_AMETHYST_BUD);
               naturalBlocks.accept(Items.MEDIUM_AMETHYST_BUD);
               naturalBlocks.accept(Items.LARGE_AMETHYST_BUD);
               naturalBlocks.accept(Items.AMETHYST_CLUSTER);
               naturalBlocks.accept(Items.OAK_LOG);
               naturalBlocks.accept(Items.SPRUCE_LOG);
               naturalBlocks.accept(Items.BIRCH_LOG);
               naturalBlocks.accept(Items.JUNGLE_LOG);
               naturalBlocks.accept(Items.ACACIA_LOG);
               naturalBlocks.accept(Items.DARK_OAK_LOG);
               naturalBlocks.accept(Items.MANGROVE_LOG);
               naturalBlocks.accept(Items.MANGROVE_ROOTS);
               naturalBlocks.accept(Items.MUDDY_MANGROVE_ROOTS);
               naturalBlocks.accept(Items.CHERRY_LOG);
               naturalBlocks.accept(Items.PALE_OAK_LOG);
               naturalBlocks.accept(Items.POPLAR_LOG);
               naturalBlocks.accept(Items.MUSHROOM_STEM);
               naturalBlocks.accept(Items.CRIMSON_STEM);
               naturalBlocks.accept(Items.WARPED_STEM);
               naturalBlocks.accept(Items.OAK_LEAVES);
               naturalBlocks.accept(Items.SPRUCE_LEAVES);
               naturalBlocks.accept(Items.BIRCH_LEAVES);
               naturalBlocks.accept(Items.JUNGLE_LEAVES);
               naturalBlocks.accept(Items.ACACIA_LEAVES);
               naturalBlocks.accept(Items.DARK_OAK_LEAVES);
               naturalBlocks.accept(Items.MANGROVE_LEAVES);
               naturalBlocks.accept(Items.CHERRY_LEAVES);
               naturalBlocks.accept(Items.PALE_OAK_LEAVES);
               naturalBlocks.accept(Items.RED_POPLAR_LEAVES);
               naturalBlocks.accept(Items.ORANGE_POPLAR_LEAVES);
               naturalBlocks.accept(Items.YELLOW_POPLAR_LEAVES);
               naturalBlocks.accept(Items.AZALEA_LEAVES);
               naturalBlocks.accept(Items.FLOWERING_AZALEA_LEAVES);
               naturalBlocks.accept(Items.BROWN_MUSHROOM_BLOCK);
               naturalBlocks.accept(Items.RED_MUSHROOM_BLOCK);
               naturalBlocks.accept(Items.NETHER_WART_BLOCK);
               naturalBlocks.accept(Items.WARPED_WART_BLOCK);
               naturalBlocks.accept(Items.SHROOMLIGHT);
               naturalBlocks.accept(Items.OAK_SAPLING);
               naturalBlocks.accept(Items.SPRUCE_SAPLING);
               naturalBlocks.accept(Items.BIRCH_SAPLING);
               naturalBlocks.accept(Items.JUNGLE_SAPLING);
               naturalBlocks.accept(Items.ACACIA_SAPLING);
               naturalBlocks.accept(Items.DARK_OAK_SAPLING);
               naturalBlocks.accept(Items.MANGROVE_PROPAGULE);
               naturalBlocks.accept(Items.CHERRY_SAPLING);
               naturalBlocks.accept(Items.PALE_OAK_SAPLING);
               naturalBlocks.accept(Items.POPLAR_SAPLING);
               naturalBlocks.accept(Items.AZALEA);
               naturalBlocks.accept(Items.FLOWERING_AZALEA);
               naturalBlocks.accept(Items.BROWN_MUSHROOM);
               naturalBlocks.accept(Items.RED_MUSHROOM);
               naturalBlocks.accept(Items.SHELF_MUSHROOM);
               naturalBlocks.accept(Items.CRIMSON_FUNGUS);
               naturalBlocks.accept(Items.WARPED_FUNGUS);
               naturalBlocks.accept(Items.SHORT_GRASS);
               naturalBlocks.accept(Items.FERN);
               naturalBlocks.accept(Items.DRY_SHORT_GRASS);
               naturalBlocks.accept(Items.BUSH);
               naturalBlocks.accept(Items.RED_SHRUB);
               naturalBlocks.accept(Items.DEAD_BUSH);
               naturalBlocks.accept(Items.DANDELION);
               naturalBlocks.accept(Items.POPPY);
               naturalBlocks.accept(Items.BLUE_ORCHID);
               naturalBlocks.accept(Items.ALLIUM);
               naturalBlocks.accept(Items.AZURE_BLUET);
               naturalBlocks.accept(Items.RED_TULIP);
               naturalBlocks.accept(Items.ORANGE_TULIP);
               naturalBlocks.accept(Items.WHITE_TULIP);
               naturalBlocks.accept(Items.PINK_TULIP);
               naturalBlocks.accept(Items.OXEYE_DAISY);
               naturalBlocks.accept(Items.CORNFLOWER);
               naturalBlocks.accept(Items.LILY_OF_THE_VALLEY);
               naturalBlocks.accept(Items.TORCHFLOWER);
               naturalBlocks.accept(Items.CACTUS_FLOWER);
               naturalBlocks.accept(Items.CLOSED_EYEBLOSSOM);
               naturalBlocks.accept(Items.OPEN_EYEBLOSSOM);
               naturalBlocks.accept(Items.WITHER_ROSE);
               naturalBlocks.accept(Items.PINK_PETALS);
               naturalBlocks.accept(Items.WILDFLOWERS);
               naturalBlocks.accept(Items.LEAF_LITTER);
               naturalBlocks.accept(Items.SPORE_BLOSSOM);
               naturalBlocks.accept(Items.FIREFLY_BUSH);
               naturalBlocks.accept(Items.BAMBOO);
               naturalBlocks.accept(Items.SUGAR_CANE);
               naturalBlocks.accept(Items.CACTUS);
               naturalBlocks.accept(Items.CRIMSON_ROOTS);
               naturalBlocks.accept(Items.WARPED_ROOTS);
               naturalBlocks.accept(Items.NETHER_SPROUTS);
               naturalBlocks.accept(Items.WEEPING_VINES);
               naturalBlocks.accept(Items.TWISTING_VINES);
               naturalBlocks.accept(Items.VINE);
               naturalBlocks.accept(Items.TALL_GRASS);
               naturalBlocks.accept(Items.LARGE_FERN);
               naturalBlocks.accept(Items.DRY_TALL_GRASS);
               naturalBlocks.accept(Items.SUNFLOWER);
               naturalBlocks.accept(Items.LILAC);
               naturalBlocks.accept(Items.ROSE_BUSH);
               naturalBlocks.accept(Items.PEONY);
               naturalBlocks.accept(Items.PITCHER_PLANT);
               naturalBlocks.accept(Items.BIG_DRIPLEAF);
               naturalBlocks.accept(Items.SMALL_DRIPLEAF);
               naturalBlocks.accept(Items.CHORUS_PLANT);
               naturalBlocks.accept(Items.CHORUS_FLOWER);
               naturalBlocks.accept(Items.GLOW_LICHEN);
               naturalBlocks.accept(Items.HANGING_ROOTS);
               naturalBlocks.accept(Items.FROGSPAWN);
               naturalBlocks.accept(Items.TURTLE_EGG);
               naturalBlocks.accept(Items.SNIFFER_EGG);
               naturalBlocks.accept(Items.DRIED_GHAST);
               naturalBlocks.accept(Items.WHEAT_SEEDS);
               naturalBlocks.accept(Items.COCOA_BEANS);
               naturalBlocks.accept(Items.PUMPKIN_SEEDS);
               naturalBlocks.accept(Items.MELON_SEEDS);
               naturalBlocks.accept(Items.BEETROOT_SEEDS);
               naturalBlocks.accept(Items.TORCHFLOWER_SEEDS);
               naturalBlocks.accept(Items.PITCHER_POD);
               naturalBlocks.accept(Items.GLOW_BERRIES);
               naturalBlocks.accept(Items.SWEET_BERRIES);
               naturalBlocks.accept(Items.NETHER_WART);
               naturalBlocks.accept(Items.LILY_PAD);
               naturalBlocks.accept(Items.SEAGRASS);
               naturalBlocks.accept(Items.SEA_PICKLE);
               naturalBlocks.accept(Items.KELP);
               naturalBlocks.accept(Items.DRIED_KELP_BLOCK);
               naturalBlocks.accept(Items.TUBE_CORAL_BLOCK);
               naturalBlocks.accept(Items.BRAIN_CORAL_BLOCK);
               naturalBlocks.accept(Items.BUBBLE_CORAL_BLOCK);
               naturalBlocks.accept(Items.FIRE_CORAL_BLOCK);
               naturalBlocks.accept(Items.HORN_CORAL_BLOCK);
               naturalBlocks.accept(Items.DEAD_TUBE_CORAL_BLOCK);
               naturalBlocks.accept(Items.DEAD_BRAIN_CORAL_BLOCK);
               naturalBlocks.accept(Items.DEAD_BUBBLE_CORAL_BLOCK);
               naturalBlocks.accept(Items.DEAD_FIRE_CORAL_BLOCK);
               naturalBlocks.accept(Items.DEAD_HORN_CORAL_BLOCK);
               naturalBlocks.accept(Items.TUBE_CORAL);
               naturalBlocks.accept(Items.BRAIN_CORAL);
               naturalBlocks.accept(Items.BUBBLE_CORAL);
               naturalBlocks.accept(Items.FIRE_CORAL);
               naturalBlocks.accept(Items.HORN_CORAL);
               naturalBlocks.accept(Items.DEAD_TUBE_CORAL);
               naturalBlocks.accept(Items.DEAD_BRAIN_CORAL);
               naturalBlocks.accept(Items.DEAD_BUBBLE_CORAL);
               naturalBlocks.accept(Items.DEAD_FIRE_CORAL);
               naturalBlocks.accept(Items.DEAD_HORN_CORAL);
               naturalBlocks.accept(Items.TUBE_CORAL_FAN);
               naturalBlocks.accept(Items.BRAIN_CORAL_FAN);
               naturalBlocks.accept(Items.BUBBLE_CORAL_FAN);
               naturalBlocks.accept(Items.FIRE_CORAL_FAN);
               naturalBlocks.accept(Items.HORN_CORAL_FAN);
               naturalBlocks.accept(Items.DEAD_TUBE_CORAL_FAN);
               naturalBlocks.accept(Items.DEAD_BRAIN_CORAL_FAN);
               naturalBlocks.accept(Items.DEAD_BUBBLE_CORAL_FAN);
               naturalBlocks.accept(Items.DEAD_FIRE_CORAL_FAN);
               naturalBlocks.accept(Items.DEAD_HORN_CORAL_FAN);
               naturalBlocks.accept(Items.SPONGE);
               naturalBlocks.accept(Items.WET_SPONGE);
               naturalBlocks.accept(Items.MELON);
               naturalBlocks.accept(Items.PUMPKIN);
               naturalBlocks.accept(Items.CARVED_PUMPKIN);
               naturalBlocks.accept(Items.JACK_O_LANTERN);
               naturalBlocks.accept(Items.HAY_BLOCK);
               naturalBlocks.accept(Items.BEE_NEST);
               naturalBlocks.accept(Items.HONEYCOMB_BLOCK);
               naturalBlocks.accept(Items.SLIME_BLOCK);
               naturalBlocks.accept(Items.HONEY_BLOCK);
               naturalBlocks.accept(Items.RESIN_BLOCK);
               naturalBlocks.accept(Items.OCHRE_FROGLIGHT);
               naturalBlocks.accept(Items.VERDANT_FROGLIGHT);
               naturalBlocks.accept(Items.PEARLESCENT_FROGLIGHT);
               naturalBlocks.accept(Items.SCULK);
               naturalBlocks.accept(Items.SCULK_VEIN);
               naturalBlocks.accept(Items.SCULK_CATALYST);
               naturalBlocks.accept(Items.SCULK_SHRIEKER);
               naturalBlocks.accept(Items.SCULK_SENSOR);
               naturalBlocks.accept(Items.COBWEB);
               naturalBlocks.accept(Items.BEDROCK);
            })
            .build()
      );
      Registry.register(
         registry,
         FUNCTIONAL_BLOCKS,
         CreativeModeTab.builder(CreativeModeTab.Row.TOP, 3)
            .title(Component.translatable("itemGroup.functional"))
            .icon(() -> new ItemStack(Items.OAK_SIGN))
            .displayItems(
               (parameters, functionalBlocks) -> {
                  functionalBlocks.accept(Items.TORCH);
                  functionalBlocks.accept(Items.SOUL_TORCH);
                  functionalBlocks.accept(Items.COPPER_TORCH);
                  functionalBlocks.accept(Items.REDSTONE_TORCH);
                  functionalBlocks.accept(Items.LANTERN);
                  functionalBlocks.accept(Items.SOUL_LANTERN);
                  Items.COPPER_LANTERN.forEach(functionalBlocks::accept);
                  functionalBlocks.accept(Items.IRON_CHAIN);
                  Items.COPPER_CHAIN.forEach(functionalBlocks::accept);
                  functionalBlocks.accept(Items.END_ROD);
                  functionalBlocks.accept(Items.SEA_LANTERN);
                  functionalBlocks.accept(Items.REDSTONE_LAMP);
                  Items.COPPER_BULB.forEach(functionalBlocks::accept);
                  functionalBlocks.accept(Items.GLOWSTONE);
                  functionalBlocks.accept(Items.SHROOMLIGHT);
                  functionalBlocks.accept(Items.OCHRE_FROGLIGHT);
                  functionalBlocks.accept(Items.VERDANT_FROGLIGHT);
                  functionalBlocks.accept(Items.PEARLESCENT_FROGLIGHT);
                  functionalBlocks.accept(Items.CRYING_OBSIDIAN);
                  functionalBlocks.accept(Items.GLOW_LICHEN);
                  functionalBlocks.accept(Items.MAGMA_BLOCK);
                  functionalBlocks.accept(Items.CRAFTING_TABLE);
                  functionalBlocks.accept(Items.STONECUTTER);
                  functionalBlocks.accept(Items.CARTOGRAPHY_TABLE);
                  functionalBlocks.accept(Items.FLETCHING_TABLE);
                  functionalBlocks.accept(Items.SMITHING_TABLE);
                  functionalBlocks.accept(Items.GRINDSTONE);
                  functionalBlocks.accept(Items.LOOM);
                  functionalBlocks.accept(Items.FURNACE);
                  functionalBlocks.accept(Items.SMOKER);
                  functionalBlocks.accept(Items.BLAST_FURNACE);
                  functionalBlocks.accept(Items.CAMPFIRE);
                  functionalBlocks.accept(Items.SOUL_CAMPFIRE);
                  functionalBlocks.accept(Items.ANVIL);
                  functionalBlocks.accept(Items.CHIPPED_ANVIL);
                  functionalBlocks.accept(Items.DAMAGED_ANVIL);
                  functionalBlocks.accept(Items.COMPOSTER);
                  functionalBlocks.accept(Items.NOTE_BLOCK);
                  functionalBlocks.accept(Items.JUKEBOX);
                  functionalBlocks.accept(Items.ENCHANTING_TABLE);
                  functionalBlocks.accept(Items.END_CRYSTAL);
                  functionalBlocks.accept(Items.BREWING_STAND);
                  functionalBlocks.accept(Items.CAULDRON);
                  functionalBlocks.accept(Items.BELL);
                  functionalBlocks.accept(Items.BEACON);
                  functionalBlocks.accept(Items.CONDUIT);
                  functionalBlocks.accept(Items.LODESTONE);
                  functionalBlocks.accept(Items.LADDER);
                  functionalBlocks.accept(Items.SCAFFOLDING);
                  functionalBlocks.accept(Items.BEE_NEST);
                  functionalBlocks.accept(Items.BEEHIVE);
                  functionalBlocks.accept(Items.SUSPICIOUS_SAND);
                  functionalBlocks.accept(Items.SUSPICIOUS_GRAVEL);
                  Items.LIGHTNING_ROD.forEach(functionalBlocks::accept);
                  functionalBlocks.accept(Items.FLOWER_POT);
                  functionalBlocks.accept(Items.DECORATED_POT);
                  functionalBlocks.accept(Items.ARMOR_STAND);
                  functionalBlocks.accept(Items.ITEM_FRAME);
                  functionalBlocks.accept(Items.GLOW_ITEM_FRAME);
                  functionalBlocks.accept(Items.PAINTING);
                  parameters.holders()
                     .lookup(Registries.PAINTING_VARIANT)
                     .ifPresent(
                        paintings -> generatePresetPaintings(
                           functionalBlocks,
                           parameters.holders(),
                           (HolderLookup.RegistryLookup<PaintingVariant>)paintings,
                           variant -> variant.is(PaintingVariantTags.PLACEABLE),
                           CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                        )
                     );
                  functionalBlocks.accept(Items.BOOKSHELF);
                  functionalBlocks.accept(Items.CHISELED_BOOKSHELF);
                  functionalBlocks.accept(Items.OAK_SHELF);
                  functionalBlocks.accept(Items.SPRUCE_SHELF);
                  functionalBlocks.accept(Items.BIRCH_SHELF);
                  functionalBlocks.accept(Items.JUNGLE_SHELF);
                  functionalBlocks.accept(Items.ACACIA_SHELF);
                  functionalBlocks.accept(Items.DARK_OAK_SHELF);
                  functionalBlocks.accept(Items.MANGROVE_SHELF);
                  functionalBlocks.accept(Items.CHERRY_SHELF);
                  functionalBlocks.accept(Items.PALE_OAK_SHELF);
                  functionalBlocks.accept(Items.POPLAR_SHELF);
                  functionalBlocks.accept(Items.BAMBOO_SHELF);
                  functionalBlocks.accept(Items.CRIMSON_SHELF);
                  functionalBlocks.accept(Items.WARPED_SHELF);
                  functionalBlocks.accept(Items.LECTERN);
                  functionalBlocks.accept(Items.TINTED_GLASS);
                  functionalBlocks.accept(Items.OAK_SIGN);
                  functionalBlocks.accept(Items.OAK_HANGING_SIGN);
                  functionalBlocks.accept(Items.SPRUCE_SIGN);
                  functionalBlocks.accept(Items.SPRUCE_HANGING_SIGN);
                  functionalBlocks.accept(Items.BIRCH_SIGN);
                  functionalBlocks.accept(Items.BIRCH_HANGING_SIGN);
                  functionalBlocks.accept(Items.JUNGLE_SIGN);
                  functionalBlocks.accept(Items.JUNGLE_HANGING_SIGN);
                  functionalBlocks.accept(Items.ACACIA_SIGN);
                  functionalBlocks.accept(Items.ACACIA_HANGING_SIGN);
                  functionalBlocks.accept(Items.DARK_OAK_SIGN);
                  functionalBlocks.accept(Items.DARK_OAK_HANGING_SIGN);
                  functionalBlocks.accept(Items.MANGROVE_SIGN);
                  functionalBlocks.accept(Items.MANGROVE_HANGING_SIGN);
                  functionalBlocks.accept(Items.CHERRY_SIGN);
                  functionalBlocks.accept(Items.CHERRY_HANGING_SIGN);
                  functionalBlocks.accept(Items.PALE_OAK_SIGN);
                  functionalBlocks.accept(Items.PALE_OAK_HANGING_SIGN);
                  functionalBlocks.accept(Items.POPLAR_SIGN);
                  functionalBlocks.accept(Items.POPLAR_HANGING_SIGN);
                  functionalBlocks.accept(Items.BAMBOO_SIGN);
                  functionalBlocks.accept(Items.BAMBOO_HANGING_SIGN);
                  functionalBlocks.accept(Items.CRIMSON_SIGN);
                  functionalBlocks.accept(Items.CRIMSON_HANGING_SIGN);
                  functionalBlocks.accept(Items.WARPED_SIGN);
                  functionalBlocks.accept(Items.WARPED_HANGING_SIGN);
                  functionalBlocks.accept(Items.CHEST);
                  Items.COPPER_CHEST.forEach(functionalBlocks::accept);
                  functionalBlocks.accept(Items.BARREL);
                  functionalBlocks.accept(Items.ENDER_CHEST);
                  functionalBlocks.accept(Items.SHULKER_BOX);
                  registerColoredItems(functionalBlocks, gameplayColorOrder, Items.DYED_SHULKER_BOX);
                  functionalBlocks.accept(Items.RESPAWN_ANCHOR);
                  registerColoredItems(functionalBlocks, gameplayColorOrder, Items.BED);
                  functionalBlocks.accept(Items.CANDLE);
                  registerColoredItems(functionalBlocks, gameplayColorOrder, Items.DYED_CANDLE);
                  registerColoredItems(functionalBlocks, gameplayColorOrder, Items.BANNER);
                  functionalBlocks.accept(Raid.getOminousBannerInstance(parameters.holders().lookupOrThrow(Registries.BANNER_PATTERN)));
                  functionalBlocks.accept(Items.SKELETON_SKULL);
                  functionalBlocks.accept(Items.WITHER_SKELETON_SKULL);
                  functionalBlocks.accept(Items.PLAYER_HEAD);
                  functionalBlocks.accept(Items.ZOMBIE_HEAD);
                  functionalBlocks.accept(Items.CREEPER_HEAD);
                  functionalBlocks.accept(Items.PIGLIN_HEAD);
                  functionalBlocks.accept(Items.DRAGON_HEAD);
                  functionalBlocks.accept(Items.DRAGON_EGG);
                  functionalBlocks.accept(Items.END_PORTAL_FRAME);
                  functionalBlocks.accept(Items.VAULT);
                  functionalBlocks.accept(Items.ENDER_EYE);
                  functionalBlocks.accept(Items.STRAW_BED);
                  Items.COPPER_GOLEM_STATUE.forEach(functionalBlocks::accept);
                  functionalBlocks.accept(Items.INFESTED_STONE);
                  functionalBlocks.accept(Items.INFESTED_COBBLESTONE);
                  functionalBlocks.accept(Items.INFESTED_STONE_BRICKS);
                  functionalBlocks.accept(Items.INFESTED_MOSSY_STONE_BRICKS);
                  functionalBlocks.accept(Items.INFESTED_CRACKED_STONE_BRICKS);
                  functionalBlocks.accept(Items.INFESTED_CHISELED_STONE_BRICKS);
                  functionalBlocks.accept(Items.INFESTED_DEEPSLATE);
               }
            )
            .build()
      );
      Registry.register(
         registry,
         REDSTONE_BLOCKS,
         CreativeModeTab.builder(CreativeModeTab.Row.TOP, 4)
            .title(Component.translatable("itemGroup.redstone"))
            .icon(() -> new ItemStack(Items.REDSTONE))
            .displayItems((parameters, redstoneBlocks) -> {
               redstoneBlocks.accept(Items.REDSTONE);
               redstoneBlocks.accept(Items.REDSTONE_TORCH);
               redstoneBlocks.accept(Items.REDSTONE_BLOCK);
               redstoneBlocks.accept(Items.REPEATER);
               redstoneBlocks.accept(Items.COMPARATOR);
               redstoneBlocks.accept(Items.TARGET);
               Items.COPPER_BULB.waxed().forEach(redstoneBlocks::accept);
               redstoneBlocks.accept(Items.LEVER);
               redstoneBlocks.accept(Items.OAK_BUTTON);
               redstoneBlocks.accept(Items.STONE_BUTTON);
               redstoneBlocks.accept(Items.OAK_PRESSURE_PLATE);
               redstoneBlocks.accept(Items.STONE_PRESSURE_PLATE);
               redstoneBlocks.accept(Items.LIGHT_WEIGHTED_PRESSURE_PLATE);
               redstoneBlocks.accept(Items.HEAVY_WEIGHTED_PRESSURE_PLATE);
               redstoneBlocks.accept(Items.SCULK_SENSOR);
               redstoneBlocks.accept(Items.CALIBRATED_SCULK_SENSOR);
               redstoneBlocks.accept(Items.SCULK_SHRIEKER);
               redstoneBlocks.accept(Items.AMETHYST_BLOCK);
               redstoneBlocks.accept(Items.WOOL.white());
               redstoneBlocks.accept(Items.TRIPWIRE_HOOK);
               redstoneBlocks.accept(Items.STRING);
               redstoneBlocks.accept(Items.LECTERN);
               redstoneBlocks.accept(Items.DAYLIGHT_DETECTOR);
               redstoneBlocks.accept(Items.LIGHTNING_ROD.waxed().unaffected());
               redstoneBlocks.accept(Items.PISTON);
               redstoneBlocks.accept(Items.STICKY_PISTON);
               redstoneBlocks.accept(Items.SLIME_BLOCK);
               redstoneBlocks.accept(Items.HONEY_BLOCK);
               redstoneBlocks.accept(Items.DISPENSER);
               redstoneBlocks.accept(Items.DROPPER);
               redstoneBlocks.accept(Items.CRAFTER);
               redstoneBlocks.accept(Items.HOPPER);
               redstoneBlocks.accept(Items.CHEST);
               redstoneBlocks.accept(Items.COPPER_CHEST.waxed().unaffected());
               redstoneBlocks.accept(Items.BARREL);
               redstoneBlocks.accept(Items.CHISELED_BOOKSHELF);
               redstoneBlocks.accept(Items.OAK_SHELF);
               redstoneBlocks.accept(Items.FURNACE);
               redstoneBlocks.accept(Items.TRAPPED_CHEST);
               redstoneBlocks.accept(Items.JUKEBOX);
               redstoneBlocks.accept(Items.DECORATED_POT);
               redstoneBlocks.accept(Items.OBSERVER);
               redstoneBlocks.accept(Items.NOTE_BLOCK);
               redstoneBlocks.accept(Items.COMPOSTER);
               redstoneBlocks.accept(Items.CAULDRON);
               redstoneBlocks.accept(Items.RAIL);
               redstoneBlocks.accept(Items.POWERED_RAIL);
               redstoneBlocks.accept(Items.DETECTOR_RAIL);
               redstoneBlocks.accept(Items.ACTIVATOR_RAIL);
               redstoneBlocks.accept(Items.MINECART);
               redstoneBlocks.accept(Items.HOPPER_MINECART);
               redstoneBlocks.accept(Items.CHEST_MINECART);
               redstoneBlocks.accept(Items.FURNACE_MINECART);
               redstoneBlocks.accept(Items.TNT_MINECART);
               redstoneBlocks.accept(Items.OAK_CHEST_BOAT);
               redstoneBlocks.accept(Items.BAMBOO_CHEST_RAFT);
               redstoneBlocks.accept(Items.OAK_DOOR);
               redstoneBlocks.accept(Items.IRON_DOOR);
               redstoneBlocks.accept(Items.OAK_FENCE_GATE);
               redstoneBlocks.accept(Items.OAK_TRAPDOOR);
               redstoneBlocks.accept(Items.IRON_TRAPDOOR);
               redstoneBlocks.accept(Items.TNT);
               redstoneBlocks.accept(Items.REDSTONE_LAMP);
               redstoneBlocks.accept(Items.BELL);
               redstoneBlocks.accept(Items.BIG_DRIPLEAF);
               redstoneBlocks.accept(Items.ARMOR_STAND);
               redstoneBlocks.accept(Items.REDSTONE_ORE);
            })
            .build()
      );
      Registry.register(
         registry,
         HOTBAR,
         CreativeModeTab.builder(CreativeModeTab.Row.TOP, 5)
            .title(Component.translatable("itemGroup.hotbar"))
            .icon(() -> new ItemStack(Blocks.BOOKSHELF))
            .alignedRight()
            .type(CreativeModeTab.Type.HOTBAR)
            .build()
      );
      Registry.register(
         registry,
         SEARCH,
         CreativeModeTab.builder(CreativeModeTab.Row.TOP, 6)
            .title(Component.translatable("itemGroup.search"))
            .icon(() -> new ItemStack(Items.COMPASS))
            .displayItems((parameters, search) -> {
               Set<ItemStack> tempItems = ItemStackLinkedSet.createTypeAndComponentsSet();

               for (CreativeModeTab tab : registry) {
                  if (tab.getType() != CreativeModeTab.Type.SEARCH) {
                     tempItems.addAll(tab.getSearchTabDisplayItems());
                  }
               }

               search.acceptAll(tempItems);
            })
            .backgroundTexture(SEARCH_BACKGROUND)
            .alignedRight()
            .type(CreativeModeTab.Type.SEARCH)
            .build()
      );
      Registry.register(
         registry,
         TOOLS_AND_UTILITIES,
         CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 0)
            .title(Component.translatable("itemGroup.tools"))
            .icon(() -> new ItemStack(Items.DIAMOND_PICKAXE))
            .displayItems(
               (parameters, toolsAndUtilities) -> {
                  toolsAndUtilities.accept(Items.WOODEN_SHOVEL);
                  toolsAndUtilities.accept(Items.WOODEN_PICKAXE);
                  toolsAndUtilities.accept(Items.WOODEN_AXE);
                  toolsAndUtilities.accept(Items.WOODEN_HOE);
                  toolsAndUtilities.accept(Items.STONE_SHOVEL);
                  toolsAndUtilities.accept(Items.STONE_PICKAXE);
                  toolsAndUtilities.accept(Items.STONE_AXE);
                  toolsAndUtilities.accept(Items.STONE_HOE);
                  toolsAndUtilities.accept(Items.COPPER_SHOVEL);
                  toolsAndUtilities.accept(Items.COPPER_PICKAXE);
                  toolsAndUtilities.accept(Items.COPPER_AXE);
                  toolsAndUtilities.accept(Items.COPPER_HOE);
                  toolsAndUtilities.accept(Items.IRON_SHOVEL);
                  toolsAndUtilities.accept(Items.IRON_PICKAXE);
                  toolsAndUtilities.accept(Items.IRON_AXE);
                  toolsAndUtilities.accept(Items.IRON_HOE);
                  toolsAndUtilities.accept(Items.GOLDEN_SHOVEL);
                  toolsAndUtilities.accept(Items.GOLDEN_PICKAXE);
                  toolsAndUtilities.accept(Items.GOLDEN_AXE);
                  toolsAndUtilities.accept(Items.GOLDEN_HOE);
                  toolsAndUtilities.accept(Items.DIAMOND_SHOVEL);
                  toolsAndUtilities.accept(Items.DIAMOND_PICKAXE);
                  toolsAndUtilities.accept(Items.DIAMOND_AXE);
                  toolsAndUtilities.accept(Items.DIAMOND_HOE);
                  toolsAndUtilities.accept(Items.NETHERITE_SHOVEL);
                  toolsAndUtilities.accept(Items.NETHERITE_PICKAXE);
                  toolsAndUtilities.accept(Items.NETHERITE_AXE);
                  toolsAndUtilities.accept(Items.NETHERITE_HOE);
                  toolsAndUtilities.accept(Items.BUCKET);
                  toolsAndUtilities.accept(Items.WATER_BUCKET);
                  toolsAndUtilities.accept(Items.COD_BUCKET);
                  toolsAndUtilities.accept(Items.SALMON_BUCKET);
                  toolsAndUtilities.accept(Items.TROPICAL_FISH_BUCKET);
                  toolsAndUtilities.accept(Items.PUFFERFISH_BUCKET);
                  toolsAndUtilities.accept(Items.AXOLOTL_BUCKET);
                  toolsAndUtilities.accept(Items.TADPOLE_BUCKET);
                  toolsAndUtilities.accept(Items.SULFUR_CUBE_BUCKET);
                  toolsAndUtilities.accept(Items.LAVA_BUCKET);
                  toolsAndUtilities.accept(Items.POWDER_SNOW_BUCKET);
                  toolsAndUtilities.accept(Items.MILK_BUCKET);
                  toolsAndUtilities.accept(Items.FISHING_ROD);
                  toolsAndUtilities.accept(Items.FLINT_AND_STEEL);
                  toolsAndUtilities.accept(Items.FIRE_CHARGE);
                  toolsAndUtilities.accept(Items.BONE_MEAL);
                  toolsAndUtilities.accept(Items.SHEARS);
                  toolsAndUtilities.accept(Items.BRUSH);
                  toolsAndUtilities.accept(Items.NAME_TAG);
                  toolsAndUtilities.accept(Items.LEAD);
                  toolsAndUtilities.accept(Items.BUNDLE);
                  Items.DYED_BUNDLE.forEach(toolsAndUtilities::accept);
                  toolsAndUtilities.accept(Items.COMPASS);
                  toolsAndUtilities.accept(Items.RECOVERY_COMPASS);
                  toolsAndUtilities.accept(Items.CLOCK);
                  toolsAndUtilities.accept(Items.SPYGLASS);
                  toolsAndUtilities.accept(Items.MAP);
                  toolsAndUtilities.accept(Items.WRITABLE_BOOK);
                  toolsAndUtilities.accept(Items.WIND_CHARGE);
                  toolsAndUtilities.accept(Items.ENDER_PEARL);
                  toolsAndUtilities.accept(Items.ENDER_EYE);
                  toolsAndUtilities.accept(Items.ELYTRA);
                  generateFireworksAllDurations(toolsAndUtilities, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                  toolsAndUtilities.accept(Items.SADDLE);
                  Items.HARNESS.forEach(toolsAndUtilities::accept);
                  toolsAndUtilities.accept(Items.CARROT_ON_A_STICK);
                  toolsAndUtilities.accept(Items.WARPED_FUNGUS_ON_A_STICK);
                  toolsAndUtilities.accept(Items.OAK_BOAT);
                  toolsAndUtilities.accept(Items.OAK_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.SPRUCE_BOAT);
                  toolsAndUtilities.accept(Items.SPRUCE_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.BIRCH_BOAT);
                  toolsAndUtilities.accept(Items.BIRCH_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.JUNGLE_BOAT);
                  toolsAndUtilities.accept(Items.JUNGLE_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.ACACIA_BOAT);
                  toolsAndUtilities.accept(Items.ACACIA_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.DARK_OAK_BOAT);
                  toolsAndUtilities.accept(Items.DARK_OAK_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.MANGROVE_BOAT);
                  toolsAndUtilities.accept(Items.MANGROVE_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.CHERRY_BOAT);
                  toolsAndUtilities.accept(Items.CHERRY_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.PALE_OAK_BOAT);
                  toolsAndUtilities.accept(Items.PALE_OAK_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.POPLAR_BOAT);
                  toolsAndUtilities.accept(Items.POPLAR_CHEST_BOAT);
                  toolsAndUtilities.accept(Items.BAMBOO_RAFT);
                  toolsAndUtilities.accept(Items.BAMBOO_CHEST_RAFT);
                  toolsAndUtilities.accept(Items.RAIL);
                  toolsAndUtilities.accept(Items.POWERED_RAIL);
                  toolsAndUtilities.accept(Items.DETECTOR_RAIL);
                  toolsAndUtilities.accept(Items.ACTIVATOR_RAIL);
                  toolsAndUtilities.accept(Items.MINECART);
                  toolsAndUtilities.accept(Items.HOPPER_MINECART);
                  toolsAndUtilities.accept(Items.CHEST_MINECART);
                  toolsAndUtilities.accept(Items.FURNACE_MINECART);
                  toolsAndUtilities.accept(Items.TNT_MINECART);
                  parameters.holders()
                     .lookup(Registries.INSTRUMENT)
                     .ifPresent(
                        instruments -> generateInstrumentTypes(
                           toolsAndUtilities, instruments, Items.GOAT_HORN, InstrumentTags.GOAT_HORNS, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                        )
                     );
                  toolsAndUtilities.accept(Items.MUSIC_DISC_13);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_CAT);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_BLOCKS);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_CHIRP);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_FAR);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_MALL);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_MELLOHI);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_STAL);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_STRAD);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_WARD);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_11);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_CREATOR_MUSIC_BOX);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_WAIT);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_CREATOR);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_PRECIPICE);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_OTHERSIDE);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_RELIC);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_5);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_PIGSTEP);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_TEARS);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_LAVA_CHICKEN);
                  toolsAndUtilities.accept(Items.MUSIC_DISC_BOUNCE);
               }
            )
            .build()
      );
      Registry.register(
         registry,
         COMBAT,
         CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 1)
            .title(Component.translatable("itemGroup.combat"))
            .icon(() -> new ItemStack(Items.NETHERITE_SWORD))
            .displayItems(
               (parameters, combat) -> {
                  combat.accept(Items.WOODEN_SWORD);
                  combat.accept(Items.STONE_SWORD);
                  combat.accept(Items.COPPER_SWORD);
                  combat.accept(Items.IRON_SWORD);
                  combat.accept(Items.GOLDEN_SWORD);
                  combat.accept(Items.DIAMOND_SWORD);
                  combat.accept(Items.NETHERITE_SWORD);
                  combat.accept(Items.WOODEN_SPEAR);
                  combat.accept(Items.STONE_SPEAR);
                  combat.accept(Items.COPPER_SPEAR);
                  combat.accept(Items.IRON_SPEAR);
                  combat.accept(Items.GOLDEN_SPEAR);
                  combat.accept(Items.DIAMOND_SPEAR);
                  combat.accept(Items.NETHERITE_SPEAR);
                  combat.accept(Items.WOODEN_AXE);
                  combat.accept(Items.STONE_AXE);
                  combat.accept(Items.COPPER_AXE);
                  combat.accept(Items.IRON_AXE);
                  combat.accept(Items.GOLDEN_AXE);
                  combat.accept(Items.DIAMOND_AXE);
                  combat.accept(Items.NETHERITE_AXE);
                  combat.accept(Items.TRIDENT);
                  combat.accept(Items.MACE);
                  combat.accept(Items.SHIELD);
                  combat.accept(Items.LEATHER_HELMET);
                  combat.accept(Items.LEATHER_CHESTPLATE);
                  combat.accept(Items.LEATHER_LEGGINGS);
                  combat.accept(Items.LEATHER_BOOTS);
                  combat.accept(Items.COPPER_HELMET);
                  combat.accept(Items.COPPER_CHESTPLATE);
                  combat.accept(Items.COPPER_LEGGINGS);
                  combat.accept(Items.COPPER_BOOTS);
                  combat.accept(Items.CHAINMAIL_HELMET);
                  combat.accept(Items.CHAINMAIL_CHESTPLATE);
                  combat.accept(Items.CHAINMAIL_LEGGINGS);
                  combat.accept(Items.CHAINMAIL_BOOTS);
                  combat.accept(Items.IRON_HELMET);
                  combat.accept(Items.IRON_CHESTPLATE);
                  combat.accept(Items.IRON_LEGGINGS);
                  combat.accept(Items.IRON_BOOTS);
                  combat.accept(Items.GOLDEN_HELMET);
                  combat.accept(Items.GOLDEN_CHESTPLATE);
                  combat.accept(Items.GOLDEN_LEGGINGS);
                  combat.accept(Items.GOLDEN_BOOTS);
                  combat.accept(Items.DIAMOND_HELMET);
                  combat.accept(Items.DIAMOND_CHESTPLATE);
                  combat.accept(Items.DIAMOND_LEGGINGS);
                  combat.accept(Items.DIAMOND_BOOTS);
                  combat.accept(Items.NETHERITE_HELMET);
                  combat.accept(Items.NETHERITE_CHESTPLATE);
                  combat.accept(Items.NETHERITE_LEGGINGS);
                  combat.accept(Items.NETHERITE_BOOTS);
                  combat.accept(Items.TURTLE_HELMET);
                  combat.accept(Items.LEATHER_HORSE_ARMOR);
                  combat.accept(Items.COPPER_HORSE_ARMOR);
                  combat.accept(Items.IRON_HORSE_ARMOR);
                  combat.accept(Items.GOLDEN_HORSE_ARMOR);
                  combat.accept(Items.DIAMOND_HORSE_ARMOR);
                  combat.accept(Items.NETHERITE_HORSE_ARMOR);
                  combat.accept(Items.WOLF_ARMOR);
                  combat.accept(Items.COPPER_NAUTILUS_ARMOR);
                  combat.accept(Items.IRON_NAUTILUS_ARMOR);
                  combat.accept(Items.GOLDEN_NAUTILUS_ARMOR);
                  combat.accept(Items.DIAMOND_NAUTILUS_ARMOR);
                  combat.accept(Items.NETHERITE_NAUTILUS_ARMOR);
                  combat.accept(Items.TOTEM_OF_UNDYING);
                  combat.accept(Items.TNT);
                  combat.accept(Items.END_CRYSTAL);
                  combat.accept(Items.SNOWBALL);
                  combat.accept(Items.EGG);
                  combat.accept(Items.BROWN_EGG);
                  combat.accept(Items.BLUE_EGG);
                  combat.accept(Items.WIND_CHARGE);
                  combat.accept(Items.BOW);
                  combat.accept(Items.CROSSBOW);
                  generateFireworksAllDurations(combat, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                  combat.accept(Items.ARROW);
                  combat.accept(Items.SPECTRAL_ARROW);
                  parameters.holders()
                     .lookup(Registries.POTION)
                     .ifPresent(
                        potions -> generatePotionEffectTypes(
                           combat, potions, Items.TIPPED_ARROW, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, parameters.enabledFeatures()
                        )
                     );
               }
            )
            .build()
      );
      Registry.register(
         registry,
         FOOD_AND_DRINKS,
         CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 2)
            .title(Component.translatable("itemGroup.foodAndDrink"))
            .icon(() -> new ItemStack(Items.GOLDEN_APPLE))
            .displayItems(
               (parameters, consumables) -> {
                  consumables.accept(Items.APPLE);
                  consumables.accept(Items.GOLDEN_APPLE);
                  consumables.accept(Items.ENCHANTED_GOLDEN_APPLE);
                  consumables.accept(Items.MELON_SLICE);
                  consumables.accept(Items.SWEET_BERRIES);
                  consumables.accept(Items.GLOW_BERRIES);
                  consumables.accept(Items.CHORUS_FRUIT);
                  consumables.accept(Items.CARROT);
                  consumables.accept(Items.GOLDEN_CARROT);
                  consumables.accept(Items.POTATO);
                  consumables.accept(Items.BAKED_POTATO);
                  consumables.accept(Items.POISONOUS_POTATO);
                  consumables.accept(Items.BEETROOT);
                  consumables.accept(Items.GOLDEN_DANDELION);
                  consumables.accept(Items.DRIED_KELP);
                  consumables.accept(Items.BEEF);
                  consumables.accept(Items.COOKED_BEEF);
                  consumables.accept(Items.PORKCHOP);
                  consumables.accept(Items.COOKED_PORKCHOP);
                  consumables.accept(Items.MUTTON);
                  consumables.accept(Items.COOKED_MUTTON);
                  consumables.accept(Items.CHICKEN);
                  consumables.accept(Items.COOKED_CHICKEN);
                  consumables.accept(Items.RABBIT);
                  consumables.accept(Items.COOKED_RABBIT);
                  consumables.accept(Items.COD);
                  consumables.accept(Items.COOKED_COD);
                  consumables.accept(Items.SALMON);
                  consumables.accept(Items.COOKED_SALMON);
                  consumables.accept(Items.TROPICAL_FISH);
                  consumables.accept(Items.PUFFERFISH);
                  consumables.accept(Items.BREAD);
                  consumables.accept(Items.COOKIE);
                  consumables.accept(Items.CAKE);
                  consumables.accept(Items.PUMPKIN_PIE);
                  consumables.accept(Items.ROTTEN_FLESH);
                  consumables.accept(Items.SPIDER_EYE);
                  consumables.accept(Items.MUSHROOM_STEW);
                  consumables.accept(Items.BEETROOT_SOUP);
                  consumables.accept(Items.RABBIT_STEW);
                  generateSuspiciousStews(consumables, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                  consumables.accept(Items.MILK_BUCKET);
                  consumables.accept(Items.HONEY_BOTTLE);
                  generateOminousBottles(consumables, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                  parameters.holders()
                     .lookup(Registries.POTION)
                     .ifPresent(
                        potions -> {
                           generatePotionEffectTypes(
                              consumables, potions, Items.POTION, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, parameters.enabledFeatures()
                           );
                           generatePotionEffectTypes(
                              consumables, potions, Items.SPLASH_POTION, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, parameters.enabledFeatures()
                           );
                           generatePotionEffectTypes(
                              consumables, potions, Items.LINGERING_POTION, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS, parameters.enabledFeatures()
                           );
                        }
                     );
               }
            )
            .build()
      );
      Registry.register(
         registry,
         INGREDIENTS,
         CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 3)
            .title(Component.translatable("itemGroup.ingredients"))
            .icon(() -> new ItemStack(Items.IRON_INGOT))
            .displayItems((parameters, ingredients) -> {
               ingredients.accept(Items.COAL);
               ingredients.accept(Items.CHARCOAL);
               ingredients.accept(Items.RAW_COPPER);
               ingredients.accept(Items.RAW_IRON);
               ingredients.accept(Items.RAW_GOLD);
               ingredients.accept(Items.EMERALD);
               ingredients.accept(Items.LAPIS_LAZULI);
               ingredients.accept(Items.DIAMOND);
               ingredients.accept(Items.ANCIENT_DEBRIS);
               ingredients.accept(Items.QUARTZ);
               ingredients.accept(Items.AMETHYST_SHARD);
               ingredients.accept(Items.COPPER_NUGGET);
               ingredients.accept(Items.IRON_NUGGET);
               ingredients.accept(Items.GOLD_NUGGET);
               ingredients.accept(Items.COPPER_INGOT);
               ingredients.accept(Items.IRON_INGOT);
               ingredients.accept(Items.GOLD_INGOT);
               ingredients.accept(Items.NETHERITE_SCRAP);
               ingredients.accept(Items.NETHERITE_INGOT);
               ingredients.accept(Items.STICK);
               ingredients.accept(Items.FLINT);
               ingredients.accept(Items.WHEAT);
               ingredients.accept(Items.BONE);
               ingredients.accept(Items.BONE_MEAL);
               ingredients.accept(Items.STRING);
               ingredients.accept(Items.FEATHER);
               ingredients.accept(Items.SNOWBALL);
               ingredients.accept(Items.EGG);
               ingredients.accept(Items.BROWN_EGG);
               ingredients.accept(Items.BLUE_EGG);
               ingredients.accept(Items.LEATHER);
               ingredients.accept(Items.RABBIT_HIDE);
               ingredients.accept(Items.HONEYCOMB);
               ingredients.accept(Items.RESIN_CLUMP);
               ingredients.accept(Items.INK_SAC);
               ingredients.accept(Items.GLOW_INK_SAC);
               ingredients.accept(Items.TURTLE_SCUTE);
               ingredients.accept(Items.ARMADILLO_SCUTE);
               ingredients.accept(Items.SLIME_BALL);
               ingredients.accept(Items.CLAY_BALL);
               ingredients.accept(Items.PRISMARINE_SHARD);
               ingredients.accept(Items.PRISMARINE_CRYSTALS);
               ingredients.accept(Items.NAUTILUS_SHELL);
               ingredients.accept(Items.HEART_OF_THE_SEA);
               ingredients.accept(Items.FIRE_CHARGE);
               ingredients.accept(Items.BLAZE_ROD);
               ingredients.accept(Items.BREEZE_ROD);
               ingredients.accept(Items.HEAVY_CORE);
               ingredients.accept(Items.NETHER_STAR);
               ingredients.accept(Items.ENDER_PEARL);
               ingredients.accept(Items.ENDER_EYE);
               ingredients.accept(Items.SHULKER_SHELL);
               ingredients.accept(Items.POPPED_CHORUS_FRUIT);
               ingredients.accept(Items.ECHO_SHARD);
               ingredients.accept(Items.DISC_FRAGMENT_5);
               Items.DYE.forEach(dye -> ingredients.accept(dye));
               ingredients.accept(Items.BOWL);
               ingredients.accept(Items.BRICK);
               ingredients.accept(Items.NETHER_BRICK);
               ingredients.accept(Items.RESIN_BRICK);
               ingredients.accept(Items.PAPER);
               ingredients.accept(Items.BOOK);
               ingredients.accept(Items.FIREWORK_STAR);
               ingredients.accept(Items.GLASS_BOTTLE);
               ingredients.accept(Items.NETHER_WART);
               ingredients.accept(Items.REDSTONE);
               ingredients.accept(Items.GLOWSTONE_DUST);
               ingredients.accept(Items.GUNPOWDER);
               ingredients.accept(Items.DRAGON_BREATH);
               ingredients.accept(Items.FERMENTED_SPIDER_EYE);
               ingredients.accept(Items.BLAZE_POWDER);
               ingredients.accept(Items.SUGAR);
               ingredients.accept(Items.RABBIT_FOOT);
               ingredients.accept(Items.GLISTERING_MELON_SLICE);
               ingredients.accept(Items.SPIDER_EYE);
               ingredients.accept(Items.PUFFERFISH);
               ingredients.accept(Items.MAGMA_CREAM);
               ingredients.accept(Items.GOLDEN_CARROT);
               ingredients.accept(Items.GHAST_TEAR);
               ingredients.accept(Items.TURTLE_HELMET);
               ingredients.accept(Items.PHANTOM_MEMBRANE);
               ingredients.accept(Items.FIELD_MASONED_BANNER_PATTERN);
               ingredients.accept(Items.BORDURE_INDENTED_BANNER_PATTERN);
               ingredients.accept(Items.FLOWER_BANNER_PATTERN);
               ingredients.accept(Items.CREEPER_BANNER_PATTERN);
               ingredients.accept(Items.SKULL_BANNER_PATTERN);
               ingredients.accept(Items.MOJANG_BANNER_PATTERN);
               ingredients.accept(Items.GLOBE_BANNER_PATTERN);
               ingredients.accept(Items.PIGLIN_BANNER_PATTERN);
               ingredients.accept(Items.FLOW_BANNER_PATTERN);
               ingredients.accept(Items.GUSTER_BANNER_PATTERN);
               ingredients.accept(Items.ANGLER_POTTERY_SHERD);
               ingredients.accept(Items.ARCHER_POTTERY_SHERD);
               ingredients.accept(Items.ARMS_UP_POTTERY_SHERD);
               ingredients.accept(Items.BLADE_POTTERY_SHERD);
               ingredients.accept(Items.BREWER_POTTERY_SHERD);
               ingredients.accept(Items.BURN_POTTERY_SHERD);
               ingredients.accept(Items.DANGER_POTTERY_SHERD);
               ingredients.accept(Items.EXPLORER_POTTERY_SHERD);
               ingredients.accept(Items.FLOW_POTTERY_SHERD);
               ingredients.accept(Items.FRIEND_POTTERY_SHERD);
               ingredients.accept(Items.GUSTER_POTTERY_SHERD);
               ingredients.accept(Items.HEART_POTTERY_SHERD);
               ingredients.accept(Items.HEARTBREAK_POTTERY_SHERD);
               ingredients.accept(Items.HOWL_POTTERY_SHERD);
               ingredients.accept(Items.MINER_POTTERY_SHERD);
               ingredients.accept(Items.MOURNER_POTTERY_SHERD);
               ingredients.accept(Items.PLENTY_POTTERY_SHERD);
               ingredients.accept(Items.PRIZE_POTTERY_SHERD);
               ingredients.accept(Items.SCRAPE_POTTERY_SHERD);
               ingredients.accept(Items.SHEAF_POTTERY_SHERD);
               ingredients.accept(Items.SHELTER_POTTERY_SHERD);
               ingredients.accept(Items.SKULL_POTTERY_SHERD);
               ingredients.accept(Items.SNORT_POTTERY_SHERD);
               ingredients.accept(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
               ingredients.accept(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE);
               ingredients.accept(Items.EXPERIENCE_BOTTLE);
               ingredients.accept(Items.TRIAL_KEY);
               ingredients.accept(Items.OMINOUS_TRIAL_KEY);
               parameters.holders().lookup(Registries.ENCHANTMENT).ifPresent(enchantments -> {
                  generateEnchantmentBookTypesOnlyMaxLevel(ingredients, enchantments, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                  generateEnchantmentBookTypesAllLevels(ingredients, enchantments, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
               });
            })
            .build()
      );
      Registry.register(
         registry,
         SPAWN_EGGS,
         CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 4)
            .title(Component.translatable("itemGroup.spawnEggs"))
            .icon(() -> new ItemStack(Items.CREEPER_SPAWN_EGG))
            .displayItems((parameters, spawnEggs) -> {
               spawnEggs.accept(Items.SPAWNER);
               spawnEggs.accept(Items.TRIAL_SPAWNER);
               spawnEggs.accept(Items.CREAKING_HEART);
               spawnEggs.accept(Items.CHICKEN_SPAWN_EGG);
               spawnEggs.accept(Items.COW_SPAWN_EGG);
               spawnEggs.accept(Items.PIG_SPAWN_EGG);
               spawnEggs.accept(Items.SHEEP_SPAWN_EGG);
               spawnEggs.accept(Items.CAMEL_SPAWN_EGG);
               spawnEggs.accept(Items.DONKEY_SPAWN_EGG);
               spawnEggs.accept(Items.HORSE_SPAWN_EGG);
               spawnEggs.accept(Items.MULE_SPAWN_EGG);
               spawnEggs.accept(Items.CAT_SPAWN_EGG);
               spawnEggs.accept(Items.PARROT_SPAWN_EGG);
               spawnEggs.accept(Items.WOLF_SPAWN_EGG);
               spawnEggs.accept(Items.ARMADILLO_SPAWN_EGG);
               spawnEggs.accept(Items.BAT_SPAWN_EGG);
               spawnEggs.accept(Items.BEE_SPAWN_EGG);
               spawnEggs.accept(Items.FOX_SPAWN_EGG);
               spawnEggs.accept(Items.GOAT_SPAWN_EGG);
               spawnEggs.accept(Items.LLAMA_SPAWN_EGG);
               spawnEggs.accept(Items.OCELOT_SPAWN_EGG);
               spawnEggs.accept(Items.PANDA_SPAWN_EGG);
               spawnEggs.accept(Items.POLAR_BEAR_SPAWN_EGG);
               spawnEggs.accept(Items.RABBIT_SPAWN_EGG);
               spawnEggs.accept(Items.AXOLOTL_SPAWN_EGG);
               spawnEggs.accept(Items.COD_SPAWN_EGG);
               spawnEggs.accept(Items.DOLPHIN_SPAWN_EGG);
               spawnEggs.accept(Items.FROG_SPAWN_EGG);
               spawnEggs.accept(Items.GLOW_SQUID_SPAWN_EGG);
               spawnEggs.accept(Items.NAUTILUS_SPAWN_EGG);
               spawnEggs.accept(Items.PUFFERFISH_SPAWN_EGG);
               spawnEggs.accept(Items.SALMON_SPAWN_EGG);
               spawnEggs.accept(Items.SQUID_SPAWN_EGG);
               spawnEggs.accept(Items.TADPOLE_SPAWN_EGG);
               spawnEggs.accept(Items.TROPICAL_FISH_SPAWN_EGG);
               spawnEggs.accept(Items.TURTLE_SPAWN_EGG);
               spawnEggs.accept(Items.ALLAY_SPAWN_EGG);
               spawnEggs.accept(Items.MOOSHROOM_SPAWN_EGG);
               spawnEggs.accept(Items.SNIFFER_SPAWN_EGG);
               spawnEggs.accept(Items.SULFUR_CUBE_SPAWN_EGG);
               spawnEggs.accept(Items.COPPER_GOLEM_SPAWN_EGG);
               spawnEggs.accept(Items.IRON_GOLEM_SPAWN_EGG);
               spawnEggs.accept(Items.SNOW_GOLEM_SPAWN_EGG);
               spawnEggs.accept(Items.TRADER_LLAMA_SPAWN_EGG);
               spawnEggs.accept(Items.VILLAGER_SPAWN_EGG);
               spawnEggs.accept(Items.WANDERING_TRADER_SPAWN_EGG);
               spawnEggs.accept(Items.BOGGED_SPAWN_EGG);
               spawnEggs.accept(Items.CAMEL_HUSK_SPAWN_EGG);
               spawnEggs.accept(Items.DROWNED_SPAWN_EGG);
               spawnEggs.accept(Items.HUSK_SPAWN_EGG);
               spawnEggs.accept(Items.PARCHED_SPAWN_EGG);
               spawnEggs.accept(Items.SKELETON_SPAWN_EGG);
               spawnEggs.accept(Items.SKELETON_HORSE_SPAWN_EGG);
               spawnEggs.accept(Items.STRAY_SPAWN_EGG);
               spawnEggs.accept(Items.ZOMBIE_SPAWN_EGG);
               spawnEggs.accept(Items.ZOMBIE_HORSE_SPAWN_EGG);
               spawnEggs.accept(Items.ZOMBIE_NAUTILUS_SPAWN_EGG);
               spawnEggs.accept(Items.ZOMBIE_VILLAGER_SPAWN_EGG);
               spawnEggs.accept(Items.CAVE_SPIDER_SPAWN_EGG);
               spawnEggs.accept(Items.SPIDER_SPAWN_EGG);
               spawnEggs.accept(Items.BREEZE_SPAWN_EGG);
               spawnEggs.accept(Items.CREAKING_SPAWN_EGG);
               spawnEggs.accept(Items.CREEPER_SPAWN_EGG);
               spawnEggs.accept(Items.ELDER_GUARDIAN_SPAWN_EGG);
               spawnEggs.accept(Items.GUARDIAN_SPAWN_EGG);
               spawnEggs.accept(Items.PHANTOM_SPAWN_EGG);
               spawnEggs.accept(Items.SILVERFISH_SPAWN_EGG);
               spawnEggs.accept(Items.SLIME_SPAWN_EGG);
               spawnEggs.accept(Items.WARDEN_SPAWN_EGG);
               spawnEggs.accept(Items.WITCH_SPAWN_EGG);
               spawnEggs.accept(Items.EVOKER_SPAWN_EGG);
               spawnEggs.accept(Items.PILLAGER_SPAWN_EGG);
               spawnEggs.accept(Items.RAVAGER_SPAWN_EGG);
               spawnEggs.accept(Items.VEX_SPAWN_EGG);
               spawnEggs.accept(Items.VINDICATOR_SPAWN_EGG);
               spawnEggs.accept(Items.BLAZE_SPAWN_EGG);
               spawnEggs.accept(Items.GHAST_SPAWN_EGG);
               spawnEggs.accept(Items.HAPPY_GHAST_SPAWN_EGG);
               spawnEggs.accept(Items.HOGLIN_SPAWN_EGG);
               spawnEggs.accept(Items.MAGMA_CUBE_SPAWN_EGG);
               spawnEggs.accept(Items.PIGLIN_SPAWN_EGG);
               spawnEggs.accept(Items.PIGLIN_BRUTE_SPAWN_EGG);
               spawnEggs.accept(Items.STRIDER_SPAWN_EGG);
               spawnEggs.accept(Items.WITHER_SKELETON_SPAWN_EGG);
               spawnEggs.accept(Items.ZOGLIN_SPAWN_EGG);
               spawnEggs.accept(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG);
               spawnEggs.accept(Items.ENDERMAN_SPAWN_EGG);
               spawnEggs.accept(Items.ENDERMITE_SPAWN_EGG);
               spawnEggs.accept(Items.SHULKER_SPAWN_EGG);
            })
            .build()
      );
      Registry.register(
         registry,
         OP_BLOCKS,
         CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 5)
            .title(Component.translatable("itemGroup.op"))
            .icon(() -> new ItemStack(Items.COMMAND_BLOCK))
            .alignedRight()
            .displayItems(
               (parameters, opBlocks) -> {
                  if (parameters.hasPermissions()) {
                     opBlocks.accept(Items.COMMAND_BLOCK);
                     opBlocks.accept(Items.CHAIN_COMMAND_BLOCK);
                     opBlocks.accept(Items.REPEATING_COMMAND_BLOCK);
                     opBlocks.accept(Items.COMMAND_BLOCK_MINECART);
                     opBlocks.accept(Items.JIGSAW);
                     opBlocks.accept(Items.STRUCTURE_BLOCK);
                     opBlocks.accept(Items.STRUCTURE_VOID);
                     opBlocks.accept(Items.BARRIER);
                     opBlocks.accept(Items.DEBUG_STICK);
                     opBlocks.accept(Items.TEST_INSTANCE_BLOCK);

                     for (TestBlockMode mode : TestBlockMode.values()) {
                        opBlocks.accept(TestBlock.setModeOnStack(new ItemStack(Items.TEST_BLOCK), mode));
                     }

                     for (int lightLevel = 15; lightLevel >= 0; lightLevel--) {
                        opBlocks.accept(LightBlock.setLightOnStack(new ItemStack(Items.LIGHT), lightLevel));
                     }

                     parameters.holders()
                        .lookup(Registries.PAINTING_VARIANT)
                        .ifPresent(
                           paintings -> generatePresetPaintings(
                              opBlocks,
                              parameters.holders(),
                              (HolderLookup.RegistryLookup<PaintingVariant>)paintings,
                              variant -> !variant.is(PaintingVariantTags.PLACEABLE),
                              CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                           )
                        );
                  }
               }
            )
            .build()
      );
      return Registry.register(
         registry,
         INVENTORY,
         CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 6)
            .title(Component.translatable("itemGroup.inventory"))
            .icon(() -> new ItemStack(Blocks.CHEST))
            .backgroundTexture(INVENTORY_BACKGROUND)
            .hideTitle()
            .alignedRight()
            .type(CreativeModeTab.Type.INVENTORY)
            .noScrollBar()
            .build()
      );
   }

   private static void registerColoredItems(
      final CreativeModeTab.Output coloredBlocks, final List<DyeColor> gameplayColorOrder, final ColorCollection<Item> items
   ) {
      gameplayColorOrder.forEach(color -> coloredBlocks.accept(items.pick(color)));
   }

   public static void validate() {
      Map<Pair<CreativeModeTab.Row, Integer>, String> positions = new HashMap<>();

      for (ResourceKey<CreativeModeTab> tabKey : BuiltInRegistries.CREATIVE_MODE_TAB.registryKeySet()) {
         CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(tabKey);
         String current = tab.getDisplayName().getString();
         String previous = positions.put(Pair.of(tab.row(), tab.column()), current);
         if (previous != null) {
            throw new IllegalArgumentException("Duplicate position: " + current + " vs. " + previous);
         }
      }
   }

   public static CreativeModeTab getDefaultTab() {
      return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(BUILDING_BLOCKS);
   }

   private static void generatePotionEffectTypes(
      final CreativeModeTab.Output output,
      final HolderLookup<Potion> potions,
      final Item item,
      final CreativeModeTab.TabVisibility tabVisibility,
      final FeatureFlagSet enabledFeatures
   ) {
      potions.listElements()
         .filter(potion -> potion.value().isEnabled(enabledFeatures))
         .map(potion -> PotionContents.createItemStack(item, potion))
         .forEach(stack -> output.accept(stack, tabVisibility));
   }

   private static void generateEnchantmentBookTypesOnlyMaxLevel(
      final CreativeModeTab.Output output, final HolderLookup<Enchantment> enchantments, final CreativeModeTab.TabVisibility tabVisibility
   ) {
      enchantments.listElements()
         .map(enchantment -> EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, enchantment.value().getMaxLevel())))
         .forEach(stack -> output.accept(stack, tabVisibility));
   }

   private static void generateEnchantmentBookTypesAllLevels(
      final CreativeModeTab.Output output, final HolderLookup<Enchantment> enchantments, final CreativeModeTab.TabVisibility tabVisibility
   ) {
      enchantments.listElements()
         .flatMap(
            enchantment -> IntStream.rangeClosed(enchantment.value().getMinLevel(), enchantment.value().getMaxLevel())
               .mapToObj(level -> EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level)))
         )
         .forEach(stack -> output.accept(stack, tabVisibility));
   }

   private static void generateInstrumentTypes(
      final CreativeModeTab.Output output,
      final HolderLookup<Instrument> instruments,
      final Item instrumentItem,
      final TagKey<Instrument> instrumentTagKey,
      final CreativeModeTab.TabVisibility tabVisibility
   ) {
      instruments.get(instrumentTagKey)
         .ifPresent(
            tag -> tag.stream()
               .map(instrument -> InstrumentItem.create(instrumentItem, (Holder<Instrument>)instrument))
               .forEach(stack -> output.accept(stack, tabVisibility))
         );
   }

   private static void generateSuspiciousStews(final CreativeModeTab.Output output, final CreativeModeTab.TabVisibility tabVisibility) {
      List<SuspiciousEffectHolder> effectHolders = SuspiciousEffectHolder.getAllEffectHolders();
      Set<ItemStack> stewItems = ItemStackLinkedSet.createTypeAndComponentsSet();

      for (SuspiciousEffectHolder effectHolder : effectHolders) {
         ItemStack stack = new ItemStack(Items.SUSPICIOUS_STEW);
         stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effectHolder.getSuspiciousEffects());
         stewItems.add(stack);
      }

      output.acceptAll(stewItems, tabVisibility);
   }

   private static void generateOminousBottles(final CreativeModeTab.Output output, final CreativeModeTab.TabVisibility tabVisibility) {
      for (int i = 0; i <= 4; i++) {
         ItemStack stack = new ItemStack(Items.OMINOUS_BOTTLE);
         stack.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(i));
         output.accept(stack, tabVisibility);
      }
   }

   private static void generateFireworksAllDurations(final CreativeModeTab.Output output, final CreativeModeTab.TabVisibility tabVisibility) {
      for (byte duration : FireworkRocketItem.CRAFTABLE_DURATIONS) {
         ItemStack firework = new ItemStack(Items.FIREWORK_ROCKET);
         firework.set(DataComponents.FIREWORKS, new Fireworks(duration, List.of()));
         output.accept(firework, tabVisibility);
      }
   }

   private static void generatePresetPaintings(
      final CreativeModeTab.Output output,
      final HolderLookup.Provider context,
      final HolderLookup.RegistryLookup<PaintingVariant> paintings,
      final Predicate<Holder<PaintingVariant>> filter,
      final CreativeModeTab.TabVisibility tabVisibility
   ) {
      RegistryOps<Tag> ops = context.createSerializationContext(NbtOps.INSTANCE);
      paintings.listElements().filter(filter).sorted(PAINTING_COMPARATOR).forEach(painting -> {
         ItemStack stack = new ItemStack(Items.PAINTING);
         stack.set(DataComponents.PAINTING_VARIANT, painting);
         output.accept(stack, tabVisibility);
      });
   }

   private static void copperBlockFamilies(final Consumer<WeatheringCopperCollection<Item>> output) {
      output.accept(Items.COPPER_BLOCK);
      output.accept(Items.CHISELED_COPPER);
      output.accept(Items.COPPER_GRATE);
      output.accept(Items.CUT_COPPER);
      output.accept(Items.CUT_COPPER_STAIRS);
      output.accept(Items.CUT_COPPER_SLAB);
      output.accept(Items.COPPER_BARS);
      output.accept(Items.COPPER_DOOR);
      output.accept(Items.COPPER_TRAPDOOR);
      output.accept(Items.COPPER_BULB);
      output.accept(Items.COPPER_CHAIN);
   }

   public static List<CreativeModeTab> tabs() {
      return streamAllTabs().filter(CreativeModeTab::shouldDisplay).toList();
   }

   public static List<CreativeModeTab> allTabs() {
      return streamAllTabs().toList();
   }

   private static Stream<CreativeModeTab> streamAllTabs() {
      return BuiltInRegistries.CREATIVE_MODE_TAB.stream();
   }

   public static CreativeModeTab searchTab() {
      return BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(SEARCH);
   }

   private static void buildAllTabContents(final CreativeModeTab.ItemDisplayParameters parameters) {
      streamAllTabs().filter(tab -> tab.getType() == CreativeModeTab.Type.CATEGORY).forEach(tab -> tab.buildContents(parameters));
      streamAllTabs().filter(tab -> tab.getType() != CreativeModeTab.Type.CATEGORY).forEach(tab -> tab.buildContents(parameters));
   }

   public static boolean tryRebuildTabContents(final FeatureFlagSet enabledFeatures, final boolean hasPermissions, final HolderLookup.Provider lookup) {
      if (CACHED_PARAMETERS != null && !CACHED_PARAMETERS.needsUpdate(enabledFeatures, hasPermissions, lookup)) {
         return false;
      }

      CACHED_PARAMETERS = new CreativeModeTab.ItemDisplayParameters(enabledFeatures, hasPermissions, lookup);
      buildAllTabContents(CACHED_PARAMETERS);
      return true;
   }
}
