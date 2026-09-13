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

public class SnowyVillagePools {
   public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("village/snowy/town_centers");
   private static final ResourceKey<StructureTemplatePool> TERMINATORS_KEY = Pools.createKey("village/snowy/terminators");

   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
      Holder<PlacedFeature> spruceVillage = placedFeatures.getOrThrow(VillagePlacements.SPRUCE_VILLAGE);
      Holder<PlacedFeature> pileSnowVillage = placedFeatures.getOrThrow(VillagePlacements.PILE_SNOW_VILLAGE);
      Holder<PlacedFeature> pileIceVillage = placedFeatures.getOrThrow(VillagePlacements.PILE_ICE_VILLAGE);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> streetSnowyOrTaiga = processorLists.getOrThrow(ProcessorLists.STREET_SNOWY_OR_TAIGA);
      Holder<StructureProcessorList> farmSnowy = processorLists.getOrThrow(ProcessorLists.FARM_SNOWY);
      Holder<StructureProcessorList> zombieSnowy = processorLists.getOrThrow(ProcessorLists.ZOMBIE_SNOWY);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Holder<StructureTemplatePool> terminators = pools.getOrThrow(TERMINATORS_KEY);
      context.register(
         START,
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_1"), 100),
               Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_2"), 50),
               Pair.of(StructurePoolElement.legacy("village/snowy/town_centers/snowy_meeting_point_3"), 150),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_2"), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/town_centers/snowy_meeting_point_3"), 3)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/snowy/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/corner_01", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/corner_02", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/corner_03", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/square_01", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/straight_01", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/straight_02", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/straight_03", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/straight_04", streetSnowyOrTaiga), 7),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/straight_06", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/straight_08", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/crossroad_02", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/streets/crossroad_03", streetSnowyOrTaiga), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/snowy/streets/crossroad_04", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/streets/crossroad_05", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/streets/crossroad_06", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/streets/turn_01", streetSnowyOrTaiga), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/snowy/zombie/streets",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/corner_01", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/corner_02", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/corner_03", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/square_01", streetSnowyOrTaiga), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/straight_01", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/straight_02", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/straight_03", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/straight_04", streetSnowyOrTaiga), 7),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/straight_06", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/straight_08", streetSnowyOrTaiga), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/crossroad_02", streetSnowyOrTaiga), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/crossroad_03", streetSnowyOrTaiga), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/crossroad_04", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/crossroad_05", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/crossroad_06", streetSnowyOrTaiga), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/zombie/streets/turn_01", streetSnowyOrTaiga), 3)
               }
            ),
            StructureTemplatePool.Projection.TERRAIN_MATCHING
         )
      );
      Pools.register(
         context,
         "village/snowy/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_2"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_3"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_4"), 3),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_5"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_6"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_7"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_small_house_8"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_medium_house_1"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_medium_house_2"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_medium_house_3"), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_butchers_shop_1"), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_butchers_shop_2"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_tool_smith_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_fletcher_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_shepherds_house_1"), 3),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_armorer_house_1"), 1),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_armorer_house_2"), 1),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_fisher_cottage"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_tannery_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_cartographer_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_library_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_masons_house_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_masons_house_2"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_weapon_smith_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_temple_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_farm_1", farmSnowy), 3),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_farm_2", farmSnowy), 3),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_animal_pen_1"), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_animal_pen_2"), 2),
                  Pair.of(StructurePoolElement.empty(), 6)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/snowy/zombie/houses",
         new StructureTemplatePool(
            terminators,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_1", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_2", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_3", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_4", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_5", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_6", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_7", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_small_house_8", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_medium_house_1", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_medium_house_2", zombieSnowy), 2),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/houses/snowy_medium_house_3", zombieSnowy), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_butchers_shop_1", zombieSnowy), 2),
               new Pair[]{
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_butchers_shop_2", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_tool_smith_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_fletcher_house_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_shepherds_house_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_armorer_house_1", zombieSnowy), 1),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_armorer_house_2", zombieSnowy), 1),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_fisher_cottage", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_tannery_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_cartographer_house_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_library_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_masons_house_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_masons_house_2", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_weapon_smith_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_temple_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_farm_1", zombieSnowy), 3),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_farm_2", zombieSnowy), 3),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_animal_pen_1", zombieSnowy), 2),
                  Pair.of(StructurePoolElement.legacy("village/snowy/houses/snowy_animal_pen_2", zombieSnowy), 2),
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
         "village/snowy/trees",
         new StructureTemplatePool(empty, ImmutableList.of(Pair.of(StructurePoolElement.feature(spruceVillage), 1)), StructureTemplatePool.Projection.RIGID)
      );
      Pools.register(
         context,
         "village/snowy/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/snowy_lamp_post_01"), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/snowy_lamp_post_02"), 4),
               Pair.of(StructurePoolElement.legacy("village/snowy/snowy_lamp_post_03"), 1),
               Pair.of(StructurePoolElement.feature(spruceVillage), 4),
               Pair.of(StructurePoolElement.feature(pileSnowVillage), 4),
               Pair.of(StructurePoolElement.feature(pileIceVillage), 1),
               Pair.of(StructurePoolElement.empty(), 9)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/snowy/zombie/decor",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/snowy_lamp_post_01", zombieSnowy), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/snowy_lamp_post_02", zombieSnowy), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/snowy_lamp_post_03", zombieSnowy), 1),
               Pair.of(StructurePoolElement.feature(spruceVillage), 4),
               Pair.of(StructurePoolElement.feature(pileSnowVillage), 4),
               Pair.of(StructurePoolElement.feature(pileIceVillage), 4),
               Pair.of(StructurePoolElement.empty(), 7)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/snowy/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/villagers/baby"), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "village/snowy/zombie/villagers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/villagers/nitwit"), 1),
               Pair.of(StructurePoolElement.legacy("village/snowy/zombie/villagers/unemployed"), 10)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
