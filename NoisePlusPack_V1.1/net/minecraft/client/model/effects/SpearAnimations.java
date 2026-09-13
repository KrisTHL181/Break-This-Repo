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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpearAnimations {
   static float progress(float p_458081_, float p_458435_, float p_459481_) {
      return Mth.clamp(Mth.inverseLerp(p_458081_, p_458435_, p_459481_), 0.0F, 1.0F);
   }

   public static <T extends HumanoidRenderState> void thirdPersonHandUse(
      ModelPart p_455936_, ModelPart p_454358_, boolean p_454141_, ItemStack p_455547_, T p_458807_
   ) {
      int i = p_454141_ ? 1 : -1;
      p_455936_.yRot = -0.1F * i + p_454358_.yRot;
      p_455936_.xRot = (float) (-Math.PI / 2) + p_454358_.xRot + 0.8F;
      if (p_458807_.isFallFlying || p_458807_.swimAmount > 0.0F) {
         p_455936_.xRot -= 0.9599311F;
      }

      p_455936_.yRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * p_455936_.yRot, -60.0F, 60.0F);
      p_455936_.xRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * p_455936_.xRot, -120.0F, 30.0F);
      if (!(p_458807_.ticksUsingItem <= 0.0F)
         && (!p_458807_.isUsingItem || p_458807_.useItemHand == (p_454141_ ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND))) {
         KineticWeapon kineticweapon = p_455547_.get(DataComponents.KINETIC_WEAPON);
         if (kineticweapon != null) {
            SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, p_458807_.ticksUsingItem);
            p_455936_.yRot = p_455936_.yRot
               + -i * spearanimations$useparams.swayScaleFast() * (float) (Math.PI / 180.0) * spearanimations$useparams.swayIntensity() * 1.0F;
            p_455936_.zRot = p_455936_.zRot
               + -i * spearanimations$useparams.swayScaleSlow() * (float) (Math.PI / 180.0) * spearanimations$useparams.swayIntensity() * 0.5F;
            p_455936_.xRot = p_455936_.xRot
               + (float) (Math.PI / 180.0)
                  * (
                     -40.0F * spearanimations$useparams.raiseProgressStart()
                        + 30.0F * spearanimations$useparams.raiseProgressMiddle()
                        + -20.0F * spearanimations$useparams.raiseProgressEnd()
                        + 20.0F * spearanimations$useparams.lowerProgress()
                        + 10.0F * spearanimations$useparams.raiseBackProgress()
                        + 0.6F * spearanimations$useparams.swayScaleSlow() * spearanimations$useparams.swayIntensity()
                  );
         }
      }
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonUseItem(
      S p_454120_, PoseStack p_460100_, float p_460157_, HumanoidArm p_459860_, ItemStack p_451097_
   ) {
      KineticWeapon kineticweapon = p_451097_.get(DataComponents.KINETIC_WEAPON);
      if (kineticweapon != null && p_460157_ != 0.0F) {
         float f = Ease.inQuad(progress(p_454120_.attackTime, 0.05F, 0.2F));
         float f1 = Ease.inOutExpo(progress(p_454120_.attackTime, 0.4F, 1.0F));
         SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, p_460157_);
         int i = p_459860_ == HumanoidArm.RIGHT ? 1 : -1;
         float f2 = 1.0F - Ease.outBack(1.0F - spearanimations$useparams.raiseProgress());
         float f3 = 0.125F;
         float f4 = hitFeedbackAmount(p_454120_.ticksSinceKineticHitFeedback);
         p_460100_.translate(0.0, -f4 * 0.4, -kineticweapon.forwardMovement() * (f2 - spearanimations$useparams.raiseBackProgress()) + f4);
         p_460100_.rotateAround(
            Axis.XN.rotationDegrees(70.0F * (spearanimations$useparams.raiseProgress() - spearanimations$useparams.raiseBackProgress()) - 40.0F * (f - f1)),
            0.0F,
            -0.03125F,
            0.125F
         );
         p_460100_.rotateAround(
            Axis.YP.rotationDegrees(i * 90 * (spearanimations$useparams.raiseProgress() - spearanimations$useparams.swayProgress() + 3.0F * f1 + f)),
            0.0F,
            0.0F,
            0.125F
         );
      }
   }

   public static <T extends HumanoidRenderState> void thirdPersonAttackHand(HumanoidModel<T> p_453858_, T p_452955_) {
      float f = p_452955_.attackTime;
      HumanoidArm humanoidarm = p_452955_.attackArm;
      p_453858_.rightArm.yRot = p_453858_.rightArm.yRot - p_453858_.body.yRot;
      p_453858_.leftArm.yRot = p_453858_.leftArm.yRot - p_453858_.body.yRot;
      p_453858_.leftArm.xRot = p_453858_.leftArm.xRot - p_453858_.body.yRot;
      float f1 = Ease.inOutSine(progress(f, 0.0F, 0.05F));
      float f2 = Ease.inQuad(progress(f, 0.05F, 0.2F));
      float f3 = Ease.inOutExpo(progress(f, 0.4F, 1.0F));
      p_453858_.getArm(humanoidarm).xRot += (90.0F * f1 - 120.0F * f2 + 30.0F * f3) * (float) (Math.PI / 180.0);
   }

   public static <S extends ArmedEntityRenderState> void thirdPersonAttackItem(S p_458551_, PoseStack p_452897_) {
      if (!(p_458551_.attackTime <= 0.0F)) {
         KineticWeapon kineticweapon = p_458551_.getMainHandItemStack().get(DataComponents.KINETIC_WEAPON);
         float f = kineticweapon != null ? kineticweapon.forwardMovement() : 0.0F;
         float f1 = 0.125F;
         float f2 = p_458551_.attackTime;
         float f3 = Ease.inQuad(progress(f2, 0.05F, 0.2F));
         float f4 = Ease.inOutExpo(progress(f2, 0.4F, 1.0F));
         p_452897_.rotateAround(Axis.XN.rotationDegrees(70.0F * (f3 - f4)), 0.0F, -0.125F, 0.125F);
         p_452897_.translate(0.0F, f * (f3 - f4), 0.0F);
      }
   }

   private static float hitFeedbackAmount(float p_451376_) {
      return 0.4F * (Ease.outQuart(progress(p_451376_, 1.0F, 3.0F)) - Ease.inOutSine(progress(p_451376_, 3.0F, 10.0F)));
   }

   public static void firstPersonUse(float p_450297_, PoseStack p_458350_, float p_454446_, HumanoidArm p_456598_, ItemStack p_454772_) {
      KineticWeapon kineticweapon = p_454772_.get(DataComponents.KINETIC_WEAPON);
      if (kineticweapon != null) {
         SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, p_454446_);
         int i = p_456598_ == HumanoidArm.RIGHT ? 1 : -1;
         p_458350_.translate(
            i
               * (
                  spearanimations$useparams.raiseProgress() * 0.15F
                     + spearanimations$useparams.raiseProgressEnd() * -0.05F
                     + spearanimations$useparams.swayProgress() * -0.1F
                     + spearanimations$useparams.swayScaleSlow() * 0.005F
               ),
            spearanimations$useparams.raiseProgress() * -0.075F
               + spearanimations$useparams.raiseProgressMiddle() * 0.075F
               + spearanimations$useparams.swayScaleFast() * 0.01F,
            spearanimations$useparams.raiseProgressStart() * 0.05
               + spearanimations$useparams.raiseProgressEnd() * -0.05
               + spearanimations$useparams.swayScaleSlow() * 0.005F
         );
         p_458350_.rotateAround(
            Axis.XP
               .rotationDegrees(
                  -65.0F * Ease.inOutBack(spearanimations$useparams.raiseProgress())
                     - 35.0F * spearanimations$useparams.lowerProgress()
                     + 100.0F * spearanimations$useparams.raiseBackProgress()
                     + -0.5F * spearanimations$useparams.swayScaleFast()
               ),
            0.0F,
            0.1F,
            0.0F
         );
         p_458350_.rotateAround(
            Axis.YN
               .rotationDegrees(
                  i
                     * (
                        -90.0F * progress(spearanimations$useparams.raiseProgress(), 0.5F, 0.55F)
                           + 90.0F * spearanimations$useparams.swayProgress()
                           + 2.0F * spearanimations$useparams.swayScaleSlow()
                     )
               ),
            i * 0.15F,
            0.0F,
            0.0F
         );
         p_458350_.translate(0.0F, -hitFeedbackAmount(p_450297_), 0.0F);
      }
   }

   public static void firstPersonAttack(float p_450980_, PoseStack p_457121_, int p_454745_, HumanoidArm p_450654_) {
      float f = Ease.inOutSine(progress(p_450980_, 0.0F, 0.05F));
      float f1 = Ease.outBack(progress(p_450980_, 0.05F, 0.2F));
      float f2 = Ease.inOutExpo(progress(p_450980_, 0.4F, 1.0F));
      p_457121_.translate(p_454745_ * 0.1F * (f - f1), -0.075F * (f - f2), 0.65F * (f - f1));
      p_457121_.mulPose(Axis.XP.rotationDegrees(-70.0F * (f - f2)));
      p_457121_.translate(0.0, 0.0, -0.25 * (f2 - f1));
   }

   @OnlyIn(Dist.CLIENT)
   record UseParams(
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
      public static SpearAnimations.UseParams fromKineticWeapon(KineticWeapon p_455063_, float p_454591_) {
         int i = p_455063_.delayTicks();
         int j = p_455063_.dismountConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         int k = j - 20;
         int l = p_455063_.knockbackConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         int i1 = l - 40;
         int j1 = p_455063_.damageConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
         float f = SpearAnimations.progress(p_454591_, 0.0F, i);
         float f1 = SpearAnimations.progress(f, 0.0F, 0.5F);
         float f2 = SpearAnimations.progress(f, 0.5F, 0.8F);
         float f3 = SpearAnimations.progress(f, 0.8F, 1.0F);
         float f4 = SpearAnimations.progress(p_454591_, k, i1);
         float f5 = Ease.outCubic(Ease.inOutElastic(SpearAnimations.progress(p_454591_ - 20.0F, i1, l)));
         float f6 = SpearAnimations.progress(p_454591_, j1 - 5, j1);
         float f7 = 2.0F * Ease.outCirc(f4) - 2.0F * Ease.inCirc(f6);
         float f8 = Mth.sin(p_454591_ * 19.0F * (float) (Math.PI / 180.0)) * f7;
         float f9 = Mth.sin(p_454591_ * 30.0F * (float) (Math.PI / 180.0)) * f7;
         return new SpearAnimations.UseParams(f, f1, f2, f3, f4, f5, f6, f7, f8, f9);
      }
   }
}
