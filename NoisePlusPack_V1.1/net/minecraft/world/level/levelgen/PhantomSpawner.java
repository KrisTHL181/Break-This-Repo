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
import net.minecraft.world.entity.EntityType;
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
   public void tick(ServerLevel p_64576_, boolean p_64577_) {
      if (p_64577_ && p_64576_.getGameRules().get(GameRules.SPAWN_PHANTOMS)) {
         RandomSource randomsource = p_64576_.random;
         this.nextTick--;
         if (this.nextTick <= 0) {
            this.nextTick = this.nextTick + (60 + randomsource.nextInt(60)) * 20;
            if (p_64576_.getSkyDarken() >= 5 || !p_64576_.dimensionType().hasSkyLight()) {
               for (ServerPlayer serverplayer : p_64576_.players()) {
                  if (!serverplayer.isSpectator()) {
                     BlockPos blockpos = serverplayer.blockPosition();
                     if (!p_64576_.dimensionType().hasSkyLight() || blockpos.getY() >= p_64576_.getSeaLevel() && p_64576_.canSeeSky(blockpos)) {
                        DifficultyInstance difficultyinstance = p_64576_.getCurrentDifficultyAt(blockpos);
                        if (difficultyinstance.isHarderThan(randomsource.nextFloat() * 3.0F)) {
                           ServerStatsCounter serverstatscounter = serverplayer.getStats();
                           int i = Mth.clamp(serverstatscounter.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)), 1, Integer.MAX_VALUE);
                           int j = 24000;
                           if (randomsource.nextInt(i) >= 72000) {
                              BlockPos blockpos1 = blockpos.above(20 + randomsource.nextInt(15))
                                 .east(-10 + randomsource.nextInt(21))
                                 .south(-10 + randomsource.nextInt(21));
                              BlockState blockstate = p_64576_.getBlockState(blockpos1);
                              FluidState fluidstate = p_64576_.getFluidState(blockpos1);
                              if (NaturalSpawner.isValidEmptySpawnBlock(p_64576_, blockpos1, blockstate, fluidstate, EntityType.PHANTOM)) {
                                 SpawnGroupData spawngroupdata = null;
                                 int k = 1 + randomsource.nextInt(difficultyinstance.getDifficulty().getId() + 1);

                                 for (int l = 0; l < k; l++) {
                                    Phantom phantom = EntityType.PHANTOM.create(p_64576_, EntitySpawnReason.NATURAL);
                                    if (phantom != null) {
                                       phantom.snapTo(blockpos1, 0.0F, 0.0F);
                                       spawngroupdata = phantom.finalizeSpawn(p_64576_, difficultyinstance, EntitySpawnReason.NATURAL, spawngroupdata);
                                       p_64576_.addFreshEntityWithPassengers(phantom);
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
