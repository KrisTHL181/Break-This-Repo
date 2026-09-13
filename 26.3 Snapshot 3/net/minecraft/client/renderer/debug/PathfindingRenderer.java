package net.minecraft.client.renderer.debug;

import java.util.Locale;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PathfindingRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float MAX_RENDER_DIST = 80.0F;
   private static final int MAX_TARGETING_DIST = 8;
   private static final boolean SHOW_ONLY_SELECTED = false;
   private static final boolean SHOW_OPEN_CLOSED = true;
   private static final boolean SHOW_OPEN_CLOSED_COST_MALUS = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_TEXT = false;
   private static final boolean SHOW_OPEN_CLOSED_NODE_TYPE_WITH_BOX = true;
   private static final boolean SHOW_GROUND_LABELS = true;
   private static final float TEXT_SCALE = 0.32F;

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      debugValues.forEachEntity(DebugSubscriptions.ENTITY_PATHS, (entity, info) -> renderPath(camX, camY, camZ, info.path(), info.maxNodeDistance()));
   }

   private static void renderPath(final double camX, final double camY, final double camZ, final Path path, final float maxNodeDistance) {
      renderPath(path, maxNodeDistance, true, true, camX, camY, camZ);
   }

   public static void renderPath(
      final Path path,
      final float maxNodeDistance,
      final boolean renderOpenAndClosedSets,
      final boolean renderGroundLabels,
      final double camX,
      final double camY,
      final double camZ
   ) {
      renderPathLine(path, camX, camY, camZ);
      BlockPos pos = path.getTarget();
      if (distanceToCamera(pos, camX, camY, camZ) <= 80.0F) {
         Gizmos.cuboid(
            new AABB(pos.getX() + 0.25F, pos.getY() + 0.25F, pos.getZ() + 0.25, pos.getX() + 0.75F, pos.getY() + 0.75F, pos.getZ() + 0.75F),
            GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 0.0F, 1.0F, 0.0F))
         );

         for (int i = 0; i < path.getNodeCount(); i++) {
            Node n = path.getNode(i);
            if (distanceToCamera(n.asBlockPos(), camX, camY, camZ) <= 80.0F) {
               float r = i == path.getNextNodeIndex() ? 1.0F : 0.0F;
               float b = i == path.getNextNodeIndex() ? 0.0F : 1.0F;
               AABB aabb = new AABB(
                  n.x + 0.5F - maxNodeDistance,
                  n.y + 0.01F * i,
                  n.z + 0.5F - maxNodeDistance,
                  n.x + 0.5F + maxNodeDistance,
                  n.y + 0.25F + 0.01F * i,
                  n.z + 0.5F + maxNodeDistance
               );
               Gizmos.cuboid(aabb, GizmoStyle.fill(ARGB.colorFromFloat(0.5F, r, 0.0F, b)));
            }
         }
      }

      Path.DebugData debugData = path.debugData();
      if (renderOpenAndClosedSets && debugData != null) {
         for (Node node : debugData.closedSet()) {
            if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= 80.0F) {
               Gizmos.cuboid(
                  new AABB(
                     node.x + 0.5F - maxNodeDistance / 2.0F,
                     node.y + 0.01F,
                     node.z + 0.5F - maxNodeDistance / 2.0F,
                     node.x + 0.5F + maxNodeDistance / 2.0F,
                     node.y + 0.1,
                     node.z + 0.5F + maxNodeDistance / 2.0F
                  ),
                  GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 1.0F, 0.8F, 0.8F))
               );
            }
         }

         for (Node node : debugData.openSet()) {
            if (distanceToCamera(node.asBlockPos(), camX, camY, camZ) <= 80.0F) {
               Gizmos.cuboid(
                  new AABB(
                     node.x + 0.5F - maxNodeDistance / 2.0F,
                     node.y + 0.01F,
                     node.z + 0.5F - maxNodeDistance / 2.0F,
                     node.x + 0.5F + maxNodeDistance / 2.0F,
                     node.y + 0.1,
                     node.z + 0.5F + maxNodeDistance / 2.0F
                  ),
                  GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 0.8F, 1.0F, 1.0F))
               );
            }
         }
      }

      if (renderGroundLabels) {
         for (int i = 0; i < path.getNodeCount(); i++) {
            Node n = path.getNode(i);
            if (distanceToCamera(n.asBlockPos(), camX, camY, camZ) <= 80.0F) {
               Gizmos.billboardText(String.valueOf(n.type), new Vec3(n.x + 0.5, n.y + 0.75, n.z + 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.32F))
                  .setAlwaysOnTop();
               Gizmos.billboardText(
                     String.format(Locale.ROOT, "%.2f", n.costMalus),
                     new Vec3(n.x + 0.5, n.y + 0.25, n.z + 0.5),
                     TextGizmo.Style.whiteAndCentered().withScale(0.32F)
                  )
                  .setAlwaysOnTop();
            }
         }
      }
   }

   public static void renderPathLine(final Path path, final double camX, final double camY, final double camZ) {
      if (path.getNodeCount() >= 2) {
         Vec3 last = path.getNode(0).asVec3();

         for (int i = 1; i < path.getNodeCount(); i++) {
            Node n = path.getNode(i);
            if (distanceToCamera(n.asBlockPos(), camX, camY, camZ) > 80.0F) {
               last = n.asVec3();
            } else {
               float hue = (float)i / path.getNodeCount() * 0.33F;
               int color = ARGB.opaque(Mth.hsvToRgb(hue, 0.9F, 0.9F));
               Gizmos.arrow(last.add(0.5, 0.5, 0.5), n.asVec3().add(0.5, 0.5, 0.5), color);
               last = n.asVec3();
            }
         }
      }
   }

   private static float distanceToCamera(final BlockPos n, final double camX, final double camY, final double camZ) {
      return (float)(Math.abs(n.getX() - camX) + Math.abs(n.getY() - camY) + Math.abs(n.getZ() - camZ));
   }
}
