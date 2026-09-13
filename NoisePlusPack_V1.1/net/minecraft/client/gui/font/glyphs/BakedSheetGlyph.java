package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BakedSheetGlyph implements BakedGlyph, EffectGlyph {
   public static final float Z_FIGHTER = 0.001F;
   final GlyphInfo info;
   final GlyphRenderTypes renderTypes;
   final GpuTextureView textureView;
   private final float u0;
   private final float u1;
   private final float v0;
   private final float v1;
   private final float left;
   private final float right;
   private final float up;
   private final float down;

   public BakedSheetGlyph(
      GlyphInfo p_425258_,
      GlyphRenderTypes p_424415_,
      GpuTextureView p_430801_,
      float p_427953_,
      float p_430539_,
      float p_428368_,
      float p_424995_,
      float p_426241_,
      float p_423297_,
      float p_426504_,
      float p_430743_
   ) {
      this.info = p_425258_;
      this.renderTypes = p_424415_;
      this.textureView = p_430801_;
      this.u0 = p_427953_;
      this.u1 = p_430539_;
      this.v0 = p_428368_;
      this.v1 = p_424995_;
      this.left = p_426241_;
      this.right = p_423297_;
      this.up = p_426504_;
      this.down = p_430743_;
   }

   float left(BakedSheetGlyph.GlyphInstance p_431736_) {
      return p_431736_.x
         + this.left
         + (p_431736_.style.isItalic() ? Math.min(this.shearTop(), this.shearBottom()) : 0.0F)
         - extraThickness(p_431736_.style.isBold());
   }

   float top(BakedSheetGlyph.GlyphInstance p_430840_) {
      return p_430840_.y + this.up - extraThickness(p_430840_.style.isBold());
   }

   float right(BakedSheetGlyph.GlyphInstance p_422684_) {
      return p_422684_.x
         + this.right
         + (p_422684_.hasShadow() ? p_422684_.shadowOffset : 0.0F)
         + (p_422684_.style.isItalic() ? Math.max(this.shearTop(), this.shearBottom()) : 0.0F)
         + extraThickness(p_422684_.style.isBold());
   }

   float bottom(BakedSheetGlyph.GlyphInstance p_429814_) {
      return p_429814_.y + this.down + (p_429814_.hasShadow() ? p_429814_.shadowOffset : 0.0F) + extraThickness(p_429814_.style.isBold());
   }

   void renderChar(BakedSheetGlyph.GlyphInstance p_428903_, Matrix4f p_430874_, VertexConsumer p_429607_, int p_426377_, boolean p_425909_) {
      Style style = p_428903_.style();
      boolean flag = style.isItalic();
      float f = p_428903_.x();
      float f1 = p_428903_.y();
      int i = p_428903_.color();
      boolean flag1 = style.isBold();
      float f3 = p_425909_ ? 0.0F : 0.001F;
      float f2;
      if (p_428903_.hasShadow()) {
         int j = p_428903_.shadowColor();
         this.render(flag, f + p_428903_.shadowOffset(), f1 + p_428903_.shadowOffset(), 0.0F, p_430874_, p_429607_, j, flag1, p_426377_);
         if (flag1) {
            this.render(
               flag, f + p_428903_.boldOffset() + p_428903_.shadowOffset(), f1 + p_428903_.shadowOffset(), f3, p_430874_, p_429607_, j, true, p_426377_
            );
         }

         f2 = p_425909_ ? 0.0F : 0.03F;
      } else {
         f2 = 0.0F;
      }

      this.render(flag, f, f1, f2, p_430874_, p_429607_, i, flag1, p_426377_);
      if (flag1) {
         this.render(flag, f + p_428903_.boldOffset(), f1, f2 + f3, p_430874_, p_429607_, i, true, p_426377_);
      }
   }

   private void render(
      boolean p_431682_,
      float p_424496_,
      float p_425530_,
      float p_431364_,
      Matrix4f p_425883_,
      VertexConsumer p_423922_,
      int p_428502_,
      boolean p_427243_,
      int p_423261_
   ) {
      float f = p_424496_ + this.left;
      float f1 = p_424496_ + this.right;
      float f2 = p_425530_ + this.up;
      float f3 = p_425530_ + this.down;
      float f4 = p_431682_ ? this.shearTop() : 0.0F;
      float f5 = p_431682_ ? this.shearBottom() : 0.0F;
      float f6 = extraThickness(p_427243_);
      p_423922_.addVertex(p_425883_, f + f4 - f6, f2 - f6, p_431364_).setColor(p_428502_).setUv(this.u0, this.v0).setLight(p_423261_);
      p_423922_.addVertex(p_425883_, f + f5 - f6, f3 + f6, p_431364_).setColor(p_428502_).setUv(this.u0, this.v1).setLight(p_423261_);
      p_423922_.addVertex(p_425883_, f1 + f5 + f6, f3 + f6, p_431364_).setColor(p_428502_).setUv(this.u1, this.v1).setLight(p_423261_);
      p_423922_.addVertex(p_425883_, f1 + f4 + f6, f2 - f6, p_431364_).setColor(p_428502_).setUv(this.u1, this.v0).setLight(p_423261_);
   }

   private static float extraThickness(boolean p_423433_) {
      return p_423433_ ? 0.1F : 0.0F;
   }

   private float shearBottom() {
      return 1.0F - 0.25F * this.down;
   }

   private float shearTop() {
      return 1.0F - 0.25F * this.up;
   }

   void renderEffect(BakedSheetGlyph.EffectInstance p_431325_, Matrix4f p_424382_, VertexConsumer p_424421_, int p_431384_, boolean p_422342_) {
      float f = p_422342_ ? 0.0F : p_431325_.depth;
      if (p_431325_.hasShadow()) {
         this.buildEffect(p_431325_, p_431325_.shadowOffset(), f, p_431325_.shadowColor(), p_424421_, p_431384_, p_424382_);
         f += p_422342_ ? 0.0F : 0.03F;
      }

      this.buildEffect(p_431325_, 0.0F, f, p_431325_.color, p_424421_, p_431384_, p_424382_);
   }

   private void buildEffect(
      BakedSheetGlyph.EffectInstance p_428609_, float p_430911_, float p_422339_, int p_429525_, VertexConsumer p_426857_, int p_429037_, Matrix4f p_429810_
   ) {
      p_426857_.addVertex(p_429810_, p_428609_.x0 + p_430911_, p_428609_.y1 + p_430911_, p_422339_)
         .setColor(p_429525_)
         .setUv(this.u0, this.v0)
         .setLight(p_429037_);
      p_426857_.addVertex(p_429810_, p_428609_.x1 + p_430911_, p_428609_.y1 + p_430911_, p_422339_)
         .setColor(p_429525_)
         .setUv(this.u0, this.v1)
         .setLight(p_429037_);
      p_426857_.addVertex(p_429810_, p_428609_.x1 + p_430911_, p_428609_.y0 + p_430911_, p_422339_)
         .setColor(p_429525_)
         .setUv(this.u1, this.v1)
         .setLight(p_429037_);
      p_426857_.addVertex(p_429810_, p_428609_.x0 + p_430911_, p_428609_.y0 + p_430911_, p_422339_)
         .setColor(p_429525_)
         .setUv(this.u1, this.v0)
         .setLight(p_429037_);
   }

   @Override
   public GlyphInfo info() {
      return this.info;
   }

   @Override
   public TextRenderable.Styled createGlyph(float p_429248_, float p_430475_, int p_427536_, int p_427558_, Style p_424869_, float p_422931_, float p_431312_) {
      return new BakedSheetGlyph.GlyphInstance(p_429248_, p_430475_, p_427536_, p_427558_, this, p_424869_, p_422931_, p_431312_);
   }

   @Override
   public TextRenderable createEffect(
      float p_427340_, float p_423280_, float p_424457_, float p_427022_, float p_424759_, int p_429013_, int p_427342_, float p_425149_
   ) {
      return new BakedSheetGlyph.EffectInstance(this, p_427340_, p_423280_, p_424457_, p_427022_, p_424759_, p_429013_, p_427342_, p_425149_);
   }

   @OnlyIn(Dist.CLIENT)
   record EffectInstance(BakedSheetGlyph glyph, float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset)
      implements TextRenderable {
      @Override
      public float left() {
         return this.x0;
      }

      @Override
      public float top() {
         return this.y0;
      }

      @Override
      public float right() {
         return this.x1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
      }

      @Override
      public float bottom() {
         return this.y1 + (this.hasShadow() ? this.shadowOffset : 0.0F);
      }

      boolean hasShadow() {
         return this.shadowColor() != 0;
      }

      @Override
      public void render(Matrix4f p_428538_, VertexConsumer p_427021_, int p_425137_, boolean p_424854_) {
         this.glyph.renderEffect(this, p_428538_, p_427021_, p_425137_, false);
      }

      @Override
      public RenderType renderType(Font.DisplayMode p_426780_) {
         return this.glyph.renderTypes.select(p_426780_);
      }

      @Override
      public GpuTextureView textureView() {
         return this.glyph.textureView;
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.glyph.renderTypes.guiPipeline();
      }
   }

   @OnlyIn(Dist.CLIENT)
   record GlyphInstance(float x, float y, int color, int shadowColor, BakedSheetGlyph glyph, Style style, float boldOffset, float shadowOffset)
      implements TextRenderable.Styled {
      @Override
      public float left() {
         return this.glyph.left(this);
      }

      @Override
      public float top() {
         return this.glyph.top(this);
      }

      @Override
      public float right() {
         return this.glyph.right(this);
      }

      @Override
      public float activeRight() {
         return this.x + this.glyph.info.getAdvance(this.style.isBold());
      }

      @Override
      public float bottom() {
         return this.glyph.bottom(this);
      }

      boolean hasShadow() {
         return this.shadowColor() != 0;
      }

      @Override
      public void render(Matrix4f p_425380_, VertexConsumer p_429235_, int p_428404_, boolean p_431677_) {
         this.glyph.renderChar(this, p_425380_, p_429235_, p_428404_, p_431677_);
      }

      @Override
      public RenderType renderType(Font.DisplayMode p_427013_) {
         return this.glyph.renderTypes.select(p_427013_);
      }

      @Override
      public GpuTextureView textureView() {
         return this.glyph.textureView;
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.glyph.renderTypes.guiPipeline();
      }
   }
}
