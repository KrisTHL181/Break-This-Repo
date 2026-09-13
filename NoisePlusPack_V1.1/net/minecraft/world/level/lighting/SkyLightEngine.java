package net.minecraft.world.level.lighting;

import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.jspecify.annotations.Nullable;

public final class SkyLightEngine extends LightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, SkyLightSectionStorage> {
   private static final long REMOVE_TOP_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.decreaseAllDirections(15);
   private static final long REMOVE_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.decreaseSkipOneDirection(15, Direction.UP);
   private static final long ADD_SKY_SOURCE_ENTRY = LightEngine.QueueEntry.increaseSkipOneDirection(15, false, Direction.UP);
   private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
   private final ChunkSkyLightSources emptyChunkSources;

   public SkyLightEngine(LightChunkGetter p_75843_) {
      this(p_75843_, new SkyLightSectionStorage(p_75843_));
   }

   @VisibleForTesting
   protected SkyLightEngine(LightChunkGetter p_282215_, SkyLightSectionStorage p_282341_) {
      super(p_282215_, p_282341_);
      this.emptyChunkSources = new ChunkSkyLightSources(p_282215_.getLevel());
   }

   private static boolean isSourceLevel(int p_285004_) {
      return p_285004_ == 15;
   }

   private int getLowestSourceY(int p_285058_, int p_285191_, int p_285111_) {
      ChunkSkyLightSources chunkskylightsources = this.getChunkSources(SectionPos.blockToSectionCoord(p_285058_), SectionPos.blockToSectionCoord(p_285191_));
      return chunkskylightsources == null
         ? p_285111_
         : chunkskylightsources.getLowestSourceY(SectionPos.sectionRelative(p_285058_), SectionPos.sectionRelative(p_285191_));
   }

   private @Nullable ChunkSkyLightSources getChunkSources(int p_285270_, int p_285307_) {
      LightChunk lightchunk = this.chunkSource.getChunkForLighting(p_285270_, p_285307_);
      return lightchunk != null ? lightchunk.getSkyLightSources() : null;
   }

   @Override
   protected void checkNode(long p_75859_) {
      int i = BlockPos.getX(p_75859_);
      int j = BlockPos.getY(p_75859_);
      int k = BlockPos.getZ(p_75859_);
      long l = SectionPos.blockToSection(p_75859_);
      int i1 = this.storage.lightOnInSection(l) ? this.getLowestSourceY(i, k, Integer.MAX_VALUE) : Integer.MAX_VALUE;
      if (i1 != Integer.MAX_VALUE) {
         this.updateSourcesInColumn(i, k, i1);
      }

      if (this.storage.storingLightForSection(l)) {
         boolean flag = j >= i1;
         if (flag) {
            this.enqueueDecrease(p_75859_, REMOVE_SKY_SOURCE_ENTRY);
            this.enqueueIncrease(p_75859_, ADD_SKY_SOURCE_ENTRY);
         } else {
            int j1 = this.storage.getStoredLevel(p_75859_);
            if (j1 > 0) {
               this.storage.setStoredLevel(p_75859_, 0);
               this.enqueueDecrease(p_75859_, LightEngine.QueueEntry.decreaseAllDirections(j1));
            } else {
               this.enqueueDecrease(p_75859_, PULL_LIGHT_IN_ENTRY);
            }
         }
      }
   }

   private void updateSourcesInColumn(int p_285053_, int p_285140_, int p_285337_) {
      int i = SectionPos.sectionToBlockCoord(this.storage.getBottomSectionY());
      this.removeSourcesBelow(p_285053_, p_285140_, p_285337_, i);
      this.addSourcesAbove(p_285053_, p_285140_, p_285337_, i);
   }

   private void removeSourcesBelow(int p_285475_, int p_285138_, int p_285130_, int p_285112_) {
      if (p_285130_ > p_285112_) {
         int i = SectionPos.blockToSectionCoord(p_285475_);
         int j = SectionPos.blockToSectionCoord(p_285138_);
         int k = p_285130_ - 1;

         for (int l = SectionPos.blockToSectionCoord(k); this.storage.hasLightDataAtOrBelow(l); l--) {
            if (this.storage.storingLightForSection(SectionPos.asLong(i, l, j))) {
               int i1 = SectionPos.sectionToBlockCoord(l);
               int j1 = i1 + 15;

               for (int k1 = Math.min(j1, k); k1 >= i1; k1--) {
                  long l1 = BlockPos.asLong(p_285475_, k1, p_285138_);
                  if (!isSourceLevel(this.storage.getStoredLevel(l1))) {
                     return;
                  }

                  this.storage.setStoredLevel(l1, 0);
                  this.enqueueDecrease(l1, k1 == p_285130_ - 1 ? REMOVE_TOP_SKY_SOURCE_ENTRY : REMOVE_SKY_SOURCE_ENTRY);
               }
            }
         }
      }
   }

   private void addSourcesAbove(int p_285241_, int p_285212_, int p_284972_, int p_285134_) {
      int i = SectionPos.blockToSectionCoord(p_285241_);
      int j = SectionPos.blockToSectionCoord(p_285212_);
      int k = Math.max(
         Math.max(this.getLowestSourceY(p_285241_ - 1, p_285212_, Integer.MIN_VALUE), this.getLowestSourceY(p_285241_ + 1, p_285212_, Integer.MIN_VALUE)),
         Math.max(this.getLowestSourceY(p_285241_, p_285212_ - 1, Integer.MIN_VALUE), this.getLowestSourceY(p_285241_, p_285212_ + 1, Integer.MIN_VALUE))
      );
      int l = Math.max(p_284972_, p_285134_);

      for (long i1 = SectionPos.asLong(i, SectionPos.blockToSectionCoord(l), j); !this.storage.isAboveData(i1); i1 = SectionPos.offset(i1, Direction.UP)) {
         if (this.storage.storingLightForSection(i1)) {
            int j1 = SectionPos.sectionToBlockCoord(SectionPos.y(i1));
            int k1 = j1 + 15;

            for (int l1 = Math.max(j1, l); l1 <= k1; l1++) {
               long i2 = BlockPos.asLong(p_285241_, l1, p_285212_);
               if (isSourceLevel(this.storage.getStoredLevel(i2))) {
                  return;
               }

               this.storage.setStoredLevel(i2, 15);
               if (l1 < k || l1 == p_284972_) {
                  this.enqueueIncrease(i2, ADD_SKY_SOURCE_ENTRY);
               }
            }
         }
      }
   }

   @Override
   protected void propagateIncrease(long p_285341_, long p_285204_, int p_285003_) {
      BlockState blockstate = null;
      int i = this.countEmptySectionsBelowIfAtBorder(p_285341_);

      for (Direction direction : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(p_285204_, direction)) {
            long j = BlockPos.offset(p_285341_, direction);
            if (this.storage.storingLightForSection(SectionPos.blockToSection(j))) {
               int k = this.storage.getStoredLevel(j);
               int l = p_285003_ - 1;
               if (l > k) {
                  this.mutablePos.set(j);
                  BlockState blockstate1 = this.getState(this.mutablePos);
                  int i1 = p_285003_ - this.getOpacity(blockstate1);
                  if (i1 > k) {
                     if (blockstate == null) {
                        blockstate = LightEngine.QueueEntry.isFromEmptyShape(p_285204_)
                           ? Blocks.AIR.defaultBlockState()
                           : this.getState(this.mutablePos.set(p_285341_));
                     }

                     if (!this.shapeOccludes(blockstate, blockstate1, direction)) {
                        this.storage.setStoredLevel(j, i1);
                        if (i1 > 1) {
                           this.enqueueIncrease(j, LightEngine.QueueEntry.increaseSkipOneDirection(i1, isEmptyShape(blockstate1), direction.getOpposite()));
                        }

                        this.propagateFromEmptySections(j, direction, i1, true, i);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected void propagateDecrease(long p_285015_, long p_285395_) {
      int i = this.countEmptySectionsBelowIfAtBorder(p_285015_);
      int j = LightEngine.QueueEntry.getFromLevel(p_285395_);

      for (Direction direction : PROPAGATION_DIRECTIONS) {
         if (LightEngine.QueueEntry.shouldPropagateInDirection(p_285395_, direction)) {
            long k = BlockPos.offset(p_285015_, direction);
            if (this.storage.storingLightForSection(SectionPos.blockToSection(k))) {
               int l = this.storage.getStoredLevel(k);
               if (l != 0) {
                  if (l <= j - 1) {
                     this.storage.setStoredLevel(k, 0);
                     this.enqueueDecrease(k, LightEngine.QueueEntry.decreaseSkipOneDirection(l, direction.getOpposite()));
                     this.propagateFromEmptySections(k, direction, l, false, i);
                  } else {
                     this.enqueueIncrease(k, LightEngine.QueueEntry.increaseOnlyOneDirection(l, false, direction.getOpposite()));
                  }
               }
            }
         }
      }
   }

   private int countEmptySectionsBelowIfAtBorder(long p_285356_) {
      int i = BlockPos.getY(p_285356_);
      int j = SectionPos.sectionRelative(i);
      if (j != 0) {
         return 0;
      }

      int k = BlockPos.getX(p_285356_);
      int l = BlockPos.getZ(p_285356_);
      int i1 = SectionPos.sectionRelative(k);
      int j1 = SectionPos.sectionRelative(l);
      if (i1 != 0 && i1 != 15 && j1 != 0 && j1 != 15) {
         return 0;
      }

      int k1 = SectionPos.blockToSectionCoord(k);
      int l1 = SectionPos.blockToSectionCoord(i);
      int i2 = SectionPos.blockToSectionCoord(l);
      int j2 = 0;

      while (!this.storage.storingLightForSection(SectionPos.asLong(k1, l1 - j2 - 1, i2)) && this.storage.hasLightDataAtOrBelow(l1 - j2 - 1)) {
         j2++;
      }

      return j2;
   }

   private void propagateFromEmptySections(long p_284965_, Direction p_285308_, int p_284977_, boolean p_285001_, int p_285052_) {
      if (p_285052_ != 0) {
         int i = BlockPos.getX(p_284965_);
         int j = BlockPos.getZ(p_284965_);
         if (crossedSectionEdge(p_285308_, SectionPos.sectionRelative(i), SectionPos.sectionRelative(j))) {
            int k = BlockPos.getY(p_284965_);
            int l = SectionPos.blockToSectionCoord(i);
            int i1 = SectionPos.blockToSectionCoord(j);
            int j1 = SectionPos.blockToSectionCoord(k) - 1;
            int k1 = j1 - p_285052_ + 1;

            while (j1 >= k1) {
               if (!this.storage.storingLightForSection(SectionPos.asLong(l, j1, i1))) {
                  j1--;
               } else {
                  int l1 = SectionPos.sectionToBlockCoord(j1);

                  for (int i2 = 15; i2 >= 0; i2--) {
                     long j2 = BlockPos.asLong(i, l1 + i2, j);
                     if (p_285001_) {
                        this.storage.setStoredLevel(j2, p_284977_);
                        if (p_284977_ > 1) {
                           this.enqueueIncrease(j2, LightEngine.QueueEntry.increaseSkipOneDirection(p_284977_, true, p_285308_.getOpposite()));
                        }
                     } else {
                        this.storage.setStoredLevel(j2, 0);
                        this.enqueueDecrease(j2, LightEngine.QueueEntry.decreaseSkipOneDirection(p_284977_, p_285308_.getOpposite()));
                     }
                  }

                  j1--;
               }
            }
         }
      }
   }

   private static boolean crossedSectionEdge(Direction p_285014_, int p_284991_, int p_285468_) {
      return switch (p_285014_) {
         case NORTH -> p_285468_ == 15;
         case SOUTH -> p_285468_ == 0;
         case WEST -> p_284991_ == 15;
         case EAST -> p_284991_ == 0;
         default -> false;
      };
   }

   @Override
   public void setLightEnabled(ChunkPos p_285459_, boolean p_285013_) {
      super.setLightEnabled(p_285459_, p_285013_);
      if (p_285013_) {
         ChunkSkyLightSources chunkskylightsources = Objects.requireNonNullElse(this.getChunkSources(p_285459_.x, p_285459_.z), this.emptyChunkSources);
         int i = chunkskylightsources.getHighestLowestSourceY() - 1;
         int j = SectionPos.blockToSectionCoord(i) + 1;
         long k = SectionPos.getZeroNode(p_285459_.x, p_285459_.z);
         int l = this.storage.getTopSectionY(k);
         int i1 = Math.max(this.storage.getBottomSectionY(), j);

         for (int j1 = l - 1; j1 >= i1; j1--) {
            DataLayer datalayer = this.storage.getDataLayerToWrite(SectionPos.asLong(p_285459_.x, j1, p_285459_.z));
            if (datalayer != null && datalayer.isEmpty()) {
               datalayer.fill(15);
            }
         }
      }
   }

   @Override
   public void propagateLightSources(ChunkPos p_285333_) {
      long i = SectionPos.getZeroNode(p_285333_.x, p_285333_.z);
      this.storage.setLightEnabled(i, true);
      ChunkSkyLightSources chunkskylightsources = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x, p_285333_.z), this.emptyChunkSources);
      ChunkSkyLightSources chunkskylightsources1 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x, p_285333_.z - 1), this.emptyChunkSources);
      ChunkSkyLightSources chunkskylightsources2 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x, p_285333_.z + 1), this.emptyChunkSources);
      ChunkSkyLightSources chunkskylightsources3 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x - 1, p_285333_.z), this.emptyChunkSources);
      ChunkSkyLightSources chunkskylightsources4 = Objects.requireNonNullElse(this.getChunkSources(p_285333_.x + 1, p_285333_.z), this.emptyChunkSources);
      int j = this.storage.getTopSectionY(i);
      int k = this.storage.getBottomSectionY();
      int l = SectionPos.sectionToBlockCoord(p_285333_.x);
      int i1 = SectionPos.sectionToBlockCoord(p_285333_.z);

      for (int j1 = j - 1; j1 >= k; j1--) {
         long k1 = SectionPos.asLong(p_285333_.x, j1, p_285333_.z);
         DataLayer datalayer = this.storage.getDataLayerToWrite(k1);
         if (datalayer != null) {
            int l1 = SectionPos.sectionToBlockCoord(j1);
            int i2 = l1 + 15;
            boolean flag = false;

            for (int j2 = 0; j2 < 16; j2++) {
               for (int k2 = 0; k2 < 16; k2++) {
                  int l2 = chunkskylightsources.getLowestSourceY(k2, j2);
                  if (l2 <= i2) {
                     int i3 = j2 == 0 ? chunkskylightsources1.getLowestSourceY(k2, 15) : chunkskylightsources.getLowestSourceY(k2, j2 - 1);
                     int j3 = j2 == 15 ? chunkskylightsources2.getLowestSourceY(k2, 0) : chunkskylightsources.getLowestSourceY(k2, j2 + 1);
                     int k3 = k2 == 0 ? chunkskylightsources3.getLowestSourceY(15, j2) : chunkskylightsources.getLowestSourceY(k2 - 1, j2);
                     int l3 = k2 == 15 ? chunkskylightsources4.getLowestSourceY(0, j2) : chunkskylightsources.getLowestSourceY(k2 + 1, j2);
                     int i4 = Math.max(Math.max(i3, j3), Math.max(k3, l3));

                     for (int j4 = i2; j4 >= Math.max(l1, l2); j4--) {
                        datalayer.set(k2, SectionPos.sectionRelative(j4), j2, 15);
                        if (j4 == l2 || j4 < i4) {
                           long k4 = BlockPos.asLong(l + k2, j4, i1 + j2);
                           this.enqueueIncrease(k4, LightEngine.QueueEntry.increaseSkySourceInDirections(j4 == l2, j4 < i3, j4 < j3, j4 < k3, j4 < l3));
                        }
                     }

                     if (l2 < l1) {
                        flag = true;
                     }
                  }
               }
            }

            if (!flag) {
               break;
            }
         }
      }
   }
}
