package net.minecraft.client.renderer.entity.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ArrowLayer;
import net.minecraft.client.renderer.entity.layers.BeeStingerLayer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.Deadmau5EarsLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ParrotOnShoulderLayer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.SpinAttackEffectLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.Vec3;

public class AvatarRenderer<AvatarlikeEntity extends Avatar & ClientAvatarEntity> extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
   public AvatarRenderer(final EntityRendererProvider.Context context, final boolean slimSteve) {
      super(context, new PlayerModel(context.bakeLayer(slimSteve ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slimSteve), 0.5F);
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(
               slimSteve ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR, context.getModelSet(), part -> new PlayerModel(part, slimSteve)
            ),
            context.getEquipmentRenderer()
         )
      );
      this.addLayer(new PlayerItemInHandLayer<>(this));
      this.addLayer(new ArrowLayer<>(this, context));
      this.addLayer(new Deadmau5EarsLayer(this, context.getModelSet()));
      this.addLayer(new CapeLayer(this, context.getModelSet(), context.getEquipmentAssets()));
      this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getPlayerSkinRenderCache()));
      this.addLayer(new WingsLayer<>(this, context.getModelSet(), context.getEquipmentRenderer()));
      this.addLayer(new ParrotOnShoulderLayer(this, context.getModelSet()));
      this.addLayer(new SpinAttackEffectLayer(this, context.getModelSet()));
      this.addLayer(new BeeStingerLayer<>(this, context));
   }

   protected boolean shouldRenderLayers(final AvatarRenderState state) {
      return !state.isSpectator;
   }

   public Vec3 getRenderOffset(final AvatarRenderState state) {
      Vec3 offset = super.getRenderOffset(state);
      return state.isCrouching ? offset.add(0.0, state.scale * -2.0F / 16.0, 0.0) : offset;
   }

   private static HumanoidModel.ArmPose getArmPose(final Avatar avatar, final HumanoidArm arm) {
      ItemStack mainHandItem = avatar.getItemInHand(InteractionHand.MAIN_HAND);
      ItemStack offHandItem = avatar.getItemInHand(InteractionHand.OFF_HAND);
      HumanoidModel.ArmPose mainHandPose = getArmPose(avatar, mainHandItem, InteractionHand.MAIN_HAND);
      HumanoidModel.ArmPose offHandPose = getArmPose(avatar, offHandItem, InteractionHand.OFF_HAND);
      if (mainHandPose.isTwoHanded()) {
         offHandPose = offHandItem.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
      }

      return avatar.getMainArm() == arm ? mainHandPose : offHandPose;
   }

   private static HumanoidModel.ArmPose getArmPose(final Avatar avatar, final ItemStack itemInHand, final InteractionHand hand) {
      if (itemInHand.isEmpty()) {
         return HumanoidModel.ArmPose.EMPTY;
      }

      if (!avatar.swinging && itemInHand.is(Items.CROSSBOW) && CrossbowItem.isCharged(itemInHand)) {
         return HumanoidModel.ArmPose.CROSSBOW_HOLD;
      }

      if (avatar.getUsedItemHand() == hand && avatar.getUseItemRemainingTicks() > 0) {
         ItemUseAnimation anim = itemInHand.getUseAnimation();
         if (anim == ItemUseAnimation.BLOCK) {
            return HumanoidModel.ArmPose.BLOCK;
         }

         if (anim == ItemUseAnimation.BOW) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
         }

         if (anim == ItemUseAnimation.TRIDENT) {
            return HumanoidModel.ArmPose.THROW_TRIDENT;
         }

         if (anim == ItemUseAnimation.CROSSBOW) {
            return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
         }

         if (anim == ItemUseAnimation.SPYGLASS) {
            return HumanoidModel.ArmPose.SPYGLASS;
         }

         if (anim == ItemUseAnimation.TOOT_HORN) {
            return HumanoidModel.ArmPose.TOOT_HORN;
         }

         if (anim == ItemUseAnimation.BRUSH) {
            return HumanoidModel.ArmPose.BRUSH;
         }

         if (anim == ItemUseAnimation.SPEAR) {
            return HumanoidModel.ArmPose.SPEAR;
         }
      }

      SwingAnimation attack = itemInHand.get(DataComponents.SWING_ANIMATION);
      if (attack != null && attack.type() == SwingAnimationType.STAB && avatar.swinging) {
         return HumanoidModel.ArmPose.SPEAR;
      } else {
         return itemInHand.is(ItemTags.SPEARS) ? HumanoidModel.ArmPose.SPEAR : HumanoidModel.ArmPose.ITEM;
      }
   }

   public Identifier getTextureLocation(final AvatarRenderState state) {
      return state.skin.body().texturePath();
   }

   protected void scale(final AvatarRenderState state, final PoseStack poseStack) {
      float s = 0.9375F;
      poseStack.scale(0.9375F, 0.9375F, 0.9375F);
   }

   protected void submitNameDisplay(
      final AvatarRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
   ) {
      poseStack.pushPose();
      int offset = state.showExtraEars ? -10 : 0;
      this.submitNameDisplay(state, poseStack, submitNodeCollector, camera, offset);
      poseStack.popPose();
   }

   public AvatarRenderState createRenderState() {
      return new AvatarRenderState();
   }

   public void extractRenderState(final AvatarlikeEntity entity, final AvatarRenderState state, final float partialTicks) {
      super.extractRenderState(entity, state, partialTicks);
      HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
      state.leftArmPose = getArmPose(entity, HumanoidArm.LEFT);
      state.rightArmPose = getArmPose(entity, HumanoidArm.RIGHT);
      state.skin = entity.getSkin();
      state.arrowCount = entity.getArrowCount();
      state.stingerCount = entity.getStingerCount();
      state.isSpectator = entity.isSpectator();
      state.showHat = entity.isModelPartShown(PlayerModelPart.HAT);
      state.showJacket = entity.isModelPartShown(PlayerModelPart.JACKET);
      state.showLeftPants = entity.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
      state.showRightPants = entity.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
      state.showLeftSleeve = entity.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
      state.showRightSleeve = entity.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
      state.showCape = entity.isModelPartShown(PlayerModelPart.CAPE);
      this.extractFlightData(entity, state, partialTicks);
      this.extractCapeState(entity, state, partialTicks);
      state.parrotOnLeftShoulder = entity.getParrotVariantOnShoulder(true);
      state.parrotOnRightShoulder = entity.getParrotVariantOnShoulder(false);
      state.id = entity.getId();
      state.showExtraEars = entity.showExtraEars();
      state.heldOnHead.clear();
      if (state.isUsingItem) {
         ItemStack useItem = entity.getItemInHand(state.useItemHand);
         if (useItem.is(Items.SPYGLASS)) {
            this.itemModelResolver.updateForLiving(state.heldOnHead, useItem, ItemDisplayContext.HEAD, entity);
         }
      }
   }

   protected boolean shouldShowName(final AvatarlikeEntity entity, final double distanceToCameraSq) {
      return super.shouldShowName(entity, distanceToCameraSq)
         && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
   }

   private void extractFlightData(final AvatarlikeEntity entity, final AvatarRenderState state, final float partialTicks) {
      state.fallFlyingTimeInTicks = entity.getFallFlyingTicks() + partialTicks;
      Vec3 lookAngle = entity.getViewVector(partialTicks);
      Vec3 movement = entity.avatarState().deltaMovementOnPreviousTick().lerp(entity.getDeltaMovement(), partialTicks);
      if (movement.horizontalDistanceSqr() > 1.0E-5F && lookAngle.horizontalDistanceSqr() > 1.0E-5F) {
         state.shouldApplyFlyingYRot = true;
         double dot = movement.horizontal().normalize().dot(lookAngle.horizontal().normalize());
         double sign = movement.x * lookAngle.z - movement.z * lookAngle.x;
         state.flyingYRot = (float)(Math.signum(sign) * Math.acos(Math.min(1.0, Math.abs(dot))));
      } else {
         state.shouldApplyFlyingYRot = false;
         state.flyingYRot = 0.0F;
      }
   }

   private void extractCapeState(final AvatarlikeEntity entity, final AvatarRenderState state, final float partialTicks) {
      ClientAvatarState clientState = entity.avatarState();
      double deltaX = clientState.getInterpolatedCloakX(partialTicks) - Mth.lerp(partialTicks, entity.xo, entity.getX());
      double deltaY = clientState.getInterpolatedCloakY(partialTicks) - Mth.lerp(partialTicks, entity.yo, entity.getY());
      double deltaZ = clientState.getInterpolatedCloakZ(partialTicks) - Mth.lerp(partialTicks, entity.zo, entity.getZ());
      float yBodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
      double forwardX = Mth.sin(yBodyRot * (float) (Math.PI / 180.0));
      double forwardZ = -Mth.cos(yBodyRot * (float) (Math.PI / 180.0));
      state.capeFlap = (float)deltaY * 10.0F;
      state.capeFlap = Mth.clamp(state.capeFlap, -6.0F, 32.0F);
      state.capeLean = (float)(deltaX * forwardX + deltaZ * forwardZ) * 100.0F;
      state.capeLean = state.capeLean * (1.0F - state.fallFlyingScale());
      state.capeLean = Mth.clamp(state.capeLean, 0.0F, 150.0F);
      state.capeLean2 = (float)(deltaX * forwardZ - deltaZ * forwardX) * 100.0F;
      state.capeLean2 = Mth.clamp(state.capeLean2, -20.0F, 20.0F);
      float pow = clientState.getInterpolatedBob(partialTicks);
      float walkDistance = clientState.getInterpolatedWalkDistance(partialTicks);
      state.capeFlap = state.capeFlap + Mth.sin(walkDistance * 6.0F) * 32.0F * pow;
   }

   public void renderRightHand(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final Identifier skinTexture, final boolean hasSleeve
   ) {
      this.renderHand(poseStack, submitNodeCollector, lightCoords, skinTexture, this.model.rightArm, hasSleeve);
   }

   public void renderLeftHand(
      final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final Identifier skinTexture, final boolean hasSleeve
   ) {
      this.renderHand(poseStack, submitNodeCollector, lightCoords, skinTexture, this.model.leftArm, hasSleeve);
   }

   private void renderHand(
      final PoseStack poseStack,
      final SubmitNodeCollector submitNodeCollector,
      final int lightCoords,
      final Identifier skinTexture,
      final ModelPart arm,
      final boolean hasSleeve
   ) {
      PlayerModel model = this.getModel();
      arm.resetPose();
      arm.visible = true;
      model.leftSleeve.visible = hasSleeve;
      model.rightSleeve.visible = hasSleeve;
      model.leftArm.zRot = -0.1F;
      model.rightArm.zRot = 0.1F;
      submitNodeCollector.submitModelPart(arm, poseStack, RenderTypes.entityTranslucent(skinTexture), lightCoords, OverlayTexture.NO_OVERLAY, null);
   }

   protected void setupRotations(final AvatarRenderState state, final PoseStack poseStack, final float bodyRot, final float entityScale) {
      float swimAmount = state.swimAmount;
      float xRot = state.xRot;
      if (state.isFallFlying) {
         super.setupRotations(state, poseStack, bodyRot, entityScale);
         float scale = state.fallFlyingScale();
         if (!state.isAutoSpinAttack) {
            poseStack.mulPose(Axis.XP.rotationDegrees(scale * (-90.0F - xRot)));
         }

         if (state.shouldApplyFlyingYRot) {
            poseStack.mulPose(Axis.YP.rotation(state.flyingYRot));
         }
      } else if (swimAmount > 0.0F) {
         super.setupRotations(state, poseStack, bodyRot, entityScale);
         float targetXRot = state.isInWater ? -90.0F - xRot : -90.0F;
         float xAngle = Mth.lerp(swimAmount, 0.0F, targetXRot);
         poseStack.mulPose(Axis.XP.rotationDegrees(xAngle));
         if (state.isVisuallySwimming) {
            poseStack.translate(0.0F, -1.0F, 0.3F);
         }
      } else {
         super.setupRotations(state, poseStack, bodyRot, entityScale);
      }
   }

   public boolean isEntityUpsideDown(final AvatarlikeEntity mob) {
      if (mob.isModelPartShown(PlayerModelPart.CAPE)) {
         return mob instanceof Player player ? isPlayerUpsideDown(player) : super.isEntityUpsideDown(mob);
      } else {
         return false;
      }
   }

   public static boolean isPlayerUpsideDown(final Player player) {
      return isUpsideDownName(player.getGameProfile().name());
   }
}
