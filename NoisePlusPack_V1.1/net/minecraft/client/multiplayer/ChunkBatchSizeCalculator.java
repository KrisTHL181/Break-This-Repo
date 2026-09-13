package net.minecraft.client.multiplayer;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChunkBatchSizeCalculator {
   private static final int MAX_OLD_SAMPLES_WEIGHT = 49;
   private static final int CLAMP_COEFFICIENT = 3;
   private double aggregatedNanosPerChunk = 2000000.0;
   private int oldSamplesWeight = 1;
   private volatile long chunkBatchStartTime = Util.getNanos();

   public void onBatchStart() {
      this.chunkBatchStartTime = Util.getNanos();
   }

   public void onBatchFinished(int p_299908_) {
      if (p_299908_ > 0) {
         double d0 = Util.getNanos() - this.chunkBatchStartTime;
         double d1 = d0 / p_299908_;
         double d2 = Mth.clamp(d1, this.aggregatedNanosPerChunk / 3.0, this.aggregatedNanosPerChunk * 3.0);
         this.aggregatedNanosPerChunk = (this.aggregatedNanosPerChunk * this.oldSamplesWeight + d2) / (this.oldSamplesWeight + 1);
         this.oldSamplesWeight = Math.min(49, this.oldSamplesWeight + 1);
      }
   }

   public float getDesiredChunksPerTick() {
      return (float)(7000000.0 / this.aggregatedNanosPerChunk);
   }
}
