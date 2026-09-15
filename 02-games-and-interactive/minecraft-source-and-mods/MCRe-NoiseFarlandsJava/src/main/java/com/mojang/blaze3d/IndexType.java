package com.mojang.blaze3d;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum IndexType {
    SHORT(2),
    INT(4);

    public final int bytes;

    IndexType(final int bytes) {
        this.bytes = bytes;
    }

    public static IndexType least(final int length) {
        return (length & -65536) != 0 ? INT : SHORT;
    }
}