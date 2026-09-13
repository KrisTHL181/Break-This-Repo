package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import org.jspecify.annotations.Nullable;

public record SteppedColumnClusterFeature(
   BlockStateProvider block,
   BlockPredicate continueThrough,
   BlockPredicate canReplace,
   HolderSet<Block> cannotPlaceOn,
   IntProvider clusterReach,
   IntProvider columnCount,
   IntProvider columnReach,
   IntProvider height
) implements Feature {
   public static final MapCodec<SteppedColumnClusterFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("block").forGetter(SteppedColumnClusterFeature::block),
            BlockPredicate.CODEC.fieldOf("continue_through").forGetter(SteppedColumnClusterFeature::continueThrough),
            BlockPredicate.CODEC.fieldOf("can_replace").forGetter(SteppedColumnClusterFeature::canReplace),
            RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("cannot_place_on").forGetter(SteppedColumnClusterFeature::cannotPlaceOn),
            IntProviders.codec(0, 13).fieldOf("cluster_reach").forGetter(SteppedColumnClusterFeature::clusterReach),
            IntProviders.codec(1, 150).fieldOf("column_count").forGetter(SteppedColumnClusterFeature::columnCount),
            IntProviders.codec(0, 3).fieldOf("column_reach").forGetter(SteppedColumnClusterFeature::columnReach),
            IntProviders.codec(1, 10).fieldOf("height").forGetter(SteppedColumnClusterFeature::height)
         )
         .apply(i, SteppedColumnClusterFeature::new)
   );

   @Override
   public MapCodec<SteppedColumnClusterFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      int lavaSeaLevel = chunkGenerator.getSeaLevel();
      if (!this.canPlaceAt(level, origin.mutable())) {
         return false;
      }

      int columnHeight = this.height.sample(random);
      int clusterReach = Math.min(columnHeight, this.clusterReach.sample(random));
      int count = this.columnCount.sample(random);
      boolean placed = false;

      for (BlockPos pos : BlockPos.randomBetweenClosed(
         random,
         count,
         origin.getX() - clusterReach,
         origin.getY(),
         origin.getZ() - clusterReach,
         origin.getX() + clusterReach,
         origin.getY(),
         origin.getZ() + clusterReach
      )) {
         int blocksToPlaceY = columnHeight - pos.distManhattan(origin);
         if (blocksToPlaceY >= 0) {
            placed |= this.placeColumn(level, random, lavaSeaLevel, pos, blocksToPlaceY, this.columnReach.sample(random));
         }
      }

      return placed;
   }

   private boolean placeColumn(
      final WorldGenLevel level, final RandomSource random, final int lavaSeaLevel, final BlockPos origin, final int columnHeight, final int reach
   ) {
      boolean placedAny = false;

      for (BlockPos pos : BlockPos.betweenClosed(
         origin.getX() - reach, origin.getY(), origin.getZ() - reach, origin.getX() + reach, origin.getY(), origin.getZ() + reach
      )) {
         int stepLimit = pos.distManhattan(origin);
         BlockPos columnPos = this.canReplace.test(level, pos)
            ? this.findSurface(level, lavaSeaLevel, pos.mutable(), stepLimit)
            : this.findAir(level, pos.mutable(), stepLimit);
         if (columnPos != null) {
            int blocksY = columnHeight - stepLimit / 2;
            BlockPos.MutableBlockPos cursor = columnPos.mutable();

            while (blocksY >= 0) {
               if (this.canReplace.test(level, cursor)) {
                  this.setBlock(level, cursor, this.block.getState(level, random, cursor));
                  cursor.move(Direction.UP);
                  placedAny = true;
               } else {
                  if (!this.continueThrough.test(level, cursor)) {
                     break;
                  }

                  cursor.move(Direction.UP);
               }

               blocksY--;
            }
         }
      }

      return placedAny;
   }

   private @Nullable BlockPos findSurface(final WorldGenLevel level, final int lavaSeaLevel, final BlockPos.MutableBlockPos cursor, int limit) {
      while (cursor.getY() > level.getMinY() + 1 && limit > 0) {
         limit--;
         if (this.canPlaceAt(level, cursor)) {
            return cursor;
         }

         cursor.move(Direction.DOWN);
      }

      return null;
   }

   private boolean canPlaceAt(final WorldGenLevel level, final BlockPos.MutableBlockPos cursor) {
      if (!this.canReplace.test(level, cursor)) {
         return false;
      }

      BlockState blockState = level.getBlockState(cursor.move(Direction.DOWN));
      cursor.move(Direction.UP);
      return !blockState.isAir() && !blockState.is(this.cannotPlaceOn);
   }

   private @Nullable BlockPos findAir(final LevelAccessor level, final BlockPos.MutableBlockPos cursor, int limit) {
      while (cursor.getY() <= level.getMaxY() && limit > 0) {
         limit--;
         BlockState blockState = level.getBlockState(cursor);
         if (blockState.is(this.cannotPlaceOn)) {
            return null;
         }

         if (blockState.isAir()) {
            return cursor;
         }

         cursor.move(Direction.UP);
      }

      return null;
   }
}
