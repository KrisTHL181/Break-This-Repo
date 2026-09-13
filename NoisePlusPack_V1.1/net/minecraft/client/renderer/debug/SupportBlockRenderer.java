package net.minecraft.client.renderer.debug;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<Entity> surroundEntities = Collections.emptyList();

   public SupportBlockRenderer(Minecraft p_286424_) {
      this.minecraft = p_286424_;
   }

   @Override
   public void emitGizmos(double p_459832_, double p_454026_, double p_453442_, DebugValueAccess p_453485_, Frustum p_452158_, float p_451061_) {
      double d0 = Util.getNanos();
      if (d0 - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = d0;
         Entity entity = this.minecraft.gameRenderer.getMainCamera().entity();
         this.surroundEntities = ImmutableList.copyOf(entity.level().getEntities(entity, entity.getBoundingBox().inflate(16.0)));
      }

      Player player = this.minecraft.player;
      if (player != null && player.mainSupportingBlockPos.isPresent()) {
         this.drawHighlights(player, () -> 0.0, -65536);
      }

      for (Entity entity1 : this.surroundEntities) {
         if (entity1 != player) {
            this.drawHighlights(entity1, () -> this.getBias(entity1), -16711936);
         }
      }
   }

   private void drawHighlights(Entity p_286273_, DoubleSupplier p_286458_, int p_451523_) {
      p_286273_.mainSupportingBlockPos.ifPresent(p_448309_ -> {
         double d0 = p_286458_.getAsDouble();
         BlockPos blockpos = p_286273_.getOnPos();
         this.highlightPosition(blockpos, 0.02 + d0, p_451523_);
         BlockPos blockpos1 = p_286273_.getOnPosLegacy();
         if (!blockpos1.equals(blockpos)) {
            this.highlightPosition(blockpos1, 0.04 + d0, -16711681);
         }
      });
   }

   private double getBias(Entity p_286713_) {
      return 0.02 * (String.valueOf(p_286713_.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
   }

   private void highlightPosition(BlockPos p_286268_, double p_286463_, int p_459902_) {
      double d0 = p_286268_.getX() - 2.0 * p_286463_;
      double d1 = p_286268_.getY() - 2.0 * p_286463_;
      double d2 = p_286268_.getZ() - 2.0 * p_286463_;
      double d3 = d0 + 1.0 + 4.0 * p_286463_;
      double d4 = d1 + 1.0 + 4.0 * p_286463_;
      double d5 = d2 + 1.0 + 4.0 * p_286463_;
      Gizmos.cuboid(new AABB(d0, d1, d2, d3, d4, d5), GizmoStyle.stroke(ARGB.color(0.4F, p_459902_)));
      VoxelShape voxelshape = this.minecraft
         .level
         .getBlockState(p_286268_)
         .getCollisionShape(this.minecraft.level, p_286268_, CollisionContext.empty())
         .move(p_286268_);
      GizmoStyle gizmostyle = GizmoStyle.stroke(p_459902_);

      for (AABB aabb : voxelshape.toAabbs()) {
         Gizmos.cuboid(aabb, gizmostyle);
      }
   }
}
