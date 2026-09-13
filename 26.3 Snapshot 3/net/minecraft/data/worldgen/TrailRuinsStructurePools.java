package net.minecraft.data.worldgen;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class TrailRuinsStructurePools {
   public static final ResourceKey<StructureTemplatePool> START = Pools.createKey("trail_ruins/tower");

   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> housesArchyProcessor = processorLists.getOrThrow(ProcessorLists.TRAIL_RUINS_HOUSES_ARCHAEOLOGY);
      Holder<StructureProcessorList> roadsArchyProcessor = processorLists.getOrThrow(ProcessorLists.TRAIL_RUINS_ROADS_ARCHAEOLOGY);
      Holder<StructureProcessorList> towerTopArchyProcessor = processorLists.getOrThrow(ProcessorLists.TRAIL_RUINS_TOWER_TOP_ARCHAEOLOGY);
      context.register(
         START,
         new StructureTemplatePool(
            empty,
            List.of(
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_5", housesArchyProcessor), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "trail_ruins/tower/tower_top",
         new StructureTemplatePool(
            empty,
            List.of(
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_top_1", towerTopArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_top_2", towerTopArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_top_3", towerTopArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_top_4", towerTopArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/tower_top_5", towerTopArchyProcessor), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "trail_ruins/tower/additions",
         new StructureTemplatePool(
            empty,
            List.of(
               Pair.of(StructurePoolElement.single("trail_ruins/tower/hall_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/hall_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/hall_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/hall_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/hall_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/large_hall_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/large_hall_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/large_hall_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/large_hall_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/large_hall_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/one_room_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/one_room_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/one_room_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/one_room_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/one_room_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/platform_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/platform_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/platform_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/platform_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/platform_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/stable_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/stable_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/stable_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/stable_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/tower/stable_5", housesArchyProcessor), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "trail_ruins/roads",
         new StructureTemplatePool(
            empty,
            List.of(
               Pair.of(StructurePoolElement.single("trail_ruins/roads/long_road_end", roadsArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/roads/road_end_1", roadsArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/roads/road_section_1", roadsArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/roads/road_section_2", roadsArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/roads/road_section_3", roadsArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/roads/road_section_4", roadsArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/roads/road_spacer_1", roadsArchyProcessor), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "trail_ruins/buildings",
         new StructureTemplatePool(
            empty,
            List.of(
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_hall_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_hall_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_hall_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_hall_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_hall_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/large_room_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/large_room_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/large_room_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/large_room_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/large_room_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/one_room_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/one_room_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/one_room_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/one_room_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/one_room_5", housesArchyProcessor), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "trail_ruins/buildings/grouped",
         new StructureTemplatePool(
            empty,
            List.of(
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_full_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_full_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_full_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_full_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_full_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_lower_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_lower_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_lower_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_lower_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_lower_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_upper_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_upper_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_upper_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_upper_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_upper_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_room_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_room_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_room_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_room_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/buildings/group_room_5", housesArchyProcessor), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "trail_ruins/decor",
         new StructureTemplatePool(
            empty,
            List.of(
               Pair.of(StructurePoolElement.single("trail_ruins/decor/decor_1", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/decor/decor_2", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/decor/decor_3", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/decor/decor_4", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/decor/decor_5", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/decor/decor_6", housesArchyProcessor), 1),
               Pair.of(StructurePoolElement.single("trail_ruins/decor/decor_7", housesArchyProcessor), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
