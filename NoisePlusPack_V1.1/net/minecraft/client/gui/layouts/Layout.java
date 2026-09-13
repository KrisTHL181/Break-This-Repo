package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface Layout extends LayoutElement {
   void visitChildren(Consumer<LayoutElement> var1);

   @Override
   default void visitWidgets(Consumer<AbstractWidget> p_270962_) {
      this.visitChildren(p_270634_ -> p_270634_.visitWidgets(p_270962_));
   }

   default void arrangeElements() {
      this.visitChildren(p_270565_ -> {
         if (p_270565_ instanceof Layout layout) {
            layout.arrangeElements();
         }
      });
   }
}
