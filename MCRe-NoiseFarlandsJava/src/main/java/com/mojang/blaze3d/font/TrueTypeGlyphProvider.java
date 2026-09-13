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
    private final float oversample;
    private final CodepointMap<TrueTypeGlyphProvider.GlyphEntry> glyphs = new CodepointMap<>(
        TrueTypeGlyphProvider.GlyphEntry[]::new, TrueTypeGlyphProvider.GlyphEntry[][]::new
    );

    public TrueTypeGlyphProvider(
        final ByteBuffer fontMemory, final FT_Face face, final float size, final float oversample, final float shiftX, final float shiftY, final String skip
    ) {
        this.fontMemory = fontMemory;
        this.face = face;
        this.oversample = oversample;
        IntSet skipSet = new IntArraySet();
        skip.codePoints().forEach(skipSet::add);
        int pixelsPerEm = Math.round(size * oversample);
        FreeType.FT_Set_Pixel_Sizes(face, pixelsPerEm, pixelsPerEm);
        float transformX = shiftX * oversample;
        float transformY = -shiftY * oversample;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FT_Vector vector = FreeTypeUtil.setVector(FT_Vector.malloc(stack), transformX, transformY);
            FreeType.FT_Set_Transform(face, null, vector);
            IntBuffer indexPtr = stack.mallocInt(1);
            int codepoint = (int)FreeType.FT_Get_First_Char(face, indexPtr);

            while (true) {
                int index = indexPtr.get(0);
                if (index == 0) {
                    return;
                }

                if (!skipSet.contains(codepoint)) {
                    this.glyphs.put(codepoint, new TrueTypeGlyphProvider.GlyphEntry(index));
                }

                codepoint = (int)FreeType.FT_Get_Next_Char(face, codepoint, indexPtr);
            }
        }
    }

    @Override
    public @Nullable UnbakedGlyph getGlyph(final int codepoint) {
        TrueTypeGlyphProvider.GlyphEntry entry = this.glyphs.get(codepoint);
        return entry != null ? this.getOrLoadGlyphInfo(codepoint, entry) : null;
    }

    private UnbakedGlyph getOrLoadGlyphInfo(final int codepoint, final TrueTypeGlyphProvider.GlyphEntry entry) {
        UnbakedGlyph result = entry.glyph;
        if (result == null) {
            FT_Face face = this.validateFontOpen();
            synchronized (face) {
                result = entry.glyph;
                if (result == null) {
                    result = this.loadGlyph(codepoint, face, entry.index);
                    entry.glyph = result;
                }
            }
        }

        return result;
    }

    private UnbakedGlyph loadGlyph(final int codepoint, final FT_Face face, final int index) {
        int errorCode = FreeType.FT_Load_Glyph(face, index, 4194312);
        if (errorCode != 0) {
            FreeTypeUtil.assertError(errorCode, String.format(Locale.ROOT, "Loading glyph U+%06X", codepoint));
        }

        FT_GlyphSlot glyph = face.glyph();
        if (glyph == null) {
            throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", codepoint));
        }

        float scaledAdvance = FreeTypeUtil.x(glyph.advance());
        FT_Bitmap bitmap = glyph.bitmap();
        int left = glyph.bitmap_left();
        int top = glyph.bitmap_top();
        int width = bitmap.width();
        int height = bitmap.rows();
        return width > 0 && height > 0
            ? new TrueTypeGlyphProvider.Glyph(left, top, width, height, scaledAdvance, index)
            : new EmptyGlyph(scaledAdvance / this.oversample);
    }

    private FT_Face validateFontOpen() {
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
    private class Glyph implements UnbakedGlyph {
        private final int width;
        private final int height;
        private final float bearingX;
        private final float bearingY;
        private final GlyphInfo info;
        private final int index;

        private Glyph(final float left, final float top, final int width, final int height, final float advance, final int index) {
            this.width = width;
            this.height = height;
            this.info = GlyphInfo.simple(advance / TrueTypeGlyphProvider.this.oversample);
            this.bearingX = left / TrueTypeGlyphProvider.this.oversample;
            this.bearingY = top / TrueTypeGlyphProvider.this.oversample;
            this.index = index;
        }

        @Override
        public GlyphInfo info() {
            return this.info;
        }

        @Override
        public BakedGlyph bake(final UnbakedGlyph.Stitcher stitcher) {
            return stitcher.stitch(this.info, new GlyphBitmap() {
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
                public void upload(final int x, final int y, final GpuTexture texture) {
                    FT_Face face = TrueTypeGlyphProvider.this.validateFontOpen();

                    try (NativeImage image = new NativeImage(NativeImage.Format.LUMINANCE, Glyph.this.width, Glyph.this.height, false)) {
                        if (image.copyFromFont(face, Glyph.this.index)) {
                            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture, image, 0, 0, x, y);
                        }
                    }
                }

                @Override
                public boolean isColored() {
                    return false;
                }
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static class GlyphEntry {
        private final int index;
        private volatile @Nullable UnbakedGlyph glyph;

        private GlyphEntry(final int index) {
            this.index = index;
        }
    }
}