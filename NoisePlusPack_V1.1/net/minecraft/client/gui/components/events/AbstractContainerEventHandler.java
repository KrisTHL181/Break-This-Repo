package net.minecraft.client.gui.components.events;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerEventHandler implements ContainerEventHandler {
   private @Nullable GuiEventListener focused;
   private boolean isDragging;

   @Override
   public final boolean isDragging() {
      return this.isDragging;
   }

   @Override
   public final void setDragging(boolean p_94681_) {
      this.isDragging = p_94681_;
   }

   @Override
   public @Nullable GuiEventListener getFocused() {
      return this.focused;
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_94677_) {
      if (this.focused != p_94677_) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         if (p_94677_ != null) {
            p_94677_.setFocused(true);
         }

         this.focused = p_94677_;
      }
   }
}
