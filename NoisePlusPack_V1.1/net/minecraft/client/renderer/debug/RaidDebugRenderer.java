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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST = 160;
   private static final float TEXT_SCALE = 0.64F;
   private final Minecraft minecraft;

   public RaidDebugRenderer(Minecraft p_113650_) {
      this.minecraft = p_113650_;
   }

   @Override
   public void emitGizmos(double p_458985_, double p_451680_, double p_455675_, DebugValueAccess p_458843_, Frustum p_458492_, float p_450251_) {
      BlockPos blockpos = this.getCamera().blockPosition();
      p_458843_.forEachChunk(DebugSubscriptions.RAIDS, (p_448300_, p_448301_) -> {
         for (BlockPos blockpos1 : p_448301_) {
            if (blockpos.closerThan(blockpos1, 160.0)) {
               highlightRaidCenter(blockpos1);
            }
         }
      });
   }

   private static void highlightRaidCenter(BlockPos p_270208_) {
      Gizmos.cuboid(p_270208_, GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 1.0F, 0.0F, 0.0F)));
      renderTextOverBlock("Raid center", p_270208_, -65536);
   }

   private static void renderTextOverBlock(String p_270237_, BlockPos p_270941_, int p_270307_) {
      Gizmos.billboardText(p_270237_, Vec3.atLowerCornerWithOffset(p_270941_, 0.5, 1.3, 0.5), TextGizmo.Style.forColor(p_270307_).withScale(0.64F))
         .setAlwaysOnTop();
   }

   private Camera getCamera() {
      return this.minecraft.gameRenderer.getMainCamera();
   }
}
