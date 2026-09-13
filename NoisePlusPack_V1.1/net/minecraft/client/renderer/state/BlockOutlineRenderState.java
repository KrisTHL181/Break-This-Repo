package net.minecraft.client.renderer.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record BlockOutlineRenderState(
   BlockPos pos,
   boolean isTranslucent,
   boolean highContrast,
   VoxelShape shape,
   @Nullable VoxelShape collisionShape,
   @Nullable VoxelShape occlusionShape,
   @Nullable VoxelShape interactionShape
) {
   public BlockOutlineRenderState(BlockPos p_425529_, boolean p_426298_, boolean p_426881_, VoxelShape p_424013_) {
      this(p_425529_, p_426298_, p_426881_, p_424013_, null, null, null);
   }
}
