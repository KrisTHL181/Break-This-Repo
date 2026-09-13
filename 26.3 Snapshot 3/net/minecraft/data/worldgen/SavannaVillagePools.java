package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VillagePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class SavannaVillagePools {
   public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("village/savanna/town_centers");
   private static final ResourceKey<StructureTemplatePool> TERMINATORS_KEY = Pools.createKey("village/savanna/terminators");
   private static final ResourceKey<StructureTemplatePool> ZOMBIE_TERMINATORS_KEY = Pools.createKey("village/savanna/zombie/terminators");

   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
      Holder<PlacedFeature> acaciaVillage = placedFeatures.getOrThrow(VillagePlacements.ACACIA_VILLAGE);
      Holder<PlacedFeature> pileHayVillage = placedFeatures.getOrThrow(VillagePlacements.PILE_HAY_VILLAGE);
      Holder<PlacedFeature> pileMelonVillage = placedFeatures.getOrThrow(VillagePlacements.PILE_MELON_VILLAGE);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> zombieSavanna = processorLists.getOrThrow(ProcessorLists.ZOMBIE_SAVANNA);
      Holder<StructureProcessorList> streetSavanna = processorLists.getOrThrow(ProcessorLists.STREET_SAVANNA);
      Holder<StructureProcessorList> farmSavanna = processorLists.getOrThrow(ProcessorLists.FARM_SAVANNA);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Holder<StructureTemplatePool> terminators = pools.getOrThrow(TERMINATORS_KEY);
      Holder<StructureTemplatePool> zombieTerminators = pools.getOrThrow(ZOMBIE_TERMINATORS_KEY);
      context.register(
         START,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_1"), 100),
               Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_2"), 50),
               Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_3"), 150),
               Pair.of(StructurePoolElement.legacy("village/savanna/town_centers/savanna_meeting_point_4"), 150),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_1", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_2", zombieSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_3", zombieSavanna), 3),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/town_centers/savanna_meeting_point_4", zombieSavanna), 3)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/savanna/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/corner_01", streetSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/corner_03", streetSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_02", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_04", streetSavanna), 7),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_05", streetSavanna), 3),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_06", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_08", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_09", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_10", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/straight_11", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/crossroad_02", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/streets/crossroad_03", streetSavanna), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/savanna/streets/crossroad_04", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/streets/crossroad_05", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/streets/crossroad_06", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/streets/crossroad_07", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/streets/split_01", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/streets/split_02", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/streets/turn_01", streetSavanna), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/savanna/zombie/streets",
         new StructureTemplatePool(
            zombieTerminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/corner_01", streetSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/corner_03", streetSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_02", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_04", streetSavanna), 7),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_05", streetSavanna), 3),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_06", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_08", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_09", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_10", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/straight_11", streetSavanna), 4),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/crossroad_02", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/crossroad_03", streetSavanna), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/crossroad_04", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/crossroad_05", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/crossroad_06", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/crossroad_07", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/split_01", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/split_02", streetSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/streets/turn_01", streetSavanna), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/savanna/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_2"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_3"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_4"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_5"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_6"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_7"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_house_8"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_medium_house_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_medium_house_2"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_butchers_shop_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_butchers_shop_2"), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_tool_smith_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_fletcher_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_shepherd_1"), 7),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_armorer_1"), 1),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_fisher_cottage_1"), 3),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_tannery_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_cartographer_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_library_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_mason_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_weaponsmith_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_weaponsmith_2"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_temple_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_temple_2"), 3),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_large_farm_1", farmSavanna), 4),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_large_farm_2", farmSavanna), 6),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_farm", farmSavanna), 4),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_animal_pen_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_animal_pen_2"), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_animal_pen_3"), 2),
                  Pair.of(StructurePoolElement.empty(), 5)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/savanna/zombie/houses",
         new StructureTemplatePool(
            zombieTerminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_1", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_2", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_3", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_4", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_5", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_6", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_7", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_small_house_8", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_medium_house_1", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_medium_house_2", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_butchers_shop_1", zombieSavanna), 2),
               Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_butchers_shop_2", zombieSavanna), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_tool_smith_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_fletcher_house_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_shepherd_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_armorer_1", zombieSavanna), 1),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_fisher_cottage_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_tannery_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_cartographer_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_library_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_mason_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_weaponsmith_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_weaponsmith_2", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_temple_1", zombieSavanna), 1),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_temple_2", zombieSavanna), 3),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_large_farm_1", zombieSavanna), 4),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_large_farm_2", zombieSavanna), 4),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_small_farm", zombieSavanna), 4),
                  Pair.of(StructurePoolElement.legacy("village/savanna/houses/savanna_animal_pen_1", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_animal_pen_2", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.legacy("village/savanna/zombie/houses/savanna_animal_pen_3", zombieSavanna), 2),
                  Pair.of(StructurePoolElement.empty(), 5)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      context.register(
         TERMINATORS_KEY,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_01", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_02", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_03", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_04", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/terminators/terminator_05", streetSavanna), 1)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      context.register(
         ZOMBIE_TERMINATORS_KEY,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_01", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_02", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_03", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_04", streetSavanna), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/terminators/terminator_05", streetSavanna), 1)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/savanna/trees",
         new StructureTemplatePool(empty, ImmutableList.of(Pair.of(StructurePoolElement.feature(acaciaVillage), 1)), StructureTemplatePool.Projection.RIGID)
      );
      Pools.register(
         context,
         "village/savanna/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/savanna_lamp_post_01"), 4),
               Pair.of(StructurePoolElement.feature(acaciaVillage), 4),
               Pair.of(StructurePoolElement.feature(pileHayVillage), 4),
               Pair.of(StructurePoolElement.feature(pileMelonVillage), 1),
               Pair.of(StructurePoolElement.empty(), 4)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/savanna/zombie/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/savanna_lamp_post_01", zombieSavanna), 4),
               Pair.of(StructurePoolElement.feature(acaciaVillage), 4),
               Pair.of(StructurePoolElement.feature(pileHayVillage), 4),
               Pair.of(StructurePoolElement.feature(pileMelonVillage), 1),
               Pair.of(StructurePoolElement.empty(), 4)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/savanna/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/villagers/baby"), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/savanna/zombie/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/savanna/zombie/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
