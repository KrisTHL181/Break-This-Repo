package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CollisionBoxRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<VoxelShape> shapes = Collections.emptyList();

   public CollisionBoxRenderer(Minecraft p_113404_) {
      this.minecraft = p_113404_;
   }

   @Override
   public void emitGizmos(double p_457034_, double p_455722_, double p_456276_, DebugValueAccess p_450353_, Frustum p_452229_, float p_450695_) {
      double d0 = Util.getNanos();
      if (d0 - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = d0;
         Entity entity = this.minecraft.gameRenderer.getMainCamera().entity();
         this.shapes = ImmutableList.copyOf(entity.level().getCollisions(entity, entity.getBoundingBox().inflate(6.0)));
      }

      for (VoxelShape voxelshape : this.shapes) {
         GizmoStyle gizmostyle = GizmoStyle.stroke(-1);

         for (AABB aabb : voxelshape.toAabbs()) {
            Gizmos.cuboid(aabb, gizmostyle);
         }
      }
   }
}
