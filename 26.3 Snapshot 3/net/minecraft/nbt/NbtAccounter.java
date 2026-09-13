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

   public NbtAccounter(final long quota, final int maxDepth) {
      this.quota = quota;
      this.maxDepth = maxDepth;
   }

   public static NbtAccounter create(final long quota) {
      return new NbtAccounter(quota, 512);
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

   public void accountBytes(final long bytesPerEntry, final long count) {
      this.accountBytes(bytesPerEntry * count);
   }

   public void accountBytes(final long size) {
      if (size < 0L) {
         throw new IllegalArgumentException("Tried to account NBT tag with negative size: " + size);
      }

      if (this.usage + size > this.quota) {
         throw new NbtAccounterException(
            "Tried to read NBT tag that was too big; tried to allocate: " + this.usage + " + " + size + " bytes where max allowed: " + this.quota
         );
      }

      this.usage += size;
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
