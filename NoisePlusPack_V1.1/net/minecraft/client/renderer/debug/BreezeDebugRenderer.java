package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int JUMP_TARGET_LINE_COLOR = ARGB.color(255, 255, 100, 255);
   private static final int TARGET_LINE_COLOR = ARGB.color(255, 100, 255, 255);
   private static final int INNER_CIRCLE_COLOR = ARGB.color(255, 0, 255, 0);
   private static final int MIDDLE_CIRCLE_COLOR = ARGB.color(255, 255, 165, 0);
   private static final int OUTER_CIRCLE_COLOR = ARGB.color(255, 255, 0, 0);
   private final Minecraft minecraft;

   public BreezeDebugRenderer(Minecraft p_312673_) {
      this.minecraft = p_312673_;
   }

   @Override
   public void emitGizmos(double p_456015_, double p_459937_, double p_453117_, DebugValueAccess p_453001_, Frustum p_450599_, float p_458405_) {
      ClientLevel clientlevel = this.minecraft.level;
      p_453001_.forEachEntity(
         DebugSubscriptions.BREEZES,
         (p_448244_, p_448245_) -> {
            p_448245_.attackTarget()
               .map(clientlevel::getEntity)
               .map(p_357967_ -> p_357967_.getPosition(this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true)))
               .ifPresent(p_448242_ -> {
                  Gizmos.arrow(p_448244_.position(), p_448242_, TARGET_LINE_COLOR);
                  Vec3 vec3 = p_448242_.add(0.0, 0.01F, 0.0);
                  Gizmos.circle(vec3, 4.0F, GizmoStyle.stroke(INNER_CIRCLE_COLOR));
                  Gizmos.circle(vec3, 8.0F, GizmoStyle.stroke(MIDDLE_CIRCLE_COLOR));
                  Gizmos.circle(vec3, 24.0F, GizmoStyle.stroke(OUTER_CIRCLE_COLOR));
               });
            p_448245_.jumpTarget().ifPresent(p_448240_ -> {
               Gizmos.arrow(p_448244_.position(), p_448240_.getCenter(), JUMP_TARGET_LINE_COLOR);
               Gizmos.cuboid(AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(p_448240_)), GizmoStyle.fill(ARGB.colorFromFloat(1.0F, 1.0F, 0.0F, 0.0F)));
            });
         }
      );
   }
}
