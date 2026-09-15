package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoatRenderState extends LivingEntityRenderState {
    public boolean hasLeftHorn = true;
    public boolean hasRightHorn = true;
    public float rammingXHeadRot;
}