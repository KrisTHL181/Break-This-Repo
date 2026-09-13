package com.mojang.blaze3d.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

@OnlyIn(Dist.CLIENT)
public class TrueTypeGlyphProvider implements GlyphProvider {
   private @Nullable ByteBuffer fontMemory;
   private @Nullable FT_Face face;
   final float oversample;
   private final CodepointMap<TrueTypeGlyphProvider.GlyphEntry> glyphs = new CodepointMap<>(
      TrueTypeGlyphProvider.GlyphEntry[]::new, TrueTypeGlyphProvider.GlyphEntry[][]::new
   );

   public TrueTypeGlyphProvider(ByteBuffer p_83846_, FT_Face p_330978_, float p_83848_, float p_83849_, float p_83850_, float p_83851_, String p_83852_) {
      this.fontMemory = p_83846_;
      this.face = p_330978_;
      this.oversample = p_83849_;
      IntSet intset = new IntArraySet();
      p_83852_.codePoints().forEach(intset::add);
      int i = Math.round(p_83848_ * p_83849_);
      FreeType.FT_Set_Pixel_Sizes(p_330978_, i, i);
      float f = p_83850_ * p_83849_;
      float f1 = -p_83851_ * p_83849_;
      MemoryStack memorystack = MemoryStack.stackPush();

      try {
         FT_Vector ft_vector = FreeTypeUtil.setVector(FT_Vector.malloc(memorystack), f, f1);
         FreeType.FT_Set_Transform(p_330978_, null, ft_vector);
         IntBuffer intbuffer = memorystack.mallocInt(1);
         int j = (int)FreeType.FT_Get_First_Char(p_330978_, intbuffer);

         while (true) {
            int k = intbuffer.get(0);
            if (k == 0) {
               break;
            }

            if (!intset.contains(j)) {
               this.glyphs.put(j, new TrueTypeGlyphProvider.GlyphEntry(k));
            }

            j = (int)FreeType.FT_Get_Next_Char(p_330978_, j, intbuffer);
         }
      } catch (Throwable var18) {
         if (memorystack != null) {
            try {
               memorystack.close();
            } catch (Throwable var17) {
               var18.addSuppressed(var17);
            }
         }

         throw var18;
      }

      if (memorystack != null) {
         memorystack.close();
      }
   }

   @Override
   public @Nullable UnbakedGlyph getGlyph(int p_231116_) {
      TrueTypeGlyphProvider.GlyphEntry truetypeglyphprovider$glyphentry = this.glyphs.get(p_231116_);
      return truetypeglyphprovider$glyphentry != null ? this.getOrLoadGlyphInfo(p_231116_, truetypeglyphprovider$glyphentry) : null;
   }

   private UnbakedGlyph getOrLoadGlyphInfo(int p_369372_, TrueTypeGlyphProvider.GlyphEntry p_364388_) {
      UnbakedGlyph unbakedglyph = p_364388_.glyph;
      if (unbakedglyph == null) {
         FT_Face ft_face = this.validateFontOpen();
         synchronized (ft_face) {
            unbakedglyph = p_364388_.glyph;
            if (unbakedglyph == null) {
               unbakedglyph = this.loadGlyph(p_369372_, ft_face, p_364388_.index);
               p_364388_.glyph = unbakedglyph;
            }
         }
      }

      return unbakedglyph;
   }

   private UnbakedGlyph loadGlyph(int p_362561_, FT_Face p_369907_, int p_362974_) {
      int i = FreeType.FT_Load_Glyph(p_369907_, p_362974_, 4194312);
      if (i != 0) {
         FreeTypeUtil.assertError(i, String.format(Locale.ROOT, "Loading glyph U+%06X", p_362561_));
      }

      FT_GlyphSlot ft_glyphslot = p_369907_.glyph();
      if (ft_glyphslot == null) {
         throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", p_362561_));
      }

      float f = FreeTypeUtil.x(ft_glyphslot.advance());
      FT_Bitmap ft_bitmap = ft_glyphslot.bitmap();
      int j = ft_glyphslot.bitmap_left();
      int k = ft_glyphslot.bitmap_top();
      int l = ft_bitmap.width();
      int i1 = ft_bitmap.rows();
      return l > 0 && i1 > 0 ? new TrueTypeGlyphProvider.Glyph(j, k, l, i1, f, p_362974_) : new EmptyGlyph(f / this.oversample);
   }

   FT_Face validateFontOpen() {
      if (this.fontMemory != null && this.face != null) {
         return this.face;
      } else {
         throw new IllegalStateException("Provider already closed");
      }
   }

   @Override
   public void close() {
      if (this.face != null) {
         synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            FreeTypeUtil.checkError(FreeType.FT_Done_Face(this.face), "Deleting face");
         }

         this.face = null;
      }

      MemoryUtil.memFree(this.fontMemory);
      this.fontMemory = null;
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return this.glyphs.keySet();
   }

   @OnlyIn(Dist.CLIENT)
   class Glyph implements UnbakedGlyph {
      final int width;
      final int height;
      final float bearingX;
      final float bearingY;
      private final GlyphInfo info;
      final int index;

      Glyph(final float p_83886_, final float p_83887_, final int p_83882_, final int p_83883_, final float p_331469_, final int p_83884_) {
         this.width = p_83882_;
         this.height = p_83883_;
         this.info = GlyphInfo.simple(p_331469_ / TrueTypeGlyphProvider.this.oversample);
         this.bearingX = p_83886_ / TrueTypeGlyphProvider.this.oversample;
         this.bearingY = p_83887_ / TrueTypeGlyphProvider.this.oversample;
         this.index = p_83884_;
      }

      @Override
      public GlyphInfo info() {
         return this.info;
      }

      @Override
      public BakedGlyph bake(UnbakedGlyph.Stitcher p_425303_) {
         return p_425303_.stitch(
            this.info,
            new GlyphBitmap() {
               @Override
               public int getPixelWidth() {
                  return Glyph.this.width;
               }

               @Override
               public int getPixelHeight() {
                  return Glyph.this.height;
               }

               @Override
               public float getOversample() {
                  return TrueTypeGlyphProvider.this.oversample;
               }

               @Override
               public float getBearingLeft() {
                  return Glyph.this.bearingX;
               }

               @Override
               public float getBearingTop() {
                  return Glyph.this.bearingY;
               }

               @Override
               public void upload(int p_231126_, int p_231127_, GpuTexture p_391563_) {
                  FT_Face ft_face = TrueTypeGlyphProvider.this.validateFontOpen();

                  try (NativeImage nativeimage = new NativeImage(NativeImage.Format.LUMINANCE, Glyph.this.width, Glyph.this.height, false)) {
                     if (nativeimage.copyFromFont(ft_face, Glyph.this.index)) {
                        RenderSystem.getDevice()
                           .createCommandEncoder()
                           .writeToTexture(p_391563_, nativeimage, 0, 0, p_231126_, p_231127_, Glyph.this.width, Glyph.this.height, 0, 0);
                     }
                  }
               }

               @Override
               public boolean isColored() {
                  return false;
               }
            }
         );
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class GlyphEntry {
      final int index;
      volatile @Nullable UnbakedGlyph glyph;

      GlyphEntry(int p_362332_) {
         this.index = p_362332_;
      }
   }
}
