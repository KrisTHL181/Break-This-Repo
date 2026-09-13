package net.minecraft.client.color;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ColorLerper {
    public static final DyeColor[] MUSIC_NOTE_COLORS = new DyeColor[]{
        DyeColor.WHITE,
        DyeColor.LIGHT_GRAY,
        DyeColor.LIGHT_BLUE,
        DyeColor.BLUE,
        DyeColor.CYAN,
        DyeColor.GREEN,
        DyeColor.LIME,
        DyeColor.YELLOW,
        DyeColor.ORANGE,
        DyeColor.PINK,
        DyeColor.RED,
        DyeColor.MAGENTA
    };

    public static int getLerpedColor(final ColorLerper.Type type, final float tick) {
        int tickCount = Mth.floor(tick);
        int value = tickCount / type.colorDuration;
        int colorCount = type.colors.length;
        int c1 = value % colorCount;
        int c2 = (value + 1) % colorCount;
        float subStep = (tickCount % type.colorDuration + Mth.frac(tick)) / type.colorDuration;
        int color1 = type.getColor(type.colors[c1]);
        int color2 = type.getColor(type.colors[c2]);
        return ARGB.srgbLerp(subStep, color1, color2);
    }

    private static int getModifiedColor(final DyeColor color, final float brightness) {
        if (color == DyeColor.WHITE) {
            return -1644826;
        }

        int src = color.getTextureDiffuseColor();
        return ARGB.color(
            255,
            Mth.clamp(Mth.floor(ARGB.red(src) * brightness), 0, 255),
            Mth.clamp(Mth.floor(ARGB.green(src) * brightness), 0, 255),
            Mth.clamp(Mth.floor(ARGB.blue(src) * brightness), 0, 255)
        );
    }

    @OnlyIn(Dist.CLIENT)
    public enum Type {
        SHEEP(25, DyeColor.values(), 0.75F),
        MUSIC_NOTE(30, ColorLerper.MUSIC_NOTE_COLORS, 1.25F);

        private final int colorDuration;
        private final Map<DyeColor, Integer> colorByDye;
        private final DyeColor[] colors;

        Type(final int colorDuration, final DyeColor[] colors, final float brightness) {
            this.colorDuration = colorDuration;
            this.colorByDye = Maps.newHashMap(
                Arrays.stream(colors).collect(Collectors.toMap(d -> (DyeColor)d, color -> ColorLerper.getModifiedColor(color, brightness)))
            );
            this.colors = colors;
        }

        public final int getColor(final DyeColor dyeColor) {
            return this.colorByDye.get(dyeColor);
        }
    }
}