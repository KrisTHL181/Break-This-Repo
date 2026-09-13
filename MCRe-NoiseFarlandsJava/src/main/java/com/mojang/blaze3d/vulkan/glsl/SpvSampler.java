package com.mojang.blaze3d.vulkan.glsl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
record SpvSampler(String name, int bindingOffset, int dimensions) {
}