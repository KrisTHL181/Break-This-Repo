package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;

public class EntityBlockIntersectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float PADDING = 0.02F;

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      debugValues.forEachBlock(DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, (pos, type) -> Gizmos.cuboid(pos, 0.02F, GizmoStyle.fill(type.color())));
   }
}
