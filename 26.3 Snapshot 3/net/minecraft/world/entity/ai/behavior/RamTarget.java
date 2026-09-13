package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public class RamTarget extends Behavior<Goat> {
   public static final int TIME_OUT_DURATION = 200;
   public static final float RAM_SPEED_FORCE_FACTOR = 1.65F;
   private final Function<Goat, UniformInt> getTimeBetweenRams;
   private final TargetingConditions ramTargeting;
   private final float speed;
   private final ToDoubleFunction<Goat> getKnockbackForce;
   private Vec3 ramDirection;
   private final Function<Goat, SoundEvent> getImpactSound;
   private final Function<Goat, SoundEvent> getHornBreakSound;

   public RamTarget(
      final Function<Goat, UniformInt> getTimeBetweenRams,
      final TargetingConditions ramTargeting,
      final float speed,
      final ToDoubleFunction<Goat> getKnockbackForce,
      final Function<Goat, SoundEvent> getImpactSound,
      final Function<Goat, SoundEvent> getHornBreakSound
   ) {
      super(ImmutableMap.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_PRESENT), 200);
      this.getTimeBetweenRams = getTimeBetweenRams;
      this.ramTargeting = ramTargeting;
      this.speed = speed;
      this.getKnockbackForce = getKnockbackForce;
      this.getImpactSound = getImpactSound;
      this.getHornBreakSound = getHornBreakSound;
      this.ramDirection = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(final ServerLevel level, final Goat body) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected boolean canStillUse(final ServerLevel level, final Goat body, final long timestamp) {
      return body.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected void start(final ServerLevel level, final Goat body, final long timestamp) {
      BlockPos curPos = body.blockPosition();
      Brain<?> brain = body.getBrain();
      Vec3 ramTargetPos = brain.getMemory(MemoryModuleType.RAM_TARGET).get();
      this.ramDirection = new Vec3(curPos.getX() - ramTargetPos.x(), 0.0, curPos.getZ() - ramTargetPos.z()).normalize();
      brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(ramTargetPos, this.speed, 0));
   }

   protected void tick(final ServerLevel level, final Goat body, final long timestamp) {
      List<LivingEntity> nearbyEntities = level.getNearbyEntities(LivingEntity.class, this.ramTargeting, body, body.getBoundingBox());
      Brain<?> brain = body.getBrain();
      if (!nearbyEntities.isEmpty()) {
         LivingEntity ramTarget = nearbyEntities.get(0);
         DamageSource damageSource = level.damageSources().noAggroMobAttack(body);
         float damage = (float)body.getAttributeValue(Attributes.ATTACK_DAMAGE);
         if (ramTarget.hurtServer(level, damageSource, damage)) {
            EnchantmentHelper.doPostAttackEffects(level, ramTarget, damageSource);
         }

         int movementSpeedLevel = body.hasEffect(MobEffects.SPEED) ? body.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
         int movementSlowdownLevel = body.hasEffect(MobEffects.SLOWNESS) ? body.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
         float speedBoostPower = 0.25F * (movementSpeedLevel - movementSlowdownLevel);
         float speedFactor = Mth.clamp(body.getSpeed() * 1.65F, 0.2F, 3.0F) + speedBoostPower;
         DamageSource source = level.damageSources().mobAttack(body);
         float blockedDamage = ramTarget.applyItemBlocking(level, source, damage);
         float blockingFactor = blockedDamage > 0.0F ? 0.5F : 1.0F;
         ramTarget.knockback(
            blockingFactor * speedFactor * this.getKnockbackForce.applyAsDouble(body), this.ramDirection.x(), this.ramDirection.z(), source, damage
         );
         this.finishRam(level, body);
         level.playSound(null, body, this.getImpactSound.apply(body), SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (this.hasRammedHornBreakingBlock(level, body)) {
         level.playSound(null, body, this.getImpactSound.apply(body), SoundSource.NEUTRAL, 1.0F, 1.0F);
         boolean dropped = body.dropHorn();
         if (dropped) {
            level.playSound(null, body, this.getHornBreakSound.apply(body), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }

         this.finishRam(level, body);
      } else {
         Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);
         Optional<Vec3> ramTarget = brain.getMemory(MemoryModuleType.RAM_TARGET);
         boolean lostOrReachedTarget = walkTarget.isEmpty()
            || ramTarget.isEmpty()
            || walkTarget.get().getTarget().currentPosition().closerThan(ramTarget.get(), 0.25);
         if (lostOrReachedTarget) {
            this.finishRam(level, body);
         }
      }
   }

   private boolean hasRammedHornBreakingBlock(final ServerLevel level, final Goat body) {
      Vec3 horizontalMovementNormalized = body.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
      BlockPos facingBlockPosition = BlockPos.containing(body.position().add(horizontalMovementNormalized));
      return level.getBlockState(facingBlockPosition).is(BlockTags.SNAPS_GOAT_HORN)
         || level.getBlockState(facingBlockPosition.above()).is(BlockTags.SNAPS_GOAT_HORN);
   }

   protected void finishRam(final ServerLevel level, final Goat body) {
      level.broadcastEntityEvent(body, (byte)59);
      body.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getTimeBetweenRams.apply(body).sample(level.getRandom()));
      body.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
   }
}
