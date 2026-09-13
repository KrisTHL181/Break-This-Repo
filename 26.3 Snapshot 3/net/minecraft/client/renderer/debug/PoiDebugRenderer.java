package net.minecraft.client.renderer.debug;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugPoiInfo;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;

public class PoiDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
   private static final float TEXT_SCALE = 0.32F;
   private static final int ORANGE = -23296;
   private final BrainDebugRenderer brainRenderer;

   public PoiDebugRenderer(final BrainDebugRenderer brainRenderer) {
      this.brainRenderer = brainRenderer;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      BlockPos playerPos = BlockPos.containing(camX, camY, camZ);
      debugValues.forEachBlock(DebugSubscriptions.POIS, (pos, poi) -> {
         if (playerPos.closerThan(pos, 30.0)) {
            highlightPoi(pos);
            this.renderPoiInfo(poi, debugValues);
         }
      });
      this.brainRenderer.getGhostPois(debugValues).forEach((poiPos, value) -> {
         if (debugValues.getBlockValue(DebugSubscriptions.POIS, poiPos) == null) {
            if (playerPos.closerThan(poiPos, 30.0)) {
               this.renderGhostPoi(poiPos, (List<String>)value);
            }
         }
      });
   }

   private static void highlightPoi(final BlockPos poiPos) {
      float padding = 0.05F;
      Gizmos.cuboid(poiPos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
   }

   private void renderGhostPoi(final BlockPos poiPos, final List<String> names) {
      float padding = 0.05F;
      Gizmos.cuboid(poiPos, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
      Gizmos.billboardTextOverBlock(names.toString(), poiPos, 0, -256, 0.32F);
      Gizmos.billboardTextOverBlock("Ghost POI", poiPos, 1, -65536, 0.32F);
   }

   private void renderPoiInfo(final DebugPoiInfo poi, final DebugValueAccess debugValues) {
      int row = 0;
      if (SharedConstants.DEBUG_BRAIN) {
         List<String> ticketHolderNames = this.getTicketHolderNames(poi, false, debugValues);
         if (ticketHolderNames.size() < 4) {
            renderTextOverPoi("Owners: " + ticketHolderNames, poi, row, -256);
         } else {
            renderTextOverPoi(ticketHolderNames.size() + " ticket holders", poi, row, -256);
         }

         row++;
         List<String> potentialTicketHolderNames = this.getTicketHolderNames(poi, true, debugValues);
         if (potentialTicketHolderNames.size() < 4) {
            renderTextOverPoi("Candidates: " + potentialTicketHolderNames, poi, row, -23296);
         } else {
            renderTextOverPoi(potentialTicketHolderNames.size() + " potential owners", poi, row, -23296);
         }

         row++;
      }

      renderTextOverPoi("Free tickets: " + poi.freeTicketCount(), poi, row, -256);
      renderTextOverPoi(poi.poiType().getRegisteredName(), poi, ++row, -1);
   }

   private static void renderTextOverPoi(final String text, final DebugPoiInfo poi, final int row, final int color) {
      Gizmos.billboardTextOverBlock(text, poi.pos(), row, color, 0.32F);
   }

   private List<String> getTicketHolderNames(final DebugPoiInfo poi, final boolean potential, final DebugValueAccess debugValues) {
      List<String> names = new ArrayList<>();
      debugValues.forEachEntity(DebugSubscriptions.BRAINS, (entity, brainDump) -> {
         boolean include = potential ? brainDump.hasPotentialPoi(poi.pos()) : brainDump.hasPoi(poi.pos());
         if (include) {
            names.add(DebugEntityNameGenerator.getEntityName(entity.getUUID()));
         }
      });
      return names;
   }
}
