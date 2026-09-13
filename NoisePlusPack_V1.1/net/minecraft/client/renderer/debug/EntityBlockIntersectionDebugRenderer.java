package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityBlockIntersectionDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float PADDING = 0.02F;

   @Override
   public void emitGizmos(double p_458928_, double p_457002_, double p_455426_, DebugValueAccess p_455996_, Frustum p_451094_, float p_452713_) {
      p_455996_.forEachBlock(
         DebugSubscriptions.ENTITY_BLOCK_INTERSECTIONS, (p_448246_, p_448247_) -> Gizmos.cuboid(p_448246_, 0.02F, GizmoStyle.fill(p_448247_.color()))
      );
   }
}
