package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface LayoutElement {
   void setX(int var1);

   void setY(int var1);

   int getX();

   int getY();

   int getWidth();

   int getHeight();

   default ScreenRectangle getRectangle() {
      return new ScreenRectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
   }

   default void setPosition(int p_265617_, int p_265577_) {
      this.setX(p_265617_);
      this.setY(p_265577_);
   }

   void visitWidgets(Consumer<AbstractWidget> var1);
}
