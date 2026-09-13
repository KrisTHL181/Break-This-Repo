package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugStructureInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StructureRenderer implements DebugRenderer.SimpleDebugRenderer {
   @Override
   public void emitGizmos(double p_455468_, double p_459439_, double p_452616_, DebugValueAccess p_453459_, Frustum p_450911_, float p_459559_) {
      p_453459_.forEachChunk(DebugSubscriptions.STRUCTURES, (p_448304_, p_448305_) -> {
         for (DebugStructureInfo debugstructureinfo : p_448305_) {
            Gizmos.cuboid(AABB.of(debugstructureinfo.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F)));

            for (DebugStructureInfo.Piece debugstructureinfo$piece : debugstructureinfo.pieces()) {
               if (debugstructureinfo$piece.isStart()) {
                  Gizmos.cuboid(AABB.of(debugstructureinfo$piece.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.0F, 1.0F, 0.0F)));
               } else {
                  Gizmos.cuboid(AABB.of(debugstructureinfo$piece.boundingBox()), GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.0F, 0.0F, 1.0F)));
               }
            }
         }
      });
   }
}
