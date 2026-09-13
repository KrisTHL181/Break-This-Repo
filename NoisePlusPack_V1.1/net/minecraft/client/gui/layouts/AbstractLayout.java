package net.minecraft.client.gui.layouts;

import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractLayout implements Layout {
   private int x;
   private int y;
   protected int width;
   protected int height;

   public AbstractLayout(int p_265185_, int p_265789_, int p_265792_, int p_265443_) {
      this.x = p_265185_;
      this.y = p_265789_;
      this.width = p_265792_;
      this.height = p_265443_;
   }

   @Override
   public void setX(int p_265701_) {
      this.visitChildren(p_265043_ -> {
         int i = p_265043_.getX() + (p_265701_ - this.getX());
         p_265043_.setX(i);
      });
      this.x = p_265701_;
   }

   @Override
   public void setY(int p_265155_) {
      this.visitChildren(p_265586_ -> {
         int i = p_265586_.getY() + (p_265155_ - this.getY());
         p_265586_.setY(i);
      });
      this.y = p_265155_;
   }

   @Override
   public int getX() {
      return this.x;
   }

   @Override
   public int getY() {
      return this.y;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract static class AbstractChildWrapper {
      public final LayoutElement child;
      public final LayoutSettings.LayoutSettingsImpl layoutSettings;

      protected AbstractChildWrapper(LayoutElement p_265145_, LayoutSettings p_265309_) {
         this.child = p_265145_;
         this.layoutSettings = p_265309_.getExposed();
      }

      public int getHeight() {
         return this.child.getHeight() + this.layoutSettings.paddingTop + this.layoutSettings.paddingBottom;
      }

      public int getWidth() {
         return this.child.getWidth() + this.layoutSettings.paddingLeft + this.layoutSettings.paddingRight;
      }

      public void setX(int p_265766_, int p_265689_) {
         float f = this.layoutSettings.paddingLeft;
         float f1 = p_265689_ - this.child.getWidth() - this.layoutSettings.paddingRight;
         int i = (int)Mth.lerp(this.layoutSettings.xAlignment, f, f1);
         this.child.setX(i + p_265766_);
      }

      public void setY(int p_265384_, int p_265375_) {
         float f = this.layoutSettings.paddingTop;
         float f1 = p_265375_ - this.child.getHeight() - this.layoutSettings.paddingBottom;
         int i = Math.round(Mth.lerp(this.layoutSettings.yAlignment, f, f1));
         this.child.setY(i + p_265384_);
      }
   }
}
