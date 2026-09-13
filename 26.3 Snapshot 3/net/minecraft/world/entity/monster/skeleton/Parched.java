package net.minecraft.world.entity.monster.skeleton;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Parched extends AbstractSkeleton {
   public Parched(final EntityType<? extends AbstractSkeleton> type, final Level level) {
      super(type, level);
   }

   @Override
   protected AbstractArrow getArrow(final ItemStack projectile, final float power, final @Nullable ItemStack firingWeapon) {
      AbstractArrow arrow = super.getArrow(projectile, power, firingWeapon);
      if (arrow instanceof Arrow arrow2) {
         arrow2.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
      }

      return arrow;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 16.0);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.PARCHED_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.PARCHED_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PARCHED_DEATH;
   }

   @Override
   protected SoundEvent getStepSound() {
      return SoundEvents.PARCHED_STEP;
   }

   @Override
   protected int getHardAttackInterval() {
      return 50;
   }

   @Override
   protected int getAttackInterval() {
      return 70;
   }

   @Override
   public boolean canBeAffected(final MobEffectInstance newEffect) {
      return newEffect.getEffect() == MobEffects.WEAKNESS ? false : super.canBeAffected(newEffect);
   }
}
