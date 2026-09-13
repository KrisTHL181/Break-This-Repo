package net.minecraft.client.gui.navigation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FocusNavigationEvent {
   ScreenDirection getVerticalDirectionForInitialFocus();

   @OnlyIn(Dist.CLIENT)
   record ArrowNavigation(ScreenDirection direction) implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return this.direction.getAxis() == ScreenAxis.VERTICAL ? this.direction : ScreenDirection.DOWN;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class InitialFocus implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return ScreenDirection.DOWN;
      }
   }

   @OnlyIn(Dist.CLIENT)
   record TabNavigation(boolean forward) implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return this.forward ? ScreenDirection.DOWN : ScreenDirection.UP;
      }
   }
}
