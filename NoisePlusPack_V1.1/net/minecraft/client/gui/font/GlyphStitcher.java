package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class GlyphStitcher implements AutoCloseable {
   private final TextureManager textureManager;
   private final Identifier texturePrefix;
   private final List<FontTexture> textures = new ArrayList<>();

   public GlyphStitcher(TextureManager p_422485_, Identifier p_457908_) {
      this.textureManager = p_422485_;
      this.texturePrefix = p_457908_;
   }

   public void reset() {
      int i = this.textures.size();
      this.textures.clear();

      for (int j = 0; j < i; j++) {
         this.textureManager.release(this.textureName(j));
      }
   }

   @Override
   public void close() {
      this.reset();
   }

   public @Nullable BakedSheetGlyph stitch(GlyphInfo p_431180_, GlyphBitmap p_429310_) {
      for (FontTexture fonttexture : this.textures) {
         BakedSheetGlyph bakedsheetglyph = fonttexture.add(p_431180_, p_429310_);
         if (bakedsheetglyph != null) {
            return bakedsheetglyph;
         }
      }

      int i = this.textures.size();
      Identifier identifier = this.textureName(i);
      boolean flag = p_429310_.isColored();
      GlyphRenderTypes glyphrendertypes = flag ? GlyphRenderTypes.createForColorTexture(identifier) : GlyphRenderTypes.createForIntensityTexture(identifier);
      FontTexture fonttexture1 = new FontTexture(identifier::toString, glyphrendertypes, flag);
      this.textures.add(fonttexture1);
      this.textureManager.register(identifier, fonttexture1);
      return fonttexture1.add(p_431180_, p_429310_);
   }

   private Identifier textureName(int p_427918_) {
      return this.texturePrefix.withSuffix("/" + p_427918_);
   }
}
