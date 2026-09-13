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

public class CollisionBoxRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<VoxelShape> shapes = Collections.emptyList();

   public CollisionBoxRenderer(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      double time = Util.getNanos();
      if (time - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = time;
         Entity cameraEntity = this.minecraft.getCameraEntity();
         this.shapes = ImmutableList.copyOf(cameraEntity.level().getCollisions(cameraEntity, cameraEntity.getBoundingBox().inflate(6.0)));
      }

      for (VoxelShape shape : this.shapes) {
         GizmoStyle style = GizmoStyle.stroke(-1);

         for (AABB aabb : shape.toAabbs()) {
            Gizmos.cuboid(aabb, style);
         }
      }
   }
}
