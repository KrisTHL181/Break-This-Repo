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
   public static void bootstrap(final BootstrapContext<StructureTemplatePool> context) {
      HolderGetter<StructureProcessorList> processorLists = context.lookup(Registries.PROCESSOR_LIST);
      Holder<StructureProcessorList> entranceReplacement = processorLists.getOrThrow(ProcessorLists.ENTRANCE_REPLACEMENT);
      Holder<StructureProcessorList> bastionGenericDegradation = processorLists.getOrThrow(ProcessorLists.BASTION_GENERIC_DEGRADATION);
      Holder<StructureProcessorList> bridge = processorLists.getOrThrow(ProcessorLists.BRIDGE);
      Holder<StructureProcessorList> rampartDegradation = processorLists.getOrThrow(ProcessorLists.RAMPART_DEGRADATION);
      HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
      Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
      Pools.register(
         context,
         "bastion/bridge/starting_pieces",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance", entranceReplacement), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/starting_pieces/entrance_face", bastionGenericDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/bridge/bridge_pieces",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/bridge/bridge_pieces/bridge", bridge), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/bridge/legs",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/legs/leg_0", bastionGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/legs/leg_1", bastionGenericDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/bridge/walls",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/walls/wall_base_0", rampartDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/walls/wall_base_1", rampartDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/bridge/ramparts",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/ramparts/rampart_0", rampartDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/ramparts/rampart_1", rampartDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/bridge/rampart_plates",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(Pair.of(StructurePoolElement.single("bastion/bridge/rampart_plates/plate_0", rampartDegradation), 1)),
            StructureTemplatePool.Projection.RIGID
         )
      );
      Pools.register(
         context,
         "bastion/bridge/connectors",
         new StructureTemplatePool(
            empty,
            ImmutableList.of(
               Pair.of(StructurePoolElement.single("bastion/bridge/connectors/back_bridge_top", bastionGenericDegradation), 1),
               Pair.of(StructurePoolElement.single("bastion/bridge/connectors/back_bridge_bottom", bastionGenericDegradation), 1)
            ),
            StructureTemplatePool.Projection.RIGID
         )
      );
   }
}
