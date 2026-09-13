package net.minecraft.world.entity.npc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
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
   public void tick(final ServerLevel level, final boolean spawnEnemies) {
      this.nextTick--;
      if (this.nextTick <= 0) {
         this.nextTick = 1200;
         Player player = level.getRandomPlayer();
         if (player != null) {
            RandomSource random = level.getRandom();
            int x = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
            int z = (8 + random.nextInt(24)) * (random.nextBoolean() ? -1 : 1);
            BlockPos spawnPos = player.blockPosition().offset(x, 0, z);
            int delta = 10;
            if (level.hasChunksAt(spawnPos.getX() - 10, spawnPos.getZ() - 10, spawnPos.getX() + 10, spawnPos.getZ() + 10)) {
               if (SpawnPlacements.isSpawnPositionOk(EntityTypes.CAT, level, spawnPos)) {
                  if (level.isCloseToVillage(spawnPos, 2)) {
                     this.spawnInVillage(level, spawnPos);
                  } else if (level.structureManager().getStructureWithPieceAt(spawnPos, StructureTags.CATS_SPAWN_IN).isValid()) {
                     this.spawnInHut(level, spawnPos);
                  }
               }
            }
         }
      }
   }

   private void spawnInVillage(final ServerLevel serverLevel, final BlockPos spawnPos) {
      int radius = 48;
      if (serverLevel.getPoiManager().getCountInRange(p -> p.is(PoiTypes.HOME), spawnPos, 48, PoiManager.Occupancy.IS_OCCUPIED) > 4L) {
         List<Cat> cats = serverLevel.getEntitiesOfClass(Cat.class, new AABB(spawnPos).inflate(48.0, 8.0, 48.0));
         if (cats.size() < 5) {
            this.spawnCat(spawnPos, serverLevel, false);
         }
      }
   }

   private void spawnInHut(final ServerLevel level, final BlockPos spawnPos) {
      int radius = 16;
      List<Cat> cats = level.getEntitiesOfClass(Cat.class, new AABB(spawnPos).inflate(16.0, 8.0, 16.0));
      if (cats.isEmpty()) {
         this.spawnCat(spawnPos, level, true);
      }
   }

   private void spawnCat(final BlockPos spawnPos, final ServerLevel level, final boolean makePersistent) {
      Cat cat = EntityTypes.CAT.create(level, EntitySpawnReason.NATURAL);
      if (cat != null) {
         cat.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), EntitySpawnReason.NATURAL, null);
         if (makePersistent) {
            cat.setPersistenceRequired();
         }

         cat.snapTo(spawnPos, 0.0F, 0.0F);
         level.addFreshEntityWithPassengers(cat);
      }
   }
}
