package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class AncientCityStructurePools {
   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
      Holder<PlacedFeature> sculkPatchAncientCity = placedFeatures.getOrThrow(CavePlacements.SCULK_PATCH_ANCIENT_CITY);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> ancientCityGenericDegradation = processorLists.getOrThrow(ProcessorLists.ANCIENT_CITY_GENERIC_DEGRADATION);
      Holder<StructureProcessorList> ancientCityWallsDegradation = processorLists.getOrThrow(ProcessorLists.ANCIENT_CITY_WALLS_DEGRADATION);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Pools.register(
         context,
         "ancient_city/structures",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.empty(), 7),
               Pair.of(StructurePoolElement.single("ancient_city/structures/barracks", ancientCityGenericDegradation), 4),
               Pair.of(StructurePoolElement.single("ancient_city/structures/chamber_1", ancientCityGenericDegradation), 4),
               Pair.of(StructurePoolElement.single("ancient_city/structures/chamber_2", ancientCityGenericDegradation), 4),
               Pair.of(StructurePoolElement.single("ancient_city/structures/chamber_3", ancientCityGenericDegradation), 4),
               Pair.of(StructurePoolElement.single("ancient_city/structures/sauna_1", ancientCityGenericDegradation), 4),
               Pair.of(StructurePoolElement.single("ancient_city/structures/small_statue", ancientCityGenericDegradation), 4),
               Pair.of(StructurePoolElement.single("ancient_city/structures/large_ruin_1", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/structures/tall_ruin_1", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/structures/tall_ruin_2", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/structures/tall_ruin_3", ancientCityGenericDegradation), 2),
               Pair.of(StructurePoolElement.single("ancient_city/structures/tall_ruin_4", ancientCityGenericDegradation), 2),
               new Pair[]{
                  Pair.of(
                     StructurePoolElement.list(
                        ImmutableList.of(
                           StructurePoolElement.single("ancient_city/structures/camp_1", ancientCityGenericDegradation),
                           StructurePoolElement.single("ancient_city/structures/camp_2", ancientCityGenericDegradation),
                           StructurePoolElement.single("ancient_city/structures/camp_3", ancientCityGenericDegradation)
                        )
                     ),
                     1
                  ),
                  Pair.of(StructurePoolElement.single("ancient_city/structures/medium_ruin_1", ancientCityGenericDegradation), 1),
                  Pair.of(StructurePoolElement.single("ancient_city/structures/medium_ruin_2", ancientCityGenericDegradation), 1),
                  Pair.of(StructurePoolElement.single("ancient_city/structures/small_ruin_1", ancientCityGenericDegradation), 1),
                  Pair.of(StructurePoolElement.single("ancient_city/structures/small_ruin_2", ancientCityGenericDegradation), 1),
                  Pair.of(StructurePoolElement.single("ancient_city/structures/large_pillar_1", ancientCityGenericDegradation), 1),
                  Pair.of(StructurePoolElement.single("ancient_city/structures/medium_pillar_1", ancientCityGenericDegradation), 1),
                  Pair.of(StructurePoolElement.list(ImmutableList.of(StructurePoolElement.single("ancient_city/structures/ice_box_1"))), 1)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "ancient_city/sculk",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.feature(sculkPatchAncientCity), 6), Pair.of(StructurePoolElement.empty(), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "ancient_city/walls",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_corner_wall_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_intersection_wall_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_lshape_wall_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_2", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_2", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_3", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_4", ancientCityWallsDegradation), 4),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_passage_1", ancientCityWallsDegradation), 3),
               Pair.of(StructurePoolElement.single("ancient_city/walls/ruined_corner_wall_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/ruined_corner_wall_2", ancientCityWallsDegradation), 1),
               new Pair[]{
                  Pair.of(StructurePoolElement.single("ancient_city/walls/ruined_horizontal_wall_stairs_1", ancientCityWallsDegradation), 2),
                  Pair.of(StructurePoolElement.single("ancient_city/walls/ruined_horizontal_wall_stairs_2", ancientCityWallsDegradation), 2),
                  Pair.of(StructurePoolElement.single("ancient_city/walls/ruined_horizontal_wall_stairs_3", ancientCityWallsDegradation), 3),
                  Pair.of(StructurePoolElement.single("ancient_city/walls/ruined_horizontal_wall_stairs_4", ancientCityWallsDegradation), 3)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "ancient_city/walls/no_corners",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_2", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_1", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_2", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_3", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_4", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_stairs_5", ancientCityWallsDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/walls/intact_horizontal_wall_bridge", ancientCityWallsDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "ancient_city/city_center/walls",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/bottom_1", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/bottom_2", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/bottom_left_corner", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/bottom_right_corner_1", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/bottom_right_corner_2", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/left", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/right", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/top", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/top_right_corner", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city_center/walls/top_left_corner", ancientCityGenericDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "ancient_city/city/entrance",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("ancient_city/city/entrance/entrance_connector", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city/entrance/entrance_path_1", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city/entrance/entrance_path_2", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city/entrance/entrance_path_3", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city/entrance/entrance_path_4", ancientCityGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("ancient_city/city/entrance/entrance_path_5", ancientCityGenericDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
