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
   public static void bootstrap(BootstrapContext<StructureTemplatePool> p_332381_) {
      HolderGetter<StructureProcessorList> holdergetter = p_332381_.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> holder = holdergetter.getOrThrow(ProcessorLists.TREASURE_ROOMS);
      Holder<StructureProcessorList> holder1 = holdergetter.getOrThrow(ProcessorLists.HIGH_WALL);
      Holder<StructureProcessorList> holder2 = holdergetter.getOrThrow(ProcessorLists.BOTTOM_RAMPART);
      Holder<StructureProcessorList> holder3 = holdergetter.getOrThrow(ProcessorLists.HIGH_RAMPART);
      Holder<StructureProcessorList> holder4 = holdergetter.getOrThrow(ProcessorLists.ROOF);
      HolderGetter<StructureTemplatePool> holdergetter1 = p_332381_.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> holder5 = holdergetter1.getOrThrow(Pools.EMPTY);
      Pools.register(
         p_332381_,
         "bastion/treasure/bases",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/bases/lava_basin", holder), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/stairs",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/stairs/lower_stairs", holder), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/bases/centers",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_1", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_2", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/bases/centers/center_3", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/brains",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/brains/center_brain", holder), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/walls",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/lava_wall", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/entrance_wall", holder1), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/walls/outer",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/top_corner", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/mid_corner", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/bottom_corner", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/outer_wall", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/medium_outer_wall", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/outer/tall_outer_wall", holder1), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/walls/bottom",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_1", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_2", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/bottom/wall_3", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/walls/mid",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/mid/wall_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/mid/wall_1", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/mid/wall_2", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/walls/top",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/top/main_entrance", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/top/wall_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/walls/top/wall_1", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/connectors",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/connectors/center_to_wall_middle", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/connectors/center_to_wall_top", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/connectors/center_to_wall_top_entrance", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/entrances",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/treasure/entrances/entrance_0", holder), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/ramparts",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/mid_wall_main", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/mid_wall_side", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/bottom_wall_0", holder2), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/top_wall", holder3), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/lava_basin_side", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/ramparts/lava_basin_main", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/corners/bottom",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/bottom/corner_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/bottom/corner_1", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/corners/edges",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/edges/bottom", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/edges/middle", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/edges/top", holder1), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/corners/middle",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/middle/corner_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/middle/corner_1", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/corners/top",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/top/corner_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/corners/top/corner_1", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/extensions/large_pool",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/fire_room", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_1", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_2", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/large_bridge_3", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/roofed_bridge", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/extensions/small_pool",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/fire_room", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/empty", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_1", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_2", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/small_bridge_3", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/extensions/houses",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/house_0", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/extensions/house_1", holder), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_332381_,
         "bastion/treasure/roofs",
         new StructureTemplatePool(
            holder5,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/treasure/roofs/wall_roof", holder4), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/roofs/corner_roof", holder4), 1),
               Pair.of(StructurePoolElement.single("bastion/treasure/roofs/center_roof", holder4), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
