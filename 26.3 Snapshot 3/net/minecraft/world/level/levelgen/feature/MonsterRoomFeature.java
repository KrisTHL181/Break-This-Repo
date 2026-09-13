package net.minecraft.world.level.levelgen.feature;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.slf4j.Logger;

public record MonsterRoomFeature() implements Feature {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final EntityType<?>[] MOBS = new EntityType[]{EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.ZOMBIE, EntityTypes.SPIDER};
   private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
   public static final MapCodec<MonsterRoomFeature> CODEC = MapCodec.unit(MonsterRoomFeature::new);

   @Override
   public MapCodec<MonsterRoomFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      Predicate<BlockState> replaceableTag = s -> !s.is(BlockTags.FEATURES_CANNOT_REPLACE);
      int hr = 3;
      int xr = random.nextInt(2) + 2;
      int minX = -xr - 1;
      int maxX = xr + 1;
      int minY = -1;
      int maxY = 4;
      int zr = random.nextInt(2) + 2;
      int minZ = -zr - 1;
      int maxZ = zr + 1;
      int holeCount = 0;

      for (int dx = minX; dx <= maxX; dx++) {
         for (int dy = -1; dy <= 4; dy++) {
            for (int dz = minZ; dz <= maxZ; dz++) {
               BlockPos holePos = origin.offset(dx, dy, dz);
               boolean solid = level.getBlockState(holePos).isSolid();
               if (dy == -1 && !solid) {
                  return false;
               }

               if (dy == 4 && !solid) {
                  return false;
               }

               if ((dx == minX || dx == maxX || dz == minZ || dz == maxZ) && dy == 0 && level.isEmptyBlock(holePos) && level.isEmptyBlock(holePos.above())) {
                  holeCount++;
               }
            }
         }
      }

      if (holeCount >= 1 && holeCount <= 5) {
         for (int dx = minX; dx <= maxX; dx++) {
            for (int dy = 3; dy >= -1; dy--) {
               for (int dz = minZ; dz <= maxZ; dz++) {
                  BlockPos wallBlock = origin.offset(dx, dy, dz);
                  BlockState wallState = level.getBlockState(wallBlock);
                  if (dx == minX || dy == -1 || dz == minZ || dx == maxX || dy == 4 || dz == maxZ) {
                     if (wallBlock.getY() >= level.getMinY() && !level.getBlockState(wallBlock.below()).isSolid()) {
                        level.setBlock(wallBlock, AIR, 2);
                     } else if (wallState.isSolid() && !wallState.is(Blocks.CHEST)) {
                        if (dy == -1 && random.nextInt(4) != 0) {
                           this.safeSetBlock(level, wallBlock, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), replaceableTag);
                        } else {
                           this.safeSetBlock(level, wallBlock, Blocks.COBBLESTONE.defaultBlockState(), replaceableTag);
                        }
                     }
                  } else if (!wallState.is(Blocks.CHEST) && !wallState.is(Blocks.SPAWNER)) {
                     this.safeSetBlock(level, wallBlock, AIR, replaceableTag);
                  }
               }
            }
         }

         for (int cc = 0; cc < 2; cc++) {
            for (int i = 0; i < 3; i++) {
               int xc = origin.getX() + random.nextInt(xr * 2 + 1) - xr;
               int yc = origin.getY();
               int zc = origin.getZ() + random.nextInt(zr * 2 + 1) - zr;
               BlockPos chestPos = new BlockPos(xc, yc, zc);
               if (level.isEmptyBlock(chestPos)) {
                  int wallCount = 0;

                  for (Direction direction : Direction.Plane.HORIZONTAL) {
                     if (level.getBlockState(chestPos.relative(direction)).isSolid()) {
                        wallCount++;
                     }
                  }

                  if (wallCount == 1) {
                     this.safeSetBlock(level, chestPos, StructurePiece.reorient(level, chestPos, Blocks.CHEST.defaultBlockState()), replaceableTag);
                     RandomizableContainer.setBlockEntityLootTable(level, random, chestPos, BuiltInLootTables.SIMPLE_DUNGEON);
                     break;
                  }
               }
            }
         }

         this.safeSetBlock(level, origin, Blocks.SPAWNER.defaultBlockState(), replaceableTag);
         if (level.getBlockEntity(origin) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(this.randomEntityId(random), random);
         } else {
            LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", new Object[]{origin.getX(), origin.getY(), origin.getZ()});
         }

         return true;
      } else {
         return false;
      }
   }

   private EntityType<?> randomEntityId(final RandomSource random) {
      return Util.getRandom(MOBS, random);
   }
}
