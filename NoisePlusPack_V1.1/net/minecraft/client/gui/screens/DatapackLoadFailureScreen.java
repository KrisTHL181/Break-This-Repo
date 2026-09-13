package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DatapackLoadFailureScreen extends Screen {
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   private final Runnable cancelCallback;
   private final Runnable safeModeCallback;

   public DatapackLoadFailureScreen(Runnable p_95894_, Runnable p_309481_) {
      super(Component.translatable("datapackFailure.title"));
      this.cancelCallback = p_95894_;
      this.safeModeCallback = p_309481_;
   }

   @Override
   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.getTitle(), this.width - 50);
      this.addRenderableWidget(
         Button.builder(Component.translatable("datapackFailure.safeMode"), p_308195_ -> this.safeModeCallback.run())
            .bounds(this.width / 2 - 155, this.height / 6 + 96, 150, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, p_308194_ -> this.cancelCallback.run())
            .bounds(this.width / 2 - 155 + 160, this.height / 6 + 96, 150, 20)
            .build()
      );
   }

   @Override
   public void render(GuiGraphics p_283519_, int p_282196_, int p_283357_, float p_283026_) {
      super.render(p_283519_, p_282196_, p_283357_, p_283026_);
      ActiveTextCollector activetextcollector = p_283519_.textRenderer();
      this.message.visitLines(TextAlignment.CENTER, this.width / 2, 70, 9, activetextcollector);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }
}
