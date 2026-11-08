package net.minecraft.client.gui.font.glyphs;

import net.minecraft.client.gui.font.TextRenderable;

public interface EffectGlyph {
   TextRenderable createEffect(float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset);
}
