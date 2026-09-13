package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class MenuTooltipPositioner implements ClientTooltipPositioner {
   private static final int MARGIN = 5;
   private static final int MOUSE_OFFSET_X = 12;
   public static final int MAX_OVERLAP_WITH_WIDGET = 3;
   public static final int MAX_DISTANCE_TO_WIDGET = 5;
   private final ScreenRectangle screenRectangle;

   public MenuTooltipPositioner(final ScreenRectangle screenRectangle) {
      this.screenRectangle = screenRectangle;
   }

   @Override
   public Vector2ic positionTooltip(final int screenWidth, final int screenHeight, final int x, final int y, final int tooltipWidth, final int tooltipHeight) {
      Vector2i result = new Vector2i(x + 12, y);
      if (result.x + tooltipWidth > screenWidth - 5) {
         result.x = Math.max(x - 12 - tooltipWidth, 9);
      }

      result.y += 3;
      int paddedHeight = tooltipHeight + 3 + 3;
      int lowestPossibleY = this.screenRectangle.bottom() + 3 + getOffset(0, 0, this.screenRectangle.height());
      int maxY = screenHeight - 5;
      if (lowestPossibleY + paddedHeight <= maxY) {
         result.y = result.y + getOffset(result.y, this.screenRectangle.top(), this.screenRectangle.height());
      } else {
         result.y = result.y - (paddedHeight + getOffset(result.y, this.screenRectangle.bottom(), this.screenRectangle.height()));
      }

      return result;
   }

   private static int getOffset(final int mouseY, final int widgetY, final int widgetHeight) {
      int distance = Math.min(Math.abs(mouseY - widgetY), widgetHeight);
      return Math.round(Mth.lerp((float)distance / widgetHeight, widgetHeight - 3, 5.0F));
   }
}
