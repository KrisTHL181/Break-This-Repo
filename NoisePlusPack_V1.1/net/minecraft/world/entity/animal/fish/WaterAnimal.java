package net.minecraft.world.entity.animal.fish;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;

public abstract class WaterAnimal extends PathfinderMob {
   public static final int AMBIENT_SOUND_INTERVAL = 120;

   protected WaterAnimal(EntityType<? extends WaterAnimal> p_458277_, Level p_455938_) {
      super(p_458277_, p_455938_);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   public boolean checkSpawnObstruction(LevelReader p_455089_) {
      return p_455089_.isUnobstructed(this);
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   protected int getBaseExperienceReward(ServerLevel p_458194_) {
      return 1 + this.random.nextInt(3);
   }

   protected void handleAirSupply(ServerLevel p_456767_, int p_455644_) {
      if (this.isAlive() && !this.isInWater()) {
         this.setAirSupply(p_455644_ - 1);
         if (this.shouldTakeDrowningDamage()) {
            this.setAirSupply(0);
            this.hurtServer(p_456767_, this.damageSources().drown(), 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }
   }

   @Override
   public void baseTick() {
      int i = this.getAirSupply();
      super.baseTick();
      if (this.level() instanceof ServerLevel serverlevel) {
         this.handleAirSupply(serverlevel, i);
      }
   }

   @Override
   public boolean isPushedByFluid() {
      return false;
   }

   @Override
   public boolean canBeLeashed() {
      return false;
   }

   public static boolean checkSurfaceWaterAnimalSpawnRules(
      EntityType<? extends WaterAnimal> p_458230_, LevelAccessor p_455660_, EntitySpawnReason p_450245_, BlockPos p_455224_, RandomSource p_457959_
   ) {
      int i = p_455660_.getSeaLevel();
      int j = i - 13;
      return p_455224_.getY() >= j
         && p_455224_.getY() <= i
         && p_455660_.getFluidState(p_455224_.below()).is(FluidTags.WATER)
         && p_455660_.getBlockState(p_455224_.above()).is(Blocks.WATER);
   }
}
