package net.minecraft.world.level.block.state.pattern;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelReader;
import org.jspecify.annotations.Nullable;

public class BlockPattern {
   private final Predicate<BlockInWorld>[][][] pattern;
   private final int depth;
   private final int height;
   private final int width;

   public BlockPattern(final Predicate<BlockInWorld>[][][] pattern) {
      this.pattern = pattern;
      this.depth = pattern.length;
      if (this.depth > 0) {
         this.height = pattern[0].length;
         if (this.height > 0) {
            this.width = pattern[0][0].length;
         } else {
            this.width = 0;
         }
      } else {
         this.height = 0;
         this.width = 0;
      }
   }

   public int getDepth() {
      return this.depth;
   }

   public int getHeight() {
      return this.height;
   }

   public int getWidth() {
      return this.width;
   }

   @VisibleForTesting
   public Predicate<BlockInWorld>[][][] getPattern() {
      return this.pattern;
   }

   @VisibleForTesting
   public BlockPattern.@Nullable BlockPatternMatch matches(final LevelReader level, final BlockPos origin, final Direction forwards, final Direction up) {
      LoadingCache<BlockPos, BlockInWorld> cache = createLevelCache(level, false);
      return this.matches(origin, forwards, up, cache);
   }

   private BlockPattern.@Nullable BlockPatternMatch matches(
      final BlockPos origin, final Direction forwards, final Direction up, final LoadingCache<BlockPos, BlockInWorld> cache
   ) {
      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            for (int z = 0; z < this.depth; z++) {
               if (!this.pattern[z][y][x].test((BlockInWorld)cache.getUnchecked(translateAndRotate(origin, forwards, up, x, y, z)))) {
                  return null;
               }
            }
         }
      }

      return new BlockPattern.BlockPatternMatch(origin, forwards, up, cache, this.width, this.height, this.depth);
   }

   public BlockPattern.@Nullable BlockPatternMatch find(final LevelReader level, final BlockPos origin) {
      LoadingCache<BlockPos, BlockInWorld> cache = createLevelCache(level, false);
      int dist = Math.max(Math.max(this.width, this.height), this.depth);

      for (BlockPos testPos : BlockPos.betweenClosed(origin, origin.offset(dist - 1, dist - 1, dist - 1))) {
         for (Direction forwards : Direction.values()) {
            for (Direction up : Direction.values()) {
               if (up != forwards && up != forwards.getOpposite()) {
                  BlockPattern.BlockPatternMatch match = this.matches(testPos, forwards, up, cache);
                  if (match != null) {
                     return match;
                  }
               }
            }
         }
      }

      return null;
   }

   public static LoadingCache<BlockPos, BlockInWorld> createLevelCache(final LevelReader level, final boolean loadChunks) {
      return CacheBuilder.newBuilder().build(new BlockPattern.BlockCacheLoader(level, loadChunks));
   }

   protected static BlockPos translateAndRotate(
      final BlockPos origin, final Direction forwardsDirection, final Direction upDirection, final int right, final int down, final int forwards
   ) {
      if (forwardsDirection != upDirection && forwardsDirection != upDirection.getOpposite()) {
         Vec3i forwardsVector = new Vec3i(forwardsDirection.getStepX(), forwardsDirection.getStepY(), forwardsDirection.getStepZ());
         Vec3i upVector = new Vec3i(upDirection.getStepX(), upDirection.getStepY(), upDirection.getStepZ());
         Vec3i rightVector = forwardsVector.cross(upVector);
         return origin.offset(
            upVector.getX() * -down + rightVector.getX() * right + forwardsVector.getX() * forwards,
            upVector.getY() * -down + rightVector.getY() * right + forwardsVector.getY() * forwards,
            upVector.getZ() * -down + rightVector.getZ() * right + forwardsVector.getZ() * forwards
         );
      } else {
         throw new IllegalArgumentException("Invalid forwards & up combination");
      }
   }

   private static class BlockCacheLoader extends CacheLoader<BlockPos, BlockInWorld> {
      private final LevelReader level;
      private final boolean loadChunks;

      public BlockCacheLoader(final LevelReader level, final boolean loadChunks) {
         this.level = level;
         this.loadChunks = loadChunks;
      }

      public BlockInWorld load(final BlockPos key) {
         return new BlockInWorld(this.level, key, this.loadChunks);
      }
   }

   public static class BlockPatternMatch {
      private final BlockPos frontTopLeft;
      private final Direction forwards;
      private final Direction up;
      private final LoadingCache<BlockPos, BlockInWorld> cache;
      private final int width;
      private final int height;
      private final int depth;

      public BlockPatternMatch(
         final BlockPos frontTopLeft,
         final Direction forwards,
         final Direction up,
         final LoadingCache<BlockPos, BlockInWorld> cache,
         final int width,
         final int height,
         final int depth
      ) {
         this.frontTopLeft = frontTopLeft;
         this.forwards = forwards;
         this.up = up;
         this.cache = cache;
         this.width = width;
         this.height = height;
         this.depth = depth;
      }

      public BlockPos getFrontTopLeft() {
         return this.frontTopLeft;
      }

      public Direction getForwards() {
         return this.forwards;
      }

      public Direction getUp() {
         return this.up;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public int getDepth() {
         return this.depth;
      }

      public BlockInWorld getBlock(final int right, final int down, final int forwards) {
         return (BlockInWorld)this.cache
            .getUnchecked(BlockPattern.translateAndRotate(this.frontTopLeft, this.getForwards(), this.getUp(), right, down, forwards));
      }

      @Override
      public String toString() {
         return MoreObjects.toStringHelper(this).add("up", this.up).add("forwards", this.forwards).add("frontTopLeft", this.frontTopLeft).toString();
      }
   }
}
