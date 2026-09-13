package net.minecraft.client.model.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;

public class SpearAnimations {
   private static float progress(final float time, final float start, final float end) {
      return Mth.clamp(Mth.inverseLerp(time, start, end), 0.0F, 1.0F);
   }

   public static <T extends HumanoidRenderState> void thirdPersonHandUse(
      final ModelPart arm, final ModelPart head, final boolean holdingInRightArm, final ItemStack item, final T state
   ) {
      int invert = holdingInRightArm ? 1 : -1;
      arm.yRot = -0.1F * invert + head.yRot;
      arm.xRot = (float) (-Math.PI / 2) + head.xRot + 0.8F;
      if (state.isFallFlying || state.swimAmount > 0.0F) {
         arm.xRot -= 0.9599311F;
      }

      arm.yRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * arm.yRot, -60.0F, 60.0F);
      arm.xRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * arm.xRot, -120.0F, 30.0F);
      if (!(state.ticksUsingItem <= 0.0F)
         && (!state.isUsingItem || state.useItemHand == (holdingInRightArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND))) {
         KineticWeapon kineticWeapon = item.get(DataComponents.KINETIC_WEAPON);
         if (kineticWeapon != null) {
            SpearAnimations.UseParams params = SpearAnimations.UseParams.fromKineticWeapon(kineticWeapon, state.ticksUsingItem);
            arm.yRot = arm.yRot + -invert * params.swayScaleFast() * (float) (Math.PI / 180.0) * params.swayIntensity() * 1.0F;
            arm.zRot = arm.zRot + -invert * params.swayScaleSlow() * (float) (Math.PI / 180.0) * params.swayIntensity() * 0.5F;
            arm.xRot = arm.xRot
               + (float) (Math.PI / 180.0)
                  * (
                     -40.0F * params.raiseProgressStart()
                        + 30.0F * params.raiseProgressMiddle()
                        + -20.0F * params.raiseProgressEnd()
                        + 20.0F * params.lowerProgress()
                        + 10.0F * params.raiseBackProgress()
                        + 0.6F * params.swayScaleSlow() * params.swayIntensity()
                  );
         }
      }
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonUseItem(
      final S state, final PoseStack poseStack, final float timeHeld, final HumanoidArm arm, final ItemStack actualItem
   ) {
      KineticWeapon kineticWeapon = actualItem.get(DataComponents.KINETIC_WEAPON);
      if (kineticWeapon != null && timeHeld != 0.0F) {
         float attack = Ease.inQuad(progress(state.attackTime, 0.05F, 0.2F));
         float retract = Ease.inOutExpo(progress(state.attackTime, 0.4F, 1.0F));
         SpearAnimations.UseParams params = SpearAnimations.UseParams.fromKineticWeapon(kineticWeapon, timeHeld);
         int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
         float raiseProgressModified = 1.0F - Ease.outBack(1.0F - params.raiseProgress());
         float itemInHandDepth = 0.125F;
         float hitFeedback = hitFeedbackAmount(state.ticksSinceKineticHitFeedback);
         poseStack.translate(0.0, -hitFeedback * 0.4, -kineticWeapon.forwardMovement() * (raiseProgressModified - params.raiseBackProgress()) + hitFeedback);
         poseStack.rotateAround(
            Axis.XN.rotationDegrees(70.0F * (params.raiseProgress() - params.raiseBackProgress()) - 40.0F * (attack - retract)), 0.0F, -0.03125F, 0.125F
         );
         poseStack.rotateAround(
            Axis.YP.rotationDegrees(invert * 90 * (params.raiseProgress() - params.swayProgress() + 3.0F * retract + attack)), 0.0F, 0.0F, 0.125F
         );
      }
   }

   public static <T extends HumanoidRenderState> void thirdPersonAttackHand(final HumanoidModel<T> model, final T state) {
      float attackTime = state.attackTime;
      HumanoidArm arm = state.attackArm;
      model.rightArm.yRot = model.rightArm.yRot - model.body.yRot;
      model.leftArm.yRot = model.leftArm.yRot - model.body.yRot;
      model.leftArm.xRot = model.leftArm.xRot - model.body.yRot;
      float prepare = Ease.inOutSine(progress(attackTime, 0.0F, 0.05F));
      float attack = Ease.inQuad(progress(attackTime, 0.05F, 0.2F));
      float retract = Ease.inOutExpo(progress(attackTime, 0.4F, 1.0F));
      model.getArm(arm).xRot += (90.0F * prepare - 120.0F * attack + 30.0F * retract) * (float) (Math.PI / 180.0);
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonAttackItem(final S state, final PoseStack poseStack) {
      if (!(state.attackTime <= 0.0F)) {
         KineticWeapon kineticWeapon = state.getMainHandItemStack().get(DataComponents.KINETIC_WEAPON);
         float jetForward = kineticWeapon != null ? kineticWeapon.forwardMovement() : 0.0F;
         float itemInHandDepth = 0.125F;
         float attackTime = state.attackTime;
         float attack = Ease.inQuad(progress(attackTime, 0.05F, 0.2F));
         float retract = Ease.inOutExpo(progress(attackTime, 0.4F, 1.0F));
         poseStack.rotateAround(Axis.XN.rotationDegrees(70.0F * (attack - retract)), 0.0F, -0.125F, 0.125F);
         poseStack.translate(0.0F, jetForward * (attack - retract), 0.0F);
      }
   }

   private static float hitFeedbackAmount(final float ticksSinceFeedbackStart) {
      return 0.4F * (Ease.outQuart(progress(ticksSinceFeedbackStart, 1.0F, 3.0F)) - Ease.inOutSine(progress(ticksSinceFeedbackStart, 3.0F, 10.0F)));
   }

   public static void firstPersonUse(
      final float ticksSinceKineticHitFeedback, final PoseStack poseStack, final float timeHeld, final HumanoidArm arm, final ItemStack itemStack
   ) {
      KineticWeapon kineticWeapon = itemStack.get(DataComponents.KINETIC_WEAPON);
      if (kineticWeapon != null) {
         SpearAnimations.UseParams params = SpearAnimations.UseParams.fromKineticWeapon(kineticWeapon, timeHeld);
         int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
         poseStack.translate(
            invert * (params.raiseProgress() * 0.15F + params.raiseProgressEnd() * -0.05F + params.swayProgress() * -0.1F + params.swayScaleSlow() * 0.005F),
            params.raiseProgress() * -0.075F + params.raiseProgressMiddle() * 0.075F + params.swayScaleFast() * 0.01F,
            params.raiseProgressStart() * 0.05 + params.raiseProgressEnd() * -0.05 + params.swayScaleSlow() * 0.005F
         );
         poseStack.rotateAround(
            Axis.XP
               .rotationDegrees(
                  -65.0F * Ease.inOutBack(params.raiseProgress())
                     - 35.0F * params.lowerProgress()
                     + 100.0F * params.raiseBackProgress()
                     + -0.5F * params.swayScaleFast()
               ),
            0.0F,
            0.1F,
            0.0F
         );
         poseStack.rotateAround(
            Axis.YN
               .rotationDegrees(
                  invert * (-90.0F * progress(params.raiseProgress(), 0.5F, 0.55F) + 90.0F * params.swayProgress() + 2.0F * params.swayScaleSlow())
               ),
            invert * 0.15F,
            0.0F,
            0.0F
         );
         poseStack.translate(0.0F, -hitFeedbackAmount(ticksSinceKineticHitFeedback), 0.0F);
      }
   }

   public static void firstPersonAttack(final float attack, final PoseStack poseStack, final int invert, final HumanoidArm arm) {
      float startingAmount = Ease.inOutSine(progress(attack, 0.0F, 0.05F));
      float middleAmount = Ease.outBack(progress(attack, 0.05F, 0.2F));
      float endingAmount = Ease.inOutExpo(progress(attack, 0.4F, 1.0F));
      poseStack.translate(invert * 0.1F * (startingAmount - middleAmount), -0.075F * (startingAmount - endingAmount), 0.65F * (startingAmount - middleAmount));
      poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F * (startingAmount - endingAmount)));
      poseStack.translate(0.0, 0.0, -0.25 * (endingAmount - middleAmount));
   }

   private record UseParams(
      float raiseProgress,
      float raiseProgressStart,
      float raiseProgressMiddle,
      float raiseProgressEnd,
      float swayProgress,
      float lowerProgress,
      float raiseBackProgress,
      float swayIntensity,
      float swayScaleSlow,
      float swayScaleFast
   ) {
      public static SpearAnimations.UseParams fromKineticWeapon(final KineticWeapon kineticWeapon, final float time) {
         int finishRaisingTick = kineticWeapon.delayTicks();
         int finishSwayingTick = kineticWeapon.dismountConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + finishRaisingTick;
         int startSwayingTick = finishSwayingTick - 20;
         int finishLoweringTick = kineticWeapon.knockbackConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + finishRaisingTick;
         int startLoweringTick = finishLoweringTick - 40;
         int finishRaisingBackTick = kineticWeapon.damageConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + finishRaisingTick;
         float raiseProgress = SpearAnimations.progress(time, 0.0F, finishRaisingTick);
         float raiseProgressStart = SpearAnimations.progress(raiseProgress, 0.0F, 0.5F);
         float raiseProgressMiddle = SpearAnimations.progress(raiseProgress, 0.5F, 0.8F);
         float raiseProgressEnd = SpearAnimations.progress(raiseProgress, 0.8F, 1.0F);
         float swayProgress = SpearAnimations.progress(time, startSwayingTick, finishSwayingTick);
         float lowerProgress = Ease.outCubic(Ease.inOutElastic(SpearAnimations.progress(time - 20.0F, startLoweringTick, finishLoweringTick)));
         float raiseBackProgress = SpearAnimations.progress(time, finishRaisingBackTick - 5, finishRaisingBackTick);
         float swayIntensity = 2.0F * Ease.outCirc(swayProgress) - 2.0F * Ease.inCirc(raiseBackProgress);
         float swayScaleSlow = Mth.sin(time * 19.0F * (float) (Math.PI / 180.0)) * swayIntensity;
         float swayScaleFast = Mth.sin(time * 30.0F * (float) (Math.PI / 180.0)) * swayIntensity;
         return new SpearAnimations.UseParams(
            raiseProgress,
            raiseProgressStart,
            raiseProgressMiddle,
            raiseProgressEnd,
            swayProgress,
            lowerProgress,
            raiseBackProgress,
            swayIntensity,
            swayScaleSlow,
            swayScaleFast
         );
      }
   }
}
