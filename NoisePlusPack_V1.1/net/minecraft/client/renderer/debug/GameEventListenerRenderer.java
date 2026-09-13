package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameEventListenerRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float BOX_HEIGHT = 1.0F;

   private void forEachListener(DebugValueAccess p_424618_, GameEventListenerRenderer.ListenerVisitor p_426833_) {
      p_424618_.forEachBlock(
         DebugSubscriptions.GAME_EVENT_LISTENERS, (p_420988_, p_420989_) -> p_426833_.accept(p_420988_.getCenter(), p_420989_.listenerRadius())
      );
      p_424618_.forEachEntity(
         DebugSubscriptions.GAME_EVENT_LISTENERS, (p_420998_, p_420999_) -> p_426833_.accept(p_420998_.position(), p_420999_.listenerRadius())
      );
   }

   @Override
   public void emitGizmos(double p_460033_, double p_456539_, double p_451893_, DebugValueAccess p_456647_, Frustum p_458372_, float p_453803_) {
      this.forEachListener(p_456647_, (p_448248_, p_448249_) -> {
         double d0 = p_448249_ * 2.0;
         Gizmos.cuboid(AABB.ofSize(p_448248_, d0, d0, d0), GizmoStyle.fill(ARGB.colorFromFloat(0.35F, 1.0F, 1.0F, 0.0F)));
      });
      this.forEachListener(
         p_456647_,
         (p_448250_, p_448251_) -> Gizmos.cuboid(
            AABB.ofSize(p_448250_, 0.5, 1.0, 0.5).move(0.0, 0.5, 0.0), GizmoStyle.fill(ARGB.colorFromFloat(0.35F, 1.0F, 1.0F, 0.0F))
         )
      );
      this.forEachListener(
         p_456647_,
         (p_448255_, p_448256_) -> {
            Gizmos.billboardText("Listener Origin", p_448255_.add(0.0, 1.8, 0.0), TextGizmo.Style.whiteAndCentered().withScale(0.4F));
            Gizmos.billboardText(
               BlockPos.containing(p_448255_).toString(), p_448255_.add(0.0, 1.5, 0.0), TextGizmo.Style.forColorAndCentered(-6959665).withScale(0.4F)
            );
         }
      );
      p_456647_.forEachEvent(DebugSubscriptions.GAME_EVENTS, (p_448252_, p_448253_, p_448254_) -> {
         Vec3 vec3 = p_448252_.pos();
         double d0 = 0.4;
         AABB aabb = AABB.ofSize(vec3.add(0.0, 0.5, 0.0), 0.4, 0.9, 0.4);
         Gizmos.cuboid(aabb, GizmoStyle.fill(ARGB.colorFromFloat(0.2F, 1.0F, 1.0F, 1.0F)));
         Gizmos.billboardText(p_448252_.event().getRegisteredName(), vec3.add(0.0, 0.85, 0.0), TextGizmo.Style.forColorAndCentered(-7564911).withScale(0.12F));
      });
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface ListenerVisitor {
      void accept(Vec3 var1, int var2);
   }
}
