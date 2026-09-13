package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameTestBlockHighlightRenderer {
   private static final int SHOW_POS_DURATION_MS = 10000;
   private static final float PADDING = 0.02F;
   private final Map<BlockPos, GameTestBlockHighlightRenderer.Marker> markers = Maps.newHashMap();

   public void highlightPos(BlockPos p_423694_, BlockPos p_426252_) {
      String s = p_426252_.toShortString();
      this.markers.put(p_423694_, new GameTestBlockHighlightRenderer.Marker(1610678016, s, Util.getMillis() + 10000L));
   }

   public void clear() {
      this.markers.clear();
   }

   public void emitGizmos() {
      long i = Util.getMillis();
      this.markers.entrySet().removeIf(p_427590_ -> i > p_427590_.getValue().removeAtTime);
      this.markers.forEach((p_448257_, p_448258_) -> this.renderMarker(p_448257_, p_448258_));
   }

   private void renderMarker(BlockPos p_430532_, GameTestBlockHighlightRenderer.Marker p_426048_) {
      Gizmos.cuboid(p_430532_, 0.02F, GizmoStyle.fill(p_426048_.color()));
      if (!p_426048_.text.isEmpty()) {
         Gizmos.billboardText(p_426048_.text, Vec3.atLowerCornerWithOffset(p_430532_, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16F))
            .setAlwaysOnTop();
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Marker(int color, String text, long removeAtTime) {
   }
}
