package net.minecraft.data.worldgen;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

public class BastionBridgePools {
   public static void bootstrap(BootstrapContext<StructureTemplatePool> p_336342_) {
      HolderGetter<StructureProcessorList> holdergetter = p_336342_.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> holder = holdergetter.getOrThrow(ProcessorLists.ENTRANCE_REPLACEMENT);
      Holder<StructureProcessorList> holder1 = holdergetter.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);
      Holder<StructureProcessorList> holder2 = holdergetter.getOrThrow(ProcessorLists.BRIDGE);
      Holder<StructureProcessorList> holder3 = holdergetter.getOrThrow(ProcessorLists.RAMPART_DEGRADATION);
      HolderGetter<StructureTemplatePool> holdergetter1 = p_336342_.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> holder4 = holdergetter1.getOrThrow(Pools.EMPTY);
      Pools.register(
         p_336342_,
         "bastion/bridge/starting_pieces",
         new StructureTemplatePool(
            holder4,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance", holder), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_face", holder1), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_336342_,
         "bastion/bridge/bridge_pieces",
         new StructureTemplatePool(
            holder4,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/bridge/bridge_pieces/bridge", holder2), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_336342_,
         "bastion/bridge/legs",
         new StructureTemplatePool(
            holder4,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/legs/leg_0", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/legs/leg_1", holder1), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_336342_,
         "bastion/bridge/walls",
         new StructureTemplatePool(
            holder4,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/walls/wall_base_0", holder3), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/walls/wall_base_1", holder3), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_336342_,
         "bastion/bridge/ramparts",
         new StructureTemplatePool(
            holder4,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/ramparts/rampart_0", holder3), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/ramparts/rampart_1", holder3), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_336342_,
         "bastion/bridge/rampart_plates",
         new StructureTemplatePool(
            holder4,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/bridge/rampart_plates/plate_0", holder3), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         p_336342_,
         "bastion/bridge/connectors",
         new StructureTemplatePool(
            holder4,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/connectors/back_bridge_top", holder1), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/connectors/back_bridge_bottom", holder1), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
