package net.minecraft.client.input;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.nio.IntBuffer;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public record PreeditEvent(String fullText, int caretPosition, List<String> blocks, int focusedBlock) {
   public PreeditEvent {
      Preconditions.checkElementIndex(focusedBlock, blocks.size());
   }

   public static @Nullable PreeditEvent createFromCallback(
      final int preeditSize, final long preeditPtr, final int blockCount, final long blockSizesPtr, final int focusedBlock, final int caret
   ) {
      if (preeditSize == 0) {
         return null;
      }

      int[] codepoints = readIntBuffer(preeditSize, preeditPtr);
      int[] blockSizes = readIntBuffer(blockCount, blockSizesPtr);
      StringBuilder fullText = new StringBuilder();
      Builder<String> blocks = ImmutableList.builder();
      int offset = 0;
      int convertedCaret = 0;

      for (int blockSize : blockSizes) {
         StringBuilder blockBuilder = new StringBuilder();

         for (int i = 0; i < blockSize; i++) {
            int codepoint = codepoints[offset];
            if (offset == caret) {
               convertedCaret = fullText.length() + blockBuilder.length();
            }

            blockBuilder.appendCodePoint(codepoint);
            offset++;
         }

         String block = blockBuilder.toString();
         blocks.add(block);
         fullText.append(block);
      }

      if (offset == caret) {
         convertedCaret = fullText.length();
      }

      return new PreeditEvent(fullText.toString(), convertedCaret, blocks.build(), focusedBlock);
   }

   private static int[] readIntBuffer(final int size, final long ptr) {
      IntBuffer buffer = MemoryUtil.memIntBuffer(ptr, size);
      int[] result = new int[size];
      buffer.get(result);
      return result;
   }

   public MutableComponent toFormattedText(final Style focusedStyle) {
      int blockCount = this.blocks.size();
      if (blockCount == 1) {
         return Component.literal(this.blocks.getFirst()).withStyle(focusedStyle);
      }

      MutableComponent result = Component.empty();

      for (int i = 0; i < blockCount; i++) {
         MutableComponent part = Component.literal(this.blocks.get(i));
         if (i == this.focusedBlock) {
            part.withStyle(focusedStyle);
         }

         result.append(part);
      }

      return result;
   }
}
