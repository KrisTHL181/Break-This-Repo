package net.minecraft.client.gui.components;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import org.slf4j.Logger;

public class MultilineTextField {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int NO_LIMIT = Integer.MAX_VALUE;
   private static final int LINE_SEEK_PIXEL_BIAS = 2;
   private final Font font;
   private final List<MultilineTextField.StringView> displayLines = Lists.newArrayList();
   private String value;
   private int cursor;
   private int selectCursor;
   private boolean selecting;
   private int characterLimit = Integer.MAX_VALUE;
   private int lineLimit = Integer.MAX_VALUE;
   private final int width;
   private Consumer<String> valueListener = s -> {};
   private Runnable cursorListener = () -> {};

   public MultilineTextField(final Font font, final int width) {
      this.font = font;
      this.width = width;
      this.setValue("");
   }

   public int characterLimit() {
      return this.characterLimit;
   }

   public void setCharacterLimit(final int characterLimit) {
      if (characterLimit < 0) {
         throw new IllegalArgumentException("Character limit cannot be negative");
      }

      this.characterLimit = characterLimit;
   }

   public void setLineLimit(final int lineLimit) {
      if (lineLimit < 0) {
         throw new IllegalArgumentException("Character limit cannot be negative");
      }

      this.lineLimit = lineLimit;
   }

   public boolean hasCharacterLimit() {
      return this.characterLimit != Integer.MAX_VALUE;
   }

   public boolean hasLineLimit() {
      return this.lineLimit != Integer.MAX_VALUE;
   }

   public void setValueListener(final Consumer<String> valueListener) {
      this.valueListener = valueListener;
   }

   public void setCursorListener(final Runnable cursorListener) {
      this.cursorListener = cursorListener;
   }

   public void setValue(final String value) {
      this.setValue(value, false);
   }

   public void setValue(final String value, final boolean allowOverflowLineLimit) {
      String newValue = this.truncateFullText(value);
      if (allowOverflowLineLimit || !this.overflowsLineLimit(newValue)) {
         this.value = newValue;
         this.cursor = this.value.length();
         this.selectCursor = this.cursor;
         this.onValueChange();
      }
   }

   public String value() {
      return this.value;
   }

   public void insertText(final String input) {
      if (!input.isEmpty() || this.hasSelection()) {
         String text = this.truncateInsertionText(StringUtil.filterText(input, true));
         MultilineTextField.StringView selected = this.getSelected();
         String newValue = new StringBuilder(this.value).replace(selected.beginIndex, selected.endIndex, text).toString();
         if (!this.overflowsLineLimit(newValue)) {
            this.value = newValue;
            this.cursor = selected.beginIndex + text.length();
            this.selectCursor = this.cursor;
            this.onValueChange();
         }
      }
   }

   public void deleteText(final int dir) {
      if (!this.hasSelection()) {
         this.selectCursor = Mth.clamp(this.cursor + dir, 0, this.value.length());
      }

      this.insertText("");
   }

   public int cursor() {
      return this.cursor;
   }

   public void setSelecting(final boolean selecting) {
      this.selecting = selecting;
   }

   public MultilineTextField.StringView getSelected() {
      return new MultilineTextField.StringView(Math.min(this.selectCursor, this.cursor), Math.max(this.selectCursor, this.cursor));
   }

   public int getLineCount() {
      return this.displayLines.size();
   }

   public int getLineAtCursor() {
      for (int i = 0; i < this.displayLines.size(); i++) {
         MultilineTextField.StringView view = this.displayLines.get(i);
         if (this.cursor >= view.beginIndex && this.cursor <= view.endIndex) {
            return i;
         }
      }

      return -1;
   }

   public MultilineTextField.StringView getLineView(final int lineIndex) {
      return this.displayLines.get(Mth.clamp(lineIndex, 0, this.displayLines.size() - 1));
   }

   public void seekCursor(final Whence whence, final int cursor) {
      switch (whence) {
         case ABSOLUTE:
            this.cursor = cursor;
            break;
         case RELATIVE:
            this.cursor += cursor;
            break;
         case END:
            this.cursor = this.value.length() + cursor;
      }

      this.cursor = Mth.clamp(this.cursor, 0, this.value.length());
      this.cursorListener.run();
      if (!this.selecting) {
         this.selectCursor = this.cursor;
      }
   }

   public void seekCursorLine(final int lineOffset) {
      if (lineOffset != 0) {
         int oldCursorLeft = this.font.width(this.value.substring(this.getCursorLineView().beginIndex, this.cursor)) + 2;
         MultilineTextField.StringView lineView = this.getCursorLineView(lineOffset);
         int newCursor = this.font.plainSubstrByWidth(this.value.substring(lineView.beginIndex, lineView.endIndex), oldCursorLeft).length();
         this.seekCursor(Whence.ABSOLUTE, lineView.beginIndex + newCursor);
      }
   }

   public void seekCursorToPoint(final double x, final double y) {
      int left = Mth.floor(x);
      int top = Mth.floor(y / 9.0);
      MultilineTextField.StringView lineView = this.displayLines.get(Mth.clamp(top, 0, this.displayLines.size() - 1));
      int clickedColumn = this.font.plainSubstrByWidth(this.value.substring(lineView.beginIndex, lineView.endIndex), left).length();
      this.seekCursor(Whence.ABSOLUTE, lineView.beginIndex + clickedColumn);
   }

   public void selectWordAtCursor() {
      MultilineTextField.StringView wordView = this.getPreviousWord();
      this.seekCursor(Whence.ABSOLUTE, wordView.beginIndex);
      this.setSelecting(true);
      this.seekCursor(Whence.ABSOLUTE, wordView.endIndex);
   }

   public boolean keyPressed(final KeyEvent event) {
      this.selecting = event.hasShiftDown();
      if (event.isSelectAll()) {
         this.cursor = this.value.length();
         this.selectCursor = 0;
         return true;
      }

      if (event.isCopy()) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         return true;
      }

      if (event.isPaste()) {
         this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
         return true;
      }

      if (event.isCut()) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         this.insertText("");
         return true;
      }

      switch (event.key()) {
         case 257:
         case 335:
            this.insertText("\n");
            return true;
         case 259:
            if (event.hasControlDownWithQuirk()) {
               MultilineTextField.StringView wordView = this.getPreviousWord();
               this.deleteText(wordView.beginIndex - this.cursor);
            } else {
               this.deleteText(-1);
            }

            return true;
         case 261:
            if (event.hasControlDownWithQuirk()) {
               MultilineTextField.StringView wordView = this.getNextWord();
               this.deleteText(wordView.beginIndex - this.cursor);
            } else {
               this.deleteText(1);
            }

            return true;
         case 262:
            if (event.hasControlDownWithQuirk()) {
               MultilineTextField.StringView wordView = this.getNextWord();
               this.seekCursor(Whence.ABSOLUTE, wordView.beginIndex);
            } else {
               this.seekCursor(Whence.RELATIVE, 1);
            }

            return true;
         case 263:
            if (event.hasControlDownWithQuirk()) {
               MultilineTextField.StringView wordView = this.getPreviousWord();
               this.seekCursor(Whence.ABSOLUTE, wordView.beginIndex);
            } else {
               this.seekCursor(Whence.RELATIVE, -1);
            }

            return true;
         case 264:
            if (!event.hasControlDownWithQuirk()) {
               this.seekCursorLine(1);
            }

            return true;
         case 265:
            if (!event.hasControlDownWithQuirk()) {
               this.seekCursorLine(-1);
            }

            return true;
         case 266:
            this.seekCursor(Whence.ABSOLUTE, 0);
            return true;
         case 267:
            this.seekCursor(Whence.END, 0);
            return true;
         case 268:
            if (event.hasControlDownWithQuirk()) {
               this.seekCursor(Whence.ABSOLUTE, 0);
            } else {
               this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
            }

            return true;
         case 269:
            if (event.hasControlDownWithQuirk()) {
               this.seekCursor(Whence.END, 0);
            } else {
               this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().endIndex);
            }

            return true;
         default:
            return false;
      }
   }

   public Iterable<MultilineTextField.StringView> iterateLines() {
      return this.displayLines;
   }

   public boolean hasSelection() {
      return this.selectCursor != this.cursor;
   }

   @VisibleForTesting
   public String getSelectedText() {
      MultilineTextField.StringView selected = this.getSelected();
      return this.value.substring(selected.beginIndex, selected.endIndex);
   }

   private MultilineTextField.StringView getCursorLineView() {
      return this.getCursorLineView(0);
   }

   private MultilineTextField.StringView getCursorLineView(final int lineOffset) {
      int lineIndex = this.getLineAtCursor();
      if (lineIndex < 0) {
         LOGGER.error("Cursor is not within text (cursor = {}, length = {})", this.cursor, this.value.length());
         return this.displayLines.getLast();
      } else {
         return this.displayLines.get(Mth.clamp(lineIndex + lineOffset, 0, this.displayLines.size() - 1));
      }
   }

   @VisibleForTesting
   public MultilineTextField.StringView getPreviousWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      }

      int startPosition = Mth.clamp(this.cursor, 0, this.value.length() - 1);

      while (startPosition > 0 && Character.isWhitespace(this.value.charAt(startPosition - 1))) {
         startPosition--;
      }

      while (startPosition > 0 && !Character.isWhitespace(this.value.charAt(startPosition - 1))) {
         startPosition--;
      }

      return new MultilineTextField.StringView(startPosition, this.getWordEndPosition(startPosition));
   }

   @VisibleForTesting
   public MultilineTextField.StringView getNextWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      }

      int startPosition = Mth.clamp(this.cursor, 0, this.value.length() - 1);

      while (startPosition < this.value.length() && !Character.isWhitespace(this.value.charAt(startPosition))) {
         startPosition++;
      }

      while (startPosition < this.value.length() && Character.isWhitespace(this.value.charAt(startPosition))) {
         startPosition++;
      }

      return new MultilineTextField.StringView(startPosition, this.getWordEndPosition(startPosition));
   }

   private int getWordEndPosition(final int from) {
      int end = from;

      while (end < this.value.length() && !Character.isWhitespace(this.value.charAt(end))) {
         end++;
      }

      return end;
   }

   private void onValueChange() {
      this.reflowDisplayLines();
      this.valueListener.accept(this.value);
      this.cursorListener.run();
   }

   private void reflowDisplayLines() {
      this.displayLines.clear();
      if (this.value.isEmpty()) {
         this.displayLines.add(MultilineTextField.StringView.EMPTY);
      } else {
         this.font
            .getSplitter()
            .splitLines(this.value, this.width, Style.EMPTY, false, (style, start, end) -> this.displayLines.add(new MultilineTextField.StringView(start, end)));
         if (this.value.charAt(this.value.length() - 1) == '\n') {
            this.displayLines.add(new MultilineTextField.StringView(this.value.length(), this.value.length()));
         }
      }
   }

   private String truncateFullText(final String input) {
      return this.hasCharacterLimit() ? StringUtil.truncateStringIfNecessary(input, this.characterLimit, false) : input;
   }

   private String truncateInsertionText(final String input) {
      String truncatedInput = input;
      if (this.hasCharacterLimit()) {
         int remainingCharacters = this.characterLimit - this.value.length();
         truncatedInput = StringUtil.truncateStringIfNecessary(input, remainingCharacters, false);
      }

      return truncatedInput;
   }

   private boolean overflowsLineLimit(final String newValue) {
      return this.hasLineLimit()
         && this.font.getSplitter().splitLines(newValue, this.width, Style.EMPTY).size() + (StringUtil.endsWithNewLine(newValue) ? 1 : 0) > this.lineLimit;
   }

   protected record StringView(int beginIndex, int endIndex) {
      private static final MultilineTextField.StringView EMPTY = new MultilineTextField.StringView(0, 0);
   }
}
