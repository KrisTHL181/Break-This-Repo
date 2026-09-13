package net.minecraft.world.entity.npc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.phys.AABB;

public class CatSpawner implements CustomSpawner {
   private static final int TICK_DELAY = 1200;
   private int nextTick;

   @Override
   public void tick(ServerLevel p_35330_, boolean p_35331_) {
      this.nextTick--;
      if (this.nextTick <= 0) {
         this.nextTick = 1200;
         Player player = p_35330_.getRandomPlayer();
         if (player != null) {
            RandomSource randomsource = p_35330_.random;
            int i = (8 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
            int j = (8 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
            BlockPos blockpos = player.blockPosition().offset(i, 0, j);
            int k = 10;
            if (p_35330_.hasChunksAt(blockpos.getX() - 10, blockpos.getZ() - 10, blockpos.getX() + 10, blockpos.getZ() + 10)
               && SpawnPlacements.isSpawnPositionOk(EntityType.CAT, p_35330_, blockpos)) {
               if (p_35330_.isCloseToVillage(blockpos, 2)) {
                  this.spawnInVillage(p_35330_, blockpos);
               } else if (p_35330_.structureManager().getStructureWithPieceAt(blockpos, StructureTags.CATS_SPAWN_IN).isValid()) {
                  this.spawnInHut(p_35330_, blockpos);
               }
            }
         }
      }
   }

   private void spawnInVillage(ServerLevel p_35327_, BlockPos p_35328_) {
      int i = 48;
      if (p_35327_.getPoiManager().getCountInRange(p_219610_ -> p_219610_.is(PoiTypes.HOME), p_35328_, 48, PoiManager.Occupancy.IS_OCCUPIED) > 4L) {
         List<Cat> list = p_35327_.getEntitiesOfClass(Cat.class, new AABB(p_35328_).inflate(48.0, 8.0, 48.0));
         if (list.size() < 5) {
            this.spawnCat(p_35328_, p_35327_, false);
         }
      }
   }

   private void spawnInHut(ServerLevel p_35337_, BlockPos p_35338_) {
      int i = 16;
      List<Cat> list = p_35337_.getEntitiesOfClass(Cat.class, new AABB(p_35338_).inflate(16.0, 8.0, 16.0));
      if (list.isEmpty()) {
         this.spawnCat(p_35338_, p_35337_, true);
      }
   }

   private void spawnCat(BlockPos p_35334_, ServerLevel p_35335_, boolean p_392522_) {
      Cat cat = EntityType.CAT.create(p_35335_, EntitySpawnReason.NATURAL);
      if (cat != null) {
         cat.finalizeSpawn(p_35335_, p_35335_.getCurrentDifficultyAt(p_35334_), EntitySpawnReason.NATURAL, null);
         if (p_392522_) {
            cat.setPersistenceRequired();
         }

         cat.snapTo(p_35334_, 0.0F, 0.0F);
         p_35335_.addFreshEntityWithPassengers(cat);
      }
   }
}
