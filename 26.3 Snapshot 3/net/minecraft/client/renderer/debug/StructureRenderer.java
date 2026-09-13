package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;

public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      debugValues.forEachChunk(DebugSubscriptions.STRUCTURES, (chunkPos, structures) -> {
         for (DebugStructureInfo structure : structures) {
            Gizmos.cuboid(AABB.of(structure.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F)));

            for (DebugStructureInfo.Piece piece : structure.pieces()) {
               if (piece.isStart()) {
                  Gizmos.cuboid(AABB.of(piece.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F)));
               } else {
                  Gizmos.cuboid(AABB.of(piece.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.0F, 0.0F, 1.0F)));
               }
            }
         }
      });
   }
}
