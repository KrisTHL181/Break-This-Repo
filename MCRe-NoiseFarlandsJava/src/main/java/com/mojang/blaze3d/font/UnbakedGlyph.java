package com.mojang.blaze3d.font;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface UnbakedGlyph {
    GlyphInfo info();

    BakedGlyph bake(UnbakedGlyph.Stitcher stitcher);

    @OnlyIn(Dist.CLIENT)
    interface Stitcher {
        BakedGlyph stitch(GlyphInfo info, GlyphBitmap glyphBitmap);

        BakedGlyph getMissing();
    }
}