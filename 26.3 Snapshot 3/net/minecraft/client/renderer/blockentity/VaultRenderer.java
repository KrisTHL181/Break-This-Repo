package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.VaultRenderState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class VaultRenderer implements BlockEntityRenderer<VaultBlockEntity, VaultRenderState> {
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   public VaultRenderer(final BlockEntityRendererProvider.Context context) {
      this.itemModelResolver = context.itemModelResolver();
   }

   public VaultRenderState createRenderState() {
      return new VaultRenderState();
   }

   public void extractRenderState(
      final VaultBlockEntity blockEntity,
      final VaultRenderState state,
      final float partialTicks,
      final Vec3 cameraPosition,
      final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
   ) {
      BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
      ItemStack displayItem = blockEntity.getSharedData().getDisplayItem();
      if (VaultBlockEntity.Client.shouldDisplayActiveEffects(blockEntity.getSharedData()) && !displayItem.isEmpty() && blockEntity.getLevel() != null) {
         state.displayItem = new ItemClusterRenderState();
         this.itemModelResolver.updateForTopItem(state.displayItem.item, displayItem, ItemDisplayContext.GROUND, blockEntity.getLevel(), null, 0);
         state.displayItem.count = ItemClusterRenderState.getRenderedAmount(displayItem.getCount());
         state.displayItem.seed = ItemClusterRenderState.getSeedForItemStack(displayItem);
         VaultClientData clientData = blockEntity.getClientData();
         state.spin = Mth.rotLerp(partialTicks, clientData.previousSpin(), clientData.currentSpin());
      }
   }

   public void submit(final VaultRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
      if (state.displayItem != null) {
         poseStack.pushPose();
         poseStack.translate(0.5F, 0.4F, 0.5F);
         poseStack.mulPose(Axis.YP.rotationDegrees(state.spin));
         ItemEntityRenderer.renderMultipleFromCount(poseStack, submitNodeCollector, state.lightCoords, state.displayItem, this.random);
         poseStack.popPose();
      }
   }
}
