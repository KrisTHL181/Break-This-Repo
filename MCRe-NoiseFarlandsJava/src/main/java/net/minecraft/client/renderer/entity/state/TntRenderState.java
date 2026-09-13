package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TntRenderState extends EntityRenderState {
    public float fuseRemainingInTicks;
    public final BlockModelRenderState blockState = new BlockModelRenderState();
}