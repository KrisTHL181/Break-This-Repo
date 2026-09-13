package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class BastionHoglinStablePools {
   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> stableDegradation = processorLists.getOrThrow(ProcessorLists.STABLE_DEGRADATION);
      Holder<StructureProcessorList> sideWallDegradation = processorLists.getOrThrow(ProcessorLists.SIDE_WALL_DEGRADATION);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Pools.register(
         context,
         "bastion/hoglin_stable/starting_pieces",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/starting_stairs_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/starting_stairs_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/starting_stairs_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/starting_stairs_3", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/starting_stairs_4", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/mirrored_starting_pieces",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/stairs_0_mirrored", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/stairs_1_mirrored", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/stairs_2_mirrored", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/stairs_3_mirrored", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/starting_pieces/stairs_4_mirrored", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/wall_bases",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/hoglin_stable/walls/wall_base", stableDegradation), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/walls",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/walls/side_wall_0", sideWallDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/walls/side_wall_1", sideWallDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/stairs",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_1_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_1_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_1_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_1_3", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_1_4", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_2_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_2_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_2_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_2_3", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_2_4", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_3_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_3_1", stableDegradation), 1),
               new Pair[]{
                  Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_3_2", stableDegradation), 1),
                  Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_3_3", stableDegradation), 1),
                  Pair.of(StructurePoolElement.single("bastion/hoglin_stable/stairs/stairs_3_4", stableDegradation), 1)
               }
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/small_stables/inner",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/inner_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/inner_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/inner_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/inner_3", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/small_stables/outer",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/outer_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/outer_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/outer_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/small_stables/outer_3", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/large_stables/inner",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/inner_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/inner_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/inner_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/inner_3", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/inner_4", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/large_stables/outer",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/outer_0", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/outer_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/outer_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/outer_3", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/large_stables/outer_4", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/posts",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/posts/stair_post", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/posts/end_post", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/ramparts",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/ramparts/ramparts_1", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/ramparts/ramparts_2", stableDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/hoglin_stable/ramparts/ramparts_3", stableDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/rampart_plates",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/hoglin_stable/rampart_plates/rampart_plate_1", stableDegradation), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/hoglin_stable/connectors",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/hoglin_stable/connectors/end_post_connector", stableDegradation), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
