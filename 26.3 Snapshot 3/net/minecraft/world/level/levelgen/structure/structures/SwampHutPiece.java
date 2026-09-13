package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class SwampHutPiece extends ScatteredFeaturePiece {
   private boolean spawnedWitch;
   private boolean spawnedCat;

   public SwampHutPiece(final RandomSource random, final int west, final int north) {
      super(StructurePieceType.SWAMPLAND_HUT, west, 64, north, 7, 7, 9, getRandomHorizontalDirection(random));
   }

   public SwampHutPiece(final CompoundTag tag) {
      super(StructurePieceType.SWAMPLAND_HUT, tag);
      this.spawnedWitch = tag.getBooleanOr("Witch", false);
      this.spawnedCat = tag.getBooleanOr("Cat", false);
   }

   @Override
   protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
      super.addAdditionalSaveData(context, tag);
      tag.putBoolean("Witch", this.spawnedWitch);
      tag.putBoolean("Cat", this.spawnedCat);
   }

   @Override
   public void postProcess(
      final WorldGenLevel level,
      final StructureManager structureManager,
      final ChunkGenerator generator,
      final RandomSource random,
      final BoundingBox chunkBB,
      final ChunkPos chunkPos,
      final BlockPos referencePos
   ) {
      if (this.updateAverageGroundHeight(level, chunkBB, 0)) {
         this.generateBox(level, chunkBB, 1, 1, 1, 5, 1, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 4, 2, 5, 4, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 1, 0, 4, 1, 0, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 2, 2, 3, 3, 2, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 2, 3, 1, 3, 6, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 2, 3, 5, 3, 6, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 2, 7, 4, 3, 7, Blocks.SPRUCE_PLANKS.defaultBlockState(), Blocks.SPRUCE_PLANKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 0, 2, 1, 3, 2, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 0, 2, 5, 3, 2, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 0, 7, 1, 3, 7, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 0, 7, 5, 3, 7, Blocks.OAK_LOG.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(), false);
         this.placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 2, 3, 2, chunkBB);
         this.placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 3, 3, 7, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 1, 3, 4, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 5, 3, 4, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 5, 3, 5, chunkBB);
         this.placeBlock(level, Blocks.POTTED_RED_MUSHROOM.defaultBlockState(), 1, 3, 5, chunkBB);
         this.placeBlock(level, Blocks.CRAFTING_TABLE.defaultBlockState(), 3, 2, 6, chunkBB);
         this.placeBlock(level, Blocks.CAULDRON.defaultBlockState(), 4, 2, 6, chunkBB);
         this.placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 1, 2, 1, chunkBB);
         this.placeBlock(level, Blocks.OAK_FENCE.defaultBlockState(), 5, 2, 1, chunkBB);
         BlockState northStairs = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         BlockState eastStairs = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState westStairs = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         BlockState southStairs = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         this.generateBox(level, chunkBB, 0, 4, 1, 6, 4, 1, northStairs, northStairs, false);
         this.generateBox(level, chunkBB, 0, 4, 2, 0, 4, 7, eastStairs, eastStairs, false);
         this.generateBox(level, chunkBB, 6, 4, 2, 6, 4, 7, westStairs, westStairs, false);
         this.generateBox(level, chunkBB, 0, 4, 8, 6, 4, 8, southStairs, southStairs, false);
         this.placeBlock(level, northStairs.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 0, 4, 1, chunkBB);
         this.placeBlock(level, northStairs.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 6, 4, 1, chunkBB);
         this.placeBlock(level, southStairs.setValue(StairBlock.SHAPE, StairsShape.OUTER_LEFT), 0, 4, 8, chunkBB);
         this.placeBlock(level, southStairs.setValue(StairBlock.SHAPE, StairsShape.OUTER_RIGHT), 6, 4, 8, chunkBB);

         for (int z = 2; z <= 7; z += 5) {
            for (int x = 1; x <= 5; x += 4) {
               this.fillColumnDown(level, Blocks.OAK_LOG.defaultBlockState(), x, -1, z, chunkBB);
            }
         }

         if (!this.spawnedWitch) {
            BlockPos pos = this.getWorldPos(2, 2, 5);
            if (chunkBB.isInside(pos)) {
               this.spawnedWitch = true;
               Witch witch = EntityTypes.WITCH.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
               if (witch != null) {
                  witch.setPersistenceRequired();
                  witch.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
                  witch.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
                  level.addFreshEntityWithPassengers(witch);
               }
            }
         }

         this.spawnCat(level, chunkBB);
      }
   }

   private void spawnCat(final ServerLevelAccessor level, final BoundingBox chunkBB) {
      if (!this.spawnedCat) {
         BlockPos pos = this.getWorldPos(2, 2, 5);
         if (chunkBB.isInside(pos)) {
            this.spawnedCat = true;
            Cat cat = EntityTypes.CAT.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (cat != null) {
               cat.setPersistenceRequired();
               cat.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
               cat.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
               level.addFreshEntityWithPassengers(cat);
            }
         }
      }
   }
}
