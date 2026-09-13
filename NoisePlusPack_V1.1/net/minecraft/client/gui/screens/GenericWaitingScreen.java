package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GenericWaitingScreen extends Screen {
   private static final int TITLE_Y = 80;
   private static final int MESSAGE_Y = 120;
   private static final int MESSAGE_MAX_WIDTH = 360;
   private final @Nullable Component messageText;
   private final Component buttonLabel;
   private final Runnable buttonCallback;
   private @Nullable MultiLineLabel message;
   private Button button;
   private int disableButtonTicks;

   public static GenericWaitingScreen createWaiting(Component p_240310_, Component p_240311_, Runnable p_240312_) {
      return new GenericWaitingScreen(p_240310_, null, p_240311_, p_240312_, 0);
   }

   public static GenericWaitingScreen createCompleted(Component p_240291_, Component p_240292_, Component p_240293_, Runnable p_240294_) {
      return new GenericWaitingScreen(p_240291_, p_240292_, p_240293_, p_240294_, 20);
   }

   protected GenericWaitingScreen(Component p_240300_, @Nullable Component p_240301_, Component p_240302_, Runnable p_240303_, int p_240304_) {
      super(p_240300_);
      this.messageText = p_240301_;
      this.buttonLabel = p_240302_;
      this.buttonCallback = p_240303_;
      this.disableButtonTicks = p_240304_;
   }

   @Override
   protected void init() {
      super.init();
      if (this.messageText != null) {
         this.message = MultiLineLabel.create(this.font, this.messageText, 360);
      }

      int i = 150;
      int j = 20;
      int k = this.message != null ? this.message.getLineCount() : 1;
      int l = Math.max(k, 5) * 9;
      int i1 = Math.min(120 + l, this.height - 40);
      this.button = this.addRenderableWidget(Button.builder(this.buttonLabel, p_239908_ -> this.onClose()).bounds((this.width - 150) / 2, i1, 150, 20).build());
   }

   @Override
   public void tick() {
      if (this.disableButtonTicks > 0) {
         this.disableButtonTicks--;
      }

      this.button.active = this.disableButtonTicks == 0;
   }

   @Override
   public void render(GuiGraphics p_283537_, int p_239719_, int p_239720_, float p_239721_) {
      super.render(p_283537_, p_239719_, p_239720_, p_239721_);
      ActiveTextCollector activetextcollector = p_283537_.textRenderer();
      p_283537_.drawCenteredString(this.font, this.title, this.width / 2, 80, -1);
      if (this.message == null) {
         String s = LoadingDotsText.get(Util.getMillis());
         p_283537_.drawCenteredString(this.font, s, this.width / 2, 120, -6250336);
      } else {
         this.message.visitLines(TextAlignment.CENTER, this.width / 2, 120, 9, activetextcollector);
      }
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return this.message != null && this.button.active;
   }

   @Override
   public void onClose() {
      this.buttonCallback.run();
   }

   @Override
   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(this.title, this.messageText != null ? this.messageText : CommonComponents.EMPTY);
   }
}
