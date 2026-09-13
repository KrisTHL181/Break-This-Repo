package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;

public class NbtAccounter {
   public static final int DEFAULT_NBT_QUOTA = 2097152;
   public static final int UNCOMPRESSED_NBT_QUOTA = 104857600;
   private static final int MAX_STACK_DEPTH = 512;
   private final long quota;
   private long usage;
   private final int maxDepth;
   private int depth;

   public NbtAccounter(long p_128922_, int p_301724_) {
      this.quota = p_128922_;
      this.maxDepth = p_301724_;
   }

   public static NbtAccounter create(long p_301706_) {
      return new NbtAccounter(p_301706_, 512);
   }

   public static NbtAccounter defaultQuota() {
      return new NbtAccounter(2097152L, 512);
   }

   public static NbtAccounter uncompressedQuota() {
      return new NbtAccounter(104857600L, 512);
   }

   public static NbtAccounter unlimitedHeap() {
      return new NbtAccounter(Long.MAX_VALUE, 512);
   }

   public void accountBytes(long p_301856_, long p_301857_) {
      this.accountBytes(p_301856_ * p_301857_);
   }

   public void accountBytes(long p_263515_) {
      if (p_263515_ < 0L) {
         throw new IllegalArgumentException("Tried to account NBT tag with negative size: " + p_263515_);
      }

      if (this.usage + p_263515_ > this.quota) {
         throw new NbtAccounterException(
            "Tried to read NBT tag that was too big; tried to allocate: " + this.usage + " + " + p_263515_ + " bytes where max allowed: " + this.quota
         );
      }

      this.usage += p_263515_;
   }

   public void pushDepth() {
      if (this.depth >= this.maxDepth) {
         throw new NbtAccounterException("Tried to read NBT tag with too high complexity, depth > " + this.maxDepth);
      }

      this.depth++;
   }

   public void popDepth() {
      if (this.depth <= 0) {
         throw new NbtAccounterException("NBT-Accounter tried to pop stack-depth at top-level");
      }

      this.depth--;
   }

   @VisibleForTesting
   public long getUsage() {
      return this.usage;
   }

   @VisibleForTesting
   public int getDepth() {
      return this.depth;
   }
}
