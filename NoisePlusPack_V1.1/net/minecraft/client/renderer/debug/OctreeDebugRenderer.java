package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableInt;

@OnlyIn(Dist.CLIENT)
public class OctreeDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;

   public OctreeDebugRenderer(Minecraft p_368722_) {
      this.minecraft = p_368722_;
   }

   @Override
   public void emitGizmos(double p_452565_, double p_460683_, double p_451087_, DebugValueAccess p_459317_, Frustum p_460934_, float p_452900_) {
      Octree octree = this.minecraft.levelRenderer.getSectionOcclusionGraph().getOctree();
      MutableInt mutableint = new MutableInt(0);
      octree.visitNodes((p_448277_, p_448278_, p_448279_, p_448280_) -> this.renderNode(p_448277_, p_448279_, p_448278_, mutableint, p_448280_), p_460934_, 32);
   }

   private void renderNode(Octree.Node p_365618_, int p_362077_, boolean p_364236_, MutableInt p_366104_, boolean p_362959_) {
      AABB aabb = p_365618_.getAABB();
      double d0 = aabb.getXsize();
      long i = Math.round(d0 / 16.0);
      if (i == 1L) {
         p_366104_.add(1);
         int j = p_362959_ ? -16711936 : -1;
         Gizmos.billboardText(String.valueOf(p_366104_.intValue()), aabb.getCenter(), TextGizmo.Style.forColorAndCentered(j).withScale(4.8F));
      }

      long k = i + 5L;
      Gizmos.cuboid(
         aabb.deflate(0.1 * p_362077_),
         GizmoStyle.stroke(ARGB.colorFromFloat(p_364236_ ? 0.4F : 1.0F, getColorComponent(k, 0.3F), getColorComponent(k, 0.8F), getColorComponent(k, 0.5F)))
      );
   }

   private static float getColorComponent(long p_368917_, float p_363248_) {
      float f = 0.1F;
      return Mth.frac(p_363248_ * (float)p_368917_) * 0.9F + 0.1F;
   }
}
