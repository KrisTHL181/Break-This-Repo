package net.minecraft.world.entity.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class AgeableWaterCreature extends AgeableMob {
   protected AgeableWaterCreature(EntityType<? extends AgeableWaterCreature> p_367291_, Level p_361850_) {
      super(p_367291_, p_361850_);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_364787_) {
      return p_364787_.isUnobstructed(this);
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   public int getBaseExperienceReward(ServerLevel p_361071_) {
      return 1 + this.random.nextInt(3);
   }

   protected void handleAirSupply(int p_366101_) {
      if (this.isAlive() && !this.isInWater()) {
         this.setAirSupply(p_366101_ - 1);
         if (this.shouldTakeDrowningDamage()) {
            this.setAirSupply(0);
            this.hurt(this.damageSources().drown(), 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }
   }

   @Override
   public void baseTick() {
      int i = this.getAirSupply();
      super.baseTick();
      this.handleAirSupply(i);
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   public static boolean checkSurfaceAgeableWaterCreatureSpawnRules(
      EntityType<? extends AgeableWaterCreature> p_369363_, LevelAccessor p_370080_, EntitySpawnReason p_367384_, BlockPos p_370200_, RandomSource p_362509_
   ) {
      int i = p_370080_.getSeaLevel();
      int j = i - 13;
      return p_370200_.getY() >= j
         && p_370200_.getY() <= i
         && p_370080_.getFluidState(p_370200_.below()).is(FluidTags.WATER)
         && p_370080_.getBlockState(p_370200_.above()).is(Blocks.WATER);
   }
}
