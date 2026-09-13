package net.minecraft.client.gui.screens.inventory.tooltip;

import org.joml.Vector2i;
import org.joml.Vector2ic;

public class DefaultTooltipPositioner implements ClientTooltipPositioner {
   public static final ClientTooltipPositioner INSTANCE = new DefaultTooltipPositioner();

   private DefaultTooltipPositioner() {
   }

   @Override
   public Vector2ic positionTooltip(final int screenWidth, final int screenHeight, final int x, final int y, final int tooltipWidth, final int tooltipHeight) {
      Vector2i result = new Vector2i(x, y).add(12, -12);
      this.positionTooltip(screenWidth, screenHeight, result, tooltipWidth, tooltipHeight);
      return result;
   }

   private void positionTooltip(final int screenWidth, final int screenHeight, final Vector2i result, final int tooltipWidth, final int tooltipHeight) {
      if (result.x + tooltipWidth > screenWidth) {
         result.x = Math.max(result.x - 24 - tooltipWidth, 4);
      }

      int paddedHeight = tooltipHeight + 3;
      if (result.y + paddedHeight > screenHeight) {
         result.y = screenHeight - paddedHeight;
      }
   }
}
