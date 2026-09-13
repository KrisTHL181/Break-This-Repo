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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaterDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public WaterDebugRenderer(Minecraft p_113717_) {
      this.minecraft = p_113717_;
   }

   @Override
   public void emitGizmos(double p_450806_, double p_455408_, double p_457906_, DebugValueAccess p_456178_, Frustum p_458278_, float p_450196_) {
      BlockPos blockpos = this.minecraft.player.blockPosition();
      LevelReader levelreader = this.minecraft.player.level();

      for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-10, -10, -10), blockpos.offset(10, 10, 10))) {
         FluidState fluidstate = levelreader.getFluidState(blockpos1);
         if (fluidstate.is(FluidTags.WATER)) {
            double d0 = blockpos1.getY() + fluidstate.getHeight(levelreader, blockpos1);
            Gizmos.cuboid(
               new AABB(blockpos1.getX() + 0.01F, blockpos1.getY() + 0.01F, blockpos1.getZ() + 0.01F, blockpos1.getX() + 0.99F, d0, blockpos1.getZ() + 0.99F),
               GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 0.0F, 1.0F, 0.0F))
            );
         }
      }

      for (BlockPos blockpos2 : BlockPos.betweenClosed(blockpos.offset(-10, -10, -10), blockpos.offset(10, 10, 10))) {
         FluidState fluidstate1 = levelreader.getFluidState(blockpos2);
         if (fluidstate1.is(FluidTags.WATER)) {
            Gizmos.billboardText(
               String.valueOf(fluidstate1.getAmount()),
               Vec3.atLowerCornerWithOffset(blockpos2, 0.5, fluidstate1.getHeight(levelreader, blockpos2), 0.5),
               TextGizmo.Style.forColorAndCentered(-16777216)
            );
         }
      }
   }
}
