package net.minecraft.client.gui.contextualbar;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ContextualBarRenderer {
   int WIDTH = 182;
   int HEIGHT = 5;
   int MARGIN_BOTTOM = 24;
   ContextualBarRenderer EMPTY = new ContextualBarRenderer() {
      @Override
      public void renderBackground(GuiGraphics p_408134_, DeltaTracker p_409772_) {
      }

      @Override
      public void render(GuiGraphics p_409095_, DeltaTracker p_407067_) {
      }
   };

   default int left(Window p_408807_) {
      return (p_408807_.getGuiScaledWidth() - 182) / 2;
   }

   default int top(Window p_406016_) {
      return p_406016_.getGuiScaledHeight() - 24 - 5;
   }

   void renderBackground(GuiGraphics var1, DeltaTracker var2);

   void render(GuiGraphics var1, DeltaTracker var2);

   static void renderExperienceLevel(GuiGraphics p_409597_, Font p_409104_, int p_410321_) {
      Component component = Component.translatable("gui.experience.level", p_410321_);
      int i = (p_409597_.guiWidth() - p_409104_.width(component)) / 2;
      int j = p_409597_.guiHeight() - 24 - 9 - 2;
      p_409597_.drawString(p_409104_, component, i + 1, j, -16777216, false);
      p_409597_.drawString(p_409104_, component, i - 1, j, -16777216, false);
      p_409597_.drawString(p_409104_, component, i, j + 1, -16777216, false);
      p_409597_.drawString(p_409104_, component, i, j - 1, -16777216, false);
      p_409597_.drawString(p_409104_, component, i, j, -8323296, false);
   }
}
