package com.mojang.blaze3d.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.client.gui.font.FontOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface GlyphProvider extends AutoCloseable {
    float BASELINE = 7.0F;

    @Override
    default void close() {
    }

    default @Nullable UnbakedGlyph getGlyph(final int codepoint) {
        return null;
    }

    IntSet getSupportedGlyphs();

    @OnlyIn(Dist.CLIENT)
    record Conditional(GlyphProvider provider, FontOption.Filter filter) implements AutoCloseable {
        @Override
        public void close() {
            this.provider.close();
        }
    }
}