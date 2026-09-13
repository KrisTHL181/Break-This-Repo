package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphBitmap;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.nio.file.Path;
import java.util.function.Supplier;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.client.renderer.texture.DynamicAtlasTree;
import net.minecraft.client.renderer.texture.DynamicAtlasTreeSlot;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

public class FontTexture extends AbstractTexture implements Dumpable {
   private static final int SIZE = 256;
   private static final int SPACING = 1;
   private final GlyphRenderTypes renderTypes;
   private final boolean colored;
   private final DynamicAtlasTree tree = new DynamicAtlasTree(0, 0, 256, 256);

   public FontTexture(final Supplier<String> label, final GlyphRenderTypes renderTypes, final boolean colored) {
      this.colored = colored;
      GpuDevice device = RenderSystem.getDevice();
      this.texture = device.createTexture(label, 7, colored ? GpuFormat.RGBA8_UNORM : GpuFormat.R8_UNORM, 256, 256, 1, 1);
      this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
      this.textureView = device.createTextureView(this.texture);
      this.renderTypes = renderTypes;
   }

   public @Nullable BakedSheetGlyph add(final GlyphInfo info, final GlyphBitmap glyph) {
      if (glyph.isColored() != this.colored) {
         return null;
      } else {
         DynamicAtlasTreeSlot slot = this.tree.insert(glyph.getPixelWidth(), glyph.getPixelHeight(), 1);
         if (slot != null) {
            glyph.upload(slot.x(), slot.y(), this.getTexture());
            float width = 256.0F;
            float height = 256.0F;
            float nudge = 0.01F;
            return new BakedSheetGlyph(
               info,
               this.renderTypes,
               this.getTextureView(),
               (slot.x() + 0.01F) / 256.0F,
               (slot.x() - 0.01F + glyph.getPixelWidth()) / 256.0F,
               (slot.y() + 0.01F) / 256.0F,
               (slot.y() - 0.01F + glyph.getPixelHeight()) / 256.0F,
               glyph.getLeft(),
               glyph.getRight(),
               glyph.getTop(),
               glyph.getBottom()
            );
         } else {
            return null;
         }
      }
   }

   @Override
   public void dumpContents(final Identifier selfId, final Path dir) {
      if (this.texture != null) {
         String outputId = selfId.toDebugFileName();
         TextureUtil.writeAsPNG(dir, outputId, this.texture, 0, argb -> ARGB.alpha(argb) == 0 ? -16777216 : argb);
      }
   }
}
