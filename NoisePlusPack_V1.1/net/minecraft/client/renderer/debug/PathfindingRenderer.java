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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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
   public void emitGizmos(double p_457376_, double p_453441_, double p_451582_, DebugValueAccess p_457498_, Frustum p_459856_, float p_459719_) {
      p_457498_.forEachEntity(
         DebugSubscriptions.ENTITY_PATHS, (p_448284_, p_448285_) -> renderPath(p_457376_, p_453441_, p_451582_, p_448285_.path(), p_448285_.maxNodeDistance())
      );
   }

   private static void renderPath(double p_270187_, double p_270252_, double p_270371_, Path p_270189_, float p_270841_) {
      renderPath(p_270189_, p_270841_, true, true, p_270187_, p_270252_, p_270371_);
   }

   public static void renderPath(Path p_425995_, float p_422888_, boolean p_457518_, boolean p_453571_, double p_424791_, double p_429196_, double p_428938_) {
      renderPathLine(p_425995_, p_424791_, p_429196_, p_428938_);
      BlockPos blockpos = p_425995_.getTarget();
      if (distanceToCamera(blockpos, p_424791_, p_429196_, p_428938_) <= 80.0F) {
         Gizmos.cuboid(
            new AABB(
               blockpos.getX() + 0.25F,
               blockpos.getY() + 0.25F,
               blockpos.getZ() + 0.25,
               blockpos.getX() + 0.75F,
               blockpos.getY() + 0.75F,
               blockpos.getZ() + 0.75F
            ),
            GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 0.0F, 1.0F, 0.0F))
         );

         for (int i = 0; i < p_425995_.getNodeCount(); i++) {
            Node node = p_425995_.getNode(i);
            if (distanceToCamera(node.asBlockPos(), p_424791_, p_429196_, p_428938_) <= 80.0F) {
               float f = i == p_425995_.getNextNodeIndex() ? 1.0F : 0.0F;
               float f1 = i == p_425995_.getNextNodeIndex() ? 0.0F : 1.0F;
               AABB aabb = new AABB(
                  node.x + 0.5F - p_422888_,
                  node.y + 0.01F * i,
                  node.z + 0.5F - p_422888_,
                  node.x + 0.5F + p_422888_,
                  node.y + 0.25F + 0.01F * i,
                  node.z + 0.5F + p_422888_
               );
               Gizmos.cuboid(aabb, GizmoStyle.fill(ARGB.colorFromFloat(0.5F, f, 0.0F, f1)));
            }
         }
      }

      Path.DebugData path$debugdata = p_425995_.debugData();
      if (p_457518_ && path$debugdata != null) {
         for (Node node2 : path$debugdata.closedSet()) {
            if (distanceToCamera(node2.asBlockPos(), p_424791_, p_429196_, p_428938_) <= 80.0F) {
               Gizmos.cuboid(
                  new AABB(
                     node2.x + 0.5F - p_422888_ / 2.0F,
                     node2.y + 0.01F,
                     node2.z + 0.5F - p_422888_ / 2.0F,
                     node2.x + 0.5F + p_422888_ / 2.0F,
                     node2.y + 0.1,
                     node2.z + 0.5F + p_422888_ / 2.0F
                  ),
                  GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 1.0F, 0.8F, 0.8F))
               );
            }
         }

         for (Node node3 : path$debugdata.openSet()) {
            if (distanceToCamera(node3.asBlockPos(), p_424791_, p_429196_, p_428938_) <= 80.0F) {
               Gizmos.cuboid(
                  new AABB(
                     node3.x + 0.5F - p_422888_ / 2.0F,
                     node3.y + 0.01F,
                     node3.z + 0.5F - p_422888_ / 2.0F,
                     node3.x + 0.5F + p_422888_ / 2.0F,
                     node3.y + 0.1,
                     node3.z + 0.5F + p_422888_ / 2.0F
                  ),
                  GizmoStyle.fill(ARGB.colorFromFloat(0.5F, 0.8F, 1.0F, 1.0F))
               );
            }
         }
      }

      if (p_453571_) {
         for (int j = 0; j < p_425995_.getNodeCount(); j++) {
            Node node1 = p_425995_.getNode(j);
            if (distanceToCamera(node1.asBlockPos(), p_424791_, p_429196_, p_428938_) <= 80.0F) {
               Gizmos.billboardText(
                     String.valueOf(node1.type), new Vec3(node1.x + 0.5, node1.y + 0.75, node1.z + 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.32F)
                  )
                  .setAlwaysOnTop();
               Gizmos.billboardText(
                     String.format(Locale.ROOT, "%.2f", node1.costMalus),
                     new Vec3(node1.x + 0.5, node1.y + 0.25, node1.z + 0.5),
                     TextGizmo.Style.whiteAndCentered().withScale(0.32F)
                  )
                  .setAlwaysOnTop();
            }
         }
      }
   }

   public static void renderPathLine(Path p_270511_, double p_270524_, double p_270163_, double p_270176_) {
      if (p_270511_.getNodeCount() >= 2) {
         Vec3 vec3 = p_270511_.getNode(0).asVec3();

         for (int i = 1; i < p_270511_.getNodeCount(); i++) {
            Node node = p_270511_.getNode(i);
            if (distanceToCamera(node.asBlockPos(), p_270524_, p_270163_, p_270176_) > 80.0F) {
               vec3 = node.asVec3();
            } else {
               float f = (float)i / p_270511_.getNodeCount() * 0.33F;
               int j = ARGB.opaque(Mth.hsvToRgb(f, 0.9F, 0.9F));
               Gizmos.arrow(vec3.add(0.5, 0.5, 0.5), node.asVec3().add(0.5, 0.5, 0.5), j);
               vec3 = node.asVec3();
            }
         }
      }
   }

   private static float distanceToCamera(BlockPos p_113635_, double p_113636_, double p_113637_, double p_113638_) {
      return (float)(Math.abs(p_113635_.getX() - p_113636_) + Math.abs(p_113635_.getY() - p_113637_) + Math.abs(p_113635_.getZ() - p_113638_));
   }
}
