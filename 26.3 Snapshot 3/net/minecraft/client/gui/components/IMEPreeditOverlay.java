package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class IMEPreeditOverlay implements Renderable {
   private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("widget/preedit");
   private static final Style FOCUSED_STYLE = Style.EMPTY.withUnderlined(true);
   private static final int SEPARATION_FROM_INPUT = 4;
   private static final int BORDER_MARGIN = 4;
   private static final int BORDER_WIDTH = 1;
   private static final int BORDER_OFFSET = 5;
   private static final int TEXT_COLOR = -16777216;
   private static final int HOT_AREA_MARGIN = 2;
   private final Font font;
   private int inputLeft;
   private int inputTop;
   private final int inputHeight;
   private final long initTimeMs;
   private final Component preEditText;
   private final int preEditTextWidth;
   private final int caretPos;

   public IMEPreeditOverlay(final PreeditEvent contents, final Font font, final int inputHeight) {
      this.font = font;
      this.inputHeight = inputHeight;
      this.initTimeMs = Util.getMillis();
      this.preEditText = contents.toFormattedText(FOCUSED_STYLE).withColor(-16777216);
      this.preEditTextWidth = font.width(this.preEditText);
      String textBeforeCaret = contents.fullText().substring(0, contents.caretPosition());
      this.caretPos = font.width(textBeforeCaret);
   }

   public void updateInputPosition(final int inputLeft, final int inputTop) {
      this.inputLeft = inputLeft;
      this.inputTop = inputTop;
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      int preeditLeft = this.inputLeft;
      int preeditRight = preeditLeft + this.preEditTextWidth;
      if (preeditRight > graphics.guiWidth()) {
         preeditLeft = graphics.guiWidth() - this.preEditTextWidth;
         preeditRight = preeditLeft + this.preEditTextWidth;
      }

      int inputBottom = this.inputTop + this.inputHeight;
      int preeditBottom = inputBottom + 4 + 9;
      if (preeditBottom > graphics.guiHeight()) {
         preeditBottom = this.inputTop - 4 - 9;
      }

      int preeditTop = preeditBottom - 9;
      Minecraft.getInstance()
         .textInputManager()
         .setTextInputArea(
            Math.min(preeditLeft, this.inputLeft) - 2, Math.min(preeditTop, this.inputTop) - 2, preeditRight + 2, Math.max(preeditBottom, inputBottom) + 2
         );
      int backgroundWidth = preeditRight - preeditLeft + 10;
      int backgroundHeight = preeditBottom - preeditTop + 10;
      graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, preeditLeft - 5, preeditTop - 5, backgroundWidth, backgroundHeight);
      graphics.text(this.font, this.preEditText, preeditLeft, preeditTop, -16777216, false);
      if (TextCursorUtils.isCursorVisible(Util.getMillis() - this.initTimeMs)) {
         TextCursorUtils.extractInsertCursor(graphics, preeditLeft + this.caretPos, preeditTop, -16777216, 9 + 1);
      }
   }
}
