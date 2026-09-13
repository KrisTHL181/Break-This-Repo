package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WaterDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public WaterDebugRenderer(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      BlockPos pos = this.minecraft.player.blockPosition();
      LevelReader level = this.minecraft.player.level();

      for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-10, -10, -10), pos.offset(10, 10, 10))) {
         FluidState fluidState = level.getFluidState(blockPos);
         if (fluidState.is(FluidTags.WATER)) {
            double height = blockPos.getY() + fluidState.getHeight(level, blockPos);
            Gizmos.cuboid(
               new AABB(blockPos.getX() + 0.01F, blockPos.getY() + 0.01F, blockPos.getZ() + 0.01F, blockPos.getX() + 0.99F, height, blockPos.getZ() + 0.99F),
               GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 0.0F, 1.0F, 0.0F))
            );
         }
      }

      for (BlockPos blockPos : BlockPos.betweenClosed(pos.offset(-10, -10, -10), pos.offset(10, 10, 10))) {
         FluidState fluidState = level.getFluidState(blockPos);
         if (fluidState.is(FluidTags.WATER)) {
            Gizmos.billboardText(
               String.valueOf(fluidState.getAmount()),
               Vec3.atLowerCornerWithOffset(blockPos, 0.5, fluidState.getHeight(level, blockPos), 0.5),
               TextGizmo.Style.forColorAndCentered(-16777216)
            );
         }
      }
   }
}
