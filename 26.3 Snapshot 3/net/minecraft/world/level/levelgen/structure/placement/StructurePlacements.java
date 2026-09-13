package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;

public interface StructurePlacements {
   static MapCodec<? extends StructurePlacement> bootstrap(final Registry<MapCodec<? extends StructurePlacement>> registry) {
      Registry.register(registry, "concentric_rings", ConcentricRingsStructurePlacement.CODEC);
      Registry.register(registry, "dimension_origin", DimensionOriginStructurePlacement.CODEC);
      return Registry.register(registry, "random_spread", RandomSpreadStructurePlacement.CODEC);
   }
}
