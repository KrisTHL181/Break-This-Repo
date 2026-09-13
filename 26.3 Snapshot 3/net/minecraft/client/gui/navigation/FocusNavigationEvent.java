package net.minecraft.client.gui.navigation;

import org.jspecify.annotations.Nullable;

public interface FocusNavigationEvent {
   ScreenDirection getVerticalDirectionForInitialFocus();

   record ArrowNavigation(ScreenDirection direction, @Nullable ScreenRectangle previousFocus) implements FocusNavigationEvent {
      public ArrowNavigation(final ScreenDirection direction) {
         this(direction, null);
      }

      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return this.direction.getAxis() == ScreenAxis.VERTICAL ? this.direction : ScreenDirection.DOWN;
      }

      public FocusNavigationEvent.ArrowNavigation with(final ScreenRectangle previousFocus) {
         return new FocusNavigationEvent.ArrowNavigation(this.direction(), previousFocus);
      }
   }

   class InitialFocus implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return ScreenDirection.DOWN;
      }
   }

   record TabNavigation(boolean forward) implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return this.forward ? ScreenDirection.DOWN : ScreenDirection.UP;
      }
   }
}
