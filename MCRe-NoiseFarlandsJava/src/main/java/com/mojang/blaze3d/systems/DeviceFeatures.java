package com.mojang.blaze3d.systems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DeviceFeatures(
    boolean shaderDrawParameters,
    boolean multiDrawDirectInterleaved,
    boolean multiDrawDirectSeparate,
    boolean multiDrawIndirect,
    boolean drawIndirect,
    boolean nonZeroFirstInstance,
    boolean persistentMapping
) {
}