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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class CamelHusk extends Camel {
   public CamelHusk(final EntityType<? extends Camel> type, final Level level) {
      super(type, level);
   }

   @Override
   public boolean removeWhenFarAway(final double distSqr) {
      return true;
   }

   @Override
   public boolean isMobControlled() {
      return this.getFirstPassenger() instanceof Mob;
   }

   @Override
   public InteractionResult interact(final Player player, final InteractionHand hand, final Vec3 location) {
      this.setPersistenceRequired();
      return super.interact(player, hand, location);
   }

   @Override
   public boolean canBeLeashed() {
      return !this.isMobControlled();
   }

   @Override
   public boolean isFood(final ItemStack itemStack) {
      return itemStack.is(ItemTags.CAMEL_HUSK_FOOD);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.CAMEL_HUSK_AMBIENT;
   }

   @Override
   public boolean canMate(final Animal partner) {
      return false;
   }

   @Override
   public @Nullable Camel getBreedOffspring(final ServerLevel level, final AgeableMob partner) {
      return null;
   }

   @Override
   public boolean canFallInLove() {
      return false;
   }

   @Override
   public boolean canBeABaby() {
      return false;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.CAMEL_HUSK_DEATH;
   }

   @Override
   protected SoundEvent getHurtSound(final DamageSource source) {
      return SoundEvents.CAMEL_HUSK_HURT;
   }

   @Override
   protected void playStepSound(final BlockPos pos, final BlockState blockState) {
      if (blockState.is(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS)) {
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
