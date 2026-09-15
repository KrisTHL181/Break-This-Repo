package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class StrongholdPieces {
   private static final int SMALL_DOOR_WIDTH = 3;
   private static final int SMALL_DOOR_HEIGHT = 3;
   private static final int MAX_DEPTH = 50;
   private static final int LOWEST_Y_POSITION = 10;
   private static final boolean CHECK_AIR = true;
   public static final int MAGIC_START_Y = 64;
   private static final StrongholdPieces.PieceWeight[] STRONGHOLD_PIECE_WEIGHTS = new StrongholdPieces.PieceWeight[]{
      new StrongholdPieces.PieceWeight(StrongholdPieces.Straight.class, 40, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.PrisonHall.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.LeftTurn.class, 20, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.RightTurn.class, 20, 0),
      new StrongholdPieces.PieceWeight(StrongholdPieces.RoomCrossing.class, 10, 6),
      new StrongholdPieces.PieceWeight(StrongholdPieces.StraightStairsDown.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.StairsDown.class, 5, 5),
      new StrongholdPieces.PieceWeight(StrongholdPieces.FiveCrossing.class, 5, 4),
      new StrongholdPieces.PieceWeight(StrongholdPieces.ChestCorridor.class, 5, 4),
      new StrongholdPieces.PieceWeight(StrongholdPieces.Library.class, 10, 2) {
         @Override
         public boolean doPlace(final int depth) {
            return super.doPlace(depth) && depth > 4;
         }
      },
      new StrongholdPieces.PieceWeight(StrongholdPieces.PortalRoom.class, 20, 1) {
         @Override
         public boolean doPlace(final int depth) {
            return super.doPlace(depth) && depth > 5;
         }
      }
   };
   private static List<StrongholdPieces.PieceWeight> currentPieces;
   private static @Nullable Class<? extends StrongholdPieces.StrongholdPiece> imposedPiece;
   private static int totalWeight;
   private static final StrongholdPieces.SmoothStoneSelector SMOOTH_STONE_SELECTOR = new StrongholdPieces.SmoothStoneSelector();

   public static void resetPieces() {
      currentPieces = Lists.newArrayList();

      for (StrongholdPieces.PieceWeight piece : STRONGHOLD_PIECE_WEIGHTS) {
         piece.placeCount = 0;
         currentPieces.add(piece);
      }

      imposedPiece = null;
   }

   private static boolean updatePieceWeight() {
      boolean hasAnyPieces = false;
      totalWeight = 0;

      for (StrongholdPieces.PieceWeight piece : currentPieces) {
         if (piece.maxPlaceCount > 0 && piece.placeCount < piece.maxPlaceCount) {
            hasAnyPieces = true;
         }

         totalWeight = totalWeight + piece.weight;
      }

      return hasAnyPieces;
   }

   private static StrongholdPieces.@Nullable StrongholdPiece findAndCreatePieceFactory(
      final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass,
      final StructurePieceAccessor structurePieceAccessor,
      final RandomSource random,
      final int footX,
      final int footY,
      final int footZ,
      final Direction direction,
      final int depth
   ) {
      StrongholdPieces.StrongholdPiece strongholdPiece = null;
      if (pieceClass == StrongholdPieces.Straight.class) {
         strongholdPiece = StrongholdPieces.Straight.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.PrisonHall.class) {
         strongholdPiece = StrongholdPieces.PrisonHall.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.LeftTurn.class) {
         strongholdPiece = StrongholdPieces.LeftTurn.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.RightTurn.class) {
         strongholdPiece = StrongholdPieces.RightTurn.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.RoomCrossing.class) {
         strongholdPiece = StrongholdPieces.RoomCrossing.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.StraightStairsDown.class) {
         strongholdPiece = StrongholdPieces.StraightStairsDown.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.StairsDown.class) {
         strongholdPiece = StrongholdPieces.StairsDown.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.FiveCrossing.class) {
         strongholdPiece = StrongholdPieces.FiveCrossing.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.ChestCorridor.class) {
         strongholdPiece = StrongholdPieces.ChestCorridor.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.Library.class) {
         strongholdPiece = StrongholdPieces.Library.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == StrongholdPieces.PortalRoom.class) {
         strongholdPiece = StrongholdPieces.PortalRoom.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      }

      return strongholdPiece;
   }

   private static StrongholdPieces.@Nullable StrongholdPiece generatePieceFromSmallDoor(
      final StrongholdPieces.StartPiece startPiece,
      final StructurePieceAccessor structurePieceAccessor,
      final RandomSource random,
      final int footX,
      final int footY,
      final int footZ,
      final Direction direction,
      final int depth
   ) {
      if (!updatePieceWeight()) {
         return null;
      }

      if (imposedPiece != null) {
         StrongholdPieces.StrongholdPiece strongholdPiece = findAndCreatePieceFactory(
            imposedPiece, structurePieceAccessor, random, footX, footY, footZ, direction, depth
         );
         imposedPiece = null;
         if (strongholdPiece != null) {
            return strongholdPiece;
         }
      }

      int numAttempts = 0;

      while (numAttempts < 5) {
         numAttempts++;
         int weightSelection = random.nextInt(totalWeight);

         for (StrongholdPieces.PieceWeight piece : currentPieces) {
            weightSelection -= piece.weight;
            if (weightSelection < 0) {
               if (!piece.doPlace(depth) || piece == startPiece.previousPiece) {
                  break;
               }

               StrongholdPieces.StrongholdPiece strongholdPiece = findAndCreatePieceFactory(
                  piece.pieceClass, structurePieceAccessor, random, footX, footY, footZ, direction, depth
               );
               if (strongholdPiece != null) {
                  piece.placeCount++;
                  startPiece.previousPiece = piece;
                  if (!piece.isValid()) {
                     currentPieces.remove(piece);
                  }

                  return strongholdPiece;
               }
            }
         }
      }

      BoundingBox box = StrongholdPieces.FillerCorridor.findPieceBox(structurePieceAccessor, random, footX, footY, footZ, direction);
      return box != null && box.minY() > 1 ? new StrongholdPieces.FillerCorridor(depth, box, direction) : null;
   }

   private static @Nullable StructurePiece generateAndAddPiece(
      final StrongholdPieces.StartPiece startPiece,
      final StructurePieceAccessor structurePieceAccessor,
      final RandomSource random,
      final int footX,
      final int footY,
      final int footZ,
      final Direction direction,
      final int depth
   ) {
      if (depth > 50) {
         return null;
      }

      if (Math.abs(footX - startPiece.getBoundingBox().minX()) <= 112 && Math.abs(footZ - startPiece.getBoundingBox().minZ()) <= 112) {
         StructurePiece newPiece = generatePieceFromSmallDoor(startPiece, structurePieceAccessor, random, footX, footY, footZ, direction, depth + 1);
         if (newPiece != null) {
            structurePieceAccessor.addPiece(newPiece);
            startPiece.pendingChildren.add(newPiece);
         }

         return newPiece;
      } else {
         return null;
      }
   }

   public static class ChestCorridor extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 5;
      private static final int DEPTH = 7;
      private boolean hasPlacedChest;

      public ChestCorridor(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
      }

      public ChestCorridor(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, tag);
         this.hasPlacedChest = tag.getBooleanOr("Chest", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Chest", this.hasPlacedChest);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
      }

      public static StrongholdPieces.@Nullable ChestCorridor createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 5, 5, 7, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new StrongholdPieces.ChestCorridor(genDepth, random, box, direction)
            : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 4, 6, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(level, random, chunkBB, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         this.generateBox(level, chunkBB, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), false);
         this.placeBlock(level, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 1, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 5, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 2, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 4, chunkBB);

         for (int z = 2; z <= 4; z++) {
            this.placeBlock(level, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2, 1, z, chunkBB);
         }

         if (!this.hasPlacedChest && chunkBB.isInside(this.getWorldPos(3, 2, 3))) {
            this.hasPlacedChest = true;
            this.createChest(level, chunkBB, random, 3, 2, 3, BuiltInLootTables.STRONGHOLD_CORRIDOR);
         }
      }
   }

   public static class FillerCorridor extends StrongholdPieces.StrongholdPiece {
      private final int steps;

      public FillerCorridor(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, genDepth, boundingBox);
         this.setOrientation(direction);
         this.steps = direction != Direction.NORTH && direction != Direction.SOUTH ? boundingBox.getXSpan() : boundingBox.getZSpan();
      }

      public FillerCorridor(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, tag);
         this.steps = tag.getIntOr("Steps", 0);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putInt("Steps", this.steps);
      }

      public static @Nullable BoundingBox findPieceBox(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction
      ) {
         int maxLength = 3;
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 5, 5, 4, direction);
         StructurePiece collisionPiece = structurePieceAccessor.findCollisionPiece(box);
         if (collisionPiece == null) {
            return null;
         }

         if (collisionPiece.getBoundingBox().minY() == box.minY()) {
            for (int depth = 2; depth >= 1; depth--) {
               box = BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 5, 5, depth, direction);
               if (!collisionPiece.getBoundingBox().intersects(box)) {
                  return BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 5, 5, depth + 1, direction);
               }
            }
         }

         return null;
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
         for (int i = 0; i < this.steps; i++) {
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 0, 0, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 0, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 2, 0, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3, 0, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 4, 0, i, chunkBB);

            for (int y = 1; y <= 3; y++) {
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 0, y, i, chunkBB);
               this.placeBlock(level, Blocks.CAVE_AIR.defaultBlockState(), 1, y, i, chunkBB);
               this.placeBlock(level, Blocks.CAVE_AIR.defaultBlockState(), 2, y, i, chunkBB);
               this.placeBlock(level, Blocks.CAVE_AIR.defaultBlockState(), 3, y, i, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 4, y, i, chunkBB);
            }

            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 0, 4, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3, 4, i, chunkBB);
            this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 4, 4, i, chunkBB);
         }
      }
   }

   public static class FiveCrossing extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 10;
      protected static final int HEIGHT = 9;
      protected static final int DEPTH = 11;
      private final boolean leftLow;
      private final boolean leftHigh;
      private final boolean rightLow;
      private final boolean rightHigh;

      public FiveCrossing(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
         this.leftLow = random.nextBoolean();
         this.leftHigh = random.nextBoolean();
         this.rightLow = random.nextBoolean();
         this.rightHigh = random.nextInt(3) > 0;
      }

      public FiveCrossing(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, tag);
         this.leftLow = tag.getBooleanOr("leftLow", false);
         this.leftHigh = tag.getBooleanOr("leftHigh", false);
         this.rightLow = tag.getBooleanOr("rightLow", false);
         this.rightHigh = tag.getBooleanOr("rightHigh", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("leftLow", this.leftLow);
         tag.putBoolean("leftHigh", this.leftHigh);
         tag.putBoolean("rightLow", this.rightLow);
         tag.putBoolean("rightHigh", this.rightHigh);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         int zOffA = 3;
         int zOffB = 5;
         Direction orientation = this.getOrientation();
         if (orientation == Direction.WEST || orientation == Direction.NORTH) {
            zOffA = 8 - zOffA;
            zOffB = 8 - zOffB;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 5, 1);
         if (this.leftLow) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, zOffA, 1);
         }

         if (this.leftHigh) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, zOffB, 7);
         }

         if (this.rightLow) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, zOffA, 1);
         }

         if (this.rightHigh) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, zOffB, 7);
         }
      }

      public static StrongholdPieces.@Nullable FiveCrossing createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -4, -3, 0, 10, 9, 11, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new StrongholdPieces.FiveCrossing(genDepth, random, box, direction)
            : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 9, 8, 10, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 4, 3, 0);
         if (this.leftLow) {
            this.generateBox(level, chunkBB, 0, 3, 1, 0, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightLow) {
            this.generateBox(level, chunkBB, 9, 3, 1, 9, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.leftHigh) {
            this.generateBox(level, chunkBB, 0, 5, 7, 0, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightHigh) {
            this.generateBox(level, chunkBB, 9, 5, 7, 9, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         this.generateBox(level, chunkBB, 5, 1, 10, 7, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(level, chunkBB, 1, 2, 1, 8, 2, 6, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 1, 5, 4, 4, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 8, 1, 5, 8, 4, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 4, 7, 3, 4, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 3, 5, 3, 3, 6, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 1, 7, 7, 1, 8, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false);
         this.generateBox(
            level,
            chunkBB,
            5,
            5,
            7,
            7,
            5,
            9,
            Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
            Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE),
            false
         );
         this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 6, 5, 6, chunkBB);
      }
   }

   public static class LeftTurn extends StrongholdPieces.Turn {
      public LeftTurn(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
      }

      public LeftTurn(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         Direction orientation = this.getOrientation();
         if (orientation != Direction.NORTH && orientation != Direction.EAST) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
         } else {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
         }
      }

      public static StrongholdPieces.@Nullable LeftTurn createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 5, 5, 5, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null ? new StrongholdPieces.LeftTurn(genDepth, random, box, direction) : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 4, 4, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 1, 1, 0);
         Direction orientation = this.getOrientation();
         if (orientation != Direction.NORTH && orientation != Direction.EAST) {
            this.generateBox(level, chunkBB, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(level, chunkBB, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class Library extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 14;
      protected static final int HEIGHT = 6;
      protected static final int TALL_HEIGHT = 11;
      protected static final int DEPTH = 15;
      private final boolean isTall;

      public Library(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
         this.isTall = boundingBox.getYSpan() > 6;
      }

      public Library(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, tag);
         this.isTall = tag.getBooleanOr("Tall", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Tall", this.isTall);
      }

      public static StrongholdPieces.@Nullable Library createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -4, -1, 0, 14, 11, 15, direction);
         if (!isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
            box = BoundingBox.orientBox(footX, footY, footZ, -4, -1, 0, 14, 6, 15, direction);
            if (!isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
               return null;
            }
         }

         return new StrongholdPieces.Library(genDepth, random, box, direction);
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
         int currentHeight = 11;
         if (!this.isTall) {
            currentHeight = 6;
         }

         this.generateBox(level, chunkBB, 0, 0, 0, 13, currentHeight - 1, 14, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 4, 1, 0);
         this.generateMaybeBox(
            level, chunkBB, random, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.defaultBlockState(), Blocks.COBWEB.defaultBlockState(), false, false
         );
         int bookLeft = 1;
         int bookRight = 12;

         for (int d = 1; d <= 13; d++) {
            if ((d - 1) % 4 == 0) {
               this.generateBox(level, chunkBB, 1, 1, d, 1, 4, d, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.generateBox(level, chunkBB, 12, 1, d, 12, 4, d, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 2, 3, d, chunkBB);
               this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 11, 3, d, chunkBB);
               if (this.isTall) {
                  this.generateBox(level, chunkBB, 1, 6, d, 1, 9, d, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                  this.generateBox(level, chunkBB, 12, 6, d, 12, 9, d, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               }
            } else {
               this.generateBox(level, chunkBB, 1, 1, d, 1, 4, d, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               this.generateBox(level, chunkBB, 12, 1, d, 12, 4, d, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               if (this.isTall) {
                  this.generateBox(level, chunkBB, 1, 6, d, 1, 9, d, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                  this.generateBox(level, chunkBB, 12, 6, d, 12, 9, d, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               }
            }
         }

         for (int d = 3; d < 12; d += 2) {
            this.generateBox(level, chunkBB, 3, 1, d, 4, 3, d, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 1, d, 7, 3, d, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 9, 1, d, 10, 3, d, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
         }

         if (this.isTall) {
            this.generateBox(level, chunkBB, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 11, chunkBB);
            this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 8, 5, 11, chunkBB);
            this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 10, chunkBB);
            BlockState weFence = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState nsFence = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(level, chunkBB, 3, 6, 3, 3, 6, 11, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 10, 6, 3, 10, 6, 9, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 4, 6, 2, 9, 6, 2, weFence, weFence, false);
            this.generateBox(level, chunkBB, 4, 6, 12, 7, 6, 12, weFence, weFence, false);
            this.placeBlock(level, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 3, 6, 2, chunkBB);
            this.placeBlock(level, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 3, 6, 12, chunkBB);
            this.placeBlock(level, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 10, 6, 2, chunkBB);

            for (int i = 0; i <= 2; i++) {
               this.placeBlock(
                  level, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true), 8 + i, 6, 12 - i, chunkBB
               );
               if (i != 2) {
                  this.placeBlock(
                     level, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 8 + i, 6, 11 - i, chunkBB
                  );
               }
            }

            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            this.placeBlock(level, ladder, 10, 1, 13, chunkBB);
            this.placeBlock(level, ladder, 10, 2, 13, chunkBB);
            this.placeBlock(level, ladder, 10, 3, 13, chunkBB);
            this.placeBlock(level, ladder, 10, 4, 13, chunkBB);
            this.placeBlock(level, ladder, 10, 5, 13, chunkBB);
            this.placeBlock(level, ladder, 10, 6, 13, chunkBB);
            this.placeBlock(level, ladder, 10, 7, 13, chunkBB);
            int x = 7;
            int z = 7;
            BlockState eFence = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true);
            this.placeBlock(level, eFence, 6, 9, 7, chunkBB);
            BlockState wFence = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true);
            this.placeBlock(level, wFence, 7, 9, 7, chunkBB);
            this.placeBlock(level, eFence, 6, 8, 7, chunkBB);
            this.placeBlock(level, wFence, 7, 8, 7, chunkBB);
            BlockState nsweFence = nsFence.setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            this.placeBlock(level, nsweFence, 6, 7, 7, chunkBB);
            this.placeBlock(level, nsweFence, 7, 7, 7, chunkBB);
            this.placeBlock(level, eFence, 5, 7, 7, chunkBB);
            this.placeBlock(level, wFence, 8, 7, 7, chunkBB);
            this.placeBlock(level, eFence.setValue(FenceBlock.NORTH, true), 6, 7, 6, chunkBB);
            this.placeBlock(level, eFence.setValue(FenceBlock.SOUTH, true), 6, 7, 8, chunkBB);
            this.placeBlock(level, wFence.setValue(FenceBlock.NORTH, true), 7, 7, 6, chunkBB);
            this.placeBlock(level, wFence.setValue(FenceBlock.SOUTH, true), 7, 7, 8, chunkBB);
            BlockState torch = Blocks.TORCH.defaultBlockState();
            this.placeBlock(level, torch, 5, 8, 7, chunkBB);
            this.placeBlock(level, torch, 8, 8, 7, chunkBB);
            this.placeBlock(level, torch, 6, 8, 6, chunkBB);
            this.placeBlock(level, torch, 6, 8, 8, chunkBB);
            this.placeBlock(level, torch, 7, 8, 6, chunkBB);
            this.placeBlock(level, torch, 7, 8, 8, chunkBB);
         }

         this.createChest(level, chunkBB, random, 3, 3, 5, BuiltInLootTables.STRONGHOLD_LIBRARY);
         if (this.isTall) {
            this.placeBlock(level, CAVE_AIR, 12, 9, 1, chunkBB);
            this.createChest(level, chunkBB, random, 12, 8, 1, BuiltInLootTables.STRONGHOLD_LIBRARY);
         }
      }
   }

   private static class PieceWeight {
      public final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass;
      public final int weight;
      public int placeCount;
      public final int maxPlaceCount;

      public PieceWeight(final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass, final int weight, final int maxPlaceCount) {
         this.pieceClass = pieceClass;
         this.weight = weight;
         this.maxPlaceCount = maxPlaceCount;
      }

      public boolean doPlace(final int depth) {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }

      public boolean isValid() {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }
   }

   public static class PortalRoom extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 11;
      protected static final int HEIGHT = 8;
      protected static final int DEPTH = 16;
      private boolean hasPlacedSpawner;

      public PortalRoom(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public PortalRoom(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, tag);
         this.hasPlacedSpawner = tag.getBooleanOr("Mob", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Mob", this.hasPlacedSpawner);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         if (startPiece != null) {
            ((StrongholdPieces.StartPiece)startPiece).portalRoomPiece = this;
         }
      }

      public static StrongholdPieces.@Nullable PortalRoom createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -4, -1, 0, 11, 8, 16, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null ? new StrongholdPieces.PortalRoom(genDepth, box, direction) : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 10, 7, 15, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES, 4, 1, 0);
         int y = 6;
         this.generateBox(level, chunkBB, 1, 6, 1, 1, 6, 14, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 9, 6, 1, 9, 6, 14, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 6, 1, 8, 6, 2, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 6, 14, 8, 6, 14, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 1, 1, 2, 1, 4, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 8, 1, 1, 9, 1, 4, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 1, 1, 1, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 9, 1, 1, 9, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 3, 1, 8, 7, 1, 12, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 1, 9, 6, 1, 11, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         BlockState nsBars = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
         BlockState weBars = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true);

         for (int z = 3; z < 14; z += 2) {
            this.generateBox(level, chunkBB, 0, 3, z, 0, 4, z, nsBars, nsBars, false);
            this.generateBox(level, chunkBB, 10, 3, z, 10, 4, z, nsBars, nsBars, false);
         }

         for (int x = 2; x < 9; x += 2) {
            this.generateBox(level, chunkBB, x, 3, 15, x, 4, 15, weBars, weBars, false);
         }

         BlockState blockState = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         this.generateBox(level, chunkBB, 4, 1, 5, 6, 1, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 2, 6, 6, 2, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 3, 7, 6, 3, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int x = 4; x <= 6; x++) {
            this.placeBlock(level, blockState, x, 1, 4, chunkBB);
            this.placeBlock(level, blockState, x, 2, 5, chunkBB);
            this.placeBlock(level, blockState, x, 3, 6, chunkBB);
         }

         BlockState northFrame = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.NORTH);
         BlockState southFrame = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.SOUTH);
         BlockState eastFrame = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.EAST);
         BlockState westFrame = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.WEST);
         boolean allEyes = true;
         boolean[] eyes = new boolean[12];

         for (int i = 0; i < eyes.length; i++) {
            eyes[i] = random.nextFloat() > 0.9F;
            allEyes &= eyes[i];
         }

         this.placeBlock(level, northFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[0]), 4, 3, 8, chunkBB);
         this.placeBlock(level, northFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[1]), 5, 3, 8, chunkBB);
         this.placeBlock(level, northFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[2]), 6, 3, 8, chunkBB);
         this.placeBlock(level, southFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[3]), 4, 3, 12, chunkBB);
         this.placeBlock(level, southFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[4]), 5, 3, 12, chunkBB);
         this.placeBlock(level, southFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[5]), 6, 3, 12, chunkBB);
         this.placeBlock(level, eastFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[6]), 3, 3, 9, chunkBB);
         this.placeBlock(level, eastFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[7]), 3, 3, 10, chunkBB);
         this.placeBlock(level, eastFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[8]), 3, 3, 11, chunkBB);
         this.placeBlock(level, westFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[9]), 7, 3, 9, chunkBB);
         this.placeBlock(level, westFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[10]), 7, 3, 10, chunkBB);
         this.placeBlock(level, westFrame.setValue(EndPortalFrameBlock.HAS_EYE, eyes[11]), 7, 3, 11, chunkBB);
         if (allEyes) {
            BlockState portal = Blocks.END_PORTAL.defaultBlockState();
            this.placeBlock(level, portal, 4, 3, 9, chunkBB);
            this.placeBlock(level, portal, 5, 3, 9, chunkBB);
            this.placeBlock(level, portal, 6, 3, 9, chunkBB);
            this.placeBlock(level, portal, 4, 3, 10, chunkBB);
            this.placeBlock(level, portal, 5, 3, 10, chunkBB);
            this.placeBlock(level, portal, 6, 3, 10, chunkBB);
            this.placeBlock(level, portal, 4, 3, 11, chunkBB);
            this.placeBlock(level, portal, 5, 3, 11, chunkBB);
            this.placeBlock(level, portal, 6, 3, 11, chunkBB);
         }

         if (!this.hasPlacedSpawner) {
            BlockPos pos = this.getWorldPos(5, 3, 6);
            if (chunkBB.isInside(pos)) {
               this.hasPlacedSpawner = true;
               level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
               if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
                  spawner.setEntityId(EntityTypes.SILVERFISH, random);
               }
            }
         }
      }
   }

   public static class PrisonHall extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 9;
      protected static final int HEIGHT = 5;
      protected static final int DEPTH = 11;

      public PrisonHall(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
      }

      public PrisonHall(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
      }

      public static StrongholdPieces.@Nullable PrisonHall createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 9, 5, 11, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new StrongholdPieces.PrisonHall(genDepth, random, box, direction)
            : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 8, 4, 10, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 1, 1, 0);
         this.generateBox(level, chunkBB, 1, 1, 10, 3, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(level, chunkBB, 4, 1, 1, 4, 3, 1, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 1, 3, 4, 3, 3, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 1, 7, 4, 3, 7, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 1, 9, 4, 3, 9, false, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int y = 1; y <= 3; y++) {
            this.placeBlock(
               level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, y, 4, chunkBB
            );
            this.placeBlock(
               level,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true).setValue(IronBarsBlock.EAST, true),
               4,
               y,
               5,
               chunkBB
            );
            this.placeBlock(
               level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, y, 6, chunkBB
            );
            this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 5, y, 5, chunkBB);
            this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 6, y, 5, chunkBB);
            this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 7, y, 5, chunkBB);
         }

         this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 2, chunkBB);
         this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 8, chunkBB);
         BlockState doorBottom = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST);
         BlockState doorTop = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
         this.placeBlock(level, doorBottom, 4, 1, 2, chunkBB);
         this.placeBlock(level, doorTop, 4, 2, 2, chunkBB);
         this.placeBlock(level, doorBottom, 4, 1, 8, chunkBB);
         this.placeBlock(level, doorTop, 4, 2, 8, chunkBB);
      }
   }

   public static class RightTurn extends StrongholdPieces.Turn {
      public RightTurn(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
      }

      public RightTurn(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         Direction orientation = this.getOrientation();
         if (orientation != Direction.NORTH && orientation != Direction.EAST) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
         } else {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
         }
      }

      public static StrongholdPieces.@Nullable RightTurn createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 5, 5, 5, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new StrongholdPieces.RightTurn(genDepth, random, box, direction)
            : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 4, 4, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 1, 1, 0);
         Direction orientation = this.getOrientation();
         if (orientation != Direction.NORTH && orientation != Direction.EAST) {
            this.generateBox(level, chunkBB, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(level, chunkBB, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class RoomCrossing extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 11;
      protected static final int HEIGHT = 7;
      protected static final int DEPTH = 11;
      protected final int type;

      public RoomCrossing(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
         this.type = random.nextInt(5);
      }

      public RoomCrossing(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, tag);
         this.type = tag.getIntOr("Type", 0);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putInt("Type", this.type);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 4, 1);
         this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 4);
         this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 4);
      }

      public static StrongholdPieces.@Nullable RoomCrossing createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -4, -1, 0, 11, 7, 11, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new StrongholdPieces.RoomCrossing(genDepth, random, box, direction)
            : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 10, 6, 10, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 4, 1, 0);
         this.generateBox(level, chunkBB, 4, 1, 10, 6, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(level, chunkBB, 0, 1, 4, 0, 3, 6, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(level, chunkBB, 10, 1, 4, 10, 3, 6, CAVE_AIR, CAVE_AIR, false);
         switch (this.type) {
            case 0:
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, chunkBB);
               this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 4, 3, 5, chunkBB);
               this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 6, 3, 5, chunkBB);
               this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 5, 3, 4, chunkBB);
               this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH), 5, 3, 6, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 4, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 5, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 6, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 4, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 5, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 6, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 4, chunkBB);
               this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 6, chunkBB);
               break;
            case 1:
               for (int i = 0; i < 5; i++) {
                  this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3, 1, 3 + i, chunkBB);
                  this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 7, 1, 3 + i, chunkBB);
                  this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i, 1, 3, chunkBB);
                  this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i, 1, 7, chunkBB);
               }

               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, chunkBB);
               this.placeBlock(level, Blocks.WATER.defaultBlockState(), 5, 4, 5, chunkBB);
               break;
            case 2:
               for (int z = 1; z <= 9; z++) {
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 1, 3, z, chunkBB);
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 9, 3, z, chunkBB);
               }

               for (int x = 1; x <= 9; x++) {
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), x, 3, 1, chunkBB);
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), x, 3, 9, chunkBB);
               }

               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 4, chunkBB);
               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 6, chunkBB);
               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 4, chunkBB);
               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 6, chunkBB);
               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 4, 1, 5, chunkBB);
               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 6, 1, 5, chunkBB);
               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 4, 3, 5, chunkBB);
               this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 6, 3, 5, chunkBB);

               for (int y = 1; y <= 3; y++) {
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 4, y, 4, chunkBB);
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 6, y, 4, chunkBB);
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 4, y, 6, chunkBB);
                  this.placeBlock(level, Blocks.COBBLESTONE.defaultBlockState(), 6, y, 6, chunkBB);
               }

               this.placeBlock(level, Blocks.WALL_TORCH.defaultBlockState(), 5, 3, 5, chunkBB);

               for (int z = 2; z <= 8; z++) {
                  this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 2, 3, z, chunkBB);
                  this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 3, 3, z, chunkBB);
                  if (z <= 3 || z >= 7) {
                     this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 4, 3, z, chunkBB);
                     this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 5, 3, z, chunkBB);
                     this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 6, 3, z, chunkBB);
                  }

                  this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 7, 3, z, chunkBB);
                  this.placeBlock(level, Blocks.OAK_PLANKS.defaultBlockState(), 8, 3, z, chunkBB);
               }

               BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
               this.placeBlock(level, ladder, 9, 1, 3, chunkBB);
               this.placeBlock(level, ladder, 9, 2, 3, chunkBB);
               this.placeBlock(level, ladder, 9, 3, 3, chunkBB);
               this.createChest(level, chunkBB, random, 3, 4, 8, BuiltInLootTables.STRONGHOLD_CROSSING);
         }
      }
   }

   private static class SmoothStoneSelector extends StructurePiece.BlockSelector {
      @Override
      public void next(final RandomSource random, final int worldX, final int worldY, final int worldZ, final boolean isEdge) {
         if (isEdge) {
            float selection = random.nextFloat();
            if (selection < 0.2F) {
               this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            } else if (selection < 0.5F) {
               this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            } else if (selection < 0.55F) {
               this.next = Blocks.INFESTED_STONE_BRICKS.defaultBlockState();
            } else {
               this.next = Blocks.STONE_BRICKS.defaultBlockState();
            }
         } else {
            this.next = Blocks.CAVE_AIR.defaultBlockState();
         }
      }
   }

   public static class StairsDown extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 5;
      private final boolean isSource;

      public StairsDown(final StructurePieceType type, final int genDepth, final int west, final int north, final Direction direction) {
         super(type, genDepth, makeBoundingBox(west, 64, north, direction, 5, 11, 5));
         this.isSource = true;
         this.setOrientation(direction);
         this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
      }

      public StairsDown(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_STAIRS_DOWN, genDepth, boundingBox);
         this.isSource = false;
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
      }

      public StairsDown(final StructurePieceType type, final CompoundTag tag) {
         super(type, tag);
         this.isSource = tag.getBooleanOr("Source", false);
      }

      public StairsDown(final CompoundTag tag) {
         this(StructurePieceType.STRONGHOLD_STAIRS_DOWN, tag);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Source", this.isSource);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         if (this.isSource) {
            StrongholdPieces.imposedPiece = StrongholdPieces.FiveCrossing.class;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
      }

      public static StrongholdPieces.@Nullable StairsDown createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -7, 0, 5, 11, 5, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new StrongholdPieces.StairsDown(genDepth, random, box, direction)
            : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 10, 4, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(level, random, chunkBB, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 4);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 2, 6, 1, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 1, chunkBB);
         this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 6, 1, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 2, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, 3, chunkBB);
         this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 5, 3, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, 3, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 3, chunkBB);
         this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 4, 3, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 2, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3, 2, 1, chunkBB);
         this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 3, 1, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 2, 2, 1, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 1, chunkBB);
         this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 2, 1, chunkBB);
         this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 2, chunkBB);
         this.placeBlock(level, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 1, 3, chunkBB);
      }
   }

   public static class StartPiece extends StrongholdPieces.StairsDown {
      public StrongholdPieces.@Nullable PieceWeight previousPiece;
      public StrongholdPieces.@Nullable PortalRoom portalRoomPiece;
      public final List<StructurePiece> pendingChildren = Lists.newArrayList();

      public StartPiece(final RandomSource random, final int west, final int north) {
         super(StructurePieceType.STRONGHOLD_START, 0, west, north, getRandomHorizontalDirection(random));
      }

      public StartPiece(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_START, tag);
      }

      @Override
      public BlockPos getLocatorPosition() {
         return this.portalRoomPiece != null ? this.portalRoomPiece.getLocatorPosition() : super.getLocatorPosition();
      }
   }

   public static class Straight extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 5;
      private static final int DEPTH = 7;
      private final boolean leftChild;
      private final boolean rightChild;

      public Straight(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
         this.leftChild = random.nextInt(2) == 0;
         this.rightChild = random.nextInt(2) == 0;
      }

      public Straight(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, tag);
         this.leftChild = tag.getBooleanOr("Left", false);
         this.rightChild = tag.getBooleanOr("Right", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Left", this.leftChild);
         tag.putBoolean("Right", this.rightChild);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
         if (this.leftChild) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 2);
         }

         if (this.rightChild) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 2);
         }
      }

      public static StrongholdPieces.@Nullable Straight createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -1, 0, 5, 5, 7, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null ? new StrongholdPieces.Straight(genDepth, random, box, direction) : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 4, 6, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(level, random, chunkBB, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         BlockState eastTorch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST);
         BlockState westTorch = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST);
         this.maybeGenerateBlock(level, chunkBB, random, 0.1F, 1, 2, 1, eastTorch);
         this.maybeGenerateBlock(level, chunkBB, random, 0.1F, 3, 2, 1, westTorch);
         this.maybeGenerateBlock(level, chunkBB, random, 0.1F, 1, 2, 5, eastTorch);
         this.maybeGenerateBlock(level, chunkBB, random, 0.1F, 3, 2, 5, westTorch);
         if (this.leftChild) {
            this.generateBox(level, chunkBB, 0, 1, 2, 0, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightChild) {
            this.generateBox(level, chunkBB, 4, 1, 2, 4, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class StraightStairsDown extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 8;

      public StraightStairsDown(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, genDepth, boundingBox);
         this.setOrientation(direction);
         this.entryDoor = this.randomSmallDoor(random);
      }

      public StraightStairsDown(final CompoundTag tag) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 1);
      }

      public static StrongholdPieces.@Nullable StraightStairsDown createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -7, 0, 5, 11, 8, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new StrongholdPieces.StraightStairsDown(genDepth, random, box, direction)
            : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 10, 7, true, random, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(level, random, chunkBB, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(level, random, chunkBB, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 7);
         BlockState stairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

         for (int i = 0; i < 6; i++) {
            this.placeBlock(level, stairs, 1, 6 - i, 1 + i, chunkBB);
            this.placeBlock(level, stairs, 2, 6 - i, 1 + i, chunkBB);
            this.placeBlock(level, stairs, 3, 6 - i, 1 + i, chunkBB);
            if (i < 5) {
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5 - i, 1 + i, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 2, 5 - i, 1 + i, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), 3, 5 - i, 1 + i, chunkBB);
            }
         }
      }
   }

   private abstract static class StrongholdPiece extends StructurePiece {
      protected StrongholdPieces.StrongholdPiece.SmallDoorType entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;

      protected StrongholdPiece(final StructurePieceType type, final int genDepth, final BoundingBox boundingBox) {
         super(type, genDepth, boundingBox);
      }

      public StrongholdPiece(final StructurePieceType type, final CompoundTag tag) {
         super(type, tag);
         this.entryDoor = tag.<StrongholdPieces.StrongholdPiece.SmallDoorType>read("EntryDoor", StrongholdPieces.StrongholdPiece.SmallDoorType.LEGACY_CODEC)
            .orElseThrow();
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         tag.store("EntryDoor", StrongholdPieces.StrongholdPiece.SmallDoorType.LEGACY_CODEC, this.entryDoor);
      }

      protected void generateSmallDoor(
         final WorldGenLevel level,
         final RandomSource random,
         final BoundingBox chunkBB,
         final StrongholdPieces.StrongholdPiece.SmallDoorType doorType,
         final int footX,
         final int footY,
         final int footZ
      ) {
         switch (doorType) {
            case OPENING:
               this.generateBox(level, chunkBB, footX, footY, footZ, footX + 3 - 1, footY + 3 - 1, footZ, CAVE_AIR, CAVE_AIR, false);
               break;
            case WOOD_DOOR:
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX, footY, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX, footY + 1, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX, footY + 2, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 1, footY + 2, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 2, footY + 2, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 2, footY + 1, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 2, footY, footZ, chunkBB);
               this.placeBlock(level, Blocks.OAK_DOOR.defaultBlockState(), footX + 1, footY, footZ, chunkBB);
               this.placeBlock(level, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), footX + 1, footY + 1, footZ, chunkBB);
               break;
            case GRATES:
               this.placeBlock(level, Blocks.CAVE_AIR.defaultBlockState(), footX + 1, footY, footZ, chunkBB);
               this.placeBlock(level, Blocks.CAVE_AIR.defaultBlockState(), footX + 1, footY + 1, footZ, chunkBB);
               this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), footX, footY, footZ, chunkBB);
               this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), footX, footY + 1, footZ, chunkBB);
               this.placeBlock(
                  level,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  footX,
                  footY + 2,
                  footZ,
                  chunkBB
               );
               this.placeBlock(
                  level,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  footX + 1,
                  footY + 2,
                  footZ,
                  chunkBB
               );
               this.placeBlock(
                  level,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  footX + 2,
                  footY + 2,
                  footZ,
                  chunkBB
               );
               this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), footX + 2, footY + 1, footZ, chunkBB);
               this.placeBlock(level, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), footX + 2, footY, footZ, chunkBB);
               break;
            case IRON_DOOR:
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX, footY, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX, footY + 1, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX, footY + 2, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 1, footY + 2, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 2, footY + 2, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 2, footY + 1, footZ, chunkBB);
               this.placeBlock(level, Blocks.STONE_BRICKS.defaultBlockState(), footX + 2, footY, footZ, chunkBB);
               this.placeBlock(level, Blocks.IRON_DOOR.defaultBlockState(), footX + 1, footY, footZ, chunkBB);
               this.placeBlock(
                  level, Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), footX + 1, footY + 1, footZ, chunkBB
               );
               this.placeBlock(
                  level, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH), footX + 2, footY + 1, footZ + 1, chunkBB
               );
               this.placeBlock(
                  level, Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH), footX + 2, footY + 1, footZ - 1, chunkBB
               );
         }
      }

      protected StrongholdPieces.StrongholdPiece.SmallDoorType randomSmallDoor(final RandomSource random) {
         int selection = random.nextInt(5);
         switch (selection) {
            case 0:
            case 1:
            default:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
            case 2:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.WOOD_DOOR;
            case 3:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES;
            case 4:
               return StrongholdPieces.StrongholdPiece.SmallDoorType.IRON_DOOR;
         }
      }

      protected @Nullable StructurePiece generateSmallDoorChildForward(
         final StrongholdPieces.StartPiece startPiece,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int xOff,
         final int yOff
      ) {
         Direction orientation = this.getOrientation();
         if (orientation != null) {
            switch (orientation) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + xOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() - 1,
                     orientation,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + xOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.maxZ() + 1,
                     orientation,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + xOff,
                     orientation,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + xOff,
                     orientation,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateSmallDoorChildLeft(
         final StrongholdPieces.StartPiece startPiece,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int yOff,
         final int zOff
      ) {
         Direction orientation = this.getOrientation();
         if (orientation != null) {
            switch (orientation) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateSmallDoorChildRight(
         final StrongholdPieces.StartPiece startPiece,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int yOff,
         final int zOff
      ) {
         Direction orientation = this.getOrientation();
         if (orientation != null) {
            switch (orientation) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected static boolean isOkBox(final BoundingBox box) {
         return box.minY() > 10;
      }

      protected enum SmallDoorType {
         OPENING,
         WOOD_DOOR,
         GRATES,
         IRON_DOOR;

         @Deprecated
         public static final Codec<StrongholdPieces.StrongholdPiece.SmallDoorType> LEGACY_CODEC = ExtraCodecs.legacyEnum(
            StrongholdPieces.StrongholdPiece.SmallDoorType::valueOf
         );
      }
   }

   public abstract static class Turn extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 5;
      protected static final int HEIGHT = 5;
      protected static final int DEPTH = 5;

      protected Turn(final StructurePieceType type, final int genDepth, final BoundingBox boundingBox) {
         super(type, genDepth, boundingBox);
      }

      public Turn(final StructurePieceType type, final CompoundTag tag) {
         super(type, tag);
      }
   }
}
