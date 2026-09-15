package com.mojang.blaze3d.vulkan.glsl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShaderCompileException extends Exception {
    public ShaderCompileException(final String message) {
        super(message);
    }
}