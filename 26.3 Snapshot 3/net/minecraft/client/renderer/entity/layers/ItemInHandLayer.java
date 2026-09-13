package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;

public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel<S>> extends RenderLayer<S, M> {
   public ItemInHandLayer(final RenderLayerParent<S, M> renderer) {
      super(renderer);
   }

   public void submit(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final S state, final float yRot, final float xRot
   ) {
      this.submitArmWithItem(state, state.rightHandItemState, state.rightHandItemStack, HumanoidArm.RIGHT, poseStack, submitNodeCollector, lightCoords);
      this.submitArmWithItem(state, state.leftHandItemState, state.leftHandItemStack, HumanoidArm.LEFT, poseStack, submitNodeCollector, lightCoords);
   }

   protected void submitArmWithItem(
      final S state,
      final ItemStackRenderState item,
      final ItemStack itemStack,
      final HumanoidArm arm,
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords
   ) {
      if (!item.isEmpty()) {
         poseStack.pushPose();
         this.getParentModel().translateToHand(state, arm, poseStack);
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         boolean isLeftHand = arm == HumanoidArm.LEFT;
         float offsetX = this.useBabyOffset(state) ? 0.0F : 1.0F;
         float offsetY = this.useBabyOffset(state) ? 1.0F : 2.0F;
         float offsetZ = this.useBabyOffset(state) ? -4.5F : -10.0F;
         poseStack.translate((isLeftHand ? -1 : 1) * offsetX / 16.0F, offsetY / 16.0F, offsetZ / 16.0F);
         if (state.attackTime > 0.0F && state.attackArm == arm && state.swingAnimationType == SwingAnimationType.STAB) {
            SpearAnimations.thirdPersonAttackItem(state, poseStack);
         }

         float ticksUsingItem = state.ticksUsingItem(arm);
         if (ticksUsingItem != 0.0F) {
            (arm == HumanoidArm.RIGHT ? state.rightArmPose : state.leftArmPose).animateUseItem(state, poseStack, ticksUsingItem, arm, itemStack);
         }

         item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
         poseStack.popPose();
      }
   }

   private boolean useBabyOffset(final S state) {
      return state.isBaby && state.entityType != EntityTypes.ARMOR_STAND;
   }
}
