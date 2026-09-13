package net.minecraft.client.gui.components.tabs;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GridLayoutTab implements Tab {
   private final Component title;
   protected final GridLayout layout = new GridLayout();

   public GridLayoutTab(Component p_268022_) {
      this.title = p_268022_;
   }

   @Override
   public Component getTabTitle() {
      return this.title;
   }

   @Override
   public Component getTabExtraNarration() {
      return Component.empty();
   }

   @Override
   public void visitChildren(Consumer<AbstractWidget> p_268098_) {
      this.layout.visitWidgets(p_268098_);
   }

   @Override
   public void doLayout(ScreenRectangle p_268281_) {
      this.layout.arrangeElements();
      FrameLayout.alignInRectangle(this.layout, p_268281_, 0.5F, 0.16666667F);
   }
}
