package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HoglinRenderState extends LivingEntityRenderState {
    public int attackAnimationRemainingTicks;
    public boolean isConverting;
}