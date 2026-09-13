package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FontTexture extends AbstractTexture implements Dumpable {
   private static final int SIZE = 256;
   private final GlyphRenderTypes renderTypes;
   private final boolean colored;
   private final FontTexture.Node root;

   public FontTexture(Supplier<String> p_394627_, GlyphRenderTypes p_285000_, boolean p_285085_) {
      this.colored = p_285085_;
      this.root = new FontTexture.Node(0, 0, 256, 256);
      GpuDevice gpudevice = RenderSystem.getDevice();
      this.texture = gpudevice.createTexture(p_394627_, 7, p_285085_ ? TextureFormat.RGBA8 : TextureFormat.RED8, 256, 256, 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = gpudevice.createTextureView(this.texture);
      this.renderTypes = p_285000_;
   }

   public @Nullable BakedSheetGlyph add(GlyphInfo p_431409_, GlyphBitmap p_422613_) {
      if (p_422613_.isColored() != this.colored) {
         return null;
      } else {
         FontTexture.Node fonttexture$node = this.root.insert(p_422613_);
         if (fonttexture$node != null) {
            p_422613_.upload(fonttexture$node.x, fonttexture$node.y, this.getTexture());
            float f = 256.0F;
            float f1 = 256.0F;
            float f2 = 0.01F;
            return new BakedSheetGlyph(
               p_431409_,
               this.renderTypes,
               this.getTextureView(),
               (fonttexture$node.x + 0.01F) / 256.0F,
               (fonttexture$node.x - 0.01F + p_422613_.getPixelWidth()) / 256.0F,
               (fonttexture$node.y + 0.01F) / 256.0F,
               (fonttexture$node.y - 0.01F + p_422613_.getPixelHeight()) / 256.0F,
               p_422613_.getLeft(),
               p_422613_.getRight(),
               p_422613_.getTop(),
               p_422613_.getBottom()
            );
         } else {
            return null;
         }
      }
   }

   @Override
   public void dumpContents(Identifier p_455188_, Path p_285511_) {
      if (this.texture != null) {
         String s = p_455188_.toDebugFileName();
         TextureUtil.writeAsPNG(p_285511_, s, this.texture, 0, p_285145_ -> (p_285145_ & 0xFF000000) == 0 ? -16777216 : p_285145_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Node {
      final int x;
      final int y;
      private final int width;
      private final int height;
      private FontTexture.@Nullable Node left;
      private FontTexture.@Nullable Node right;
      private boolean occupied;

      Node(int p_95113_, int p_95114_, int p_95115_, int p_95116_) {
         this.x = p_95113_;
         this.y = p_95114_;
         this.width = p_95115_;
         this.height = p_95116_;
      }

      FontTexture.@Nullable Node insert(GlyphBitmap p_425794_) {
         if (this.left != null && this.right != null) {
            FontTexture.Node fonttexture$node = this.left.insert(p_425794_);
            if (fonttexture$node == null) {
               fonttexture$node = this.right.insert(p_425794_);
            }

            return fonttexture$node;
         } else {
            if (this.occupied) {
               return null;
            }

            int i = p_425794_.getPixelWidth();
            int j = p_425794_.getPixelHeight();
            if (i > this.width || j > this.height) {
               return null;
            }

            if (i == this.width && j == this.height) {
               this.occupied = true;
               return this;
            }

            int k = this.width - i;
            int l = this.height - j;
            if (k > l) {
               this.left = new FontTexture.Node(this.x, this.y, i, this.height);
               this.right = new FontTexture.Node(this.x + i + 1, this.y, this.width - i - 1, this.height);
            } else {
               this.left = new FontTexture.Node(this.x, this.y, this.width, j);
               this.right = new FontTexture.Node(this.x, this.y + j + 1, this.width, this.height - j - 1);
            }

            return this.left.insert(p_425794_);
         }
      }
   }
}
