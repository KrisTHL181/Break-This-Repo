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

public class PlainVillagePools {
   public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("village/plains/town_centers");
   private static final ResourceKey<StructureTemplatePool> TERMINATORS_KEY = Pools.createKey("village/plains/terminators");

   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
      Holder<PlacedFeature> oakVillage = placedFeatures.getOrThrow(VillagePlacements.OAK_VILLAGE);
      Holder<PlacedFeature> flowerPlainVillage = placedFeatures.getOrThrow(VillagePlacements.FLOWER_PLAIN_VILLAGE);
      Holder<PlacedFeature> pileHayVillage = placedFeatures.getOrThrow(VillagePlacements.PILE_HAY_VILLAGE);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> mossify10Percent = processorLists.getOrThrow(ProcessorLists.MOSSIFY_10_PERCENT);
      Holder<StructureProcessorList> mossify20Percent = processorLists.getOrThrow(ProcessorLists.MOSSIFY_20_PERCENT);
      Holder<StructureProcessorList> mossify70Percent = processorLists.getOrThrow(ProcessorLists.MOSSIFY_70_PERCENT);
      Holder<StructureProcessorList> zombiePlains = processorLists.getOrThrow(ProcessorLists.ZOMBIE_PLAINS);
      Holder<StructureProcessorList> streetPlains = processorLists.getOrThrow(ProcessorLists.STREET_PLAINS);
      Holder<StructureProcessorList> farmPlains = processorLists.getOrThrow(ProcessorLists.FARM_PLAINS);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Holder<StructureTemplatePool> terminators = pools.getOrThrow(TERMINATORS_KEY);
      context.register(
         START,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_fountain_01", mossify20Percent), 50),
               Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_1", mossify20Percent), 50),
               Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_2"), 50),
               Pair.of(StructurePoolElement.legacy("village/plains/town_centers/plains_meeting_point_3", mossify70Percent), 50),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_fountain_01", zombiePlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_1", zombiePlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_2", zombiePlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/town_centers/plains_meeting_point_3", zombiePlains), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/plains/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/streets/corner_01", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/corner_02", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/corner_03", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_01", streetPlains), 4),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_02", streetPlains), 4),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_03", streetPlains), 7),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_04", streetPlains), 7),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_05", streetPlains), 3),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/straight_06", streetPlains), 4),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_01", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_02", streetPlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_03", streetPlains), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_04", streetPlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_05", streetPlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/streets/crossroad_06", streetPlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/streets/turn_01", streetPlains), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/plains/zombie/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/corner_01", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/corner_02", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/corner_03", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_01", streetPlains), 4),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_02", streetPlains), 4),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_03", streetPlains), 7),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_04", streetPlains), 7),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_05", streetPlains), 3),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/straight_06", streetPlains), 4),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_01", streetPlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_02", streetPlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_03", streetPlains), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_04", streetPlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_05", streetPlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/crossroad_06", streetPlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/streets/turn_01", streetPlains), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/plains/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_1", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_2", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_3", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_4", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_5", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_6", mossify10Percent), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_7", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_house_8", mossify10Percent), 3),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_medium_house_1", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_medium_house_2", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_big_house_1", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_butcher_shop_1", mossify10Percent), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_butcher_shop_2", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tool_smith_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_fletcher_house_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_shepherds_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_armorer_house_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_fisher_cottage_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tannery_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_cartographer_1", mossify10Percent), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_1", mossify10Percent), 5),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_2", mossify10Percent), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_masons_house_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_weaponsmith_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_3", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_4", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_stable_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_stable_2"), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_large_farm_1", farmPlains), 4),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_farm_1", farmPlains), 4),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_1"), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_2"), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_3"), 5),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_accessory_1"), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_meeting_point_4", mossify70Percent), 3),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_meeting_point_5"), 1),
                  Pair.of(StructurePoolElement.empty(), 10)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/plains/zombie/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_1", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_2", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_3", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_4", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_5", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_6", zombiePlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_7", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_small_house_8", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_medium_house_1", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_medium_house_2", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_big_house_1", zombiePlains), 2),
               Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_butcher_shop_1", zombiePlains), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_butcher_shop_2", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tool_smith_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_fletcher_house_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_shepherds_house_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_armorer_house_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_fisher_cottage_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_tannery_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_cartographer_1", zombiePlains), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_1", zombiePlains), 3),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_library_2", zombiePlains), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_masons_house_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_weaponsmith_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_3", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_temple_4", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_stable_1", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_stable_2", zombiePlains), 2),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_large_farm_1", zombiePlains), 4),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_small_farm_1", zombiePlains), 4),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_1", zombiePlains), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/houses/plains_animal_pen_2", zombiePlains), 1),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_animal_pen_3", zombiePlains), 5),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_meeting_point_4", zombiePlains), 3),
                  Pair.of(StructurePoolElement.legacy("village/plains/zombie/houses/plains_meeting_point_5", zombiePlains), 1),
                  Pair.of(StructurePoolElement.empty(), 10)
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
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_01", streetPlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_02", streetPlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_03", streetPlains), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_04", streetPlains), 1)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/plains/trees",
         new StructureTemplatePool(empty, ImmutableList.of(Pair.of(StructurePoolElement.feature(oakVillage), 1)), StructureTemplatePool.Projection.RIGID)
      );
      Pools.register(
         context,
         "village/plains/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/plains_lamp_1"), 2),
               Pair.of(StructurePoolElement.feature(oakVillage), 1),
               Pair.of(StructurePoolElement.feature(flowerPlainVillage), 1),
               Pair.of(StructurePoolElement.feature(pileHayVillage), 1),
               Pair.of(StructurePoolElement.empty(), 2)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/plains/zombie/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/plains_lamp_1", zombiePlains), 1),
               Pair.of(StructurePoolElement.feature(oakVillage), 1),
               Pair.of(StructurePoolElement.feature(flowerPlainVillage), 1),
               Pair.of(StructurePoolElement.feature(pileHayVillage), 1),
               Pair.of(StructurePoolElement.empty(), 2)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/plains/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/villagers/baby"), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/plains/zombie/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/zombie/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/common/animals",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/common/animals/cows_1"), 7),
               Pair.of(StructurePoolElement.legacy("village/common/animals/pigs_1"), 7),
               Pair.of(StructurePoolElement.legacy("village/common/animals/horses_1"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/horses_2"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/horses_3"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/horses_4"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/horses_5"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_1"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_2"), 1),
               Pair.of(StructurePoolElement.empty(), 5)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/common/sheep",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_1"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_2"), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/common/cats",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_black"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_british"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_calico"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_persian"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_ragdoll"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_red"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_siamese"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_tabby"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_white"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/cat_jellie"), 1),
               Pair.of(StructurePoolElement.empty(), 3)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/common/butcher_animals",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/common/animals/cows_1"), 3),
               Pair.of(StructurePoolElement.legacy("village/common/animals/pigs_1"), 3),
               Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_1"), 1),
               Pair.of(StructurePoolElement.legacy("village/common/animals/sheep_2"), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/common/iron_golem",
         new StructureTemplatePool(
            empty, ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/iron_golem"), 1)), StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/common/well_bottoms",
         new StructureTemplatePool(
            empty, ImmutableList.of(Pair.of(StructurePoolElement.legacy("village/common/well_bottom"), 1)), StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
