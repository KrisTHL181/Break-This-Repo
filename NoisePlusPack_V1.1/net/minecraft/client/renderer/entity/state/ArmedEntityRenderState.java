package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmedEntityRenderState extends LivingEntityRenderState {
   public HumanoidArm mainArm = HumanoidArm.RIGHT;
   public HumanoidModel.ArmPose rightArmPose = HumanoidModel.ArmPose.EMPTY;
   public final ItemStackRenderState rightHandItemState = new ItemStackRenderState();
   public ItemStack rightHandItemStack = ItemStack.EMPTY;
   public HumanoidModel.ArmPose leftArmPose = HumanoidModel.ArmPose.EMPTY;
   public final ItemStackRenderState leftHandItemState = new ItemStackRenderState();
   public ItemStack leftHandItemStack = ItemStack.EMPTY;
   public SwingAnimationType swingAnimationType = SwingAnimationType.WHACK;
   public float attackTime;

   public ItemStackRenderState getMainHandItemState() {
      return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemState : this.leftHandItemState;
   }

   public ItemStack getMainHandItemStack() {
      return this.mainArm == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
   }

   public ItemStack getUseItemStackForArm(HumanoidArm p_456879_) {
      return p_456879_ == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
   }

   public float ticksUsingItem(HumanoidArm p_459318_) {
      return 0.0F;
   }

   public static void extractArmedEntityRenderState(LivingEntity p_378749_, ArmedEntityRenderState p_378508_, ItemModelResolver p_378441_, float p_456841_) {
      p_378508_.mainArm = p_378749_.getMainArm();
      ItemStack itemstack = p_378749_.getMainHandItem();
      p_378508_.swingAnimationType = itemstack.getSwingAnimation().type();
      p_378508_.attackTime = p_378749_.getAttackAnim(p_456841_);
      p_378441_.updateForLiving(
         p_378508_.rightHandItemState, p_378749_.getItemHeldByArm(HumanoidArm.RIGHT), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, p_378749_
      );
      p_378441_.updateForLiving(p_378508_.leftHandItemState, p_378749_.getItemHeldByArm(HumanoidArm.LEFT), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, p_378749_);
      p_378508_.leftHandItemStack = p_378749_.getItemHeldByArm(HumanoidArm.LEFT).copy();
      p_378508_.rightHandItemStack = p_378749_.getItemHeldByArm(HumanoidArm.RIGHT).copy();
   }
}
