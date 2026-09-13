package net.minecraft.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

public class PatrolSpawner implements CustomSpawner {
   private int nextTick;

   @Override
   public void tick(final ServerLevel level, final boolean spawnEnemies) {
      if (spawnEnemies) {
         if (level.getGameRules().get(GameRules.SPAWN_PATROLS)) {
            RandomSource random = level.getRandom();
            this.nextTick--;
            if (this.nextTick <= 0) {
               this.nextTick = this.nextTick + 12000 + random.nextInt(1200);
               if (level.isBrightOutside()) {
                  if (random.nextInt(5) == 0) {
                     int playerCount = level.players().size();
                     if (playerCount >= 1) {
                        Player player = level.players().get(random.nextInt(playerCount));
                        if (!player.isSpectator()) {
                           if (!level.isCloseToVillage(player.blockPosition(), 2)) {
                              int x = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                              int z = (24 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
                              BlockPos.MutableBlockPos spawnPos = player.blockPosition().mutable().move(x, 0, z);
                              int delta = 10;
                              if (level.hasChunksAt(spawnPos.getX() - 10, spawnPos.getZ() - 10, spawnPos.getX() + 10, spawnPos.getZ() + 10)) {
                                 if (level.environmentAttributes().getValue(EnvironmentAttributes.CAN_PILLAGER_PATROL_SPAWN, spawnPos)) {
                                    int groupSize = (int)Math.ceil(level.getCurrentDifficultyAt(spawnPos).getEffectiveDifficulty()) + 1;

                                    for (int i = 0; i < groupSize; i++) {
                                       spawnPos.setY(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnPos).getY());
                                       if (i == 0) {
                                          if (!this.spawnPatrolMember(level, spawnPos, random, true)) {
                                             break;
                                          }
                                       } else {
                                          this.spawnPatrolMember(level, spawnPos, random, false);
                                       }

                                       spawnPos.setX(spawnPos.getX() + random.nextInt(5) - random.nextInt(5));
                                       spawnPos.setZ(spawnPos.getZ() + random.nextInt(5) - random.nextInt(5));
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

   private boolean spawnPatrolMember(final ServerLevel level, final BlockPos pos, final RandomSource random, final boolean isLeader) {
      BlockState state = level.getBlockState(pos);
      if (!NaturalSpawner.isValidEmptySpawnBlock(level, pos, state, state.getFluidState(), EntityTypes.PILLAGER)) {
         return false;
      }

      if (!PatrollingMonster.checkPatrollingMonsterSpawnRules(EntityTypes.PILLAGER, level, EntitySpawnReason.PATROL, pos, random)) {
         return false;
      }

      PatrollingMonster mob = EntityTypes.PILLAGER.create(level, EntitySpawnReason.PATROL);
      if (mob != null) {
         if (isLeader) {
            mob.setPatrolLeader(true);
            mob.findPatrolTarget();
         }

         mob.setPos(pos.getX(), pos.getY(), pos.getZ());
         mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.PATROL, null);
         level.addFreshEntityWithPassengers(mob);
         return true;
      } else {
         return false;
      }
   }
}
