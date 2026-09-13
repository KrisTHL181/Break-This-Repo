package net.minecraft.world.entity.animal.equine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Donkey extends AbstractChestedHorse {
   public Donkey(EntityType<? extends Donkey> p_460688_, Level p_458540_) {
      super(p_460688_, p_458540_);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.DONKEY_AMBIENT;
   }

   @Override
   protected SoundEvent getAngrySound() {
      return SoundEvents.DONKEY_ANGRY;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.DONKEY_DEATH;
   }

   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.DONKEY_EAT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_453353_) {
      return SoundEvents.DONKEY_HURT;
   }

   @Override
   public boolean canMate(Animal p_450526_) {
      if (p_450526_ == this) {
         return false;
      } else {
         return !(p_450526_ instanceof Donkey) && !(p_450526_ instanceof Horse) ? false : this.canParent() && ((AbstractHorse)p_450526_).canParent();
      }
   }

   @Override
   protected void playJumpSound() {
      this.playSound(SoundEvents.DONKEY_JUMP, 0.4F, 1.0F);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(ServerLevel p_453177_, AgeableMob p_459537_) {
      EntityType<? extends AbstractHorse> entitytype = p_459537_ instanceof Horse ? EntityType.MULE : EntityType.DONKEY;
      AbstractHorse abstracthorse = entitytype.create(p_453177_, EntitySpawnReason.BREEDING);
      if (abstracthorse != null) {
         this.setOffspringAttributes(p_459537_, abstracthorse);
      }

      return abstracthorse;
   }
}
