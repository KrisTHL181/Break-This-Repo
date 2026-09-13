package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

public interface StructurePlacement {
   Codec<StructurePlacement> CODEC = BuiltInRegistries.STRUCTURE_PLACEMENT.byNameCodec().dispatch(StructurePlacement::codec, c -> c);

   boolean isStructureChunk(ChunkGeneratorStructureState state, int sourceX, int sourceZ);

   default boolean applyAdditionalChunkRestrictions(final int sourceX, final int sourceZ, final long levelSeed) {
      return true;
   }

   default BlockPos getLocatePos(final ChunkPos chunkPos) {
      return new BlockPos(chunkPos.getMinBlockX(), 0, chunkPos.getMinBlockZ()).offset(this.locateOffset());
   }

   default Vec3i locateOffset() {
      return Vec3i.ZERO;
   }

   MapCodec<? extends StructurePlacement> codec();
}
