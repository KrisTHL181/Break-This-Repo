package com.mojang.blaze3d.systems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SurfaceException extends Exception {
    public SurfaceException(final String message) {
        super(message);
    }

    public SurfaceException(final Throwable cause) {
        super(cause);
    }
}