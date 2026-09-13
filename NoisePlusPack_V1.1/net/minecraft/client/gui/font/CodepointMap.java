package net.minecraft.client.gui.font;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.function.IntFunction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CodepointMap<T> {
   private static final int BLOCK_BITS = 8;
   private static final int BLOCK_SIZE = 256;
   private static final int IN_BLOCK_MASK = 255;
   private static final int MAX_BLOCK = 4351;
   private static final int BLOCK_COUNT = 4352;
   private final T[] empty;
   private final @Nullable T[][] blockMap;
   private final IntFunction<T[]> blockConstructor;

   public CodepointMap(IntFunction<T[]> p_285284_, IntFunction<T[][]> p_285275_) {
      this.empty = (T[])((Object[])p_285284_.apply(256));
      this.blockMap = (T[][])((Object[][])p_285275_.apply(4352));
      Arrays.fill(this.blockMap, this.empty);
      this.blockConstructor = p_285284_;
   }

   public void clear() {
      Arrays.fill(this.blockMap, this.empty);
   }

   public @Nullable T get(int p_285131_) {
      int i = p_285131_ >> 8;
      int j = p_285131_ & 0xFF;
      return this.blockMap[i][j];
   }

   public @Nullable T put(int p_285321_, T p_285073_) {
      int i = p_285321_ >> 8;
      int j = p_285321_ & 0xFF;
      T[] at = this.blockMap[i];
      if (at == this.empty) {
         at = (T[])((Object[])this.blockConstructor.apply(256));
         this.blockMap[i] = at;
         at[j] = p_285073_;
         return null;
      } else {
         T t = at[j];
         at[j] = p_285073_;
         return t;
      }
   }

   public T computeIfAbsent(int p_285365_, IntFunction<T> p_285147_) {
      int i = p_285365_ >> 8;
      int j = p_285365_ & 0xFF;
      T[] at = this.blockMap[i];
      T t = at[j];
      if (t != null) {
         return t;
      }

      if (at == this.empty) {
         at = (T[])((Object[])this.blockConstructor.apply(256));
         this.blockMap[i] = at;
      }

      T t1 = p_285147_.apply(p_285365_);
      at[j] = t1;
      return t1;
   }

   public @Nullable T remove(int p_285488_) {
      int i = p_285488_ >> 8;
      int j = p_285488_ & 0xFF;
      T[] at = this.blockMap[i];
      if (at == this.empty) {
         return null;
      }

      T t = at[j];
      at[j] = null;
      return t;
   }

   public void forEach(CodepointMap.Output<T> p_285048_) {
      for (int i = 0; i < this.blockMap.length; i++) {
         T[] at = this.blockMap[i];
         if (at != this.empty) {
            for (int j = 0; j < at.length; j++) {
               T t = at[j];
               if (t != null) {
                  int k = i << 8 | j;
                  p_285048_.accept(k, t);
               }
            }
         }
      }
   }

   public IntSet keySet() {
      IntOpenHashSet intopenhashset = new IntOpenHashSet();
      this.forEach((p_285165_, p_285389_) -> intopenhashset.add(p_285165_));
      return intopenhashset;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface Output<T> {
      void accept(int var1, T var2);
   }
}
