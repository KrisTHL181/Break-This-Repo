package net.minecraft.client.gui.components;

import java.time.Duration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.inventory.tooltip.BelowOrAboveWidgetTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class WidgetTooltipHolder {
   private @Nullable Tooltip tooltip;
   private Duration delay = Duration.ZERO;
   private long displayStartTime;
   private boolean wasDisplayed;

   public void setDelay(final Duration delay) {
      this.delay = delay;
   }

   public void set(final @Nullable Tooltip tooltip) {
      this.tooltip = tooltip;
   }

   public @Nullable Tooltip get() {
      return this.tooltip;
   }

   public void refreshTooltipForNextRenderPass(
      final GuiGraphicsExtractor graphics,
      final int mouseX,
      final int mouseY,
      final boolean isHovered,
      final boolean isFocused,
      final ScreenRectangle screenRectangle
   ) {
      if (this.tooltip == null) {
         this.wasDisplayed = false;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         boolean shouldDisplay = isHovered || isFocused && minecraft.getLastInputType().isKeyboard();
         if (shouldDisplay != this.wasDisplayed) {
            if (shouldDisplay) {
               this.displayStartTime = Util.getMillis();
            }

            this.wasDisplayed = shouldDisplay;
         }

         if (shouldDisplay && Util.getMillis() - this.displayStartTime > this.delay.toMillis()) {
            graphics.setTooltipForNextFrame(
               minecraft.font,
               this.tooltip.toCharSequence(minecraft),
               this.tooltip.component(),
               this.createTooltipPositioner(screenRectangle, isHovered, isFocused),
               mouseX,
               mouseY,
               isFocused,
               this.tooltip.style()
            );
         }
      }
   }

   private ClientTooltipPositioner createTooltipPositioner(final ScreenRectangle screenRectangle, final boolean isHovered, final boolean isFocused) {
      return !isHovered && isFocused && Minecraft.getInstance().getLastInputType().isKeyboard()
         ? new BelowOrAboveWidgetTooltipPositioner(screenRectangle)
         : new MenuTooltipPositioner(screenRectangle);
   }

   public void updateNarration(final NarrationElementOutput output) {
      if (this.tooltip != null) {
         this.tooltip.updateNarration(output);
      }
   }
}
