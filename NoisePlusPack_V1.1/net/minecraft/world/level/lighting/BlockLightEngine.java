package net.minecraft.world.level.lighting;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;

public final class BlockLightEngine extends LightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {
   private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

   public BlockLightEngine(LightChunkGetter p_75492_) {
      this(p_75492_, new BlockLightSectionStorage(p_75492_));
   }

   @VisibleForTesting
   public BlockLightEngine(LightChunkGetter p_278252_, BlockLightSectionStorage p_278255_) {
      super(p_278252_, p_278255_);
   }

   @Override
   protected void checkNode(long p_285169_) {
      long i = SectionPos.blockToSection(p_285169_);
      if (this.storage.storingLightForSection(i)) {
         BlockState blockstate = this.getState(this.mutablePos.set(p_285169_));
         int j = this.getEmission(p_285169_, blockstate);
         int k = this.storage.getStoredLevel(p_285169_);
         if (j < k) {
            this.storage.setStoredLevel(p_285169_, 0);
            this.enqueueDecrease(p_285169_, LightEngine.QueueEntry.decreaseAllDirections(k));
         } else {
            this.enqueueDecrease(p_285169_, PULL_LIGHT_IN_ENTRY);
         }

         if (j > 0) {
            this.enqueueIncrease(p_285169_, LightEngine.QueueEntry.increaseLightFromEmission(j, isEmptyShape(blockstate)));
         }
      }
   }

   @Override
   protected void propagateIncrease(long p_285500_, long p_285410_, int p_285492_) {
      BlockState blockstate = null;

      for (Direction direction : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(p_285410_, direction)) {
            long i = BlockPos.offset(p_285500_, direction);
            if (this.storage.storingLightForSection(SectionPos.blockToSection(i))) {
               int j = this.storage.getStoredLevel(i);
               int k = p_285492_ - 1;
               if (k > j) {
                  this.mutablePos.set(i);
                  BlockState blockstate1 = this.getState(this.mutablePos);
                  int l = p_285492_ - this.getOpacity(blockstate1);
                  if (l > j) {
                     if (blockstate == null) {
                        blockstate = LightEngine.QueueEntry.isFromEmptyShape(p_285410_)
                           ? Blocks.AIR.defaultBlockState()
                           : this.getState(this.mutablePos.set(p_285500_));
                     }

                     if (!this.shapeOccludes(blockstate, blockstate1, direction)) {
                        this.storage.setStoredLevel(i, l);
                        if (l > 1) {
                           this.enqueueIncrease(i, LightEngine.QueueEntry.increaseSkipOneDirection(l, isEmptyShape(blockstate1), direction.getOpposite()));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void propagateDecrease(long p_285435_, long p_285230_) {
      int i = LightEngine.QueueEntry.getFromLevel(p_285230_);

      for (Direction direction : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(p_285230_, direction)) {
            long j = BlockPos.offset(p_285435_, direction);
            if (this.storage.storingLightForSection(SectionPos.blockToSection(j))) {
               int k = this.storage.getStoredLevel(j);
               if (k != 0) {
                  if (k <= i - 1) {
                     BlockState blockstate = this.getState(this.mutablePos.set(j));
                     int l = this.getEmission(j, blockstate);
                     this.storage.setStoredLevel(j, 0);
                     if (l < k) {
                        this.enqueueDecrease(j, LightEngine.QueueEntry.decreaseSkipOneDirection(k, direction.getOpposite()));
                     }

                     if (l > 0) {
                        this.enqueueIncrease(j, LightEngine.QueueEntry.increaseLightFromEmission(l, isEmptyShape(blockstate)));
                     }
                  } else {
                     this.enqueueIncrease(j, LightEngine.QueueEntry.increaseOnlyOneDirection(k, false, direction.getOpposite()));
                  }
               }
            }
         }
      }
   }

   private int getEmission(long p_285243_, BlockState p_284973_) {
      int i = p_284973_.getLightEmission();
      return i > 0 && this.storage.lightOnInSection(SectionPos.blockToSection(p_285243_)) ? i : 0;
   }

   @Override
   public void propagateLightSources(ChunkPos p_285274_) {
      this.setLightEnabled(p_285274_, true);
      LightChunk lightchunk = this.chunkSource.getChunkForLighting(p_285274_.x, p_285274_.z);
      if (lightchunk != null) {
         lightchunk.findBlockLightSources((p_360638_, p_360639_) -> {
            int i = p_360639_.getLightEmission();
            this.enqueueIncrease(p_360638_.asLong(), LightEngine.QueueEntry.increaseLightFromEmission(i, isEmptyShape(p_360639_)));
         });
      }
   }
}
