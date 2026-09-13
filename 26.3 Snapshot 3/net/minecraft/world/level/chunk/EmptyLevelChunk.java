package net.minecraft.world.level.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

public class EmptyLevelChunk extends LevelChunk {
   private final Holder<Biome> biome;

   public EmptyLevelChunk(final Level level, final ChunkPos pos, final Holder<Biome> biome) {
      super(level, pos);
      this.biome = biome;
   }

   @Override
   public BlockState getBlockState(final BlockPos pos) {
      return Blocks.VOID_AIR.defaultBlockState();
   }

   @Override
   public @Nullable BlockState setBlockState(final BlockPos pos, final BlockState state, final @Block.UpdateFlags int flags) {
      return null;
   }

   @Override
   public FluidState getFluidState(final BlockPos pos) {
      return Fluids.EMPTY.defaultFluidState();
   }

   @Override
   public int getLightEmission(final BlockPos pos) {
      return 0;
   }

   @Override
   public @Nullable BlockEntity getBlockEntity(final BlockPos pos, final LevelChunk.EntityCreationType creationType) {
      return null;
   }

   @Override
   public void addAndRegisterBlockEntity(final BlockEntity blockEntity) {
   }

   @Override
   public void setBlockEntity(final BlockEntity blockEntity) {
   }

   @Override
   public void removeBlockEntity(final BlockPos pos) {
   }

   @Override
   public boolean isEmpty() {
      return true;
   }

   @Override
   public boolean isYSpaceEmpty(final int yStartInclusive, final int yEndInclusive) {
      return true;
   }

   @Override
   public FullChunkStatus getFullStatus() {
      return FullChunkStatus.FULL;
   }

   @Override
   public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ) {
      return this.biome;
   }
}
