package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.GpuFormat;
import java.util.Locale;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record VertexFormatElement(String name, int offset, GpuFormat format) {
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s %s offset:%d", this.name, this.format, this.offset);
    }
}