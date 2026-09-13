package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import org.jspecify.annotations.Nullable;

public class AllMissingGlyphProvider implements GlyphProvider {
   private static final UnbakedGlyph MISSING_INSTANCE = new UnbakedGlyph() {
      @Override
      public GlyphInfo info() {
         return SpecialGlyphs.MISSING;
      }

      @Override
      public BakedGlyph bake(final UnbakedGlyph.Stitcher stitcher) {
         return stitcher.getMissing();
      }
   };

   @Override
   public @Nullable UnbakedGlyph getGlyph(final int codepoint) {
      return MISSING_INSTANCE;
   }

   @Override
   public IntSet getSupportedGlyphs() {
      return IntSets.EMPTY_SET;
   }
}
