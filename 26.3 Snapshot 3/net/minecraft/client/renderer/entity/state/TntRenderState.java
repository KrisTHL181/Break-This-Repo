package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;

public class TntRenderState extends EntityRenderState {
   public float fuseRemainingInTicks;
   public final BlockModelRenderState blockState = new BlockModelRenderState();
}
