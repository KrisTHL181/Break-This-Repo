package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SquidRenderState extends LivingEntityRenderState {
    public float tentacleAngle;
    public float xBodyRot;
    public float zBodyRot;
}