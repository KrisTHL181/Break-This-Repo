package net.minecraft.client.renderer.blockentity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndGatewayRenderState extends EndPortalRenderState {
    public int height;
    public float scale;
    public int color;
    public float animationTime;
}