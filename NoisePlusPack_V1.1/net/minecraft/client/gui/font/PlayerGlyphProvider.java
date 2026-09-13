package net.minecraft.client.gui.font;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PlayerGlyphProvider {
   static final GlyphInfo GLYPH_INFO = GlyphInfo.simple(8.0F);
   final PlayerSkinRenderCache playerSkinRenderCache;
   private final LoadingCache<FontDescription.PlayerSprite, GlyphSource> wrapperCache = CacheBuilder.newBuilder()
      .expireAfterAccess(PlayerSkinRenderCache.CACHE_DURATION)
      .build(
         new CacheLoader<FontDescription.PlayerSprite, GlyphSource>() {
            public GlyphSource load(FontDescription.PlayerSprite p_427054_) {
               final Supplier<PlayerSkinRenderCache.RenderInfo> supplier = PlayerGlyphProvider.this.playerSkinRenderCache.createLookup(p_427054_.profile());
               final boolean flag = p_427054_.hat();
               return new SingleSpriteSource(
                  new BakedGlyph() {
                     @Override
                     public GlyphInfo info() {
                        return PlayerGlyphProvider.GLYPH_INFO;
                     }

                     @Override
                     public TextRenderable.Styled createGlyph(
                        float p_425925_, float p_422420_, int p_425274_, int p_430258_, Style p_422700_, float p_424218_, float p_426381_
                     ) {
                        return new PlayerGlyphProvider.Instance(supplier, flag, p_425925_, p_422420_, p_425274_, p_430258_, p_426381_, p_422700_);
                     }
                  }
               );
            }
         }
      );

   public PlayerGlyphProvider(PlayerSkinRenderCache p_425708_) {
      this.playerSkinRenderCache = p_425708_;
   }

   public GlyphSource sourceForPlayer(FontDescription.PlayerSprite p_428681_) {
      return (GlyphSource)this.wrapperCache.getUnchecked(p_428681_);
   }

   @OnlyIn(Dist.CLIENT)
   record Instance(Supplier<PlayerSkinRenderCache.RenderInfo> skin, boolean hat, float x, float y, int color, int shadowColor, float shadowOffset, Style style)
      implements PlainTextRenderable {
      @Override
      public void renderSprite(Matrix4f p_423329_, VertexConsumer p_426451_, int p_429352_, float p_422691_, float p_426443_, float p_426037_, int p_430247_) {
         float f = p_422691_ + this.left();
         float f1 = p_422691_ + this.right();
         float f2 = p_426443_ + this.top();
         float f3 = p_426443_ + this.bottom();
         renderQuad(p_423329_, p_426451_, p_429352_, f, f1, f2, f3, p_426037_, p_430247_, 8.0F, 8.0F, 8, 8, 64, 64);
         if (this.hat) {
            renderQuad(p_423329_, p_426451_, p_429352_, f, f1, f2, f3, p_426037_, p_430247_, 40.0F, 8.0F, 8, 8, 64, 64);
         }
      }

      private static void renderQuad(
         Matrix4f p_423855_,
         VertexConsumer p_430131_,
         int p_426798_,
         float p_425819_,
         float p_428152_,
         float p_428729_,
         float p_429596_,
         float p_431238_,
         int p_424037_,
         float p_431562_,
         float p_422360_,
         int p_427835_,
         int p_425705_,
         int p_423155_,
         int p_427119_
      ) {
         float f = (p_431562_ + 0.0F) / p_423155_;
         float f1 = (p_431562_ + p_427835_) / p_423155_;
         float f2 = (p_422360_ + 0.0F) / p_427119_;
         float f3 = (p_422360_ + p_425705_) / p_427119_;
         p_430131_.addVertex(p_423855_, p_425819_, p_428729_, p_431238_).setUv(f, f2).setColor(p_424037_).setLight(p_426798_);
         p_430131_.addVertex(p_423855_, p_425819_, p_429596_, p_431238_).setUv(f, f3).setColor(p_424037_).setLight(p_426798_);
         p_430131_.addVertex(p_423855_, p_428152_, p_429596_, p_431238_).setUv(f1, f3).setColor(p_424037_).setLight(p_426798_);
         p_430131_.addVertex(p_423855_, p_428152_, p_428729_, p_431238_).setUv(f1, f2).setColor(p_424037_).setLight(p_426798_);
      }

      @Override
      public RenderType renderType(Font.DisplayMode p_430405_) {
         return this.skin.get().glyphRenderTypes().select(p_430405_);
      }

      @Override
      public RenderPipeline guiPipeline() {
         return this.skin.get().glyphRenderTypes().guiPipeline();
      }

      @Override
      public GpuTextureView textureView() {
         return this.skin.get().textureView();
      }
   }
}
