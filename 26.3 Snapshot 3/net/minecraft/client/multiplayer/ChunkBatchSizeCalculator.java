package net.minecraft.client.multiplayer;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class ChunkBatchSizeCalculator {
   private static final int MAX_OLD_SAMPLES_WEIGHT = 49;
   private static final int CLAMP_COEFFICIENT = 3;
   private double aggregatedNanosPerChunk = 2000000.0;
   private int oldSamplesWeight = 1;
   private volatile long chunkBatchStartTime = Util.getNanos();

   public void onBatchStart() {
      this.chunkBatchStartTime = Util.getNanos();
   }

   public void onBatchFinished(final int batchSize) {
      if (batchSize > 0) {
         double batchDuration = Util.getNanos() - this.chunkBatchStartTime;
         double nanosPerChunk = batchDuration / batchSize;
         double clampedNanosPerChunk = Mth.clamp(nanosPerChunk, this.aggregatedNanosPerChunk / 3.0, this.aggregatedNanosPerChunk * 3.0);
         this.aggregatedNanosPerChunk = (this.aggregatedNanosPerChunk * this.oldSamplesWeight + clampedNanosPerChunk) / (this.oldSamplesWeight + 1);
         this.oldSamplesWeight = Math.min(49, this.oldSamplesWeight + 1);
      }
   }

   public float getDesiredChunksPerTick() {
      return (float)(7000000.0 / this.aggregatedNanosPerChunk);
   }
}
