package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockEntityWithBoundingBoxRenderer<T extends BlockEntity & BoundingBoxRenderable>
   implements BlockEntityRenderer<T, BlockEntityWithBoundingBoxRenderState> {
   public static final int STRUCTURE_VOIDS_COLOR = ARGB.colorFromFloat(0.2F, 0.75F, 0.75F, 1.0F);

   public BlockEntityWithBoundingBoxRenderState createRenderState() {
      return new BlockEntityWithBoundingBoxRenderState();
   }

   public void extractRenderState(
      final T blockEntity,
      final BlockEntityWithBoundingBoxRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      extract(blockEntity, state);
   }

   public static <T extends BlockEntity & BoundingBoxRenderable> void extract(final T blockEntity, final BlockEntityWithBoundingBoxRenderState state) {
      LocalPlayer player = Minecraft.getInstance().player;
      state.isVisible = player.canUseGameMasterBlocks() || player.isSpectator();
      state.box = blockEntity.getRenderableBox();
      state.mode = blockEntity.renderMode();
      BlockPos pos = state.box.localPos();
      Vec3i size = state.box.size();
      BlockPos entityPos = state.blockPos;
      BlockPos startingPos = entityPos.offset(pos);
      if (state.isVisible && blockEntity.getLevel() != null && state.mode == BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS) {
         state.invisibleBlocks = new BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[size.getX() * size.getY() * size.getZ()];

         for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
               for (int z = 0; z < size.getZ(); z++) {
                  int index = z * size.getX() * size.getY() + y * size.getX() + x;
                  BlockState blockState = blockEntity.getLevel().getBlockState(startingPos.offset(x, y, z));
                  if (blockState.isAir()) {
                     state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR;
                  } else if (blockState.is(Blocks.STRUCTURE_VOID)) {
                     state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID;
                  } else if (blockState.is(Blocks.BARRIER)) {
                     state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER;
                  } else if (blockState.is(Blocks.LIGHT)) {
                     state.invisibleBlocks[index] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT;
                  }
               }
            }
         }
      } else {
         state.invisibleBlocks = null;
      }

      if (state.isVisible) {
      }

      state.structureVoids = null;
   }

   public void submit(
      final BlockEntityWithBoundingBoxRenderState state,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final CameraRenderState camera
   ) {
      if (state.isVisible) {
         BoundingBoxRenderable.Mode mode = state.mode;
         if (mode != BoundingBoxRenderable.Mode.NONE) {
            BoundingBoxRenderable.RenderableBox box = state.box;
            BlockPos pos = box.localPos();
            Vec3i size = box.size();
            if (size.getX() >= 1 && size.getY() >= 1 && size.getZ() >= 1) {
               float lineAlpha = 1.0F;
               float lineRGB = 0.9F;
               BlockPos far = pos.offset(size);
               Gizmos.cuboid(
                  new AABB(pos.getX(), pos.getY(), pos.getZ(), far.getX(), far.getY(), far.getZ()).move(state.blockPos),
                  GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.9F, 0.9F, 0.9F)),
                  true
               );
               this.renderInvisibleBlocks(state, pos, size);
            }
         }
      }
   }

   private void renderInvisibleBlocks(final BlockEntityWithBoundingBoxRenderState state, final BlockPos localPos, final Vec3i size) {
      if (state.invisibleBlocks != null) {
         BlockPos entityPos = state.blockPos;
         BlockPos startingPos = entityPos.offset(localPos);

         for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
               for (int z = 0; z < size.getZ(); z++) {
                  int index = z * size.getX() * size.getY() + y * size.getX() + x;
                  BlockEntityWithBoundingBoxRenderState.InvisibleBlockType invisibleBlockType = state.invisibleBlocks[index];
                  if (invisibleBlockType != null) {
                     float scale = invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR ? 0.05F : 0.0F;
                     double renderX0 = startingPos.getX() + x + 0.45F - scale;
                     double renderY0 = startingPos.getY() + y + 0.45F - scale;
                     double renderZ0 = startingPos.getZ() + z + 0.45F - scale;
                     double renderX1 = startingPos.getX() + x + 0.55F + scale;
                     double renderY1 = startingPos.getY() + y + 0.55F + scale;
                     double renderZ1 = startingPos.getZ() + z + 0.55F + scale;
                     AABB aabb = new AABB(renderX0, renderY0, renderZ0, renderX1, renderY1, renderZ1);
                     if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 1.0F)));
                     } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 0.75F, 0.75F)));
                     } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(-65536));
                     } else if (invisibleBlockType == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT) {
                        Gizmos.cuboid(aabb, GizmoStyle.stroke(-256));
                     }
                  }
               }
            }
         }
      }
   }

   private void renderStructureVoids(final BlockEntityWithBoundingBoxRenderState state, final BlockPos startingPosition, final Vec3i size) {
      if (state.structureVoids != null) {
         DiscreteVoxelShape shape = new BitSetDiscreteVoxelShape(size.getX(), size.getY(), size.getZ());

         for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
               for (int z = 0; z < size.getZ(); z++) {
                  int index = z * size.getX() * size.getY() + y * size.getX() + x;
                  if (state.structureVoids[index]) {
                     shape.fill(x, y, z);
                  }
               }
            }
         }

         shape.forAllFaces((direction, xx, yx, zx) -> {
            float scale = 0.48F;
            float x0 = xx + startingPosition.getX() + 0.5F - 0.48F;
            float y0 = yx + startingPosition.getY() + 0.5F - 0.48F;
            float z0 = zx + startingPosition.getZ() + 0.5F - 0.48F;
            float x1 = xx + startingPosition.getX() + 0.5F + 0.48F;
            float y1 = yx + startingPosition.getY() + 0.5F + 0.48F;
            float z1 = zx + startingPosition.getZ() + 0.5F + 0.48F;
            Gizmos.rect(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), direction, GizmoStyle.fill(STRUCTURE_VOIDS_COLOR));
         });
      }
   }

   @Override
   public boolean shouldRenderOffScreen() {
      return true;
   }

   @Override
   public int getViewDistance() {
      return 96;
   }
}
