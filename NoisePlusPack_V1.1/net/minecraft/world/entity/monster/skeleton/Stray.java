package net.minecraft.world.entity.monster.skeleton;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public class Stray extends AbstractSkeleton {
   public Stray(EntityType<? extends Stray> p_455263_, Level p_458013_) {
      super(p_455263_, p_458013_);
   }

   public static boolean checkStraySpawnRules(
      EntityType<Stray> p_456984_, ServerLevelAccessor p_453955_, EntitySpawnReason p_460530_, BlockPos p_453169_, RandomSource p_460177_
   ) {
      BlockPos blockpos = p_453169_;

      do {
         blockpos = blockpos.above();
      } while (p_453955_.getBlockState(blockpos).is(Blocks.POWDER_SNOW));

      return Monster.checkMonsterSpawnRules(p_456984_, p_453955_, p_460530_, p_453169_, p_460177_)
         && (EntitySpawnReason.isSpawner(p_460530_) || p_453955_.canSeeSky(blockpos.below()));
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.STRAY_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource p_456165_) {
      return SoundEvents.STRAY_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.STRAY_DEATH;
   }

   @Override
   SoundEvent getStepSound() {
      return SoundEvents.STRAY_STEP;
   }

   @Override
   protected AbstractArrow getArrow(ItemStack p_452268_, float p_452804_, @Nullable ItemStack p_459386_) {
      AbstractArrow abstractarrow = super.getArrow(p_452268_, p_452804_, p_459386_);
      if (abstractarrow instanceof Arrow) {
         ((Arrow)abstractarrow).addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600));
      }

      return abstractarrow;
   }
}
