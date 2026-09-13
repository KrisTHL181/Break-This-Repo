package net.minecraft.world.entity.animal.golem;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public abstract class AbstractGolem extends PathfinderMob {
   protected AbstractGolem(EntityType<? extends AbstractGolem> p_456706_, Level p_451466_) {
      super(p_456706_, p_451466_);
   }

   @Override
   protected @Nullable SoundEvent getAmbientSound() {
      return null;
   }

   @Override
   protected @Nullable SoundEvent getHurtSound(DamageSource p_452305_) {
      return null;
   }

   @Override
   protected @Nullable SoundEvent getDeathSound() {
      return null;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   public boolean removeWhenFarAway(double p_457896_) {
      return false;
   }
}
