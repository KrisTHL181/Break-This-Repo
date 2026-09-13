package net.minecraft.world.entity.animal.equine;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public class Donkey extends AbstractChestedHorse {
   public Donkey(final EntityType<? extends Donkey> type, final Level level) {
      super(type, level);
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
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.DONKEY_HURT;
   }

   @Override
   public boolean canMate(final Animal partner) {
      if (partner == this) {
         return false;
      } else {
         return !(partner instanceof Donkey) && !(partner instanceof Horse) ? false : this.canParent() && ((AbstractHorse)partner).canParent();
      }
   }

   @Override
   protected void playJumpSound() {
      this.playSound(SoundEvents.DONKEY_JUMP, 0.4F, 1.0F);
   }

   @Override
   public @Nullable AgeableMob getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      EntityType<? extends AbstractHorse> babyType = partner instanceof Horse ? EntityTypes.MULE : EntityTypes.DONKEY;
      AbstractHorse baby = babyType.create(level, EntitySpawnReason.BREEDING);
      if (baby != null) {
         this.setOffspringAttributes(partner, baby);
      }

      return baby;
   }
}
