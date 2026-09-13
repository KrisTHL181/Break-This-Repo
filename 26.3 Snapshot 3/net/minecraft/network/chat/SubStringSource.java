package net.minecraft.network.chat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

public class SubStringSource {
   private final String plainText;
   private final List<Style> charStyles;
   private final Int2IntFunction reverseCharModifier;

   private SubStringSource(final String plainText, final List<Style> charStyles, final Int2IntFunction reverseCharModifier) {
      this.plainText = plainText;
      this.charStyles = ImmutableList.copyOf(charStyles);
      this.reverseCharModifier = reverseCharModifier;
   }

   public String getPlainText() {
      return this.plainText;
   }

   public List<FormattedCharSequence> substring(final int start, final int length, final boolean reverse) {
      if (length == 0) {
         return ImmutableList.of();
      }

      List<FormattedCharSequence> parts = Lists.newArrayList();
      Style currentRunStyle = this.charStyles.get(start);
      int currentRunStart = start;

      for (int i = 1; i < length; i++) {
         int actualIndex = start + i;
         Style charStyle = this.charStyles.get(actualIndex);
         if (!charStyle.equals(currentRunStyle)) {
            String currentRunText = this.plainText.substring(currentRunStart, actualIndex);
            parts.add(
               reverse
                  ? FormattedCharSequence.backward(currentRunText, currentRunStyle, this.reverseCharModifier)
                  : FormattedCharSequence.forward(currentRunText, currentRunStyle)
            );
            currentRunStyle = charStyle;
            currentRunStart = actualIndex;
         }
      }

      if (currentRunStart < start + length) {
         String lastRunText = this.plainText.substring(currentRunStart, start + length);
         parts.add(
            reverse
               ? FormattedCharSequence.backward(lastRunText, currentRunStyle, this.reverseCharModifier)
               : FormattedCharSequence.forward(lastRunText, currentRunStyle)
         );
      }

      return reverse ? Lists.reverse(parts) : parts;
   }

   public static SubStringSource create(final FormattedText text) {
      return create(text, ch -> ch, s -> s);
   }

   public static SubStringSource create(final FormattedText text, final Int2IntFunction reverseCharModifier, final UnaryOperator<String> shaper) {
      StringBuilder plainText = new StringBuilder();
      List<Style> charStyles = Lists.newArrayList();
      text.visit((style, contents) -> {
         StringDecomposer.iterateFormatted(contents, style, (position, charStyle, codepoint) -> {
            plainText.appendCodePoint(codepoint);
            int charCount = Character.charCount(codepoint);

            for (int i = 0; i < charCount; i++) {
               charStyles.add(charStyle);
            }

            return true;
         });
         return Optional.empty();
      }, Style.EMPTY);
      return new SubStringSource(shaper.apply(plainText.toString()), charStyles, reverseCharModifier);
   }
}
