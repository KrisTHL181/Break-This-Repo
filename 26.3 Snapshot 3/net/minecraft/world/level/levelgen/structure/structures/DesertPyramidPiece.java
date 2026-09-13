package net.minecraft.world.level.levelgen.structure.structures;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidPiece extends ScatteredFeaturePiece {
   public static final int WIDTH = 21;
   public static final int DEPTH = 21;
   private final boolean[] hasPlacedChest = new boolean[4];
   private final List<BlockPos> potentialSuspiciousSandWorldPositions = new ArrayList<>();
   private BlockPos randomCollapsedRoofPos = BlockPos.ZERO;

   public DesertPyramidPiece(final RandomSource random, final int west, final int north) {
      super(StructurePieceType.DESERT_PYRAMID_PIECE, west, 64, north, 21, 15, 21, getRandomHorizontalDirection(random));
   }

   public DesertPyramidPiece(final CompoundTag tag) {
      super(StructurePieceType.DESERT_PYRAMID_PIECE, tag);
      this.hasPlacedChest[0] = tag.getBooleanOr("hasPlacedChest0", false);
      this.hasPlacedChest[1] = tag.getBooleanOr("hasPlacedChest1", false);
      this.hasPlacedChest[2] = tag.getBooleanOr("hasPlacedChest2", false);
      this.hasPlacedChest[3] = tag.getBooleanOr("hasPlacedChest3", false);
   }

   @Override
   protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
      super.addAdditionalSaveData(context, tag);
      tag.putBoolean("hasPlacedChest0", this.hasPlacedChest[0]);
      tag.putBoolean("hasPlacedChest1", this.hasPlacedChest[1]);
      tag.putBoolean("hasPlacedChest2", this.hasPlacedChest[2]);
      tag.putBoolean("hasPlacedChest3", this.hasPlacedChest[3]);
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
      if (this.updateHeightPositionToLowestGroundHeight(level, -random.nextInt(3))) {
         this.generateBox(
            level, chunkBB, 0, -4, 0, this.width - 1, 0, this.depth - 1, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );

         for (int pos = 1; pos <= 9; pos++) {
            this.generateBox(
               level,
               chunkBB,
               pos,
               pos,
               pos,
               this.width - 1 - pos,
               pos,
               this.depth - 1 - pos,
               Blocks.SANDSTONE.defaultBlockState(),
               Blocks.SANDSTONE.defaultBlockState(),
               false
            );
            this.generateBox(
               level,
               chunkBB,
               pos + 1,
               pos,
               pos + 1,
               this.width - 2 - pos,
               pos,
               this.depth - 2 - pos,
               Blocks.AIR.defaultBlockState(),
               Blocks.AIR.defaultBlockState(),
               false
            );
         }

         for (int x = 0; x < this.width; x++) {
            for (int z = 0; z < this.depth; z++) {
               int startY = -5;
               this.fillColumnDown(level, Blocks.SANDSTONE.defaultBlockState(), x, -5, z, chunkBB);
            }
         }

         BlockState northStairs = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         BlockState southStairs = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState eastStairs = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState westStairs = Blocks.SANDSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 10, 1, 3, 10, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.placeBlock(level, northStairs, 2, 10, 0, chunkBB);
         this.placeBlock(level, southStairs, 2, 10, 4, chunkBB);
         this.placeBlock(level, eastStairs, 0, 10, 2, chunkBB);
         this.placeBlock(level, westStairs, 4, 10, 2, chunkBB);
         this.generateBox(
            level, chunkBB, this.width - 5, 0, 0, this.width - 1, 9, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false
         );
         this.generateBox(
            level, chunkBB, this.width - 4, 10, 1, this.width - 2, 10, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.placeBlock(level, northStairs, this.width - 3, 10, 0, chunkBB);
         this.placeBlock(level, southStairs, this.width - 3, 10, 4, chunkBB);
         this.placeBlock(level, eastStairs, this.width - 5, 10, 2, chunkBB);
         this.placeBlock(level, westStairs, this.width - 1, 10, 2, chunkBB);
         this.generateBox(level, chunkBB, 8, 0, 0, 12, 4, 4, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 9, 1, 0, 11, 3, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 1, 1, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 2, 1, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 9, 3, 1, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, 3, 1, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 3, 1, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 2, 1, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 11, 1, 1, chunkBB);
         this.generateBox(level, chunkBB, 4, 1, 1, 8, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 1, 2, 8, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 12, 1, 1, 16, 3, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 12, 1, 2, 16, 2, 2, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(
            level, chunkBB, 5, 4, 5, this.width - 6, 4, this.depth - 6, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(level, chunkBB, 9, 4, 9, 11, 4, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 1, 8, 8, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 12, 1, 8, 12, 3, 8, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 1, 12, 8, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 12, 1, 12, 12, 3, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 1, 5, 4, 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            level, chunkBB, this.width - 5, 1, 5, this.width - 2, 4, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(level, chunkBB, 6, 7, 9, 6, 7, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            level, chunkBB, this.width - 7, 7, 9, this.width - 7, 7, 11, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(level, chunkBB, 5, 5, 9, 5, 7, 11, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            level,
            chunkBB,
            this.width - 6,
            5,
            9,
            this.width - 6,
            7,
            11,
            Blocks.CUT_SANDSTONE.defaultBlockState(),
            Blocks.CUT_SANDSTONE.defaultBlockState(),
            false
         );
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 5, 5, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 5, 6, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 6, 6, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), this.width - 6, 5, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), this.width - 6, 6, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), this.width - 7, 6, 10, chunkBB);
         this.generateBox(level, chunkBB, 2, 4, 4, 2, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, this.width - 3, 4, 4, this.width - 3, 6, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(level, northStairs, 2, 4, 5, chunkBB);
         this.placeBlock(level, northStairs, 2, 3, 4, chunkBB);
         this.placeBlock(level, northStairs, this.width - 3, 4, 5, chunkBB);
         this.placeBlock(level, northStairs, this.width - 3, 3, 4, chunkBB);
         this.generateBox(level, chunkBB, 1, 1, 3, 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            level, chunkBB, this.width - 3, 1, 3, this.width - 2, 2, 3, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.placeBlock(level, Blocks.SANDSTONE.defaultBlockState(), 1, 1, 2, chunkBB);
         this.placeBlock(level, Blocks.SANDSTONE.defaultBlockState(), this.width - 2, 1, 2, chunkBB);
         this.placeBlock(level, Blocks.SANDSTONE_SLAB.defaultBlockState(), 1, 2, 2, chunkBB);
         this.placeBlock(level, Blocks.SANDSTONE_SLAB.defaultBlockState(), this.width - 2, 2, 2, chunkBB);
         this.placeBlock(level, westStairs, 2, 1, 2, chunkBB);
         this.placeBlock(level, eastStairs, this.width - 3, 1, 2, chunkBB);
         this.generateBox(level, chunkBB, 4, 3, 5, 4, 3, 17, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            level, chunkBB, this.width - 5, 3, 5, this.width - 5, 3, 17, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(level, chunkBB, 3, 1, 5, 4, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, this.width - 6, 1, 5, this.width - 5, 2, 16, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);

         for (int z = 5; z <= 17; z += 2) {
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 4, 1, z, chunkBB);
            this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 4, 2, z, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), this.width - 5, 1, z, chunkBB);
            this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), this.width - 5, 2, z, chunkBB);
         }

         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 10, 0, 7, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 10, 0, 8, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 9, 0, 9, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 11, 0, 9, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 8, 0, 10, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 12, 0, 10, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 7, 0, 10, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 13, 0, 10, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 9, 0, 11, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 11, 0, 11, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 10, 0, 12, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 10, 0, 13, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.blue().defaultBlockState(), 10, 0, 10, chunkBB);

         for (int x = 0; x <= this.width - 1; x += this.width - 1) {
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 2, 1, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 2, 2, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 2, 3, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 3, 1, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 3, 2, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 3, 3, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 4, 1, chunkBB);
            this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), x, 4, 2, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 4, 3, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 5, 1, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 5, 2, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 5, 3, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 6, 1, chunkBB);
            this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), x, 6, 2, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 6, 3, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 7, 1, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 7, 2, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 7, 3, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 8, 1, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 8, 2, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 8, 3, chunkBB);
         }

         for (int x = 2; x <= this.width - 3; x += this.width - 3 - 2) {
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x - 1, 2, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 2, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x + 1, 2, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x - 1, 3, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 3, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x + 1, 3, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x - 1, 4, 0, chunkBB);
            this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), x, 4, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x + 1, 4, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x - 1, 5, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 5, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x + 1, 5, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x - 1, 6, 0, chunkBB);
            this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), x, 6, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x + 1, 6, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x - 1, 7, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x, 7, 0, chunkBB);
            this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), x + 1, 7, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x - 1, 8, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x, 8, 0, chunkBB);
            this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), x + 1, 8, 0, chunkBB);
         }

         this.generateBox(level, chunkBB, 8, 4, 0, 12, 6, 0, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 8, 6, 0, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 12, 6, 0, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 9, 5, 0, chunkBB);
         this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, 5, 0, chunkBB);
         this.placeBlock(level, Blocks.DYED_TERRACOTTA.orange().defaultBlockState(), 11, 5, 0, chunkBB);
         this.generateBox(level, chunkBB, 8, -14, 8, 12, -11, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(
            level, chunkBB, 8, -10, 8, 12, -10, 12, Blocks.CHISELED_SANDSTONE.defaultBlockState(), Blocks.CHISELED_SANDSTONE.defaultBlockState(), false
         );
         this.generateBox(level, chunkBB, 8, -9, 8, 12, -9, 12, Blocks.CUT_SANDSTONE.defaultBlockState(), Blocks.CUT_SANDSTONE.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, -8, 8, 12, -1, 12, Blocks.SANDSTONE.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 9, -11, 9, 11, -1, 11, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(level, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 10, -11, 10, chunkBB);
         this.generateBox(level, chunkBB, 9, -13, 9, 11, -13, 11, Blocks.TNT.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 8, -11, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 8, -10, 10, chunkBB);
         this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 7, -10, 10, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 7, -11, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 12, -11, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 12, -10, 10, chunkBB);
         this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 13, -10, 10, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 13, -11, 10, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 10, -11, 8, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 10, -10, 8, chunkBB);
         this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 7, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 7, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 10, -11, 12, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 10, -10, 12, chunkBB);
         this.placeBlock(level, Blocks.CHISELED_SANDSTONE.defaultBlockState(), 10, -10, 13, chunkBB);
         this.placeBlock(level, Blocks.CUT_SANDSTONE.defaultBlockState(), 10, -11, 13, chunkBB);

         for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!this.hasPlacedChest[direction.get2DDataValue()]) {
               int xo = direction.getStepX() * 2;
               int zo = direction.getStepZ() * 2;
               this.hasPlacedChest[direction.get2DDataValue()] = this.createChest(
                  level, chunkBB, random, 10 + xo, -11, 10 + zo, BuiltInLootTables.DESERT_PYRAMID
               );
            }
         }

         this.addCellar(level, chunkBB);
      }
   }

   private void addCellar(final WorldGenLevel level, final BoundingBox chunkBB) {
      BlockPos roomCenter = new BlockPos(16, -4, 13);
      this.addCellarStairs(roomCenter, level, chunkBB);
      this.addCellarRoom(roomCenter, level, chunkBB);
   }

   private void addCellarStairs(final BlockPos roomCenter, final WorldGenLevel level, final BoundingBox chunkBB) {
      int x = roomCenter.getX();
      int y = roomCenter.getY();
      int z = roomCenter.getZ();
      BlockState sandStoneStairs = Blocks.SANDSTONE_STAIRS.defaultBlockState();
      this.placeBlock(level, sandStoneStairs.rotate(Rotation.COUNTERCLOCKWISE_90), 13, -1, 17, chunkBB);
      this.placeBlock(level, sandStoneStairs.rotate(Rotation.COUNTERCLOCKWISE_90), 14, -2, 17, chunkBB);
      this.placeBlock(level, sandStoneStairs.rotate(Rotation.COUNTERCLOCKWISE_90), 15, -3, 17, chunkBB);
      BlockState sand = Blocks.SAND.defaultBlockState();
      BlockState sandStone = Blocks.SANDSTONE.defaultBlockState();
      boolean variant = level.getRandom().nextBoolean();
      this.placeBlock(level, sand, x - 4, y + 4, z + 4, chunkBB);
      this.placeBlock(level, sand, x - 3, y + 4, z + 4, chunkBB);
      this.placeBlock(level, sand, x - 2, y + 4, z + 4, chunkBB);
      this.placeBlock(level, sand, x - 1, y + 4, z + 4, chunkBB);
      this.placeBlock(level, sand, x, y + 4, z + 4, chunkBB);
      this.placeBlock(level, sand, x - 2, y + 3, z + 4, chunkBB);
      this.placeBlock(level, variant ? sand : sandStone, x - 1, y + 3, z + 4, chunkBB);
      this.placeBlock(level, !variant ? sand : sandStone, x, y + 3, z + 4, chunkBB);
      this.placeBlock(level, sand, x - 1, y + 2, z + 4, chunkBB);
      this.placeBlock(level, sandStone, x, y + 2, z + 4, chunkBB);
      this.placeBlock(level, sand, x, y + 1, z + 4, chunkBB);
   }

   private void addCellarRoom(final BlockPos roomCenter, final WorldGenLevel level, final BoundingBox chunkBB) {
      int x = roomCenter.getX();
      int y = roomCenter.getY();
      int z = roomCenter.getZ();
      BlockState cutSandStone = Blocks.CUT_SANDSTONE.defaultBlockState();
      BlockState hieroglyphsSandStone = Blocks.CHISELED_SANDSTONE.defaultBlockState();
      this.generateBox(level, chunkBB, x - 3, y + 1, z - 3, x - 3, y + 1, z + 2, cutSandStone, cutSandStone, true);
      this.generateBox(level, chunkBB, x + 3, y + 1, z - 3, x + 3, y + 1, z + 2, cutSandStone, cutSandStone, true);
      this.generateBox(level, chunkBB, x - 3, y + 1, z - 3, x + 3, y + 1, z - 2, cutSandStone, cutSandStone, true);
      this.generateBox(level, chunkBB, x - 3, y + 1, z + 3, x + 3, y + 1, z + 3, cutSandStone, cutSandStone, true);
      this.generateBox(level, chunkBB, x - 3, y + 2, z - 3, x - 3, y + 2, z + 2, hieroglyphsSandStone, hieroglyphsSandStone, true);
      this.generateBox(level, chunkBB, x + 3, y + 2, z - 3, x + 3, y + 2, z + 2, hieroglyphsSandStone, hieroglyphsSandStone, true);
      this.generateBox(level, chunkBB, x - 3, y + 2, z - 3, x + 3, y + 2, z - 2, hieroglyphsSandStone, hieroglyphsSandStone, true);
      this.generateBox(level, chunkBB, x - 3, y + 2, z + 3, x + 3, y + 2, z + 3, hieroglyphsSandStone, hieroglyphsSandStone, true);
      this.generateBox(level, chunkBB, x - 3, -1, z - 3, x - 3, -1, z + 2, cutSandStone, cutSandStone, true);
      this.generateBox(level, chunkBB, x + 3, -1, z - 3, x + 3, -1, z + 2, cutSandStone, cutSandStone, true);
      this.generateBox(level, chunkBB, x - 3, -1, z - 3, x + 3, -1, z - 2, cutSandStone, cutSandStone, true);
      this.generateBox(level, chunkBB, x - 3, -1, z + 3, x + 3, -1, z + 3, cutSandStone, cutSandStone, true);
      this.placeSandBox(x - 2, y + 1, z - 2, x + 2, y + 3, z + 2);
      this.placeCollapsedRoof(level, chunkBB, x - 2, y + 4, z - 2, x + 2, z + 2);
      BlockState orangeTeracotta = Blocks.DYED_TERRACOTTA.orange().defaultBlockState();
      BlockState blueTeracotta = Blocks.DYED_TERRACOTTA.blue().defaultBlockState();
      this.placeBlock(level, blueTeracotta, x, y, z, chunkBB);
      this.placeBlock(level, orangeTeracotta, x + 1, y, z - 1, chunkBB);
      this.placeBlock(level, orangeTeracotta, x + 1, y, z + 1, chunkBB);
      this.placeBlock(level, orangeTeracotta, x - 1, y, z - 1, chunkBB);
      this.placeBlock(level, orangeTeracotta, x - 1, y, z + 1, chunkBB);
      this.placeBlock(level, orangeTeracotta, x + 2, y, z, chunkBB);
      this.placeBlock(level, orangeTeracotta, x - 2, y, z, chunkBB);
      this.placeBlock(level, orangeTeracotta, x, y, z + 2, chunkBB);
      this.placeBlock(level, orangeTeracotta, x, y, z - 2, chunkBB);
      this.placeBlock(level, orangeTeracotta, x + 3, y, z, chunkBB);
      this.placeSand(x + 3, y + 1, z);
      this.placeSand(x + 3, y + 2, z);
      this.placeBlock(level, cutSandStone, x + 4, y + 1, z, chunkBB);
      this.placeBlock(level, hieroglyphsSandStone, x + 4, y + 2, z, chunkBB);
      this.placeBlock(level, orangeTeracotta, x - 3, y, z, chunkBB);
      this.placeSand(x - 3, y + 1, z);
      this.placeSand(x - 3, y + 2, z);
      this.placeBlock(level, cutSandStone, x - 4, y + 1, z, chunkBB);
      this.placeBlock(level, hieroglyphsSandStone, x - 4, y + 2, z, chunkBB);
      this.placeBlock(level, orangeTeracotta, x, y, z + 3, chunkBB);
      this.placeSand(x, y + 1, z + 3);
      this.placeSand(x, y + 2, z + 3);
      this.placeBlock(level, orangeTeracotta, x, y, z - 3, chunkBB);
      this.placeSand(x, y + 1, z - 3);
      this.placeSand(x, y + 2, z - 3);
      this.placeBlock(level, cutSandStone, x, y + 1, z - 4, chunkBB);
      this.placeBlock(level, hieroglyphsSandStone, x, -2, z - 4, chunkBB);
   }

   private void placeSand(final int x, final int y, final int z) {
      BlockPos worldPos = this.getWorldPos(x, y, z);
      this.potentialSuspiciousSandWorldPositions.add(worldPos);
   }

   private void placeSandBox(final int x0, final int y0, final int z0, final int x1, final int y1, final int z1) {
      for (int y = y0; y <= y1; y++) {
         for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
               this.placeSand(x, y, z);
            }
         }
      }
   }

   private void placeCollapsedRoofPiece(final WorldGenLevel level, final int x, final int y, final int z, final BoundingBox chunkBB) {
      if (level.getRandom().nextFloat() < 0.33F) {
         BlockState blockState = Blocks.SANDSTONE.defaultBlockState();
         this.placeBlock(level, blockState, x, y, z, chunkBB);
      } else {
         BlockState blockState = Blocks.SAND.defaultBlockState();
         this.placeBlock(level, blockState, x, y, z, chunkBB);
      }
   }

   private void placeCollapsedRoof(final WorldGenLevel level, final BoundingBox chunkBB, final int x0, final int y0, final int z0, final int x1, final int z1) {
      for (int x = x0; x <= x1; x++) {
         for (int z = z0; z <= z1; z++) {
            this.placeCollapsedRoofPiece(level, x, y0, z, chunkBB);
         }
      }

      RandomSource random = RandomSource.createThreadLocalInstance(level.getSeed()).forkPositional().at(this.getWorldPos(x0, y0, z0));
      int roofPosX = random.nextIntBetweenInclusive(x0, x1);
      int roofPosZ = random.nextIntBetweenInclusive(z0, z1);
      this.randomCollapsedRoofPos = new BlockPos(this.getWorldX(roofPosX, roofPosZ), this.getWorldY(y0), this.getWorldZ(roofPosX, roofPosZ));
   }

   public List<BlockPos> getPotentialSuspiciousSandWorldPositions() {
      return this.potentialSuspiciousSandWorldPositions;
   }

   public BlockPos getRandomCollapsedRoofPos() {
      return this.randomCollapsedRoofPos;
   }
}
