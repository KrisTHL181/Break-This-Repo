package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AlertScreen extends Screen {
   private static final int LABEL_Y = 90;
   private final Component messageText;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   private final Runnable callback;
   private final Component okButton;
   private final boolean shouldCloseOnEsc;

   public AlertScreen(final Runnable callback, final Component title, final Component messageText) {
      this(callback, title, messageText, CommonComponents.GUI_BACK, true);
   }

   public AlertScreen(final Runnable callback, final Component title, final Component messageText, final Component okButton, final boolean shouldCloseOnEsc) {
      super(title);
      this.callback = callback;
      this.messageText = messageText;
      this.okButton = okButton;
      this.shouldCloseOnEsc = shouldCloseOnEsc;
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.messageText);
   }

   @Override
   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.messageText, this.width - 50);
      int textHeight = this.message.getLineCount() * 9;
      int buttonY = Mth.clamp(90 + textHeight + 12, this.height / 6 + 96, this.height - 24);
      int buttonWidth = 150;
      this.addRenderableWidget(Button.builder(this.okButton, button -> this.callback.run()).bounds((this.width - 150) / 2, buttonY, 150, 20).build());
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      ActiveTextCollector textRenderer = graphics.textRenderer();
      graphics.centeredText(this.font, this.title, this.width / 2, 70, -1);
      this.message.visitLines(TextAlignment.CENTER, this.width / 2, 90, 9, textRenderer);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return this.shouldCloseOnEsc;
   }
}
