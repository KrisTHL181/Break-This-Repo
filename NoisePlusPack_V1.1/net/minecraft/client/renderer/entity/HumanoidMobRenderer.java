package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class HumanoidMobRenderer<T extends Mob, S extends HumanoidRenderState, M extends HumanoidModel<S>> extends AgeableMobRenderer<T, S, M> {
   public HumanoidMobRenderer(EntityRendererProvider.Context p_174169_, M p_174170_, float p_174171_) {
      this(p_174169_, p_174170_, p_174170_, p_174171_);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context p_362704_, M p_368165_, M p_364268_, float p_362118_) {
      this(p_362704_, p_368165_, p_364268_, p_362118_, CustomHeadLayer.Transforms.DEFAULT);
   }

   public HumanoidMobRenderer(EntityRendererProvider.Context p_174173_, M p_174174_, M p_363809_, float p_174175_, CustomHeadLayer.Transforms p_362585_) {
      super(p_174173_, p_174174_, p_363809_, p_174175_);
      this.addLayer(new CustomHeadLayer<>(this, p_174173_.getModelSet(), p_174173_.getPlayerSkinRenderCache(), p_362585_));
      this.addLayer(new WingsLayer<>(this, p_174173_.getModelSet(), p_174173_.getEquipmentRenderer()));
      this.addLayer(new ItemInHandLayer<>(this));
   }

   protected HumanoidModel.ArmPose getArmPose(T p_375388_, HumanoidArm p_378786_) {
      ItemStack itemstack = p_375388_.getItemHeldByArm(p_378786_);
      SwingAnimation swinganimation = itemstack.get(DataComponents.SWING_ANIMATION);
      if (swinganimation != null && swinganimation.type() == SwingAnimationType.STAB && p_375388_.swinging) {
         return HumanoidModel.ArmPose.SPEAR;
      } else {
         return itemstack.is(ItemTags.SPEARS) ? HumanoidModel.ArmPose.SPEAR : HumanoidModel.ArmPose.EMPTY;
      }
   }

   public void extractRenderState(T p_368012_, S p_365777_, float p_367477_) {
      super.extractRenderState(p_368012_, p_365777_, p_367477_);
      extractHumanoidRenderState(p_368012_, p_365777_, p_367477_, this.itemModelResolver);
      p_365777_.leftArmPose = this.getArmPose(p_368012_, HumanoidArm.LEFT);
      p_365777_.rightArmPose = this.getArmPose(p_368012_, HumanoidArm.RIGHT);
   }

   public static void extractHumanoidRenderState(LivingEntity p_362179_, HumanoidRenderState p_369040_, float p_362487_, ItemModelResolver p_377966_) {
      ArmedEntityRenderState.extractArmedEntityRenderState(p_362179_, p_369040_, p_377966_, p_362487_);
      p_369040_.isCrouching = p_362179_.isCrouching();
      p_369040_.isFallFlying = p_362179_.isFallFlying();
      p_369040_.isVisuallySwimming = p_362179_.isVisuallySwimming();
      p_369040_.isPassenger = p_362179_.isPassenger();
      p_369040_.speedValue = 1.0F;
      if (p_369040_.isFallFlying) {
         p_369040_.speedValue = (float)p_362179_.getDeltaMovement().lengthSqr();
         p_369040_.speedValue /= 0.2F;
         p_369040_.speedValue = p_369040_.speedValue * (p_369040_.speedValue * p_369040_.speedValue);
      }

      if (p_369040_.speedValue < 1.0F) {
         p_369040_.speedValue = 1.0F;
      }

      p_369040_.swimAmount = p_362179_.getSwimAmount(p_362487_);
      p_369040_.attackArm = getAttackArm(p_362179_);
      p_369040_.useItemHand = p_362179_.getUsedItemHand();
      p_369040_.maxCrossbowChargeDuration = CrossbowItem.getChargeDuration(p_362179_.getUseItem(), p_362179_);
      p_369040_.ticksUsingItem = p_362179_.getTicksUsingItem(p_362487_);
      p_369040_.isUsingItem = p_362179_.isUsingItem();
      p_369040_.elytraRotX = p_362179_.elytraAnimationState.getRotX(p_362487_);
      p_369040_.elytraRotY = p_362179_.elytraAnimationState.getRotY(p_362487_);
      p_369040_.elytraRotZ = p_362179_.elytraAnimationState.getRotZ(p_362487_);
      p_369040_.headEquipment = getEquipmentIfRenderable(p_362179_, EquipmentSlot.HEAD);
      p_369040_.chestEquipment = getEquipmentIfRenderable(p_362179_, EquipmentSlot.CHEST);
      p_369040_.legsEquipment = getEquipmentIfRenderable(p_362179_, EquipmentSlot.LEGS);
      p_369040_.feetEquipment = getEquipmentIfRenderable(p_362179_, EquipmentSlot.FEET);
   }

   private static ItemStack getEquipmentIfRenderable(LivingEntity p_377024_, EquipmentSlot p_376334_) {
      ItemStack itemstack = p_377024_.getItemBySlot(p_376334_);
      return HumanoidArmorLayer.shouldRender(itemstack, p_376334_) ? itemstack.copy() : ItemStack.EMPTY;
   }

   private static HumanoidArm getAttackArm(LivingEntity p_369949_) {
      HumanoidArm humanoidarm = p_369949_.getMainArm();
      return p_369949_.swingingArm == InteractionHand.MAIN_HAND ? humanoidarm : humanoidarm.getOpposite();
   }
}
