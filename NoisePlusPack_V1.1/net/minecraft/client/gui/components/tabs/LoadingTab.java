package net.minecraft.client.gui.components.tabs;

import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoadingTab implements Tab {
   private final Component title;
   private final Component loadingTitle;
   protected final LinearLayout layout = LinearLayout.vertical();

   public LoadingTab(Font p_409244_, Component p_407132_, Component p_406344_) {
      this.title = p_407132_;
      this.loadingTitle = p_406344_;
      LoadingDotsWidget loadingdotswidget = new LoadingDotsWidget(p_409244_, p_406344_);
      this.layout.defaultCellSetting().alignVerticallyMiddle().alignHorizontallyCenter();
      this.layout.addChild(loadingdotswidget, p_410185_ -> p_410185_.paddingBottom(30));
   }

   @Override
   public Component getTabTitle() {
      return this.title;
   }

   @Override
   public Component getTabExtraNarration() {
      return this.loadingTitle;
   }

   @Override
   public void visitChildren(Consumer<AbstractWidget> p_408138_) {
      this.layout.visitWidgets(p_408138_);
   }

   @Override
   public void doLayout(ScreenRectangle p_406077_) {
      this.layout.arrangeElements();
      FrameLayout.alignInRectangle(this.layout, p_406077_, 0.5F, 0.5F);
   }
}
