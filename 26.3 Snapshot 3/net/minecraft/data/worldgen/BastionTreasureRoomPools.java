package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class BastionTreasureRoomPools {
   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> treasureRooms = processorLists.getOrThrow(ProcessorLists.TREASURE_ROOMS);
      Holder<StructureProcessorList> highWall = processorLists.getOrThrow(ProcessorLists.HIGH_WALL);
      Holder<StructureProcessorList> bottomRampart = processorLists.getOrThrow(ProcessorLists.BOTTOM_RAMPART);
      Holder<StructureProcessorList> highRampart = processorLists.getOrThrow(ProcessorLists.HIGH_RAMPART);
      Holder<StructureProcessorList> roof = processorLists.getOrThrow(ProcessorLists.ROOF);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Pools.register(
         context,
         "bastion/treasure/bases",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/bases/lava_basin", treasureRooms), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/stairs",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/stairs/lower_stairs", treasureRooms), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/bases/centers",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_1", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_2", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_3", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/brains",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/brains/center_brain", treasureRooms), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/walls",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/lava_wall", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/entrance_wall", highWall), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/walls/outer",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/top_corner", highWall), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/mid_corner", highWall), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/bottom_corner", highWall), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/outer_wall", highWall), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/medium_outer_wall", highWall), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/tall_outer_wall", highWall), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/walls/bottom",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_1", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_2", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_3", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/walls/mid",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/mid/wall_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/mid/wall_1", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/mid/wall_2", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/walls/top",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/top/main_entrance", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/top/wall_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/top/wall_1", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/connectors",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/connectors/center_to_wall_middle", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/connectors/center_to_wall_top", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/connectors/center_to_wall_top_entrance", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/entrances",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/entrances/entrance_0", treasureRooms), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/ramparts",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/mid_wall_main", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/mid_wall_side", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/bottom_wall_0", bottomRampart), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/top_wall", highRampart), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/lava_basin_side", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/lava_basin_main", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/corners/bottom",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/bottom/corner_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/bottom/corner_1", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/corners/edges",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/edges/bottom", highWall), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/edges/middle", highWall), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/edges/top", highWall), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/corners/middle",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/middle/corner_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/middle/corner_1", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/corners/top",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/top/corner_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/top/corner_1", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/extensions/large_pool",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/fire_room", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_1", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_2", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_3", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/roofed_bridge", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/extensions/small_pool",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/fire_room", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_1", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_2", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_3", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/extensions/houses",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/house_0", treasureRooms), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/house_1", treasureRooms), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/treasure/roofs",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/roofs/wall_roof", roof), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/roofs/corner_roof", roof), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/roofs/center_roof", roof), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
