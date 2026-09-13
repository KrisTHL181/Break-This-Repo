package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

public class ItemEntityRenderer extends EntityRenderer<ItemEntity, ItemEntityRenderState> {
   private static final float ITEM_MIN_HOVER_HEIGHT = 0.0625F;
   private static final float ITEM_BUNDLE_OFFSET_SCALE = 0.15F;
   private static final float FLAT_ITEM_DEPTH_THRESHOLD = 0.0625F;
   private final ItemModelResolver itemModelResolver;
   private final RandomSource random = RandomSource.create();

   public ItemEntityRenderer(final EntityRendererProvider.Context context) {
      super(context);
      this.itemModelResolver = context.getItemModelResolver();
      this.shadowRadius = 0.15F;
      this.shadowStrength = 0.75F;
   }

   public ItemEntityRenderState createRenderState() {
      return new ItemEntityRenderState();
   }

   public void extractRenderState(final ItemEntity entity, final ItemEntityRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      state.bobOffset = entity.bobOffs;
      state.extractItemGroupRenderState(entity, entity.getItem(), this.itemModelResolver);
   }

   public void submit(
      final ItemEntityRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      if (!state.item.isEmpty()) {
         poseStack.pushPose();
         AABB boundingBox = state.item.getModelBoundingBox();
         float minOffsetY = -((float)boundingBox.minY) + 0.0625F;
         float bob = Mth.sin(state.ageInTicks / 10.0F + state.bobOffset) * 0.1F + 0.1F;
         poseStack.translate(0.0F, bob + minOffsetY, 0.0F);
         float spin = ItemEntity.getSpin(state.ageInTicks, state.bobOffset);
         poseStack.mulPose(Axis.YP.rotation(spin));
         submitMultipleFromCount(poseStack, submitNodeCollector, state.lightCoords, state, this.random, boundingBox);
         poseStack.popPose();
         super.submit(state, poseStack, submitNodeCollector, camera);
      }
   }

   public static void submitMultipleFromCount(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final ItemClusterRenderState state,
      final RandomSource random
   ) {
      submitMultipleFromCount(poseStack, submitNodeCollector, lightCoords, state, random, state.item.getModelBoundingBox());
   }

   public static void submitMultipleFromCount(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final ItemClusterRenderState state,
      final RandomSource random,
      final AABB modelBoundingBox
   ) {
      int amount = state.count;
      if (amount != 0) {
         random.setSeed(state.seed);
         ItemStackRenderState item = state.item;
         float modelDepth = (float)modelBoundingBox.getZsize();
         if (modelDepth > 0.0625F) {
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

            for (int i = 1; i < amount; i++) {
               poseStack.pushPose();
               float xo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float yo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float zo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               poseStack.translate(xo, yo, zo);
               item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
               poseStack.popPose();
            }
         } else {
            float offsetZ = modelDepth * 1.5F;
            poseStack.translate(0.0F, 0.0F, -(offsetZ * (amount - 1) / 2.0F));
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.translate(0.0F, 0.0F, offsetZ);

            for (int i = 1; i < amount; i++) {
               poseStack.pushPose();
               float xo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float yo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               poseStack.translate(xo, yo, 0.0F);
               item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
               poseStack.popPose();
               poseStack.translate(0.0F, 0.0F, offsetZ);
            }
         }
      }
   }

   public static void renderMultipleFromCount(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final ItemClusterRenderState state,
      final RandomSource random
   ) {
      AABB modelBoundingBox = state.item.getModelBoundingBox();
      int amount = state.count;
      if (amount != 0) {
         random.setSeed(state.seed);
         ItemStackRenderState item = state.item;
         float modelDepth = (float)modelBoundingBox.getZsize();
         if (modelDepth > 0.0625F) {
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);

            for (int i = 1; i < amount; i++) {
               poseStack.pushPose();
               float xo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float yo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               float zo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F;
               poseStack.translate(xo, yo, zo);
               item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
               poseStack.popPose();
            }
         } else {
            float offsetZ = modelDepth * 1.5F;
            poseStack.translate(0.0F, 0.0F, -(offsetZ * (amount - 1) / 2.0F));
            item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.translate(0.0F, 0.0F, offsetZ);

            for (int i = 1; i < amount; i++) {
               poseStack.pushPose();
               float xo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               float yo = (random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
               poseStack.translate(xo, yo, 0.0F);
               item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
               poseStack.popPose();
               poseStack.translate(0.0F, 0.0F, offsetZ);
            }
         }
      }
   }
}
