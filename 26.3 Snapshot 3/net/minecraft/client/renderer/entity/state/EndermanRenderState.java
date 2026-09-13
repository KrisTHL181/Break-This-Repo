package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;

public class EndermanRenderState extends HumanoidRenderState {
   public boolean isCreepy;
   public final BlockModelRenderState carriedBlock = new BlockModelRenderState();
}
