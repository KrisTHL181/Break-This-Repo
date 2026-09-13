package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class AtlasGlyphProvider {
   static final GlyphInfo GLYPH_INFO = GlyphInfo.simple(8.0F);
   final TextureAtlas atlas;
   final GlyphRenderTypes renderTypes;
   private final GlyphSource missingWrapper;
   private final Map<Identifier, GlyphSource> wrapperCache = new HashMap<>();
   private final Function<Identifier, GlyphSource> spriteResolver;

   public AtlasGlyphProvider(TextureAtlas p_423776_) {
      this.atlas = p_423776_;
      this.renderTypes = GlyphRenderTypes.createForColorTexture(p_423776_.location());
      TextureAtlasSprite textureatlassprite = p_423776_.missingSprite();
      this.missingWrapper = this.createSprite(textureatlassprite);
      this.spriteResolver = p_447983_ -> {
         TextureAtlasSprite textureatlassprite1 = p_423776_.getSprite(p_447983_);
         return textureatlassprite1 == textureatlassprite ? this.missingWrapper : this.createSprite(textureatlassprite1);
      };
   }

   public GlyphSource sourceForSprite(Identifier p_460421_) {
      return this.wrapperCache.computeIfAbsent(p_460421_, this.spriteResolver);
   }

   private GlyphSource createSprite(final TextureAtlasSprite p_430930_) {
      return new SingleSpriteSource(
         new BakedGlyph() {
            @Override
            public GlyphInfo info() {
               return AtlasGlyphProvider.GLYPH_INFO;
            }

            @Override
            public TextRenderable.Styled createGlyph(
               float p_422755_, float p_422593_, int p_429125_, int p_428352_, Style p_425631_, float p_426190_, float p_429163_
            ) {
               return new AtlasGlyphProvider.Instance(
                  AtlasGlyphProvider.this.renderTypes,
                  AtlasGlyphProvider.this.atlas.getTextureView(),
                  p_430930_,
                  p_422755_,
                  p_422593_,
                  p_429125_,
                  p_428352_,
                  p_429163_,
                  p_425631_
               );
            }
         }
      );
   }

   @OnlyIn(Dist.CLIENT)
   record Instance(
      GlyphRenderTypes renderTypes,
      GpuTextureView textureView,
      TextureAtlasSprite sprite,
      float x,
      float y,
      int color,
      int shadowColor,
      float shadowOffset,
      Style style
   ) implements PlainTextRenderable {
      @Override
      public void renderSprite(Matrix4f p_425478_, VertexConsumer p_429855_, int p_428090_, float p_429012_, float p_431372_, float p_430727_, int p_424820_) {
         float f = p_429012_ + this.left();
         float f1 = p_429012_ + this.right();
         float f2 = p_431372_ + this.top();
         float f3 = p_431372_ + this.bottom();
         p_429855_.addVertex(p_425478_, f, f2, p_430727_).setUv(this.sprite.getU0(), this.sprite.getV0()).setColor(p_424820_).setLight(p_428090_);
         p_429855_.addVertex(p_425478_, f, f3, p_430727_).setUv(this.sprite.getU0(), this.sprite.getV1()).setColor(p_424820_).setLight(p_428090_);
         p_429855_.addVertex(p_425478_, f1, f3, p_430727_).setUv(this.sprite.getU1(), this.sprite.getV1()).setColor(p_424820_).setLight(p_428090_);
         p_429855_.addVertex(p_425478_, f1, f2, p_430727_).setUv(this.sprite.getU1(), this.sprite.getV0()).setColor(p_424820_).setLight(p_428090_);
      }

      @Override
      public RenderType renderType(Font.DisplayMode p_429668_) {
         return this.renderTypes.select(p_429668_);
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.renderTypes.guiPipeline();
      }
   }
}
