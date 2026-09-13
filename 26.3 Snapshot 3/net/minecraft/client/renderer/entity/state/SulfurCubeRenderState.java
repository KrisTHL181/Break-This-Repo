package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;

public class SulfurCubeRenderState extends SlimeRenderState {
   public BlockModelRenderState containedBlock = new BlockModelRenderState();
   public float fuseRemainingTicks;
}
