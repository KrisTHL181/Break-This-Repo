package net.minecraft.util;

import com.mojang.serialization.Codec;
import java.util.HexFormat;

public record ColorRGBA(int rgba) {
    public static final Codec<ColorRGBA> CODEC = ExtraCodecs.STRING_ARGB_COLOR.xmap(ColorRGBA::new, ColorRGBA::rgba);

    @Override
    public String toString() {
        return HexFormat.of().toHexDigits(this.rgba, 8);
    }
}