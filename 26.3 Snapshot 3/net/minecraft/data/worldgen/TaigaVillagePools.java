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

public class TaigaVillagePools {
   public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("village/taiga/town_centers");
   private static final ResourceKey<StructureTemplatePool> TERMINATORS_KEY = Pools.createKey("village/taiga/terminators");

   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
      Holder<PlacedFeature> spruceVillage = placedFeatures.getOrThrow(VillagePlacements.SPRUCE_VILLAGE);
      Holder<PlacedFeature> pineVillage = placedFeatures.getOrThrow(VillagePlacements.PINE_VILLAGE);
      Holder<PlacedFeature> pilePumpkinVillage = placedFeatures.getOrThrow(VillagePlacements.PILE_PUMPKIN_VILLAGE);
      Holder<PlacedFeature> patchTaigaGrassVillage = placedFeatures.getOrThrow(VillagePlacements.PATCH_TAIGA_GRASS_VILLAGE);
      Holder<PlacedFeature> patchBerryBushVillage = placedFeatures.getOrThrow(VillagePlacements.PATCH_BERRY_BUSH_VILLAGE);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> mossify10Percent = processorLists.getOrThrow(ProcessorLists.MOSSIFY_10_PERCENT);
      Holder<StructureProcessorList> zombieTaiga = processorLists.getOrThrow(ProcessorLists.ZOMBIE_TAIGA);
      Holder<StructureProcessorList> streetSnowyOrTaiga = processorLists.getOrThrow(ProcessorLists.STREET_SNOWY_OR_TAIGA);
      Holder<StructureProcessorList> farmTaiga = processorLists.getOrThrow(ProcessorLists.FARM_TAIGA);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Holder<StructureTemplatePool> terminators = pools.getOrThrow(TERMINATORS_KEY);
      context.register(
         START,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/town_centers/taiga_meeting_point_1", mossify10Percent), 49),
               Pair.of(StructurePoolElement.legacy("village/taiga/town_centers/taiga_meeting_point_2", mossify10Percent), 49),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_1", zombieTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/town_centers/taiga_meeting_point_2", zombieTaiga), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/taiga/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/corner_01", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/corner_02", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/corner_03", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/straight_01", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/straight_02", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/straight_03", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/straight_04", streetSnowyOrTaiga), 7),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/straight_05", streetSnowyOrTaiga), 7),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/straight_06", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/crossroad_01", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/crossroad_02", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/streets/crossroad_03", streetSnowyOrTaiga), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/taiga/streets/crossroad_04", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/streets/crossroad_05", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/streets/crossroad_06", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/streets/turn_01", streetSnowyOrTaiga), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/taiga/zombie/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/corner_01", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/corner_02", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/corner_03", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/straight_01", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/straight_02", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/straight_03", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/straight_04", streetSnowyOrTaiga), 7),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/straight_05", streetSnowyOrTaiga), 7),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/straight_06", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/crossroad_01", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/crossroad_02", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/crossroad_03", streetSnowyOrTaiga), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/crossroad_04", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/crossroad_05", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/crossroad_06", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/streets/turn_01", streetSnowyOrTaiga), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/taiga/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_small_house_1", mossify10Percent), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_small_house_2", mossify10Percent), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_small_house_3", mossify10Percent), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_small_house_4", mossify10Percent), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_small_house_5", mossify10Percent), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_medium_house_1", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_medium_house_2", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_medium_house_3", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_medium_house_4", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_butcher_shop_1", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_tool_smith_1", mossify10Percent), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_fletcher_house_1", mossify10Percent), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_shepherds_house_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_armorer_house_1", mossify10Percent), 1),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_armorer_2", mossify10Percent), 1),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_fisher_cottage_1", mossify10Percent), 3),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_tannery_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_cartographer_house_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_library_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_masons_house_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_weaponsmith_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_weaponsmith_2", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_temple_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_large_farm_1", farmTaiga), 6),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_large_farm_2", farmTaiga), 6),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_small_farm_1", mossify10Percent), 1),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_animal_pen_1", mossify10Percent), 2),
                  Pair.of(StructurePoolElement.empty(), 6)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/taiga/zombie/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_small_house_1", zombieTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_small_house_2", zombieTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_small_house_3", zombieTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_small_house_4", zombieTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_small_house_5", zombieTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_medium_house_1", zombieTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_medium_house_2", zombieTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_medium_house_3", zombieTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_medium_house_4", zombieTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_butcher_shop_1", zombieTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_tool_smith_1", zombieTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_fletcher_house_1", zombieTaiga), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_shepherds_house_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_armorer_house_1", zombieTaiga), 1),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_fisher_cottage_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_tannery_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_cartographer_house_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_library_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_masons_house_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_weaponsmith_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_weaponsmith_2", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_temple_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_large_farm_1", zombieTaiga), 6),
                  Pair.of(StructurePoolElement.legacy("village/taiga/zombie/houses/taiga_large_farm_2", zombieTaiga), 6),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_small_farm_1", zombieTaiga), 1),
                  Pair.of(StructurePoolElement.legacy("village/taiga/houses/taiga_animal_pen_1", zombieTaiga), 2),
                  Pair.of(StructurePoolElement.empty(), 6)
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
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_01", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_02", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_03", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/plains/terminators/terminator_04", streetSnowyOrTaiga), 1)
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/taiga/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_lamp_post_1"), 10),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_1"), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_2"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_3"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_4"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_5"), 2),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_6"), 1),
               Pair.of(StructurePoolElement.feature(spruceVillage), 4),
               Pair.of(StructurePoolElement.feature(pineVillage), 4),
               Pair.of(StructurePoolElement.feature(pilePumpkinVillage), 2),
               Pair.of(StructurePoolElement.feature(patchTaigaGrassVillage), 4),
               Pair.of(StructurePoolElement.feature(patchBerryBushVillage), 1),
               new Pair[]{Pair.of(StructurePoolElement.empty(), 4)}
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/taiga/zombie/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_1"), 4),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_2"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_3"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/taiga_decoration_4"), 1),
               Pair.of(StructurePoolElement.feature(spruceVillage), 4),
               Pair.of(StructurePoolElement.feature(pineVillage), 4),
               Pair.of(StructurePoolElement.feature(pilePumpkinVillage), 2),
               Pair.of(StructurePoolElement.feature(patchTaigaGrassVillage), 4),
               Pair.of(StructurePoolElement.feature(patchBerryBushVillage), 1),
               Pair.of(StructurePoolElement.empty(), 4)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/taiga/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/villagers/baby"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/taiga/zombie/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/taiga/zombie/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
