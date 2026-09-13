package net.minecraft.client.gui.components;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class ContainerObjectSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
   public ContainerObjectSelectionList(Minecraft p_94010_, int p_94011_, int p_94012_, int p_94013_, int p_94014_) {
      super(p_94010_, p_94011_, p_94012_, p_94013_, p_94014_);
   }

   @Override
   public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_265385_) {
      if (this.getItemCount() == 0) {
         return null;
      } else if (!(p_265385_ instanceof FocusNavigationEvent.ArrowNavigation focusnavigationevent$arrownavigation)) {
         return super.nextFocusPath(p_265385_);
      } else {
         ContainerObjectSelectionList.Entry e = this.getFocused();
         if (focusnavigationevent$arrownavigation.direction().getAxis() == ScreenAxis.HORIZONTAL && e != null) {
            return ComponentPath.path(this, e.nextFocusPath(p_265385_));
         }

         int i = -1;
         ScreenDirection screendirection = focusnavigationevent$arrownavigation.direction();
         if (e != null) {
            i = e.children().indexOf(e.getFocused());
         }

         if (i == -1) {
            switch (screendirection) {
               case LEFT:
                  i = Integer.MAX_VALUE;
                  screendirection = ScreenDirection.DOWN;
                  break;
               case RIGHT:
                  i = 0;
                  screendirection = ScreenDirection.DOWN;
                  break;
               default:
                  i = 0;
            }
         }

         E e1 = (E)e;

         ComponentPath componentpath;
         do {
            e1 = this.nextEntry(screendirection, p_420704_ -> !p_420704_.children().isEmpty(), e1);
            if (e1 == null) {
               return null;
            }

            componentpath = e1.focusPathAtIndex(focusnavigationevent$arrownavigation, i);
         } while (componentpath == null);

         return ComponentPath.path(this, componentpath);
      }
   }

   @Override
   public void setFocused(@Nullable GuiEventListener p_265559_) {
      if (this.getFocused() != p_265559_) {
         super.setFocused(p_265559_);
         if (p_265559_ == null) {
            this.setSelected(null);
         }
      }
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      return this.isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : super.narrationPriority();
   }

   @Override
   protected boolean entriesCanBeSelected() {
      return false;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput p_313248_) {
      if (this.getHovered() instanceof E e) {
         e.updateNarration(p_313248_.nest());
         this.narrateListElementPosition(p_313248_, e);
      } else if (this.getFocused() instanceof E e1) {
         e1.updateNarration(p_313248_.nest());
         this.narrateListElementPosition(p_313248_, e1);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements ContainerEventHandler {
      private @Nullable GuiEventListener focused;
      private @Nullable NarratableEntry lastNarratable;
      private boolean dragging;

      @Override
      public boolean isDragging() {
         return this.dragging;
      }

      @Override
      public void setDragging(boolean p_94028_) {
         this.dragging = p_94028_;
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent p_423427_, boolean p_430949_) {
         return ContainerEventHandler.super.mouseClicked(p_423427_, p_430949_);
      }

      @Override
      public void setFocused(@Nullable GuiEventListener p_94024_) {
         if (this.focused != null) {
            this.focused.setFocused(false);
         }

         if (p_94024_ != null) {
            p_94024_.setFocused(true);
         }

         this.focused = p_94024_;
      }

      @Override
      public @Nullable GuiEventListener getFocused() {
         return this.focused;
      }

      public @Nullable ComponentPath focusPathAtIndex(FocusNavigationEvent p_265435_, int p_265432_) {
         if (this.children().isEmpty()) {
            return null;
         }

         ComponentPath componentpath = this.children().get(Math.min(p_265432_, this.children().size() - 1)).nextFocusPath(p_265435_);
         return ComponentPath.path(this, componentpath);
      }

      @Override
      public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent p_265672_) {
         if (p_265672_ instanceof FocusNavigationEvent.ArrowNavigation focusnavigationevent$arrownavigation) {
            int i = switch (focusnavigationevent$arrownavigation.direction()) {
               case LEFT -> -1;
               case RIGHT -> 1;
               case UP, DOWN -> 0;
            };
            if (i == 0) {
               return null;
            }

            int j = Mth.clamp(i + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

            for (int k = j; k >= 0 && k < this.children().size(); k += i) {
               GuiEventListener guieventlistener = this.children().get(k);
               ComponentPath componentpath = guieventlistener.nextFocusPath(p_265672_);
               if (componentpath != null) {
                  return ComponentPath.path(this, componentpath);
               }
            }
         }

         return ContainerEventHandler.super.nextFocusPath(p_265672_);
      }

      public abstract List<? extends NarratableEntry> narratables();

      void updateNarration(NarrationElementOutput p_168855_) {
         List<? extends NarratableEntry> list = this.narratables();
         Screen.NarratableSearchResult screen$narratablesearchresult = Screen.findNarratableWidget(list, this.lastNarratable);
         if (screen$narratablesearchresult != null) {
            if (screen$narratablesearchresult.priority().isTerminal()) {
               this.lastNarratable = screen$narratablesearchresult.entry();
            }

            if (list.size() > 1) {
               p_168855_.add(
                  NarratedElementType.POSITION, Component.translatable("narrator.position.object_list", screen$narratablesearchresult.index() + 1, list.size())
               );
            }

            screen$narratablesearchresult.entry().updateNarration(p_168855_.nest());
         }
      }
   }
}
