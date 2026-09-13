package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockDisplayEntityRenderState extends DisplayEntityRenderState {
    public final BlockModelRenderState blockModel = new BlockModelRenderState();

    @Override
    public boolean hasSubState() {
        return !this.blockModel.isEmpty();
    }
}