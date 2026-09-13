package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.references.BlockItemIds;
import net.minecraft.references.ItemIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BannerPatternTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.TestBlock;
import net.minecraft.world.level.block.WeatheringCopperCollection;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.properties.TestBlockMode;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.waypoints.Waypoint;

public class Items {
   public static final Item AIR = registerItem(BlockItemIds.AIR.item(), AirItem::new, new Item.Properties().useBlockDescriptionPrefix());
   public static final Item STONE = registerBlock(BlockItemIds.STONE, Blocks.STONE);
   public static final Item GRANITE = registerBlock(BlockItemIds.GRANITE, Blocks.GRANITE);
   public static final Item POLISHED_GRANITE = registerBlock(BlockItemIds.POLISHED_GRANITE, Blocks.POLISHED_GRANITE);
   public static final Item DIORITE = registerBlock(BlockItemIds.DIORITE, Blocks.DIORITE);
   public static final Item POLISHED_DIORITE = registerBlock(BlockItemIds.POLISHED_DIORITE, Blocks.POLISHED_DIORITE);
   public static final Item ANDESITE = registerBlock(BlockItemIds.ANDESITE, Blocks.ANDESITE);
   public static final Item POLISHED_ANDESITE = registerBlock(BlockItemIds.POLISHED_ANDESITE, Blocks.POLISHED_ANDESITE);
   public static final Item DEEPSLATE = registerBlock(BlockItemIds.DEEPSLATE, Blocks.DEEPSLATE);
   public static final Item COBBLED_DEEPSLATE = registerBlock(BlockItemIds.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
   public static final Item POLISHED_DEEPSLATE = registerBlock(BlockItemIds.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE);
   public static final Item CALCITE = registerBlock(BlockItemIds.CALCITE, Blocks.CALCITE);
   public static final Item TUFF = registerBlock(BlockItemIds.TUFF, Blocks.TUFF);
   public static final Item TUFF_SLAB = registerBlock(BlockItemIds.TUFF_SLAB, Blocks.TUFF_SLAB);
   public static final Item TUFF_STAIRS = registerBlock(BlockItemIds.TUFF_STAIRS, Blocks.TUFF_STAIRS);
   public static final Item TUFF_WALL = registerBlock(BlockItemIds.TUFF_WALL, Blocks.TUFF_WALL);
   public static final Item CHISELED_TUFF = registerBlock(BlockItemIds.CHISELED_TUFF, Blocks.CHISELED_TUFF);
   public static final Item POLISHED_TUFF = registerBlock(BlockItemIds.POLISHED_TUFF, Blocks.POLISHED_TUFF);
   public static final Item POLISHED_TUFF_SLAB = registerBlock(BlockItemIds.POLISHED_TUFF_SLAB, Blocks.POLISHED_TUFF_SLAB);
   public static final Item POLISHED_TUFF_STAIRS = registerBlock(BlockItemIds.POLISHED_TUFF_STAIRS, Blocks.POLISHED_TUFF_STAIRS);
   public static final Item POLISHED_TUFF_WALL = registerBlock(BlockItemIds.POLISHED_TUFF_WALL, Blocks.POLISHED_TUFF_WALL);
   public static final Item TUFF_BRICKS = registerBlock(BlockItemIds.TUFF_BRICKS, Blocks.TUFF_BRICKS);
   public static final Item TUFF_BRICK_SLAB = registerBlock(BlockItemIds.TUFF_BRICK_SLAB, Blocks.TUFF_BRICK_SLAB);
   public static final Item TUFF_BRICK_STAIRS = registerBlock(BlockItemIds.TUFF_BRICK_STAIRS, Blocks.TUFF_BRICK_STAIRS);
   public static final Item TUFF_BRICK_WALL = registerBlock(BlockItemIds.TUFF_BRICK_WALL, Blocks.TUFF_BRICK_WALL);
   public static final Item CHISELED_TUFF_BRICKS = registerBlock(BlockItemIds.CHISELED_TUFF_BRICKS, Blocks.CHISELED_TUFF_BRICKS);
   public static final Item SULFUR = registerBlock(BlockItemIds.SULFUR, Blocks.SULFUR);
   public static final Item POTENT_SULFUR = registerBlock(BlockItemIds.POTENT_SULFUR, Blocks.POTENT_SULFUR);
   public static final Item SULFUR_SLAB = registerBlock(BlockItemIds.SULFUR_SLAB, Blocks.SULFUR_SLAB);
   public static final Item SULFUR_STAIRS = registerBlock(BlockItemIds.SULFUR_STAIRS, Blocks.SULFUR_STAIRS);
   public static final Item SULFUR_WALL = registerBlock(BlockItemIds.SULFUR_WALL, Blocks.SULFUR_WALL);
   public static final Item POLISHED_SULFUR = registerBlock(BlockItemIds.POLISHED_SULFUR, Blocks.POLISHED_SULFUR);
   public static final Item POLISHED_SULFUR_SLAB = registerBlock(BlockItemIds.POLISHED_SULFUR_SLAB, Blocks.POLISHED_SULFUR_SLAB);
   public static final Item POLISHED_SULFUR_STAIRS = registerBlock(BlockItemIds.POLISHED_SULFUR_STAIRS, Blocks.POLISHED_SULFUR_STAIRS);
   public static final Item POLISHED_SULFUR_WALL = registerBlock(BlockItemIds.POLISHED_SULFUR_WALL, Blocks.POLISHED_SULFUR_WALL);
   public static final Item SULFUR_BRICKS = registerBlock(BlockItemIds.SULFUR_BRICKS, Blocks.SULFUR_BRICKS);
   public static final Item SULFUR_BRICK_SLAB = registerBlock(BlockItemIds.SULFUR_BRICK_SLAB, Blocks.SULFUR_BRICK_SLAB);
   public static final Item SULFUR_BRICK_STAIRS = registerBlock(BlockItemIds.SULFUR_BRICK_STAIRS, Blocks.SULFUR_BRICK_STAIRS);
   public static final Item SULFUR_BRICK_WALL = registerBlock(BlockItemIds.SULFUR_BRICK_WALL, Blocks.SULFUR_BRICK_WALL);
   public static final Item CHISELED_SULFUR = registerBlock(BlockItemIds.CHISELED_SULFUR, Blocks.CHISELED_SULFUR);
   public static final Item CINNABAR = registerBlock(BlockItemIds.CINNABAR, Blocks.CINNABAR);
   public static final Item CINNABAR_SLAB = registerBlock(BlockItemIds.CINNABAR_SLAB, Blocks.CINNABAR_SLAB);
   public static final Item CINNABAR_STAIRS = registerBlock(BlockItemIds.CINNABAR_STAIRS, Blocks.CINNABAR_STAIRS);
   public static final Item CINNABAR_WALL = registerBlock(BlockItemIds.CINNABAR_WALL, Blocks.CINNABAR_WALL);
   public static final Item POLISHED_CINNABAR = registerBlock(BlockItemIds.POLISHED_CINNABAR, Blocks.POLISHED_CINNABAR);
   public static final Item POLISHED_CINNABAR_SLAB = registerBlock(BlockItemIds.POLISHED_CINNABAR_SLAB, Blocks.POLISHED_CINNABAR_SLAB);
   public static final Item POLISHED_CINNABAR_STAIRS = registerBlock(BlockItemIds.POLISHED_CINNABAR_STAIRS, Blocks.POLISHED_CINNABAR_STAIRS);
   public static final Item POLISHED_CINNABAR_WALL = registerBlock(BlockItemIds.POLISHED_CINNABAR_WALL, Blocks.POLISHED_CINNABAR_WALL);
   public static final Item CINNABAR_BRICKS = registerBlock(BlockItemIds.CINNABAR_BRICKS, Blocks.CINNABAR_BRICKS);
   public static final Item CINNABAR_BRICK_SLAB = registerBlock(BlockItemIds.CINNABAR_BRICK_SLAB, Blocks.CINNABAR_BRICK_SLAB);
   public static final Item CINNABAR_BRICK_STAIRS = registerBlock(BlockItemIds.CINNABAR_BRICK_STAIRS, Blocks.CINNABAR_BRICK_STAIRS);
   public static final Item CINNABAR_BRICK_WALL = registerBlock(BlockItemIds.CINNABAR_BRICK_WALL, Blocks.CINNABAR_BRICK_WALL);
   public static final Item CHISELED_CINNABAR = registerBlock(BlockItemIds.CHISELED_CINNABAR, Blocks.CHISELED_CINNABAR);
   public static final Item DRIPSTONE_BLOCK = registerBlock(BlockItemIds.DRIPSTONE_BLOCK, Blocks.DRIPSTONE_BLOCK);
   public static final Item GRASS_BLOCK = registerBlock(BlockItemIds.GRASS_BLOCK, Blocks.GRASS_BLOCK);
   public static final Item DIRT = registerBlock(BlockItemIds.DIRT, Blocks.DIRT);
   public static final Item COARSE_DIRT = registerBlock(BlockItemIds.COARSE_DIRT, Blocks.COARSE_DIRT);
   public static final Item PODZOL = registerBlock(BlockItemIds.PODZOL, Blocks.PODZOL);
   public static final Item ROOTED_DIRT = registerBlock(BlockItemIds.ROOTED_DIRT, Blocks.ROOTED_DIRT);
   public static final Item MUD = registerBlock(BlockItemIds.MUD, Blocks.MUD);
   public static final Item CRIMSON_NYLIUM = registerBlock(BlockItemIds.CRIMSON_NYLIUM, Blocks.CRIMSON_NYLIUM);
   public static final Item WARPED_NYLIUM = registerBlock(BlockItemIds.WARPED_NYLIUM, Blocks.WARPED_NYLIUM);
   public static final Item COBBLESTONE = registerBlock(BlockItemIds.COBBLESTONE, Blocks.COBBLESTONE);
   public static final Item OAK_PLANKS = registerBlock(BlockItemIds.OAK_PLANKS, Blocks.OAK_PLANKS);
   public static final Item SPRUCE_PLANKS = registerBlock(BlockItemIds.SPRUCE_PLANKS, Blocks.SPRUCE_PLANKS);
   public static final Item BIRCH_PLANKS = registerBlock(BlockItemIds.BIRCH_PLANKS, Blocks.BIRCH_PLANKS);
   public static final Item JUNGLE_PLANKS = registerBlock(BlockItemIds.JUNGLE_PLANKS, Blocks.JUNGLE_PLANKS);
   public static final Item ACACIA_PLANKS = registerBlock(BlockItemIds.ACACIA_PLANKS, Blocks.ACACIA_PLANKS);
   public static final Item CHERRY_PLANKS = registerBlock(BlockItemIds.CHERRY_PLANKS, Blocks.CHERRY_PLANKS);
   public static final Item DARK_OAK_PLANKS = registerBlock(BlockItemIds.DARK_OAK_PLANKS, Blocks.DARK_OAK_PLANKS);
   public static final Item PALE_OAK_PLANKS = registerBlock(BlockItemIds.PALE_OAK_PLANKS, Blocks.PALE_OAK_PLANKS);
   public static final Item MANGROVE_PLANKS = registerBlock(BlockItemIds.MANGROVE_PLANKS, Blocks.MANGROVE_PLANKS);
   public static final Item POPLAR_PLANKS = registerBlock(BlockItemIds.POPLAR_PLANKS, Blocks.POPLAR_PLANKS);
   public static final Item BAMBOO_PLANKS = registerBlock(BlockItemIds.BAMBOO_PLANKS, Blocks.BAMBOO_PLANKS);
   public static final Item CRIMSON_PLANKS = registerBlock(BlockItemIds.CRIMSON_PLANKS, Blocks.CRIMSON_PLANKS);
   public static final Item WARPED_PLANKS = registerBlock(BlockItemIds.WARPED_PLANKS, Blocks.WARPED_PLANKS);
   public static final Item BAMBOO_MOSAIC = registerBlock(BlockItemIds.BAMBOO_MOSAIC, Blocks.BAMBOO_MOSAIC);
   public static final Item OAK_SAPLING = registerBlock(
      BlockItemIds.OAK_SAPLING, Blocks.OAK_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item SPRUCE_SAPLING = registerBlock(
      BlockItemIds.SPRUCE_SAPLING, Blocks.SPRUCE_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BIRCH_SAPLING = registerBlock(
      BlockItemIds.BIRCH_SAPLING, Blocks.BIRCH_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item JUNGLE_SAPLING = registerBlock(
      BlockItemIds.JUNGLE_SAPLING, Blocks.JUNGLE_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item ACACIA_SAPLING = registerBlock(
      BlockItemIds.ACACIA_SAPLING, Blocks.ACACIA_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item CHERRY_SAPLING = registerBlock(
      BlockItemIds.CHERRY_SAPLING, Blocks.CHERRY_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item DARK_OAK_SAPLING = registerBlock(
      BlockItemIds.DARK_OAK_SAPLING, Blocks.DARK_OAK_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item PALE_OAK_SAPLING = registerBlock(
      BlockItemIds.PALE_OAK_SAPLING, Blocks.PALE_OAK_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item MANGROVE_PROPAGULE = registerBlock(
      BlockItemIds.MANGROVE_PROPAGULE, Blocks.MANGROVE_PROPAGULE, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item POPLAR_SAPLING = registerBlock(
      BlockItemIds.POPLAR_SAPLING, Blocks.POPLAR_SAPLING, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BEDROCK = registerBlock(BlockItemIds.BEDROCK, Blocks.BEDROCK);
   public static final Item SAND = registerBlock(BlockItemIds.SAND, Blocks.SAND);
   public static final Item SUSPICIOUS_SAND = registerBlock(BlockItemIds.SUSPICIOUS_SAND, Blocks.SUSPICIOUS_SAND);
   public static final Item SUSPICIOUS_GRAVEL = registerBlock(BlockItemIds.SUSPICIOUS_GRAVEL, Blocks.SUSPICIOUS_GRAVEL);
   public static final Item RED_SAND = registerBlock(BlockItemIds.RED_SAND, Blocks.RED_SAND);
   public static final Item GRAVEL = registerBlock(BlockItemIds.GRAVEL, Blocks.GRAVEL);
   public static final Item COAL_ORE = registerBlock(BlockItemIds.COAL_ORE, Blocks.COAL_ORE);
   public static final Item DEEPSLATE_COAL_ORE = registerBlock(BlockItemIds.DEEPSLATE_COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
   public static final Item IRON_ORE = registerBlock(BlockItemIds.IRON_ORE, Blocks.IRON_ORE);
   public static final Item DEEPSLATE_IRON_ORE = registerBlock(BlockItemIds.DEEPSLATE_IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
   public static final Item COPPER_ORE = registerBlock(BlockItemIds.COPPER_ORE, Blocks.COPPER_ORE);
   public static final Item DEEPSLATE_COPPER_ORE = registerBlock(BlockItemIds.DEEPSLATE_COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
   public static final Item GOLD_ORE = registerBlock(BlockItemIds.GOLD_ORE, Blocks.GOLD_ORE);
   public static final Item DEEPSLATE_GOLD_ORE = registerBlock(BlockItemIds.DEEPSLATE_GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
   public static final Item REDSTONE_ORE = registerBlock(BlockItemIds.REDSTONE_ORE, Blocks.REDSTONE_ORE);
   public static final Item DEEPSLATE_REDSTONE_ORE = registerBlock(BlockItemIds.DEEPSLATE_REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
   public static final Item EMERALD_ORE = registerBlock(BlockItemIds.EMERALD_ORE, Blocks.EMERALD_ORE);
   public static final Item DEEPSLATE_EMERALD_ORE = registerBlock(BlockItemIds.DEEPSLATE_EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
   public static final Item LAPIS_ORE = registerBlock(BlockItemIds.LAPIS_ORE, Blocks.LAPIS_ORE);
   public static final Item DEEPSLATE_LAPIS_ORE = registerBlock(BlockItemIds.DEEPSLATE_LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
   public static final Item DIAMOND_ORE = registerBlock(BlockItemIds.DIAMOND_ORE, Blocks.DIAMOND_ORE);
   public static final Item DEEPSLATE_DIAMOND_ORE = registerBlock(BlockItemIds.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
   public static final Item NETHER_GOLD_ORE = registerBlock(BlockItemIds.NETHER_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
   public static final Item NETHER_QUARTZ_ORE = registerBlock(BlockItemIds.NETHER_QUARTZ_ORE, Blocks.NETHER_QUARTZ_ORE);
   public static final Item ANCIENT_DEBRIS = registerBlock(BlockItemIds.ANCIENT_DEBRIS, Blocks.ANCIENT_DEBRIS, new Item.Properties().fireResistant());
   public static final Item COAL_BLOCK = registerBlock(BlockItemIds.COAL_BLOCK, Blocks.COAL_BLOCK);
   public static final Item RAW_IRON_BLOCK = registerBlock(BlockItemIds.RAW_IRON_BLOCK, Blocks.RAW_IRON_BLOCK);
   public static final Item RAW_COPPER_BLOCK = registerBlock(BlockItemIds.RAW_COPPER_BLOCK, Blocks.RAW_COPPER_BLOCK);
   public static final Item RAW_GOLD_BLOCK = registerBlock(BlockItemIds.RAW_GOLD_BLOCK, Blocks.RAW_GOLD_BLOCK);
   public static final Item HEAVY_CORE = registerBlock(BlockItemIds.HEAVY_CORE, Blocks.HEAVY_CORE, new Item.Properties().rarity(Rarity.EPIC));
   public static final Item AMETHYST_BLOCK = registerBlock(BlockItemIds.AMETHYST_BLOCK, Blocks.AMETHYST_BLOCK);
   public static final Item BUDDING_AMETHYST = registerBlock(BlockItemIds.BUDDING_AMETHYST, Blocks.BUDDING_AMETHYST);
   public static final Item IRON_BLOCK = registerBlock(BlockItemIds.IRON_BLOCK, Blocks.IRON_BLOCK);
   public static final WeatheringCopperCollection<Item> COPPER_BLOCK = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_BLOCK, Blocks.COPPER_BLOCK, Items::registerBlock
   );
   public static final Item GOLD_BLOCK = registerBlock(BlockItemIds.GOLD_BLOCK, Blocks.GOLD_BLOCK);
   public static final Item DIAMOND_BLOCK = registerBlock(BlockItemIds.DIAMOND_BLOCK, Blocks.DIAMOND_BLOCK);
   public static final Item NETHERITE_BLOCK = registerBlock(BlockItemIds.NETHERITE_BLOCK, Blocks.NETHERITE_BLOCK, new Item.Properties().fireResistant());
   public static final WeatheringCopperCollection<Item> CHISELED_COPPER = WeatheringCopperCollection.registerItems(
      BlockItemIds.CHISELED_COPPER, Blocks.CHISELED_COPPER, Items::registerBlock
   );
   public static final WeatheringCopperCollection<Item> CUT_COPPER = WeatheringCopperCollection.registerItems(
      BlockItemIds.CUT_COPPER, Blocks.CUT_COPPER, Items::registerBlock
   );
   public static final WeatheringCopperCollection<Item> CUT_COPPER_STAIRS = WeatheringCopperCollection.registerItems(
      BlockItemIds.CUT_COPPER_STAIRS, Blocks.CUT_COPPER_STAIRS, Items::registerBlock
   );
   public static final WeatheringCopperCollection<Item> CUT_COPPER_SLAB = WeatheringCopperCollection.registerItems(
      BlockItemIds.CUT_COPPER_SLAB, Blocks.CUT_COPPER_SLAB, Items::registerBlock
   );
   public static final Item OAK_LOG = registerBlock(BlockItemIds.OAK_LOG, Blocks.OAK_LOG);
   public static final Item SPRUCE_LOG = registerBlock(BlockItemIds.SPRUCE_LOG, Blocks.SPRUCE_LOG);
   public static final Item BIRCH_LOG = registerBlock(BlockItemIds.BIRCH_LOG, Blocks.BIRCH_LOG);
   public static final Item JUNGLE_LOG = registerBlock(BlockItemIds.JUNGLE_LOG, Blocks.JUNGLE_LOG);
   public static final Item ACACIA_LOG = registerBlock(BlockItemIds.ACACIA_LOG, Blocks.ACACIA_LOG);
   public static final Item CHERRY_LOG = registerBlock(BlockItemIds.CHERRY_LOG, Blocks.CHERRY_LOG);
   public static final Item PALE_OAK_LOG = registerBlock(BlockItemIds.PALE_OAK_LOG, Blocks.PALE_OAK_LOG);
   public static final Item DARK_OAK_LOG = registerBlock(BlockItemIds.DARK_OAK_LOG, Blocks.DARK_OAK_LOG);
   public static final Item MANGROVE_LOG = registerBlock(BlockItemIds.MANGROVE_LOG, Blocks.MANGROVE_LOG);
   public static final Item POPLAR_LOG = registerBlock(BlockItemIds.POPLAR_LOG, Blocks.POPLAR_LOG);
   public static final Item MANGROVE_ROOTS = registerBlock(
      BlockItemIds.MANGROVE_ROOTS, Blocks.MANGROVE_ROOTS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item MUDDY_MANGROVE_ROOTS = registerBlock(BlockItemIds.MUDDY_MANGROVE_ROOTS, Blocks.MUDDY_MANGROVE_ROOTS);
   public static final Item CRIMSON_STEM = registerBlock(BlockItemIds.CRIMSON_STEM, Blocks.CRIMSON_STEM);
   public static final Item WARPED_STEM = registerBlock(BlockItemIds.WARPED_STEM, Blocks.WARPED_STEM);
   public static final Item BAMBOO_BLOCK = registerBlock(BlockItemIds.BAMBOO_BLOCK, Blocks.BAMBOO_BLOCK);
   public static final Item STRIPPED_OAK_LOG = registerBlock(BlockItemIds.STRIPPED_OAK_LOG, Blocks.STRIPPED_OAK_LOG);
   public static final Item STRIPPED_SPRUCE_LOG = registerBlock(BlockItemIds.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_SPRUCE_LOG);
   public static final Item STRIPPED_BIRCH_LOG = registerBlock(BlockItemIds.STRIPPED_BIRCH_LOG, Blocks.STRIPPED_BIRCH_LOG);
   public static final Item STRIPPED_JUNGLE_LOG = registerBlock(BlockItemIds.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_JUNGLE_LOG);
   public static final Item STRIPPED_ACACIA_LOG = registerBlock(BlockItemIds.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_ACACIA_LOG);
   public static final Item STRIPPED_CHERRY_LOG = registerBlock(BlockItemIds.STRIPPED_CHERRY_LOG, Blocks.STRIPPED_CHERRY_LOG);
   public static final Item STRIPPED_DARK_OAK_LOG = registerBlock(BlockItemIds.STRIPPED_DARK_OAK_LOG, Blocks.STRIPPED_DARK_OAK_LOG);
   public static final Item STRIPPED_PALE_OAK_LOG = registerBlock(BlockItemIds.STRIPPED_PALE_OAK_LOG, Blocks.STRIPPED_PALE_OAK_LOG);
   public static final Item STRIPPED_MANGROVE_LOG = registerBlock(BlockItemIds.STRIPPED_MANGROVE_LOG, Blocks.STRIPPED_MANGROVE_LOG);
   public static final Item STRIPPED_POPLAR_LOG = registerBlock(BlockItemIds.STRIPPED_POPLAR_LOG, Blocks.STRIPPED_POPLAR_LOG);
   public static final Item STRIPPED_CRIMSON_STEM = registerBlock(BlockItemIds.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_CRIMSON_STEM);
   public static final Item STRIPPED_WARPED_STEM = registerBlock(BlockItemIds.STRIPPED_WARPED_STEM, Blocks.STRIPPED_WARPED_STEM);
   public static final Item STRIPPED_OAK_WOOD = registerBlock(BlockItemIds.STRIPPED_OAK_WOOD, Blocks.STRIPPED_OAK_WOOD);
   public static final Item STRIPPED_SPRUCE_WOOD = registerBlock(BlockItemIds.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_WOOD);
   public static final Item STRIPPED_BIRCH_WOOD = registerBlock(BlockItemIds.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_BIRCH_WOOD);
   public static final Item STRIPPED_JUNGLE_WOOD = registerBlock(BlockItemIds.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_WOOD);
   public static final Item STRIPPED_ACACIA_WOOD = registerBlock(BlockItemIds.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_ACACIA_WOOD);
   public static final Item STRIPPED_CHERRY_WOOD = registerBlock(BlockItemIds.STRIPPED_CHERRY_WOOD, Blocks.STRIPPED_CHERRY_WOOD);
   public static final Item STRIPPED_DARK_OAK_WOOD = registerBlock(BlockItemIds.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD);
   public static final Item STRIPPED_PALE_OAK_WOOD = registerBlock(BlockItemIds.STRIPPED_PALE_OAK_WOOD, Blocks.STRIPPED_PALE_OAK_WOOD);
   public static final Item STRIPPED_MANGROVE_WOOD = registerBlock(BlockItemIds.STRIPPED_MANGROVE_WOOD, Blocks.STRIPPED_MANGROVE_WOOD);
   public static final Item STRIPPED_POPLAR_WOOD = registerBlock(BlockItemIds.STRIPPED_POPLAR_WOOD, Blocks.STRIPPED_POPLAR_WOOD);
   public static final Item STRIPPED_CRIMSON_HYPHAE = registerBlock(BlockItemIds.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_HYPHAE);
   public static final Item STRIPPED_WARPED_HYPHAE = registerBlock(BlockItemIds.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE);
   public static final Item STRIPPED_BAMBOO_BLOCK = registerBlock(BlockItemIds.STRIPPED_BAMBOO_BLOCK, Blocks.STRIPPED_BAMBOO_BLOCK);
   public static final Item OAK_WOOD = registerBlock(BlockItemIds.OAK_WOOD, Blocks.OAK_WOOD);
   public static final Item SPRUCE_WOOD = registerBlock(BlockItemIds.SPRUCE_WOOD, Blocks.SPRUCE_WOOD);
   public static final Item BIRCH_WOOD = registerBlock(BlockItemIds.BIRCH_WOOD, Blocks.BIRCH_WOOD);
   public static final Item JUNGLE_WOOD = registerBlock(BlockItemIds.JUNGLE_WOOD, Blocks.JUNGLE_WOOD);
   public static final Item ACACIA_WOOD = registerBlock(BlockItemIds.ACACIA_WOOD, Blocks.ACACIA_WOOD);
   public static final Item CHERRY_WOOD = registerBlock(BlockItemIds.CHERRY_WOOD, Blocks.CHERRY_WOOD);
   public static final Item PALE_OAK_WOOD = registerBlock(BlockItemIds.PALE_OAK_WOOD, Blocks.PALE_OAK_WOOD);
   public static final Item DARK_OAK_WOOD = registerBlock(BlockItemIds.DARK_OAK_WOOD, Blocks.DARK_OAK_WOOD);
   public static final Item MANGROVE_WOOD = registerBlock(BlockItemIds.MANGROVE_WOOD, Blocks.MANGROVE_WOOD);
   public static final Item POPLAR_WOOD = registerBlock(BlockItemIds.POPLAR_WOOD, Blocks.POPLAR_WOOD);
   public static final Item CRIMSON_HYPHAE = registerBlock(BlockItemIds.CRIMSON_HYPHAE, Blocks.CRIMSON_HYPHAE);
   public static final Item WARPED_HYPHAE = registerBlock(BlockItemIds.WARPED_HYPHAE, Blocks.WARPED_HYPHAE);
   public static final Item OAK_LEAVES = registerBlock(
      BlockItemIds.OAK_LEAVES, Blocks.OAK_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item SPRUCE_LEAVES = registerBlock(
      BlockItemIds.SPRUCE_LEAVES, Blocks.SPRUCE_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BIRCH_LEAVES = registerBlock(
      BlockItemIds.BIRCH_LEAVES, Blocks.BIRCH_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item JUNGLE_LEAVES = registerBlock(
      BlockItemIds.JUNGLE_LEAVES, Blocks.JUNGLE_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item ACACIA_LEAVES = registerBlock(
      BlockItemIds.ACACIA_LEAVES, Blocks.ACACIA_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item CHERRY_LEAVES = registerBlock(
      BlockItemIds.CHERRY_LEAVES, Blocks.CHERRY_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item DARK_OAK_LEAVES = registerBlock(
      BlockItemIds.DARK_OAK_LEAVES, Blocks.DARK_OAK_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item PALE_OAK_LEAVES = registerBlock(
      BlockItemIds.PALE_OAK_LEAVES, Blocks.PALE_OAK_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item MANGROVE_LEAVES = registerBlock(
      BlockItemIds.MANGROVE_LEAVES, Blocks.MANGROVE_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item RED_POPLAR_LEAVES = registerBlock(
      BlockItemIds.RED_POPLAR_LEAVES, Blocks.RED_POPLAR_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item ORANGE_POPLAR_LEAVES = registerBlock(
      BlockItemIds.ORANGE_POPLAR_LEAVES, Blocks.ORANGE_POPLAR_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item YELLOW_POPLAR_LEAVES = registerBlock(
      BlockItemIds.YELLOW_POPLAR_LEAVES, Blocks.YELLOW_POPLAR_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item AZALEA_LEAVES = registerBlock(
      BlockItemIds.AZALEA_LEAVES, Blocks.AZALEA_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item FLOWERING_AZALEA_LEAVES = registerBlock(
      BlockItemIds.FLOWERING_AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item SPONGE = registerBlock(BlockItemIds.SPONGE, Blocks.SPONGE);
   public static final Item WET_SPONGE = registerBlock(BlockItemIds.WET_SPONGE, Blocks.WET_SPONGE);
   public static final Item GLASS = registerBlock(BlockItemIds.GLASS, Blocks.GLASS);
   public static final Item TINTED_GLASS = registerBlock(BlockItemIds.TINTED_GLASS, Blocks.TINTED_GLASS);
   public static final Item LAPIS_BLOCK = registerBlock(BlockItemIds.LAPIS_BLOCK, Blocks.LAPIS_BLOCK);
   public static final Item SANDSTONE = registerBlock(BlockItemIds.SANDSTONE, Blocks.SANDSTONE);
   public static final Item CHISELED_SANDSTONE = registerBlock(BlockItemIds.CHISELED_SANDSTONE, Blocks.CHISELED_SANDSTONE);
   public static final Item CUT_SANDSTONE = registerBlock(BlockItemIds.CUT_SANDSTONE, Blocks.CUT_SANDSTONE);
   public static final Item COBWEB = registerBlock(BlockItemIds.COBWEB, Blocks.COBWEB);
   public static final Item SHORT_GRASS = registerBlock(
      BlockItemIds.SHORT_GRASS, Blocks.SHORT_GRASS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item FERN = registerBlock(BlockItemIds.FERN, Blocks.FERN, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item BUSH = registerBlock(BlockItemIds.BUSH, Blocks.BUSH, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW));
   public static final Item RED_SHRUB = registerBlock(
      BlockItemIds.RED_SHRUB, Blocks.RED_SHRUB, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item AZALEA = registerBlock(BlockItemIds.AZALEA, Blocks.AZALEA, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item FLOWERING_AZALEA = registerBlock(
      BlockItemIds.FLOWERING_AZALEA, Blocks.FLOWERING_AZALEA, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item DEAD_BUSH = registerBlock(BlockItemIds.DEAD_BUSH, Blocks.DEAD_BUSH);
   public static final Item FIREFLY_BUSH = registerBlock(
      BlockItemIds.FIREFLY_BUSH, Blocks.FIREFLY_BUSH, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item DRY_SHORT_GRASS = registerBlock(
      BlockItemIds.SHORT_DRY_GRASS, Blocks.SHORT_DRY_GRASS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item DRY_TALL_GRASS = registerBlock(
      BlockItemIds.TALL_DRY_GRASS, Blocks.TALL_DRY_GRASS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item SEAGRASS = registerBlock(BlockItemIds.SEAGRASS, Blocks.SEAGRASS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW));
   public static final Item SEA_PICKLE = registerBlock(
      BlockItemIds.SEA_PICKLE, Blocks.SEA_PICKLE, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final ColorCollection<Item> WOOL = ColorCollection.registerBlockItems(
      BlockItemIds.WOOL, Blocks.WOOL, (id, block, var2) -> registerBlock(id, block)
   );
   public static final ColorCollection<Item> WOOL_STAIRS = ColorCollection.registerBlockItems(
      BlockItemIds.WOOL_STAIRS, Blocks.WOOL_STAIRS, (id, block, var2) -> registerBlock(id, block)
   );
   public static final ColorCollection<Item> WOOL_SLAB = ColorCollection.registerBlockItems(
      BlockItemIds.WOOL_SLAB, Blocks.WOOL_SLAB, (id, block, var2) -> registerBlock(id, block)
   );
   public static final Item DANDELION = registerBlock(
      BlockItemIds.DANDELION, Blocks.DANDELION, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item GOLDEN_DANDELION = registerBlock(BlockItemIds.GOLDEN_DANDELION, Blocks.GOLDEN_DANDELION);
   public static final Item OPEN_EYEBLOSSOM = registerBlock(
      BlockItemIds.OPEN_EYEBLOSSOM, Blocks.OPEN_EYEBLOSSOM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item CLOSED_EYEBLOSSOM = registerBlock(
      BlockItemIds.CLOSED_EYEBLOSSOM, Blocks.CLOSED_EYEBLOSSOM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item POPPY = registerBlock(BlockItemIds.POPPY, Blocks.POPPY, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item BLUE_ORCHID = registerBlock(
      BlockItemIds.BLUE_ORCHID, Blocks.BLUE_ORCHID, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item ALLIUM = registerBlock(BlockItemIds.ALLIUM, Blocks.ALLIUM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item AZURE_BLUET = registerBlock(
      BlockItemIds.AZURE_BLUET, Blocks.AZURE_BLUET, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item RED_TULIP = registerBlock(
      BlockItemIds.RED_TULIP, Blocks.RED_TULIP, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item ORANGE_TULIP = registerBlock(
      BlockItemIds.ORANGE_TULIP, Blocks.ORANGE_TULIP, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item WHITE_TULIP = registerBlock(
      BlockItemIds.WHITE_TULIP, Blocks.WHITE_TULIP, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item PINK_TULIP = registerBlock(
      BlockItemIds.PINK_TULIP, Blocks.PINK_TULIP, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item OXEYE_DAISY = registerBlock(
      BlockItemIds.OXEYE_DAISY, Blocks.OXEYE_DAISY, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item CORNFLOWER = registerBlock(
      BlockItemIds.CORNFLOWER, Blocks.CORNFLOWER, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item LILY_OF_THE_VALLEY = registerBlock(
      BlockItemIds.LILY_OF_THE_VALLEY, Blocks.LILY_OF_THE_VALLEY, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item WITHER_ROSE = registerBlock(
      BlockItemIds.WITHER_ROSE, Blocks.WITHER_ROSE, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item TORCHFLOWER = registerBlock(
      BlockItemIds.TORCHFLOWER, Blocks.TORCHFLOWER, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item PITCHER_PLANT = registerBlock(
      BlockItemIds.PITCHER_PLANT, Blocks.PITCHER_PLANT, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item SPORE_BLOSSOM = registerBlock(
      BlockItemIds.SPORE_BLOSSOM, Blocks.SPORE_BLOSSOM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item BROWN_MUSHROOM = registerBlock(
      BlockItemIds.BROWN_MUSHROOM, Blocks.BROWN_MUSHROOM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item RED_MUSHROOM = registerBlock(
      BlockItemIds.RED_MUSHROOM, Blocks.RED_MUSHROOM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item SHELF_MUSHROOM = registerBlock(
      BlockItemIds.SHELF_MUSHROOM, Blocks.SHELF_MUSHROOM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item CRIMSON_FUNGUS = registerBlock(
      BlockItemIds.CRIMSON_FUNGUS, Blocks.CRIMSON_FUNGUS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item WARPED_FUNGUS = registerBlock(
      BlockItemIds.WARPED_FUNGUS, Blocks.WARPED_FUNGUS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item CRIMSON_ROOTS = registerBlock(
      BlockItemIds.CRIMSON_ROOTS, Blocks.CRIMSON_ROOTS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item WARPED_ROOTS = registerBlock(
      BlockItemIds.WARPED_ROOTS, Blocks.WARPED_ROOTS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item NETHER_SPROUTS = registerBlock(
      BlockItemIds.NETHER_SPROUTS, Blocks.NETHER_SPROUTS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item WEEPING_VINES = registerBlock(
      BlockItemIds.WEEPING_VINES, Blocks.WEEPING_VINES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item TWISTING_VINES = registerBlock(
      BlockItemIds.TWISTING_VINES, Blocks.TWISTING_VINES, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item SUGAR_CANE = registerBlock(
      BlockItemIds.SUGAR_CANE, Blocks.SUGAR_CANE, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item KELP = registerBlock(BlockItemIds.KELP, Blocks.KELP, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW));
   public static final Item PINK_PETALS = registerBlock(
      BlockItemIds.PINK_PETALS, Blocks.PINK_PETALS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item WILDFLOWERS = registerBlock(
      BlockItemIds.WILDFLOWERS, Blocks.WILDFLOWERS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item LEAF_LITTER = registerBlock(
      BlockItemIds.LEAF_LITTER, Blocks.LEAF_LITTER, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item MOSS_CARPET = registerBlock(
      BlockItemIds.MOSS_CARPET, Blocks.MOSS_CARPET, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item MOSS_BLOCK = registerBlock(
      BlockItemIds.MOSS_BLOCK, Blocks.MOSS_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item PALE_MOSS_CARPET = registerBlock(
      BlockItemIds.PALE_MOSS_CARPET, Blocks.PALE_MOSS_CARPET, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item PALE_HANGING_MOSS = registerBlock(
      BlockItemIds.PALE_HANGING_MOSS, Blocks.PALE_HANGING_MOSS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item PALE_MOSS_BLOCK = registerBlock(
      BlockItemIds.PALE_MOSS_BLOCK, Blocks.PALE_MOSS_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item HANGING_ROOTS = registerBlock(
      BlockItemIds.HANGING_ROOTS, Blocks.HANGING_ROOTS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BIG_DRIPLEAF = registerBlock(
      BlockItemIds.BIG_DRIPLEAF, Blocks.BIG_DRIPLEAF, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM), Blocks.BIG_DRIPLEAF_STEM
   );
   public static final Item SMALL_DRIPLEAF = registerBlock(
      BlockItemIds.SMALL_DRIPLEAF, Blocks.SMALL_DRIPLEAF, DoubleHighBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BAMBOO = registerBlock(BlockItemIds.BAMBOO, Blocks.BAMBOO);
   public static final Item OAK_SLAB = registerBlock(BlockItemIds.OAK_SLAB, Blocks.OAK_SLAB);
   public static final Item SPRUCE_SLAB = registerBlock(BlockItemIds.SPRUCE_SLAB, Blocks.SPRUCE_SLAB);
   public static final Item BIRCH_SLAB = registerBlock(BlockItemIds.BIRCH_SLAB, Blocks.BIRCH_SLAB);
   public static final Item JUNGLE_SLAB = registerBlock(BlockItemIds.JUNGLE_SLAB, Blocks.JUNGLE_SLAB);
   public static final Item ACACIA_SLAB = registerBlock(BlockItemIds.ACACIA_SLAB, Blocks.ACACIA_SLAB);
   public static final Item CHERRY_SLAB = registerBlock(BlockItemIds.CHERRY_SLAB, Blocks.CHERRY_SLAB);
   public static final Item DARK_OAK_SLAB = registerBlock(BlockItemIds.DARK_OAK_SLAB, Blocks.DARK_OAK_SLAB);
   public static final Item PALE_OAK_SLAB = registerBlock(BlockItemIds.PALE_OAK_SLAB, Blocks.PALE_OAK_SLAB);
   public static final Item MANGROVE_SLAB = registerBlock(BlockItemIds.MANGROVE_SLAB, Blocks.MANGROVE_SLAB);
   public static final Item POPLAR_SLAB = registerBlock(BlockItemIds.POPLAR_SLAB, Blocks.POPLAR_SLAB);
   public static final Item BAMBOO_SLAB = registerBlock(BlockItemIds.BAMBOO_SLAB, Blocks.BAMBOO_SLAB);
   public static final Item BAMBOO_MOSAIC_SLAB = registerBlock(BlockItemIds.BAMBOO_MOSAIC_SLAB, Blocks.BAMBOO_MOSAIC_SLAB);
   public static final Item CRIMSON_SLAB = registerBlock(BlockItemIds.CRIMSON_SLAB, Blocks.CRIMSON_SLAB);
   public static final Item WARPED_SLAB = registerBlock(BlockItemIds.WARPED_SLAB, Blocks.WARPED_SLAB);
   public static final Item STONE_SLAB = registerBlock(BlockItemIds.STONE_SLAB, Blocks.STONE_SLAB);
   public static final Item SMOOTH_STONE_SLAB = registerBlock(BlockItemIds.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE_SLAB);
   public static final Item SANDSTONE_SLAB = registerBlock(BlockItemIds.SANDSTONE_SLAB, Blocks.SANDSTONE_SLAB);
   public static final Item CUT_STANDSTONE_SLAB = registerBlock(BlockItemIds.CUT_SANDSTONE_SLAB, Blocks.CUT_SANDSTONE_SLAB);
   public static final Item PETRIFIED_OAK_SLAB = registerBlock(BlockItemIds.PETRIFIED_OAK_SLAB, Blocks.PETRIFIED_OAK_SLAB);
   public static final Item COBBLESTONE_SLAB = registerBlock(BlockItemIds.COBBLESTONE_SLAB, Blocks.COBBLESTONE_SLAB);
   public static final Item BRICK_SLAB = registerBlock(BlockItemIds.BRICK_SLAB, Blocks.BRICK_SLAB);
   public static final Item STONE_BRICK_SLAB = registerBlock(BlockItemIds.STONE_BRICK_SLAB, Blocks.STONE_BRICK_SLAB);
   public static final Item MUD_BRICK_SLAB = registerBlock(BlockItemIds.MUD_BRICK_SLAB, Blocks.MUD_BRICK_SLAB);
   public static final Item NETHER_BRICK_SLAB = registerBlock(BlockItemIds.NETHER_BRICK_SLAB, Blocks.NETHER_BRICK_SLAB);
   public static final Item QUARTZ_SLAB = registerBlock(BlockItemIds.QUARTZ_SLAB, Blocks.QUARTZ_SLAB);
   public static final Item RED_SANDSTONE_SLAB = registerBlock(BlockItemIds.RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_SLAB);
   public static final Item CUT_RED_SANDSTONE_SLAB = registerBlock(BlockItemIds.CUT_RED_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE_SLAB);
   public static final Item PURPUR_SLAB = registerBlock(BlockItemIds.PURPUR_SLAB, Blocks.PURPUR_SLAB);
   public static final Item PRISMARINE_SLAB = registerBlock(BlockItemIds.PRISMARINE_SLAB, Blocks.PRISMARINE_SLAB);
   public static final Item PRISMARINE_BRICK_SLAB = registerBlock(BlockItemIds.PRISMARINE_BRICK_SLAB, Blocks.PRISMARINE_BRICK_SLAB);
   public static final Item DARK_PRISMARINE_SLAB = registerBlock(BlockItemIds.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE_SLAB);
   public static final Item SMOOTH_QUARTZ = registerBlock(BlockItemIds.SMOOTH_QUARTZ, Blocks.SMOOTH_QUARTZ);
   public static final Item SMOOTH_RED_SANDSTONE = registerBlock(BlockItemIds.SMOOTH_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE);
   public static final Item SMOOTH_SANDSTONE = registerBlock(BlockItemIds.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE);
   public static final Item SMOOTH_STONE = registerBlock(BlockItemIds.SMOOTH_STONE, Blocks.SMOOTH_STONE);
   public static final Item BRICKS = registerBlock(BlockItemIds.BRICKS, Blocks.BRICKS);
   public static final Item ACACIA_SHELF = registerBlock(
      BlockItemIds.ACACIA_SHELF, Blocks.ACACIA_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item BAMBOO_SHELF = registerBlock(
      BlockItemIds.BAMBOO_SHELF, Blocks.BAMBOO_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item BIRCH_SHELF = registerBlock(
      BlockItemIds.BIRCH_SHELF, Blocks.BIRCH_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item CHERRY_SHELF = registerBlock(
      BlockItemIds.CHERRY_SHELF, Blocks.CHERRY_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item CRIMSON_SHELF = registerBlock(
      BlockItemIds.CRIMSON_SHELF, Blocks.CRIMSON_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item DARK_OAK_SHELF = registerBlock(
      BlockItemIds.DARK_OAK_SHELF, Blocks.DARK_OAK_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item JUNGLE_SHELF = registerBlock(
      BlockItemIds.JUNGLE_SHELF, Blocks.JUNGLE_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item MANGROVE_SHELF = registerBlock(
      BlockItemIds.MANGROVE_SHELF, Blocks.MANGROVE_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item POPLAR_SHELF = registerBlock(
      BlockItemIds.POPLAR_SHELF, Blocks.POPLAR_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item OAK_SHELF = registerBlock(
      BlockItemIds.OAK_SHELF, Blocks.OAK_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item PALE_OAK_SHELF = registerBlock(
      BlockItemIds.PALE_OAK_SHELF, Blocks.PALE_OAK_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item SPRUCE_SHELF = registerBlock(
      BlockItemIds.SPRUCE_SHELF, Blocks.SPRUCE_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item WARPED_SHELF = registerBlock(
      BlockItemIds.WARPED_SHELF, Blocks.WARPED_SHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item BOOKSHELF = registerBlock(BlockItemIds.BOOKSHELF, Blocks.BOOKSHELF);
   public static final Item CHISELED_BOOKSHELF = registerBlock(
      BlockItemIds.CHISELED_BOOKSHELF, Blocks.CHISELED_BOOKSHELF, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item DECORATED_POT = registerBlock(
      BlockItemIds.DECORATED_POT,
      Blocks.DECORATED_POT,
      new Item.Properties().component(DataComponents.POT_DECORATIONS, PotDecorations.EMPTY).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item MOSSY_COBBLESTONE = registerBlock(BlockItemIds.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
   public static final Item OBSIDIAN = registerBlock(BlockItemIds.OBSIDIAN, Blocks.OBSIDIAN);
   public static final Item TORCH = registerBlock(
      BlockItemIds.TORCH, Blocks.TORCH, (b, p) -> new StandingAndWallBlockItem(b, Blocks.WALL_TORCH, Direction.DOWN, p)
   );
   public static final Item END_ROD = registerBlock(BlockItemIds.END_ROD, Blocks.END_ROD);
   public static final Item CHORUS_PLANT = registerBlock(BlockItemIds.CHORUS_PLANT, Blocks.CHORUS_PLANT);
   public static final Item CHORUS_FLOWER = registerBlock(BlockItemIds.CHORUS_FLOWER, Blocks.CHORUS_FLOWER);
   public static final Item PURPUR_BLOCK = registerBlock(BlockItemIds.PURPUR_BLOCK, Blocks.PURPUR_BLOCK);
   public static final Item PURPUR_PILLAR = registerBlock(BlockItemIds.PURPUR_PILLAR, Blocks.PURPUR_PILLAR);
   public static final Item PURPUR_STAIRS = registerBlock(BlockItemIds.PURPUR_STAIRS, Blocks.PURPUR_STAIRS);
   public static final Item SPAWNER = registerBlock(BlockItemIds.SPAWNER, Blocks.SPAWNER);
   public static final Item CREAKING_HEART = registerBlock(BlockItemIds.CREAKING_HEART, Blocks.CREAKING_HEART);
   public static final Item CHEST = registerBlock(BlockItemIds.CHEST, Blocks.CHEST, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
   public static final Item CRAFTING_TABLE = registerBlock(BlockItemIds.CRAFTING_TABLE, Blocks.CRAFTING_TABLE);
   public static final Item FARMLAND = registerBlock(BlockItemIds.FARMLAND, Blocks.FARMLAND);
   public static final Item FURNACE = registerBlock(
      BlockItemIds.FURNACE, Blocks.FURNACE, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item LADDER = registerBlock(BlockItemIds.LADDER, Blocks.LADDER);
   public static final Item COBBLESTONE_STAIRS = registerBlock(BlockItemIds.COBBLESTONE_STAIRS, Blocks.COBBLESTONE_STAIRS);
   public static final Item SNOW = registerBlock(BlockItemIds.SNOW, Blocks.SNOW);
   public static final Item ICE = registerBlock(BlockItemIds.ICE, Blocks.ICE);
   public static final Item SNOW_BLOCK = registerBlock(BlockItemIds.SNOW_BLOCK, Blocks.SNOW_BLOCK);
   public static final Item CACTUS = registerBlock(
      BlockItemIds.CACTUS, Blocks.CACTUS, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item CACTUS_FLOWER = registerBlock(
      BlockItemIds.CACTUS_FLOWER, Blocks.CACTUS_FLOWER, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item CLAY = registerBlock(BlockItemIds.CLAY, Blocks.CLAY);
   public static final Item JUKEBOX = registerBlock(BlockItemIds.JUKEBOX, Blocks.JUKEBOX);
   public static final Item OAK_FENCE = registerBlock(BlockItemIds.OAK_FENCE, Blocks.OAK_FENCE);
   public static final Item SPRUCE_FENCE = registerBlock(BlockItemIds.SPRUCE_FENCE, Blocks.SPRUCE_FENCE);
   public static final Item BIRCH_FENCE = registerBlock(BlockItemIds.BIRCH_FENCE, Blocks.BIRCH_FENCE);
   public static final Item JUNGLE_FENCE = registerBlock(BlockItemIds.JUNGLE_FENCE, Blocks.JUNGLE_FENCE);
   public static final Item ACACIA_FENCE = registerBlock(BlockItemIds.ACACIA_FENCE, Blocks.ACACIA_FENCE);
   public static final Item CHERRY_FENCE = registerBlock(BlockItemIds.CHERRY_FENCE, Blocks.CHERRY_FENCE);
   public static final Item DARK_OAK_FENCE = registerBlock(BlockItemIds.DARK_OAK_FENCE, Blocks.DARK_OAK_FENCE);
   public static final Item PALE_OAK_FENCE = registerBlock(BlockItemIds.PALE_OAK_FENCE, Blocks.PALE_OAK_FENCE);
   public static final Item MANGROVE_FENCE = registerBlock(BlockItemIds.MANGROVE_FENCE, Blocks.MANGROVE_FENCE);
   public static final Item POPLAR_FENCE = registerBlock(BlockItemIds.POPLAR_FENCE, Blocks.POPLAR_FENCE);
   public static final Item BAMBOO_FENCE = registerBlock(BlockItemIds.BAMBOO_FENCE, Blocks.BAMBOO_FENCE);
   public static final Item CRIMSON_FENCE = registerBlock(BlockItemIds.CRIMSON_FENCE, Blocks.CRIMSON_FENCE);
   public static final Item WARPED_FENCE = registerBlock(BlockItemIds.WARPED_FENCE, Blocks.WARPED_FENCE);
   public static final Item PUMPKIN = registerBlock(BlockItemIds.PUMPKIN, Blocks.PUMPKIN, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item CARVED_PUMPKIN = registerBlock(
      BlockItemIds.CARVED_PUMPKIN,
      Blocks.CARVED_PUMPKIN,
      p -> Waypoint.addHideAttribute(p)
         .component(
            DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.HEAD).setSwappable(false).setCameraOverlay(Identifier.withDefaultNamespace("misc/pumpkinblur")).build()
         )
         .compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item JACK_O_LANTERN = registerBlock(BlockItemIds.JACK_O_LANTERN, Blocks.JACK_O_LANTERN);
   public static final Item NETHERRACK = registerBlock(BlockItemIds.NETHERRACK, Blocks.NETHERRACK);
   public static final Item SOUL_SAND = registerBlock(BlockItemIds.SOUL_SAND, Blocks.SOUL_SAND);
   public static final Item SOUL_SOIL = registerBlock(BlockItemIds.SOUL_SOIL, Blocks.SOUL_SOIL);
   public static final Item BASALT = registerBlock(BlockItemIds.BASALT, Blocks.BASALT);
   public static final Item POLISHED_BASALT = registerBlock(BlockItemIds.POLISHED_BASALT, Blocks.POLISHED_BASALT);
   public static final Item SMOOTH_BASALT = registerBlock(BlockItemIds.SMOOTH_BASALT, Blocks.SMOOTH_BASALT);
   public static final Item SOUL_TORCH = registerBlock(
      BlockItemIds.SOUL_TORCH, Blocks.SOUL_TORCH, (b, p) -> new StandingAndWallBlockItem(b, Blocks.SOUL_WALL_TORCH, Direction.DOWN, p)
   );
   public static final Item COPPER_TORCH = registerBlock(
      BlockItemIds.COPPER_TORCH, Blocks.COPPER_TORCH, (b, p) -> new StandingAndWallBlockItem(b, Blocks.COPPER_WALL_TORCH, Direction.DOWN, p)
   );
   public static final Item GLOWSTONE = registerBlock(BlockItemIds.GLOWSTONE, Blocks.GLOWSTONE);
   public static final Item INFESTED_STONE = registerBlock(BlockItemIds.INFESTED_STONE, Blocks.INFESTED_STONE);
   public static final Item INFESTED_COBBLESTONE = registerBlock(BlockItemIds.INFESTED_COBBLESTONE, Blocks.INFESTED_COBBLESTONE);
   public static final Item INFESTED_STONE_BRICKS = registerBlock(BlockItemIds.INFESTED_STONE_BRICKS, Blocks.INFESTED_STONE_BRICKS);
   public static final Item INFESTED_MOSSY_STONE_BRICKS = registerBlock(BlockItemIds.INFESTED_MOSSY_STONE_BRICKS, Blocks.INFESTED_MOSSY_STONE_BRICKS);
   public static final Item INFESTED_CRACKED_STONE_BRICKS = registerBlock(BlockItemIds.INFESTED_CRACKED_STONE_BRICKS, Blocks.INFESTED_CRACKED_STONE_BRICKS);
   public static final Item INFESTED_CHISELED_STONE_BRICKS = registerBlock(BlockItemIds.INFESTED_CHISELED_STONE_BRICKS, Blocks.INFESTED_CHISELED_STONE_BRICKS);
   public static final Item INFESTED_DEEPSLATE = registerBlock(BlockItemIds.INFESTED_DEEPSLATE, Blocks.INFESTED_DEEPSLATE);
   public static final Item STONE_BRICKS = registerBlock(BlockItemIds.STONE_BRICKS, Blocks.STONE_BRICKS);
   public static final Item MOSSY_STONE_BRICKS = registerBlock(BlockItemIds.MOSSY_STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
   public static final Item CRACKED_STONE_BRICKS = registerBlock(BlockItemIds.CRACKED_STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
   public static final Item CHISELED_STONE_BRICKS = registerBlock(BlockItemIds.CHISELED_STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS);
   public static final Item PACKED_MUD = registerBlock(BlockItemIds.PACKED_MUD, Blocks.PACKED_MUD);
   public static final Item MUD_BRICKS = registerBlock(BlockItemIds.MUD_BRICKS, Blocks.MUD_BRICKS);
   public static final Item DEEPSLATE_BRICKS = registerBlock(BlockItemIds.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);
   public static final Item CRACKED_DEEPSLATE_BRICKS = registerBlock(BlockItemIds.CRACKED_DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS);
   public static final Item DEEPSLATE_TILES = registerBlock(BlockItemIds.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILES);
   public static final Item CRACKED_DEEPSLATE_TILES = registerBlock(BlockItemIds.CRACKED_DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES);
   public static final Item CHISELED_DEEPSLATE = registerBlock(BlockItemIds.CHISELED_DEEPSLATE, Blocks.CHISELED_DEEPSLATE);
   public static final Item REINFORCED_DEEPSLATE = registerBlock(BlockItemIds.REINFORCED_DEEPSLATE, Blocks.REINFORCED_DEEPSLATE);
   public static final Item BROWN_MUSHROOM_BLOCK = registerBlock(
      BlockItemIds.BROWN_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item RED_MUSHROOM_BLOCK = registerBlock(
      BlockItemIds.RED_MUSHROOM_BLOCK, Blocks.RED_MUSHROOM_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item MUSHROOM_STEM = registerBlock(
      BlockItemIds.MUSHROOM_STEM, Blocks.MUSHROOM_STEM, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item IRON_BARS = registerBlock(BlockItemIds.IRON_BARS, Blocks.IRON_BARS);
   public static final WeatheringCopperCollection<Item> COPPER_BARS = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_BARS, Blocks.COPPER_BARS, Items::registerBlock
   );
   public static final Item IRON_CHAIN = registerBlock(BlockItemIds.IRON_CHAIN, Blocks.IRON_CHAIN);
   public static final WeatheringCopperCollection<Item> COPPER_CHAIN = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_CHAIN, Blocks.COPPER_CHAIN, Items::registerBlock
   );
   public static final Item GLASS_PANE = registerBlock(BlockItemIds.GLASS_PANE, Blocks.GLASS_PANE);
   public static final Item MELON = registerBlock(BlockItemIds.MELON, Blocks.MELON, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item VINE = registerBlock(BlockItemIds.VINE, Blocks.VINE, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM));
   public static final Item GLOW_LICHEN = registerBlock(
      BlockItemIds.GLOW_LICHEN, Blocks.GLOW_LICHEN, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item RESIN_CLUMP = registerItem(BlockItemIds.RESIN_CLUMP, createBlockItemWithCustomItemName(Blocks.RESIN_CLUMP));
   public static final Item RESIN_BLOCK = registerBlock(BlockItemIds.RESIN_BLOCK, Blocks.RESIN_BLOCK);
   public static final Item RESIN_BRICKS = registerBlock(BlockItemIds.RESIN_BRICKS, Blocks.RESIN_BRICKS);
   public static final Item RESIN_BRICK_STAIRS = registerBlock(BlockItemIds.RESIN_BRICK_STAIRS, Blocks.RESIN_BRICK_STAIRS);
   public static final Item RESIN_BRICK_SLAB = registerBlock(BlockItemIds.RESIN_BRICK_SLAB, Blocks.RESIN_BRICK_SLAB);
   public static final Item RESIN_BRICK_WALL = registerBlock(BlockItemIds.RESIN_BRICK_WALL, Blocks.RESIN_BRICK_WALL);
   public static final Item CHISELED_RESIN_BRICKS = registerBlock(BlockItemIds.CHISELED_RESIN_BRICKS, Blocks.CHISELED_RESIN_BRICKS);
   public static final Item BRICK_STAIRS = registerBlock(BlockItemIds.BRICK_STAIRS, Blocks.BRICK_STAIRS);
   public static final Item STONE_BRICK_STAIRS = registerBlock(BlockItemIds.STONE_BRICK_STAIRS, Blocks.STONE_BRICK_STAIRS);
   public static final Item MUD_BRICK_STAIRS = registerBlock(BlockItemIds.MUD_BRICK_STAIRS, Blocks.MUD_BRICK_STAIRS);
   public static final Item MYCELIUM = registerBlock(BlockItemIds.MYCELIUM, Blocks.MYCELIUM);
   public static final Item LILY_PAD = registerBlock(
      BlockItemIds.LILY_PAD, Blocks.LILY_PAD, PlaceOnWaterBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item NETHER_BRICKS = registerBlock(BlockItemIds.NETHER_BRICKS, Blocks.NETHER_BRICKS);
   public static final Item CRACKED_NETHER_BRICKS = registerBlock(BlockItemIds.CRACKED_NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS);
   public static final Item CHISELED_NETHER_BRICKS = registerBlock(BlockItemIds.CHISELED_NETHER_BRICKS, Blocks.CHISELED_NETHER_BRICKS);
   public static final Item NETHER_BRICK_FENCE = registerBlock(BlockItemIds.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_FENCE);
   public static final Item NETHER_BRICK_STAIRS = registerBlock(BlockItemIds.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICK_STAIRS);
   public static final Item SCULK = registerBlock(BlockItemIds.SCULK, Blocks.SCULK);
   public static final Item SCULK_VEIN = registerBlock(BlockItemIds.SCULK_VEIN, Blocks.SCULK_VEIN);
   public static final Item SCULK_CATALYST = registerBlock(BlockItemIds.SCULK_CATALYST, Blocks.SCULK_CATALYST);
   public static final Item SCULK_SHRIEKER = registerBlock(BlockItemIds.SCULK_SHRIEKER, Blocks.SCULK_SHRIEKER);
   public static final Item ENCHANTING_TABLE = registerBlock(BlockItemIds.ENCHANTING_TABLE, Blocks.ENCHANTING_TABLE);
   public static final Item END_PORTAL_FRAME = registerBlock(BlockItemIds.END_PORTAL_FRAME, Blocks.END_PORTAL_FRAME);
   public static final Item END_STONE = registerBlock(BlockItemIds.END_STONE, Blocks.END_STONE);
   public static final Item END_STONE_BRICKS = registerBlock(BlockItemIds.END_STONE_BRICKS, Blocks.END_STONE_BRICKS);
   public static final Item DRAGON_EGG = registerBlock(BlockItemIds.DRAGON_EGG, Blocks.DRAGON_EGG, new Item.Properties().rarity(Rarity.EPIC));
   public static final Item SANDSTONE_STAIRS = registerBlock(BlockItemIds.SANDSTONE_STAIRS, Blocks.SANDSTONE_STAIRS);
   public static final Item ENDER_CHEST = registerBlock(BlockItemIds.ENDER_CHEST, Blocks.ENDER_CHEST);
   public static final Item EMERALD_BLOCK = registerBlock(BlockItemIds.EMERALD_BLOCK, Blocks.EMERALD_BLOCK);
   public static final Item OAK_STAIRS = registerBlock(BlockItemIds.OAK_STAIRS, Blocks.OAK_STAIRS);
   public static final Item SPRUCE_STAIRS = registerBlock(BlockItemIds.SPRUCE_STAIRS, Blocks.SPRUCE_STAIRS);
   public static final Item BIRCH_STAIRS = registerBlock(BlockItemIds.BIRCH_STAIRS, Blocks.BIRCH_STAIRS);
   public static final Item JUNGLE_STAIRS = registerBlock(BlockItemIds.JUNGLE_STAIRS, Blocks.JUNGLE_STAIRS);
   public static final Item ACACIA_STAIRS = registerBlock(BlockItemIds.ACACIA_STAIRS, Blocks.ACACIA_STAIRS);
   public static final Item CHERRY_STAIRS = registerBlock(BlockItemIds.CHERRY_STAIRS, Blocks.CHERRY_STAIRS);
   public static final Item DARK_OAK_STAIRS = registerBlock(BlockItemIds.DARK_OAK_STAIRS, Blocks.DARK_OAK_STAIRS);
   public static final Item PALE_OAK_STAIRS = registerBlock(BlockItemIds.PALE_OAK_STAIRS, Blocks.PALE_OAK_STAIRS);
   public static final Item MANGROVE_STAIRS = registerBlock(BlockItemIds.MANGROVE_STAIRS, Blocks.MANGROVE_STAIRS);
   public static final Item POPLAR_STAIRS = registerBlock(BlockItemIds.POPLAR_STAIRS, Blocks.POPLAR_STAIRS);
   public static final Item BAMBOO_STAIRS = registerBlock(BlockItemIds.BAMBOO_STAIRS, Blocks.BAMBOO_STAIRS);
   public static final Item BAMBOO_MOSAIC_STAIRS = registerBlock(BlockItemIds.BAMBOO_MOSAIC_STAIRS, Blocks.BAMBOO_MOSAIC_STAIRS);
   public static final Item CRIMSON_STAIRS = registerBlock(BlockItemIds.CRIMSON_STAIRS, Blocks.CRIMSON_STAIRS);
   public static final Item WARPED_STAIRS = registerBlock(BlockItemIds.WARPED_STAIRS, Blocks.WARPED_STAIRS);
   public static final Item COMMAND_BLOCK = registerBlock(
      BlockItemIds.COMMAND_BLOCK, Blocks.COMMAND_BLOCK, GameMasterBlockItem::new, new Item.Properties().rarity(Rarity.EPIC)
   );
   public static final Item BEACON = registerBlock(BlockItemIds.BEACON, Blocks.BEACON, new Item.Properties().rarity(Rarity.RARE));
   public static final Item COBBLESTONE_WALL = registerBlock(BlockItemIds.COBBLESTONE_WALL, Blocks.COBBLESTONE_WALL);
   public static final Item MOSSY_COBBLESTONE_WALL = registerBlock(BlockItemIds.MOSSY_COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL);
   public static final Item BRICK_WALL = registerBlock(BlockItemIds.BRICK_WALL, Blocks.BRICK_WALL);
   public static final Item PRISMARINE_WALL = registerBlock(BlockItemIds.PRISMARINE_WALL, Blocks.PRISMARINE_WALL);
   public static final Item RED_SANDSTONE_WALL = registerBlock(BlockItemIds.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE_WALL);
   public static final Item MOSSY_STONE_BRICK_WALL = registerBlock(BlockItemIds.MOSSY_STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL);
   public static final Item GRANITE_WALL = registerBlock(BlockItemIds.GRANITE_WALL, Blocks.GRANITE_WALL);
   public static final Item STONE_BRICK_WALL = registerBlock(BlockItemIds.STONE_BRICK_WALL, Blocks.STONE_BRICK_WALL);
   public static final Item MUD_BRICK_WALL = registerBlock(BlockItemIds.MUD_BRICK_WALL, Blocks.MUD_BRICK_WALL);
   public static final Item NETHER_BRICK_WALL = registerBlock(BlockItemIds.NETHER_BRICK_WALL, Blocks.NETHER_BRICK_WALL);
   public static final Item ANDESITE_WALL = registerBlock(BlockItemIds.ANDESITE_WALL, Blocks.ANDESITE_WALL);
   public static final Item RED_NETHER_BRICK_WALL = registerBlock(BlockItemIds.RED_NETHER_BRICK_WALL, Blocks.RED_NETHER_BRICK_WALL);
   public static final Item SANDSTONE_WALL = registerBlock(BlockItemIds.SANDSTONE_WALL, Blocks.SANDSTONE_WALL);
   public static final Item END_STONE_BRICK_WALL = registerBlock(BlockItemIds.END_STONE_BRICK_WALL, Blocks.END_STONE_BRICK_WALL);
   public static final Item DIORITE_WALL = registerBlock(BlockItemIds.DIORITE_WALL, Blocks.DIORITE_WALL);
   public static final Item BLACKSTONE_WALL = registerBlock(BlockItemIds.BLACKSTONE_WALL, Blocks.BLACKSTONE_WALL);
   public static final Item POLISHED_BLACKSTONE_WALL = registerBlock(BlockItemIds.POLISHED_BLACKSTONE_WALL, Blocks.POLISHED_BLACKSTONE_WALL);
   public static final Item POLISHED_BLACKSTONE_BRICK_WALL = registerBlock(BlockItemIds.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
   public static final Item COBBLED_DEEPSLATE_WALL = registerBlock(BlockItemIds.COBBLED_DEEPSLATE_WALL, Blocks.COBBLED_DEEPSLATE_WALL);
   public static final Item POLISHED_DEEPSLATE_WALL = registerBlock(BlockItemIds.POLISHED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE_WALL);
   public static final Item DEEPSLATE_BRICK_WALL = registerBlock(BlockItemIds.DEEPSLATE_BRICK_WALL, Blocks.DEEPSLATE_BRICK_WALL);
   public static final Item DEEPSLATE_TILE_WALL = registerBlock(BlockItemIds.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_TILE_WALL);
   public static final Item ANVIL = registerBlock(BlockItemIds.ANVIL, Blocks.ANVIL);
   public static final Item CHIPPED_ANVIL = registerBlock(BlockItemIds.CHIPPED_ANVIL, Blocks.CHIPPED_ANVIL);
   public static final Item DAMAGED_ANVIL = registerBlock(BlockItemIds.DAMAGED_ANVIL, Blocks.DAMAGED_ANVIL);
   public static final Item CHISELED_QUARTZ_BLOCK = registerBlock(BlockItemIds.CHISELED_QUARTZ_BLOCK, Blocks.CHISELED_QUARTZ_BLOCK);
   public static final Item QUARTZ_BLOCK = registerBlock(BlockItemIds.QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK);
   public static final Item QUARTZ_BRICKS = registerBlock(BlockItemIds.QUARTZ_BRICKS, Blocks.QUARTZ_BRICKS);
   public static final Item QUARTZ_PILLAR = registerBlock(BlockItemIds.QUARTZ_PILLAR, Blocks.QUARTZ_PILLAR);
   public static final Item QUARTZ_STAIRS = registerBlock(BlockItemIds.QUARTZ_STAIRS, Blocks.QUARTZ_STAIRS);
   public static final ColorCollection<Item> DYED_TERRACOTTA = ColorCollection.registerBlockItems(
      BlockItemIds.DYED_TERRACOTTA, Blocks.DYED_TERRACOTTA, (id, block, var2) -> registerBlock(id, block)
   );
   public static final Item BARRIER = registerBlock(BlockItemIds.BARRIER, Blocks.BARRIER, new Item.Properties().rarity(Rarity.EPIC));
   public static final Item LIGHT = registerBlock(
      BlockItemIds.LIGHT,
      Blocks.LIGHT,
      p -> p.rarity(Rarity.EPIC).component(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(LightBlock.LEVEL, 15))
   );
   public static final Item HAY_BLOCK = registerBlock(
      BlockItemIds.HAY_BLOCK, Blocks.HAY_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final ColorCollection<Item> CARPET = ColorCollection.registerBlockItems(
      BlockItemIds.CARPET,
      Blocks.CARPET,
      (id, block, color) -> registerBlock(id, block, p -> p.component(DataComponents.EQUIPPABLE, Equippable.llamaSwag(color)))
   );
   public static final Item TERRACOTTA = registerBlock(BlockItemIds.TERRACOTTA, Blocks.TERRACOTTA);
   public static final Item PACKED_ICE = registerBlock(BlockItemIds.PACKED_ICE, Blocks.PACKED_ICE);
   public static final Item DIRT_PATH = registerBlock(BlockItemIds.DIRT_PATH, Blocks.DIRT_PATH);
   public static final Item SUNFLOWER = registerBlock(
      BlockItemIds.SUNFLOWER, Blocks.SUNFLOWER, DoubleHighBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item LILAC = registerBlock(
      BlockItemIds.LILAC, Blocks.LILAC, DoubleHighBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item ROSE_BUSH = registerBlock(
      BlockItemIds.ROSE_BUSH, Blocks.ROSE_BUSH, DoubleHighBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item PEONY = registerBlock(
      BlockItemIds.PEONY, Blocks.PEONY, DoubleHighBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item TALL_GRASS = registerBlock(
      BlockItemIds.TALL_GRASS, Blocks.TALL_GRASS, DoubleHighBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item LARGE_FERN = registerBlock(
      BlockItemIds.LARGE_FERN, Blocks.LARGE_FERN, DoubleHighBlockItem::new, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final ColorCollection<Item> STAINED_GLASS = ColorCollection.registerBlockItems(
      BlockItemIds.STAINED_GLASS, Blocks.STAINED_GLASS, (id, block, var2) -> registerBlock(id, block)
   );
   public static final ColorCollection<Item> STAINED_GLASS_PANE = ColorCollection.registerBlockItems(
      BlockItemIds.STAINED_GLASS_PANE, Blocks.STAINED_GLASS_PANE, (id, block, var2) -> registerBlock(id, block)
   );
   public static final Item PRISMARINE = registerBlock(BlockItemIds.PRISMARINE, Blocks.PRISMARINE);
   public static final Item PRISMARINE_BRICKS = registerBlock(BlockItemIds.PRISMARINE_BRICKS, Blocks.PRISMARINE_BRICKS);
   public static final Item DARK_PRISMARINE = registerBlock(BlockItemIds.DARK_PRISMARINE, Blocks.DARK_PRISMARINE);
   public static final Item PRISMARINE_STAIRS = registerBlock(BlockItemIds.PRISMARINE_STAIRS, Blocks.PRISMARINE_STAIRS);
   public static final Item PRISMARINE_BRICK_STAIRS = registerBlock(BlockItemIds.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE_BRICK_STAIRS);
   public static final Item DARK_PRISMARINE_STAIRS = registerBlock(BlockItemIds.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE_STAIRS);
   public static final Item SEA_LANTERN = registerBlock(BlockItemIds.SEA_LANTERN, Blocks.SEA_LANTERN);
   public static final Item RED_SANDSTONE = registerBlock(BlockItemIds.RED_SANDSTONE, Blocks.RED_SANDSTONE);
   public static final Item CHISELED_RED_SANDSTONE = registerBlock(BlockItemIds.CHISELED_RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE);
   public static final Item CUT_RED_SANDSTONE = registerBlock(BlockItemIds.CUT_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE);
   public static final Item RED_SANDSTONE_STAIRS = registerBlock(BlockItemIds.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE_STAIRS);
   public static final Item REPEATING_COMMAND_BLOCK = registerBlock(
      BlockItemIds.REPEATING_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK, GameMasterBlockItem::new, new Item.Properties().rarity(Rarity.EPIC)
   );
   public static final Item CHAIN_COMMAND_BLOCK = registerBlock(
      BlockItemIds.CHAIN_COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, GameMasterBlockItem::new, new Item.Properties().rarity(Rarity.EPIC)
   );
   public static final Item MAGMA_BLOCK = registerBlock(BlockItemIds.MAGMA_BLOCK, Blocks.MAGMA_BLOCK);
   public static final Item NETHER_WART_BLOCK = registerBlock(
      BlockItemIds.NETHER_WART_BLOCK, Blocks.NETHER_WART_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item WARPED_WART_BLOCK = registerBlock(
      BlockItemIds.WARPED_WART_BLOCK, Blocks.WARPED_WART_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item RED_NETHER_BRICKS = registerBlock(BlockItemIds.RED_NETHER_BRICKS, Blocks.RED_NETHER_BRICKS);
   public static final Item BONE_BLOCK = registerBlock(BlockItemIds.BONE_BLOCK, Blocks.BONE_BLOCK);
   public static final Item STRUCTURE_VOID = registerBlock(BlockItemIds.STRUCTURE_VOID, Blocks.STRUCTURE_VOID, new Item.Properties().rarity(Rarity.EPIC));
   public static final Item SHULKER_BOX = registerBlock(
      BlockItemIds.SHULKER_BOX, Blocks.SHULKER_BOX, new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final ColorCollection<Item> DYED_SHULKER_BOX = ColorCollection.registerBlockItems(
      BlockItemIds.DYED_SHULKER_BOX,
      Blocks.DYED_SHULKER_BOX,
      (id, block, var2) -> registerBlock(id, block, new Item.Properties().stacksTo(1).component(DataComponents.CONTAINER, ItemContainerContents.EMPTY))
   );
   public static final ColorCollection<Item> GLAZED_TERRACOTTA = ColorCollection.registerBlockItems(
      BlockItemIds.GLAZED_TERRACOTTA, Blocks.GLAZED_TERRACOTTA, (id, block, var2) -> registerBlock(id, block)
   );
   public static final ColorCollection<Item> CONCRETE = ColorCollection.registerBlockItems(
      BlockItemIds.CONCRETE, Blocks.CONCRETE, (id, block, var2) -> registerBlock(id, block)
   );
   public static final ColorCollection<Item> CONCRETE_POWDER = ColorCollection.registerBlockItems(
      BlockItemIds.CONCRETE_POWDER, Blocks.CONCRETE_POWDER, (id, block, var2) -> registerBlock(id, block)
   );
   public static final Item TURTLE_EGG = registerBlock(BlockItemIds.TURTLE_EGG, Blocks.TURTLE_EGG);
   public static final Item SNIFFER_EGG = registerBlock(BlockItemIds.SNIFFER_EGG, Blocks.SNIFFER_EGG, p -> p.rarity(Rarity.UNCOMMON));
   public static final Item DRIED_GHAST = registerBlock(BlockItemIds.DRIED_GHAST, Blocks.DRIED_GHAST);
   public static final Item DEAD_TUBE_CORAL_BLOCK = registerBlock(BlockItemIds.DEAD_TUBE_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK);
   public static final Item DEAD_BRAIN_CORAL_BLOCK = registerBlock(BlockItemIds.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_BRAIN_CORAL_BLOCK);
   public static final Item DEAD_BUBBLE_CORAL_BLOCK = registerBlock(BlockItemIds.DEAD_BUBBLE_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK);
   public static final Item DEAD_FIRE_CORAL_BLOCK = registerBlock(BlockItemIds.DEAD_FIRE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK);
   public static final Item DEAD_HORN_CORAL_BLOCK = registerBlock(BlockItemIds.DEAD_HORN_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK);
   public static final Item TUBE_CORAL_BLOCK = registerBlock(BlockItemIds.TUBE_CORAL_BLOCK, Blocks.TUBE_CORAL_BLOCK);
   public static final Item BRAIN_CORAL_BLOCK = registerBlock(BlockItemIds.BRAIN_CORAL_BLOCK, Blocks.BRAIN_CORAL_BLOCK);
   public static final Item BUBBLE_CORAL_BLOCK = registerBlock(BlockItemIds.BUBBLE_CORAL_BLOCK, Blocks.BUBBLE_CORAL_BLOCK);
   public static final Item FIRE_CORAL_BLOCK = registerBlock(BlockItemIds.FIRE_CORAL_BLOCK, Blocks.FIRE_CORAL_BLOCK);
   public static final Item HORN_CORAL_BLOCK = registerBlock(BlockItemIds.HORN_CORAL_BLOCK, Blocks.HORN_CORAL_BLOCK);
   public static final Item TUBE_CORAL = registerBlock(BlockItemIds.TUBE_CORAL, Blocks.TUBE_CORAL);
   public static final Item BRAIN_CORAL = registerBlock(BlockItemIds.BRAIN_CORAL, Blocks.BRAIN_CORAL);
   public static final Item BUBBLE_CORAL = registerBlock(BlockItemIds.BUBBLE_CORAL, Blocks.BUBBLE_CORAL);
   public static final Item FIRE_CORAL = registerBlock(BlockItemIds.FIRE_CORAL, Blocks.FIRE_CORAL);
   public static final Item HORN_CORAL = registerBlock(BlockItemIds.HORN_CORAL, Blocks.HORN_CORAL);
   public static final Item DEAD_BRAIN_CORAL = registerBlock(BlockItemIds.DEAD_BRAIN_CORAL, Blocks.DEAD_BRAIN_CORAL);
   public static final Item DEAD_BUBBLE_CORAL = registerBlock(BlockItemIds.DEAD_BUBBLE_CORAL, Blocks.DEAD_BUBBLE_CORAL);
   public static final Item DEAD_FIRE_CORAL = registerBlock(BlockItemIds.DEAD_FIRE_CORAL, Blocks.DEAD_FIRE_CORAL);
   public static final Item DEAD_HORN_CORAL = registerBlock(BlockItemIds.DEAD_HORN_CORAL, Blocks.DEAD_HORN_CORAL);
   public static final Item DEAD_TUBE_CORAL = registerBlock(BlockItemIds.DEAD_TUBE_CORAL, Blocks.DEAD_TUBE_CORAL);
   public static final Item TUBE_CORAL_FAN = registerBlock(
      BlockItemIds.TUBE_CORAL_FAN, Blocks.TUBE_CORAL_FAN, (b, p) -> new StandingAndWallBlockItem(b, Blocks.TUBE_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item BRAIN_CORAL_FAN = registerBlock(
      BlockItemIds.BRAIN_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, (b, p) -> new StandingAndWallBlockItem(b, Blocks.BRAIN_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item BUBBLE_CORAL_FAN = registerBlock(
      BlockItemIds.BUBBLE_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, (b, p) -> new StandingAndWallBlockItem(b, Blocks.BUBBLE_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item FIRE_CORAL_FAN = registerBlock(
      BlockItemIds.FIRE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, (b, p) -> new StandingAndWallBlockItem(b, Blocks.FIRE_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item HORN_CORAL_FAN = registerBlock(
      BlockItemIds.HORN_CORAL_FAN, Blocks.HORN_CORAL_FAN, (b, p) -> new StandingAndWallBlockItem(b, Blocks.HORN_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item DEAD_TUBE_CORAL_FAN = registerBlock(
      BlockItemIds.DEAD_TUBE_CORAL_FAN,
      Blocks.DEAD_TUBE_CORAL_FAN,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.DEAD_TUBE_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item DEAD_BRAIN_CORAL_FAN = registerBlock(
      BlockItemIds.DEAD_BRAIN_CORAL_FAN,
      Blocks.DEAD_BRAIN_CORAL_FAN,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.DEAD_BRAIN_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item DEAD_BUBBLE_CORAL_FAN = registerBlock(
      BlockItemIds.DEAD_BUBBLE_CORAL_FAN,
      Blocks.DEAD_BUBBLE_CORAL_FAN,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.DEAD_BUBBLE_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item DEAD_FIRE_CORAL_FAN = registerBlock(
      BlockItemIds.DEAD_FIRE_CORAL_FAN,
      Blocks.DEAD_FIRE_CORAL_FAN,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.DEAD_FIRE_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item DEAD_HORN_CORAL_FAN = registerBlock(
      BlockItemIds.DEAD_HORN_CORAL_FAN,
      Blocks.DEAD_HORN_CORAL_FAN,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.DEAD_HORN_CORAL_WALL_FAN, Direction.DOWN, p)
   );
   public static final Item BLUE_ICE = registerBlock(BlockItemIds.BLUE_ICE, Blocks.BLUE_ICE);
   public static final Item CONDUIT = registerBlock(BlockItemIds.CONDUIT, Blocks.CONDUIT, new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final Item POLISHED_GRANITE_STAIRS = registerBlock(BlockItemIds.POLISHED_GRANITE_STAIRS, Blocks.POLISHED_GRANITE_STAIRS);
   public static final Item SMOOTH_RED_SANDSTONE_STAIRS = registerBlock(BlockItemIds.SMOOTH_RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE_STAIRS);
   public static final Item MOSSY_STONE_BRICK_STAIRS = registerBlock(BlockItemIds.MOSSY_STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS);
   public static final Item POLISHED_DIORITE_STAIRS = registerBlock(BlockItemIds.POLISHED_DIORITE_STAIRS, Blocks.POLISHED_DIORITE_STAIRS);
   public static final Item MOSSY_COBBLESTONE_STAIRS = registerBlock(BlockItemIds.MOSSY_COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS);
   public static final Item END_STONE_BRICK_STAIRS = registerBlock(BlockItemIds.END_STONE_BRICK_STAIRS, Blocks.END_STONE_BRICK_STAIRS);
   public static final Item STONE_STAIRS = registerBlock(BlockItemIds.STONE_STAIRS, Blocks.STONE_STAIRS);
   public static final Item SMOOTH_SANDSTONE_STAIRS = registerBlock(BlockItemIds.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE_STAIRS);
   public static final Item SMOOTH_QUARTZ_STAIRS = registerBlock(BlockItemIds.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ_STAIRS);
   public static final Item GRANITE_STAIRS = registerBlock(BlockItemIds.GRANITE_STAIRS, Blocks.GRANITE_STAIRS);
   public static final Item ANDESITE_STAIRS = registerBlock(BlockItemIds.ANDESITE_STAIRS, Blocks.ANDESITE_STAIRS);
   public static final Item RED_NETHER_BRICK_STAIRS = registerBlock(BlockItemIds.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICK_STAIRS);
   public static final Item POLISHED_ANDESITE_STAIRS = registerBlock(BlockItemIds.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_ANDESITE_STAIRS);
   public static final Item DIORITE_STAIRS = registerBlock(BlockItemIds.DIORITE_STAIRS, Blocks.DIORITE_STAIRS);
   public static final Item COBBLED_DEEPSLATE_STAIRS = registerBlock(BlockItemIds.COBBLED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE_STAIRS);
   public static final Item POLISHED_DEEPSLATE_STAIRS = registerBlock(BlockItemIds.POLISHED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE_STAIRS);
   public static final Item DEEPSLATE_BRICK_STAIRS = registerBlock(BlockItemIds.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_STAIRS);
   public static final Item DEEPSLATE_TILE_STAIRS = registerBlock(BlockItemIds.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILE_STAIRS);
   public static final Item POLISHED_GRANITE_SLAB = registerBlock(BlockItemIds.POLISHED_GRANITE_SLAB, Blocks.POLISHED_GRANITE_SLAB);
   public static final Item SMOOTH_RED_SANDSTONE_SLAB = registerBlock(BlockItemIds.SMOOTH_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_SLAB);
   public static final Item MOSSY_STONE_BRICK_SLAB = registerBlock(BlockItemIds.MOSSY_STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB);
   public static final Item POLISHED_DIORITE_SLAB = registerBlock(BlockItemIds.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE_SLAB);
   public static final Item MOSSY_COBBLESTONE_SLAB = registerBlock(BlockItemIds.MOSSY_COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB);
   public static final Item END_STONE_BRICK_SLAB = registerBlock(BlockItemIds.END_STONE_BRICK_SLAB, Blocks.END_STONE_BRICK_SLAB);
   public static final Item SMOOTH_SANDSTONE_SLAB = registerBlock(BlockItemIds.SMOOTH_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE_SLAB);
   public static final Item SMOOTH_QUARTZ_SLAB = registerBlock(BlockItemIds.SMOOTH_QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ_SLAB);
   public static final Item GRANITE_SLAB = registerBlock(BlockItemIds.GRANITE_SLAB, Blocks.GRANITE_SLAB);
   public static final Item ANDESITE_SLAB = registerBlock(BlockItemIds.ANDESITE_SLAB, Blocks.ANDESITE_SLAB);
   public static final Item RED_NETHER_BRICK_SLAB = registerBlock(BlockItemIds.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICK_SLAB);
   public static final Item POLISHED_ANDESITE_SLAB = registerBlock(BlockItemIds.POLISHED_ANDESITE_SLAB, Blocks.POLISHED_ANDESITE_SLAB);
   public static final Item DIORITE_SLAB = registerBlock(BlockItemIds.DIORITE_SLAB, Blocks.DIORITE_SLAB);
   public static final Item COBBLED_DEEPSLATE_SLAB = registerBlock(BlockItemIds.COBBLED_DEEPSLATE_SLAB, Blocks.COBBLED_DEEPSLATE_SLAB);
   public static final Item POLISHED_DEEPSLATE_SLAB = registerBlock(BlockItemIds.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE_SLAB);
   public static final Item DEEPSLATE_BRICK_SLAB = registerBlock(BlockItemIds.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_SLAB);
   public static final Item DEEPSLATE_TILE_SLAB = registerBlock(BlockItemIds.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILE_SLAB);
   public static final Item SCAFFOLDING = registerBlock(BlockItemIds.SCAFFOLDING, Blocks.SCAFFOLDING, ScaffoldingBlockItem::new);
   public static final Item REDSTONE = registerItem(
      BlockItemIds.REDSTONE_DUST, createBlockItemWithCustomItemName(Blocks.REDSTONE_WIRE), new Item.Properties().trimMaterial(TrimMaterials.REDSTONE)
   );
   public static final Item REDSTONE_TORCH = registerBlock(
      BlockItemIds.REDSTONE_TORCH, Blocks.REDSTONE_TORCH, (b, p) -> new StandingAndWallBlockItem(b, Blocks.REDSTONE_WALL_TORCH, Direction.DOWN, p)
   );
   public static final Item REDSTONE_BLOCK = registerBlock(BlockItemIds.REDSTONE_BLOCK, Blocks.REDSTONE_BLOCK);
   public static final Item REPEATER = registerBlock(BlockItemIds.REPEATER, Blocks.REPEATER);
   public static final Item COMPARATOR = registerBlock(BlockItemIds.COMPARATOR, Blocks.COMPARATOR);
   public static final Item PISTON = registerBlock(BlockItemIds.PISTON, Blocks.PISTON);
   public static final Item STICKY_PISTON = registerBlock(BlockItemIds.STICKY_PISTON, Blocks.STICKY_PISTON);
   public static final Item SLIME_BLOCK = registerBlock(BlockItemIds.SLIME_BLOCK, Blocks.SLIME_BLOCK);
   public static final Item HONEY_BLOCK = registerBlock(BlockItemIds.HONEY_BLOCK, Blocks.HONEY_BLOCK);
   public static final Item OBSERVER = registerBlock(BlockItemIds.OBSERVER, Blocks.OBSERVER);
   public static final Item HOPPER = registerBlock(BlockItemIds.HOPPER, Blocks.HOPPER, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
   public static final Item DISPENSER = registerBlock(
      BlockItemIds.DISPENSER, Blocks.DISPENSER, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item DROPPER = registerBlock(
      BlockItemIds.DROPPER, Blocks.DROPPER, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item LECTERN = registerBlock(BlockItemIds.LECTERN, Blocks.LECTERN);
   public static final Item TARGET = registerBlock(BlockItemIds.TARGET, Blocks.TARGET);
   public static final Item LEVER = registerBlock(BlockItemIds.LEVER, Blocks.LEVER);
   public static final WeatheringCopperCollection<Item> LIGHTNING_ROD = WeatheringCopperCollection.registerItems(
      BlockItemIds.LIGHTNING_ROD, Blocks.LIGHTNING_ROD, Items::registerBlock
   );
   public static final Item DAYLIGHT_DETECTOR = registerBlock(BlockItemIds.DAYLIGHT_DETECTOR, Blocks.DAYLIGHT_DETECTOR);
   public static final Item SCULK_SENSOR = registerBlock(BlockItemIds.SCULK_SENSOR, Blocks.SCULK_SENSOR);
   public static final Item CALIBRATED_SCULK_SENSOR = registerBlock(BlockItemIds.CALIBRATED_SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR);
   public static final Item TRIPWIRE_HOOK = registerBlock(BlockItemIds.TRIPWIRE_HOOK, Blocks.TRIPWIRE_HOOK);
   public static final Item TRAPPED_CHEST = registerBlock(
      BlockItemIds.TRAPPED_CHEST, Blocks.TRAPPED_CHEST, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item TNT = registerBlock(BlockItemIds.TNT, Blocks.TNT);
   public static final Item REDSTONE_LAMP = registerBlock(BlockItemIds.REDSTONE_LAMP, Blocks.REDSTONE_LAMP);
   public static final Item NOTE_BLOCK = registerBlock(BlockItemIds.NOTE_BLOCK, Blocks.NOTE_BLOCK);
   public static final Item STONE_BUTTON = registerBlock(BlockItemIds.STONE_BUTTON, Blocks.STONE_BUTTON);
   public static final Item POLISHED_BLACKSTONE_BUTTON = registerBlock(BlockItemIds.POLISHED_BLACKSTONE_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON);
   public static final Item OAK_BUTTON = registerBlock(BlockItemIds.OAK_BUTTON, Blocks.OAK_BUTTON);
   public static final Item SPRUCE_BUTTON = registerBlock(BlockItemIds.SPRUCE_BUTTON, Blocks.SPRUCE_BUTTON);
   public static final Item BIRCH_BUTTON = registerBlock(BlockItemIds.BIRCH_BUTTON, Blocks.BIRCH_BUTTON);
   public static final Item JUNGLE_BUTTON = registerBlock(BlockItemIds.JUNGLE_BUTTON, Blocks.JUNGLE_BUTTON);
   public static final Item ACACIA_BUTTON = registerBlock(BlockItemIds.ACACIA_BUTTON, Blocks.ACACIA_BUTTON);
   public static final Item CHERRY_BUTTON = registerBlock(BlockItemIds.CHERRY_BUTTON, Blocks.CHERRY_BUTTON);
   public static final Item DARK_OAK_BUTTON = registerBlock(BlockItemIds.DARK_OAK_BUTTON, Blocks.DARK_OAK_BUTTON);
   public static final Item PALE_OAK_BUTTON = registerBlock(BlockItemIds.PALE_OAK_BUTTON, Blocks.PALE_OAK_BUTTON);
   public static final Item MANGROVE_BUTTON = registerBlock(BlockItemIds.MANGROVE_BUTTON, Blocks.MANGROVE_BUTTON);
   public static final Item POPLAR_BUTTON = registerBlock(BlockItemIds.POPLAR_BUTTON, Blocks.POPLAR_BUTTON);
   public static final Item BAMBOO_BUTTON = registerBlock(BlockItemIds.BAMBOO_BUTTON, Blocks.BAMBOO_BUTTON);
   public static final Item CRIMSON_BUTTON = registerBlock(BlockItemIds.CRIMSON_BUTTON, Blocks.CRIMSON_BUTTON);
   public static final Item WARPED_BUTTON = registerBlock(BlockItemIds.WARPED_BUTTON, Blocks.WARPED_BUTTON);
   public static final Item STONE_PRESSURE_PLATE = registerBlock(BlockItemIds.STONE_PRESSURE_PLATE, Blocks.STONE_PRESSURE_PLATE);
   public static final Item POLISHED_BLACKSTONE_PRESSURE_PLATE = registerBlock(
      BlockItemIds.POLISHED_BLACKSTONE_PRESSURE_PLATE, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE
   );
   public static final Item LIGHT_WEIGHTED_PRESSURE_PLATE = registerBlock(BlockItemIds.LIGHT_WEIGHTED_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
   public static final Item HEAVY_WEIGHTED_PRESSURE_PLATE = registerBlock(BlockItemIds.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
   public static final Item OAK_PRESSURE_PLATE = registerBlock(BlockItemIds.OAK_PRESSURE_PLATE, Blocks.OAK_PRESSURE_PLATE);
   public static final Item SPRUCE_PRESSURE_PLATE = registerBlock(BlockItemIds.SPRUCE_PRESSURE_PLATE, Blocks.SPRUCE_PRESSURE_PLATE);
   public static final Item BIRCH_PRESSURE_PLATE = registerBlock(BlockItemIds.BIRCH_PRESSURE_PLATE, Blocks.BIRCH_PRESSURE_PLATE);
   public static final Item JUNGLE_PRESSURE_PLATE = registerBlock(BlockItemIds.JUNGLE_PRESSURE_PLATE, Blocks.JUNGLE_PRESSURE_PLATE);
   public static final Item ACACIA_PRESSURE_PLATE = registerBlock(BlockItemIds.ACACIA_PRESSURE_PLATE, Blocks.ACACIA_PRESSURE_PLATE);
   public static final Item CHERRY_PRESSURE_PLATE = registerBlock(BlockItemIds.CHERRY_PRESSURE_PLATE, Blocks.CHERRY_PRESSURE_PLATE);
   public static final Item DARK_OAK_PRESSURE_PLATE = registerBlock(BlockItemIds.DARK_OAK_PRESSURE_PLATE, Blocks.DARK_OAK_PRESSURE_PLATE);
   public static final Item PALE_OAK_PRESSURE_PLATE = registerBlock(BlockItemIds.PALE_OAK_PRESSURE_PLATE, Blocks.PALE_OAK_PRESSURE_PLATE);
   public static final Item MANGROVE_PRESSURE_PLATE = registerBlock(BlockItemIds.MANGROVE_PRESSURE_PLATE, Blocks.MANGROVE_PRESSURE_PLATE);
   public static final Item POPLAR_PRESSURE_PLATE = registerBlock(BlockItemIds.POPLAR_PRESSURE_PLATE, Blocks.POPLAR_PRESSURE_PLATE);
   public static final Item BAMBOO_PRESSURE_PLATE = registerBlock(BlockItemIds.BAMBOO_PRESSURE_PLATE, Blocks.BAMBOO_PRESSURE_PLATE);
   public static final Item CRIMSON_PRESSURE_PLATE = registerBlock(BlockItemIds.CRIMSON_PRESSURE_PLATE, Blocks.CRIMSON_PRESSURE_PLATE);
   public static final Item WARPED_PRESSURE_PLATE = registerBlock(BlockItemIds.WARPED_PRESSURE_PLATE, Blocks.WARPED_PRESSURE_PLATE);
   public static final Item IRON_DOOR = registerBlock(BlockItemIds.IRON_DOOR, Blocks.IRON_DOOR, DoubleHighBlockItem::new);
   public static final Item OAK_DOOR = registerBlock(BlockItemIds.OAK_DOOR, Blocks.OAK_DOOR, DoubleHighBlockItem::new);
   public static final Item SPRUCE_DOOR = registerBlock(BlockItemIds.SPRUCE_DOOR, Blocks.SPRUCE_DOOR, DoubleHighBlockItem::new);
   public static final Item BIRCH_DOOR = registerBlock(BlockItemIds.BIRCH_DOOR, Blocks.BIRCH_DOOR, DoubleHighBlockItem::new);
   public static final Item JUNGLE_DOOR = registerBlock(BlockItemIds.JUNGLE_DOOR, Blocks.JUNGLE_DOOR, DoubleHighBlockItem::new);
   public static final Item ACACIA_DOOR = registerBlock(BlockItemIds.ACACIA_DOOR, Blocks.ACACIA_DOOR, DoubleHighBlockItem::new);
   public static final Item CHERRY_DOOR = registerBlock(BlockItemIds.CHERRY_DOOR, Blocks.CHERRY_DOOR, DoubleHighBlockItem::new);
   public static final Item DARK_OAK_DOOR = registerBlock(BlockItemIds.DARK_OAK_DOOR, Blocks.DARK_OAK_DOOR, DoubleHighBlockItem::new);
   public static final Item PALE_OAK_DOOR = registerBlock(BlockItemIds.PALE_OAK_DOOR, Blocks.PALE_OAK_DOOR, DoubleHighBlockItem::new);
   public static final Item MANGROVE_DOOR = registerBlock(BlockItemIds.MANGROVE_DOOR, Blocks.MANGROVE_DOOR, DoubleHighBlockItem::new);
   public static final Item POPLAR_DOOR = registerBlock(BlockItemIds.POPLAR_DOOR, Blocks.POPLAR_DOOR, DoubleHighBlockItem::new);
   public static final Item BAMBOO_DOOR = registerBlock(BlockItemIds.BAMBOO_DOOR, Blocks.BAMBOO_DOOR, DoubleHighBlockItem::new);
   public static final Item CRIMSON_DOOR = registerBlock(BlockItemIds.CRIMSON_DOOR, Blocks.CRIMSON_DOOR, DoubleHighBlockItem::new);
   public static final Item WARPED_DOOR = registerBlock(BlockItemIds.WARPED_DOOR, Blocks.WARPED_DOOR, DoubleHighBlockItem::new);
   public static final WeatheringCopperCollection<Item> COPPER_DOOR = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_DOOR, Blocks.COPPER_DOOR, (id, block) -> registerBlock(id, block, DoubleHighBlockItem::new)
   );
   public static final Item IRON_TRAPDOOR = registerBlock(BlockItemIds.IRON_TRAPDOOR, Blocks.IRON_TRAPDOOR);
   public static final Item OAK_TRAPDOOR = registerBlock(BlockItemIds.OAK_TRAPDOOR, Blocks.OAK_TRAPDOOR);
   public static final Item SPRUCE_TRAPDOOR = registerBlock(BlockItemIds.SPRUCE_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR);
   public static final Item BIRCH_TRAPDOOR = registerBlock(BlockItemIds.BIRCH_TRAPDOOR, Blocks.BIRCH_TRAPDOOR);
   public static final Item JUNGLE_TRAPDOOR = registerBlock(BlockItemIds.JUNGLE_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR);
   public static final Item ACACIA_TRAPDOOR = registerBlock(BlockItemIds.ACACIA_TRAPDOOR, Blocks.ACACIA_TRAPDOOR);
   public static final Item CHERRY_TRAPDOOR = registerBlock(BlockItemIds.CHERRY_TRAPDOOR, Blocks.CHERRY_TRAPDOOR);
   public static final Item DARK_OAK_TRAPDOOR = registerBlock(BlockItemIds.DARK_OAK_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR);
   public static final Item PALE_OAK_TRAPDOOR = registerBlock(BlockItemIds.PALE_OAK_TRAPDOOR, Blocks.PALE_OAK_TRAPDOOR);
   public static final Item MANGROVE_TRAPDOOR = registerBlock(BlockItemIds.MANGROVE_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR);
   public static final Item POPLAR_TRAPDOOR = registerBlock(BlockItemIds.POPLAR_TRAPDOOR, Blocks.POPLAR_TRAPDOOR);
   public static final Item BAMBOO_TRAPDOOR = registerBlock(BlockItemIds.BAMBOO_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR);
   public static final Item CRIMSON_TRAPDOOR = registerBlock(BlockItemIds.CRIMSON_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR);
   public static final Item WARPED_TRAPDOOR = registerBlock(BlockItemIds.WARPED_TRAPDOOR, Blocks.WARPED_TRAPDOOR);
   public static final WeatheringCopperCollection<Item> COPPER_TRAPDOOR = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_TRAPDOOR, Blocks.COPPER_TRAPDOOR, Items::registerBlock
   );
   public static final Item OAK_FENCE_GATE = registerBlock(BlockItemIds.OAK_FENCE_GATE, Blocks.OAK_FENCE_GATE);
   public static final Item SPRUCE_FENCE_GATE = registerBlock(BlockItemIds.SPRUCE_FENCE_GATE, Blocks.SPRUCE_FENCE_GATE);
   public static final Item BIRCH_FENCE_GATE = registerBlock(BlockItemIds.BIRCH_FENCE_GATE, Blocks.BIRCH_FENCE_GATE);
   public static final Item JUNGLE_FENCE_GATE = registerBlock(BlockItemIds.JUNGLE_FENCE_GATE, Blocks.JUNGLE_FENCE_GATE);
   public static final Item ACACIA_FENCE_GATE = registerBlock(BlockItemIds.ACACIA_FENCE_GATE, Blocks.ACACIA_FENCE_GATE);
   public static final Item CHERRY_FENCE_GATE = registerBlock(BlockItemIds.CHERRY_FENCE_GATE, Blocks.CHERRY_FENCE_GATE);
   public static final Item DARK_OAK_FENCE_GATE = registerBlock(BlockItemIds.DARK_OAK_FENCE_GATE, Blocks.DARK_OAK_FENCE_GATE);
   public static final Item PALE_OAK_FENCE_GATE = registerBlock(BlockItemIds.PALE_OAK_FENCE_GATE, Blocks.PALE_OAK_FENCE_GATE);
   public static final Item MANGROVE_FENCE_GATE = registerBlock(BlockItemIds.MANGROVE_FENCE_GATE, Blocks.MANGROVE_FENCE_GATE);
   public static final Item POPLAR_FENCE_GATE = registerBlock(BlockItemIds.POPLAR_FENCE_GATE, Blocks.POPLAR_FENCE_GATE);
   public static final Item BAMBOO_FENCE_GATE = registerBlock(BlockItemIds.BAMBOO_FENCE_GATE, Blocks.BAMBOO_FENCE_GATE);
   public static final Item CRIMSON_FENCE_GATE = registerBlock(BlockItemIds.CRIMSON_FENCE_GATE, Blocks.CRIMSON_FENCE_GATE);
   public static final Item WARPED_FENCE_GATE = registerBlock(BlockItemIds.WARPED_FENCE_GATE, Blocks.WARPED_FENCE_GATE);
   public static final Item POWERED_RAIL = registerBlock(BlockItemIds.POWERED_RAIL, Blocks.POWERED_RAIL);
   public static final Item DETECTOR_RAIL = registerBlock(BlockItemIds.DETECTOR_RAIL, Blocks.DETECTOR_RAIL);
   public static final Item RAIL = registerBlock(BlockItemIds.RAIL, Blocks.RAIL);
   public static final Item ACTIVATOR_RAIL = registerBlock(BlockItemIds.ACTIVATOR_RAIL, Blocks.ACTIVATOR_RAIL);
   public static final Item SADDLE = registerItem(ItemIds.SADDLE, new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE, Equippable.saddle()));
   public static final ColorCollection<Item> HARNESS = ColorCollection.registerItems(
      ItemIds.HARNESS, (name, color) -> registerItem(name, new Item.Properties().stacksTo(1).component(DataComponents.EQUIPPABLE, Equippable.harness(color)))
   );
   public static final Item MINECART = registerItem(ItemIds.MINECART, p -> new MinecartItem(EntityTypes.MINECART, p), new Item.Properties().stacksTo(1));
   public static final Item CHEST_MINECART = registerItem(
      ItemIds.CHEST_MINECART, p -> new MinecartItem(EntityTypes.CHEST_MINECART, p), new Item.Properties().stacksTo(1)
   );
   public static final Item FURNACE_MINECART = registerItem(
      ItemIds.FURNACE_MINECART, p -> new MinecartItem(EntityTypes.FURNACE_MINECART, p), new Item.Properties().stacksTo(1)
   );
   public static final Item TNT_MINECART = registerItem(
      ItemIds.TNT_MINECART, p -> new MinecartItem(EntityTypes.TNT_MINECART, p), new Item.Properties().stacksTo(1)
   );
   public static final Item HOPPER_MINECART = registerItem(
      ItemIds.HOPPER_MINECART, p -> new MinecartItem(EntityTypes.HOPPER_MINECART, p), new Item.Properties().stacksTo(1)
   );
   public static final Item CARROT_ON_A_STICK = registerItem(
      ItemIds.CARROT_ON_A_STICK, p -> new FoodOnAStickItem<>(EntityTypes.PIG, 7, p), new Item.Properties().durability(25)
   );
   public static final Item WARPED_FUNGUS_ON_A_STICK = registerItem(
      ItemIds.WARPED_FUNGUS_ON_A_STICK, p -> new FoodOnAStickItem<>(EntityTypes.STRIDER, 1, p), new Item.Properties().durability(100)
   );
   public static final Item PHANTOM_MEMBRANE = registerItem(ItemIds.PHANTOM_MEMBRANE);
   public static final Item ELYTRA = registerItem(
      ItemIds.ELYTRA,
      new Item.Properties()
         .durability(432)
         .rarity(Rarity.EPIC)
         .component(DataComponents.GLIDER, Unit.INSTANCE)
         .component(
            DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.CHEST)
               .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
               .setAsset(EquipmentAssets.ELYTRA)
               .setDamageOnHurt(false)
               .build()
         )
         .repairable(PHANTOM_MEMBRANE)
   );
   public static final Item OAK_BOAT = registerItem(ItemIds.OAK_BOAT, p -> new BoatItem(EntityTypes.OAK_BOAT, p), new Item.Properties().stacksTo(1));
   public static final Item OAK_CHEST_BOAT = registerItem(
      ItemIds.OAK_CHEST_BOAT, p -> new BoatItem(EntityTypes.OAK_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item SPRUCE_BOAT = registerItem(ItemIds.SPRUCE_BOAT, p -> new BoatItem(EntityTypes.SPRUCE_BOAT, p), new Item.Properties().stacksTo(1));
   public static final Item SPRUCE_CHEST_BOAT = registerItem(
      ItemIds.SPRUCE_CHEST_BOAT, p -> new BoatItem(EntityTypes.SPRUCE_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item BIRCH_BOAT = registerItem(ItemIds.BIRCH_BOAT, p -> new BoatItem(EntityTypes.BIRCH_BOAT, p), new Item.Properties().stacksTo(1));
   public static final Item BIRCH_CHEST_BOAT = registerItem(
      ItemIds.BIRCH_CHEST_BOAT, p -> new BoatItem(EntityTypes.BIRCH_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item JUNGLE_BOAT = registerItem(ItemIds.JUNGLE_BOAT, p -> new BoatItem(EntityTypes.JUNGLE_BOAT, p), new Item.Properties().stacksTo(1));
   public static final Item JUNGLE_CHEST_BOAT = registerItem(
      ItemIds.JUNGLE_CHEST_BOAT, p -> new BoatItem(EntityTypes.JUNGLE_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item ACACIA_BOAT = registerItem(ItemIds.ACACIA_BOAT, p -> new BoatItem(EntityTypes.ACACIA_BOAT, p), new Item.Properties().stacksTo(1));
   public static final Item ACACIA_CHEST_BOAT = registerItem(
      ItemIds.ACACIA_CHEST_BOAT, p -> new BoatItem(EntityTypes.ACACIA_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item CHERRY_BOAT = registerItem(ItemIds.CHERRY_BOAT, p -> new BoatItem(EntityTypes.CHERRY_BOAT, p), new Item.Properties().stacksTo(1));
   public static final Item CHERRY_CHEST_BOAT = registerItem(
      ItemIds.CHERRY_CHEST_BOAT, p -> new BoatItem(EntityTypes.CHERRY_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item DARK_OAK_BOAT = registerItem(
      ItemIds.DARK_OAK_BOAT, p -> new BoatItem(EntityTypes.DARK_OAK_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item DARK_OAK_CHEST_BOAT = registerItem(
      ItemIds.DARK_OAK_CHEST_BOAT, p -> new BoatItem(EntityTypes.DARK_OAK_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item PALE_OAK_BOAT = registerItem(
      ItemIds.PALE_OAK_BOAT, p -> new BoatItem(EntityTypes.PALE_OAK_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item PALE_OAK_CHEST_BOAT = registerItem(
      ItemIds.PALE_OAK_CHEST_BOAT, p -> new BoatItem(EntityTypes.PALE_OAK_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item MANGROVE_BOAT = registerItem(
      ItemIds.MANGROVE_BOAT, p -> new BoatItem(EntityTypes.MANGROVE_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item MANGROVE_CHEST_BOAT = registerItem(
      ItemIds.MANGROVE_CHEST_BOAT, p -> new BoatItem(EntityTypes.MANGROVE_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item POPLAR_BOAT = registerItem(ItemIds.POPLAR_BOAT, p -> new BoatItem(EntityTypes.POPLAR_BOAT, p), new Item.Properties().stacksTo(1));
   public static final Item POPLAR_CHEST_BOAT = registerItem(
      ItemIds.POPLAR_CHEST_BOAT, p -> new BoatItem(EntityTypes.POPLAR_CHEST_BOAT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item BAMBOO_RAFT = registerItem(ItemIds.BAMBOO_RAFT, p -> new BoatItem(EntityTypes.BAMBOO_RAFT, p), new Item.Properties().stacksTo(1));
   public static final Item BAMBOO_CHEST_RAFT = registerItem(
      ItemIds.BAMBOO_CHEST_RAFT, p -> new BoatItem(EntityTypes.BAMBOO_CHEST_RAFT, p), new Item.Properties().stacksTo(1)
   );
   public static final Item STRUCTURE_BLOCK = registerBlock(
      BlockItemIds.STRUCTURE_BLOCK, Blocks.STRUCTURE_BLOCK, GameMasterBlockItem::new, new Item.Properties().rarity(Rarity.EPIC)
   );
   public static final Item JIGSAW = registerBlock(BlockItemIds.JIGSAW, Blocks.JIGSAW, GameMasterBlockItem::new, new Item.Properties().rarity(Rarity.EPIC));
   public static final Item TEST_BLOCK = registerBlock(
      BlockItemIds.TEST_BLOCK,
      Blocks.TEST_BLOCK,
      GameMasterBlockItem::new,
      new Item.Properties().rarity(Rarity.EPIC).component(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(TestBlock.MODE, TestBlockMode.START))
   );
   public static final Item TEST_INSTANCE_BLOCK = registerBlock(
      BlockItemIds.TEST_INSTANCE_BLOCK, Blocks.TEST_INSTANCE_BLOCK, GameMasterBlockItem::new, new Item.Properties().rarity(Rarity.EPIC)
   );
   public static final Item TURTLE_HELMET = registerItem(
      ItemIds.TURTLE_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.TURTLE_SCUTE, ArmorType.HELMET)
   );
   public static final Item TURTLE_SCUTE = registerItem(ItemIds.TURTLE_SCUTE);
   public static final Item ARMADILLO_SCUTE = registerItem(ItemIds.ARMADILLO_SCUTE);
   public static final Item WOLF_ARMOR = registerItem(ItemIds.WOLF_ARMOR, new Item.Properties().wolfArmor(ArmorMaterials.ARMADILLO_SCUTE));
   public static final Item FLINT_AND_STEEL = registerItem(ItemIds.FLINT_AND_STEEL, FlintAndSteelItem::new, new Item.Properties().durability(64));
   public static final Item BOWL = registerItem(ItemIds.BOWL);
   public static final Item APPLE = registerItem(ItemIds.APPLE, new Item.Properties().food(Foods.APPLE).compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item BOW = registerItem(ItemIds.BOW, BowItem::new, new Item.Properties().durability(384).enchantable(1));
   public static final Item ARROW = registerItem(ItemIds.ARROW, ArrowItem::new);
   public static final Item COAL = registerItem(ItemIds.COAL);
   public static final Item CHARCOAL = registerItem(ItemIds.CHARCOAL);
   public static final Item DIAMOND = registerItem(ItemIds.DIAMOND, new Item.Properties().trimMaterial(TrimMaterials.DIAMOND));
   public static final Item EMERALD = registerItem(ItemIds.EMERALD, new Item.Properties().trimMaterial(TrimMaterials.EMERALD));
   public static final Item LAPIS_LAZULI = registerItem(ItemIds.LAPIS_LAZULI, new Item.Properties().trimMaterial(TrimMaterials.LAPIS));
   public static final Item QUARTZ = registerItem(ItemIds.QUARTZ, new Item.Properties().trimMaterial(TrimMaterials.QUARTZ));
   public static final Item AMETHYST_SHARD = registerItem(ItemIds.AMETHYST_SHARD, new Item.Properties().trimMaterial(TrimMaterials.AMETHYST));
   public static final Item RAW_IRON = registerItem(ItemIds.RAW_IRON);
   public static final Item IRON_INGOT = registerItem(ItemIds.IRON_INGOT, new Item.Properties().trimMaterial(TrimMaterials.IRON));
   public static final Item RAW_COPPER = registerItem(ItemIds.RAW_COPPER);
   public static final Item COPPER_INGOT = registerItem(ItemIds.COPPER_INGOT, new Item.Properties().trimMaterial(TrimMaterials.COPPER));
   public static final Item RAW_GOLD = registerItem(ItemIds.RAW_GOLD);
   public static final Item GOLD_INGOT = registerItem(ItemIds.GOLD_INGOT, new Item.Properties().trimMaterial(TrimMaterials.GOLD));
   public static final Item NETHERITE_INGOT = registerItem(ItemIds.NETHERITE_INGOT, new Item.Properties().fireResistant().trimMaterial(TrimMaterials.NETHERITE));
   public static final Item NETHERITE_SCRAP = registerItem(ItemIds.NETHERITE_SCRAP, new Item.Properties().fireResistant());
   public static final Item WOODEN_SWORD = registerItem(ItemIds.WOODEN_SWORD, new Item.Properties().sword(ToolMaterial.WOOD, 3.0F, -2.4F));
   public static final Item WOODEN_SHOVEL = registerItem(ItemIds.WOODEN_SHOVEL, new Item.Properties().shovel(ToolMaterial.WOOD, 1.5F, -3.0F));
   public static final Item WOODEN_PICKAXE = registerItem(ItemIds.WOODEN_PICKAXE, new Item.Properties().pickaxe(ToolMaterial.WOOD, 1.0F, -2.8F));
   public static final Item WOODEN_AXE = registerItem(ItemIds.WOODEN_AXE, new Item.Properties().axe(ToolMaterial.WOOD, 6.0F, -3.2F));
   public static final Item WOODEN_HOE = registerItem(ItemIds.WOODEN_HOE, new Item.Properties().hoe(ToolMaterial.WOOD, 0.0F, -3.0F));
   public static final Item COPPER_SWORD = registerItem(ItemIds.COPPER_SWORD, new Item.Properties().sword(ToolMaterial.COPPER, 3.0F, -2.4F));
   public static final Item COPPER_SHOVEL = registerItem(ItemIds.COPPER_SHOVEL, new Item.Properties().shovel(ToolMaterial.COPPER, 1.5F, -3.0F));
   public static final Item COPPER_PICKAXE = registerItem(ItemIds.COPPER_PICKAXE, new Item.Properties().pickaxe(ToolMaterial.COPPER, 1.0F, -2.8F));
   public static final Item COPPER_AXE = registerItem(ItemIds.COPPER_AXE, new Item.Properties().axe(ToolMaterial.COPPER, 7.0F, -3.2F));
   public static final Item COPPER_HOE = registerItem(ItemIds.COPPER_HOE, new Item.Properties().hoe(ToolMaterial.COPPER, -1.0F, -2.0F));
   public static final Item STONE_SWORD = registerItem(ItemIds.STONE_SWORD, new Item.Properties().sword(ToolMaterial.STONE, 3.0F, -2.4F));
   public static final Item STONE_SHOVEL = registerItem(ItemIds.STONE_SHOVEL, new Item.Properties().shovel(ToolMaterial.STONE, 1.5F, -3.0F));
   public static final Item STONE_PICKAXE = registerItem(ItemIds.STONE_PICKAXE, new Item.Properties().pickaxe(ToolMaterial.STONE, 1.0F, -2.8F));
   public static final Item STONE_AXE = registerItem(ItemIds.STONE_AXE, new Item.Properties().axe(ToolMaterial.STONE, 7.0F, -3.2F));
   public static final Item STONE_HOE = registerItem(ItemIds.STONE_HOE, new Item.Properties().hoe(ToolMaterial.STONE, -1.0F, -2.0F));
   public static final Item GOLDEN_SWORD = registerItem(ItemIds.GOLDEN_SWORD, new Item.Properties().sword(ToolMaterial.GOLD, 3.0F, -2.4F));
   public static final Item GOLDEN_SHOVEL = registerItem(ItemIds.GOLDEN_SHOVEL, new Item.Properties().shovel(ToolMaterial.GOLD, 1.5F, -3.0F));
   public static final Item GOLDEN_PICKAXE = registerItem(ItemIds.GOLDEN_PICKAXE, new Item.Properties().pickaxe(ToolMaterial.GOLD, 1.0F, -2.8F));
   public static final Item GOLDEN_AXE = registerItem(ItemIds.GOLDEN_AXE, new Item.Properties().axe(ToolMaterial.GOLD, 6.0F, -3.0F));
   public static final Item GOLDEN_HOE = registerItem(ItemIds.GOLDEN_HOE, new Item.Properties().hoe(ToolMaterial.GOLD, 0.0F, -3.0F));
   public static final Item IRON_SWORD = registerItem(ItemIds.IRON_SWORD, new Item.Properties().sword(ToolMaterial.IRON, 3.0F, -2.4F));
   public static final Item IRON_SHOVEL = registerItem(ItemIds.IRON_SHOVEL, new Item.Properties().shovel(ToolMaterial.IRON, 1.5F, -3.0F));
   public static final Item IRON_PICKAXE = registerItem(ItemIds.IRON_PICKAXE, new Item.Properties().pickaxe(ToolMaterial.IRON, 1.0F, -2.8F));
   public static final Item IRON_AXE = registerItem(ItemIds.IRON_AXE, new Item.Properties().axe(ToolMaterial.IRON, 6.0F, -3.1F));
   public static final Item IRON_HOE = registerItem(ItemIds.IRON_HOE, new Item.Properties().hoe(ToolMaterial.IRON, -2.0F, -1.0F));
   public static final Item DIAMOND_SWORD = registerItem(ItemIds.DIAMOND_SWORD, new Item.Properties().sword(ToolMaterial.DIAMOND, 3.0F, -2.4F));
   public static final Item DIAMOND_SHOVEL = registerItem(ItemIds.DIAMOND_SHOVEL, new Item.Properties().shovel(ToolMaterial.DIAMOND, 1.5F, -3.0F));
   public static final Item DIAMOND_PICKAXE = registerItem(ItemIds.DIAMOND_PICKAXE, new Item.Properties().pickaxe(ToolMaterial.DIAMOND, 1.0F, -2.8F));
   public static final Item DIAMOND_AXE = registerItem(ItemIds.DIAMOND_AXE, new Item.Properties().axe(ToolMaterial.DIAMOND, 5.0F, -3.0F));
   public static final Item DIAMOND_HOE = registerItem(ItemIds.DIAMOND_HOE, new Item.Properties().hoe(ToolMaterial.DIAMOND, -3.0F, 0.0F));
   public static final Item NETHERITE_SWORD = registerItem(
      ItemIds.NETHERITE_SWORD, new Item.Properties().sword(ToolMaterial.NETHERITE, 3.0F, -2.4F).fireResistant()
   );
   public static final Item NETHERITE_SHOVEL = registerItem(
      ItemIds.NETHERITE_SHOVEL, new Item.Properties().shovel(ToolMaterial.NETHERITE, 1.5F, -3.0F).fireResistant()
   );
   public static final Item NETHERITE_PICKAXE = registerItem(
      ItemIds.NETHERITE_PICKAXE, new Item.Properties().pickaxe(ToolMaterial.NETHERITE, 1.0F, -2.8F).fireResistant()
   );
   public static final Item NETHERITE_AXE = registerItem(ItemIds.NETHERITE_AXE, new Item.Properties().axe(ToolMaterial.NETHERITE, 5.0F, -3.0F).fireResistant());
   public static final Item NETHERITE_HOE = registerItem(ItemIds.NETHERITE_HOE, new Item.Properties().hoe(ToolMaterial.NETHERITE, -4.0F, 0.0F).fireResistant());
   public static final Item STICK = registerItem(ItemIds.STICK);
   public static final Item MUSHROOM_STEW = registerItem(
      ItemIds.MUSHROOM_STEW, new Item.Properties().stacksTo(1).food(Foods.MUSHROOM_STEW).usingConvertsTo(BOWL)
   );
   public static final Item STRING = registerItem(BlockItemIds.TRIPWIRE, createBlockItemWithCustomItemName(Blocks.TRIPWIRE));
   public static final Item FEATHER = registerItem(ItemIds.FEATHER);
   public static final Item GUNPOWDER = registerItem(ItemIds.GUNPOWDER);
   public static final Item WHEAT_SEEDS = registerItem(
      BlockItemIds.WHEAT_CROP, createBlockItemWithCustomItemName(Blocks.WHEAT), new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item WHEAT = registerItem(ItemIds.WHEAT, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM));
   public static final Item BREAD = registerItem(ItemIds.BREAD, new Item.Properties().food(Foods.BREAD).compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH));
   public static final Item LEATHER_HELMET = registerItem(ItemIds.LEATHER_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.LEATHER, ArmorType.HELMET));
   public static final Item LEATHER_CHESTPLATE = registerItem(
      ItemIds.LEATHER_CHESTPLATE, new Item.Properties().humanoidArmor(ArmorMaterials.LEATHER, ArmorType.CHESTPLATE)
   );
   public static final Item LEATHER_LEGGINGS = registerItem(
      ItemIds.LEATHER_LEGGINGS, new Item.Properties().humanoidArmor(ArmorMaterials.LEATHER, ArmorType.LEGGINGS)
   );
   public static final Item LEATHER_BOOTS = registerItem(ItemIds.LEATHER_BOOTS, new Item.Properties().humanoidArmor(ArmorMaterials.LEATHER, ArmorType.BOOTS));
   public static final Item COPPER_HELMET = registerItem(ItemIds.COPPER_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.COPPER, ArmorType.HELMET));
   public static final Item COPPER_CHESTPLATE = registerItem(
      ItemIds.COPPER_CHESTPLATE, new Item.Properties().humanoidArmor(ArmorMaterials.COPPER, ArmorType.CHESTPLATE)
   );
   public static final Item COPPER_LEGGINGS = registerItem(
      ItemIds.COPPER_LEGGINGS, new Item.Properties().humanoidArmor(ArmorMaterials.COPPER, ArmorType.LEGGINGS)
   );
   public static final Item COPPER_BOOTS = registerItem(ItemIds.COPPER_BOOTS, new Item.Properties().humanoidArmor(ArmorMaterials.COPPER, ArmorType.BOOTS));
   public static final Item CHAINMAIL_HELMET = registerItem(
      ItemIds.CHAINMAIL_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.CHAINMAIL, ArmorType.HELMET).rarity(Rarity.UNCOMMON)
   );
   public static final Item CHAINMAIL_CHESTPLATE = registerItem(
      ItemIds.CHAINMAIL_CHESTPLATE, new Item.Properties().humanoidArmor(ArmorMaterials.CHAINMAIL, ArmorType.CHESTPLATE).rarity(Rarity.UNCOMMON)
   );
   public static final Item CHAINMAIL_LEGGINGS = registerItem(
      ItemIds.CHAINMAIL_LEGGINGS, new Item.Properties().humanoidArmor(ArmorMaterials.CHAINMAIL, ArmorType.LEGGINGS).rarity(Rarity.UNCOMMON)
   );
   public static final Item CHAINMAIL_BOOTS = registerItem(
      ItemIds.CHAINMAIL_BOOTS, new Item.Properties().humanoidArmor(ArmorMaterials.CHAINMAIL, ArmorType.BOOTS).rarity(Rarity.UNCOMMON)
   );
   public static final Item IRON_HELMET = registerItem(ItemIds.IRON_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.IRON, ArmorType.HELMET));
   public static final Item IRON_CHESTPLATE = registerItem(
      ItemIds.IRON_CHESTPLATE, new Item.Properties().humanoidArmor(ArmorMaterials.IRON, ArmorType.CHESTPLATE)
   );
   public static final Item IRON_LEGGINGS = registerItem(ItemIds.IRON_LEGGINGS, new Item.Properties().humanoidArmor(ArmorMaterials.IRON, ArmorType.LEGGINGS));
   public static final Item IRON_BOOTS = registerItem(ItemIds.IRON_BOOTS, new Item.Properties().humanoidArmor(ArmorMaterials.IRON, ArmorType.BOOTS));
   public static final Item DIAMOND_HELMET = registerItem(ItemIds.DIAMOND_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.HELMET));
   public static final Item DIAMOND_CHESTPLATE = registerItem(
      ItemIds.DIAMOND_CHESTPLATE, new Item.Properties().humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.CHESTPLATE)
   );
   public static final Item DIAMOND_LEGGINGS = registerItem(
      ItemIds.DIAMOND_LEGGINGS, new Item.Properties().humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.LEGGINGS)
   );
   public static final Item DIAMOND_BOOTS = registerItem(ItemIds.DIAMOND_BOOTS, new Item.Properties().humanoidArmor(ArmorMaterials.DIAMOND, ArmorType.BOOTS));
   public static final Item GOLDEN_HELMET = registerItem(ItemIds.GOLDEN_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.GOLD, ArmorType.HELMET));
   public static final Item GOLDEN_CHESTPLATE = registerItem(
      ItemIds.GOLDEN_CHESTPLATE, new Item.Properties().humanoidArmor(ArmorMaterials.GOLD, ArmorType.CHESTPLATE)
   );
   public static final Item GOLDEN_LEGGINGS = registerItem(
      ItemIds.GOLDEN_LEGGINGS, new Item.Properties().humanoidArmor(ArmorMaterials.GOLD, ArmorType.LEGGINGS)
   );
   public static final Item GOLDEN_BOOTS = registerItem(ItemIds.GOLDEN_BOOTS, new Item.Properties().humanoidArmor(ArmorMaterials.GOLD, ArmorType.BOOTS));
   public static final Item NETHERITE_HELMET = registerItem(
      ItemIds.NETHERITE_HELMET, new Item.Properties().humanoidArmor(ArmorMaterials.NETHERITE, ArmorType.HELMET).fireResistant()
   );
   public static final Item NETHERITE_CHESTPLATE = registerItem(
      ItemIds.NETHERITE_CHESTPLATE, new Item.Properties().humanoidArmor(ArmorMaterials.NETHERITE, ArmorType.CHESTPLATE).fireResistant()
   );
   public static final Item NETHERITE_LEGGINGS = registerItem(
      ItemIds.NETHERITE_LEGGINGS, new Item.Properties().humanoidArmor(ArmorMaterials.NETHERITE, ArmorType.LEGGINGS).fireResistant()
   );
   public static final Item NETHERITE_BOOTS = registerItem(
      ItemIds.NETHERITE_BOOTS, new Item.Properties().humanoidArmor(ArmorMaterials.NETHERITE, ArmorType.BOOTS).fireResistant()
   );
   public static final Item FLINT = registerItem(ItemIds.FLINT);
   public static final Item PORKCHOP = registerItem(ItemIds.PORKCHOP, new Item.Properties().food(Foods.PORKCHOP));
   public static final Item COOKED_PORKCHOP = registerItem(ItemIds.COOKED_PORKCHOP, new Item.Properties().food(Foods.COOKED_PORKCHOP));
   public static final Item PAINTING = registerItem(ItemIds.PAINTING, p -> new HangingEntityItem(EntityTypes.PAINTING, p));
   public static final Item GOLDEN_APPLE = registerItem(ItemIds.GOLDEN_APPLE, new Item.Properties().food(Foods.GOLDEN_APPLE, Consumables.GOLDEN_APPLE));
   public static final Item ENCHANTED_GOLDEN_APPLE = registerItem(
      ItemIds.ENCHANTED_GOLDEN_APPLE,
      new Item.Properties()
         .rarity(Rarity.RARE)
         .food(Foods.ENCHANTED_GOLDEN_APPLE, Consumables.ENCHANTED_GOLDEN_APPLE)
         .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
   );
   public static final Item OAK_SIGN = registerBlock(
      BlockItemIds.OAK_SIGN, Blocks.OAK_SIGN, (b, p) -> new SignItem(b, Blocks.OAK_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item SPRUCE_SIGN = registerBlock(
      BlockItemIds.SPRUCE_SIGN, Blocks.SPRUCE_SIGN, (b, p) -> new SignItem(b, Blocks.SPRUCE_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item BIRCH_SIGN = registerBlock(
      BlockItemIds.BIRCH_SIGN, Blocks.BIRCH_SIGN, (b, p) -> new SignItem(b, Blocks.BIRCH_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item JUNGLE_SIGN = registerBlock(
      BlockItemIds.JUNGLE_SIGN, Blocks.JUNGLE_SIGN, (b, p) -> new SignItem(b, Blocks.JUNGLE_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item ACACIA_SIGN = registerBlock(
      BlockItemIds.ACACIA_SIGN, Blocks.ACACIA_SIGN, (b, p) -> new SignItem(b, Blocks.ACACIA_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item CHERRY_SIGN = registerBlock(
      BlockItemIds.CHERRY_SIGN, Blocks.CHERRY_SIGN, (b, p) -> new SignItem(b, Blocks.CHERRY_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item DARK_OAK_SIGN = registerBlock(
      BlockItemIds.DARK_OAK_SIGN, Blocks.DARK_OAK_SIGN, (b, p) -> new SignItem(b, Blocks.DARK_OAK_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item PALE_OAK_SIGN = registerBlock(
      BlockItemIds.PALE_OAK_SIGN, Blocks.PALE_OAK_SIGN, (b, p) -> new SignItem(b, Blocks.PALE_OAK_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item MANGROVE_SIGN = registerBlock(
      BlockItemIds.MANGROVE_SIGN, Blocks.MANGROVE_SIGN, (b, p) -> new SignItem(b, Blocks.MANGROVE_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item POPLAR_SIGN = registerBlock(
      BlockItemIds.POPLAR_SIGN, Blocks.POPLAR_SIGN, (b, p) -> new SignItem(b, Blocks.POPLAR_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item BAMBOO_SIGN = registerBlock(
      BlockItemIds.BAMBOO_SIGN, Blocks.BAMBOO_SIGN, (b, p) -> new SignItem(b, Blocks.BAMBOO_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item CRIMSON_SIGN = registerBlock(
      BlockItemIds.CRIMSON_SIGN, Blocks.CRIMSON_SIGN, (b, p) -> new SignItem(b, Blocks.CRIMSON_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item WARPED_SIGN = registerBlock(
      BlockItemIds.WARPED_SIGN, Blocks.WARPED_SIGN, (b, p) -> new SignItem(b, Blocks.WARPED_WALL_SIGN, p), new Item.Properties().stacksTo(16)
   );
   public static final Item OAK_HANGING_SIGN = registerBlock(
      BlockItemIds.OAK_HANGING_SIGN,
      Blocks.OAK_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.OAK_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item SPRUCE_HANGING_SIGN = registerBlock(
      BlockItemIds.SPRUCE_HANGING_SIGN,
      Blocks.SPRUCE_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.SPRUCE_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item BIRCH_HANGING_SIGN = registerBlock(
      BlockItemIds.BIRCH_HANGING_SIGN,
      Blocks.BIRCH_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.BIRCH_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item JUNGLE_HANGING_SIGN = registerBlock(
      BlockItemIds.JUNGLE_HANGING_SIGN,
      Blocks.JUNGLE_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.JUNGLE_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item ACACIA_HANGING_SIGN = registerBlock(
      BlockItemIds.ACACIA_HANGING_SIGN,
      Blocks.ACACIA_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.ACACIA_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item CHERRY_HANGING_SIGN = registerBlock(
      BlockItemIds.CHERRY_HANGING_SIGN,
      Blocks.CHERRY_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.CHERRY_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item DARK_OAK_HANGING_SIGN = registerBlock(
      BlockItemIds.DARK_OAK_HANGING_SIGN,
      Blocks.DARK_OAK_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.DARK_OAK_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item PALE_OAK_HANGING_SIGN = registerBlock(
      BlockItemIds.PALE_OAK_HANGING_SIGN,
      Blocks.PALE_OAK_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.PALE_OAK_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item MANGROVE_HANGING_SIGN = registerBlock(
      BlockItemIds.MANGROVE_HANGING_SIGN,
      Blocks.MANGROVE_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.MANGROVE_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item POPLAR_HANGING_SIGN = registerBlock(
      BlockItemIds.POPLAR_HANGING_SIGN,
      Blocks.POPLAR_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.POPLAR_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item BAMBOO_HANGING_SIGN = registerBlock(
      BlockItemIds.BAMBOO_HANGING_SIGN,
      Blocks.BAMBOO_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.BAMBOO_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item CRIMSON_HANGING_SIGN = registerBlock(
      BlockItemIds.CRIMSON_HANGING_SIGN,
      Blocks.CRIMSON_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.CRIMSON_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item WARPED_HANGING_SIGN = registerBlock(
      BlockItemIds.WARPED_HANGING_SIGN,
      Blocks.WARPED_HANGING_SIGN,
      (b, p) -> new HangingSignItem(b, Blocks.WARPED_WALL_HANGING_SIGN, p),
      new Item.Properties().stacksTo(16)
   );
   public static final Item BUCKET = registerItem(ItemIds.BUCKET, p -> new BucketItem(Fluids.EMPTY, p), new Item.Properties().stacksTo(16));
   public static final Item WATER_BUCKET = registerItem(
      ItemIds.WATER_BUCKET, p -> new BucketItem(Fluids.WATER, p), new Item.Properties().craftRemainder(BUCKET).stacksTo(1)
   );
   public static final Item LAVA_BUCKET = registerItem(
      ItemIds.LAVA_BUCKET, p -> new BucketItem(Fluids.LAVA, p), new Item.Properties().craftRemainder(BUCKET).stacksTo(1)
   );
   public static final Item POWDER_SNOW_BUCKET = registerItem(
      BlockItemIds.POWDER_SNOW,
      p -> new SolidBucketItem(Blocks.POWDER_SNOW, SoundEvents.BUCKET_EMPTY_POWDER_SNOW, p),
      new Item.Properties().stacksTo(1).useItemDescriptionPrefix()
   );
   public static final Item SNOWBALL = registerItem(ItemIds.SNOWBALL, SnowballItem::new, new Item.Properties().stacksTo(16));
   public static final Item LEATHER = registerItem(ItemIds.LEATHER);
   public static final Item MILK_BUCKET = registerItem(
      ItemIds.MILK_BUCKET,
      new Item.Properties().craftRemainder(BUCKET).component(DataComponents.CONSUMABLE, Consumables.MILK_BUCKET).usingConvertsTo(BUCKET).stacksTo(1)
   );
   public static final Item PUFFERFISH_BUCKET = registerItem(
      ItemIds.PUFFERFISH_BUCKET,
      p -> new MobBucketItem(EntityTypes.PUFFERFISH, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, p),
      new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).component(DataComponents.FOOD, Foods.PUFFERFISH)
   );
   public static final Item SALMON_BUCKET = registerItem(
      ItemIds.SALMON_BUCKET,
      p -> new MobBucketItem(EntityTypes.SALMON, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, p),
      new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).component(DataComponents.FOOD, Foods.SALMON)
   );
   public static final Item COD_BUCKET = registerItem(
      ItemIds.COD_BUCKET,
      p -> new MobBucketItem(EntityTypes.COD, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, p),
      new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).component(DataComponents.FOOD, Foods.COD)
   );
   public static final Item TROPICAL_FISH_BUCKET = registerItem(
      ItemIds.TROPICAL_FISH_BUCKET,
      p -> new MobBucketItem(EntityTypes.TROPICAL_FISH, Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, p),
      new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).component(DataComponents.FOOD, Foods.TROPICAL_FISH)
   );
   public static final Item AXOLOTL_BUCKET = registerItem(
      ItemIds.AXOLOTL_BUCKET,
      p -> new MobBucketItem(EntityTypes.AXOLOTL, Fluids.WATER, SoundEvents.BUCKET_EMPTY_AXOLOTL, p),
      new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)
   );
   public static final Item SULFUR_CUBE_BUCKET = registerItem(
      ItemIds.SULFUR_CUBE_BUCKET,
      p -> new MobBucketItem(EntityTypes.SULFUR_CUBE, Fluids.EMPTY, SoundEvents.BUCKET_EMPTY_SULFUR_CUBE, p),
      new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)
   );
   public static final Item TADPOLE_BUCKET = registerItem(
      ItemIds.TADPOLE_BUCKET,
      p -> new MobBucketItem(EntityTypes.TADPOLE, Fluids.WATER, SoundEvents.BUCKET_EMPTY_TADPOLE, p),
      new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY)
   );
   public static final Item BRICK = registerItem(ItemIds.BRICK);
   public static final Item CLAY_BALL = registerItem(ItemIds.CLAY_BALL);
   public static final Item DRIED_KELP_BLOCK = registerBlock(
      BlockItemIds.DRIED_KELP_BLOCK, Blocks.DRIED_KELP_BLOCK, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item PAPER = registerItem(ItemIds.PAPER);
   public static final Item BOOK = registerItem(ItemIds.BOOK, new Item.Properties().enchantable(1));
   public static final Item SLIME_BALL = registerItem(ItemIds.SLIME_BALL);
   public static final Item EGG = registerItem(
      ItemIds.EGG, EggItem::new, new Item.Properties().stacksTo(16).delayedHolderComponent(DataComponents.CHICKEN_VARIANT, ChickenVariants.TEMPERATE)
   );
   public static final Item BLUE_EGG = registerItem(
      ItemIds.BLUE_EGG, EggItem::new, new Item.Properties().stacksTo(16).delayedHolderComponent(DataComponents.CHICKEN_VARIANT, ChickenVariants.COLD)
   );
   public static final Item BROWN_EGG = registerItem(
      ItemIds.BROWN_EGG, EggItem::new, new Item.Properties().stacksTo(16).delayedHolderComponent(DataComponents.CHICKEN_VARIANT, ChickenVariants.WARM)
   );
   public static final Item COMPASS = registerItem(ItemIds.COMPASS, CompassItem::new);
   public static final Item RECOVERY_COMPASS = registerItem(ItemIds.RECOVERY_COMPASS, new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final Item BUNDLE = registerItem(
      ItemIds.BUNDLE, BundleItem::new, new Item.Properties().stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
   );
   public static final ColorCollection<Item> DYED_BUNDLE = ColorCollection.registerItems(
      ItemIds.DYED_BUNDLE,
      (name, var1) -> registerItem(name, BundleItem::new, new Item.Properties().stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY))
   );
   public static final ColorCollection<Item> CUSHION = ColorCollection.registerItems(
      ItemIds.CUSHION, (name, color) -> registerItem(name, p -> new CushionItem(p.stacksTo(16), color))
   );
   public static final Item FISHING_ROD = registerItem(ItemIds.FISHING_ROD, FishingRodItem::new, new Item.Properties().durability(64).enchantable(1));
   public static final Item CLOCK = registerItem(ItemIds.CLOCK);
   public static final Item SPYGLASS = registerItem(ItemIds.SPYGLASS, SpyglassItem::new, new Item.Properties().stacksTo(1));
   public static final Item GLOWSTONE_DUST = registerItem(ItemIds.GLOWSTONE_DUST);
   public static final Item COD = registerItem(ItemIds.COD, new Item.Properties().food(Foods.COD));
   public static final Item SALMON = registerItem(ItemIds.SALMON, new Item.Properties().food(Foods.SALMON));
   public static final Item TROPICAL_FISH = registerItem(ItemIds.TROPICAL_FISH, new Item.Properties().food(Foods.TROPICAL_FISH));
   public static final Item PUFFERFISH = registerItem(ItemIds.PUFFERFISH, new Item.Properties().food(Foods.PUFFERFISH, Consumables.PUFFERFISH));
   public static final Item COOKED_COD = registerItem(ItemIds.COOKED_COD, new Item.Properties().food(Foods.COOKED_COD));
   public static final Item COOKED_SALMON = registerItem(ItemIds.COOKED_SALMON, new Item.Properties().food(Foods.COOKED_SALMON));
   public static final Item INK_SAC = registerItem(ItemIds.INK_SAC, InkSacItem::new);
   public static final Item GLOW_INK_SAC = registerItem(ItemIds.GLOW_INK_SAC, GlowInkSacItem::new);
   public static final Item COCOA_BEANS = registerItem(
      BlockItemIds.COCOA_CROP, createBlockItemWithCustomItemName(Blocks.COCOA), new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final ColorCollection<Item> DYE = ColorCollection.registerItems(
      ItemIds.DYE, (id, color) -> registerItem(id, DyeItem::new, new Item.Properties().component(DataComponents.DYE, color))
   );
   public static final Item BONE_MEAL = registerItem(ItemIds.BONE_MEAL, BoneMealItem::new);
   public static final Item BONE = registerItem(ItemIds.BONE);
   public static final Item SUGAR = registerItem(ItemIds.SUGAR);
   public static final Item CAKE = registerBlock(
      BlockItemIds.CAKE, Blocks.CAKE, new Item.Properties().stacksTo(1).compostable(NumberProviders.COMPOSTABLE_ALWAYS_ADD_ONE)
   );
   public static final ColorCollection<Item> BED = ColorCollection.registerBlockItems(
      BlockItemIds.BED, Blocks.BED, (id, block, var2) -> registerBlock(id, block, new Item.Properties().stacksTo(1))
   );
   public static final Item STRAW_BED = registerBlock(
      BlockItemIds.STRAW_BED, Blocks.STRAW_BED, new Item.Properties().stacksTo(16).compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item COOKIE = registerItem(ItemIds.COOKIE, new Item.Properties().food(Foods.COOKIE).compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH));
   public static final Item CRAFTER = registerBlock(
      BlockItemIds.CRAFTER, Blocks.CRAFTER, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item FILLED_MAP = registerItem(
      ItemIds.FILLED_MAP,
      MapItem::new,
      new Item.Properties().component(DataComponents.MAP_COLOR, MapItemColor.DEFAULT).component(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY)
   );
   public static final Item SHEARS = registerItem(
      ItemIds.SHEARS, ShearsItem::new, new Item.Properties().durability(238).component(DataComponents.TOOL, ShearsItem.createToolProperties())
   );
   public static final Item MELON_SLICE = registerItem(
      ItemIds.MELON_SLICE, new Item.Properties().food(Foods.MELON_SLICE).compostable(NumberProviders.COMPOSTABLE_LOW_MEDIUM)
   );
   public static final Item DRIED_KELP = registerItem(
      ItemIds.DRIED_KELP, new Item.Properties().food(Foods.DRIED_KELP, Consumables.DRIED_KELP).compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item PUMPKIN_SEEDS = registerItem(
      BlockItemIds.PUMPKIN_CROP.item(),
      createBlockItemWithCustomItemName(Blocks.PUMPKIN_STEM),
      new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item MELON_SEEDS = registerItem(
      BlockItemIds.MELON_CROP.item(), createBlockItemWithCustomItemName(Blocks.MELON_STEM), new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BEEF = registerItem(ItemIds.BEEF, new Item.Properties().food(Foods.BEEF));
   public static final Item COOKED_BEEF = registerItem(ItemIds.COOKED_BEEF, new Item.Properties().food(Foods.COOKED_BEEF));
   public static final Item CHICKEN = registerItem(ItemIds.CHICKEN, new Item.Properties().food(Foods.CHICKEN, Consumables.CHICKEN));
   public static final Item COOKED_CHICKEN = registerItem(ItemIds.COOKED_CHICKEN, new Item.Properties().food(Foods.COOKED_CHICKEN));
   public static final Item ROTTEN_FLESH = registerItem(ItemIds.ROTTEN_FLESH, new Item.Properties().food(Foods.ROTTEN_FLESH, Consumables.ROTTEN_FLESH));
   public static final Item ENDER_PEARL = registerItem(ItemIds.ENDER_PEARL, EnderpearlItem::new, new Item.Properties().stacksTo(16).useCooldown(1.0F));
   public static final Item BLAZE_ROD = registerItem(ItemIds.BLAZE_ROD);
   public static final Item GHAST_TEAR = registerItem(ItemIds.GHAST_TEAR);
   public static final Item GOLD_NUGGET = registerItem(ItemIds.GOLD_NUGGET);
   public static final Item NETHER_WART = registerItem(
      BlockItemIds.NETHER_WART, createBlockItemWithCustomItemName(Blocks.NETHER_WART), new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item GLASS_BOTTLE = registerItem(ItemIds.GLASS_BOTTLE, BottleItem::new);
   public static final Item POTION = registerItem(
      ItemIds.POTION,
      PotionItem::new,
      new Item.Properties()
         .stacksTo(1)
         .component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
         .component(DataComponents.CONSUMABLE, Consumables.DEFAULT_DRINK)
         .usingConvertsTo(GLASS_BOTTLE)
   );
   public static final Item SPIDER_EYE = registerItem(ItemIds.SPIDER_EYE, new Item.Properties().food(Foods.SPIDER_EYE, Consumables.SPIDER_EYE));
   public static final Item FERMENTED_SPIDER_EYE = registerItem(ItemIds.FERMENTED_SPIDER_EYE);
   public static final Item BLAZE_POWDER = registerItem(ItemIds.BLAZE_POWDER);
   public static final Item MAGMA_CREAM = registerItem(ItemIds.MAGMA_CREAM);
   public static final Item BREWING_STAND = registerBlock(
      BlockItemIds.BREWING_STAND, Blocks.BREWING_STAND, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item CAULDRON = registerBlock(
      BlockItemIds.CAULDRON, Blocks.CAULDRON, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON, Blocks.POWDER_SNOW_CAULDRON
   );
   public static final Item ENDER_EYE = registerItem(ItemIds.ENDER_EYE, EnderEyeItem::new);
   public static final Item GLISTERING_MELON_SLICE = registerItem(ItemIds.GLISTERING_MELON_SLICE);
   public static final Item CHICKEN_SPAWN_EGG = registerSpawnEgg(ItemIds.CHICKEN_SPAWN_EGG, EntityTypes.CHICKEN);
   public static final Item COW_SPAWN_EGG = registerSpawnEgg(ItemIds.COW_SPAWN_EGG, EntityTypes.COW);
   public static final Item PIG_SPAWN_EGG = registerSpawnEgg(ItemIds.PIG_SPAWN_EGG, EntityTypes.PIG);
   public static final Item SHEEP_SPAWN_EGG = registerSpawnEgg(ItemIds.SHEEP_SPAWN_EGG, EntityTypes.SHEEP);
   public static final Item CAMEL_SPAWN_EGG = registerSpawnEgg(ItemIds.CAMEL_SPAWN_EGG, EntityTypes.CAMEL);
   public static final Item DONKEY_SPAWN_EGG = registerSpawnEgg(ItemIds.DONKEY_SPAWN_EGG, EntityTypes.DONKEY);
   public static final Item HORSE_SPAWN_EGG = registerSpawnEgg(ItemIds.HORSE_SPAWN_EGG, EntityTypes.HORSE);
   public static final Item MULE_SPAWN_EGG = registerSpawnEgg(ItemIds.MULE_SPAWN_EGG, EntityTypes.MULE);
   public static final Item CAT_SPAWN_EGG = registerSpawnEgg(ItemIds.CAT_SPAWN_EGG, EntityTypes.CAT);
   public static final Item PARROT_SPAWN_EGG = registerSpawnEgg(ItemIds.PARROT_SPAWN_EGG, EntityTypes.PARROT);
   public static final Item WOLF_SPAWN_EGG = registerSpawnEgg(ItemIds.WOLF_SPAWN_EGG, EntityTypes.WOLF);
   public static final Item ARMADILLO_SPAWN_EGG = registerSpawnEgg(ItemIds.ARMADILLO_SPAWN_EGG, EntityTypes.ARMADILLO);
   public static final Item BAT_SPAWN_EGG = registerSpawnEgg(ItemIds.BAT_SPAWN_EGG, EntityTypes.BAT);
   public static final Item BEE_SPAWN_EGG = registerSpawnEgg(ItemIds.BEE_SPAWN_EGG, EntityTypes.BEE);
   public static final Item FOX_SPAWN_EGG = registerSpawnEgg(ItemIds.FOX_SPAWN_EGG, EntityTypes.FOX);
   public static final Item GOAT_SPAWN_EGG = registerSpawnEgg(ItemIds.GOAT_SPAWN_EGG, EntityTypes.GOAT);
   public static final Item LLAMA_SPAWN_EGG = registerSpawnEgg(ItemIds.LLAMA_SPAWN_EGG, EntityTypes.LLAMA);
   public static final Item OCELOT_SPAWN_EGG = registerSpawnEgg(ItemIds.OCELOT_SPAWN_EGG, EntityTypes.OCELOT);
   public static final Item PANDA_SPAWN_EGG = registerSpawnEgg(ItemIds.PANDA_SPAWN_EGG, EntityTypes.PANDA);
   public static final Item POLAR_BEAR_SPAWN_EGG = registerSpawnEgg(ItemIds.POLAR_BEAR_SPAWN_EGG, EntityTypes.POLAR_BEAR);
   public static final Item RABBIT_SPAWN_EGG = registerSpawnEgg(ItemIds.RABBIT_SPAWN_EGG, EntityTypes.RABBIT);
   public static final Item AXOLOTL_SPAWN_EGG = registerSpawnEgg(ItemIds.AXOLOTL_SPAWN_EGG, EntityTypes.AXOLOTL);
   public static final Item COD_SPAWN_EGG = registerSpawnEgg(ItemIds.COD_SPAWN_EGG, EntityTypes.COD);
   public static final Item DOLPHIN_SPAWN_EGG = registerSpawnEgg(ItemIds.DOLPHIN_SPAWN_EGG, EntityTypes.DOLPHIN);
   public static final Item FROG_SPAWN_EGG = registerSpawnEgg(ItemIds.FROG_SPAWN_EGG, EntityTypes.FROG);
   public static final Item GLOW_SQUID_SPAWN_EGG = registerSpawnEgg(ItemIds.GLOW_SQUID_SPAWN_EGG, EntityTypes.GLOW_SQUID);
   public static final Item NAUTILUS_SPAWN_EGG = registerSpawnEgg(ItemIds.NAUTILUS_SPAWN_EGG, EntityTypes.NAUTILUS);
   public static final Item PUFFERFISH_SPAWN_EGG = registerSpawnEgg(ItemIds.PUFFERFISH_SPAWN_EGG, EntityTypes.PUFFERFISH);
   public static final Item SALMON_SPAWN_EGG = registerSpawnEgg(ItemIds.SALMON_SPAWN_EGG, EntityTypes.SALMON);
   public static final Item SQUID_SPAWN_EGG = registerSpawnEgg(ItemIds.SQUID_SPAWN_EGG, EntityTypes.SQUID);
   public static final Item TADPOLE_SPAWN_EGG = registerSpawnEgg(ItemIds.TADPOLE_SPAWN_EGG, EntityTypes.TADPOLE);
   public static final Item TROPICAL_FISH_SPAWN_EGG = registerSpawnEgg(ItemIds.TROPICAL_FISH_SPAWN_EGG, EntityTypes.TROPICAL_FISH);
   public static final Item TURTLE_SPAWN_EGG = registerSpawnEgg(ItemIds.TURTLE_SPAWN_EGG, EntityTypes.TURTLE);
   public static final Item ALLAY_SPAWN_EGG = registerSpawnEgg(ItemIds.ALLAY_SPAWN_EGG, EntityTypes.ALLAY);
   public static final Item MOOSHROOM_SPAWN_EGG = registerSpawnEgg(ItemIds.MOOSHROOM_SPAWN_EGG, EntityTypes.MOOSHROOM);
   public static final Item SNIFFER_SPAWN_EGG = registerSpawnEgg(ItemIds.SNIFFER_SPAWN_EGG, EntityTypes.SNIFFER);
   public static final Item SULFUR_CUBE_SPAWN_EGG = registerSpawnEgg(ItemIds.SULFUR_CUBE_SPAWN_EGG, EntityTypes.SULFUR_CUBE);
   public static final Item COPPER_GOLEM_SPAWN_EGG = registerSpawnEgg(ItemIds.COPPER_GOLEM_SPAWN_EGG, EntityTypes.COPPER_GOLEM);
   public static final Item IRON_GOLEM_SPAWN_EGG = registerSpawnEgg(ItemIds.IRON_GOLEM_SPAWN_EGG, EntityTypes.IRON_GOLEM);
   public static final Item SNOW_GOLEM_SPAWN_EGG = registerSpawnEgg(ItemIds.SNOW_GOLEM_SPAWN_EGG, EntityTypes.SNOW_GOLEM);
   public static final Item TRADER_LLAMA_SPAWN_EGG = registerSpawnEgg(ItemIds.TRADER_LLAMA_SPAWN_EGG, EntityTypes.TRADER_LLAMA);
   public static final Item VILLAGER_SPAWN_EGG = registerSpawnEgg(ItemIds.VILLAGER_SPAWN_EGG, EntityTypes.VILLAGER);
   public static final Item WANDERING_TRADER_SPAWN_EGG = registerSpawnEgg(ItemIds.WANDERING_TRADER_SPAWN_EGG, EntityTypes.WANDERING_TRADER);
   public static final Item BOGGED_SPAWN_EGG = registerSpawnEgg(ItemIds.BOGGED_SPAWN_EGG, EntityTypes.BOGGED);
   public static final Item CAMEL_HUSK_SPAWN_EGG = registerSpawnEgg(ItemIds.CAMEL_HUSK_SPAWN_EGG, EntityTypes.CAMEL_HUSK);
   public static final Item DROWNED_SPAWN_EGG = registerSpawnEgg(ItemIds.DROWNED_SPAWN_EGG, EntityTypes.DROWNED);
   public static final Item HUSK_SPAWN_EGG = registerSpawnEgg(ItemIds.HUSK_SPAWN_EGG, EntityTypes.HUSK);
   public static final Item PARCHED_SPAWN_EGG = registerSpawnEgg(ItemIds.PARCHED_SPAWN_EGG, EntityTypes.PARCHED);
   public static final Item SKELETON_SPAWN_EGG = registerSpawnEgg(ItemIds.SKELETON_SPAWN_EGG, EntityTypes.SKELETON);
   public static final Item SKELETON_HORSE_SPAWN_EGG = registerSpawnEgg(ItemIds.SKELETON_HORSE_SPAWN_EGG, EntityTypes.SKELETON_HORSE);
   public static final Item STRAY_SPAWN_EGG = registerSpawnEgg(ItemIds.STRAY_SPAWN_EGG, EntityTypes.STRAY);
   public static final Item WITHER_SPAWN_EGG = registerSpawnEgg(ItemIds.WITHER_SPAWN_EGG, EntityTypes.WITHER);
   public static final Item WITHER_SKELETON_SPAWN_EGG = registerSpawnEgg(ItemIds.WITHER_SKELETON_SPAWN_EGG, EntityTypes.WITHER_SKELETON);
   public static final Item ZOMBIE_SPAWN_EGG = registerSpawnEgg(ItemIds.ZOMBIE_SPAWN_EGG, EntityTypes.ZOMBIE);
   public static final Item ZOMBIE_HORSE_SPAWN_EGG = registerSpawnEgg(ItemIds.ZOMBIE_HORSE_SPAWN_EGG, EntityTypes.ZOMBIE_HORSE);
   public static final Item ZOMBIE_NAUTILUS_SPAWN_EGG = registerSpawnEgg(ItemIds.ZOMBIE_NAUTILUS_SPAWN_EGG, EntityTypes.ZOMBIE_NAUTILUS);
   public static final Item ZOMBIE_VILLAGER_SPAWN_EGG = registerSpawnEgg(ItemIds.ZOMBIE_VILLAGER_SPAWN_EGG, EntityTypes.ZOMBIE_VILLAGER);
   public static final Item CAVE_SPIDER_SPAWN_EGG = registerSpawnEgg(ItemIds.CAVE_SPIDER_SPAWN_EGG, EntityTypes.CAVE_SPIDER);
   public static final Item SPIDER_SPAWN_EGG = registerSpawnEgg(ItemIds.SPIDER_SPAWN_EGG, EntityTypes.SPIDER);
   public static final Item BREEZE_SPAWN_EGG = registerSpawnEgg(ItemIds.BREEZE_SPAWN_EGG, EntityTypes.BREEZE);
   public static final Item CREAKING_SPAWN_EGG = registerSpawnEgg(ItemIds.CREAKING_SPAWN_EGG, EntityTypes.CREAKING);
   public static final Item CREEPER_SPAWN_EGG = registerSpawnEgg(ItemIds.CREEPER_SPAWN_EGG, EntityTypes.CREEPER);
   public static final Item ELDER_GUARDIAN_SPAWN_EGG = registerSpawnEgg(ItemIds.ELDER_GUARDIAN_SPAWN_EGG, EntityTypes.ELDER_GUARDIAN);
   public static final Item GUARDIAN_SPAWN_EGG = registerSpawnEgg(ItemIds.GUARDIAN_SPAWN_EGG, EntityTypes.GUARDIAN);
   public static final Item PHANTOM_SPAWN_EGG = registerSpawnEgg(ItemIds.PHANTOM_SPAWN_EGG, EntityTypes.PHANTOM);
   public static final Item SILVERFISH_SPAWN_EGG = registerSpawnEgg(ItemIds.SILVERFISH_SPAWN_EGG, EntityTypes.SILVERFISH);
   public static final Item SLIME_SPAWN_EGG = registerSpawnEgg(ItemIds.SLIME_SPAWN_EGG, EntityTypes.SLIME);
   public static final Item WARDEN_SPAWN_EGG = registerSpawnEgg(ItemIds.WARDEN_SPAWN_EGG, EntityTypes.WARDEN);
   public static final Item WITCH_SPAWN_EGG = registerSpawnEgg(ItemIds.WITCH_SPAWN_EGG, EntityTypes.WITCH);
   public static final Item EVOKER_SPAWN_EGG = registerSpawnEgg(ItemIds.EVOKER_SPAWN_EGG, EntityTypes.EVOKER);
   public static final Item PILLAGER_SPAWN_EGG = registerSpawnEgg(ItemIds.PILLAGER_SPAWN_EGG, EntityTypes.PILLAGER);
   public static final Item RAVAGER_SPAWN_EGG = registerSpawnEgg(ItemIds.RAVAGER_SPAWN_EGG, EntityTypes.RAVAGER);
   public static final Item VINDICATOR_SPAWN_EGG = registerSpawnEgg(ItemIds.VINDICATOR_SPAWN_EGG, EntityTypes.VINDICATOR);
   public static final Item VEX_SPAWN_EGG = registerSpawnEgg(ItemIds.VEX_SPAWN_EGG, EntityTypes.VEX);
   public static final Item BLAZE_SPAWN_EGG = registerSpawnEgg(ItemIds.BLAZE_SPAWN_EGG, EntityTypes.BLAZE);
   public static final Item GHAST_SPAWN_EGG = registerSpawnEgg(ItemIds.GHAST_SPAWN_EGG, EntityTypes.GHAST);
   public static final Item HAPPY_GHAST_SPAWN_EGG = registerSpawnEgg(ItemIds.HAPPY_GHAST_SPAWN_EGG, EntityTypes.HAPPY_GHAST);
   public static final Item HOGLIN_SPAWN_EGG = registerSpawnEgg(ItemIds.HOGLIN_SPAWN_EGG, EntityTypes.HOGLIN);
   public static final Item MAGMA_CUBE_SPAWN_EGG = registerSpawnEgg(ItemIds.MAGMA_CUBE_SPAWN_EGG, EntityTypes.MAGMA_CUBE);
   public static final Item PIGLIN_SPAWN_EGG = registerSpawnEgg(ItemIds.PIGLIN_SPAWN_EGG, EntityTypes.PIGLIN);
   public static final Item PIGLIN_BRUTE_SPAWN_EGG = registerSpawnEgg(ItemIds.PIGLIN_BRUTE_SPAWN_EGG, EntityTypes.PIGLIN_BRUTE);
   public static final Item STRIDER_SPAWN_EGG = registerSpawnEgg(ItemIds.STRIDER_SPAWN_EGG, EntityTypes.STRIDER);
   public static final Item ZOGLIN_SPAWN_EGG = registerSpawnEgg(ItemIds.ZOGLIN_SPAWN_EGG, EntityTypes.ZOGLIN);
   public static final Item ZOMBIFIED_PIGLIN_SPAWN_EGG = registerSpawnEgg(ItemIds.ZOMBIFIED_PIGLIN_SPAWN_EGG, EntityTypes.ZOMBIFIED_PIGLIN);
   public static final Item ENDER_DRAGON_SPAWN_EGG = registerSpawnEgg(ItemIds.ENDER_DRAGON_SPAWN_EGG, EntityTypes.ENDER_DRAGON);
   public static final Item ENDERMAN_SPAWN_EGG = registerSpawnEgg(ItemIds.ENDERMAN_SPAWN_EGG, EntityTypes.ENDERMAN);
   public static final Item ENDERMITE_SPAWN_EGG = registerSpawnEgg(ItemIds.ENDERMITE_SPAWN_EGG, EntityTypes.ENDERMITE);
   public static final Item SHULKER_SPAWN_EGG = registerSpawnEgg(ItemIds.SHULKER_SPAWN_EGG, EntityTypes.SHULKER);
   public static final Item EXPERIENCE_BOTTLE = registerItem(
      ItemIds.EXPERIENCE_BOTTLE,
      ExperienceBottleItem::new,
      new Item.Properties().rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
   );
   public static final Item FIRE_CHARGE = registerItem(ItemIds.FIRE_CHARGE, FireChargeItem::new);
   public static final Item WIND_CHARGE = registerItem(ItemIds.WIND_CHARGE, WindChargeItem::new, new Item.Properties().useCooldown(0.5F));
   public static final Item WRITABLE_BOOK = registerItem(
      ItemIds.WRITABLE_BOOK,
      WritableBookItem::new,
      new Item.Properties().stacksTo(1).component(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY)
   );
   public static final Item WRITTEN_BOOK = registerItem(
      ItemIds.WRITTEN_BOOK, WrittenBookItem::new, new Item.Properties().stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
   );
   public static final Item BREEZE_ROD = registerItem(ItemIds.BREEZE_ROD);
   public static final Item MACE = registerItem(
      ItemIds.MACE,
      MaceItem::new,
      new Item.Properties()
         .rarity(Rarity.EPIC)
         .durability(500)
         .component(DataComponents.TOOL, MaceItem.createToolProperties())
         .repairable(BREEZE_ROD)
         .attributes(MaceItem.createAttributes())
         .enchantable(15)
         .component(DataComponents.WEAPON, new Weapon(1))
   );
   public static final Item ITEM_FRAME = registerItem(ItemIds.ITEM_FRAME, p -> new ItemFrameItem(EntityTypes.ITEM_FRAME, p));
   public static final Item GLOW_ITEM_FRAME = registerItem(ItemIds.GLOW_ITEM_FRAME, p -> new ItemFrameItem(EntityTypes.GLOW_ITEM_FRAME, p));
   public static final Item FLOWER_POT = registerBlock(BlockItemIds.FLOWER_POT, Blocks.FLOWER_POT);
   public static final Item CARROT = registerItem(
      BlockItemIds.CARROT_CROP,
      createBlockItemWithCustomItemName(Blocks.CARROTS),
      new Item.Properties().food(Foods.CARROT).compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item POTATO = registerItem(
      BlockItemIds.POTATO_CROP,
      createBlockItemWithCustomItemName(Blocks.POTATOES),
      new Item.Properties().food(Foods.POTATO).compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item BAKED_POTATO = registerItem(
      ItemIds.BAKED_POTATO, new Item.Properties().food(Foods.BAKED_POTATO).compostable(NumberProviders.COMPOSTABLE_MEDIUM_HIGH)
   );
   public static final Item POISONOUS_POTATO = registerItem(
      ItemIds.POISONOUS_POTATO, new Item.Properties().food(Foods.POISONOUS_POTATO, Consumables.POISONOUS_POTATO)
   );
   public static final Item MAP = registerItem(ItemIds.MAP, EmptyMapItem::new);
   public static final Item GOLDEN_CARROT = registerItem(ItemIds.GOLDEN_CARROT, new Item.Properties().food(Foods.GOLDEN_CARROT));
   public static final Item SKELETON_SKULL = registerBlock(
      BlockItemIds.SKELETON_SKULL,
      Blocks.SKELETON_SKULL,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.SKELETON_WALL_SKULL, Direction.DOWN, Waypoint.addHideAttribute(p)),
      new Item.Properties().rarity(Rarity.UNCOMMON).equippableUnswappable(EquipmentSlot.HEAD)
   );
   public static final Item WITHER_SKELETON_SKULL = registerBlock(
      BlockItemIds.WITHER_SKELETON_SKULL,
      Blocks.WITHER_SKELETON_SKULL,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.WITHER_SKELETON_WALL_SKULL, Direction.DOWN, Waypoint.addHideAttribute(p)),
      new Item.Properties().rarity(Rarity.RARE).equippableUnswappable(EquipmentSlot.HEAD)
   );
   public static final Item PLAYER_HEAD = registerBlock(
      BlockItemIds.PLAYER_HEAD,
      Blocks.PLAYER_HEAD,
      (b, p) -> new PlayerHeadItem(b, Blocks.PLAYER_WALL_HEAD, Waypoint.addHideAttribute(p)),
      new Item.Properties().rarity(Rarity.UNCOMMON).equippableUnswappable(EquipmentSlot.HEAD)
   );
   public static final Item ZOMBIE_HEAD = registerBlock(
      BlockItemIds.ZOMBIE_HEAD,
      Blocks.ZOMBIE_HEAD,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.ZOMBIE_WALL_HEAD, Direction.DOWN, Waypoint.addHideAttribute(p)),
      new Item.Properties().rarity(Rarity.UNCOMMON).equippableUnswappable(EquipmentSlot.HEAD)
   );
   public static final Item CREEPER_HEAD = registerBlock(
      BlockItemIds.CREEPER_HEAD,
      Blocks.CREEPER_HEAD,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.CREEPER_WALL_HEAD, Direction.DOWN, Waypoint.addHideAttribute(p)),
      new Item.Properties().rarity(Rarity.UNCOMMON).equippableUnswappable(EquipmentSlot.HEAD)
   );
   public static final Item DRAGON_HEAD = registerBlock(
      BlockItemIds.DRAGON_HEAD,
      Blocks.DRAGON_HEAD,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.DRAGON_WALL_HEAD, Direction.DOWN, Waypoint.addHideAttribute(p)),
      new Item.Properties().rarity(Rarity.EPIC).equippableUnswappable(EquipmentSlot.HEAD)
   );
   public static final Item PIGLIN_HEAD = registerBlock(
      BlockItemIds.PIGLIN_HEAD,
      Blocks.PIGLIN_HEAD,
      (b, p) -> new StandingAndWallBlockItem(b, Blocks.PIGLIN_WALL_HEAD, Direction.DOWN, Waypoint.addHideAttribute(p)),
      new Item.Properties().rarity(Rarity.UNCOMMON).equippableUnswappable(EquipmentSlot.HEAD)
   );
   public static final Item NETHER_STAR = registerItem(
      ItemIds.NETHER_STAR,
      new Item.Properties()
         .rarity(Rarity.RARE)
         .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
         .delayedComponent(DataComponents.DAMAGE_RESISTANT, context -> new DamageResistant(context.getOrThrow(DamageTypeTags.IS_EXPLOSION)))
   );
   public static final Item PUMPKIN_PIE = registerItem(
      ItemIds.PUMPKIN_PIE, new Item.Properties().food(Foods.PUMPKIN_PIE).compostable(NumberProviders.COMPOSTABLE_ALWAYS_ADD_ONE)
   );
   public static final Item FIREWORK_ROCKET = registerItem(
      ItemIds.FIREWORK_ROCKET, FireworkRocketItem::new, new Item.Properties().component(DataComponents.FIREWORKS, new Fireworks(1, List.of()))
   );
   public static final Item FIREWORK_STAR = registerItem(ItemIds.FIREWORK_STAR);
   public static final Item ENCHANTED_BOOK = registerItem(
      ItemIds.ENCHANTED_BOOK,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.RARE)
         .component(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
         .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
   );
   public static final Item NETHER_BRICK = registerItem(ItemIds.NETHER_BRICK);
   public static final Item RESIN_BRICK = registerItem(ItemIds.RESIN_BRICK, new Item.Properties().trimMaterial(TrimMaterials.RESIN));
   public static final Item PRISMARINE_SHARD = registerItem(ItemIds.PRISMARINE_SHARD);
   public static final Item PRISMARINE_CRYSTALS = registerItem(ItemIds.PRISMARINE_CRYSTALS);
   public static final Item RABBIT = registerItem(ItemIds.RABBIT, new Item.Properties().food(Foods.RABBIT));
   public static final Item COOKED_RABBIT = registerItem(ItemIds.COOKED_RABBIT, new Item.Properties().food(Foods.COOKED_RABBIT));
   public static final Item RABBIT_STEW = registerItem(ItemIds.RABBIT_STEW, new Item.Properties().stacksTo(1).food(Foods.RABBIT_STEW).usingConvertsTo(BOWL));
   public static final Item RABBIT_FOOT = registerItem(ItemIds.RABBIT_FOOT);
   public static final Item RABBIT_HIDE = registerItem(ItemIds.RABBIT_HIDE);
   public static final Item ARMOR_STAND = registerItem(ItemIds.ARMOR_STAND, ArmorStandItem::new, new Item.Properties().stacksTo(16));
   public static final Item COPPER_HORSE_ARMOR = registerItem(ItemIds.COPPER_HORSE_ARMOR, new Item.Properties().horseArmor(ArmorMaterials.COPPER));
   public static final Item IRON_HORSE_ARMOR = registerItem(ItemIds.IRON_HORSE_ARMOR, new Item.Properties().horseArmor(ArmorMaterials.IRON));
   public static final Item GOLDEN_HORSE_ARMOR = registerItem(ItemIds.GOLDEN_HORSE_ARMOR, new Item.Properties().horseArmor(ArmorMaterials.GOLD));
   public static final Item DIAMOND_HORSE_ARMOR = registerItem(ItemIds.DIAMOND_HORSE_ARMOR, new Item.Properties().horseArmor(ArmorMaterials.DIAMOND));
   public static final Item NETHERITE_HORSE_ARMOR = registerItem(
      ItemIds.NETHERITE_HORSE_ARMOR, new Item.Properties().horseArmor(ArmorMaterials.NETHERITE).fireResistant()
   );
   public static final Item LEATHER_HORSE_ARMOR = registerItem(ItemIds.LEATHER_HORSE_ARMOR, new Item.Properties().horseArmor(ArmorMaterials.LEATHER));
   public static final Item LEAD = registerItem(ItemIds.LEAD, LeadItem::new);
   public static final Item NAME_TAG = registerItem(ItemIds.NAME_TAG, NameTagItem::new);
   public static final Item COMMAND_BLOCK_MINECART = registerItem(
      ItemIds.COMMAND_BLOCK_MINECART, p -> new MinecartItem(EntityTypes.COMMAND_BLOCK_MINECART, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
   );
   public static final Item MUTTON = registerItem(ItemIds.MUTTON, new Item.Properties().food(Foods.MUTTON));
   public static final Item COOKED_MUTTON = registerItem(ItemIds.COOKED_MUTTON, new Item.Properties().food(Foods.COOKED_MUTTON));
   public static final ColorCollection<Item> BANNER = ColorCollection.registerBlockItems(
      BlockItemIds.BANNER,
      Blocks.BANNER,
      (id, block, color) -> registerBlock(
         id,
         block,
         (b, p) -> new BannerItem(b, Blocks.WALL_BANNER.pick(color), p),
         new Item.Properties().stacksTo(16).component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
      )
   );
   public static final Item END_CRYSTAL = registerItem(
      ItemIds.END_CRYSTAL, EndCrystalItem::new, new Item.Properties().component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
   );
   public static final Item CHORUS_FRUIT = registerItem(
      ItemIds.CHORUS_FRUIT, new Item.Properties().food(Foods.CHORUS_FRUIT, Consumables.CHORUS_FRUIT).useCooldown(1.0F)
   );
   public static final Item POPPED_CHORUS_FRUIT = registerItem(ItemIds.POPPED_CHORUS_FRUIT);
   public static final Item TORCHFLOWER_SEEDS = registerItem(
      BlockItemIds.TORCHFLOWER_CROP,
      createBlockItemWithCustomItemName(Blocks.TORCHFLOWER_CROP),
      new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item PITCHER_POD = registerItem(
      BlockItemIds.PITCHER_CROP, createBlockItemWithCustomItemName(Blocks.PITCHER_CROP), new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BEETROOT = registerItem(
      ItemIds.BEETROOT, new Item.Properties().food(Foods.BEETROOT).compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item BEETROOT_SEEDS = registerItem(
      BlockItemIds.BEETROOT_CROP, createBlockItemWithCustomItemName(Blocks.BEETROOTS), new Item.Properties().compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item BEETROOT_SOUP = registerItem(
      ItemIds.BEETROOT_SOUP, new Item.Properties().stacksTo(1).food(Foods.BEETROOT_SOUP).usingConvertsTo(BOWL)
   );
   public static final Item DRAGON_BREATH = registerItem(ItemIds.DRAGON_BREATH, new Item.Properties().craftRemainder(GLASS_BOTTLE).rarity(Rarity.UNCOMMON));
   public static final Item SPLASH_POTION = registerItem(
      ItemIds.SPLASH_POTION, SplashPotionItem::new, new Item.Properties().stacksTo(1).component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
   );
   public static final Item SPECTRAL_ARROW = registerItem(ItemIds.SPECTRAL_ARROW, SpectralArrowItem::new);
   public static final Item TIPPED_ARROW = registerItem(
      ItemIds.TIPPED_ARROW,
      TippedArrowItem::new,
      new Item.Properties().component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).component(DataComponents.POTION_DURATION_SCALE, 0.125F)
   );
   public static final Item LINGERING_POTION = registerItem(
      ItemIds.LINGERING_POTION,
      LingeringPotionItem::new,
      new Item.Properties().stacksTo(1).component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).component(DataComponents.POTION_DURATION_SCALE, 0.25F)
   );
   public static final Item SHIELD = registerItem(
      ItemIds.SHIELD,
      ShieldItem::new,
      new Item.Properties()
         .durability(336)
         .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
         .repairable(ItemTags.WOODEN_TOOL_MATERIALS)
         .equippableUnswappable(EquipmentSlot.OFFHAND)
         .delayedComponent(
            DataComponents.BLOCKS_ATTACKS,
            context -> new BlocksAttacks(
               0.25F,
               1.0F,
               List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
               new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
               Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
               Optional.of(SoundEvents.SHIELD_BLOCK),
               Optional.of(SoundEvents.SHIELD_BREAK)
            )
         )
         .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK)
   );
   public static final Item WOODEN_SPEAR = registerItem(
      ItemIds.WOODEN_SPEAR, new Item.Properties().spear(ToolMaterial.WOOD, 0.65F, 0.7F, 0.75F, 5.0F, 14.0F, 10.0F, 5.1F, 15.0F, 4.6F)
   );
   public static final Item STONE_SPEAR = registerItem(
      ItemIds.STONE_SPEAR, new Item.Properties().spear(ToolMaterial.STONE, 0.75F, 0.82F, 0.7F, 4.5F, 13.0F, 9.0F, 5.1F, 13.75F, 4.6F)
   );
   public static final Item COPPER_SPEAR = registerItem(
      ItemIds.COPPER_SPEAR, new Item.Properties().spear(ToolMaterial.COPPER, 0.85F, 0.82F, 0.65F, 4.0F, 12.0F, 8.25F, 5.1F, 12.5F, 4.6F)
   );
   public static final Item IRON_SPEAR = registerItem(
      ItemIds.IRON_SPEAR, new Item.Properties().spear(ToolMaterial.IRON, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F)
   );
   public static final Item GOLDEN_SPEAR = registerItem(
      ItemIds.GOLDEN_SPEAR, new Item.Properties().spear(ToolMaterial.GOLD, 0.95F, 0.7F, 0.7F, 3.5F, 13.0F, 8.5F, 5.1F, 13.75F, 4.6F)
   );
   public static final Item DIAMOND_SPEAR = registerItem(
      ItemIds.DIAMOND_SPEAR, new Item.Properties().spear(ToolMaterial.DIAMOND, 1.05F, 1.075F, 0.5F, 3.0F, 10.0F, 6.5F, 5.1F, 10.0F, 4.6F)
   );
   public static final Item NETHERITE_SPEAR = registerItem(
      ItemIds.NETHERITE_SPEAR, new Item.Properties().spear(ToolMaterial.NETHERITE, 1.15F, 1.2F, 0.4F, 2.5F, 9.0F, 5.5F, 5.1F, 8.75F, 4.6F).fireResistant()
   );
   public static final Item TOTEM_OF_UNDYING = registerItem(
      ItemIds.TOTEM_OF_UNDYING,
      new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).component(DataComponents.DEATH_PROTECTION, DeathProtection.TOTEM_OF_UNDYING)
   );
   public static final Item SHULKER_SHELL = registerItem(ItemIds.SHULKER_SHELL);
   public static final Item IRON_NUGGET = registerItem(ItemIds.IRON_NUGGET);
   public static final Item COPPER_NUGGET = registerItem(ItemIds.COPPER_NUGGET);
   public static final Item KNOWLEDGE_BOOK = registerItem(
      ItemIds.KNOWLEDGE_BOOK, KnowledgeBookItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.RECIPES, List.of())
   );
   public static final Item DEBUG_STICK = registerItem(
      ItemIds.DEBUG_STICK,
      DebugStickItem::new,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.EPIC)
         .component(DataComponents.DEBUG_STICK_STATE, DebugStickState.EMPTY)
         .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
   );
   public static final Item MUSIC_DISC_13 = registerItem(
      ItemIds.MUSIC_DISC_13, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.THIRTEEN)
   );
   public static final Item MUSIC_DISC_CAT = registerItem(
      ItemIds.MUSIC_DISC_CAT, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.CAT)
   );
   public static final Item MUSIC_DISC_BLOCKS = registerItem(
      ItemIds.MUSIC_DISC_BLOCKS, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.BLOCKS)
   );
   public static final Item MUSIC_DISC_BOUNCE = registerItem(
      ItemIds.MUSIC_DISC_BOUNCE, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.BOUNCE)
   );
   public static final Item MUSIC_DISC_CHIRP = registerItem(
      ItemIds.MUSIC_DISC_CHIRP, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.CHIRP)
   );
   public static final Item MUSIC_DISC_CREATOR = registerItem(
      ItemIds.MUSIC_DISC_CREATOR, new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(JukeboxSongs.CREATOR)
   );
   public static final Item MUSIC_DISC_CREATOR_MUSIC_BOX = registerItem(
      ItemIds.MUSIC_DISC_CREATOR_MUSIC_BOX, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.CREATOR_MUSIC_BOX)
   );
   public static final Item MUSIC_DISC_FAR = registerItem(
      ItemIds.MUSIC_DISC_FAR, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.FAR)
   );
   public static final Item MUSIC_DISC_LAVA_CHICKEN = registerItem(
      ItemIds.MUSIC_DISC_LAVA_CHICKEN, new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(JukeboxSongs.LAVA_CHICKEN)
   );
   public static final Item MUSIC_DISC_MALL = registerItem(
      ItemIds.MUSIC_DISC_MALL, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.MALL)
   );
   public static final Item MUSIC_DISC_MELLOHI = registerItem(
      ItemIds.MUSIC_DISC_MELLOHI, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.MELLOHI)
   );
   public static final Item MUSIC_DISC_STAL = registerItem(
      ItemIds.MUSIC_DISC_STAL, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.STAL)
   );
   public static final Item MUSIC_DISC_STRAD = registerItem(
      ItemIds.MUSIC_DISC_STRAD, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.STRAD)
   );
   public static final Item MUSIC_DISC_WARD = registerItem(
      ItemIds.MUSIC_DISC_WARD, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.WARD)
   );
   public static final Item MUSIC_DISC_11 = registerItem(
      ItemIds.MUSIC_DISC_11, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.ELEVEN)
   );
   public static final Item MUSIC_DISC_WAIT = registerItem(
      ItemIds.MUSIC_DISC_WAIT, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.WAIT)
   );
   public static final Item MUSIC_DISC_OTHERSIDE = registerItem(
      ItemIds.MUSIC_DISC_OTHERSIDE, new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(JukeboxSongs.OTHERSIDE)
   );
   public static final Item MUSIC_DISC_RELIC = registerItem(
      ItemIds.MUSIC_DISC_RELIC, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.RELIC)
   );
   public static final Item MUSIC_DISC_5 = registerItem(
      ItemIds.MUSIC_DISC_5, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.FIVE)
   );
   public static final Item MUSIC_DISC_PIGSTEP = registerItem(
      ItemIds.MUSIC_DISC_PIGSTEP, new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(JukeboxSongs.PIGSTEP)
   );
   public static final Item MUSIC_DISC_PRECIPICE = registerItem(
      ItemIds.MUSIC_DISC_PRECIPICE, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.PRECIPICE)
   );
   public static final Item MUSIC_DISC_TEARS = registerItem(
      ItemIds.MUSIC_DISC_TEARS, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(JukeboxSongs.TEARS)
   );
   public static final Item DISC_FRAGMENT_5 = registerItem(ItemIds.DISC_FRAGMENT_5, DiscFragmentItem::new, new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final Item TRIDENT = registerItem(
      ItemIds.TRIDENT,
      TridentItem::new,
      new Item.Properties()
         .rarity(Rarity.RARE)
         .durability(250)
         .attributes(TridentItem.createAttributes())
         .component(DataComponents.TOOL, TridentItem.createToolProperties())
         .enchantable(1)
         .component(DataComponents.WEAPON, new Weapon(1))
   );
   public static final Item NAUTILUS_SHELL = registerItem(ItemIds.NAUTILUS_SHELL, new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final Item IRON_NAUTILUS_ARMOR = registerItem(ItemIds.IRON_NAUTILUS_ARMOR, new Item.Properties().nautilusArmor(ArmorMaterials.IRON));
   public static final Item GOLDEN_NAUTILUS_ARMOR = registerItem(ItemIds.GOLDEN_NAUTILUS_ARMOR, new Item.Properties().nautilusArmor(ArmorMaterials.GOLD));
   public static final Item DIAMOND_NAUTILUS_ARMOR = registerItem(ItemIds.DIAMOND_NAUTILUS_ARMOR, new Item.Properties().nautilusArmor(ArmorMaterials.DIAMOND));
   public static final Item NETHERITE_NAUTILUS_ARMOR = registerItem(
      ItemIds.NETHERITE_NAUTILUS_ARMOR, new Item.Properties().nautilusArmor(ArmorMaterials.NETHERITE).fireResistant()
   );
   public static final Item COPPER_NAUTILUS_ARMOR = registerItem(ItemIds.COPPER_NAUTILUS_ARMOR, new Item.Properties().nautilusArmor(ArmorMaterials.COPPER));
   public static final Item HEART_OF_THE_SEA = registerItem(ItemIds.HEART_OF_THE_SEA, new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final Item CROSSBOW = registerItem(
      ItemIds.CROSSBOW,
      CrossbowItem::new,
      new Item.Properties().stacksTo(1).durability(465).component(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).enchantable(1)
   );
   public static final Item SUSPICIOUS_STEW = registerItem(
      ItemIds.SUSPICIOUS_STEW,
      new Item.Properties()
         .stacksTo(1)
         .food(Foods.SUSPICIOUS_STEW)
         .component(DataComponents.SUSPICIOUS_STEW_EFFECTS, SuspiciousStewEffects.EMPTY)
         .usingConvertsTo(BOWL)
   );
   public static final Item LOOM = registerBlock(BlockItemIds.LOOM, Blocks.LOOM);
   public static final Item FLOWER_BANNER_PATTERN = registerItem(
      ItemIds.FLOWER_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_FLOWER))
   );
   public static final Item CREEPER_BANNER_PATTERN = registerItem(
      ItemIds.CREEPER_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.UNCOMMON)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_CREEPER))
   );
   public static final Item SKULL_BANNER_PATTERN = registerItem(
      ItemIds.SKULL_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.RARE)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_SKULL))
   );
   public static final Item MOJANG_BANNER_PATTERN = registerItem(
      ItemIds.MOJANG_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.RARE)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_MOJANG))
   );
   public static final Item GLOBE_BANNER_PATTERN = registerItem(
      ItemIds.GLOBE_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_GLOBE))
   );
   public static final Item PIGLIN_BANNER_PATTERN = registerItem(
      ItemIds.PIGLIN_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.UNCOMMON)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_PIGLIN))
   );
   public static final Item FLOW_BANNER_PATTERN = registerItem(
      ItemIds.FLOW_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.RARE)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_FLOW))
   );
   public static final Item GUSTER_BANNER_PATTERN = registerItem(
      ItemIds.GUSTER_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .rarity(Rarity.RARE)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_GUSTER))
   );
   public static final Item FIELD_MASONED_BANNER_PATTERN = registerItem(
      ItemIds.FIELD_MASONED_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_FIELD_MASONED))
   );
   public static final Item BORDURE_INDENTED_BANNER_PATTERN = registerItem(
      ItemIds.BORDURE_INDENTED_BANNER_PATTERN,
      new Item.Properties()
         .stacksTo(1)
         .delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(BannerPatternTags.PATTERN_ITEM_BORDURE_INDENTED))
   );
   public static final Item GOAT_HORN = registerItem(
      ItemIds.GOAT_HORN,
      InstrumentItem::new,
      new Item.Properties()
         .rarity(Rarity.UNCOMMON)
         .stacksTo(1)
         .delayedComponent(DataComponents.INSTRUMENT, context -> new InstrumentComponent(context.getOrThrow(Instruments.PONDER_GOAT_HORN)))
   );
   public static final Item COMPOSTER = registerBlock(BlockItemIds.COMPOSTER, Blocks.COMPOSTER);
   public static final Item BARREL = registerBlock(BlockItemIds.BARREL, Blocks.BARREL, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
   public static final Item SMOKER = registerBlock(BlockItemIds.SMOKER, Blocks.SMOKER, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
   public static final Item BLAST_FURNACE = registerBlock(
      BlockItemIds.BLAST_FURNACE, Blocks.BLAST_FURNACE, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item CARTOGRAPHY_TABLE = registerBlock(BlockItemIds.CARTOGRAPHY_TABLE, Blocks.CARTOGRAPHY_TABLE);
   public static final Item FLETCHING_TABLE = registerBlock(BlockItemIds.FLETCHING_TABLE, Blocks.FLETCHING_TABLE);
   public static final Item GRINDSTONE = registerBlock(BlockItemIds.GRINDSTONE, Blocks.GRINDSTONE);
   public static final Item SMITHING_TABLE = registerBlock(BlockItemIds.SMITHING_TABLE, Blocks.SMITHING_TABLE);
   public static final Item STONECUTTER = registerBlock(BlockItemIds.STONECUTTER, Blocks.STONECUTTER);
   public static final Item BELL = registerBlock(BlockItemIds.BELL, Blocks.BELL);
   public static final Item LANTERN = registerBlock(BlockItemIds.LANTERN, Blocks.LANTERN);
   public static final Item SOUL_LANTERN = registerBlock(BlockItemIds.SOUL_LANTERN, Blocks.SOUL_LANTERN);
   public static final WeatheringCopperCollection<Item> COPPER_LANTERN = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_LANTERN, Blocks.COPPER_LANTERN, Items::registerBlock
   );
   public static final Item SWEET_BERRIES = registerItem(
      BlockItemIds.SWEET_BERRY_CROP,
      createBlockItemWithCustomItemName(Blocks.SWEET_BERRY_BUSH),
      new Item.Properties().food(Foods.SWEET_BERRIES).compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item GLOW_BERRIES = registerItem(
      BlockItemIds.GLOW_BERRY_CROP,
      createBlockItemWithCustomItemName(Blocks.CAVE_VINES),
      new Item.Properties().food(Foods.GLOW_BERRIES).compostable(NumberProviders.COMPOSTABLE_LOW)
   );
   public static final Item CAMPFIRE = registerBlock(
      BlockItemIds.CAMPFIRE, Blocks.CAMPFIRE, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item SOUL_CAMPFIRE = registerBlock(
      BlockItemIds.SOUL_CAMPFIRE, Blocks.SOUL_CAMPFIRE, p -> p.component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
   );
   public static final Item SHROOMLIGHT = registerBlock(
      BlockItemIds.SHROOMLIGHT, Blocks.SHROOMLIGHT, new Item.Properties().compostable(NumberProviders.COMPOSTABLE_MEDIUM)
   );
   public static final Item HONEYCOMB = registerItem(ItemIds.HONEYCOMB, HoneycombItem::new);
   public static final Item BEE_NEST = registerBlock(
      BlockItemIds.BEE_NEST,
      Blocks.BEE_NEST,
      new Item.Properties()
         .component(DataComponents.BEES, Bees.EMPTY)
         .component(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(BeehiveBlock.HONEY_LEVEL, 0))
   );
   public static final Item BEEHIVE = registerBlock(
      BlockItemIds.BEEHIVE,
      Blocks.BEEHIVE,
      new Item.Properties()
         .component(DataComponents.BEES, Bees.EMPTY)
         .component(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(BeehiveBlock.HONEY_LEVEL, 0))
   );
   public static final Item HONEY_BOTTLE = registerItem(
      ItemIds.HONEY_BOTTLE,
      new Item.Properties().craftRemainder(GLASS_BOTTLE).food(Foods.HONEY_BOTTLE, Consumables.HONEY_BOTTLE).usingConvertsTo(GLASS_BOTTLE).stacksTo(16)
   );
   public static final Item HONEYCOMB_BLOCK = registerBlock(BlockItemIds.HONEYCOMB_BLOCK, Blocks.HONEYCOMB_BLOCK);
   public static final Item LODESTONE = registerBlock(BlockItemIds.LODESTONE, Blocks.LODESTONE);
   public static final Item CRYING_OBSIDIAN = registerBlock(BlockItemIds.CRYING_OBSIDIAN, Blocks.CRYING_OBSIDIAN);
   public static final Item BLACKSTONE = registerBlock(BlockItemIds.BLACKSTONE, Blocks.BLACKSTONE);
   public static final Item BLACKSTONE_SLAB = registerBlock(BlockItemIds.BLACKSTONE_SLAB, Blocks.BLACKSTONE_SLAB);
   public static final Item BLACKSTONE_STAIRS = registerBlock(BlockItemIds.BLACKSTONE_STAIRS, Blocks.BLACKSTONE_STAIRS);
   public static final Item GILDED_BLACKSTONE = registerBlock(BlockItemIds.GILDED_BLACKSTONE, Blocks.GILDED_BLACKSTONE);
   public static final Item POLISHED_BLACKSTONE = registerBlock(BlockItemIds.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
   public static final Item POLISHED_BLACKSTONE_SLAB = registerBlock(BlockItemIds.POLISHED_BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE_SLAB);
   public static final Item POLISHED_BLACKSTONE_STAIRS = registerBlock(BlockItemIds.POLISHED_BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE_STAIRS);
   public static final Item CHISELED_POLISHED_BLACKSTONE = registerBlock(BlockItemIds.CHISELED_POLISHED_BLACKSTONE, Blocks.CHISELED_POLISHED_BLACKSTONE);
   public static final Item POLISHED_BLACKSTONE_BRICKS = registerBlock(BlockItemIds.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS);
   public static final Item POLISHED_BLACKSTONE_BRICK_SLAB = registerBlock(BlockItemIds.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB);
   public static final Item POLISHED_BLACKSTONE_BRICK_STAIRS = registerBlock(
      BlockItemIds.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS
   );
   public static final Item CRACKED_POLISHED_BLACKSTONE_BRICKS = registerBlock(
      BlockItemIds.CRACKED_POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS
   );
   public static final Item RESPAWN_ANCHOR = registerBlock(BlockItemIds.RESPAWN_ANCHOR, Blocks.RESPAWN_ANCHOR);
   public static final Item CANDLE = registerBlock(BlockItemIds.CANDLE, Blocks.CANDLE);
   public static final ColorCollection<Item> DYED_CANDLE = ColorCollection.registerBlockItems(
      BlockItemIds.DYED_CANDLE, Blocks.DYED_CANDLE, (id, block, var2) -> registerBlock(id, block)
   );
   public static final Item SMALL_AMETHYST_BUD = registerBlock(BlockItemIds.SMALL_AMETHYST_BUD, Blocks.SMALL_AMETHYST_BUD);
   public static final Item MEDIUM_AMETHYST_BUD = registerBlock(BlockItemIds.MEDIUM_AMETHYST_BUD, Blocks.MEDIUM_AMETHYST_BUD);
   public static final Item LARGE_AMETHYST_BUD = registerBlock(BlockItemIds.LARGE_AMETHYST_BUD, Blocks.LARGE_AMETHYST_BUD);
   public static final Item AMETHYST_CLUSTER = registerBlock(BlockItemIds.AMETHYST_CLUSTER, Blocks.AMETHYST_CLUSTER);
   public static final Item POINTED_DRIPSTONE = registerBlock(BlockItemIds.POINTED_DRIPSTONE, Blocks.POINTED_DRIPSTONE);
   public static final Item SULFUR_SPIKE = registerBlock(BlockItemIds.SULFUR_SPIKE, Blocks.SULFUR_SPIKE);
   public static final Item OCHRE_FROGLIGHT = registerBlock(BlockItemIds.OCHRE_FROGLIGHT, Blocks.OCHRE_FROGLIGHT);
   public static final Item VERDANT_FROGLIGHT = registerBlock(BlockItemIds.VERDANT_FROGLIGHT, Blocks.VERDANT_FROGLIGHT);
   public static final Item PEARLESCENT_FROGLIGHT = registerBlock(BlockItemIds.PEARLESCENT_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT);
   public static final Item FROGSPAWN = registerBlock(BlockItemIds.FROGSPAWN, Blocks.FROGSPAWN, PlaceOnWaterBlockItem::new);
   public static final Item ECHO_SHARD = registerItem(ItemIds.ECHO_SHARD, new Item.Properties().rarity(Rarity.UNCOMMON));
   public static final Item BRUSH = registerItem(ItemIds.BRUSH, BrushItem::new, new Item.Properties().durability(64));
   public static final Item NETHERITE_UPGRADE_SMITHING_TEMPLATE = registerItem(
      ItemIds.NETHERITE_UPGRADE_SMITHING_TEMPLATE, SmithingTemplateItem::createNetheriteUpgradeTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item DUNE_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item COAST_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item WILD_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item WARD_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.RARE)
   );
   public static final Item EYE_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.RARE)
   );
   public static final Item VEX_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.RARE)
   );
   public static final Item TIDE_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item RIB_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.RARE)
   );
   public static final Item WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.EPIC)
   );
   public static final Item RAISER_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item HOST_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item FLOW_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item BOLT_ARMOR_TRIM_SMITHING_TEMPLATE = registerItem(
      ItemIds.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(Rarity.UNCOMMON)
   );
   public static final Item ANGLER_POTTERY_SHERD = registerItem(
      ItemIds.ANGLER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.ANGLER)
   );
   public static final Item ARCHER_POTTERY_SHERD = registerItem(
      ItemIds.ARCHER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.ARCHER)
   );
   public static final Item ARMS_UP_POTTERY_SHERD = registerItem(
      ItemIds.ARMS_UP_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.ARMS_UP)
   );
   public static final Item BLADE_POTTERY_SHERD = registerItem(
      ItemIds.BLADE_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.BLADE)
   );
   public static final Item BREWER_POTTERY_SHERD = registerItem(
      ItemIds.BREWER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.BREWER)
   );
   public static final Item BURN_POTTERY_SHERD = registerItem(
      ItemIds.BURN_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.BURN)
   );
   public static final Item DANGER_POTTERY_SHERD = registerItem(
      ItemIds.DANGER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.DANGER)
   );
   public static final Item EXPLORER_POTTERY_SHERD = registerItem(
      ItemIds.EXPLORER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.EXPLORER)
   );
   public static final Item FLOW_POTTERY_SHERD = registerItem(
      ItemIds.FLOW_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.FLOW)
   );
   public static final Item FRIEND_POTTERY_SHERD = registerItem(
      ItemIds.FRIEND_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.FRIEND)
   );
   public static final Item GUSTER_POTTERY_SHERD = registerItem(
      ItemIds.GUSTER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.GUSTER)
   );
   public static final Item HEART_POTTERY_SHERD = registerItem(
      ItemIds.HEART_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.HEART)
   );
   public static final Item HEARTBREAK_POTTERY_SHERD = registerItem(
      ItemIds.HEARTBREAK_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.HEARTBREAK)
   );
   public static final Item HOWL_POTTERY_SHERD = registerItem(
      ItemIds.HOWL_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.HOWL)
   );
   public static final Item MINER_POTTERY_SHERD = registerItem(
      ItemIds.MINER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.MINER)
   );
   public static final Item MOURNER_POTTERY_SHERD = registerItem(
      ItemIds.MOURNER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.MOURNER)
   );
   public static final Item PLENTY_POTTERY_SHERD = registerItem(
      ItemIds.PLENTY_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.PLENTY)
   );
   public static final Item PRIZE_POTTERY_SHERD = registerItem(
      ItemIds.PRIZE_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.PRIZE)
   );
   public static final Item SCRAPE_POTTERY_SHERD = registerItem(
      ItemIds.SCRAPE_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.SCRAPE)
   );
   public static final Item SHEAF_POTTERY_SHERD = registerItem(
      ItemIds.SHEAF_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.SHEAF)
   );
   public static final Item SHELTER_POTTERY_SHERD = registerItem(
      ItemIds.SHELTER_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.SHELTER)
   );
   public static final Item SKULL_POTTERY_SHERD = registerItem(
      ItemIds.SKULL_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.SKULL)
   );
   public static final Item SNORT_POTTERY_SHERD = registerItem(
      ItemIds.SNORT_POTTERY_SHERD, new Item.Properties().rarity(Rarity.UNCOMMON).potPattern(DecoratedPotPatterns.SNORT)
   );
   public static final WeatheringCopperCollection<Item> COPPER_GRATE = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_GRATE, Blocks.COPPER_GRATE, Items::registerBlock
   );
   public static final WeatheringCopperCollection<Item> COPPER_BULB = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_BULB, Blocks.COPPER_BULB, Items::registerBlock
   );
   public static final WeatheringCopperCollection<Item> COPPER_CHEST = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_CHEST, Blocks.COPPER_CHEST, Items::registerBlock
   );
   public static final WeatheringCopperCollection<Item> COPPER_GOLEM_STATUE = WeatheringCopperCollection.registerItems(
      BlockItemIds.COPPER_GOLEM_STATUE,
      Blocks.COPPER_GOLEM_STATUE,
      (id, block) -> registerBlock(
         id,
         block,
         new Item.Properties()
            .component(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.STANDING))
      )
   );
   public static final Item TRIAL_SPAWNER = registerBlock(BlockItemIds.TRIAL_SPAWNER, Blocks.TRIAL_SPAWNER);
   public static final Item TRIAL_KEY = registerItem(ItemIds.TRIAL_KEY);
   public static final Item OMINOUS_TRIAL_KEY = registerItem(ItemIds.OMINOUS_TRIAL_KEY);
   public static final Item VAULT = registerBlock(BlockItemIds.VAULT, Blocks.VAULT);
   public static final Item OMINOUS_BOTTLE = registerItem(
      ItemIds.OMINOUS_BOTTLE,
      new Item.Properties()
         .rarity(Rarity.UNCOMMON)
         .component(DataComponents.CONSUMABLE, Consumables.OMINOUS_BOTTLE)
         .component(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(0))
   );

   private static Function<Item.Properties, Item> createBlockItemWithCustomItemName(final Block block) {
      return p -> new BlockItem(block, p.useItemDescriptionPrefix());
   }

   private static Item registerSpawnEgg(final ResourceKey<Item> id, final EntityType<?> type) {
      return registerItem(id, SpawnEggItem::new, new Item.Properties().spawnEgg(type));
   }

   private static Item registerBlock(final BlockItemId id, final Block block) {
      return registerBlock(id, block, BlockItem::new);
   }

   private static Item registerBlock(final BlockItemId id, final Block block, final Item.Properties properties) {
      return registerBlock(id, block, BlockItem::new, properties);
   }

   private static Item registerBlock(final BlockItemId id, final Block block, final UnaryOperator<Item.Properties> propertiesFunction) {
      return registerBlock(id, block, (b, p) -> new BlockItem(b, propertiesFunction.apply(p)));
   }

   private static Item registerBlock(final BlockItemId id, final Block block, final Block... alternatives) {
      Item item = registerBlock(id, block);

      for (Block alternative : alternatives) {
         Item.BY_BLOCK.put(alternative, item);
      }

      return item;
   }

   private static Item registerBlock(final BlockItemId id, final Block block, final Item.Properties properties, final Block... alternatives) {
      Item item = registerBlock(id, block, properties);

      for (Block alternative : alternatives) {
         Item.BY_BLOCK.put(alternative, item);
      }

      return item;
   }

   private static Item registerBlock(final BlockItemId id, final Block block, final BiFunction<Block, Item.Properties, Item> itemFactory) {
      return registerBlock(id, block, itemFactory, new Item.Properties());
   }

   private static Item registerBlock(
      final BlockItemId id, final Block block, final BiFunction<Block, Item.Properties, Item> itemFactory, final Item.Properties properties
   ) {
      return registerItem(id.item(), p -> itemFactory.apply(block, p), properties.useBlockDescriptionPrefix().requiredFeatures(block.requiredFeatures()));
   }

   private static Item registerItem(final ResourceKey<Item> id, final Item.Properties properties) {
      return registerItem(id, Item::new, properties);
   }

   private static Item registerItem(final ResourceKey<Item> id) {
      return registerItem(id, Item::new, new Item.Properties());
   }

   private static Item registerItem(final BlockItemId id, final Function<Item.Properties, Item> itemFactory) {
      return registerItem(id.item(), itemFactory);
   }

   private static Item registerItem(final ResourceKey<Item> id, final Function<Item.Properties, Item> itemFactory) {
      return registerItem(id, itemFactory, new Item.Properties());
   }

   private static Item registerItem(final BlockItemId id, final Function<Item.Properties, Item> itemFactory, final Item.Properties properties) {
      return registerItem(id.item(), itemFactory, properties);
   }

   private static Item registerItem(final ResourceKey<Item> id, final Function<Item.Properties, Item> itemFactory, final Item.Properties properties) {
      Item item = itemFactory.apply(properties.setId(id));
      if (item instanceof BlockItem blockItem) {
         blockItem.registerBlocks(Item.BY_BLOCK, item);
      }

      return Registry.register(BuiltInRegistries.ITEM, id, item);
   }
}
