package net.minecraft.client.gui.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GlyphRenderTypes(RenderType normal, RenderType seeThrough, RenderType polygonOffset, RenderPipeline guiPipeline) {
   public static GlyphRenderTypes createForIntensityTexture(Identifier p_453613_) {
      return new GlyphRenderTypes(
         RenderTypes.textIntensity(p_453613_),
         RenderTypes.textIntensitySeeThrough(p_453613_),
         RenderTypes.textIntensityPolygonOffset(p_453613_),
         RenderPipelines.GUI_TEXT_INTENSITY
      );
   }

   public static GlyphRenderTypes createForColorTexture(Identifier p_453239_) {
      return new GlyphRenderTypes(
         RenderTypes.text(p_453239_), RenderTypes.textSeeThrough(p_453239_), RenderTypes.textPolygonOffset(p_453239_), RenderPipelines.GUI_TEXT
      );
   }

   public RenderType select(Font.DisplayMode p_285259_) {
      return switch (p_285259_) {
         case NORMAL -> this.normal;
         case SEE_THROUGH -> this.seeThrough;
         case POLYGON_OFFSET -> this.polygonOffset;
      };
   }
}
