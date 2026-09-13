package net.minecraft.client.renderer.debug;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.debug.DebugGoalInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GoalSelectorDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private final Minecraft minecraft;

   public GoalSelectorDebugRenderer(Minecraft p_113546_) {
      this.minecraft = p_113546_;
   }

   @Override
   public void emitGizmos(double p_455320_, double p_459189_, double p_460977_, DebugValueAccess p_455810_, Frustum p_451428_, float p_457276_) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      BlockPos blockpos = BlockPos.containing(camera.position().x, 0.0, camera.position().z);
      p_455810_.forEachEntity(DebugSubscriptions.GOAL_SELECTORS, (p_448260_, p_448261_) -> {
         if (blockpos.closerThan(p_448260_.blockPosition(), 160.0)) {
            for (int i = 0; i < p_448261_.goals().size(); i++) {
               DebugGoalInfo.DebugGoal debuggoalinfo$debuggoal = p_448261_.goals().get(i);
               double d0 = p_448260_.getBlockX() + 0.5;
               double d1 = p_448260_.getY() + 2.0 + i * 0.25;
               double d2 = p_448260_.getBlockZ() + 0.5;
               int j = debuggoalinfo$debuggoal.isRunning() ? -16711936 : -3355444;
               Gizmos.billboardText(debuggoalinfo$debuggoal.name(), new Vec3(d0, d1, d2), TextGizmo.Style.forColorAndCentered(j));
            }
         }
      });
   }
}
