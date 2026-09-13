package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.AxisAlignedLinearPosTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.PosAlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.AppendLoot;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class ProcessorLists {
   private static final ResourceKey<StructureProcessorList> EMPTY = createKey("empty");
   public static final ResourceKey<StructureProcessorList> ZOMBIE_PLAINS = createKey("zombie_plains");
   public static final ResourceKey<StructureProcessorList> ZOMBIE_SAVANNA = createKey("zombie_savanna");
   public static final ResourceKey<StructureProcessorList> ZOMBIE_SNOWY = createKey("zombie_snowy");
   public static final ResourceKey<StructureProcessorList> ZOMBIE_TAIGA = createKey("zombie_taiga");
   public static final ResourceKey<StructureProcessorList> ZOMBIE_DESERT = createKey("zombie_desert");
   public static final ResourceKey<StructureProcessorList> MOSSIFY_10_PERCENT = createKey("mossify_10_percent");
   public static final ResourceKey<StructureProcessorList> MOSSIFY_20_PERCENT = createKey("mossify_20_percent");
   public static final ResourceKey<StructureProcessorList> MOSSIFY_70_PERCENT = createKey("mossify_70_percent");
   public static final ResourceKey<StructureProcessorList> STREET_PLAINS = createKey("street_plains");
   public static final ResourceKey<StructureProcessorList> STREET_SAVANNA = createKey("street_savanna");
   public static final ResourceKey<StructureProcessorList> STREET_SNOWY_OR_TAIGA = createKey("street_snowy_or_taiga");
   public static final ResourceKey<StructureProcessorList> FARM_PLAINS = createKey("farm_plains");
   public static final ResourceKey<StructureProcessorList> FARM_SAVANNA = createKey("farm_savanna");
   public static final ResourceKey<StructureProcessorList> FARM_SNOWY = createKey("farm_snowy");
   public static final ResourceKey<StructureProcessorList> FARM_TAIGA = createKey("farm_taiga");
   public static final ResourceKey<StructureProcessorList> FARM_DESERT = createKey("farm_desert");
   public static final ResourceKey<StructureProcessorList> OUTPOST_ROT = createKey("outpost_rot");
   public static final ResourceKey<StructureProcessorList> BOTTOM_RAMPART = createKey("bottom_rampart");
   public static final ResourceKey<StructureProcessorList> TREASURE_ROOMS = createKey("treasure_rooms");
   public static final ResourceKey<StructureProcessorList> HOUSING = createKey("housing");
   public static final ResourceKey<StructureProcessorList> SIDE_WALL_DEGRADATION = createKey("side_wall_degradation");
   public static final ResourceKey<StructureProcessorList> STABLE_DEGRADATION = createKey("stable_degradation");
   public static final ResourceKey<StructureProcessorList> BASTION_GENERIC_DEGRADATION = createKey("bastion_generic_degradation");
   public static final ResourceKey<StructureProcessorList> RAMPART_DEGRADATION = createKey("rampart_degradation");
   public static final ResourceKey<StructureProcessorList> ENTRANCE_REPLACEMENT = createKey("entrance_replacement");
   public static final ResourceKey<StructureProcessorList> BRIDGE = createKey("bridge");
   public static final ResourceKey<StructureProcessorList> ROOF = createKey("roof");
   public static final ResourceKey<StructureProcessorList> HIGH_WALL = createKey("high_wall");
   public static final ResourceKey<StructureProcessorList> HIGH_RAMPART = createKey("high_rampart");
   public static final ResourceKey<StructureProcessorList> FOSSIL_ROT = createKey("fossil_rot");
   public static final ResourceKey<StructureProcessorList> FOSSIL_COAL = createKey("fossil_coal");
   public static final ResourceKey<StructureProcessorList> FOSSIL_DIAMONDS = createKey("fossil_diamonds");
   public static final ResourceKey<StructureProcessorList> ANCIENT_CITY_START_DEGRADATION = createKey("ancient_city_start_degradation");
   public static final ResourceKey<StructureProcessorList> ANCIENT_CITY_GENERIC_DEGRADATION = createKey("ancient_city_generic_degradation");
   public static final ResourceKey<StructureProcessorList> ANCIENT_CITY_WALLS_DEGRADATION = createKey("ancient_city_walls_degradation");
   public static final ResourceKey<StructureProcessorList> TRAIL_RUINS_HOUSES_ARCHAEOLOGY = createKey("trail_ruins_houses_archaeology");
   public static final ResourceKey<StructureProcessorList> TRAIL_RUINS_ROADS_ARCHAEOLOGY = createKey("trail_ruins_roads_archaeology");
   public static final ResourceKey<StructureProcessorList> TRAIL_RUINS_TOWER_TOP_ARCHAEOLOGY = createKey("trail_ruins_tower_top_archaeology");
   public static final ResourceKey<StructureProcessorList> TRIAL_CHAMBERS_COPPER_BULB_DEGRADATION = createKey("trial_chambers_copper_bulb_degradation");

   private static ResourceKey<StructureProcessorList> createKey(String p_256151_) {
      return ResourceKey.create(Registries.PROCESSOR_LIST, Identifier.withDefaultNamespace(p_256151_));
   }

   private static void register(
      BootstrapContext<StructureProcessorList> p_332799_, ResourceKey<StructureProcessorList> p_256192_, List<StructureProcessor> p_255805_
   ) {
      p_332799_.register(p_256192_, new StructureProcessorList(p_255805_));
   }

   public static void bootstrap(BootstrapContext<StructureProcessorList> p_333601_) {
      HolderGetter<Block> holdergetter = p_333601_.lookup(Registries.BLOCK);
      ProcessorRule processorrule = new ProcessorRule(
         new RandomBlockMatchTest(Blocks.BLACKSTONE, 0.01F), AlwaysTrueTest.INSTANCE, Blocks.GILDED_BLACKSTONE.defaultBlockState()
      );
      ProcessorRule processorrule1 = new ProcessorRule(
         new RandomBlockMatchTest(Blocks.GILDED_BLACKSTONE, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.BLACKSTONE.defaultBlockState()
      );
      register(p_333601_, EMPTY, ImmutableList.of());
      register(
         p_333601_,
         ZOMBIE_PLAINS,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.8F), AlwaysTrueTest.INSTANCE, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                  new ProcessorRule(new TagMatchTest(BlockTags.DOORS), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.WALL_TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.07F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.MOSSY_COBBLESTONE, 0.07F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHITE_TERRACOTTA, 0.07F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.OAK_LOG, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.OAK_PLANKS, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.OAK_STAIRS, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.STRIPPED_OAK_LOG, 0.02F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GLASS_PANE, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule[]{
                     new ProcessorRule(
                        new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
                     ),
                     new ProcessorRule(
                        new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)
                     ),
                     new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CARROTS.defaultBlockState()),
                     new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.POTATOES.defaultBlockState()),
                     new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.BEETROOTS.defaultBlockState())
                  }
               )
            )
         )
      );
      register(
         p_333601_,
         ZOMBIE_SAVANNA,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new TagMatchTest(BlockTags.DOORS), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.WALL_TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.ACACIA_PLANKS, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.ACACIA_STAIRS, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.ACACIA_LOG, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.ACACIA_WOOD, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.ORANGE_TERRACOTTA, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.YELLOW_TERRACOTTA, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.RED_TERRACOTTA, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GLASS_PANE, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(
                     new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
                  ),
                  new ProcessorRule[]{
                     new ProcessorRule(
                        new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)
                     ),
                     new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.MELON_STEM.defaultBlockState())
                  }
               )
            )
         )
      );
      register(
         p_333601_,
         ZOMBIE_SNOWY,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new TagMatchTest(BlockTags.DOORS), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.WALL_TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.LANTERN), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SPRUCE_PLANKS, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SPRUCE_SLAB, 0.4F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.STRIPPED_SPRUCE_LOG, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.STRIPPED_SPRUCE_WOOD, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GLASS_PANE, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(
                     new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
                  ),
                  new ProcessorRule(
                     new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.CARROTS.defaultBlockState()),
                  new ProcessorRule[]{
                     new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.8F), AlwaysTrueTest.INSTANCE, Blocks.POTATOES.defaultBlockState())
                  }
               )
            )
         )
      );
      register(
         p_333601_,
         ZOMBIE_TAIGA,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.8F), AlwaysTrueTest.INSTANCE, Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                  new ProcessorRule(new TagMatchTest(BlockTags.DOORS), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.WALL_TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(
                     new BlockMatchTest(Blocks.CAMPFIRE), AlwaysTrueTest.INSTANCE, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false)
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.08F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SPRUCE_LOG, 0.08F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GLASS_PANE, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(
                     new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true)
                  ),
                  new ProcessorRule(
                     new BlockStateMatchTest(Blocks.GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.BROWN_STAINED_GLASS_PANE.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true)
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.PUMPKIN_STEM.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.POTATOES.defaultBlockState()),
                  new ProcessorRule[0]
               )
            )
         )
      );
      register(
         p_333601_,
         ZOMBIE_DESERT,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new TagMatchTest(BlockTags.DOORS), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.WALL_TORCH), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SMOOTH_SANDSTONE, 0.08F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.CUT_SANDSTONE, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.TERRACOTTA, 0.08F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SMOOTH_SANDSTONE_STAIRS, 0.08F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SMOOTH_SANDSTONE_SLAB, 0.08F), AlwaysTrueTest.INSTANCE, Blocks.COBWEB.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.BEETROOTS.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.MELON_STEM.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         MOSSIFY_10_PERCENT,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.MOSSY_COBBLESTONE.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         MOSSIFY_20_PERCENT,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.MOSSY_COBBLESTONE.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         MOSSIFY_70_PERCENT,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.COBBLESTONE, 0.7F), AlwaysTrueTest.INSTANCE, Blocks.MOSSY_COBBLESTONE.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         STREET_PLAINS,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new BlockMatchTest(Blocks.DIRT_PATH), new BlockMatchTest(Blocks.WATER), Blocks.OAK_PLANKS.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.DIRT_PATH, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.GRASS_BLOCK.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.GRASS_BLOCK), new BlockMatchTest(Blocks.WATER), Blocks.WATER.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.DIRT), new BlockMatchTest(Blocks.WATER), Blocks.WATER.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         STREET_SAVANNA,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new BlockMatchTest(Blocks.DIRT_PATH), new BlockMatchTest(Blocks.WATER), Blocks.ACACIA_PLANKS.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.DIRT_PATH, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.GRASS_BLOCK.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.GRASS_BLOCK), new BlockMatchTest(Blocks.WATER), Blocks.WATER.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.DIRT), new BlockMatchTest(Blocks.WATER), Blocks.WATER.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         STREET_SNOWY_OR_TAIGA,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new BlockMatchTest(Blocks.DIRT_PATH), new BlockMatchTest(Blocks.WATER), Blocks.SPRUCE_PLANKS.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.DIRT_PATH), new BlockMatchTest(Blocks.ICE), Blocks.SPRUCE_PLANKS.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.DIRT_PATH, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.GRASS_BLOCK.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.GRASS_BLOCK), new BlockMatchTest(Blocks.WATER), Blocks.WATER.defaultBlockState()),
                  new ProcessorRule(new BlockMatchTest(Blocks.DIRT), new BlockMatchTest(Blocks.WATER), Blocks.WATER.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         FARM_PLAINS,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CARROTS.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.POTATOES.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.BEETROOTS.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         FARM_SAVANNA,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.MELON_STEM.defaultBlockState()))
            )
         )
      );
      register(
         p_333601_,
         FARM_SNOWY,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.CARROTS.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.8F), AlwaysTrueTest.INSTANCE, Blocks.POTATOES.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         FARM_TAIGA,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.PUMPKIN_STEM.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.POTATOES.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         FARM_DESERT,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.BEETROOTS.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.WHEAT, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.MELON_STEM.defaultBlockState())
               )
            )
         )
      );
      register(p_333601_, OUTPOST_ROT, ImmutableList.of(new BlockRotProcessor(0.05F)));
      register(
         p_333601_,
         BOTTOM_RAMPART,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.MAGMA_BLOCK, 0.75F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS, 0.15F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         TREASURE_ROOMS,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.35F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.CHISELED_POLISHED_BLACKSTONE, 0.1F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         HOUSING,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.3F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.BLACKSTONE, 1.0E-4F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         SIDE_WALL_DEGRADATION,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.CHISELED_POLISHED_BLACKSTONE, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.GOLD_BLOCK, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         STABLE_DEGRADATION,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.1F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.BLACKSTONE, 1.0E-4F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         BASTION_GENERIC_DEGRADATION,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.3F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.BLACKSTONE, 1.0E-4F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.GOLD_BLOCK, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         RAMPART_DEGRADATION,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.4F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.BLACKSTONE, 0.01F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 1.0E-4F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.BLACKSTONE, 1.0E-4F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.GOLD_BLOCK, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         ENTRANCE_REPLACEMENT,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.CHISELED_POLISHED_BLACKSTONE, 0.5F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.GOLD_BLOCK, 0.6F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  processorrule1,
                  processorrule
               )
            )
         )
      );
      register(
         p_333601_,
         BRIDGE,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.3F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.BLACKSTONE, 1.0E-4F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState())
               )
            )
         )
      );
      register(
         p_333601_,
         ROOF,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.3F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.15F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.BLACKSTONE.defaultBlockState()
                  )
               )
            )
         )
      );
      register(
         p_333601_,
         HIGH_WALL,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.01F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.5F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.BLACKSTONE.defaultBlockState()
                  ),
                  processorrule1
               )
            )
         )
      );
      register(
         p_333601_,
         HIGH_RAMPART,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.GOLD_BLOCK, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     AlwaysTrueTest.INSTANCE,
                     AlwaysTrueTest.INSTANCE,
                     new AxisAlignedLinearPosTest(0.0F, 0.05F, 0, 100, Direction.Axis.Y),
                     Blocks.AIR.defaultBlockState()
                  ),
                  processorrule1
               )
            )
         )
      );
      register(p_333601_, FOSSIL_ROT, ImmutableList.of(new BlockRotProcessor(0.9F), new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)));
      register(p_333601_, FOSSIL_COAL, ImmutableList.of(new BlockRotProcessor(0.1F), new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)));
      register(
         p_333601_,
         FOSSIL_DIAMONDS,
         ImmutableList.of(
            new BlockRotProcessor(0.1F),
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(new BlockMatchTest(Blocks.COAL_ORE), AlwaysTrueTest.INSTANCE, Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState())
               )
            ),
            new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
         )
      );
      register(
         p_333601_,
         ANCIENT_CITY_START_DEGRADATION,
         ImmutableList.of(
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.DEEPSLATE_BRICKS, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.DEEPSLATE_TILES, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SOUL_LANTERN, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState())
               )
            ),
            new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
         )
      );
      register(
         p_333601_,
         ANCIENT_CITY_GENERIC_DEGRADATION,
         ImmutableList.of(
            new BlockRotProcessor(holdergetter.getOrThrow(BlockTags.ANCIENT_CITY_REPLACEABLE), 0.95F),
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.DEEPSLATE_BRICKS, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.DEEPSLATE_TILES, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SOUL_LANTERN, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState())
               )
            ),
            new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
         )
      );
      register(
         p_333601_,
         ANCIENT_CITY_WALLS_DEGRADATION,
         ImmutableList.of(
            new BlockRotProcessor(holdergetter.getOrThrow(BlockTags.ANCIENT_CITY_REPLACEABLE), 0.95F),
            new RuleProcessor(
               ImmutableList.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.DEEPSLATE_BRICKS, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.DEEPSLATE_TILES, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                  ),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.DEEPSLATE_TILE_SLAB, 0.3F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.SOUL_LANTERN, 0.05F), AlwaysTrueTest.INSTANCE, Blocks.AIR.defaultBlockState())
               )
            ),
            new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
         )
      );
      register(
         p_333601_,
         TRAIL_RUINS_HOUSES_ARCHAEOLOGY,
         List.of(
            new RuleProcessor(
               List.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GRAVEL, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.DIRT.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GRAVEL, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.COARSE_DIRT.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.MUD_BRICKS, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.PACKED_MUD.defaultBlockState())
               )
            ),
            trailsArchyLootProcessor(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, 6),
            trailsArchyLootProcessor(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE, 3)
         )
      );
      register(
         p_333601_,
         TRAIL_RUINS_ROADS_ARCHAEOLOGY,
         List.of(
            new RuleProcessor(
               List.of(
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GRAVEL, 0.2F), AlwaysTrueTest.INSTANCE, Blocks.DIRT.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.GRAVEL, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.COARSE_DIRT.defaultBlockState()),
                  new ProcessorRule(new RandomBlockMatchTest(Blocks.MUD_BRICKS, 0.1F), AlwaysTrueTest.INSTANCE, Blocks.PACKED_MUD.defaultBlockState())
               )
            ),
            trailsArchyLootProcessor(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, 2)
         )
      );
      register(p_333601_, TRAIL_RUINS_TOWER_TOP_ARCHAEOLOGY, List.of(trailsArchyLootProcessor(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON, 2)));
      register(
         p_333601_,
         TRIAL_CHAMBERS_COPPER_BULB_DEGRADATION,
         List.of(
            new RuleProcessor(
               List.of(
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.WAXED_COPPER_BULB, 0.1F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.WAXED_OXIDIZED_COPPER_BULB.defaultBlockState().setValue(CopperBulbBlock.LIT, true)
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.WAXED_COPPER_BULB, 0.33333334F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.WAXED_WEATHERED_COPPER_BULB.defaultBlockState().setValue(CopperBulbBlock.LIT, true)
                  ),
                  new ProcessorRule(
                     new RandomBlockMatchTest(Blocks.WAXED_COPPER_BULB, 0.5F),
                     AlwaysTrueTest.INSTANCE,
                     Blocks.WAXED_EXPOSED_COPPER_BULB.defaultBlockState().setValue(CopperBulbBlock.LIT, true)
                  )
               )
            ),
            new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
         )
      );
   }

   private static CappedProcessor trailsArchyLootProcessor(ResourceKey<LootTable> p_332708_, int p_281868_) {
      return new CappedProcessor(
         new RuleProcessor(
            List.of(
               new ProcessorRule(
                  new TagMatchTest(BlockTags.TRAIL_RUINS_REPLACEABLE),
                  AlwaysTrueTest.INSTANCE,
                  PosAlwaysTrueTest.INSTANCE,
                  Blocks.SUSPICIOUS_GRAVEL.defaultBlockState(),
                  new AppendLoot(p_332708_)
               )
            )
         ),
         ConstantInt.of(p_281868_)
      );
   }
}
