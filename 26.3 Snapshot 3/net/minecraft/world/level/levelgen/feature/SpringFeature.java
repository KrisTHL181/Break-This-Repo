package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.material.FluidState;

public record SpringFeature(FluidState state, boolean requiresBlockBelow, int rockCount, int holeCount, HolderSet<Block> validBlocks) implements Feature {
   public static final MapCodec<SpringFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            FluidState.CODEC.fieldOf("state").forGetter(SpringFeature::state),
            Codec.BOOL.optionalFieldOf("requires_block_below", true).forGetter(SpringFeature::requiresBlockBelow),
            Codec.INT.optionalFieldOf("rock_count", 4).forGetter(SpringFeature::rockCount),
            Codec.INT.optionalFieldOf("hole_count", 1).forGetter(SpringFeature::holeCount),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("valid_blocks").forGetter(SpringFeature::validBlocks)
         )
         .apply(i, SpringFeature::new)
   );

   @Override
   public MapCodec<SpringFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      if (!level.getBlockState(origin.above()).is(this.validBlocks)) {
         return false;
      }

      if (this.requiresBlockBelow && !level.getBlockState(origin.below()).is(this.validBlocks)) {
         return false;
      }

      BlockState currentState = level.getBlockState(origin);
      if (!currentState.isAir() && !currentState.is(this.validBlocks)) {
         return false;
      }

      int placed = 0;
      int rockCount = 0;
      if (level.getBlockState(origin.west()).is(this.validBlocks)) {
         rockCount++;
      }

      if (level.getBlockState(origin.east()).is(this.validBlocks)) {
         rockCount++;
      }

      if (level.getBlockState(origin.north()).is(this.validBlocks)) {
         rockCount++;
      }

      if (level.getBlockState(origin.south()).is(this.validBlocks)) {
         rockCount++;
      }

      if (level.getBlockState(origin.below()).is(this.validBlocks)) {
         rockCount++;
      }

      int holeCount = 0;
      if (level.isEmptyBlock(origin.west())) {
         holeCount++;
      }

      if (level.isEmptyBlock(origin.east())) {
         holeCount++;
      }

      if (level.isEmptyBlock(origin.north())) {
         holeCount++;
      }

      if (level.isEmptyBlock(origin.south())) {
         holeCount++;
      }

      if (level.isEmptyBlock(origin.below())) {
         holeCount++;
      }

      if (rockCount == this.rockCount && holeCount == this.holeCount) {
         level.setBlock(origin, this.state.createLegacyBlock(), 2);
         level.scheduleTick(origin, this.state.getType(), 0);
         placed++;
      }

      return placed > 0;
   }
}
