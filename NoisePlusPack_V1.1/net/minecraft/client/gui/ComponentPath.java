package net.minecraft.client.gui;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ComponentPath {
   static ComponentPath leaf(GuiEventListener p_265344_) {
      return new ComponentPath.Leaf(p_265344_);
   }

   static @Nullable ComponentPath path(ContainerEventHandler p_265254_, @Nullable ComponentPath p_265405_) {
      return p_265405_ == null ? null : new ComponentPath.Path(p_265254_, p_265405_);
   }

   static ComponentPath path(GuiEventListener p_265555_, ContainerEventHandler... p_265487_) {
      ComponentPath componentpath = leaf(p_265555_);

      for (ContainerEventHandler containereventhandler : p_265487_) {
         componentpath = path(containereventhandler, componentpath);
      }

      return componentpath;
   }

   GuiEventListener component();

   void applyFocus(boolean var1);

   @OnlyIn(Dist.CLIENT)
   record Leaf(GuiEventListener component) implements ComponentPath {
      @Override
      public void applyFocus(boolean p_265248_) {
         this.component.setFocused(p_265248_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Path(ContainerEventHandler component, ComponentPath childPath) implements ComponentPath {
      @Override
      public void applyFocus(boolean p_265230_) {
         if (!p_265230_) {
            this.component.setFocused(null);
         } else {
            this.component.setFocused(this.childPath.component());
         }

         this.childPath.applyFocus(p_265230_);
      }
   }
}
