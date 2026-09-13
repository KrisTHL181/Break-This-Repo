package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChunkSkyLightSources {
   private static final int SIZE = 16;
   public static final int NEGATIVE_INFINITY = Integer.MIN_VALUE;
   private final int minY;
   private final BitStorage heightmap;
   private final BlockPos.MutableBlockPos mutablePos1 = new BlockPos.MutableBlockPos();
   private final BlockPos.MutableBlockPos mutablePos2 = new BlockPos.MutableBlockPos();

   public ChunkSkyLightSources(final LevelHeightAccessor level) {
      this.minY = level.getMinY() - 1;
      int maxY = level.getMaxY() + 1;
      int bits = Mth.ceillog2(maxY - this.minY + 1);
      this.heightmap = new SimpleBitStorage(bits, 256);
   }

   public void fillFrom(final ChunkAccess chunk) {
      int maxSectionIndex = chunk.getHighestFilledSectionIndex();
      if (maxSectionIndex == -1) {
         this.fill(this.minY);
      } else {
         for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
               int initialEdgeY = Math.max(this.findLowestSourceY(chunk, maxSectionIndex, x, z), this.minY);
               this.set(index(x, z), initialEdgeY);
            }
         }
      }
   }

   private int findLowestSourceY(final ChunkAccess chunk, final int topSectionIndex, final int x, final int z) {
      int topY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(topSectionIndex) + 1);
      BlockPos.MutableBlockPos topPos = this.mutablePos1.set(x, topY, z);
      BlockPos.MutableBlockPos bottomPos = this.mutablePos2.setWithOffset(topPos, Direction.DOWN);
      BlockState topState = Blocks.AIR.defaultBlockState();

      for (int sectionIndex = topSectionIndex; sectionIndex >= 0; sectionIndex--) {
         LevelChunkSection section = chunk.getSection(sectionIndex);
         if (section.hasOnlyAir()) {
            topState = Blocks.AIR.defaultBlockState();
            int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            topPos.setY(SectionPos.sectionToBlockCoord(sectionY));
            bottomPos.setY(topPos.getY() - 1);
         } else {
            for (int y = 15; y >= 0; y--) {
               BlockState bottomState = section.getBlockState(x, y, z);
               if (isEdgeOccluded(topState, bottomState)) {
                  return topPos.getY();
               }

               topState = bottomState;
               topPos.set(bottomPos);
               bottomPos.move(Direction.DOWN);
            }
         }
      }

      return this.minY;
   }

   public boolean update(final BlockGetter level, final int x, final int y, final int z) {
      int upperEdgeY = y + 1;
      int index = index(x, z);
      int currentLowestSourceY = this.get(index);
      if (upperEdgeY < currentLowestSourceY) {
         return false;
      }

      BlockPos topPos = this.mutablePos1.set(x, y + 1, z);
      BlockState topState = level.getBlockState(topPos);
      BlockPos middlePos = this.mutablePos2.set(x, y, z);
      BlockState middleState = level.getBlockState(middlePos);
      if (this.updateEdge(level, index, currentLowestSourceY, topPos, topState, middlePos, middleState)) {
         return true;
      }

      BlockPos bottomPos = this.mutablePos1.set(x, y - 1, z);
      BlockState bottomState = level.getBlockState(bottomPos);
      return this.updateEdge(level, index, currentLowestSourceY, middlePos, middleState, bottomPos, bottomState);
   }

   private boolean updateEdge(
      final BlockGetter level,
      final int index,
      final int oldTopEdgeY,
      final BlockPos topPos,
      final BlockState topState,
      final BlockPos bottomPos,
      final BlockState bottomState
   ) {
      int checkedEdgeY = topPos.getY();
      if (isEdgeOccluded(topState, bottomState)) {
         if (checkedEdgeY > oldTopEdgeY) {
            this.set(index, checkedEdgeY);
            return true;
         }
      } else if (checkedEdgeY == oldTopEdgeY) {
         this.set(index, this.findLowestSourceBelow(level, bottomPos, bottomState));
         return true;
      }

      return false;
   }

   private int findLowestSourceBelow(final BlockGetter level, final BlockPos startPos, final BlockState startState) {
      BlockPos.MutableBlockPos topPos = this.mutablePos1.set(startPos);
      BlockPos.MutableBlockPos bottomPos = this.mutablePos2.setWithOffset(startPos, Direction.DOWN);
      BlockState topState = startState;

      while (bottomPos.getY() >= this.minY) {
         BlockState bottomState = level.getBlockState(bottomPos);
         if (isEdgeOccluded(topState, bottomState)) {
            return topPos.getY();
         }

         topState = bottomState;
         topPos.set(bottomPos);
         bottomPos.move(Direction.DOWN);
      }

      return this.minY;
   }

   private static boolean isEdgeOccluded(final BlockState topState, final BlockState bottomState) {
      if (bottomState.getLightDampening() != 0) {
         return true;
      }

      VoxelShape topShape = LightEngine.getOcclusionShape(topState, Direction.DOWN);
      VoxelShape bottomShape = LightEngine.getOcclusionShape(bottomState, Direction.UP);
      return Shapes.faceShapeOccludes(topShape, bottomShape);
   }

   public int getLowestSourceY(final int x, final int z) {
      int value = this.get(index(x, z));
      return this.extendSourcesBelowWorld(value);
   }

   public int getHighestLowestSourceY() {
      int maxValue = Integer.MIN_VALUE;

      for (int i = 0; i < this.heightmap.getSize(); i++) {
         int value = this.heightmap.get(i);
         if (value > maxValue) {
            maxValue = value;
         }
      }

      return this.extendSourcesBelowWorld(maxValue + this.minY);
   }

   private void fill(final int lowestSourceY) {
      int value = lowestSourceY - this.minY;

      for (int i = 0; i < this.heightmap.getSize(); i++) {
         this.heightmap.set(i, value);
      }
   }

   private void set(final int index, final int value) {
      this.heightmap.set(index, value - this.minY);
   }

   private int get(final int index) {
      return this.heightmap.get(index) + this.minY;
   }

   private int extendSourcesBelowWorld(final int value) {
      return value == this.minY ? Integer.MIN_VALUE : value;
   }

   private static int index(final int x, final int z) {
      return x + z * 16;
   }
}
