package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public record DeltaFeature(BlockState contents, BlockState rim, IntProvider size, IntProvider rimSize) implements Feature {
   public static final MapCodec<DeltaFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockState.CODEC.fieldOf("contents").forGetter(DeltaFeature::contents),
            BlockState.CODEC.fieldOf("rim").forGetter(DeltaFeature::rim),
            IntProviders.codec(0, 16).fieldOf("size").forGetter(DeltaFeature::size),
            IntProviders.codec(0, 16).fieldOf("rim_size").forGetter(DeltaFeature::rimSize)
         )
         .apply(i, DeltaFeature::new)
   );
   private static final ImmutableList<Block> CANNOT_REPLACE = ImmutableList.of(
      Blocks.BEDROCK, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICK_FENCE, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_WART, Blocks.CHEST, Blocks.SPAWNER
   );
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final double RIM_SPAWN_CHANCE = 0.9;

   @Override
   public MapCodec<DeltaFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      boolean anyPlaced = false;
      boolean spawnRim = random.nextDouble() < 0.9;
      int rimX = spawnRim ? this.rimSize.sample(random) : 0;
      int rimZ = spawnRim ? this.rimSize.sample(random) : 0;
      boolean hasRim = spawnRim && rimX != 0 && rimZ != 0;
      int radiusX = this.size.sample(random);
      int radiusZ = this.size.sample(random);
      int radiusLimit = Math.max(radiusX, radiusZ);

      for (BlockPos pos : BlockPos.withinManhattan(origin, radiusX, 0, radiusZ)) {
         if (pos.distManhattan(origin) > radiusLimit) {
            break;
         }

         if (this.isClear(level, pos)) {
            if (hasRim) {
               anyPlaced = true;
               this.setBlock(level, pos, this.rim);
            }

            BlockPos posOffset = pos.offset(rimX, 0, rimZ);
            if (this.isClear(level, posOffset)) {
               anyPlaced = true;
               this.setBlock(level, posOffset, this.contents);
            }
         }
      }

      return anyPlaced;
   }

   private boolean isClear(final LevelAccessor level, final BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.is(this.contents.getBlock())) {
         return false;
      }

      if (CANNOT_REPLACE.contains(state.getBlock())) {
         return false;
      }

      for (Direction d : DIRECTIONS) {
         boolean isAir = level.getBlockState(pos.relative(d)).isAir();
         if (isAir && d != Direction.UP || !isAir && d == Direction.UP) {
            return false;
         }
      }

      return true;
   }
}
