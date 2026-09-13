package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

public interface BoundingBoxRenderable {
   BoundingBoxRenderable.Mode renderMode();

   BoundingBoxRenderable.RenderableBox getRenderableBox();

   enum Mode {
      NONE,
      BOX,
      BOX_AND_INVISIBLE_BLOCKS;
   }

   record RenderableBox(BlockPos localPos, Vec3i size) {
      public static BoundingBoxRenderable.RenderableBox fromCorners(int p_397818_, int p_393600_, int p_393481_, int p_394705_, int p_392453_, int p_392682_) {
         int i = Math.min(p_397818_, p_394705_);
         int j = Math.min(p_393600_, p_392453_);
         int k = Math.min(p_393481_, p_392682_);
         return new BoundingBoxRenderable.RenderableBox(
            new BlockPos(i, j, k), new Vec3i(Math.max(p_397818_, p_394705_) - i, Math.max(p_393600_, p_392453_) - j, Math.max(p_393481_, p_392682_) - k)
         );
      }
   }
}
