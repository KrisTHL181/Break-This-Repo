package net.minecraft.client.renderer.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NeighborsUpdateRenderer implements DebugRenderer.SimpleDebugRenderer {
   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      int shrinkTime = DebugSubscriptions.NEIGHBOR_UPDATES.expireAfterTicks();
      double shrinkSpeed = 1.0 / (shrinkTime * 2);
      Map<BlockPos, NeighborsUpdateRenderer.LastUpdate> lastUpdates = new HashMap<>();
      debugValues.forEachEvent(DebugSubscriptions.NEIGHBOR_UPDATES, (blockPos, remainingTicks, totalLifetime) -> {
         long age = totalLifetime - remainingTicks;
         NeighborsUpdateRenderer.LastUpdate lastUpdatex = lastUpdates.getOrDefault(blockPos, NeighborsUpdateRenderer.LastUpdate.NONE);
         lastUpdates.put(blockPos, lastUpdatex.tryCount((int)age));
      });

      for (Entry<BlockPos, NeighborsUpdateRenderer.LastUpdate> entry : lastUpdates.entrySet()) {
         BlockPos pos = entry.getKey();
         NeighborsUpdateRenderer.LastUpdate lastUpdate = entry.getValue();
         AABB aabb = new AABB(pos).inflate(0.002).deflate(shrinkSpeed * lastUpdate.age);
         Gizmos.cuboid(aabb, GizmoStyle.stroke(-1));
      }

      for (Entry<BlockPos, NeighborsUpdateRenderer.LastUpdate> entry : lastUpdates.entrySet()) {
         BlockPos pos = entry.getKey();
         NeighborsUpdateRenderer.LastUpdate lastUpdate = entry.getValue();
         Gizmos.billboardText(String.valueOf(lastUpdate.count), Vec3.atCenterOf(pos), TextGizmo.Style.whiteAndCentered());
      }
   }

   private record LastUpdate(int count, int age) {
      private static final NeighborsUpdateRenderer.LastUpdate NONE = new NeighborsUpdateRenderer.LastUpdate(0, Integer.MAX_VALUE);

      public NeighborsUpdateRenderer.LastUpdate tryCount(final int age) {
         if (age == this.age) {
            return new NeighborsUpdateRenderer.LastUpdate(this.count + 1, age);
         } else {
            return age < this.age ? new NeighborsUpdateRenderer.LastUpdate(1, age) : this;
         }
      }
   }
}
