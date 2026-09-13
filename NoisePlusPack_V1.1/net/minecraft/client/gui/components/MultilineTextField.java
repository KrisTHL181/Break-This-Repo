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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
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
   private Consumer<String> valueListener = p_239235_ -> {};
   private Runnable cursorListener = () -> {};

   public MultilineTextField(Font p_239611_, int p_239612_) {
      this.font = p_239611_;
      this.width = p_239612_;
      this.setValue("");
   }

   public int characterLimit() {
      return this.characterLimit;
   }

   public void setCharacterLimit(int p_240163_) {
      if (p_240163_ < 0) {
         throw new IllegalArgumentException("Character limit cannot be negative");
      }

      this.characterLimit = p_240163_;
   }

   public void setLineLimit(int p_407026_) {
      if (p_407026_ < 0) {
         throw new IllegalArgumentException("Character limit cannot be negative");
      }

      this.lineLimit = p_407026_;
   }

   public boolean hasCharacterLimit() {
      return this.characterLimit != Integer.MAX_VALUE;
   }

   public boolean hasLineLimit() {
      return this.lineLimit != Integer.MAX_VALUE;
   }

   public void setValueListener(Consumer<String> p_239920_) {
      this.valueListener = p_239920_;
   }

   public void setCursorListener(Runnable p_239258_) {
      this.cursorListener = p_239258_;
   }

   public void setValue(String p_239678_) {
      this.setValue(p_239678_, false);
   }

   public void setValue(String p_407212_, boolean p_408427_) {
      String s = this.truncateFullText(p_407212_);
      if (p_408427_ || !this.overflowsLineLimit(s)) {
         this.value = s;
         this.cursor = this.value.length();
         this.selectCursor = this.cursor;
         this.onValueChange();
      }
   }

   public String value() {
      return this.value;
   }

   public void insertText(String p_240016_) {
      if (!p_240016_.isEmpty() || this.hasSelection()) {
         String s = this.truncateInsertionText(StringUtil.filterText(p_240016_, true));
         MultilineTextField.StringView multilinetextfield$stringview = this.getSelected();
         String s1 = new StringBuilder(this.value).replace(multilinetextfield$stringview.beginIndex, multilinetextfield$stringview.endIndex, s).toString();
         if (!this.overflowsLineLimit(s1)) {
            this.value = s1;
            this.cursor = multilinetextfield$stringview.beginIndex + s.length();
            this.selectCursor = this.cursor;
            this.onValueChange();
         }
      }
   }

   public void deleteText(int p_239475_) {
      if (!this.hasSelection()) {
         this.selectCursor = Mth.clamp(this.cursor + p_239475_, 0, this.value.length());
      }

      this.insertText("");
   }

   public int cursor() {
      return this.cursor;
   }

   public void setSelecting(boolean p_239951_) {
      this.selecting = p_239951_;
   }

   public MultilineTextField.StringView getSelected() {
      return new MultilineTextField.StringView(Math.min(this.selectCursor, this.cursor), Math.max(this.selectCursor, this.cursor));
   }

   public int getLineCount() {
      return this.displayLines.size();
   }

   public int getLineAtCursor() {
      for (int i = 0; i < this.displayLines.size(); i++) {
         MultilineTextField.StringView multilinetextfield$stringview = this.displayLines.get(i);
         if (this.cursor >= multilinetextfield$stringview.beginIndex && this.cursor <= multilinetextfield$stringview.endIndex) {
            return i;
         }
      }

      return -1;
   }

   public MultilineTextField.StringView getLineView(int p_239145_) {
      return this.displayLines.get(Mth.clamp(p_239145_, 0, this.displayLines.size() - 1));
   }

   public void seekCursor(Whence p_239798_, int p_239799_) {
      switch (p_239798_) {
         case ABSOLUTE:
            this.cursor = p_239799_;
            break;
         case RELATIVE:
            this.cursor += p_239799_;
            break;
         case END:
            this.cursor = this.value.length() + p_239799_;
      }

      this.cursor = Mth.clamp(this.cursor, 0, this.value.length());
      this.cursorListener.run();
      if (!this.selecting) {
         this.selectCursor = this.cursor;
      }
   }

   public void seekCursorLine(int p_239394_) {
      if (p_239394_ != 0) {
         int i = this.font.width(this.value.substring(this.getCursorLineView().beginIndex, this.cursor)) + 2;
         MultilineTextField.StringView multilinetextfield$stringview = this.getCursorLineView(p_239394_);
         int j = this.font
            .plainSubstrByWidth(this.value.substring(multilinetextfield$stringview.beginIndex, multilinetextfield$stringview.endIndex), i)
            .length();
         this.seekCursor(Whence.ABSOLUTE, multilinetextfield$stringview.beginIndex + j);
      }
   }

   public void seekCursorToPoint(double p_239579_, double p_239580_) {
      int i = Mth.floor(p_239579_);
      int j = Mth.floor(p_239580_ / 9.0);
      MultilineTextField.StringView multilinetextfield$stringview = this.displayLines.get(Mth.clamp(j, 0, this.displayLines.size() - 1));
      int k = this.font.plainSubstrByWidth(this.value.substring(multilinetextfield$stringview.beginIndex, multilinetextfield$stringview.endIndex), i).length();
      this.seekCursor(Whence.ABSOLUTE, multilinetextfield$stringview.beginIndex + k);
   }

   public void selectWordAtCursor() {
      MultilineTextField.StringView multilinetextfield$stringview = this.getPreviousWord();
      this.seekCursor(Whence.ABSOLUTE, multilinetextfield$stringview.beginIndex);
      this.setSelecting(true);
      this.seekCursor(Whence.ABSOLUTE, multilinetextfield$stringview.endIndex);
   }

   public boolean keyPressed(KeyEvent p_425216_) {
      this.selecting = p_425216_.hasShiftDown();
      if (p_425216_.isSelectAll()) {
         this.cursor = this.value.length();
         this.selectCursor = 0;
         return true;
      }

      if (p_425216_.isCopy()) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         return true;
      }

      if (p_425216_.isPaste()) {
         this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
         return true;
      }

      if (p_425216_.isCut()) {
         Minecraft.getInstance().keyboardHandler.setClipboard(this.getSelectedText());
         this.insertText("");
         return true;
      }

      switch (p_425216_.key()) {
         case 257:
         case 335:
            this.insertText("\n");
            return true;
         case 259:
            if (p_425216_.hasControlDownWithQuirk()) {
               MultilineTextField.StringView multilinetextfield$stringview3 = this.getPreviousWord();
               this.deleteText(multilinetextfield$stringview3.beginIndex - this.cursor);
            } else {
               this.deleteText(-1);
            }

            return true;
         case 261:
            if (p_425216_.hasControlDownWithQuirk()) {
               MultilineTextField.StringView multilinetextfield$stringview2 = this.getNextWord();
               this.deleteText(multilinetextfield$stringview2.beginIndex - this.cursor);
            } else {
               this.deleteText(1);
            }

            return true;
         case 262:
            if (p_425216_.hasControlDownWithQuirk()) {
               MultilineTextField.StringView multilinetextfield$stringview1 = this.getNextWord();
               this.seekCursor(Whence.ABSOLUTE, multilinetextfield$stringview1.beginIndex);
            } else {
               this.seekCursor(Whence.RELATIVE, 1);
            }

            return true;
         case 263:
            if (p_425216_.hasControlDownWithQuirk()) {
               MultilineTextField.StringView multilinetextfield$stringview = this.getPreviousWord();
               this.seekCursor(Whence.ABSOLUTE, multilinetextfield$stringview.beginIndex);
            } else {
               this.seekCursor(Whence.RELATIVE, -1);
            }

            return true;
         case 264:
            if (!p_425216_.hasControlDownWithQuirk()) {
               this.seekCursorLine(1);
            }

            return true;
         case 265:
            if (!p_425216_.hasControlDownWithQuirk()) {
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
            if (p_425216_.hasControlDownWithQuirk()) {
               this.seekCursor(Whence.ABSOLUTE, 0);
            } else {
               this.seekCursor(Whence.ABSOLUTE, this.getCursorLineView().beginIndex);
            }

            return true;
         case 269:
            if (p_425216_.hasControlDownWithQuirk()) {
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
      MultilineTextField.StringView multilinetextfield$stringview = this.getSelected();
      return this.value.substring(multilinetextfield$stringview.beginIndex, multilinetextfield$stringview.endIndex);
   }

   private MultilineTextField.StringView getCursorLineView() {
      return this.getCursorLineView(0);
   }

   private MultilineTextField.StringView getCursorLineView(int p_239855_) {
      int i = this.getLineAtCursor();
      if (i < 0) {
         LOGGER.error("Cursor is not within text (cursor = {}, length = {})", this.cursor, this.value.length());
         return this.displayLines.getLast();
      } else {
         return this.displayLines.get(Mth.clamp(i + p_239855_, 0, this.displayLines.size() - 1));
      }
   }

   @VisibleForTesting
   public MultilineTextField.StringView getPreviousWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      }

      int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);

      while (i > 0 && Character.isWhitespace(this.value.charAt(i - 1))) {
         i--;
      }

      while (i > 0 && !Character.isWhitespace(this.value.charAt(i - 1))) {
         i--;
      }

      return new MultilineTextField.StringView(i, this.getWordEndPosition(i));
   }

   @VisibleForTesting
   public MultilineTextField.StringView getNextWord() {
      if (this.value.isEmpty()) {
         return MultilineTextField.StringView.EMPTY;
      }

      int i = Mth.clamp(this.cursor, 0, this.value.length() - 1);

      while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
         i++;
      }

      while (i < this.value.length() && Character.isWhitespace(this.value.charAt(i))) {
         i++;
      }

      return new MultilineTextField.StringView(i, this.getWordEndPosition(i));
   }

   private int getWordEndPosition(int p_240093_) {
      int i = p_240093_;

      while (i < this.value.length() && !Character.isWhitespace(this.value.charAt(i))) {
         i++;
      }

      return i;
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
            .splitLines(
               this.value,
               this.width,
               Style.EMPTY,
               false,
               (p_239846_, p_239847_, p_239848_) -> this.displayLines.add(new MultilineTextField.StringView(p_239847_, p_239848_))
            );
         if (this.value.charAt(this.value.length() - 1) == '\n') {
            this.displayLines.add(new MultilineTextField.StringView(this.value.length(), this.value.length()));
         }
      }
   }

   private String truncateFullText(String p_239843_) {
      return this.hasCharacterLimit() ? StringUtil.truncateStringIfNecessary(p_239843_, this.characterLimit, false) : p_239843_;
   }

   private String truncateInsertionText(String p_239418_) {
      String s = p_239418_;
      if (this.hasCharacterLimit()) {
         int i = this.characterLimit - this.value.length();
         s = StringUtil.truncateStringIfNecessary(p_239418_, i, false);
      }

      return s;
   }

   private boolean overflowsLineLimit(String p_407017_) {
      return this.hasLineLimit()
         && this.font.getSplitter().splitLines(p_407017_, this.width, Style.EMPTY).size() + (StringUtil.endsWithNewLine(p_407017_) ? 1 : 0) > this.lineLimit;
   }

   @OnlyIn(Dist.CLIENT)
   protected record StringView(int beginIndex, int endIndex) {
      static final MultilineTextField.StringView EMPTY = new MultilineTextField.StringView(0, 0);
   }
}
