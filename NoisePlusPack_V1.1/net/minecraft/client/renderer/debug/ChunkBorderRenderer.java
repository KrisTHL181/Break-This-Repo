package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChunkBorderRenderer implements DebugRenderer.SimpleDebugRenderer {
   private static final float THICK_WIDTH = 4.0F;
   private static final float THIN_WIDTH = 1.0F;
   private final Minecraft minecraft;
   private static final int CELL_BORDER = ARGB.color(255, 0, 155, 155);
   private static final int YELLOW = ARGB.color(255, 255, 255, 0);
   private static final int MAJOR_LINES = ARGB.colorFromFloat(1.0F, 0.25F, 0.25F, 1.0F);

   public ChunkBorderRenderer(Minecraft p_113356_) {
      this.minecraft = p_113356_;
   }

   @Override
   public void emitGizmos(double p_455685_, double p_454589_, double p_458975_, DebugValueAccess p_457408_, Frustum p_450814_, float p_460997_) {
      Entity entity = this.minecraft.gameRenderer.getMainCamera().entity();
      float f = this.minecraft.level.getMinY();
      float f1 = this.minecraft.level.getMaxY() + 1;
      SectionPos sectionpos = SectionPos.of(entity.blockPosition());
      double d0 = sectionpos.minBlockX();
      double d1 = sectionpos.minBlockZ();

      for (int i = -16; i <= 32; i += 16) {
         for (int j = -16; j <= 32; j += 16) {
            Gizmos.line(new Vec3(d0 + i, f, d1 + j), new Vec3(d0 + i, f1, d1 + j), ARGB.colorFromFloat(0.5F, 1.0F, 0.0F, 0.0F), 4.0F);
         }
      }

      for (int l = 2; l < 16; l += 2) {
         int i2 = l % 4 == 0 ? CELL_BORDER : YELLOW;
         Gizmos.line(new Vec3(d0 + l, f, d1), new Vec3(d0 + l, f1, d1), i2, 1.0F);
         Gizmos.line(new Vec3(d0 + l, f, d1 + 16.0), new Vec3(d0 + l, f1, d1 + 16.0), i2, 1.0F);
      }

      for (int i1 = 2; i1 < 16; i1 += 2) {
         int j2 = i1 % 4 == 0 ? CELL_BORDER : YELLOW;
         Gizmos.line(new Vec3(d0, f, d1 + i1), new Vec3(d0, f1, d1 + i1), j2, 1.0F);
         Gizmos.line(new Vec3(d0 + 16.0, f, d1 + i1), new Vec3(d0 + 16.0, f1, d1 + i1), j2, 1.0F);
      }

      for (int j1 = this.minecraft.level.getMinY(); j1 <= this.minecraft.level.getMaxY() + 1; j1 += 2) {
         float f2 = j1;
         int k = j1 % 8 == 0 ? CELL_BORDER : YELLOW;
         Gizmos.line(new Vec3(d0, f2, d1), new Vec3(d0, f2, d1 + 16.0), k, 1.0F);
         Gizmos.line(new Vec3(d0, f2, d1 + 16.0), new Vec3(d0 + 16.0, f2, d1 + 16.0), k, 1.0F);
         Gizmos.line(new Vec3(d0 + 16.0, f2, d1 + 16.0), new Vec3(d0 + 16.0, f2, d1), k, 1.0F);
         Gizmos.line(new Vec3(d0 + 16.0, f2, d1), new Vec3(d0, f2, d1), k, 1.0F);
      }

      for (int k1 = 0; k1 <= 16; k1 += 16) {
         for (int k2 = 0; k2 <= 16; k2 += 16) {
            Gizmos.line(new Vec3(d0 + k1, f, d1 + k2), new Vec3(d0 + k1, f1, d1 + k2), MAJOR_LINES, 4.0F);
         }
      }

      Gizmos.cuboid(
            new AABB(
               sectionpos.minBlockX(),
               sectionpos.minBlockY(),
               sectionpos.minBlockZ(),
               sectionpos.maxBlockX() + 1,
               sectionpos.maxBlockY() + 1,
               sectionpos.maxBlockZ() + 1
            ),
            GizmoStyle.stroke(MAJOR_LINES, 1.0F)
         )
         .setAlwaysOnTop();

      for (int l1 = this.minecraft.level.getMinY(); l1 <= this.minecraft.level.getMaxY() + 1; l1 += 16) {
         Gizmos.line(new Vec3(d0, l1, d1), new Vec3(d0, l1, d1 + 16.0), MAJOR_LINES, 4.0F);
         Gizmos.line(new Vec3(d0, l1, d1 + 16.0), new Vec3(d0 + 16.0, l1, d1 + 16.0), MAJOR_LINES, 4.0F);
         Gizmos.line(new Vec3(d0 + 16.0, l1, d1 + 16.0), new Vec3(d0 + 16.0, l1, d1), MAJOR_LINES, 4.0F);
         Gizmos.line(new Vec3(d0 + 16.0, l1, d1), new Vec3(d0, l1, d1), MAJOR_LINES, 4.0F);
      }
   }
}
