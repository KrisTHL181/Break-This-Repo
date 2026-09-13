package net.minecraft.util;

import java.util.Locale;
import java.util.function.Consumer;

public class StaticCache2D<T> {
   private final int minX;
   private final int minZ;
   private final int sizeX;
   private final int sizeZ;
   private final Object[] cache;

   public static <T> StaticCache2D<T> create(int p_344065_, int p_343803_, int p_342635_, StaticCache2D.Initializer<T> p_344385_) {
      int i = p_344065_ - p_342635_;
      int j = p_343803_ - p_342635_;
      int k = 2 * p_342635_ + 1;
      return new StaticCache2D<>(i, j, k, k, p_344385_);
   }

   private StaticCache2D(int p_342073_, int p_344333_, int p_344505_, int p_343577_, StaticCache2D.Initializer<T> p_342393_) {
      this.minX = p_342073_;
      this.minZ = p_344333_;
      this.sizeX = p_344505_;
      this.sizeZ = p_343577_;
      this.cache = new Object[this.sizeX * this.sizeZ];

      for (int i = p_342073_; i < p_342073_ + p_344505_; i++) {
         for (int j = p_344333_; j < p_344333_ + p_343577_; j++) {
            this.cache[this.getIndex(i, j)] = p_342393_.get(i, j);
         }
      }
   }

   public void forEach(Consumer<T> p_342330_) {
      for (Object object : this.cache) {
         p_342330_.accept((T)object);
      }
   }

   public T get(int p_345297_, int p_343942_) {
      if (!this.contains(p_345297_, p_343942_)) {
         throw new IllegalArgumentException("Requested out of range value (" + p_345297_ + "," + p_343942_ + ") from " + this);
      } else {
         return (T)this.cache[this.getIndex(p_345297_, p_343942_)];
      }
   }

   public boolean contains(int p_344805_, int p_345047_) {
      int i = p_344805_ - this.minX;
      int j = p_345047_ - this.minZ;
      return i >= 0 && i < this.sizeX && j >= 0 && j < this.sizeZ;
   }

   @Override
   public String toString() {
      return String.format(Locale.ROOT, "StaticCache2D[%d, %d, %d, %d]", this.minX, this.minZ, this.minX + this.sizeX, this.minZ + this.sizeZ);
   }

   private int getIndex(int p_343524_, int p_345052_) {
      int i = p_343524_ - this.minX;
      int j = p_345052_ - this.minZ;
      return i * this.sizeZ + j;
   }

   @FunctionalInterface
   public interface Initializer<T> {
      T get(int var1, int var2);
   }
}
