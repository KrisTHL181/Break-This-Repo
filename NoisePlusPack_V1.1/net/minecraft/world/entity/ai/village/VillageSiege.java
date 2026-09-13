package net.minecraft.world.entity.ai.village;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class VillageSiege implements CustomSpawner {
   private static final Logger LOGGER = LogUtils.getLogger();
   private boolean hasSetupSiege;
   private VillageSiege.State siegeState = VillageSiege.State.SIEGE_DONE;
   private int zombiesToSpawn;
   private int nextSpawnTime;
   private int spawnX;
   private int spawnY;
   private int spawnZ;

   @Override
   public void tick(ServerLevel p_27013_, boolean p_27014_) {
      if (!p_27013_.isBrightOutside() && p_27014_) {
         long i = p_27013_.getDayTime() % 24000L;
         if (i == 18000L) {
            this.siegeState = p_27013_.random.nextInt(10) == 0 ? VillageSiege.State.SIEGE_TONIGHT : VillageSiege.State.SIEGE_DONE;
         }

         if (this.siegeState != VillageSiege.State.SIEGE_DONE) {
            if (!this.hasSetupSiege) {
               if (!this.tryToSetupSiege(p_27013_)) {
                  return;
               }

               this.hasSetupSiege = true;
            }

            if (this.nextSpawnTime > 0) {
               this.nextSpawnTime--;
            } else {
               this.nextSpawnTime = 2;
               if (this.zombiesToSpawn > 0) {
                  this.trySpawn(p_27013_);
                  this.zombiesToSpawn--;
               } else {
                  this.siegeState = VillageSiege.State.SIEGE_DONE;
               }
            }
         }
      } else {
         this.siegeState = VillageSiege.State.SIEGE_DONE;
         this.hasSetupSiege = false;
      }
   }

   private boolean tryToSetupSiege(ServerLevel p_27008_) {
      for (Player player : p_27008_.players()) {
         if (!player.isSpectator()) {
            BlockPos blockpos = player.blockPosition();
            if (p_27008_.isVillage(blockpos) && !p_27008_.getBiome(blockpos).is(BiomeTags.WITHOUT_ZOMBIE_SIEGES)) {
               for (int i = 0; i < 10; i++) {
                  float f = p_27008_.random.nextFloat() * (float) (Math.PI * 2);
                  this.spawnX = blockpos.getX() + Mth.floor(Mth.cos(f) * 32.0F);
                  this.spawnY = blockpos.getY();
                  this.spawnZ = blockpos.getZ() + Mth.floor(Mth.sin(f) * 32.0F);
                  if (this.findRandomSpawnPos(p_27008_, new BlockPos(this.spawnX, this.spawnY, this.spawnZ)) != null) {
                     this.nextSpawnTime = 0;
                     this.zombiesToSpawn = 20;
                     break;
                  }
               }

               return true;
            }
         }
      }

      return false;
   }

   private void trySpawn(ServerLevel p_27017_) {
      Vec3 vec3 = this.findRandomSpawnPos(p_27017_, new BlockPos(this.spawnX, this.spawnY, this.spawnZ));
      if (vec3 != null) {
         Zombie zombie;
         try {
            zombie = new Zombie(p_27017_);
            zombie.finalizeSpawn(p_27017_, p_27017_.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.EVENT, null);
         } catch (Exception exception) {
            LOGGER.warn("Failed to create zombie for village siege at {}", vec3, exception);
            return;
         }

         zombie.snapTo(vec3.x, vec3.y, vec3.z, p_27017_.random.nextFloat() * 360.0F, 0.0F);
         p_27017_.addFreshEntityWithPassengers(zombie);
      }
   }

   private @Nullable Vec3 findRandomSpawnPos(ServerLevel p_27010_, BlockPos p_27011_) {
      for (int i = 0; i < 10; i++) {
         int j = p_27011_.getX() + p_27010_.random.nextInt(16) - 8;
         int k = p_27011_.getZ() + p_27010_.random.nextInt(16) - 8;
         int l = p_27010_.getHeight(Heightmap.Types.WORLD_SURFACE, j, k);
         BlockPos blockpos = new BlockPos(j, l, k);
         if (p_27010_.isVillage(blockpos) && Monster.checkMonsterSpawnRules(EntityType.ZOMBIE, p_27010_, EntitySpawnReason.EVENT, blockpos, p_27010_.random)) {
            return Vec3.atBottomCenterOf(blockpos);
         }
      }

      return null;
   }

   enum State {
      SIEGE_CAN_ACTIVATE,
      SIEGE_TONIGHT,
      SIEGE_DONE;
   }
}
