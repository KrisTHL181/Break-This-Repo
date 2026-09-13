package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LinearLayout implements Layout {
   private final GridLayout wrapped;
   private final LinearLayout.Orientation orientation;
   private int nextChildIndex = 0;

   private LinearLayout(LinearLayout.Orientation p_265341_) {
      this(0, 0, p_265341_);
   }

   public LinearLayout(int p_265093_, int p_265502_, LinearLayout.Orientation p_265112_) {
      this.wrapped = new GridLayout(p_265093_, p_265502_);
      this.orientation = p_265112_;
   }

   public LinearLayout spacing(int p_298391_) {
      this.orientation.setSpacing(this.wrapped, p_298391_);
      return this;
   }

   public LayoutSettings newCellSettings() {
      return this.wrapped.newCellSettings();
   }

   public LayoutSettings defaultCellSetting() {
      return this.wrapped.defaultCellSetting();
   }

   public <T extends LayoutElement> T addChild(T p_265475_, LayoutSettings p_265684_) {
      return this.orientation.addChild(this.wrapped, p_265475_, this.nextChildIndex++, p_265684_);
   }

   public <T extends LayoutElement> T addChild(T p_265140_) {
      return this.addChild(p_265140_, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T p_300762_, Consumer<LayoutSettings> p_300497_) {
      return this.orientation.addChild(this.wrapped, p_300762_, this.nextChildIndex++, Util.make(this.newCellSettings(), p_300497_));
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> p_265508_) {
      this.wrapped.visitChildren(p_265508_);
   }

   @Override
   public void arrangeElements() {
      this.wrapped.arrangeElements();
   }

   @Override
   public int getWidth() {
      return this.wrapped.getWidth();
   }

   @Override
   public int getHeight() {
      return this.wrapped.getHeight();
   }

   @Override
   public void setX(int p_297321_) {
      this.wrapped.setX(p_297321_);
   }

   @Override
   public void setY(int p_299381_) {
      this.wrapped.setY(p_299381_);
   }

   @Override
   public int getX() {
      return this.wrapped.getX();
   }

   @Override
   public int getY() {
      return this.wrapped.getY();
   }

   public static LinearLayout vertical() {
      return new LinearLayout(LinearLayout.Orientation.VERTICAL);
   }

   public static LinearLayout horizontal() {
      return new LinearLayout(LinearLayout.Orientation.HORIZONTAL);
   }

   @OnlyIn(Dist.CLIENT)
   public enum Orientation {
      HORIZONTAL,
      VERTICAL;

      void setSpacing(GridLayout p_299858_, int p_299775_) {
         switch (this) {
            case HORIZONTAL:
               p_299858_.columnSpacing(p_299775_);
               break;
            case VERTICAL:
               p_299858_.rowSpacing(p_299775_);
         }
      }

      public <T extends LayoutElement> T addChild(GridLayout p_298633_, T p_297548_, int p_300692_, LayoutSettings p_298693_) {
         return (T)(switch (this) {
            case HORIZONTAL -> p_298633_.addChild(p_297548_, 0, p_300692_, p_298693_);
            case VERTICAL -> p_298633_.addChild(p_297548_, p_300692_, 0, p_298693_);
         });
      }
   }
}
