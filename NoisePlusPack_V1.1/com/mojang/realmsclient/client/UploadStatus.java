package com.mojang.realmsclient.client;

import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UploadStatus {
   private volatile long bytesWritten;
   private volatile long totalBytes;
   private long previousTimeSnapshot = Util.getMillis();
   private long previousBytesWritten;
   private long bytesPerSecond;

   public void setTotalBytes(long p_363757_) {
      this.totalBytes = p_363757_;
   }

   public void restart() {
      this.bytesWritten = 0L;
      this.previousTimeSnapshot = Util.getMillis();
      this.previousBytesWritten = 0L;
      this.bytesPerSecond = 0L;
   }

   public long getTotalBytes() {
      return this.totalBytes;
   }

   public long getBytesWritten() {
      return this.bytesWritten;
   }

   public void onWrite(long p_368608_) {
      this.bytesWritten = p_368608_;
   }

   public boolean uploadStarted() {
      return this.bytesWritten > 0L;
   }

   public boolean uploadCompleted() {
      return this.bytesWritten >= this.totalBytes;
   }

   public double getPercentage() {
      return Math.min((double)this.getBytesWritten() / this.getTotalBytes(), 1.0);
   }

   public void refreshBytesPerSecond() {
      long i = Util.getMillis();
      long j = i - this.previousTimeSnapshot;
      if (j >= 1000L) {
         long k = this.bytesWritten;
         this.bytesPerSecond = 1000L * (k - this.previousBytesWritten) / j;
         this.previousBytesWritten = k;
         this.previousTimeSnapshot = i;
      }
   }

   public long getBytesPerSecond() {
      return this.bytesPerSecond;
   }
}
