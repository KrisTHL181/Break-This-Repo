package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.BlendOp;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record BlendEquation(BlendFactor sourceFactor, BlendFactor destFactor, BlendOp op) {
}