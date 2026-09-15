package net.minecraft.client.renderer.block.model.properties.conditional;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ConditionalBlockModelProperty {
    boolean get(BlockState state);
}