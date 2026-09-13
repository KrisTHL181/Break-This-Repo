package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class DefaultTooltipPositioner implements ClientTooltipPositioner {
   public static final ClientTooltipPositioner INSTANCE = new DefaultTooltipPositioner();

   private DefaultTooltipPositioner() {
   }

   @Override
   public Vector2ic positionTooltip(int p_281867_, int p_282915_, int p_283108_, int p_282881_, int p_283243_, int p_282104_) {
      Vector2i vector2i = new Vector2i(p_283108_, p_282881_).add(12, -12);
      this.positionTooltip(p_281867_, p_282915_, vector2i, p_283243_, p_282104_);
      return vector2i;
   }

   private void positionTooltip(int p_282431_, int p_282309_, Vector2i p_282004_, int p_283148_, int p_281715_) {
      if (p_282004_.x + p_283148_ > p_282431_) {
         p_282004_.x = Math.max(p_282004_.x - 24 - p_283148_, 4);
      }

      int i = p_281715_ + 3;
      if (p_282004_.y + i > p_282309_) {
         p_282004_.y = p_282309_ - i;
      }
   }
}
