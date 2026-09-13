package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;

public class ArmedEntityRenderState extends LivingEntityRenderState {
   public HumanoidArm mainArm = HumanoidArm.RIGHT;
   public HumanoidArm attackArm = HumanoidArm.RIGHT;
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

   public ItemStack getUseItemStackForArm(final HumanoidArm arm) {
      return arm == HumanoidArm.RIGHT ? this.rightHandItemStack : this.leftHandItemStack;
   }

   public float ticksUsingItem(final HumanoidArm arm) {
      return 0.0F;
   }

   public static void extractArmedEntityRenderState(
      final LivingEntity entity, final ArmedEntityRenderState state, final ItemModelResolver itemModelResolver, final float partialTicks
   ) {
      state.mainArm = entity.getMainArm();
      state.attackArm = entity.swingingArm != InteractionHand.OFF_HAND ? state.mainArm : state.mainArm.getOpposite();
      ItemStack itemStack = entity.getItemHeldByArm(state.attackArm);
      state.swingAnimationType = itemStack.getSwingAnimation().type();
      state.attackTime = entity.getAttackAnim(partialTicks);
      itemModelResolver.updateForLiving(
         state.rightHandItemState, entity.getItemHeldByArm(HumanoidArm.RIGHT), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, entity
      );
      itemModelResolver.updateForLiving(state.leftHandItemState, entity.getItemHeldByArm(HumanoidArm.LEFT), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, entity);
      state.leftHandItemStack = entity.getItemHeldByArm(HumanoidArm.LEFT).copy();
      state.rightHandItemStack = entity.getItemHeldByArm(HumanoidArm.RIGHT).copy();
   }
}
