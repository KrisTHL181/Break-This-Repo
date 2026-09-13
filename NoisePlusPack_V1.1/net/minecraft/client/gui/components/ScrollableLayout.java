package net.minecraft.client.gui.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ScrollableLayout implements Layout {
   private static final int SCROLLBAR_SPACING = 4;
   private static final int SCROLLBAR_RESERVE = 10;
   final Layout content;
   private final ScrollableLayout.Container container;
   private int minWidth;
   private int maxHeight;

   public ScrollableLayout(Minecraft p_406045_, Layout p_407684_, int p_410064_) {
      this.content = p_407684_;
      this.container = new ScrollableLayout.Container(p_406045_, 0, p_410064_);
   }

   public void setMinWidth(int p_407097_) {
      this.minWidth = p_407097_;
      this.container.setWidth(Math.max(this.content.getWidth(), p_407097_));
   }

   public void setMaxHeight(int p_408817_) {
      this.maxHeight = p_408817_;
      this.container.setHeight(Math.min(this.content.getHeight(), p_408817_));
      this.container.refreshScrollAmount();
   }

   @Override
   public void arrangeElements() {
      this.content.arrangeElements();
      int i = this.content.getWidth();
      this.container.setWidth(Math.max(i + 20, this.minWidth));
      this.container.setHeight(Math.min(this.content.getHeight(), this.maxHeight));
      this.container.refreshScrollAmount();
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> p_407905_) {
      p_407905_.accept(this.container);
   }

   @Override
   public void setX(int p_406141_) {
      this.container.setX(p_406141_);
   }

   @Override
   public void setY(int p_407828_) {
      this.container.setY(p_407828_);
   }

   @Override
   public int getX() {
      return this.container.getX();
   }

   @Override
   public int getY() {
      return this.container.getY();
   }

   @Override
   public int getWidth() {
      return this.container.getWidth();
   }

   @Override
   public int getHeight() {
      return this.container.getHeight();
   }

   @OnlyIn(Dist.CLIENT)
   class Container extends AbstractContainerWidget {
      private final Minecraft minecraft;
      private final List<AbstractWidget> children = new ArrayList<>();

      public Container(final Minecraft p_407115_, final int p_406032_, final int p_409399_) {
         super(0, 0, p_406032_, p_409399_, CommonComponents.EMPTY);
         this.minecraft = p_407115_;
         ScrollableLayout.this.content.visitWidgets(this.children::add);
      }

      @Override
      protected int contentHeight() {
         return ScrollableLayout.this.content.getHeight();
      }

      @Override
      protected double scrollRate() {
         return 10.0;
      }

      @Override
      protected void renderWidget(GuiGraphics p_407943_, int p_407166_, int p_410440_, float p_407907_) {
         p_407943_.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

         for (AbstractWidget abstractwidget : this.children) {
            abstractwidget.render(p_407943_, p_407166_, p_410440_, p_407907_);
         }

         p_407943_.disableScissor();
         this.renderScrollbar(p_407943_, p_407166_, p_410440_);
      }

      @Override
      protected void updateWidgetNarration(NarrationElementOutput p_407520_) {
      }

      @Override
      public ScreenRectangle getBorderForArrowNavigation(ScreenDirection p_410060_) {
         return new ScreenRectangle(this.getX(), this.getY(), this.width, this.contentHeight());
      }

      @Override
      public void setFocused(@Nullable GuiEventListener p_409374_) {
         super.setFocused(p_409374_);
         if (p_409374_ != null && this.minecraft.getLastInputType().isKeyboard()) {
            ScreenRectangle screenrectangle = this.getRectangle();
            ScreenRectangle screenrectangle1 = p_409374_.getRectangle();
            int i = screenrectangle1.top() - screenrectangle.top();
            int j = screenrectangle1.bottom() - screenrectangle.bottom();
            if (i < 0) {
               this.setScrollAmount(this.scrollAmount() + i - 14.0);
            } else if (j > 0) {
               this.setScrollAmount(this.scrollAmount() + j + 14.0);
            }
         }
      }

      @Override
      public void setX(int p_408984_) {
         super.setX(p_408984_);
         ScrollableLayout.this.content.setX(p_408984_ + 10);
      }

      @Override
      public void setY(int p_410658_) {
         super.setY(p_410658_);
         ScrollableLayout.this.content.setY(p_410658_ - (int)this.scrollAmount());
      }

      @Override
      public void setScrollAmount(double p_409837_) {
         super.setScrollAmount(p_409837_);
         ScrollableLayout.this.content.setY(this.getRectangle().top() - (int)this.scrollAmount());
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public Collection<? extends NarratableEntry> getNarratables() {
         return this.children;
      }
   }
}
