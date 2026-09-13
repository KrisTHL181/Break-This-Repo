package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public abstract class LightEngine<M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> implements LayerLightEventListener {
   public static final int MAX_LEVEL = 15;
   protected static final int MIN_OPACITY = 1;
   protected static final long PULL_LIGHT_IN_ENTRY = LightEngine.QueueEntry.decreaseAllDirections(1);
   private static final int MIN_QUEUE_SIZE = 512;
   protected static final Direction[] PROPAGATION_DIRECTIONS = Direction.values();
   protected final LightChunkGetter chunkSource;
   protected final S storage;
   private final LongOpenHashSet blockNodesToCheck = new LongOpenHashSet(512, 0.5F);
   private final LongArrayFIFOQueue decreaseQueue = new LongArrayFIFOQueue();
   private final LongArrayFIFOQueue increaseQueue = new LongArrayFIFOQueue();
   private static final int CACHE_SIZE = 2;
   private final long[] lastChunkPos = new long[2];
   private final LightChunk[] lastChunk = new LightChunk[2];

   protected LightEngine(LightChunkGetter p_285189_, S p_284964_) {
      this.chunkSource = p_285189_;
      this.storage = p_284964_;
      this.clearChunkCache();
   }

   public static boolean hasDifferentLightProperties(BlockState p_285110_, BlockState p_285372_) {
      return p_285372_ == p_285110_
         ? false
         : p_285372_.getLightBlock() != p_285110_.getLightBlock()
            || p_285372_.getLightEmission() != p_285110_.getLightEmission()
            || p_285372_.useShapeForLightOcclusion()
            || p_285110_.useShapeForLightOcclusion();
   }

   public static int getLightBlockInto(BlockState p_285453_, BlockState p_285318_, Direction p_285196_, int p_285248_) {
      boolean flag = isEmptyShape(p_285453_);
      boolean flag1 = isEmptyShape(p_285318_);
      if (flag && flag1) {
         return p_285248_;
      }

      VoxelShape voxelshape = flag ? Shapes.empty() : p_285453_.getOcclusionShape();
      VoxelShape voxelshape1 = flag1 ? Shapes.empty() : p_285318_.getOcclusionShape();
      return Shapes.mergedFaceOccludes(voxelshape, voxelshape1, p_285196_) ? 16 : p_285248_;
   }

   public static VoxelShape getOcclusionShape(BlockState p_285136_, Direction p_285376_) {
      return isEmptyShape(p_285136_) ? Shapes.empty() : p_285136_.getFaceOcclusionShape(p_285376_);
   }

   protected static boolean isEmptyShape(BlockState p_285133_) {
      return !p_285133_.canOcclude() || !p_285133_.useShapeForLightOcclusion();
   }

   protected BlockState getState(BlockPos p_285338_) {
      int i = SectionPos.blockToSectionCoord(p_285338_.getX());
      int j = SectionPos.blockToSectionCoord(p_285338_.getZ());
      LightChunk lightchunk = this.getChunk(i, j);
      return lightchunk == null ? Blocks.BEDROCK.defaultBlockState() : lightchunk.getBlockState(p_285338_);
   }

   protected int getOpacity(BlockState p_285084_) {
      return Math.max(1, p_285084_.getLightBlock());
   }

   protected boolean shapeOccludes(BlockState p_285154_, BlockState p_285155_, Direction p_285327_) {
      VoxelShape voxelshape = getOcclusionShape(p_285154_, p_285327_);
      VoxelShape voxelshape1 = getOcclusionShape(p_285155_, p_285327_.getOpposite());
      return Shapes.faceShapeOccludes(voxelshape, voxelshape1);
   }

   protected @Nullable LightChunk getChunk(int p_284967_, int p_285447_) {
      long i = ChunkPos.asLong(p_284967_, p_285447_);

      for (int j = 0; j < 2; j++) {
         if (i == this.lastChunkPos[j]) {
            return this.lastChunk[j];
         }
      }

      LightChunk lightchunk = this.chunkSource.getChunkForLighting(p_284967_, p_285447_);

      for (int k = 1; k > 0; k--) {
         this.lastChunkPos[k] = this.lastChunkPos[k - 1];
         this.lastChunk[k] = this.lastChunk[k - 1];
      }

      this.lastChunkPos[0] = i;
      this.lastChunk[0] = lightchunk;
      return lightchunk;
   }

   private void clearChunkCache() {
      Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
      Arrays.fill(this.lastChunk, null);
   }

   @Override
   public void checkBlock(BlockPos p_285352_) {
      this.blockNodesToCheck.add(p_285352_.asLong());
   }

   public void queueSectionData(long p_285221_, @Nullable DataLayer p_285427_) {
      this.storage.queueSectionData(p_285221_, p_285427_);
   }

   public void retainData(ChunkPos p_285314_, boolean p_284937_) {
      this.storage.retainData(SectionPos.getZeroNode(p_285314_.x, p_285314_.z), p_284937_);
   }

   @Override
   public void updateSectionStatus(SectionPos p_285167_, boolean p_284934_) {
      this.storage.updateSectionStatus(p_285167_.asLong(), p_284934_);
   }

   @Override
   public void setLightEnabled(ChunkPos p_285116_, boolean p_285522_) {
      this.storage.setLightEnabled(SectionPos.getZeroNode(p_285116_.x, p_285116_.z), p_285522_);
   }

   @Override
   public int runLightUpdates() {
      LongIterator longiterator = this.blockNodesToCheck.iterator();

      while (longiterator.hasNext()) {
         this.checkNode(longiterator.nextLong());
      }

      this.blockNodesToCheck.clear();
      this.blockNodesToCheck.trim(512);
      int i = 0;
      i += this.propagateDecreases();
      i += this.propagateIncreases();
      this.clearChunkCache();
      this.storage.markNewInconsistencies(this);
      this.storage.swapSectionMap();
      return i;
   }

   private int propagateIncreases() {
      int i;
      for (i = 0; !this.increaseQueue.isEmpty(); i++) {
         long j = this.increaseQueue.dequeueLong();
         long k = this.increaseQueue.dequeueLong();
         int l = this.storage.getStoredLevel(j);
         int i1 = LightEngine.QueueEntry.getFromLevel(k);
         if (LightEngine.QueueEntry.isIncreaseFromEmission(k) && l < i1) {
            this.storage.setStoredLevel(j, i1);
            l = i1;
         }

         if (l == i1) {
            this.propagateIncrease(j, k, l);
         }
      }

      return i;
   }

   private int propagateDecreases() {
      int i;
      for (i = 0; !this.decreaseQueue.isEmpty(); i++) {
         long j = this.decreaseQueue.dequeueLong();
         long k = this.decreaseQueue.dequeueLong();
         this.propagateDecrease(j, k);
      }

      return i;
   }

   protected void enqueueDecrease(long p_285228_, long p_285464_) {
      this.decreaseQueue.enqueue(p_285228_);
      this.decreaseQueue.enqueue(p_285464_);
   }

   protected void enqueueIncrease(long p_285223_, long p_285022_) {
      this.increaseQueue.enqueue(p_285223_);
      this.increaseQueue.enqueue(p_285022_);
   }

   @Override
   public boolean hasLightWork() {
      return this.storage.hasInconsistencies() || !this.blockNodesToCheck.isEmpty() || !this.decreaseQueue.isEmpty() || !this.increaseQueue.isEmpty();
   }

   @Override
   public @Nullable DataLayer getDataLayerData(SectionPos p_285093_) {
      return this.storage.getDataLayerData(p_285093_.asLong());
   }

   @Override
   public int getLightValue(BlockPos p_285149_) {
      return this.storage.getLightValue(p_285149_.asLong());
   }

   public String getDebugData(long p_285363_) {
      return this.getDebugSectionType(p_285363_).display();
   }

   public LayerLightSectionStorage.SectionType getDebugSectionType(long p_285320_) {
      return this.storage.getDebugSectionType(p_285320_);
   }

   protected abstract void checkNode(long var1);

   protected abstract void propagateIncrease(long var1, long var3, int var5);

   protected abstract void propagateDecrease(long var1, long var3);

   public static class QueueEntry {
      private static final int FROM_LEVEL_BITS = 4;
      private static final int DIRECTION_BITS = 6;
      private static final long LEVEL_MASK = 15L;
      private static final long DIRECTIONS_MASK = 1008L;
      private static final long FLAG_FROM_EMPTY_SHAPE = 1024L;
      private static final long FLAG_INCREASE_FROM_EMISSION = 2048L;

      public static long decreaseSkipOneDirection(int p_285429_, Direction p_285207_) {
         long i = withoutDirection(1008L, p_285207_);
         return withLevel(i, p_285429_);
      }

      public static long decreaseAllDirections(int p_285144_) {
         return withLevel(1008L, p_285144_);
      }

      public static long increaseLightFromEmission(int p_285199_, boolean p_284986_) {
         long i = 1008L;
         i |= 2048L;
         if (p_284986_) {
            i |= 1024L;
         }

         return withLevel(i, p_285199_);
      }

      public static long increaseSkipOneDirection(int p_285091_, boolean p_285186_, Direction p_285382_) {
         long i = withoutDirection(1008L, p_285382_);
         if (p_285186_) {
            i |= 1024L;
         }

         return withLevel(i, p_285091_);
      }

      public static long increaseOnlyOneDirection(int p_285025_, boolean p_285384_, Direction p_285072_) {
         long i = 0L;
         if (p_285384_) {
            i |= 1024L;
         }

         i = withDirection(i, p_285072_);
         return withLevel(i, p_285025_);
      }

      public static long increaseSkySourceInDirections(boolean p_285487_, boolean p_285390_, boolean p_285476_, boolean p_285505_, boolean p_285127_) {
         long i = withLevel(0L, 15);
         if (p_285487_) {
            i = withDirection(i, Direction.DOWN);
         }

         if (p_285390_) {
            i = withDirection(i, Direction.NORTH);
         }

         if (p_285476_) {
            i = withDirection(i, Direction.SOUTH);
         }

         if (p_285505_) {
            i = withDirection(i, Direction.WEST);
         }

         if (p_285127_) {
            i = withDirection(i, Direction.EAST);
         }

         return i;
      }

      public static int getFromLevel(long p_285483_) {
         return (int)(p_285483_ & 15L);
      }

      public static boolean isFromEmptyShape(long p_285436_) {
         return (p_285436_ & 1024L) != 0L;
      }

      public static boolean isIncreaseFromEmission(long p_285348_) {
         return (p_285348_ & 2048L) != 0L;
      }

      public static boolean shouldPropagateInDirection(long p_285347_, Direction p_285291_) {
         return (p_285347_ & 1L << p_285291_.ordinal() + 4) != 0L;
      }

      private static long withLevel(long p_285234_, int p_285042_) {
         return p_285234_ & -16L | p_285042_ & 15L;
      }

      private static long withDirection(long p_285295_, Direction p_285016_) {
         return p_285295_ | 1L << p_285016_.ordinal() + 4;
      }

      private static long withoutDirection(long p_285366_, Direction p_285489_) {
         return p_285366_ & ~(1L << p_285489_.ordinal() + 4);
      }
   }
}
