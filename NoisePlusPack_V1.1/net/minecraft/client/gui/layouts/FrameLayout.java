package net.minecraft.client.gui.layouts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FrameLayout extends AbstractLayout {
   private final List<FrameLayout.ChildContainer> children = new ArrayList<>();
   private int minWidth;
   private int minHeight;
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults().align(0.5F, 0.5F);

   public FrameLayout() {
      this(0, 0, 0, 0);
   }

   public FrameLayout(int p_270073_, int p_270705_) {
      this(0, 0, p_270073_, p_270705_);
   }

   public FrameLayout(int p_265719_, int p_265042_, int p_265587_, int p_265682_) {
      super(p_265719_, p_265042_, p_265587_, p_265682_);
      this.setMinDimensions(p_265587_, p_265682_);
   }

   public FrameLayout setMinDimensions(int p_265169_, int p_265616_) {
      return this.setMinWidth(p_265169_).setMinHeight(p_265616_);
   }

   public FrameLayout setMinHeight(int p_265646_) {
      this.minHeight = p_265646_;
      return this;
   }

   public FrameLayout setMinWidth(int p_265764_) {
      this.minWidth = p_265764_;
      return this;
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      int i = this.minWidth;
      int j = this.minHeight;

      for (FrameLayout.ChildContainer framelayout$childcontainer : this.children) {
         i = Math.max(i, framelayout$childcontainer.getWidth());
         j = Math.max(j, framelayout$childcontainer.getHeight());
      }

      for (FrameLayout.ChildContainer framelayout$childcontainer1 : this.children) {
         framelayout$childcontainer1.setX(this.getX(), i);
         framelayout$childcontainer1.setY(this.getY(), j);
      }

      this.width = i;
      this.height = j;
   }

   public <T extends LayoutElement> T addChild(T p_265071_) {
      return this.addChild(p_265071_, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T p_265386_, LayoutSettings p_265532_) {
      this.children.add(new FrameLayout.ChildContainer(p_265386_, p_265532_));
      return p_265386_;
   }

   public <T extends LayoutElement> T addChild(T p_298612_, Consumer<LayoutSettings> p_301357_) {
      return this.addChild(p_298612_, Util.make(this.newChildLayoutSettings(), p_301357_));
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> p_265070_) {
      this.children.forEach(p_265653_ -> p_265070_.accept(p_265653_.child));
   }

   public static void centerInRectangle(LayoutElement p_265197_, int p_265518_, int p_265334_, int p_265540_, int p_265632_) {
      alignInRectangle(p_265197_, p_265518_, p_265334_, p_265540_, p_265632_, 0.5F, 0.5F);
   }

   public static void centerInRectangle(LayoutElement p_268229_, ScreenRectangle p_268113_) {
      centerInRectangle(p_268229_, p_268113_.position().x(), p_268113_.position().y(), p_268113_.width(), p_268113_.height());
   }

   public static void alignInRectangle(LayoutElement p_275320_, ScreenRectangle p_275389_, float p_275607_, float p_275662_) {
      alignInRectangle(p_275320_, p_275389_.left(), p_275389_.top(), p_275389_.width(), p_275389_.height(), p_275607_, p_275662_);
   }

   public static void alignInRectangle(LayoutElement p_265662_, int p_265497_, int p_265030_, int p_265535_, int p_265427_, float p_265271_, float p_265365_) {
      alignInDimension(p_265497_, p_265535_, p_265662_.getWidth(), p_265662_::setX, p_265271_);
      alignInDimension(p_265030_, p_265427_, p_265662_.getHeight(), p_265662_::setY, p_265365_);
   }

   public static void alignInDimension(int p_265164_, int p_265100_, int p_265351_, Consumer<Integer> p_265614_, float p_265428_) {
      int i = (int)Mth.lerp(p_265428_, 0.0F, p_265100_ - p_265351_);
      p_265614_.accept(p_265164_ + i);
   }

   @OnlyIn(Dist.CLIENT)
   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement p_265667_, LayoutSettings p_265430_) {
         super(p_265667_, p_265430_);
      }
   }
}
