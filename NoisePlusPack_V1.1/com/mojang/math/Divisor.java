package com.mojang.math;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.util.NoSuchElementException;

public class Divisor implements IntIterator {
   private final int denominator;
   private final int quotient;
   private final int mod;
   private int returnedParts;
   private int remainder;

   public Divisor(int p_254018_, int p_254504_) {
      this.denominator = p_254504_;
      if (p_254504_ > 0) {
         this.quotient = p_254018_ / p_254504_;
         this.mod = p_254018_ % p_254504_;
      } else {
         this.quotient = 0;
         this.mod = 0;
      }
   }

   public boolean hasNext() {
      return this.returnedParts < this.denominator;
   }

   public int nextInt() {
      if (!this.hasNext()) {
         throw new NoSuchElementException();
      }

      int i = this.quotient;
      this.remainder = this.remainder + this.mod;
      if (this.remainder >= this.denominator) {
         this.remainder = this.remainder - this.denominator;
         i++;
      }

      this.returnedParts++;
      return i;
   }

   @VisibleForTesting
   public static Iterable<Integer> asIterable(int p_254381_, int p_254129_) {
      return () -> new Divisor(p_254381_, p_254129_);
   }
}
