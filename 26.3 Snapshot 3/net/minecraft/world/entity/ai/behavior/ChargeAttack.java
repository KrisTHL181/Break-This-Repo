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

   public ChargeAttack(
      final int timeBetweenAttacks,
      final TargetingConditions chargeTargeting,
      final float speed,
      final float knockbackForce,
      final double maxChargeDistance,
      final double maxTargetDetectionDistance,
      final SoundEvent chargeSound
   ) {
      super(ImmutableMap.of(MemoryModuleType.CHARGE_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
      this.timeBetweenAttacks = timeBetweenAttacks;
      this.chargeTargeting = chargeTargeting;
      this.speed = speed;
      this.knockbackForce = knockbackForce;
      this.maxChargeDistance = maxChargeDistance;
      this.maxTargetDetectionDistance = maxTargetDetectionDistance;
      this.chargeSound = chargeSound;
      this.chargeVelocityVector = Vec3.ZERO;
      this.startPosition = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Animal body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
   }

   protected boolean canStillUse(final ServerLevel level, final Animal body, final long timestamp) {
      Brain<?> brain = body.getBrain();
      Optional<LivingEntity> attackCandidate = brain.getMemory(MemoryModuleType.ATTACK_TARGET);
      if (attackCandidate.isEmpty()) {
         return false;
      } else {
         LivingEntity attackTarget = attackCandidate.get();
         if (body instanceof TamableAnimal tamedAnimal && tamedAnimal.isTame()) {
            return false;
         } else if (body.position().subtract(this.startPosition).lengthSqr() >= this.maxChargeDistance * this.maxChargeDistance) {
            return false;
         } else if (attackTarget.position().subtract(body.position()).lengthSqr() >= this.maxTargetDetectionDistance * this.maxTargetDetectionDistance) {
            return false;
         } else {
            return !body.hasLineOfSight(attackTarget) ? false : !brain.hasMemoryValue(MemoryModuleType.CHARGE_COOLDOWN_TICKS);
         }
      }
   }

   protected void start(final ServerLevel level, final Animal body, final long timestamp) {
      Brain<?> brain = body.getBrain();
      this.startPosition = body.position();
      LivingEntity attackCandidate = brain.getMemory(MemoryModuleType.ATTACK_TARGET).get();
      Vec3 direction = attackCandidate.position().subtract(body.position()).normalize();
      this.chargeVelocityVector = direction.scale(this.speed);
      if (this.canStillUse(level, body, timestamp)) {
         body.playSound(this.chargeSound);
      }
   }

   protected void tick(final ServerLevel level, final Animal body, final long timestamp) {
      Brain<?> brain = body.getBrain();
      LivingEntity attackTarget = brain.getMemory(MemoryModuleType.ATTACK_TARGET).orElseThrow();
      body.lookAt(attackTarget, 360.0F, 360.0F);
      body.setDeltaMovement(this.chargeVelocityVector);
      List<LivingEntity> collidingEntities = new ArrayList<>(1);
      level.getEntities(
         EntityTypeTest.forClass(LivingEntity.class), body.getBoundingBox(), e -> this.chargeTargeting.test(level, body, e), collidingEntities, 1
      );
      if (!collidingEntities.isEmpty()) {
         LivingEntity closestAttackTarget = collidingEntities.get(0);
         if (body.hasPassenger(closestAttackTarget)) {
            return;
         }

         this.dealDamageToTarget(level, body, closestAttackTarget);
         this.dealKnockBack(level, body, closestAttackTarget);
         this.stop(level, body, timestamp);
      }
   }

   private void dealDamageToTarget(final ServerLevel level, final Animal body, final LivingEntity target) {
      DamageSource damageSource = level.damageSources().mobAttack(body);
      float damage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE);
      if (target.hurtServer(level, damageSource, damage)) {
         EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
      }
   }

   private void dealKnockBack(final ServerLevel level, final Animal body, final LivingEntity target) {
      int movementSpeedLevel = body.hasEffect(MobEffects.SPEED) ? body.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
      int movementSlowdownLevel = body.hasEffect(MobEffects.SLOWNESS) ? body.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
      float speedBoostPower = 0.25F * (movementSpeedLevel - movementSlowdownLevel);
      float speedFactor = Mth.clamp(this.speed * (float)body.getAttributeValue(Attributes.MOVEMENT_SPEED), 0.2F, 2.0F) + speedBoostPower;
      DamageSource damageSource = level.damageSources().mobAttack(body);
      float damage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE);
      body.causeExtraKnockback(target, speedFactor * this.knockbackForce, body.getDeltaMovement(), damageSource, damage, false);
   }

   protected void stop(final ServerLevel level, final Animal body, final long timestamp) {
      body.getBrain().setMemory(MemoryModuleType.CHARGE_COOLDOWN_TICKS, this.timeBetweenAttacks);
      body.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
   }
}
