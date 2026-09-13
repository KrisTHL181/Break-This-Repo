package net.minecraft.client.gui.components;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EditBox extends AbstractWidget {
   private static final WidgetSprites SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("widget/text_field"), Identifier.withDefaultNamespace("widget/text_field_highlighted")
   );
   public static final int BACKWARDS = -1;
   public static final int FORWARDS = 1;
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   public static final int DEFAULT_TEXT_COLOR = -2039584;
   public static final Style DEFAULT_HINT_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
   public static final Style SEARCH_HINT_STYLE = Style.EMPTY.applyFormats(ChatFormatting.GRAY, ChatFormatting.ITALIC);
   private static final int CURSOR_BLINK_INTERVAL_MS = 300;
   private final Font font;
   private String value = "";
   private int maxLength = 32;
   private boolean bordered = true;
   private boolean canLoseFocus = true;
   private boolean isEditable = true;
   private boolean centered = false;
   private boolean textShadow = true;
   private boolean invertHighlightedTextColor = true;
   private int displayPos;
   private int cursorPos;
   private int highlightPos;
   private int textColor = -2039584;
   private int textColorUneditable = -9408400;
   private @Nullable String suggestion;
   private @Nullable Consumer<String> responder;
   private Predicate<String> filter = Objects::nonNull;
   private final List<EditBox.TextFormatter> formatters = new ArrayList<>();
   private @Nullable Component hint;
   private long focusedTime = Util.getMillis();
   private int textX;
   private int textY;

   public EditBox(Font p_299161_, int p_299570_, int p_297565_, Component p_300284_) {
      this(p_299161_, 0, 0, p_299570_, p_297565_, p_300284_);
   }

   public EditBox(Font p_94114_, int p_94115_, int p_94116_, int p_94117_, int p_94118_, Component p_94119_) {
      this(p_94114_, p_94115_, p_94116_, p_94117_, p_94118_, null, p_94119_);
   }

   public EditBox(Font p_94106_, int p_94107_, int p_94108_, int p_94109_, int p_94110_, @Nullable EditBox p_94111_, Component p_94112_) {
      super(p_94107_, p_94108_, p_94109_, p_94110_, p_94112_);
      this.font = p_94106_;
      if (p_94111_ != null) {
         this.setValue(p_94111_.getValue());
      }

      this.updateTextPosition();
   }

   public void setResponder(Consumer<String> p_94152_) {
      this.responder = p_94152_;
   }

   public void addFormatter(EditBox.TextFormatter p_426158_) {
      this.formatters.add(p_426158_);
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      Component component = this.getMessage();
      return Component.translatable("gui.narrate.editBox", component, this.value);
   }

   public void setValue(String p_94145_) {
      if (this.filter.test(p_94145_)) {
         if (p_94145_.length() > this.maxLength) {
            this.value = p_94145_.substring(0, this.maxLength);
         } else {
            this.value = p_94145_;
         }

         this.moveCursorToEnd(false);
         this.setHighlightPos(this.cursorPos);
         this.onValueChange(p_94145_);
      }
   }

   public String getValue() {
      return this.value;
   }

   public String getHighlighted() {
      int i = Math.min(this.cursorPos, this.highlightPos);
      int j = Math.max(this.cursorPos, this.highlightPos);
      return this.value.substring(i, j);
   }

   @Override
   public void setX(int p_409307_) {
      super.setX(p_409307_);
      this.updateTextPosition();
   }

   @Override
   public void setY(int p_409179_) {
      super.setY(p_409179_);
      this.updateTextPosition();
   }

   public void setFilter(Predicate<String> p_94154_) {
      this.filter = p_94154_;
   }

   public void insertText(String p_94165_) {
      int i = Math.min(this.cursorPos, this.highlightPos);
      int j = Math.max(this.cursorPos, this.highlightPos);
      int k = this.maxLength - this.value.length() - (i - j);
      if (k > 0) {
         String s = StringUtil.filterText(p_94165_);
         int l = s.length();
         if (k < l) {
            if (Character.isHighSurrogate(s.charAt(k - 1))) {
               k--;
            }

            s = s.substring(0, k);
            l = k;
         }

         String s1 = new StringBuilder(this.value).replace(i, j, s).toString();
         if (this.filter.test(s1)) {
            this.value = s1;
            this.setCursorPosition(i + l);
            this.setHighlightPos(this.cursorPos);
            this.onValueChange(this.value);
         }
      }
   }

   private void onValueChange(String p_94175_) {
      if (this.responder != null) {
         this.responder.accept(p_94175_);
      }

      this.updateTextPosition();
   }

   private void deleteText(int p_94218_, boolean p_425735_) {
      if (p_425735_) {
         this.deleteWords(p_94218_);
      } else {
         this.deleteChars(p_94218_);
      }
   }

   public void deleteWords(int p_94177_) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            this.deleteCharsToPos(this.getWordPosition(p_94177_));
         }
      }
   }

   public void deleteChars(int p_94181_) {
      this.deleteCharsToPos(this.getCursorPos(p_94181_));
   }

   public void deleteCharsToPos(int p_310763_) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            int i = Math.min(p_310763_, this.cursorPos);
            int j = Math.max(p_310763_, this.cursorPos);
            if (i != j) {
               String s = new StringBuilder(this.value).delete(i, j).toString();
               if (this.filter.test(s)) {
                  this.value = s;
                  this.moveCursorTo(i, false);
               }
            }
         }
      }
   }

   public int getWordPosition(int p_94185_) {
      return this.getWordPosition(p_94185_, this.getCursorPosition());
   }

   private int getWordPosition(int p_94129_, int p_94130_) {
      return this.getWordPosition(p_94129_, p_94130_, true);
   }

   private int getWordPosition(int p_94141_, int p_94142_, boolean p_94143_) {
      int i = p_94142_;
      boolean flag = p_94141_ < 0;
      int j = Math.abs(p_94141_);

      for (int k = 0; k < j; k++) {
         if (!flag) {
            int l = this.value.length();
            i = this.value.indexOf(32, i);
            if (i == -1) {
               i = l;
            } else {
               while (p_94143_ && i < l && this.value.charAt(i) == ' ') {
                  i++;
               }
            }
         } else {
            while (p_94143_ && i > 0 && this.value.charAt(i - 1) == ' ') {
               i--;
            }

            while (i > 0 && this.value.charAt(i - 1) != ' ') {
               i--;
            }
         }
      }

      return i;
   }

   public void moveCursor(int p_94189_, boolean p_297286_) {
      this.moveCursorTo(this.getCursorPos(p_94189_), p_297286_);
   }

   private int getCursorPos(int p_94221_) {
      return Util.offsetByCodepoints(this.value, this.cursorPos, p_94221_);
   }

   public void moveCursorTo(int p_94193_, boolean p_300521_) {
      this.setCursorPosition(p_94193_);
      if (!p_300521_) {
         this.setHighlightPos(this.cursorPos);
      }

      this.onValueChange(this.value);
   }

   public void setCursorPosition(int p_94197_) {
      this.cursorPos = Mth.clamp(p_94197_, 0, this.value.length());
      this.scrollTo(this.cursorPos);
   }

   public void moveCursorToStart(boolean p_299543_) {
      this.moveCursorTo(0, p_299543_);
   }

   public void moveCursorToEnd(boolean p_297711_) {
      this.moveCursorTo(this.value.length(), p_297711_);
   }

   @Override
   public boolean keyPressed(KeyEvent p_424460_) {
      if (this.isActive() && this.isFocused()) {
         switch (p_424460_.key()) {
            case 259:
               if (this.isEditable) {
                  this.deleteText(-1, p_424460_.hasControlDownWithQuirk());
               }

               return true;
            case 260:
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               if (p_424460_.isSelectAll()) {
                  this.moveCursorToEnd(false);
                  this.setHighlightPos(0);
                  return true;
               } else if (p_424460_.isCopy()) {
                  Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                  return true;
               } else if (p_424460_.isPaste()) {
                  if (this.isEditable()) {
                     this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                  }

                  return true;
               } else {
                  if (p_424460_.isCut()) {
                     Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                     if (this.isEditable()) {
                        this.insertText("");
                     }

                     return true;
                  }

                  return false;
               }
            case 261:
               if (this.isEditable) {
                  this.deleteText(1, p_424460_.hasControlDownWithQuirk());
               }

               return true;
            case 262:
               if (p_424460_.hasControlDownWithQuirk()) {
                  this.moveCursorTo(this.getWordPosition(1), p_424460_.hasShiftDown());
               } else {
                  this.moveCursor(1, p_424460_.hasShiftDown());
               }

               return true;
            case 263:
               if (p_424460_.hasControlDownWithQuirk()) {
                  this.moveCursorTo(this.getWordPosition(-1), p_424460_.hasShiftDown());
               } else {
                  this.moveCursor(-1, p_424460_.hasShiftDown());
               }

               return true;
            case 268:
               this.moveCursorToStart(p_424460_.hasShiftDown());
               return true;
            case 269:
               this.moveCursorToEnd(p_424460_.hasShiftDown());
               return true;
         }
      } else {
         return false;
      }
   }

   public boolean canConsumeInput() {
      return this.isActive() && this.isFocused() && this.isEditable();
   }

   @Override
   public boolean charTyped(CharacterEvent p_426447_) {
      if (!this.canConsumeInput()) {
         return false;
      }

      if (p_426447_.isAllowedChatCharacter()) {
         if (this.isEditable) {
            this.insertText(p_426447_.codepointAsString());
         }

         return true;
      } else {
         return false;
      }
   }

   private int findClickedPositionInText(MouseButtonEvent p_423300_) {
      int i = Math.min(Mth.floor(p_423300_.x()) - this.textX, this.getInnerWidth());
      String s = this.value.substring(this.displayPos);
      return this.displayPos + this.font.plainSubstrByWidth(s, i).length();
   }

   private void selectWord(MouseButtonEvent p_425345_) {
      int i = this.findClickedPositionInText(p_425345_);
      int j = this.getWordPosition(-1, i);
      int k = this.getWordPosition(1, i);
      this.moveCursorTo(j, false);
      this.moveCursorTo(k, true);
   }

   @Override
   public void onClick(MouseButtonEvent p_424070_, boolean p_426154_) {
      if (p_426154_) {
         this.selectWord(p_424070_);
      } else {
         this.moveCursorTo(this.findClickedPositionInText(p_424070_), p_424070_.hasShiftDown());
      }
   }

   @Override
   protected void onDrag(MouseButtonEvent p_423108_, double p_426806_, double p_423936_) {
      this.moveCursorTo(this.findClickedPositionInText(p_423108_), true);
   }

   @Override
   public void playDownSound(SoundManager p_279245_) {
   }

   @Override
   public void renderWidget(GuiGraphics p_283252_, int p_281594_, int p_282100_, float p_283101_) {
      if (this.isVisible()) {
         if (this.isBordered()) {
            Identifier identifier = SPRITES.get(this.isActive(), this.isFocused());
            p_283252_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.getWidth(), this.getHeight());
         }

         int i1 = this.isEditable ? this.textColor : this.textColorUneditable;
         int i = this.cursorPos - this.displayPos;
         String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
         boolean flag = i >= 0 && i <= s.length();
         boolean flag1 = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L && flag;
         int j = this.textX;
         int k = Mth.clamp(this.highlightPos - this.displayPos, 0, s.length());
         if (!s.isEmpty()) {
            String s1 = flag ? s.substring(0, i) : s;
            FormattedCharSequence formattedcharsequence = this.applyFormat(s1, this.displayPos);
            p_283252_.drawString(this.font, formattedcharsequence, j, this.textY, i1, this.textShadow);
            j += this.font.width(formattedcharsequence) + 1;
         }

         boolean flag2 = this.cursorPos < this.value.length() || this.value.length() >= this.getMaxLength();
         int j1 = j;
         if (!flag) {
            j1 = i > 0 ? this.textX + this.width : this.textX;
         } else if (flag2) {
            j1 = j - 1;
            j--;
         }

         if (!s.isEmpty() && flag && i < s.length()) {
            p_283252_.drawString(this.font, this.applyFormat(s.substring(i), this.cursorPos), j, this.textY, i1, this.textShadow);
         }

         if (this.hint != null && s.isEmpty() && !this.isFocused()) {
            p_283252_.drawString(this.font, this.hint, j, this.textY, i1);
         }

         if (!flag2 && this.suggestion != null) {
            p_283252_.drawString(this.font, this.suggestion, j1 - 1, this.textY, -8355712, this.textShadow);
         }

         if (k != i) {
            int l = this.textX + this.font.width(s.substring(0, k));
            p_283252_.textHighlight(
               Math.min(j1, this.getX() + this.width),
               this.textY - 1,
               Math.min(l - 1, this.getX() + this.width),
               this.textY + 1 + 9,
               this.invertHighlightedTextColor
            );
         }

         if (flag1) {
            if (flag2) {
               p_283252_.fill(j1, this.textY - 1, j1 + 1, this.textY + 1 + 9, i1);
            } else {
               p_283252_.drawString(this.font, "_", j1, this.textY, i1, this.textShadow);
            }
         }

         if (this.isHovered()) {
            p_283252_.requestCursor(this.isEditable() ? CursorTypes.IBEAM : CursorTypes.NOT_ALLOWED);
         }
      }
   }

   private FormattedCharSequence applyFormat(String p_428392_, int p_430793_) {
      for (EditBox.TextFormatter editbox$textformatter : this.formatters) {
         FormattedCharSequence formattedcharsequence = editbox$textformatter.format(p_428392_, p_430793_);
         if (formattedcharsequence != null) {
            return formattedcharsequence;
         }
      }

      return FormattedCharSequence.forward(p_428392_, Style.EMPTY);
   }

   private void updateTextPosition() {
      if (this.font != null) {
         String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
         this.textX = this.getX() + (this.isCentered() ? (this.getWidth() - this.font.width(s)) / 2 : (this.bordered ? 4 : 0));
         this.textY = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
      }
   }

   public void setMaxLength(int p_94200_) {
      this.maxLength = p_94200_;
      if (this.value.length() > p_94200_) {
         this.value = this.value.substring(0, p_94200_);
         this.onValueChange(this.value);
      }
   }

   private int getMaxLength() {
      return this.maxLength;
   }

   public int getCursorPosition() {
      return this.cursorPos;
   }

   public boolean isBordered() {
      return this.bordered;
   }

   public void setBordered(boolean p_94183_) {
      this.bordered = p_94183_;
      this.updateTextPosition();
   }

   public void setTextColor(int p_94203_) {
      this.textColor = p_94203_;
   }

   public void setTextColorUneditable(int p_94206_) {
      this.textColorUneditable = p_94206_;
   }

   @Override
   public void setFocused(boolean p_265520_) {
      if (this.canLoseFocus || p_265520_) {
         super.setFocused(p_265520_);
         if (p_265520_) {
            this.focusedTime = Util.getMillis();
         }
      }
   }

   private boolean isEditable() {
      return this.isEditable;
   }

   public void setEditable(boolean p_94187_) {
      this.isEditable = p_94187_;
   }

   private boolean isCentered() {
      return this.centered;
   }

   public void setCentered(boolean p_407491_) {
      this.centered = p_407491_;
      this.updateTextPosition();
   }

   public void setTextShadow(boolean p_410170_) {
      this.textShadow = p_410170_;
   }

   public void setInvertHighlightedTextColor(boolean p_459135_) {
      this.invertHighlightedTextColor = p_459135_;
   }

   public int getInnerWidth() {
      return this.isBordered() ? this.width - 8 : this.width;
   }

   public void setHighlightPos(int p_94209_) {
      this.highlightPos = Mth.clamp(p_94209_, 0, this.value.length());
      this.scrollTo(this.highlightPos);
   }

   private void scrollTo(int p_299591_) {
      if (this.font != null) {
         this.displayPos = Math.min(this.displayPos, this.value.length());
         int i = this.getInnerWidth();
         String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), i);
         int j = s.length() + this.displayPos;
         if (p_299591_ == this.displayPos) {
            this.displayPos = this.displayPos - this.font.plainSubstrByWidth(this.value, i, true).length();
         }

         if (p_299591_ > j) {
            this.displayPos += p_299591_ - j;
         } else if (p_299591_ <= this.displayPos) {
            this.displayPos = this.displayPos - (this.displayPos - p_299591_);
         }

         this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
      }
   }

   public void setCanLoseFocus(boolean p_94191_) {
      this.canLoseFocus = p_94191_;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean p_94195_) {
      this.visible = p_94195_;
   }

   public void setSuggestion(@Nullable String p_94168_) {
      this.suggestion = p_94168_;
   }

   public int getScreenX(int p_94212_) {
      return p_94212_ > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, p_94212_));
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_259237_) {
      p_259237_.add(NarratedElementType.TITLE, this.createNarrationMessage());
   }

   public void setHint(Component p_259584_) {
      boolean flag = p_259584_.getStyle().equals(Style.EMPTY);
      this.hint = flag ? p_259584_.copy().withStyle(DEFAULT_HINT_STYLE) : p_259584_;
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface TextFormatter {
      @Nullable FormattedCharSequence format(String var1, int var2);
   }
}
