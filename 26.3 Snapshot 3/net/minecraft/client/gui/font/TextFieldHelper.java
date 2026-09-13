package net.minecraft.client.gui.font;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

public class TextFieldHelper {
   private final Supplier<String> getMessageFn;
   private final Consumer<String> setMessageFn;
   private final Supplier<String> getClipboardFn;
   private final Consumer<String> setClipboardFn;
   private final Predicate<String> stringValidator;
   private int cursorPos;
   private int selectionPos;

   public TextFieldHelper(
      final Supplier<String> getMessageFn,
      final Consumer<String> setMessageFn,
      final Supplier<String> getClipboardFn,
      final Consumer<String> setClipboardFn,
      final Predicate<String> stringValidator
   ) {
      this.getMessageFn = getMessageFn;
      this.setMessageFn = setMessageFn;
      this.getClipboardFn = getClipboardFn;
      this.setClipboardFn = setClipboardFn;
      this.stringValidator = stringValidator;
      this.setCursorToEnd();
   }

   public static Supplier<String> createClipboardGetter(final Minecraft minecraft) {
      return () -> getClipboardContents(minecraft);
   }

   public static String getClipboardContents(final Minecraft minecraft) {
      return ChatFormatting.stripFormatting(minecraft.keyboardHandler.getClipboard().replaceAll("\\r", ""));
   }

   public static Consumer<String> createClipboardSetter(final Minecraft minecraft) {
      return text -> setClipboardContents(minecraft, text);
   }

   public static void setClipboardContents(final Minecraft minecraft, final String text) {
      minecraft.keyboardHandler.setClipboard(text);
   }

   public boolean charTyped(final CharacterEvent event) {
      if (event.isAllowedChatCharacter()) {
         this.insertText(this.getMessageFn.get(), event.codepointAsString());
      }

      return true;
   }

   public boolean keyPressed(final KeyEvent event) {
      if (event.isSelectAll()) {
         this.selectAll();
         return true;
      }

      if (event.isCopy()) {
         this.copy();
         return true;
      }

      if (event.isPaste()) {
         this.paste();
         return true;
      }

      if (event.isCut()) {
         this.cut();
         return true;
      }

      TextFieldHelper.CursorStep cursorStep = event.hasControlDownWithQuirk() ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;
      if (event.key() == 259) {
         this.removeFromCursor(-1, cursorStep);
         return true;
      }

      if (event.key() == 261) {
         this.removeFromCursor(1, cursorStep);
      } else {
         if (event.isLeft()) {
            this.moveBy(-1, event.hasShiftDown(), cursorStep);
            return true;
         }

         if (event.isRight()) {
            this.moveBy(1, event.hasShiftDown(), cursorStep);
            return true;
         }

         if (event.key() == 268) {
            this.setCursorToStart(event.hasShiftDown());
            return true;
         }

         if (event.key() == 269) {
            this.setCursorToEnd(event.hasShiftDown());
            return true;
         }
      }

      return false;
   }

   private int clampToMsgLength(final int value) {
      return Mth.clamp(value, 0, this.getMessageFn.get().length());
   }

   private void insertText(String message, final String text) {
      if (this.selectionPos != this.cursorPos) {
         message = this.deleteSelection(message);
      }

      this.cursorPos = Mth.clamp(this.cursorPos, 0, message.length());
      String newPageText = new StringBuilder(message).insert(this.cursorPos, text).toString();
      if (this.stringValidator.test(newPageText)) {
         this.setMessageFn.accept(newPageText);
         this.selectionPos = this.cursorPos = Math.min(newPageText.length(), this.cursorPos + text.length());
      }
   }

   public void insertText(final String text) {
      this.insertText(this.getMessageFn.get(), text);
   }

   private void resetSelectionIfNeeded(final boolean selecting) {
      if (!selecting) {
         this.selectionPos = this.cursorPos;
      }
   }

   public void moveBy(final int count, final boolean selecting, final TextFieldHelper.CursorStep scope) {
      switch (scope) {
         case CHARACTER:
            this.moveByChars(count, selecting);
            break;
         case WORD:
            this.moveByWords(count, selecting);
      }
   }

   public void moveByChars(final int count) {
      this.moveByChars(count, false);
   }

   public void moveByChars(final int count, final boolean selecting) {
      this.cursorPos = Util.offsetByCodepoints(this.getMessageFn.get(), this.cursorPos, count);
      this.resetSelectionIfNeeded(selecting);
   }

   public void moveByWords(final int count) {
      this.moveByWords(count, false);
   }

   public void moveByWords(final int count, final boolean selecting) {
      this.cursorPos = StringSplitter.getWordPosition(this.getMessageFn.get(), count, this.cursorPos, true);
      this.resetSelectionIfNeeded(selecting);
   }

   public void removeFromCursor(final int count, final TextFieldHelper.CursorStep scope) {
      switch (scope) {
         case CHARACTER:
            this.removeCharsFromCursor(count);
            break;
         case WORD:
            this.removeWordsFromCursor(count);
      }
   }

   public void removeWordsFromCursor(final int count) {
      int wordPosition = StringSplitter.getWordPosition(this.getMessageFn.get(), count, this.cursorPos, true);
      this.removeCharsFromCursor(wordPosition - this.cursorPos);
   }

   public void removeCharsFromCursor(final int count) {
      String message = this.getMessageFn.get();
      if (!message.isEmpty()) {
         String newMessage;
         if (this.selectionPos != this.cursorPos) {
            newMessage = this.deleteSelection(message);
         } else {
            int otherPos = Util.offsetByCodepoints(message, this.cursorPos, count);
            int start = Math.min(otherPos, this.cursorPos);
            int end = Math.max(otherPos, this.cursorPos);
            newMessage = new StringBuilder(message).delete(start, end).toString();
            if (count < 0) {
               this.selectionPos = this.cursorPos = start;
            }
         }

         this.setMessageFn.accept(newMessage);
      }
   }

   public void cut() {
      String message = this.getMessageFn.get();
      this.setClipboardFn.accept(this.getSelected(message));
      this.setMessageFn.accept(this.deleteSelection(message));
   }

   public void paste() {
      this.insertText(this.getMessageFn.get(), this.getClipboardFn.get());
      this.selectionPos = this.cursorPos;
   }

   public void copy() {
      this.setClipboardFn.accept(this.getSelected(this.getMessageFn.get()));
   }

   public void selectAll() {
      this.selectionPos = 0;
      this.cursorPos = this.getMessageFn.get().length();
   }

   private String getSelected(final String text) {
      int startIndex = Math.min(this.cursorPos, this.selectionPos);
      int endIndex = Math.max(this.cursorPos, this.selectionPos);
      return text.substring(startIndex, endIndex);
   }

   private String deleteSelection(final String message) {
      if (this.selectionPos == this.cursorPos) {
         return message;
      }

      int startIndex = Math.min(this.cursorPos, this.selectionPos);
      int endIndex = Math.max(this.cursorPos, this.selectionPos);
      String updatedText = message.substring(0, startIndex) + message.substring(endIndex);
      this.selectionPos = this.cursorPos = startIndex;
      return updatedText;
   }

   public void setCursorToStart() {
      this.setCursorToStart(false);
   }

   public void setCursorToStart(final boolean selecting) {
      this.cursorPos = 0;
      this.resetSelectionIfNeeded(selecting);
   }

   public void setCursorToEnd() {
      this.setCursorToEnd(false);
   }

   public void setCursorToEnd(final boolean selecting) {
      this.cursorPos = this.getMessageFn.get().length();
      this.resetSelectionIfNeeded(selecting);
   }

   public int getCursorPos() {
      return this.cursorPos;
   }

   public void setCursorPos(final int value) {
      this.setCursorPos(value, true);
   }

   public void setCursorPos(final int value, final boolean selecting) {
      this.cursorPos = this.clampToMsgLength(value);
      this.resetSelectionIfNeeded(selecting);
   }

   public int getSelectionPos() {
      return this.selectionPos;
   }

   public void setSelectionPos(final int value) {
      this.selectionPos = this.clampToMsgLength(value);
   }

   public void setSelectionRange(final int start, final int end) {
      int maxSize = this.getMessageFn.get().length();
      this.cursorPos = Mth.clamp(start, 0, maxSize);
      this.selectionPos = Mth.clamp(end, 0, maxSize);
   }

   public boolean isSelecting() {
      return this.cursorPos != this.selectionPos;
   }

   public enum CursorStep {
      CHARACTER,
      WORD;
   }
}
