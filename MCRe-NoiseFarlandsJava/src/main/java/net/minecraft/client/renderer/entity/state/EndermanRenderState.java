package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndermanRenderState extends HumanoidRenderState {
    public boolean isCreepy;
    public final BlockModelRenderState carriedBlock = new BlockModelRenderState();
}