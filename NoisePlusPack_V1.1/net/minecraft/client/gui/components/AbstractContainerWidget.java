package net.minecraft.client.gui.components;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerWidget extends AbstractScrollArea implements ContainerEventHandler {
   private @Nullable GuiEventListener focused;
   private boolean isDragging;

   public AbstractContainerWidget(int p_310492_, int p_309402_, int p_313085_, int p_312513_, Component p_310986_) {
      super(p_310492_, p_309402_, p_313085_, p_312513_, p_310986_);
   }

   @Override
   public final boolean isDragging() {
      return this.isDragging;
   }

   @Override
   public final void setDragging(boolean p_311596_) {
      this.isDragging = p_311596_;
   }

   @Override
   public @Nullable GuiEventListener getFocused() {
      return this.focused;
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_312828_) {
      if (this.focused != null) {
         this.focused.setFocused(false);
      }

      if (p_312828_ != null) {
         p_312828_.setFocused(true);
      }

      this.focused = p_312828_;
   }

   @Override
   public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_311207_) {
      return ContainerEventHandler.super.nextFocusPath(p_311207_);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_424458_, boolean p_428441_) {
      boolean flag = this.updateScrolling(p_424458_);
      return ContainerEventHandler.super.mouseClicked(p_424458_, p_428441_) || flag;
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent p_431035_) {
      super.mouseReleased(p_431035_);
      return ContainerEventHandler.super.mouseReleased(p_431035_);
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent p_425509_, double p_310748_, double p_313111_) {
      super.mouseDragged(p_425509_, p_310748_, p_313111_);
      return ContainerEventHandler.super.mouseDragged(p_425509_, p_310748_, p_313111_);
   }

   @Override
   public boolean isFocused() {
      return ContainerEventHandler.super.isFocused();
   }

   @Override
   public void setFocused(boolean p_310891_) {
      ContainerEventHandler.super.setFocused(p_310891_);
   }
}
