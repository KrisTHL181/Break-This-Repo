package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class BelowOrAboveWidgetTooltipPositioner implements ClientTooltipPositioner {
   private final ScreenRectangle screenRectangle;

   public BelowOrAboveWidgetTooltipPositioner(ScreenRectangle p_312932_) {
      this.screenRectangle = p_312932_;
   }

   @Override
   public Vector2ic positionTooltip(int p_282513_, int p_281649_, int p_283308_, int p_282740_, int p_281398_, int p_283404_) {
      Vector2i vector2i = new Vector2i();
      vector2i.x = this.screenRectangle.left() + 3;
      vector2i.y = this.screenRectangle.bottom() + 3 + 1;
      if (vector2i.y + p_283404_ + 3 > p_281649_) {
         vector2i.y = this.screenRectangle.top() - p_283404_ - 3 - 1;
      }

      if (vector2i.x + p_281398_ > p_282513_) {
         vector2i.x = Math.max(this.screenRectangle.right() - p_281398_ - 3, 4);
      }

      return vector2i;
   }
}
