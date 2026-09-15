package net.minecraft.client.gui.font.glyphs;

import net.minecraft.client.gui.font.TextRenderable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface EffectGlyph {
   TextRenderable createEffect(float var1, float var2, float var3, float var4, float var5, int var6, int var7, float var8);
}
