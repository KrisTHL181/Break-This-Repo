package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.platform.CompareOp;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DepthStencilState(CompareOp depthTest, boolean writeDepth, float depthBiasScaleFactor, float depthBiasConstant) {
    public static final DepthStencilState DEFAULT = new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true);

    public DepthStencilState(final CompareOp depthTest, final boolean depthWrite) {
        this(depthTest, depthWrite, 0.0F, 0.0F);
    }
}