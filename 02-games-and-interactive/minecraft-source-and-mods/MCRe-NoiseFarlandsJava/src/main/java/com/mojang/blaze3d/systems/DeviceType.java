package com.mojang.blaze3d.systems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum DeviceType {
    OTHER,
    INTEGRATED,
    DISCRETE,
    VIRTUAL,
    CPU;
}