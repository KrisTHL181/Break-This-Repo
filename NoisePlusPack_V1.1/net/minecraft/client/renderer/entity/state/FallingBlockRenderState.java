package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FallingBlockRenderState extends EntityRenderState {
   public MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
}
