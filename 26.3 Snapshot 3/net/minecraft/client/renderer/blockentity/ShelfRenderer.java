package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShelfRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity, ShelfRenderState> {
   private static final float ITEM_SIZE = 0.25F;
   private static final float ALIGN_ITEMS_TO_BOTTOM = -0.25F;
   private final ItemModelResolver itemModelResolver;

   public ShelfRenderer(final BlockEntityRendererProvider.Context context) {
      this.itemModelResolver = context.itemModelResolver();
   }

   public ShelfRenderState createRenderState() {
      return new ShelfRenderState();
   }

   public void extractRenderState(
      final ShelfBlockEntity blockEntity,
      final ShelfRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      state.alignToBottom = blockEntity.getAlignItemsToBottom();
      state.facing = blockEntity.getBlockState().getValue(ShelfBlock.FACING);
      NonNullList<ItemStack> items = blockEntity.getItems();
      int seed = HashCommon.long2int(blockEntity.getBlockPos().asLong());

      for (int slot = 0; slot < items.size(); slot++) {
         ItemStack itemStack = items.get(slot);
         if (!itemStack.isEmpty()) {
            ItemStackRenderState itemStackRenderState = new ItemStackRenderState();
            this.itemModelResolver
               .updateForTopItem(itemStackRenderState, itemStack, ItemDisplayContext.ON_SHELF, blockEntity.level(), blockEntity, seed + slot);
            state.items[slot] = itemStackRenderState;
         }
      }
   }

   public void submit(final ShelfRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      float yRot = state.facing.getAxis().isHorizontal() ? -state.facing.toYRot() : 180.0F;

      for (int slot = 0; slot < state.items.length; slot++) {
         ItemStackRenderState itemStackRenderState = state.items[slot];
         if (itemStackRenderState != null) {
            this.submitItem(state, itemStackRenderState, poseStack, submitNodeCollector, slot, yRot);
         }
      }
   }

   private void submitItem(
      final ShelfRenderState state,
      final ItemStackRenderState itemStackRenderState,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int slot,
      final float yRot
   ) {
      float itemSlotPosition = (slot - 1) * 0.3125F;
      Vec3 itemOffset = new Vec3(itemSlotPosition, state.alignToBottom ? -0.25 : 0.0, -0.25);
      poseStack.pushPose();
      poseStack.translate(0.5F, 0.5F, 0.5F);
      poseStack.mulPose(Axis.YP.rotationDegrees(yRot));
      poseStack.translate(itemOffset);
      poseStack.scale(0.25F, 0.25F, 0.25F);
      AABB box = itemStackRenderState.getModelBoundingBox();
      double offsetY = -box.minY;
      if (!state.alignToBottom) {
         offsetY += -(box.maxY - box.minY) / 2.0;
      }

      poseStack.translate(0.0, offsetY, 0.0);
      itemStackRenderState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
      poseStack.popPose();
   }
}
