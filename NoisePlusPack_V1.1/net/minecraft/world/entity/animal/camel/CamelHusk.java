package net.minecraft.world.entity.animal.camel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class CamelHusk extends Camel {
   public CamelHusk(EntityType<? extends Camel> p_458403_, Level p_457819_) {
      super(p_458403_, p_457819_);
   }

   @Override
   public boolean removeWhenFarAway(double p_452350_) {
      return true;
   }

   @Override
   public boolean isMobControlled() {
      return this.getFirstPassenger() instanceof Mob;
   }

   @Override
   public InteractionResult interact(Player p_454664_, InteractionHand p_459304_) {
      this.setPersistenceRequired();
      return super.interact(p_454664_, p_459304_);
   }

   @Override
   public boolean canBeLeashed() {
      return !this.isMobControlled();
   }

   @Override
   public boolean isFood(ItemStack p_450511_) {
      return p_450511_.is(ItemTags.CAMEL_HUSK_FOOD);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.CAMEL_HUSK_AMBIENT;
   }

   @Override
   public boolean canMate(Animal p_460463_) {
      return false;
   }

   @Override
   public @Nullable Camel getBreedOffspring(ServerLevel p_459539_, AgeableMob p_454635_) {
      return null;
   }

   @Override
   public boolean canFallInLove() {
      return false;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CAMEL_HUSK_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_457587_) {
      return SoundEvents.CAMEL_HUSK_HURT;
   }

   @Override
   protected void playStepSound(BlockPos p_452071_, BlockState p_456041_) {
      if (p_456041_.is(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS)) {
         this.playSound(SoundEvents.CAMEL_HUSK_STEP_SAND, 0.4F, 1.0F);
      } else {
         this.playSound(SoundEvents.CAMEL_HUSK_STEP, 0.4F, 1.0F);
      }
   }

   @Override
   protected SoundEvent getDashingSound() {
      return SoundEvents.CAMEL_HUSK_DASH;
   }

   @Override
   protected SoundEvent getDashReadySound() {
      return SoundEvents.CAMEL_HUSK_DASH_READY;
   }

   @Override
   protected SoundEvent getEatingSound() {
      return SoundEvents.CAMEL_HUSK_EAT;
   }

   @Override
   protected SoundEvent getStandUpSound() {
      return SoundEvents.CAMEL_HUSK_STAND;
   }

   @Override
   protected SoundEvent getSitDownSound() {
      return SoundEvents.CAMEL_HUSK_SIT;
   }

   @Override
   protected Holder.Reference<SoundEvent> getSaddleSound() {
      return SoundEvents.CAMEL_HUSK_SADDLE;
   }

   @Override
   public float chargeSpeedModifier() {
      return 4.0F;
   }
}
