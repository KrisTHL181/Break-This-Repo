package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public class DimensionOriginStructurePlacement implements StructurePlacement {
   public static final DimensionOriginStructurePlacement INSTANCE = new DimensionOriginStructurePlacement();
   public static final MapCodec<DimensionOriginStructurePlacement> CODEC = MapCodec.unit(INSTANCE);

   private DimensionOriginStructurePlacement() {
   }

   @Override
   public boolean isStructureChunk(final ChunkGeneratorStructureState state, final int sourceX, final int sourceZ) {
      ChunkPos origin = state.getDimensionOrigin();
      return origin.x() == sourceX && origin.z() == sourceZ;
   }

   @Override
   public MapCodec<DimensionOriginStructurePlacement> codec() {
      return CODEC;
   }
}
