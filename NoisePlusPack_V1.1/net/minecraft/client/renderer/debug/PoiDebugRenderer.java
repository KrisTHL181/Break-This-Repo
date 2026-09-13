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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PoiDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final int MAX_RENDER_DIST_FOR_POI_INFO = 30;
   private static final float TEXT_SCALE = 0.32F;
   private static final int ORANGE = -23296;
   private final BrainDebugRenderer brainRenderer;

   public PoiDebugRenderer(BrainDebugRenderer p_423648_) {
      this.brainRenderer = p_423648_;
   }

   @Override
   public void emitGizmos(double p_452403_, double p_458022_, double p_456589_, DebugValueAccess p_455719_, Frustum p_454926_, float p_450596_) {
      BlockPos blockpos = BlockPos.containing(p_452403_, p_458022_, p_456589_);
      p_455719_.forEachBlock(DebugSubscriptions.POIS, (p_448297_, p_448298_) -> {
         if (blockpos.closerThan(p_448297_, 30.0)) {
            highlightPoi(p_448297_);
            this.renderPoiInfo(p_448298_, p_455719_);
         }
      });
      this.brainRenderer.getGhostPois(p_455719_).forEach((p_448293_, p_448294_) -> {
         if (p_455719_.getBlockValue(DebugSubscriptions.POIS, p_448293_) == null && blockpos.closerThan(p_448293_, 30.0)) {
            this.renderGhostPoi(p_448293_, (List<String>)p_448294_);
         }
      });
   }

   private static void highlightPoi(BlockPos p_430422_) {
      float f = 0.05F;
      Gizmos.cuboid(p_430422_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
   }

   private void renderGhostPoi(BlockPos p_429608_, List<String> p_428316_) {
      float f = 0.05F;
      Gizmos.cuboid(p_429608_, 0.05F, GizmoStyle.fill(ARGB.colorFromFloat(0.3F, 0.2F, 0.2F, 1.0F)));
      Gizmos.billboardTextOverBlock(p_428316_.toString(), p_429608_, 0, -256, 0.32F);
      Gizmos.billboardTextOverBlock("Ghost POI", p_429608_, 1, -65536, 0.32F);
   }

   private void renderPoiInfo(DebugPoiInfo p_430302_, DebugValueAccess p_428830_) {
      int i = 0;
      if (SharedConstants.DEBUG_BRAIN) {
         List<String> list = this.getTicketHolderNames(p_430302_, false, p_428830_);
         if (list.size() < 4) {
            renderTextOverPoi("Owners: " + list, p_430302_, i, -256);
         } else {
            renderTextOverPoi(list.size() + " ticket holders", p_430302_, i, -256);
         }

         i++;
         List<String> list1 = this.getTicketHolderNames(p_430302_, true, p_428830_);
         if (list1.size() < 4) {
            renderTextOverPoi("Candidates: " + list1, p_430302_, i, -23296);
         } else {
            renderTextOverPoi(list1.size() + " potential owners", p_430302_, i, -23296);
         }

         i++;
      }

      renderTextOverPoi("Free tickets: " + p_430302_.freeTicketCount(), p_430302_, i, -256);
      renderTextOverPoi(p_430302_.poiType().getRegisteredName(), p_430302_, ++i, -1);
   }

   private static void renderTextOverPoi(String p_427463_, DebugPoiInfo p_430300_, int p_426136_, int p_424914_) {
      Gizmos.billboardTextOverBlock(p_427463_, p_430300_.pos(), p_426136_, p_424914_, 0.32F);
   }

   private List<String> getTicketHolderNames(DebugPoiInfo p_429201_, boolean p_426035_, DebugValueAccess p_429116_) {
      List<String> list = new ArrayList<>();
      p_429116_.forEachEntity(DebugSubscriptions.BRAINS, (p_425410_, p_425347_) -> {
         boolean flag = p_426035_ ? p_425347_.hasPotentialPoi(p_429201_.pos()) : p_425347_.hasPoi(p_429201_.pos());
         if (flag) {
            list.add(DebugEntityNameGenerator.getEntityName(p_425410_.getUUID()));
         }
      });
      return list;
   }
}
