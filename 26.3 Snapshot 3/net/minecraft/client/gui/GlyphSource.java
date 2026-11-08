package net.minecraft.client.gui;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.util.RandomSource;

public interface GlyphSource {
   BakedGlyph getGlyph(int codepoint);

   BakedGlyph getRandomGlyph(RandomSource random, int width);
}
