package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusableTextWidget extends MultiLineTextWidget {
   public static final int DEFAULT_PADDING = 4;
   private final int padding;
   private final int maxWidth;
   private final boolean alwaysShowBorder;
   private final FocusableTextWidget.BackgroundFill backgroundFill;

   FocusableTextWidget(Component p_299786_, Font p_299475_, int p_299147_, int p_335803_, FocusableTextWidget.BackgroundFill p_431404_, boolean p_299140_) {
      super(p_299786_, p_299475_);
      this.active = true;
      this.padding = p_299147_;
      this.maxWidth = p_335803_;
      this.alwaysShowBorder = p_299140_;
      this.backgroundFill = p_431404_;
      this.updateWidth();
      this.updateHeight();
      this.setCentered(true);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput p_300724_) {
      p_300724_.add(NarratedElementType.TITLE, this.getMessage());
   }

   @Override
   public void renderWidget(GuiGraphics p_297672_, int p_301298_, int p_300386_, float p_299545_) {
      int i = this.alwaysShowBorder && !this.isFocused() ? ARGB.color(this.alpha, -6250336) : ARGB.white(this.alpha);
      switch (this.backgroundFill) {
         case ALWAYS:
            p_297672_.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
            break;
         case ON_FOCUS:
            if (this.isFocused()) {
               p_297672_.fill(this.getX() + 1, this.getY(), this.getRight(), this.getBottom(), ARGB.black(this.alpha));
            }
         case NEVER:
      }

      if (this.isFocused() || this.alwaysShowBorder) {
         p_297672_.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), i);
      }

      super.renderWidget(p_297672_, p_301298_, p_300386_, p_299545_);
   }

   @Override
   protected int getTextX() {
      return this.getX() + this.padding;
   }

   @Override
   protected int getTextY() {
      return super.getTextY() + this.padding;
   }

   @Override
   public MultiLineTextWidget setMaxWidth(int p_455703_) {
      return super.setMaxWidth(p_455703_ - this.padding * 2);
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   public int getPadding() {
      return this.padding;
   }

   public void updateWidth() {
      if (this.maxWidth != -1) {
         this.setWidth(this.maxWidth);
         this.setMaxWidth(this.maxWidth);
      } else {
         this.setWidth(this.getFont().width(this.getMessage()) + this.padding * 2);
      }
   }

   public void updateHeight() {
      int i = 9 * this.getFont().split(this.getMessage(), super.getWidth()).size();
      this.setHeight(i + this.padding * 2);
   }

   @Override
   public void setMessage(Component p_452838_) {
      this.message = p_452838_;
      int i;
      if (this.maxWidth != -1) {
         i = this.maxWidth;
      } else {
         i = this.getFont().width(p_452838_) + this.padding * 2;
      }

      this.setWidth(i);
      this.updateHeight();
   }

   @Override
   public void playDownSound(SoundManager p_297351_) {
   }

   public static FocusableTextWidget.Builder builder(Component p_459858_, Font p_457480_) {
      return new FocusableTextWidget.Builder(p_459858_, p_457480_);
   }

   public static FocusableTextWidget.Builder builder(Component p_453533_, Font p_455157_, int p_456785_) {
      return new FocusableTextWidget.Builder(p_453533_, p_455157_, p_456785_);
   }

   @OnlyIn(Dist.CLIENT)
   public enum BackgroundFill {
      ALWAYS,
      ON_FOCUS,
      NEVER;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final Component message;
      private final Font font;
      private final int padding;
      private int maxWidth = -1;
      private boolean alwaysShowBorder = true;
      private FocusableTextWidget.BackgroundFill backgroundFill = FocusableTextWidget.BackgroundFill.ALWAYS;

      Builder(Component p_450391_, Font p_456414_) {
         this(p_450391_, p_456414_, 4);
      }

      Builder(Component p_461088_, Font p_454917_, int p_451913_) {
         this.message = p_461088_;
         this.font = p_454917_;
         this.padding = p_451913_;
      }

      public FocusableTextWidget.Builder maxWidth(int p_451950_) {
         this.maxWidth = p_451950_;
         return this;
      }

      public FocusableTextWidget.Builder textWidth(int p_459131_) {
         this.maxWidth = p_459131_ + this.padding * 2;
         return this;
      }

      public FocusableTextWidget.Builder alwaysShowBorder(boolean p_457486_) {
         this.alwaysShowBorder = p_457486_;
         return this;
      }

      public FocusableTextWidget.Builder backgroundFill(FocusableTextWidget.BackgroundFill p_456351_) {
         this.backgroundFill = p_456351_;
         return this;
      }

      public FocusableTextWidget build() {
         return new FocusableTextWidget(this.message, this.font, this.padding, this.maxWidth, this.backgroundFill, this.alwaysShowBorder);
      }
   }
}
