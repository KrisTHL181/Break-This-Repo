package net.minecraft.world.entity.animal.fish;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class Cod extends AbstractSchoolingFish {
   public Cod(final EntityType<? extends Cod> type, final Level level) {
      super(type, level);
   }

   @Override
   public ItemStack getBucketItemStack() {
      return new ItemStack(Items.COD_BUCKET);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.COD_AMBIENT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.COD_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.COD_HURT;
   }

   @Override
   protected SoundEvent getFlopSound() {
      return SoundEvents.COD_FLOP;
   }
}
