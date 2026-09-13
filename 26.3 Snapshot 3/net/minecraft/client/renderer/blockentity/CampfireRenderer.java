package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CampfireRenderer implements BlockEntityRenderer<CampfireBlockEntity, CampfireRenderState> {
   private static final float SIZE = 0.375F;
   private final ItemModelResolver itemModelResolver;

   public CampfireRenderer(final BlockEntityRendererProvider.Context context) {
      this.itemModelResolver = context.itemModelResolver();
   }

   public CampfireRenderState createRenderState() {
      return new CampfireRenderState();
   }

   public void extractRenderState(
      final CampfireBlockEntity blockEntity,
      final CampfireRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      state.facing = blockEntity.getBlockState().getValue(CampfireBlock.FACING);
      int seed = (int)blockEntity.getBlockPos().asLong();
      state.items = new ArrayList<>();

      for (int slot = 0; slot < blockEntity.getItems().size(); slot++) {
         ItemStackRenderState itemState = new ItemStackRenderState();
         this.itemModelResolver
            .updateForTopItem(itemState, blockEntity.getItems().get(slot), ItemDisplayContext.FIXED, blockEntity.getLevel(), null, seed + slot);
         state.items.add(itemState);
      }
   }

   public void submit(final CampfireRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      Direction facing = state.facing;
      List<ItemStackRenderState> items = state.items;

      for (int slot = 0; slot < items.size(); slot++) {
         ItemStackRenderState itemState = items.get(slot);
         if (!itemState.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.44921875F, 0.5F);
            Direction direction = Direction.from2DDataValue((slot + facing.get2DDataValue()) % 4);
            float angle = -direction.toYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(-0.3125F, -0.3125F, 0.0F);
            poseStack.scale(0.375F, 0.375F, 0.375F);
            itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
         }
      }
   }
}
