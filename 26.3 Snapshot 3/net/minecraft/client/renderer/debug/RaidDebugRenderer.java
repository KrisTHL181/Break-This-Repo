package net.minecraft.client.renderer.debug;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.phys.Vec3;

public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private static final float TEXT_SCALE = 0.64F;
   private final Minecraft minecraft;

   public RaidDebugRenderer(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      BlockPos playerPos = this.getCamera().blockPosition();
      debugValues.forEachChunk(DebugSubscriptions.RAIDS, (chunkPos, raidCenters) -> {
         for (BlockPos raidCenter : raidCenters) {
            if (playerPos.closerThan(raidCenter, 160.0)) {
               highlightRaidCenter(raidCenter);
            }
         }
      });
   }

   private static void highlightRaidCenter(final BlockPos raidCenter) {
      Gizmos.cuboid(raidCenter, GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 1.0F, 0.0F, 0.0F)));
      renderTextOverBlock("Raid center", raidCenter, -65536);
   }

   private static void renderTextOverBlock(final String text, final BlockPos pos, final int color) {
      Gizmos.billboardText(text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.3, 0.5), TextGizmo.Style.forColor(color).withScale(0.64F)).setAlwaysOnTop();
   }

   private Camera getCamera() {
      return this.minecraft.gameRenderer.mainCamera();
   }
}
