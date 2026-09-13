package net.minecraft.client.gui.components.events;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.TabOrderedElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface GuiEventListener extends TabOrderedElement {
   default void mouseMoved(double p_94758_, double p_94759_) {
   }

   default boolean mouseClicked(MouseButtonEvent p_427106_, boolean p_426427_) {
      return false;
   }

   default boolean mouseReleased(MouseButtonEvent p_427516_) {
      return false;
   }

   default boolean mouseDragged(MouseButtonEvent p_431092_, double p_94740_, double p_94741_) {
      return false;
   }

   default boolean mouseScrolled(double p_94734_, double p_94735_, double p_94736_, double p_299312_) {
      return false;
   }

   default boolean keyPressed(KeyEvent p_428193_) {
      return false;
   }

   default boolean keyReleased(KeyEvent p_429995_) {
      return false;
   }

   default boolean charTyped(CharacterEvent p_428132_) {
      return false;
   }

   default @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_265234_) {
      return null;
   }

   default boolean isMouseOver(double p_94748_, double p_94749_) {
      return false;
   }

   void setFocused(boolean var1);

   boolean isFocused();

   default boolean shouldTakeFocusAfterInteraction() {
      return true;
   }

   default @Nullable ComponentPath getCurrentFocusPath() {
      return this.isFocused() ? ComponentPath.leaf(this) : null;
   }

   default ScreenRectangle getRectangle() {
      return ScreenRectangle.empty();
   }

   default ScreenRectangle getBorderForArrowNavigation(ScreenDirection p_376316_) {
      return this.getRectangle().getBorder(p_376316_);
   }
}
