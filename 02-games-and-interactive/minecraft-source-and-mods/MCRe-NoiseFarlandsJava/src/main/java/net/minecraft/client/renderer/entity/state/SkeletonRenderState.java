package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkeletonRenderState extends HumanoidRenderState {
    public boolean isAggressive;
    public boolean isShaking;
    public boolean isHoldingBow;
}