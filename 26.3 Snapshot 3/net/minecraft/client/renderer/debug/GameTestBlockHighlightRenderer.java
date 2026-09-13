package net.minecraft.client.renderer.debug;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.util.Util;
import net.minecraft.world.phys.Vec3;

public class GameTestBlockHighlightRenderer {
   private static final int SHOW_POS_DURATION_MS = 10000;
   private static final float PADDING = 0.02F;
   private final Map<BlockPos, GameTestBlockHighlightRenderer.Marker> markers = Maps.newHashMap();

   public void highlightPos(final BlockPos absolutePos, final BlockPos relativePos) {
      String text = relativePos.toShortString();
      this.markers.put(absolutePos, new GameTestBlockHighlightRenderer.Marker(1610678016, text, Util.getMillis() + 10000L));
   }

   public void clear() {
      this.markers.clear();
   }

   public void emitGizmos() {
      long time = Util.getMillis();
      this.markers.entrySet().removeIf(entry -> time > entry.getValue().removeAtTime);
      this.markers.forEach((pos, marker) -> this.renderMarker(pos, marker));
   }

   private void renderMarker(final BlockPos pos, final GameTestBlockHighlightRenderer.Marker marker) {
      Gizmos.cuboid(pos, 0.02F, GizmoStyle.fill(marker.color()));
      if (!marker.text.isEmpty()) {
         Gizmos.billboardText(marker.text, Vec3.atLowerCornerWithOffset(pos, 0.5, 1.2, 0.5), TextGizmo.Style.whiteAndCentered().withScale(0.16F))
            .setAlwaysOnTop();
      }
   }

   private record Marker(int color, String text, long removeAtTime) {
   }
}
