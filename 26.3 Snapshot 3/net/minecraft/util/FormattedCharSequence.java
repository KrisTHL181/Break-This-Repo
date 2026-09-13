package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import net.minecraft.network.chat.Style;

@FunctionalInterface
public interface FormattedCharSequence {
   FormattedCharSequence EMPTY = output -> true;

   boolean accept(final FormattedCharSink output);

   static FormattedCharSequence codepoint(final int codepoint, final Style style) {
      return output -> output.accept(0, style, codepoint);
   }

   static FormattedCharSequence forward(final String plainText, final Style style) {
      return plainText.isEmpty() ? EMPTY : output -> StringDecomposer.iterate(plainText, style, output);
   }

   static FormattedCharSequence forward(final String plainText, final Style style, final Int2IntFunction modifier) {
      return plainText.isEmpty() ? EMPTY : output -> StringDecomposer.iterate(plainText, style, decorateOutput(output, modifier));
   }

   static FormattedCharSequence backward(final String plainText, final Style style) {
      return plainText.isEmpty() ? EMPTY : output -> StringDecomposer.iterateBackwards(plainText, style, output);
   }

   static FormattedCharSequence backward(final String plainText, final Style style, final Int2IntFunction modifier) {
      return plainText.isEmpty() ? EMPTY : output -> StringDecomposer.iterateBackwards(plainText, style, decorateOutput(output, modifier));
   }

   static FormattedCharSink decorateOutput(final FormattedCharSink output, final Int2IntFunction modifier) {
      return (p, s, ch) -> output.accept(p, s, (Integer)modifier.apply(ch));
   }

   static FormattedCharSequence composite() {
      return EMPTY;
   }

   static FormattedCharSequence composite(final FormattedCharSequence part) {
      return part;
   }

   static FormattedCharSequence composite(final FormattedCharSequence first, final FormattedCharSequence second) {
      return fromPair(first, second);
   }

   static FormattedCharSequence composite(final FormattedCharSequence... parts) {
      return fromList(ImmutableList.copyOf(parts));
   }

   static FormattedCharSequence composite(final List<FormattedCharSequence> parts) {
      int size = parts.size();

      return switch (size) {
         case 0 -> EMPTY;
         case 1 -> (FormattedCharSequence)parts.get(0);
         case 2 -> fromPair(parts.get(0), parts.get(1));
         default -> fromList(ImmutableList.copyOf(parts));
      };
   }

   static FormattedCharSequence fromPair(final FormattedCharSequence first, final FormattedCharSequence second) {
      return output -> first.accept(output) && second.accept(output);
   }

   static FormattedCharSequence fromList(final List<FormattedCharSequence> partCopy) {
      return output -> {
         for (FormattedCharSequence part : partCopy) {
            if (!part.accept(output)) {
               return false;
            }
         }

         return true;
      };
   }
}
