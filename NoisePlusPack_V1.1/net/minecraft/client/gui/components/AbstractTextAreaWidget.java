package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractTextAreaWidget extends AbstractScrollArea {
   private static final WidgetSprites BACKGROUND_SPRITES = new WidgetSprites(
      Identifier.withDefaultNamespace("widget/text_field"), Identifier.withDefaultNamespace("widget/text_field_highlighted")
   );
   private static final int INNER_PADDING = 4;
   public static final int DEFAULT_TOTAL_PADDING = 8;
   private boolean showBackground = true;
   private boolean showDecorations = true;

   public AbstractTextAreaWidget(int p_378028_, int p_375960_, int p_376988_, int p_376757_, Component p_378529_) {
      super(p_378028_, p_375960_, p_376988_, p_376757_, p_378529_);
   }

   public AbstractTextAreaWidget(int p_409240_, int p_407697_, int p_407917_, int p_410081_, Component p_409161_, boolean p_406968_, boolean p_409537_) {
      this(p_409240_, p_407697_, p_407917_, p_410081_, p_409161_);
      this.showBackground = p_406968_;
      this.showDecorations = p_409537_;
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent p_423231_, boolean p_430313_) {
      boolean flag = this.updateScrolling(p_423231_);
      return super.mouseClicked(p_423231_, p_430313_) || flag;
   }

   @Override
   public boolean keyPressed(KeyEvent p_425030_) {
      boolean flag = p_425030_.isUp();
      boolean flag1 = p_425030_.isDown();
      if (flag || flag1) {
         double d0 = this.scrollAmount();
         this.setScrollAmount(this.scrollAmount() + (flag ? -1 : 1) * this.scrollRate());
         if (d0 != this.scrollAmount()) {
            return true;
         }
      }

      return super.keyPressed(p_425030_);
   }

   @Override
   public void renderWidget(GuiGraphics p_376330_, int p_376585_, int p_376181_, float p_376214_) {
      if (this.visible) {
         if (this.showBackground) {
            this.renderBackground(p_376330_);
         }

         p_376330_.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
         p_376330_.pose().pushMatrix();
         p_376330_.pose().translate(0.0F, (float)(-this.scrollAmount()));
         this.renderContents(p_376330_, p_376585_, p_376181_, p_376214_);
         p_376330_.pose().popMatrix();
         p_376330_.disableScissor();
         this.renderScrollbar(p_376330_, p_376585_, p_376181_);
         if (this.showDecorations) {
            this.renderDecorations(p_376330_);
         }
      }
   }

   protected void renderDecorations(GuiGraphics p_376435_) {
   }

   protected int innerPadding() {
      return 4;
   }

   protected int totalInnerPadding() {
      return this.innerPadding() * 2;
   }

   @Override
   public boolean isMouseOver(double p_376364_, double p_377350_) {
      return this.active
         && this.visible
         && p_376364_ >= this.getX()
         && p_377350_ >= this.getY()
         && p_376364_ < this.getRight() + 6
         && p_377350_ < this.getBottom();
   }

   @Override
   protected int scrollBarX() {
      return this.getRight();
   }

   @Override
   protected int contentHeight() {
      return this.getInnerHeight() + this.totalInnerPadding();
   }

   protected void renderBackground(GuiGraphics p_378043_) {
      this.renderBorder(p_378043_, this.getX(), this.getY(), this.getWidth(), this.getHeight());
   }

   protected void renderBorder(GuiGraphics p_376239_, int p_378450_, int p_375463_, int p_377865_, int p_375612_) {
      Identifier identifier = BACKGROUND_SPRITES.get(this.isActive(), this.isFocused());
      p_376239_.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, p_378450_, p_375463_, p_377865_, p_375612_);
   }

   protected boolean withinContentAreaTopBottom(int p_376309_, int p_378518_) {
      return p_378518_ - this.scrollAmount() >= this.getY() && p_376309_ - this.scrollAmount() <= this.getY() + this.height;
   }

   protected abstract int getInnerHeight();

   protected abstract void renderContents(GuiGraphics var1, int var2, int var3, float var4);

   protected int getInnerLeft() {
      return this.getX() + this.innerPadding();
   }

   protected int getInnerTop() {
      return this.getY() + this.innerPadding();
   }

   @Override
   public void playDownSound(SoundManager p_378011_) {
   }
}
