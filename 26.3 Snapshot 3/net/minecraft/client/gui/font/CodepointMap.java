package net.minecraft.client.gui.font;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;

public class CodepointMap<T> {
   private static final int BLOCK_BITS = 8;
   private static final int BLOCK_SIZE = 256;
   private static final int IN_BLOCK_MASK = 255;
   private static final int MAX_BLOCK = 4351;
   private static final int BLOCK_COUNT = 4352;
   private final T[] empty;
   private final @Nullable T[][] blockMap;
   private final IntFunction<T[]> blockConstructor;

   public CodepointMap(final IntFunction<T[]> blockConstructor, final IntFunction<T[][]> blockMapConstructor) {
      this.empty = (T[])((Object[])blockConstructor.apply(256));
      this.blockMap = (T[][])((Object[][])blockMapConstructor.apply(4352));
      Arrays.fill(this.blockMap, this.empty);
      this.blockConstructor = blockConstructor;
   }

   public void clear() {
      Arrays.fill(this.blockMap, this.empty);
   }

   public @Nullable T get(final int codepoint) {
      int block = codepoint >> 8;
      int offset = codepoint & 0xFF;
      return this.blockMap[block][offset];
   }

   public @Nullable T put(final int codepoint, final T value) {
      int block = codepoint >> 8;
      int offset = codepoint & 0xFF;
      T[] blockData = this.blockMap[block];
      if (blockData == this.empty) {
         blockData = (T[])((Object[])this.blockConstructor.apply(256));
         this.blockMap[block] = blockData;
         blockData[offset] = value;
         return null;
      } else {
         T previous = blockData[offset];
         blockData[offset] = value;
         return previous;
      }
   }

   public T computeIfAbsent(final int codepoint, final IntFunction<T> mapper) {
      int block = codepoint >> 8;
      int offset = codepoint & 0xFF;
      T[] blockData = this.blockMap[block];
      T current = blockData[offset];
      if (current != null) {
         return current;
      }

      if (blockData == this.empty) {
         blockData = (T[])((Object[])this.blockConstructor.apply(256));
         this.blockMap[block] = blockData;
      }

      T result = mapper.apply(codepoint);
      blockData[offset] = result;
      return result;
   }

   public @Nullable T remove(final int codepoint) {
      int block = codepoint >> 8;
      int offset = codepoint & 0xFF;
      T[] blockData = this.blockMap[block];
      if (blockData == this.empty) {
         return null;
      }

      T previous = blockData[offset];
      blockData[offset] = null;
      return previous;
   }

   public void forEach(final CodepointMap.Output<T> output) {
      for (int block = 0; block < this.blockMap.length; block++) {
         T[] blockData = this.blockMap[block];
         if (blockData != this.empty) {
            for (int offset = 0; offset < blockData.length; offset++) {
               T value = blockData[offset];
               if (value != null) {
                  int codepoint = block << 8 | offset;
                  output.accept(codepoint, value);
               }
            }
         }
      }
   }

   public IntSet keySet() {
      IntOpenHashSet result = new IntOpenHashSet();
      this.forEach((codepoint, value) -> result.add(codepoint));
      return result;
   }

   @FunctionalInterface
   public interface Output<T> {
      void accept(int codepoint, T value);
   }
}
