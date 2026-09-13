package net.minecraft.client.renderer.state.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public record BlockOutlineRenderState(
   BlockPos pos,
   boolean isTranslucent,
   boolean highContrast,
   VoxelShape shape,
   @Nullable VoxelShape collisionShape,
   @Nullable VoxelShape occlusionShape,
   @Nullable VoxelShape interactionShape
) {
   public BlockOutlineRenderState(final BlockPos pos, final boolean isTranslucent, final boolean highContrast, final VoxelShape shape) {
      this(pos, isTranslucent, highContrast, shape, null, null, null);
   }
}
