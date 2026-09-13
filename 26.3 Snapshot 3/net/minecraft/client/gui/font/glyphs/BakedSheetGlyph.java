package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4fc;

public class BakedSheetGlyph implements EffectGlyph, BakedGlyph {
   public static final float Z_FIGHTER = 0.001F;
   private final GlyphInfo info;
   private final GlyphRenderTypes renderTypes;
   private final GpuTextureView textureView;
   private final float u0;
   private final float u1;
   private final float v0;
   private final float v1;
   private final float left;
   private final float right;
   private final float up;
   private final float down;

   public BakedSheetGlyph(
      final GlyphInfo info,
      final GlyphRenderTypes renderTypes,
      final GpuTextureView textureView,
      final float u0,
      final float u1,
      final float v0,
      final float v1,
      final float left,
      final float right,
      final float up,
      final float down
   ) {
      this.info = info;
      this.renderTypes = renderTypes;
      this.textureView = textureView;
      this.u0 = u0;
      this.u1 = u1;
      this.v0 = v0;
      this.v1 = v1;
      this.left = left;
      this.right = right;
      this.up = up;
      this.down = down;
   }

   private float left(final BakedSheetGlyph.GlyphInstance instance) {
      return instance.x
         + this.left
         + (instance.style.isItalic() ? Math.min(this.shearTop(), this.shearBottom()) : 0.0F)
         - extraThickness(instance.style.isBold());
   }

   private float top(final BakedSheetGlyph.GlyphInstance instance) {
      return instance.y + this.up - extraThickness(instance.style.isBold());
   }

   private float right(final BakedSheetGlyph.GlyphInstance instance) {
      return instance.x
         + this.right
         + (instance.hasShadow() ? instance.shadowOffset : 0.0F)
         + (instance.style.isItalic() ? Math.max(this.shearTop(), this.shearBottom()) : 0.0F)
         + extraThickness(instance.style.isBold());
   }

   private float bottom(final BakedSheetGlyph.GlyphInstance instance) {
      return instance.y + this.down + (instance.hasShadow() ? instance.shadowOffset : 0.0F) + extraThickness(instance.style.isBold());
   }

   private void renderChar(
      final BakedSheetGlyph.GlyphInstance glyphInstance, final Matrix4fc pose, final VertexConsumer buffer, final int packedLightCoords, final boolean flat
   ) {
      Style style = glyphInstance.style();
      boolean italic = style.isItalic();
      float x = glyphInstance.x();
      float y = glyphInstance.y();
      int color = glyphInstance.color();
      boolean bold = style.isBold();
      float zFighter = flat ? 0.0F : 0.001F;
      float depth;
      if (glyphInstance.hasShadow()) {
         int shadowColor = glyphInstance.shadowColor();
         this.render(italic, x + glyphInstance.shadowOffset(), y + glyphInstance.shadowOffset(), 0.0F, pose, buffer, shadowColor, bold, packedLightCoords);
         if (bold) {
            this.render(
               italic,
               x + glyphInstance.boldOffset() + glyphInstance.shadowOffset(),
               y + glyphInstance.shadowOffset(),
               zFighter,
               pose,
               buffer,
               shadowColor,
               true,
               packedLightCoords
            );
         }

         depth = flat ? 0.0F : 0.03F;
      } else {
         depth = 0.0F;
      }

      this.render(italic, x, y, depth, pose, buffer, color, bold, packedLightCoords);
      if (bold) {
         this.render(italic, x + glyphInstance.boldOffset(), y, depth + zFighter, pose, buffer, color, true, packedLightCoords);
      }
   }

   private void render(
      final boolean italic,
      final float x,
      final float y,
      final float z,
      final Matrix4fc pose,
      final VertexConsumer builder,
      final int color,
      final boolean bold,
      final int packedLightCoords
   ) {
      float x0 = x + this.left;
      float x1 = x + this.right;
      float y0 = y + this.up;
      float y1 = y + this.down;
      float shearY0 = italic ? this.shearTop() : 0.0F;
      float shearY1 = italic ? this.shearBottom() : 0.0F;
      float extraThickness = extraThickness(bold);
      builder.addVertex(pose, x0 + shearY0 - extraThickness, y0 - extraThickness, z).setColor(color).setUv(this.u0, this.v0).setLight(packedLightCoords);
      builder.addVertex(pose, x0 + shearY1 - extraThickness, y1 + extraThickness, z).setColor(color).setUv(this.u0, this.v1).setLight(packedLightCoords);
      builder.addVertex(pose, x1 + shearY1 + extraThickness, y1 + extraThickness, z).setColor(color).setUv(this.u1, this.v1).setLight(packedLightCoords);
      builder.addVertex(pose, x1 + shearY0 + extraThickness, y0 - extraThickness, z).setColor(color).setUv(this.u1, this.v0).setLight(packedLightCoords);
   }

   private static float extraThickness(final boolean bold) {
      return bold ? 0.1F : 0.0F;
   }

   private float shearBottom() {
      return 1.0F - 0.25F * this.down;
   }

   private float shearTop() {
      return 1.0F - 0.25F * this.up;
   }

   private void renderEffect(
      final BakedSheetGlyph.EffectInstance effect, final Matrix4fc pose, final VertexConsumer buffer, final int packedLightCoords, final boolean flat
   ) {
      float depth = flat ? 0.0F : effect.depth;
      if (effect.hasShadow()) {
         this.buildEffect(effect, effect.shadowOffset(), depth, effect.shadowColor(), buffer, packedLightCoords, pose);
         depth += flat ? 0.0F : 0.03F;
      }

      this.buildEffect(effect, 0.0F, depth, effect.color, buffer, packedLightCoords, pose);
   }

   private void buildEffect(
      final BakedSheetGlyph.EffectInstance effect,
      final float offset,
      final float z,
      final int color,
      final VertexConsumer buffer,
      final int packedLightCoords,
      final Matrix4fc pose
   ) {
      buffer.addVertex(pose, effect.x0 + offset, effect.y1 + offset, z).setColor(color).setUv(this.u0, this.v0).setLight(packedLightCoords);
      buffer.addVertex(pose, effect.x1 + offset, effect.y1 + offset, z).setColor(color).setUv(this.u0, this.v1).setLight(packedLightCoords);
      buffer.addVertex(pose, effect.x1 + offset, effect.y0 + offset, z).setColor(color).setUv(this.u1, this.v1).setLight(packedLightCoords);
      buffer.addVertex(pose, effect.x0 + offset, effect.y0 + offset, z).setColor(color).setUv(this.u1, this.v0).setLight(packedLightCoords);
   }

   @Override
   public GlyphInfo info() {
      return this.info;
   }

   @Override
   public TextRenderable.Styled createGlyph(
      final float x, final float y, final int color, final int shadowColor, final Style style, final float boldOffset, final float shadowOffset
   ) {
      return new BakedSheetGlyph.GlyphInstance(x, y, color, shadowColor, this, style, boldOffset, shadowOffset);
   }

   @Override
   public TextRenderable createEffect(
      final float x0, final float y0, final float x1, final float y1, final float depth, final int color, final int shadowColor, final float shadowOffset
   ) {
      return new BakedSheetGlyph.EffectInstance(this, x0, y0, x1, y1, depth, color, shadowColor, shadowOffset);
   }

   private record EffectInstance(BakedSheetGlyph glyph, float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset)
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

      private boolean hasShadow() {
         return this.shadowColor() != 0;
      }

      @Override
      public void render(final Matrix4fc pose, final VertexConsumer buffer, final int packedLightCoords, final boolean flat) {
         this.glyph.renderEffect(this, pose, buffer, packedLightCoords, false);
      }

      @Override
      public RenderType renderType(final Font.DisplayMode displayMode) {
         return this.glyph.renderTypes.select(displayMode);
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

   private record GlyphInstance(float x, float y, int color, int shadowColor, BakedSheetGlyph glyph, Style style, float boldOffset, float shadowOffset)
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

      private boolean hasShadow() {
         return this.shadowColor() != 0;
      }

      @Override
      public void render(final Matrix4fc pose, final VertexConsumer buffer, final int packedLightCoords, final boolean flat) {
         this.glyph.renderChar(this, pose, buffer, packedLightCoords, flat);
      }

      @Override
      public RenderType renderType(final Font.DisplayMode displayMode) {
         return this.glyph.renderTypes.select(displayMode);
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
