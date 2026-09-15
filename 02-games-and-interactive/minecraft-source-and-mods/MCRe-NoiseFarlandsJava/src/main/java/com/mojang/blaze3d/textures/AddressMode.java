package com.mojang.blaze3d.textures;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum AddressMode {
    REPEAT,
    CLAMP_TO_EDGE;
}