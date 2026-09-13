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
   public Parched(EntityType<? extends AbstractSkeleton> p_453218_, Level p_451753_) {
      super(p_453218_, p_451753_);
   }

   @Override
   protected AbstractArrow getArrow(ItemStack p_452150_, float p_452862_, @Nullable ItemStack p_455038_) {
      AbstractArrow abstractarrow = super.getArrow(p_452150_, p_452862_, p_455038_);
      if (abstractarrow instanceof Arrow) {
         ((Arrow)abstractarrow).addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
      }

      return abstractarrow;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 16.0);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.PARCHED_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_452602_) {
      return SoundEvents.PARCHED_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.PARCHED_DEATH;
   }

   @Override
   SoundEvent getStepSound() {
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
   public boolean canBeAffected(MobEffectInstance p_459393_) {
      return p_459393_.getEffect() == MobEffects.WEAKNESS ? false : super.canBeAffected(p_459393_);
   }
}
