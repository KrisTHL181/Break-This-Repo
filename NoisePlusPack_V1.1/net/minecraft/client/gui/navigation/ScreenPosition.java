package net.minecraft.client.gui.navigation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ScreenPosition(int x, int y) {
   public static ScreenPosition of(ScreenAxis p_265175_, int p_265751_, int p_265120_) {
      return switch (p_265175_) {
         case HORIZONTAL -> new ScreenPosition(p_265751_, p_265120_);
         case VERTICAL -> new ScreenPosition(p_265120_, p_265751_);
      };
   }

   public ScreenPosition step(ScreenDirection p_265084_) {
      return switch (p_265084_) {
         case DOWN -> new ScreenPosition(this.x, this.y + 1);
         case UP -> new ScreenPosition(this.x, this.y - 1);
         case LEFT -> new ScreenPosition(this.x - 1, this.y);
         case RIGHT -> new ScreenPosition(this.x + 1, this.y);
      };
   }

   public int getCoordinate(ScreenAxis p_265656_) {
      return switch (p_265656_) {
         case HORIZONTAL -> this.x;
         case VERTICAL -> this.y;
      };
   }
}
