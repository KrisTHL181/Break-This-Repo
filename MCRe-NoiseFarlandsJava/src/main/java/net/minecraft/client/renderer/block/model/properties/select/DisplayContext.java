package net.minecraft.client.renderer.block.model.properties.select;

import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DisplayContext() implements SelectBlockModelProperty<BlockDisplayContext> {
    public BlockDisplayContext get(final BlockState blockState, final BlockDisplayContext displayContext) {
        return displayContext;
    }
}