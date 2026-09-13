package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AlertScreen extends Screen {
   private static final int LABEL_Y = 90;
   private final Component messageText;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   private final Runnable callback;
   private final Component okButton;
   private final boolean shouldCloseOnEsc;

   public AlertScreen(Runnable p_95519_, Component p_95520_, Component p_95521_) {
      this(p_95519_, p_95520_, p_95521_, CommonComponents.GUI_BACK, true);
   }

   public AlertScreen(Runnable p_239327_, Component p_239328_, Component p_239329_, Component p_239330_, boolean p_239331_) {
      super(p_239328_);
      this.callback = p_239327_;
      this.messageText = p_239329_;
      this.okButton = p_239330_;
      this.shouldCloseOnEsc = p_239331_;
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.messageText);
   }

   @Override
   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.messageText, this.width - 50);
      int i = this.message.getLineCount() * 9;
      int j = Mth.clamp(90 + i + 12, this.height / 6 + 96, this.height - 24);
      int k = 150;
      this.addRenderableWidget(Button.builder(this.okButton, p_95533_ -> this.callback.run()).bounds((this.width - 150) / 2, j, 150, 20).build());
   }

   @Override
   public void render(GuiGraphics p_281989_, int p_281583_, int p_282152_, float p_282198_) {
      super.render(p_281989_, p_281583_, p_282152_, p_282198_);
      ActiveTextCollector activetextcollector = p_281989_.textRenderer();
      p_281989_.drawCenteredString(this.font, this.title, this.width / 2, 70, -1);
      this.message.visitLines(TextAlignment.CENTER, this.width / 2, 90, 9, activetextcollector);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return this.shouldCloseOnEsc;
   }
}
