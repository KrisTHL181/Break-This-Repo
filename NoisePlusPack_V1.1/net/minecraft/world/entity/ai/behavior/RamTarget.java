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
      Function<Goat, UniformInt> p_217342_,
      TargetingConditions p_217343_,
      float p_217344_,
      ToDoubleFunction<Goat> p_217345_,
      Function<Goat, SoundEvent> p_217346_,
      Function<Goat, SoundEvent> p_217347_
   ) {
      super(ImmutableMap.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_PRESENT), 200);
      this.getTimeBetweenRams = p_217342_;
      this.ramTargeting = p_217343_;
      this.speed = p_217344_;
      this.getKnockbackForce = p_217345_;
      this.getImpactSound = p_217346_;
      this.getHornBreakSound = p_217347_;
      this.ramDirection = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(ServerLevel p_217349_, Goat p_217350_) {
      return p_217350_.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected boolean canStillUse(ServerLevel p_217352_, Goat p_217353_, long p_217354_) {
      return p_217353_.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected void start(ServerLevel p_217359_, Goat p_217360_, long p_217361_) {
      BlockPos blockpos = p_217360_.blockPosition();
      Brain<?> brain = p_217360_.getBrain();
      Vec3 vec3 = brain.getMemory(MemoryModuleType.RAM_TARGET).get();
      this.ramDirection = new Vec3(blockpos.getX() - vec3.x(), 0.0, blockpos.getZ() - vec3.z()).normalize();
      brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(vec3, this.speed, 0));
   }

   protected void tick(ServerLevel p_217366_, Goat p_217367_, long p_217368_) {
      List<LivingEntity> list = p_217366_.getNearbyEntities(LivingEntity.class, this.ramTargeting, p_217367_, p_217367_.getBoundingBox());
      Brain<?> brain = p_217367_.getBrain();
      if (!list.isEmpty()) {
         LivingEntity livingentity = list.get(0);
         DamageSource damagesource = p_217366_.damageSources().noAggroMobAttack(p_217367_);
         float f = (float)p_217367_.getAttributeValue(Attributes.ATTACK_DAMAGE);
         if (livingentity.hurtServer(p_217366_, damagesource, f)) {
            EnchantmentHelper.doPostAttackEffects(p_217366_, livingentity, damagesource);
         }

         int i = p_217367_.hasEffect(MobEffects.SPEED) ? p_217367_.getEffect(MobEffects.SPEED).getAmplifier() + 1 : 0;
         int j = p_217367_.hasEffect(MobEffects.SLOWNESS) ? p_217367_.getEffect(MobEffects.SLOWNESS).getAmplifier() + 1 : 0;
         float f1 = 0.25F * (i - j);
         float f2 = Mth.clamp(p_217367_.getSpeed() * 1.65F, 0.2F, 3.0F) + f1;
         DamageSource damagesource1 = p_217366_.damageSources().mobAttack(p_217367_);
         float f3 = livingentity.applyItemBlocking(p_217366_, damagesource1, f);
         float f4 = f3 > 0.0F ? 0.5F : 1.0F;
         livingentity.knockback(f4 * f2 * this.getKnockbackForce.applyAsDouble(p_217367_), this.ramDirection.x(), this.ramDirection.z());
         this.finishRam(p_217366_, p_217367_);
         p_217366_.playSound(null, p_217367_, this.getImpactSound.apply(p_217367_), SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (this.hasRammedHornBreakingBlock(p_217366_, p_217367_)) {
         p_217366_.playSound(null, p_217367_, this.getImpactSound.apply(p_217367_), SoundSource.NEUTRAL, 1.0F, 1.0F);
         boolean flag = p_217367_.dropHorn();
         if (flag) {
            p_217366_.playSound(null, p_217367_, this.getHornBreakSound.apply(p_217367_), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }

         this.finishRam(p_217366_, p_217367_);
      } else {
         Optional<WalkTarget> optional = brain.getMemory(MemoryModuleType.WALK_TARGET);
         Optional<Vec3> optional1 = brain.getMemory(MemoryModuleType.RAM_TARGET);
         boolean flag1 = optional.isEmpty() || optional1.isEmpty() || optional.get().getTarget().currentPosition().closerThan(optional1.get(), 0.25);
         if (flag1) {
            this.finishRam(p_217366_, p_217367_);
         }
      }
   }

   private boolean hasRammedHornBreakingBlock(ServerLevel p_217363_, Goat p_217364_) {
      Vec3 vec3 = p_217364_.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
      BlockPos blockpos = BlockPos.containing(p_217364_.position().add(vec3));
      return p_217363_.getBlockState(blockpos).is(BlockTags.SNAPS_GOAT_HORN) || p_217363_.getBlockState(blockpos.above()).is(BlockTags.SNAPS_GOAT_HORN);
   }

   protected void finishRam(ServerLevel p_217356_, Goat p_217357_) {
      p_217356_.broadcastEntityEvent(p_217357_, (byte)59);
      p_217357_.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getTimeBetweenRams.apply(p_217357_).sample(p_217356_.random));
      p_217357_.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
   }
}
