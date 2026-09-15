package com.mojang.blaze3d.textures;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum FilterMode {
    NEAREST,
    LINEAR;
}