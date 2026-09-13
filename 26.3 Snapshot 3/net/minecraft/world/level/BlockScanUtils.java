package net.minecraft.world.level;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Continuation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BlockScanUtils {
   private static boolean findBlocks(
      final LevelReader level, final BlockPos from, final BlockPos to, final @Nullable Predicate<BlockState> predicate, final BlockStateConsumer consumer
   ) {
      int minX = Math.min(from.getX(), to.getX());
      int minY = Math.min(from.getY(), to.getY());
      int minZ = Math.min(from.getZ(), to.getZ());
      int maxX = Math.max(from.getX(), to.getX());
      int maxY = Math.max(from.getY(), to.getY());
      int maxZ = Math.max(from.getZ(), to.getZ());
      return sectionBasedScan(level, minX, minY, minZ, maxX, maxY, maxZ, predicate, consumer);
   }

   private static boolean sectionBasedScan(
      final LevelReader level,
      final int minX,
      final int minY,
      final int minZ,
      final int maxX,
      final int maxY,
      final int maxZ,
      final @Nullable Predicate<BlockState> predicate,
      final BlockStateConsumer consumer
   ) {
      int minSectionX = SectionPos.blockToSectionCoord(minX);
      int minSectionY = SectionPos.blockToSectionCoord(minY);
      int minSectionZ = SectionPos.blockToSectionCoord(minZ);
      int maxSectionX = SectionPos.blockToSectionCoord(maxX);
      int maxSectionY = SectionPos.blockToSectionCoord(maxY);
      int maxSectionZ = SectionPos.blockToSectionCoord(maxZ);
      Predicate<BlockState> nonNullPredicate = predicate != null ? predicate : var0 -> true;

      for (int x = minSectionX; x <= maxSectionX; x++) {
         for (int z = minSectionZ; z <= maxSectionZ; z++) {
            ChunkAccess chunk = level.getChunk(x, z);

            for (int y = minSectionY; y <= maxSectionY; y++) {
               LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(y));
               if (predicate == null || section.maybeHas(predicate)) {
                  int sectionOriginX = SectionPos.sectionToBlockCoord(x);
                  int sectionOriginY = SectionPos.sectionToBlockCoord(y);
                  int sectionOriginZ = SectionPos.sectionToBlockCoord(z);
                  int fromX = SectionPos.sectionRelative(Math.max(sectionOriginX, minX));
                  int fromY = SectionPos.sectionRelative(Math.max(sectionOriginY, minY));
                  int fromZ = SectionPos.sectionRelative(Math.max(sectionOriginZ, minZ));
                  int toX = SectionPos.sectionRelative(Math.min(SectionPos.sectionToBlockCoord(x, 15), maxX));
                  int toY = SectionPos.sectionRelative(Math.min(SectionPos.sectionToBlockCoord(y, 15), maxY));
                  int toZ = SectionPos.sectionRelative(Math.min(SectionPos.sectionToBlockCoord(z, 15), maxZ));
                  BlockPos origin = new BlockPos(sectionOriginX, sectionOriginY, sectionOriginZ);
                  if (findBlocksInSection(section, origin, fromX, fromY, fromZ, toX, toY, toZ, nonNullPredicate, consumer)) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public static boolean findBlocksInSection(
      final LevelChunkSection section,
      final BlockPos origin,
      final int fromX,
      final int fromY,
      final int fromZ,
      final int toX,
      final int toY,
      final int toZ,
      final Predicate<BlockState> predicate,
      final BlockStateConsumer consumer
   ) {
      BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

      for (int y = fromY; y <= toY; y++) {
         for (int z = fromZ; z <= toZ; z++) {
            for (int x = fromX; x <= toX; x++) {
               BlockState blockState = section.getBlockState(x, y, z);
               if (predicate.test(blockState)) {
                  mutablePos.setWithOffset(origin, x, y, z);
                  if (consumer.apply(mutablePos, blockState).shouldAbort()) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public static class BlockMatcher {
      private final BlockPos from;
      private final BlockPos to;
      private final LevelReader levelReader;
      private @Nullable Predicate<BlockState> statePredicate = null;

      BlockMatcher(final LevelReader levelReader, final AABB box) {
         BlockPos minPos = BlockPos.containing(box.getMinPosition());
         BlockPos maxPos = BlockPos.containing(box.getMaxPosition());
         this(levelReader, minPos, maxPos);
      }

      BlockMatcher(final LevelReader levelReader, final BlockPos from, final BlockPos to) {
         this.levelReader = levelReader;
         this.from = from;
         this.to = to;
      }

      public BlockScanUtils.BlockMatcher filterState(final Predicate<BlockState> predicate) {
         this.statePredicate = this.statePredicate == null ? predicate : this.statePredicate.and(predicate);
         return this;
      }

      public boolean atLeastMatched(final int n) {
         return BlockScanUtils.findBlocks(this.levelReader, this.from, this.to, this.statePredicate, new BlockStateConsumer() {
            private int count = 0;

            @Override
            public Continuation apply(final BlockPos pos, final BlockState state) {
               return Continuation.abortIf(++this.count >= n);
            }
         });
      }

      public boolean atMostMatched(final int n) {
         return !this.atLeastMatched(n + 1);
      }

      public boolean anyMatched() {
         return BlockScanUtils.findBlocks(this.levelReader, this.from, this.to, this.statePredicate, (var0, var1) -> Continuation.ABORT);
      }

      public boolean noneMatched() {
         return !this.anyMatched();
      }

      public boolean allMatched() {
         return this.statePredicate == null
            ? true
            : !BlockScanUtils.findBlocks(this.levelReader, this.from, this.to, state -> !this.statePredicate.test(state), (var0, var1) -> Continuation.ABORT);
      }

      public void forEach(final BiConsumer<BlockPos, BlockState> consumer) {
         BlockScanUtils.findBlocks(this.levelReader, this.from, this.to, this.statePredicate, (pos, state) -> {
            consumer.accept(pos, state);
            return Continuation.CONTINUE;
         });
      }

      public boolean forEachUntil(final BlockStateConsumer consumer) {
         return BlockScanUtils.findBlocks(this.levelReader, this.from, this.to, this.statePredicate, consumer);
      }
   }
}
