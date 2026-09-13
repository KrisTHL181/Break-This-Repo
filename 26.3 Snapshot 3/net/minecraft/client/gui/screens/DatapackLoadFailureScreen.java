package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class DatapackLoadFailureScreen extends Screen {
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   private final Runnable cancelCallback;
   private final Runnable safeModeCallback;

   public DatapackLoadFailureScreen(final Runnable cancelCallback, final Runnable safeModeCallback) {
      super(Component.translatable("datapackFailure.title"));
      this.cancelCallback = cancelCallback;
      this.safeModeCallback = safeModeCallback;
   }

   @Override
   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.getTitle(), this.width - 50);
      this.addRenderableWidget(
         Button.builder(Component.translatable("datapackFailure.safeMode"), button -> this.safeModeCallback.run())
            .bounds(this.width / 2 - 155, this.height / 6 + 96, 150, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CommonComponents.GUI_BACK, button -> this.cancelCallback.run())
            .bounds(this.width / 2 - 155 + 160, this.height / 6 + 96, 150, 20)
            .build()
      );
   }

   @Override
   public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
      super.extractRenderState(graphics, mouseX, mouseY, a);
      ActiveTextCollector textRenderer = graphics.textRenderer();
      this.message.visitLines(TextAlignment.CENTER, this.width / 2, 70, 9, textRenderer);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }
}
