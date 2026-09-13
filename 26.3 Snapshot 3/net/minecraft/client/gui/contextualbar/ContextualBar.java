package net.minecraft.client.gui.contextualbar;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public interface ContextualBar {
   int WIDTH = 182;
   int HEIGHT = 5;
   int MARGIN_BOTTOM = 24;
   ContextualBar EMPTY = new ContextualBar() {
      @Override
      public void extractBackground(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      }

      @Override
      public void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker) {
      }
   };

   default int left(final Window window) {
      return (window.getGuiScaledWidth() - 182) / 2;
   }

   default int top(final Window window) {
      return window.getGuiScaledHeight() - 24 - 5;
   }

   void extractBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

   void extractRenderState(final GuiGraphicsExtractor graphics, final DeltaTracker deltaTracker);

   static void extractExperienceLevel(final GuiGraphicsExtractor graphics, final Font font, final int experienceLevel) {
      Component str = Component.translatable("gui.experience.level", experienceLevel);
      int x = (graphics.guiWidth() - font.width(str)) / 2;
      int y = graphics.guiHeight() - 24 - 9 - 2;
      graphics.text(font, str, x + 1, y, -16777216, false);
      graphics.text(font, str, x - 1, y, -16777216, false);
      graphics.text(font, str, x, y + 1, -16777216, false);
      graphics.text(font, str, x, y - 1, -16777216, false);
      graphics.text(font, str, x, y, -8323296, false);
   }
}
