package net.minecraft.client.gui.components.events;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;

public interface GuiEventListener extends TabOrderedElement {
   default void mouseMoved(final double x, final double y) {
   }

   default boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
      return false;
   }

   default boolean mouseReleased(final MouseButtonEvent event) {
      return false;
   }

   default boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
      return false;
   }

   default boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
      return false;
   }

   default boolean keyPressed(final KeyEvent event) {
      return false;
   }

   default boolean keyReleased(final KeyEvent event) {
      return false;
   }

   default boolean charTyped(final CharacterEvent event) {
      return false;
   }

   default boolean preeditUpdated(final @Nullable PreeditEvent event) {
      return false;
   }

   default @Nullable ComponentPath nextFocusPath(final FocusNavigationEvent navigationEvent) {
      return null;
   }

   default boolean isMouseOver(final double mouseX, final double mouseY) {
      return false;
   }

   void setFocused(final boolean focused);

   boolean isFocused();

   default boolean shouldTakeFocusAfterInteraction() {
      return true;
   }

   default boolean capturesInput() {
      return false;
   }

   default @Nullable ComponentPath getCurrentFocusPath() {
      return this.isFocused() ? ComponentPath.leaf(this) : null;
   }

   default ScreenRectangle getRectangle() {
      return ScreenRectangle.empty();
   }

   default ScreenRectangle getBorderForArrowNavigation(final ScreenDirection opposite) {
      return this.getRectangle().getBorder(opposite);
   }
}
