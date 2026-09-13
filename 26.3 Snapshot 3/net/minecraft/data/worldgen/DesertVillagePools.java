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

public class DesertVillagePools {
   public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("village/desert/town_centers");
   private static final ResourceKey<StructureTemplatePool> TERMINATORS_KEY = Pools.createKey("village/desert/terminators");
   private static final ResourceKey<StructureTemplatePool> ZOMBIE_TERMINATORS_KEY = Pools.createKey("village/desert/zombie/terminators");

   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
      Holder<PlacedFeature> patchCactusVillage = placedFeatures.getOrThrow(VillagePlacements.PATCH_CACTUS_VILLAGE);
      Holder<PlacedFeature> pileHayVillage = placedFeatures.getOrThrow(VillagePlacements.PILE_HAY_VILLAGE);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> zombieDesert = processorLists.getOrThrow(ProcessorLists.ZOMBIE_DESERT);
      Holder<StructureProcessorList> farmDesert = processorLists.getOrThrow(ProcessorLists.FARM_DESERT);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Holder<StructureTemplatePool> terminators = pools.getOrThrow(TERMINATORS_KEY);
      Holder<StructureTemplatePool> zombieTerminators = pools.getOrThrow(ZOMBIE_TERMINATORS_KEY);
      context.register(
         START,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_1"), 98),
               Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_2"), 98),
               Pair.of(StructurePoolElement.legacy("village/desert/town_centers/desert_meeting_point_3"), 49),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_1", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_2", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/town_centers/desert_meeting_point_3", zombieDesert), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/desert/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/streets/corner_01"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/corner_02"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/straight_01"), 4),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/straight_02"), 4),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/straight_03"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/crossroad_01"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/crossroad_02"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/crossroad_03"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/square_01"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/square_02"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/streets/turn_01"), 3)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/desert/zombie/streets",
         new StructureTemplatePool(
            zombieTerminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/corner_01"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/corner_02"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/straight_01"), 4),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/straight_02"), 4),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/straight_03"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/crossroad_01"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/crossroad_02"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/crossroad_03"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/square_01"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/square_02"), 3),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/streets/turn_01"), 3)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/desert/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_2"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_3"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_4"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_5"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_6"), 1),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_7"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_small_house_8"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_medium_house_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_medium_house_2"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_butcher_shop_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_tool_smith_1"), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_fletcher_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_shepherd_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_armorer_1"), 1),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_fisher_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_tannery_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_cartographer_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_library_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_mason_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_weaponsmith_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_temple_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_temple_2"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_large_farm_1", farmDesert), 11),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_farm_1", farmDesert), 4),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_farm_2", farmDesert), 4),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_animal_pen_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_animal_pen_2"), 2),
                  Pair.of(StructurePoolElement.empty(), 5)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/desert/zombie/houses",
         new StructureTemplatePool(
            zombieTerminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_1", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_2", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_3", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_4", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_5", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_6", zombieDesert), 1),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_7", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_small_house_8", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_medium_house_1", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/houses/desert_medium_house_2", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_butcher_shop_1", zombieDesert), 2),
               Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_tool_smith_1", zombieDesert), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_fletcher_house_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_shepherd_house_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_armorer_1", zombieDesert), 1),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_fisher_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_tannery_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_cartographer_house_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_library_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_mason_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_weaponsmith_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_temple_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_temple_2", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_large_farm_1", zombieDesert), 7),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_farm_1", zombieDesert), 4),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_farm_2", zombieDesert), 4),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_animal_pen_1", zombieDesert), 2),
                  Pair.of(StructurePoolElement.legacy("village/desert/houses/desert_animal_pen_2", zombieDesert), 2),
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
               Pair.of(StructurePoolElement.legacy("village/desert/terminators/terminator_01"), 1),
               Pair.of(StructurePoolElement.legacy("village/desert/terminators/terminator_02"), 1)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      context.register(
         ZOMBIE_TERMINATORS_KEY,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/terminators/terminator_01"), 1),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/terminators/terminator_02"), 1)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/desert/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/desert_lamp_1"), 10),
               Pair.of(StructurePoolElement.feature(patchCactusVillage), 4),
               Pair.of(StructurePoolElement.feature(pileHayVillage), 4),
               Pair.of(StructurePoolElement.empty(), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/desert/zombie/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/desert_lamp_1", zombieDesert), 10),
               Pair.of(StructurePoolElement.feature(patchCactusVillage), 4),
               Pair.of(StructurePoolElement.feature(pileHayVillage), 4),
               Pair.of(StructurePoolElement.empty(), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/desert/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/desert/villagers/baby"), 1),
               Pair.of(StructurePoolElement.legacy("village/desert/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/desert/camel",
         new StructureTemplatePool(
            empty, ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/desert/camel_spawn"), 1)), StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/desert/zombie/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/desert/zombie/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
