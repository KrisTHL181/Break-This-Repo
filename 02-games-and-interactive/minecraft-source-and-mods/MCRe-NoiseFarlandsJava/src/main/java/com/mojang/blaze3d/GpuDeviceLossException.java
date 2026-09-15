package com.mojang.blaze3d;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GpuDeviceLossException extends RuntimeException {
    public GpuDeviceLossException(final String message) {
        super(message);
    }
}