package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class BelowOrAboveWidgetTooltipPositioner implements ClientTooltipPositioner {
   private final ScreenRectangle screenRectangle;

   public BelowOrAboveWidgetTooltipPositioner(final ScreenRectangle screenRectangle) {
      this.screenRectangle = screenRectangle;
   }

   @Override
   public Vector2ic positionTooltip(final int screenWidth, final int screenHeight, final int x, final int y, final int tooltipWidth, final int tooltipHeight) {
      Vector2i result = new Vector2i();
      result.x = this.screenRectangle.left() + 3;
      result.y = this.screenRectangle.bottom() + 3 + 1;
      if (result.y + tooltipHeight + 3 > screenHeight) {
         result.y = this.screenRectangle.top() - tooltipHeight - 3 - 1;
      }

      if (result.x + tooltipWidth > screenWidth) {
         result.x = Math.max(this.screenRectangle.right() - tooltipWidth - 3, 4);
      }

      return result;
   }
}
