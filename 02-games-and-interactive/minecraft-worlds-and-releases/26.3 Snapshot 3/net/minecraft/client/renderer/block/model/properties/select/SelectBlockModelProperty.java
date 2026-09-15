package net.minecraft.client.renderer.block.model.properties.select;

import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public interface SelectBlockModelProperty<T> {
   @Nullable T get(BlockState blockState, BlockDisplayContext displayContext);
}
