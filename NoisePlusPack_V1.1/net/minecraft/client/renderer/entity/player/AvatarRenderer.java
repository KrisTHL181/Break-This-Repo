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
import net.minecraft.client.renderer.state.CameraRenderState;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AvatarRenderer<AvatarlikeEntity extends Avatar & ClientAvatarEntity>
   extends LivingEntityRenderer<AvatarlikeEntity, AvatarRenderState, PlayerModel> {
   public AvatarRenderer(EntityRendererProvider.Context p_426442_, boolean p_428946_) {
      super(p_426442_, new PlayerModel(p_426442_.bakeLayer(p_428946_ ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), p_428946_), 0.5F);
      this.addLayer(
         new HumanoidArmorLayer<>(
            this,
            ArmorModelSet.bake(
               p_428946_ ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
               p_426442_.getModelSet(),
               p_448353_ -> new PlayerModel(p_448353_, p_428946_)
            ),
            p_426442_.getEquipmentRenderer()
         )
      );
      this.addLayer(new PlayerItemInHandLayer<>(this));
      this.addLayer(new ArrowLayer<>(this, p_426442_));
      this.addLayer(new Deadmau5EarsLayer(this, p_426442_.getModelSet()));
      this.addLayer(new CapeLayer(this, p_426442_.getModelSet(), p_426442_.getEquipmentAssets()));
      this.addLayer(new CustomHeadLayer<>(this, p_426442_.getModelSet(), p_426442_.getPlayerSkinRenderCache()));
      this.addLayer(new WingsLayer<>(this, p_426442_.getModelSet(), p_426442_.getEquipmentRenderer()));
      this.addLayer(new ParrotOnShoulderLayer(this, p_426442_.getModelSet()));
      this.addLayer(new SpinAttackEffectLayer(this, p_426442_.getModelSet()));
      this.addLayer(new BeeStingerLayer<>(this, p_426442_));
   }

   protected boolean shouldRenderLayers(AvatarRenderState p_431592_) {
      return !p_431592_.isSpectator;
   }

   public Vec3 getRenderOffset(AvatarRenderState p_428717_) {
      Vec3 vec3 = super.getRenderOffset(p_428717_);
      return p_428717_.isCrouching ? vec3.add(0.0, p_428717_.scale * -2.0F / 16.0, 0.0) : vec3;
   }

   private static HumanoidModel.ArmPose getArmPose(Avatar p_424150_, HumanoidArm p_426932_) {
      ItemStack itemstack = p_424150_.getItemInHand(InteractionHand.MAIN_HAND);
      ItemStack itemstack1 = p_424150_.getItemInHand(InteractionHand.OFF_HAND);
      HumanoidModel.ArmPose humanoidmodel$armpose = getArmPose(p_424150_, itemstack, InteractionHand.MAIN_HAND);
      HumanoidModel.ArmPose humanoidmodel$armpose1 = getArmPose(p_424150_, itemstack1, InteractionHand.OFF_HAND);
      if (humanoidmodel$armpose.isTwoHanded()) {
         humanoidmodel$armpose1 = itemstack1.isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
      }

      return p_424150_.getMainArm() == p_426932_ ? humanoidmodel$armpose : humanoidmodel$armpose1;
   }

   private static HumanoidModel.ArmPose getArmPose(Avatar p_422497_, ItemStack p_429700_, InteractionHand p_430481_) {
      if (p_429700_.isEmpty()) {
         return HumanoidModel.ArmPose.EMPTY;
      }

      if (!p_422497_.swinging && p_429700_.is(Items.CROSSBOW) && CrossbowItem.isCharged(p_429700_)) {
         return HumanoidModel.ArmPose.CROSSBOW_HOLD;
      }

      if (p_422497_.getUsedItemHand() == p_430481_ && p_422497_.getUseItemRemainingTicks() > 0) {
         ItemUseAnimation itemuseanimation = p_429700_.getUseAnimation();
         if (itemuseanimation == ItemUseAnimation.BLOCK) {
            return HumanoidModel.ArmPose.BLOCK;
         }

         if (itemuseanimation == ItemUseAnimation.BOW) {
            return HumanoidModel.ArmPose.BOW_AND_ARROW;
         }

         if (itemuseanimation == ItemUseAnimation.TRIDENT) {
            return HumanoidModel.ArmPose.THROW_TRIDENT;
         }

         if (itemuseanimation == ItemUseAnimation.CROSSBOW) {
            return HumanoidModel.ArmPose.CROSSBOW_CHARGE;
         }

         if (itemuseanimation == ItemUseAnimation.SPYGLASS) {
            return HumanoidModel.ArmPose.SPYGLASS;
         }

         if (itemuseanimation == ItemUseAnimation.TOOT_HORN) {
            return HumanoidModel.ArmPose.TOOT_HORN;
         }

         if (itemuseanimation == ItemUseAnimation.BRUSH) {
            return HumanoidModel.ArmPose.BRUSH;
         }

         if (itemuseanimation == ItemUseAnimation.SPEAR) {
            return HumanoidModel.ArmPose.SPEAR;
         }
      }

      SwingAnimation swinganimation = p_429700_.get(DataComponents.SWING_ANIMATION);
      if (swinganimation != null && swinganimation.type() == SwingAnimationType.STAB && p_422497_.swinging) {
         return HumanoidModel.ArmPose.SPEAR;
      } else {
         return p_429700_.is(ItemTags.SPEARS) ? HumanoidModel.ArmPose.SPEAR : HumanoidModel.ArmPose.ITEM;
      }
   }

   public Identifier getTextureLocation(AvatarRenderState p_458757_) {
      return p_458757_.skin.body().texturePath();
   }

   protected void scale(AvatarRenderState p_428748_, PoseStack p_431286_) {
      float f = 0.9375F;
      p_431286_.scale(0.9375F, 0.9375F, 0.9375F);
   }

   protected void submitNameTag(AvatarRenderState p_425306_, PoseStack p_423892_, SubmitNodeCollector p_428344_, CameraRenderState p_426061_) {
      p_423892_.pushPose();
      int i = p_425306_.showExtraEars ? -10 : 0;
      if (p_425306_.scoreText != null) {
         p_428344_.submitNameTag(
            p_423892_,
            p_425306_.nameTagAttachment,
            i,
            p_425306_.scoreText,
            !p_425306_.isDiscrete,
            p_425306_.lightCoords,
            p_425306_.distanceToCameraSq,
            p_426061_
         );
         p_423892_.translate(0.0F, 0.25875F, 0.0F);
      }

      if (p_425306_.nameTag != null) {
         p_428344_.submitNameTag(
            p_423892_, p_425306_.nameTagAttachment, i, p_425306_.nameTag, !p_425306_.isDiscrete, p_425306_.lightCoords, p_425306_.distanceToCameraSq, p_426061_
         );
      }

      p_423892_.popPose();
   }

   public AvatarRenderState createRenderState() {
      return new AvatarRenderState();
   }

   public void extractRenderState(AvatarlikeEntity p_431243_, AvatarRenderState p_426303_, float p_430950_) {
      super.extractRenderState(p_431243_, p_426303_, p_430950_);
      HumanoidMobRenderer.extractHumanoidRenderState(p_431243_, p_426303_, p_430950_, this.itemModelResolver);
      p_426303_.leftArmPose = getArmPose(p_431243_, HumanoidArm.LEFT);
      p_426303_.rightArmPose = getArmPose(p_431243_, HumanoidArm.RIGHT);
      p_426303_.skin = p_431243_.getSkin();
      p_426303_.arrowCount = p_431243_.getArrowCount();
      p_426303_.stingerCount = p_431243_.getStingerCount();
      p_426303_.isSpectator = p_431243_.isSpectator();
      p_426303_.showHat = p_431243_.isModelPartShown(PlayerModelPart.HAT);
      p_426303_.showJacket = p_431243_.isModelPartShown(PlayerModelPart.JACKET);
      p_426303_.showLeftPants = p_431243_.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
      p_426303_.showRightPants = p_431243_.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
      p_426303_.showLeftSleeve = p_431243_.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
      p_426303_.showRightSleeve = p_431243_.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
      p_426303_.showCape = p_431243_.isModelPartShown(PlayerModelPart.CAPE);
      this.extractFlightData(p_431243_, p_426303_, p_430950_);
      this.extractCapeState(p_431243_, p_426303_, p_430950_);
      if (p_426303_.distanceToCameraSq < 100.0) {
         p_426303_.scoreText = p_431243_.belowNameDisplay();
      } else {
         p_426303_.scoreText = null;
      }

      p_426303_.parrotOnLeftShoulder = p_431243_.getParrotVariantOnShoulder(true);
      p_426303_.parrotOnRightShoulder = p_431243_.getParrotVariantOnShoulder(false);
      p_426303_.id = p_431243_.getId();
      p_426303_.showExtraEars = p_431243_.showExtraEars();
      p_426303_.heldOnHead.clear();
      if (p_426303_.isUsingItem) {
         ItemStack itemstack = p_431243_.getItemInHand(p_426303_.useItemHand);
         if (itemstack.is(Items.SPYGLASS)) {
            this.itemModelResolver.updateForLiving(p_426303_.heldOnHead, itemstack, ItemDisplayContext.HEAD, p_431243_);
         }
      }
   }

   protected boolean shouldShowName(AvatarlikeEntity p_429595_, double p_429961_) {
      return super.shouldShowName(p_429595_, p_429961_)
         && (p_429595_.shouldShowName() || p_429595_.hasCustomName() && p_429595_ == this.entityRenderDispatcher.crosshairPickEntity);
   }

   private void extractFlightData(AvatarlikeEntity p_422452_, AvatarRenderState p_427860_, float p_431214_) {
      p_427860_.fallFlyingTimeInTicks = p_422452_.getFallFlyingTicks() + p_431214_;
      Vec3 vec3 = p_422452_.getViewVector(p_431214_);
      Vec3 vec31 = p_422452_.avatarState().deltaMovementOnPreviousTick().lerp(p_422452_.getDeltaMovement(), p_431214_);
      if (vec31.horizontalDistanceSqr() > 1.0E-5F && vec3.horizontalDistanceSqr() > 1.0E-5F) {
         p_427860_.shouldApplyFlyingYRot = true;
         double d0 = vec31.horizontal().normalize().dot(vec3.horizontal().normalize());
         double d1 = vec31.x * vec3.z - vec31.z * vec3.x;
         p_427860_.flyingYRot = (float)(Math.signum(d1) * Math.acos(Math.min(1.0, Math.abs(d0))));
      } else {
         p_427860_.shouldApplyFlyingYRot = false;
         p_427860_.flyingYRot = 0.0F;
      }
   }

   private void extractCapeState(AvatarlikeEntity p_429924_, AvatarRenderState p_431661_, float p_429067_) {
      ClientAvatarState clientavatarstate = p_429924_.avatarState();
      double d0 = clientavatarstate.getInterpolatedCloakX(p_429067_) - Mth.lerp(p_429067_, p_429924_.xo, p_429924_.getX());
      double d1 = clientavatarstate.getInterpolatedCloakY(p_429067_) - Mth.lerp(p_429067_, p_429924_.yo, p_429924_.getY());
      double d2 = clientavatarstate.getInterpolatedCloakZ(p_429067_) - Mth.lerp(p_429067_, p_429924_.zo, p_429924_.getZ());
      float f = Mth.rotLerp(p_429067_, p_429924_.yBodyRotO, p_429924_.yBodyRot);
      double d3 = Mth.sin(f * (float) (Math.PI / 180.0));
      double d4 = -Mth.cos(f * (float) (Math.PI / 180.0));
      p_431661_.capeFlap = (float)d1 * 10.0F;
      p_431661_.capeFlap = Mth.clamp(p_431661_.capeFlap, -6.0F, 32.0F);
      p_431661_.capeLean = (float)(d0 * d3 + d2 * d4) * 100.0F;
      p_431661_.capeLean = p_431661_.capeLean * (1.0F - p_431661_.fallFlyingScale());
      p_431661_.capeLean = Mth.clamp(p_431661_.capeLean, 0.0F, 150.0F);
      p_431661_.capeLean2 = (float)(d0 * d4 - d2 * d3) * 100.0F;
      p_431661_.capeLean2 = Mth.clamp(p_431661_.capeLean2, -20.0F, 20.0F);
      float f1 = clientavatarstate.getInterpolatedBob(p_429067_);
      float f2 = clientavatarstate.getInterpolatedWalkDistance(p_429067_);
      p_431661_.capeFlap = p_431661_.capeFlap + Mth.sin(f2 * 6.0F) * 32.0F * f1;
   }

   public void renderRightHand(PoseStack p_428282_, SubmitNodeCollector p_424928_, int p_427204_, Identifier p_450403_, boolean p_431039_) {
      this.renderHand(p_428282_, p_424928_, p_427204_, p_450403_, this.model.rightArm, p_431039_);
   }

   public void renderLeftHand(PoseStack p_424615_, SubmitNodeCollector p_425532_, int p_427068_, Identifier p_458705_, boolean p_423290_) {
      this.renderHand(p_424615_, p_425532_, p_427068_, p_458705_, this.model.leftArm, p_423290_);
   }

   private void renderHand(PoseStack p_428166_, SubmitNodeCollector p_424874_, int p_425901_, Identifier p_452067_, ModelPart p_423467_, boolean p_423600_) {
      PlayerModel playermodel = this.getModel();
      p_423467_.resetPose();
      p_423467_.visible = true;
      playermodel.leftSleeve.visible = p_423600_;
      playermodel.rightSleeve.visible = p_423600_;
      playermodel.leftArm.zRot = -0.1F;
      playermodel.rightArm.zRot = 0.1F;
      p_424874_.submitModelPart(p_423467_, p_428166_, RenderTypes.entityTranslucent(p_452067_), p_425901_, OverlayTexture.NO_OVERLAY, null);
   }

   protected void setupRotations(AvatarRenderState p_431675_, PoseStack p_424697_, float p_430322_, float p_430544_) {
      float f = p_431675_.swimAmount;
      float f1 = p_431675_.xRot;
      if (p_431675_.isFallFlying) {
         super.setupRotations(p_431675_, p_424697_, p_430322_, p_430544_);
         float f2 = p_431675_.fallFlyingScale();
         if (!p_431675_.isAutoSpinAttack) {
            p_424697_.mulPose(Axis.XP.rotationDegrees(f2 * (-90.0F - f1)));
         }

         if (p_431675_.shouldApplyFlyingYRot) {
            p_424697_.mulPose(Axis.YP.rotation(p_431675_.flyingYRot));
         }
      } else if (f > 0.0F) {
         super.setupRotations(p_431675_, p_424697_, p_430322_, p_430544_);
         float f4 = p_431675_.isInWater ? -90.0F - f1 : -90.0F;
         float f3 = Mth.lerp(f, 0.0F, f4);
         p_424697_.mulPose(Axis.XP.rotationDegrees(f3));
         if (p_431675_.isVisuallySwimming) {
            p_424697_.translate(0.0F, -1.0F, 0.3F);
         }
      } else {
         super.setupRotations(p_431675_, p_424697_, p_430322_, p_430544_);
      }
   }

   public boolean isEntityUpsideDown(AvatarlikeEntity p_425515_) {
      if (p_425515_.isModelPartShown(PlayerModelPart.CAPE)) {
         return p_425515_ instanceof Player player ? isPlayerUpsideDown(player) : super.isEntityUpsideDown(p_425515_);
      } else {
         return false;
      }
   }

   public static boolean isPlayerUpsideDown(Player p_424650_) {
      return isUpsideDownName(p_424650_.getGameProfile().name());
   }
}
