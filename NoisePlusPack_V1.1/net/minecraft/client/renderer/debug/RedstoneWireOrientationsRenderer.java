package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RedstoneWireOrientationsRenderer implements DebugRenderer.SimpleDebugRenderer {
   @Override
   public void emitGizmos(double p_456959_, double p_455590_, double p_453399_, DebugValueAccess p_455798_, Frustum p_458798_, float p_453987_) {
      p_455798_.forEachBlock(DebugSubscriptions.REDSTONE_WIRE_ORIENTATIONS, (p_448302_, p_448303_) -> {
         Vec3 vec3 = p_448302_.getBottomCenter().subtract(0.0, 0.1, 0.0);
         Gizmos.arrow(vec3, vec3.add(p_448303_.getFront().getUnitVec3().scale(0.5)), -16776961);
         Gizmos.arrow(vec3, vec3.add(p_448303_.getUp().getUnitVec3().scale(0.4)), -65536);
         Gizmos.arrow(vec3, vec3.add(p_448303_.getSide().getUnitVec3().scale(0.3)), -256);
      });
   }
}
