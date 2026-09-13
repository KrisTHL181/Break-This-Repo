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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public SolidFaceRenderer(Minecraft p_113668_) {
      this.minecraft = p_113668_;
   }

   @Override
   public void emitGizmos(double p_456403_, double p_460298_, double p_454096_, DebugValueAccess p_455418_, Frustum p_451346_, float p_454660_) {
      BlockGetter blockgetter = this.minecraft.player.level();
      BlockPos blockpos = BlockPos.containing(p_456403_, p_460298_, p_454096_);

      for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-6, -6, -6), blockpos.offset(6, 6, 6))) {
         BlockState blockstate = blockgetter.getBlockState(blockpos1);
         if (!blockstate.is(Blocks.AIR)) {
            VoxelShape voxelshape = blockstate.getShape(blockgetter, blockpos1);

            for (AABB aabb : voxelshape.toAabbs()) {
               AABB aabb1 = aabb.move(blockpos1).inflate(0.002);
               int i = -2130771968;
               Vec3 vec3 = aabb1.getMinPosition();
               Vec3 vec31 = aabb1.getMaxPosition();
               addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.WEST, vec3, vec31, -2130771968);
               addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.SOUTH, vec3, vec31, -2130771968);
               addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.EAST, vec3, vec31, -2130771968);
               addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.NORTH, vec3, vec31, -2130771968);
               addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.DOWN, vec3, vec31, -2130771968);
               addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.UP, vec3, vec31, -2130771968);
            }
         }
      }
   }

   private static void addFaceIfSturdy(
      BlockPos p_455237_, BlockState p_453370_, BlockGetter p_455824_, Direction p_460036_, Vec3 p_453026_, Vec3 p_452496_, int p_456858_
   ) {
      if (p_453370_.isFaceSturdy(p_455824_, p_455237_, p_460036_)) {
         Gizmos.rect(p_453026_, p_452496_, p_460036_, GizmoStyle.fill(p_456858_));
      }
   }
}
