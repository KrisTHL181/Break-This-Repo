package net.minecraft.client;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;

public class StringSplitter {
   private final StringSplitter.WidthProvider widthProvider;

   public StringSplitter(final StringSplitter.WidthProvider widthProvider) {
      this.widthProvider = widthProvider;
   }

   public float stringWidth(final @Nullable String str) {
      if (str == null) {
         return 0.0F;
      }

      MutableFloat result = new MutableFloat();
      StringDecomposer.iterateFormatted(str, Style.EMPTY, (position, style, codepoint) -> {
         result.add(this.widthProvider.getWidth(codepoint, style));
         return true;
      });
      return result.floatValue();
   }

   public float stringWidth(final FormattedText text) {
      MutableFloat result = new MutableFloat();
      StringDecomposer.iterateFormatted(text, Style.EMPTY, (position, style, codepoint) -> {
         result.add(this.widthProvider.getWidth(codepoint, style));
         return true;
      });
      return result.floatValue();
   }

   public float stringWidth(final FormattedCharSequence text) {
      MutableFloat result = new MutableFloat();
      text.accept((position, style, codepoint) -> {
         result.add(this.widthProvider.getWidth(codepoint, style));
         return true;
      });
      return result.floatValue();
   }

   public int plainIndexAtWidth(final String str, final int maxWidth, final Style style) {
      StringSplitter.WidthLimitedCharSink output = new StringSplitter.WidthLimitedCharSink(maxWidth);
      StringDecomposer.iterate(str, style, output);
      return output.getPosition();
   }

   public String plainHeadByWidth(final String str, final int maxWidth, final Style style) {
      return str.substring(0, this.plainIndexAtWidth(str, maxWidth, style));
   }

   public String plainTailByWidth(final String str, final int maxWidth, final Style style) {
      MutableFloat currentWidth = new MutableFloat();
      MutableInt result = new MutableInt(str.length());
      StringDecomposer.iterateBackwards(str, style, (position, s, codepoint) -> {
         float w = currentWidth.addAndGet(this.widthProvider.getWidth(codepoint, s));
         if (w > maxWidth) {
            return false;
         }

         result.setValue(position);
         return true;
      });
      return str.substring(result.intValue());
   }

   public FormattedText headByWidth(final FormattedText text, final int width, final Style initialStyle) {
      final StringSplitter.WidthLimitedCharSink output = new StringSplitter.WidthLimitedCharSink(width);
      return text.visit(new FormattedText.StyledContentConsumer<FormattedText>() {
         private final ComponentCollector collector = new ComponentCollector();

         @Override
         public Optional<FormattedText> accept(final Style style, final String contents) {
            output.resetPosition();
            if (!StringDecomposer.iterateFormatted(contents, style, output)) {
               String partial = contents.substring(0, output.getPosition());
               if (!partial.isEmpty()) {
                  this.collector.append(FormattedText.of(partial, style));
               }

               return Optional.of(this.collector.getResultOrEmpty());
            } else {
               if (!contents.isEmpty()) {
                  this.collector.append(FormattedText.of(contents, style));
               }

               return Optional.empty();
            }
         }
      }, initialStyle).orElse(text);
   }

   public int findLineBreak(final String input, final int max, final Style initialStyle) {
      StringSplitter.LineBreakFinder finder = new StringSplitter.LineBreakFinder(max);
      StringDecomposer.iterateFormatted(input, initialStyle, finder);
      return finder.getSplitPosition();
   }

   public static int getWordPosition(final String text, final int dir, final int from, final boolean stripSpaces) {
      int result = from;
      boolean reverse = dir < 0;
      int abs = Math.abs(dir);

      for (int i = 0; i < abs; i++) {
         if (reverse) {
            while (stripSpaces && result > 0 && (text.charAt(result - 1) == ' ' || text.charAt(result - 1) == '\n')) {
               result--;
            }

            while (result > 0 && text.charAt(result - 1) != ' ' && text.charAt(result - 1) != '\n') {
               result--;
            }
         } else {
            int length = text.length();
            int index1 = text.indexOf(32, result);
            int index2 = text.indexOf(10, result);
            if (index1 == -1 && index2 == -1) {
               result = -1;
            } else if (index1 != -1 && index2 != -1) {
               result = Math.min(index1, index2);
            } else if (index1 != -1) {
               result = index1;
            } else {
               result = index2;
            }

            if (result == -1) {
               result = length;
            } else {
               while (stripSpaces && result < length && (text.charAt(result) == ' ' || text.charAt(result) == '\n')) {
                  result++;
               }
            }
         }
      }

      return result;
   }

   public void splitLines(
      final String input, final int maxWidth, final Style initialStyle, final boolean includeAll, final StringSplitter.LinePosConsumer output
   ) {
      int start = 0;
      int size = input.length();
      Style workStyle = initialStyle;

      while (start < size) {
         StringSplitter.LineBreakFinder finder = new StringSplitter.LineBreakFinder(maxWidth);
         boolean endOfText = StringDecomposer.iterateFormatted(input, start, workStyle, initialStyle, finder);
         if (endOfText) {
            output.accept(workStyle, start, size);
            break;
         }

         int lineBreak = finder.getSplitPosition();
         char firstTailChar = input.charAt(lineBreak);
         int adjustedBreak = firstTailChar != '\n' && firstTailChar != ' ' ? lineBreak : lineBreak + 1;
         output.accept(workStyle, start, includeAll ? adjustedBreak : lineBreak);
         start = adjustedBreak;
         workStyle = finder.getSplitStyle();
      }
   }

   public List<FormattedText> splitLines(final String input, final int maxWidth, final Style initialStyle) {
      List<FormattedText> result = Lists.newArrayList();
      this.splitLines(input, maxWidth, initialStyle, false, (style, start, end) -> result.add(FormattedText.of(input.substring(start, end), style)));
      return result;
   }

   public List<FormattedText> splitLines(final FormattedText input, final int maxWidth, final Style initialStyle) {
      List<FormattedText> result = Lists.newArrayList();
      this.splitLines(input, maxWidth, initialStyle, (text, wrapped) -> result.add(text));
      return result;
   }

   public void splitLines(final FormattedText input, final int maxWidth, final Style initialStyle, final BiConsumer<FormattedText, Boolean> output) {
      List<StringSplitter.LineComponent> partList = Lists.newArrayList();
      input.visit((style, contents) -> {
         if (!contents.isEmpty()) {
            partList.add(new StringSplitter.LineComponent(contents, style));
         }

         return Optional.empty();
      }, initialStyle);
      StringSplitter.FlatComponents parts = new StringSplitter.FlatComponents(partList);
      boolean shouldRestart = true;
      boolean forceNewLine = false;
      boolean isWrapped = false;

      while (shouldRestart) {
         shouldRestart = false;
         StringSplitter.LineBreakFinder finder = new StringSplitter.LineBreakFinder(maxWidth);

         for (StringSplitter.LineComponent part : parts.parts) {
            boolean endOfText = StringDecomposer.iterateFormatted(part.contents, 0, part.style, initialStyle, finder);
            if (!endOfText) {
               int lineBreak = finder.getSplitPosition();
               Style lineBreakStyle = finder.getSplitStyle();
               char firstTailChar = parts.charAt(lineBreak);
               boolean isNewLine = firstTailChar == '\n';
               boolean skipNextChar = isNewLine || firstTailChar == ' ';
               forceNewLine = isNewLine;
               FormattedText result = parts.splitAt(lineBreak, skipNextChar ? 1 : 0, lineBreakStyle);
               output.accept(result, isWrapped);
               isWrapped = !isNewLine;
               shouldRestart = true;
               break;
            }

            finder.addToOffset(part.contents.length());
         }
      }

      FormattedText lastLine = parts.getRemainder();
      if (lastLine != null) {
         output.accept(lastLine, isWrapped);
      } else if (forceNewLine) {
         output.accept(FormattedText.EMPTY, false);
      }
   }

   private static class FlatComponents {
      private final List<StringSplitter.LineComponent> parts;
      private String flatParts;

      public FlatComponents(final List<StringSplitter.LineComponent> parts) {
         this.parts = parts;
         this.flatParts = parts.stream().map(p -> p.contents).collect(Collectors.joining());
      }

      public char charAt(final int position) {
         return this.flatParts.charAt(position);
      }

      public FormattedText splitAt(final int skipPosition, final int skipSize, final Style splitStyle) {
         ComponentCollector result = new ComponentCollector();
         ListIterator<StringSplitter.LineComponent> it = this.parts.listIterator();
         int position = skipPosition;
         boolean inSkip = false;

         while (it.hasNext()) {
            StringSplitter.LineComponent element = it.next();
            String contents = element.contents;
            int contentsSize = contents.length();
            if (!inSkip) {
               if (position > contentsSize) {
                  result.append(element);
                  it.remove();
                  position -= contentsSize;
               } else {
                  String beforeSplit = contents.substring(0, position);
                  if (!beforeSplit.isEmpty()) {
                     result.append(FormattedText.of(beforeSplit, element.style));
                  }

                  position += skipSize;
                  inSkip = true;
               }
            }

            if (inSkip) {
               if (position <= contentsSize) {
                  String afterSplit = contents.substring(position);
                  if (afterSplit.isEmpty()) {
                     it.remove();
                  } else {
                     it.set(new StringSplitter.LineComponent(afterSplit, splitStyle));
                  }
                  break;
               }

               it.remove();
               position -= contentsSize;
            }
         }

         this.flatParts = this.flatParts.substring(skipPosition + skipSize);
         return result.getResultOrEmpty();
      }

      public @Nullable FormattedText getRemainder() {
         ComponentCollector result = new ComponentCollector();
         this.parts.forEach(result::append);
         this.parts.clear();
         return result.getResult();
      }
   }

   private class LineBreakFinder implements FormattedCharSink {
      private final float maxWidth;
      private int lineBreak = -1;
      private Style lineBreakStyle = Style.EMPTY;
      private boolean hadNonZeroWidthChar;
      private float width;
      private int lastSpace = -1;
      private Style lastSpaceStyle = Style.EMPTY;
      private int nextChar;
      private int offset;

      public LineBreakFinder(final float maxWidth) {
         this.maxWidth = Math.max(maxWidth, 1.0F);
      }

      @Override
      public boolean accept(final int position, final Style style, final int codepoint) {
         int adjustedPosition = position + this.offset;
         switch (codepoint) {
            case 10:
               return this.finishIteration(adjustedPosition, style);
            case 32:
               this.lastSpace = adjustedPosition;
               this.lastSpaceStyle = style;
            default:
               float charWidth = StringSplitter.this.widthProvider.getWidth(codepoint, style);
               this.width += charWidth;
               if (!this.hadNonZeroWidthChar || !(this.width > this.maxWidth)) {
                  this.hadNonZeroWidthChar |= charWidth != 0.0F;
                  this.nextChar = adjustedPosition + Character.charCount(codepoint);
                  return true;
               } else {
                  return this.lastSpace != -1 ? this.finishIteration(this.lastSpace, this.lastSpaceStyle) : this.finishIteration(adjustedPosition, style);
               }
         }
      }

      private boolean finishIteration(final int lineBreak, final Style style) {
         this.lineBreak = lineBreak;
         this.lineBreakStyle = style;
         return false;
      }

      private boolean lineBreakFound() {
         return this.lineBreak != -1;
      }

      public int getSplitPosition() {
         return this.lineBreakFound() ? this.lineBreak : this.nextChar;
      }

      public Style getSplitStyle() {
         return this.lineBreakStyle;
      }

      public void addToOffset(final int delta) {
         this.offset += delta;
      }
   }

   private static class LineComponent implements FormattedText {
      private final String contents;
      private final Style style;

      public LineComponent(final String contents, final Style style) {
         this.contents = contents;
         this.style = style;
      }

      @Override
      public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
         return output.accept(this.contents);
      }

      @Override
      public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
         return output.accept(this.style.applyTo(parentStyle), this.contents);
      }
   }

   @FunctionalInterface
   public interface LinePosConsumer {
      void accept(final Style style, int start, int end);
   }

   private class WidthLimitedCharSink implements FormattedCharSink {
      private float maxWidth;
      private int position;

      public WidthLimitedCharSink(final float maxWidth) {
         this.maxWidth = maxWidth;
      }

      @Override
      public boolean accept(final int position, final Style style, final int codepoint) {
         this.maxWidth = this.maxWidth - StringSplitter.this.widthProvider.getWidth(codepoint, style);
         if (this.maxWidth >= 0.0F) {
            this.position = position + Character.charCount(codepoint);
            return true;
         } else {
            return false;
         }
      }

      public int getPosition() {
         return this.position;
      }

      public void resetPosition() {
         this.position = 0;
      }
   }

   @FunctionalInterface
   public interface WidthProvider {
      float getWidth(int codepoint, Style style);
   }
}
