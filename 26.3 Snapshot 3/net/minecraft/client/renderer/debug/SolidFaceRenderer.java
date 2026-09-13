package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public SolidFaceRenderer(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      BlockGetter level = this.minecraft.player.level();
      BlockPos playerPos = BlockPos.containing(camX, camY, camZ);

      for (BlockPos blockPos : BlockPos.betweenClosed(playerPos.offset(-6, -6, -6), playerPos.offset(6, 6, 6))) {
         BlockState blockState = level.getBlockState(blockPos);
         if (!blockState.is(Blocks.AIR)) {
            VoxelShape shape = blockState.getShape(level, blockPos);

            for (AABB outlineBox : shape.toAabbs()) {
               AABB aabb = outlineBox.move(blockPos).inflate(0.002);
               int color = -2130771968;
               Vec3 min = aabb.getMinPosition();
               Vec3 max = aabb.getMaxPosition();
               addFaceIfSturdy(blockPos, blockState, level, Direction.WEST, min, max, -2130771968);
               addFaceIfSturdy(blockPos, blockState, level, Direction.SOUTH, min, max, -2130771968);
               addFaceIfSturdy(blockPos, blockState, level, Direction.EAST, min, max, -2130771968);
               addFaceIfSturdy(blockPos, blockState, level, Direction.NORTH, min, max, -2130771968);
               addFaceIfSturdy(blockPos, blockState, level, Direction.DOWN, min, max, -2130771968);
               addFaceIfSturdy(blockPos, blockState, level, Direction.UP, min, max, -2130771968);
            }
         }
      }
   }

   private static void addFaceIfSturdy(
      final BlockPos blockPos,
      final BlockState blockState,
      final BlockGetter level,
      final Direction direction,
      final Vec3 cornerA,
      final Vec3 cornerB,
      final int color
   ) {
      if (blockState.isFaceSturdy(level, blockPos, direction)) {
         Gizmos.rect(cornerA, cornerB, direction, GizmoStyle.fill(color));
      }
   }
}
