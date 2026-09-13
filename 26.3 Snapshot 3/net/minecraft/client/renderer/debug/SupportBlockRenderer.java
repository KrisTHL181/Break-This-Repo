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

public class SupportBlockRenderer implements DebugRenderer.SimpleDebugRenderer {
   private final Minecraft minecraft;
   private double lastUpdateTime = Double.MIN_VALUE;
   private List<Entity> surroundEntities = Collections.emptyList();

   public SupportBlockRenderer(final Minecraft minecraft) {
      this.minecraft = minecraft;
   }

   @Override
   public void emitGizmos(
      final double camX, final double camY, final double camZ, final DebugValueAccess debugValues, final Frustum frustum, final float partialTicks
   ) {
      double time = Util.getNanos();
      if (time - this.lastUpdateTime > 1.0E8) {
         this.lastUpdateTime = time;
         Entity cameraEntity = this.minecraft.getCameraEntity();
         this.surroundEntities = ImmutableList.copyOf(cameraEntity.level().getEntities(cameraEntity, cameraEntity.getBoundingBox().inflate(16.0)));
      }

      Player player = this.minecraft.player;
      if (player != null && player.mainSupportingBlockPos.isPresent()) {
         this.drawHighlights(player, () -> 0.0, -65536);
      }

      for (Entity entity : this.surroundEntities) {
         if (entity != player) {
            this.drawHighlights(entity, () -> this.getBias(entity), -16711936);
         }
      }
   }

   private void drawHighlights(final Entity entity, final DoubleSupplier biasGetter, final int color) {
      entity.mainSupportingBlockPos.ifPresent(bp -> {
         double bias = biasGetter.getAsDouble();
         BlockPos supportingBlock = entity.getOnPos();
         this.highlightPosition(supportingBlock, 0.02 + bias, color);
         BlockPos effect = entity.getOnPosLegacy();
         if (!effect.equals(supportingBlock)) {
            this.highlightPosition(effect, 0.04 + bias, -16711681);
         }
      });
   }

   private double getBias(final Entity entity) {
      return 0.02 * (String.valueOf(entity.getId() + 0.132453657).hashCode() % 1000) / 1000.0;
   }

   private void highlightPosition(final BlockPos pos, final double offset, final int color) {
      double fromX = pos.getX() - 2.0 * offset;
      double fromY = pos.getY() - 2.0 * offset;
      double fromZ = pos.getZ() - 2.0 * offset;
      double toX = fromX + 1.0 + 4.0 * offset;
      double toY = fromY + 1.0 + 4.0 * offset;
      double toZ = fromZ + 1.0 + 4.0 * offset;
      Gizmos.cuboid(new AABB(fromX, fromY, fromZ, toX, toY, toZ), GizmoStyle.stroke(ARGB.color(0.4F, color)));
      VoxelShape shape = this.minecraft.level.getBlockState(pos).getCollisionShape(this.minecraft.level, pos, CollisionContext.empty()).move(pos);
      GizmoStyle style = GizmoStyle.stroke(color);

      for (AABB aabb : shape.toAabbs()) {
         Gizmos.cuboid(aabb, style);
      }
   }
}
