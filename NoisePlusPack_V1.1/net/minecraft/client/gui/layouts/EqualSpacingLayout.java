package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EqualSpacingLayout extends AbstractLayout {
   private final EqualSpacingLayout.Orientation orientation;
   private final List<EqualSpacingLayout.ChildContainer> children = new ArrayList<>();
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();

   public EqualSpacingLayout(int p_298986_, int p_300198_, EqualSpacingLayout.Orientation p_297766_) {
      this(0, 0, p_298986_, p_300198_, p_297766_);
   }

   public EqualSpacingLayout(int p_300144_, int p_297966_, int p_301171_, int p_299404_, EqualSpacingLayout.Orientation p_300279_) {
      super(p_300144_, p_297966_, p_301171_, p_299404_);
      this.orientation = p_300279_;
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int i = 0;
         int j = this.orientation.getSecondaryLength(this);

         for (EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer : this.children) {
            i += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer);
            j = Math.max(j, this.orientation.getSecondaryLength(equalspacinglayout$childcontainer));
         }

         int k = this.orientation.getPrimaryLength(this) - i;
         int l = this.orientation.getPrimaryPosition(this);
         Iterator<EqualSpacingLayout.ChildContainer> iterator = this.children.iterator();
         EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer1 = iterator.next();
         this.orientation.setPrimaryPosition(equalspacinglayout$childcontainer1, l);
         l += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer1);
         if (this.children.size() >= 2) {
            Divisor divisor = new Divisor(k, this.children.size() - 1);

            while (divisor.hasNext()) {
               l += divisor.nextInt();
               EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer2 = iterator.next();
               this.orientation.setPrimaryPosition(equalspacinglayout$childcontainer2, l);
               l += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer2);
            }
         }

         int i1 = this.orientation.getSecondaryPosition(this);

         for (EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer3 : this.children) {
            this.orientation.setSecondaryPosition(equalspacinglayout$childcontainer3, i1, j);
         }

         switch (this.orientation) {
            case HORIZONTAL:
               this.height = j;
               break;
            case VERTICAL:
               this.width = j;
         }
      }
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> p_299750_) {
      this.children.forEach(p_298818_ -> p_299750_.accept(p_298818_.child));
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(T p_301351_) {
      return this.addChild(p_301351_, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T p_299596_, LayoutSettings p_301383_) {
      this.children.add(new EqualSpacingLayout.ChildContainer(p_299596_, p_301383_));
      return p_299596_;
   }

   public <T extends LayoutElement> T addChild(T p_298517_, Consumer<LayoutSettings> p_297917_) {
      return this.addChild(p_298517_, Util.make(this.newChildLayoutSettings(), p_297917_));
   }

   @OnlyIn(Dist.CLIENT)
   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement p_298955_, LayoutSettings p_298136_) {
         super(p_298955_, p_298136_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public enum Orientation {
      HORIZONTAL,
      VERTICAL;

      int getPrimaryLength(LayoutElement p_299718_) {
         return switch (this) {
            case HORIZONTAL -> p_299718_.getWidth();
            case VERTICAL -> p_299718_.getHeight();
         };
      }

      int getPrimaryLength(EqualSpacingLayout.ChildContainer p_298044_) {
         return switch (this) {
            case HORIZONTAL -> p_298044_.getWidth();
            case VERTICAL -> p_298044_.getHeight();
         };
      }

      int getSecondaryLength(LayoutElement p_301361_) {
         return switch (this) {
            case HORIZONTAL -> p_301361_.getHeight();
            case VERTICAL -> p_301361_.getWidth();
         };
      }

      int getSecondaryLength(EqualSpacingLayout.ChildContainer p_297317_) {
         return switch (this) {
            case HORIZONTAL -> p_297317_.getHeight();
            case VERTICAL -> p_297317_.getWidth();
         };
      }

      void setPrimaryPosition(EqualSpacingLayout.ChildContainer p_298745_, int p_300763_) {
         switch (this) {
            case HORIZONTAL:
               p_298745_.setX(p_300763_, p_298745_.getWidth());
               break;
            case VERTICAL:
               p_298745_.setY(p_300763_, p_298745_.getHeight());
         }
      }

      void setSecondaryPosition(EqualSpacingLayout.ChildContainer p_299676_, int p_297698_, int p_297461_) {
         switch (this) {
            case HORIZONTAL:
               p_299676_.setY(p_297698_, p_297461_);
               break;
            case VERTICAL:
               p_299676_.setX(p_297698_, p_297461_);
         }
      }

      int getPrimaryPosition(LayoutElement p_299240_) {
         return switch (this) {
            case HORIZONTAL -> p_299240_.getX();
            case VERTICAL -> p_299240_.getY();
         };
      }

      int getSecondaryPosition(LayoutElement p_299036_) {
         return switch (this) {
            case HORIZONTAL -> p_299036_.getY();
            case VERTICAL -> p_299036_.getX();
         };
      }
   }
}
