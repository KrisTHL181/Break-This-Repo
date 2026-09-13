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

   protected WaterAnimal(final EntityType<? extends WaterAnimal> type, final Level level) {
      super(type, level);
      this.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   public boolean checkSpawnObstruction(final LevelReader level) {
      return level.isUnobstructed(this);
   }

   @Override
   public int getAmbientSoundInterval() {
      return 120;
   }

   @Override
   protected int getBaseExperienceReward(final ServerLevel level) {
      return 1 + this.random.nextInt(3);
   }

   protected void handleAirSupply(final ServerLevel level, final int preTickAirSupply) {
      if (this.isAlive() && !this.isInWater()) {
         this.setAirSupply(preTickAirSupply - 1);
         if (this.shouldTakeDrowningDamage()) {
            this.setAirSupply(0);
            this.hurtServer(level, this.damageSources().drown(), 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }
   }

   @Override
   public void baseTick() {
      int airSupply = this.getAirSupply();
      super.baseTick();
      if (this.level() instanceof ServerLevel serverLevel) {
         this.handleAirSupply(serverLevel, airSupply);
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
      final EntityType<? extends WaterAnimal> type,
      final LevelAccessor level,
      final EntitySpawnReason spawnReason,
      final BlockPos pos,
      final RandomSource random
   ) {
      int seaLevel = level.getSeaLevel();
      int minSpawnLevel = seaLevel - 13;
      return pos.getY() >= minSpawnLevel
         && pos.getY() <= seaLevel
         && level.getFluidState(pos.below()).is(FluidTags.WATER)
         && level.getBlockState(pos.above()).is(Blocks.WATER);
   }
}
