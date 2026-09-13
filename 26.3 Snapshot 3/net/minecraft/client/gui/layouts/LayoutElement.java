package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public interface LayoutElement {
   void setX(int x);

   void setY(int y);

   int getX();

   int getY();

   int getWidth();

   int getHeight();

   default ScreenRectangle getRectangle() {
      return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
   }

   default void setPosition(final int x, final int y) {
      this.setX(x);
      this.setY(y);
   }

   void visitWidgets(final Consumer<AbstractWidget> widgetVisitor);
}
