package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;

public class PhantomSpawner implements CustomSpawner {
   private int nextTick;

   @Override
   public void tick(final ServerLevel level, final boolean spawnEnemies) {
      if (spawnEnemies) {
         if (level.getGameRules().get(GameRules.SPAWN_PHANTOMS)) {
            RandomSource random = level.getRandom();
            this.nextTick--;
            if (this.nextTick <= 0) {
               this.nextTick = this.nextTick + (60 + random.nextInt(60)) * 20;
               if (level.getSkyDarken() >= 5 || !level.dimensionType().hasSkyLight()) {
                  for (ServerPlayer player : level.players()) {
                     if (!player.isSpectator()) {
                        BlockPos playerPos = player.blockPosition();
                        if (!level.dimensionType().hasSkyLight() || playerPos.getY() >= level.getSeaLevel() && level.canSeeSky(playerPos)) {
                           DifficultyInstance difficulty = level.getCurrentDifficultyAt(playerPos);
                           if (difficulty.isHarderThan(random.nextFloat() * 3.0F)) {
                              ServerStatsCounter stats = player.getStats();
                              int value = Mth.clamp(stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                              int dayLength = 24000;
                              if (random.nextInt(value) >= 72000) {
                                 BlockPos spawnPos = playerPos.above(20 + random.nextInt(15)).east(-10 + random.nextInt(21)).south(-10 + random.nextInt(21));
                                 BlockState blockState = level.getBlockState(spawnPos);
                                 FluidState fluidState = level.getFluidState(spawnPos);
                                 if (NaturalSpawner.isValidEmptySpawnBlock(level, spawnPos, blockState, fluidState, EntityTypes.PHANTOM)) {
                                    SpawnGroupData groupData = null;
                                    int groupSize = 1 + random.nextInt(difficulty.getDifficulty().getId() + 1);

                                    for (int i = 0; i < groupSize; i++) {
                                       Phantom phantom = EntityTypes.PHANTOM.create(level, EntitySpawnReason.NATURAL);
                                       if (phantom != null) {
                                          phantom.snapTo(spawnPos, 0.0F, 0.0F);
                                          groupData = phantom.finalizeSpawn(level, difficulty, EntitySpawnReason.NATURAL, groupData);
                                          level.addFreshEntityWithPassengers(phantom);
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
