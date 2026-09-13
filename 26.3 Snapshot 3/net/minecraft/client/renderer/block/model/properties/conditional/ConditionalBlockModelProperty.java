package net.minecraft.client.renderer.block.model.properties.conditional;

import net.minecraft.world.level.block.state.BlockState;

public interface ConditionalBlockModelProperty {
   boolean get(BlockState state);
}
