package net.minecraft.client.renderer.debug;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugSubscriptions;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class VillageSectionsDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
   @Override
   public void emitGizmos(double p_456135_, double p_459850_, double p_457735_, DebugValueAccess p_455056_, Frustum p_456508_, float p_452263_) {
      p_455056_.forEachBlock(DebugSubscriptions.VILLAGE_SECTIONS, (p_448310_, p_448311_) -> {
         SectionPos sectionpos = SectionPos.of(p_448310_);
         Gizmos.cuboid(sectionpos.center(), GizmoStyle.fill(ARGB.colorFromFloat(0.15F, 0.2F, 1.0F, 0.2F)));
      });
   }
}
