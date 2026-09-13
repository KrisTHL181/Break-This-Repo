package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;

public class ChargeAttack extends Behavior<Animal> {
   private final int timeBetweenAttacks;
   private final TargetingConditions chargeTargeting;
   private final float speed;
   private final float knockbackForce;
   private final double maxTargetDetectionDistance;
   private final double maxChargeDistance;
   private final SoundEvent chargeSound;
   private Vec3 chargeVelocityVector;
   private Vec3 startPosition;

   public ChargeAttack(int p_454309_, TargetingConditions p_452607_, float p_459510_, float p_458023_, double p_460481_, double p_457572_, SoundEvent p_460832_) {
      super(ImmutableMap.of(MemoryModuleType.CHARGE_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
      this.timeBetweenAttacks = p_454309_;
      this.chargeTargeting = p_452607_;
      this.speed = p_459510_;
      this.knockbackForce = p_458023_;
      this.maxChargeDistance = p_460481_;
      this.maxTargetDetectionDistance = p_457572_;
      this.chargeSound = p_460832_;
      this.chargeVelocityVector = Vec3.ZERO;
      this.startPosition = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_460640_, Animal p_458647_) {
      return p_458647_.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
   }

   protected boolean canStillUse(ServerLevel p_460351_, Animal p_453044_, long p_460690_) {
      Brain<?> brain = p_453044_.getBrain();
      Optional<LivingEntity> optional = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
      if (optional.isEmpty()) {
         return false;
      } else {
         LivingEntity livingentity = optional.get();
         if (p_453044_ instanceof TamableAnimal tamableanimal && tamableanimal.isTame()) {
            return false;
         } else if (p_453044_.position().subtract(this.startPosition).lengthSqr() >= this.maxChargeDistance * this.maxChargeDistance) {
            return false;
         } else if (livingentity.position().subtract(p_453044_.position()).lengthSqr() >= this.maxTargetDetectionDistance * this.maxTargetDetectionDistance) {
            return false;
         } else {
            return !p_453044_.hasLineOfSight(livingentity) ? false : !brain.hasMemoryValue(MemoryModuleType.CHARGE_COOLDOWN_TICKS);
         }
      }
   }

   protected void start(ServerLevel p_455666_, Animal p_453669_, long p_459612_) {
      Brain<?> brain = p_453669_.getBrain();
      this.startPosition = p_453669_.position();
      LivingEntity livingentity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).get();
      Vec3 vec3 = livingentity.position().subtract(p_453669_.position()).normalize();
      this.chargeVelocityVector = vec3.scale(this.speed);
      if (this.canStillUse(p_455666_, p_453669_, p_459612_)) {
         p_453669_.playSound(this.chargeSound);
      }
   }

   protected void tick(ServerLevel p_457794_, Animal p_458796_, long p_455410_) {
      Brain<?> brain = p_458796_.getBrain();
      LivingEntity livingentity = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow();
      p_458796_.lookAt(livingentity, 360.0F, 360.0F);
      p_458796_.setDeltaMovement(this.chargeVelocityVector);
      List<LivingEntity> list = new ArrayList<>(1);
      p_457794_.getEntities(
         EntityTypeTest.forClass(LivingEntity.class),
         p_458796_.getBoundingBox(),
         p_455049_ -> this.chargeTargeting.test(p_457794_, p_458796_, p_455049_),
         list,
         1
      );
      if (!list.isEmpty()) {
         LivingEntity livingentity1 = list.get(0);
         if (p_458796_.hasPassenger(livingentity1)) {
            return;
         }

         this.dealDamageToTarget(p_457794_, p_458796_, livingentity1);
         this.dealKnockBack(p_458796_, livingentity1);
         this.stop(p_457794_, p_458796_, p_455410_);
      }
   }

   private void dealDamageToTarget(ServerLevel p_456640_, Animal p_450797_, LivingEntity p_451597_) {
      DamageSource damagesource = p_456640_.damageSources().mobAttack(p_450797_);
      float f = (float)p_450797_.getAttributeValue(Attributes.ATTACK_DAMAGE);
      if (p_451597_.hurtServer(p_456640_, damagesource, f)) {
         EnchantmentHelper.doPostAttackEffects(p_456640_, p_451597_, damagesource);
      }
   }

   private void dealKnockBack(Animal p_452204_, LivingEntity p_455875_) {
      int i = p_452204_.hasEffect(MobEffects.SPEED) ? p_452204_.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
      int j = p_452204_.hasEffect(MobEffects.SLOWNESS) ? p_452204_.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
      float f = 0.25F * (i - j);
      float f1 = Mth.clamp(this.speed * (float)p_452204_.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.2F, 2.0F) + f;
      p_452204_.causeExtraKnockback(p_455875_, f1 * this.knockbackForce, p_452204_.getDeltaMovement());
   }

   protected void stop(ServerLevel p_460342_, Animal p_457493_, long p_455821_) {
      p_457493_.getBrain().setMemory(MemoryModuleType.CHARGE_COOLDOWN_TICKS, this.timeBetweenAttacks);
      p_457493_.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
   }
}
