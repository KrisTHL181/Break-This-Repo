package com.mojang.blaze3d.font;

import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface UnbakedGlyph {
   GlyphInfo info();

   BakedGlyph bake(UnbakedGlyph.Stitcher var1);

   @OnlyIn(Dist.CLIENT)
   interface Stitcher {
      BakedGlyph stitch(GlyphInfo var1, GlyphBitmap var2);

      BakedGlyph getMissing();
   }
}
