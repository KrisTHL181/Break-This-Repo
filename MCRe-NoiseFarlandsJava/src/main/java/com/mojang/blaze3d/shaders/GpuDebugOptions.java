package com.mojang.blaze3d.shaders;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record GpuDebugOptions(int logLevel, boolean synchronousLogs, boolean useLabels, boolean useValidationLayers) {
}