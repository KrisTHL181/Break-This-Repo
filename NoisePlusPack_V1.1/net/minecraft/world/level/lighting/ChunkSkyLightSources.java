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

   public ChunkSkyLightSources(LevelHeightAccessor p_285502_) {
      this.minY = p_285502_.getMinY() - 1;
      int i = p_285502_.getMaxY() + 1;
      int j = Mth.ceillog2(i - this.minY + 1);
      this.heightmap = new SimpleBitStorage(j, 256);
   }

   public void fillFrom(ChunkAccess p_285152_) {
      int i = p_285152_.getHighestFilledSectionIndex();
      if (i == -1) {
         this.fill(this.minY);
      } else {
         for (int j = 0; j < 16; j++) {
            for (int k = 0; k < 16; k++) {
               int l = Math.max(this.findLowestSourceY(p_285152_, i, k, j), this.minY);
               this.set(index(k, j), l);
            }
         }
      }
   }

   private int findLowestSourceY(ChunkAccess p_285214_, int p_285171_, int p_285021_, int p_285226_) {
      int i = SectionPos.sectionToBlockCoord(p_285214_.getSectionYFromSectionIndex(p_285171_) + 1);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = this.mutablePos1.set(p_285021_, i, p_285226_);
      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = this.mutablePos2.setWithOffset(blockpos$mutableblockpos, Direction.DOWN);
      BlockState blockstate = Blocks.AIR.defaultBlockState();

      for (int j = p_285171_; j >= 0; j--) {
         LevelChunkSection levelchunksection = p_285214_.getSection(j);
         if (levelchunksection.hasOnlyAir()) {
            blockstate = Blocks.AIR.defaultBlockState();
            int l = p_285214_.getSectionYFromSectionIndex(j);
            blockpos$mutableblockpos.setY(SectionPos.sectionToBlockCoord(l));
            blockpos$mutableblockpos1.setY(blockpos$mutableblockpos.getY() - 1);
         } else {
            for (int k = 15; k >= 0; k--) {
               BlockState blockstate1 = levelchunksection.getBlockState(p_285021_, k, p_285226_);
               if (isEdgeOccluded(blockstate, blockstate1)) {
                  return blockpos$mutableblockpos.getY();
               }

               blockstate = blockstate1;
               blockpos$mutableblockpos.set(blockpos$mutableblockpos1);
               blockpos$mutableblockpos1.move(Direction.DOWN);
            }
         }
      }

      return this.minY;
   }

   public boolean update(BlockGetter p_285514_, int p_284999_, int p_285358_, int p_284944_) {
      int i = p_285358_ + 1;
      int j = index(p_284999_, p_284944_);
      int k = this.get(j);
      if (i < k) {
         return false;
      }

      BlockPos blockpos = this.mutablePos1.set(p_284999_, p_285358_ + 1, p_284944_);
      BlockState blockstate = p_285514_.getBlockState(blockpos);
      BlockPos blockpos1 = this.mutablePos2.set(p_284999_, p_285358_, p_284944_);
      BlockState blockstate1 = p_285514_.getBlockState(blockpos1);
      if (this.updateEdge(p_285514_, j, k, blockpos, blockstate, blockpos1, blockstate1)) {
         return true;
      }

      BlockPos blockpos2 = this.mutablePos1.set(p_284999_, p_285358_ - 1, p_284944_);
      BlockState blockstate2 = p_285514_.getBlockState(blockpos2);
      return this.updateEdge(p_285514_, j, k, blockpos1, blockstate1, blockpos2, blockstate2);
   }

   private boolean updateEdge(
      BlockGetter p_285066_, int p_285184_, int p_285101_, BlockPos p_285446_, BlockState p_285185_, BlockPos p_285103_, BlockState p_285009_
   ) {
      int i = p_285446_.getY();
      if (isEdgeOccluded(p_285185_, p_285009_)) {
         if (i > p_285101_) {
            this.set(p_285184_, i);
            return true;
         }
      } else if (i == p_285101_) {
         this.set(p_285184_, this.findLowestSourceBelow(p_285066_, p_285103_, p_285009_));
         return true;
      }

      return false;
   }

   private int findLowestSourceBelow(BlockGetter p_285279_, BlockPos p_285119_, BlockState p_285096_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = this.mutablePos1.set(p_285119_);
      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = this.mutablePos2.setWithOffset(p_285119_, Direction.DOWN);
      BlockState blockstate = p_285096_;

      while (blockpos$mutableblockpos1.getY() >= this.minY) {
         BlockState blockstate1 = p_285279_.getBlockState(blockpos$mutableblockpos1);
         if (isEdgeOccluded(blockstate, blockstate1)) {
            return blockpos$mutableblockpos.getY();
         }

         blockstate = blockstate1;
         blockpos$mutableblockpos.set(blockpos$mutableblockpos1);
         blockpos$mutableblockpos1.move(Direction.DOWN);
      }

      return this.minY;
   }

   private static boolean isEdgeOccluded(BlockState p_285219_, BlockState p_285512_) {
      if (p_285512_.getLightBlock() != 0) {
         return true;
      }

      VoxelShape voxelshape = LightEngine.getOcclusionShape(p_285219_, Direction.DOWN);
      VoxelShape voxelshape1 = LightEngine.getOcclusionShape(p_285512_, Direction.UP);
      return Shapes.faceShapeOccludes(voxelshape, voxelshape1);
   }

   public int getLowestSourceY(int p_285247_, int p_285082_) {
      int i = this.get(index(p_285247_, p_285082_));
      return this.extendSourcesBelowWorld(i);
   }

   public int getHighestLowestSourceY() {
      int i = Integer.MIN_VALUE;

      for (int j = 0; j < this.heightmap.getSize(); j++) {
         int k = this.heightmap.get(j);
         if (k > i) {
            i = k;
         }
      }

      return this.extendSourcesBelowWorld(i + this.minY);
   }

   private void fill(int p_285311_) {
      int i = p_285311_ - this.minY;

      for (int j = 0; j < this.heightmap.getSize(); j++) {
         this.heightmap.set(j, i);
      }
   }

   private void set(int p_285323_, int p_285220_) {
      this.heightmap.set(p_285323_, p_285220_ - this.minY);
   }

   private int get(int p_284951_) {
      return this.heightmap.get(p_284951_) + this.minY;
   }

   private int extendSourcesBelowWorld(int p_284953_) {
      return p_284953_ == this.minY ? Integer.MIN_VALUE : p_284953_;
   }

   private static int index(int p_284980_, int p_285277_) {
      return p_284980_ + p_285277_ * 16;
   }
}
