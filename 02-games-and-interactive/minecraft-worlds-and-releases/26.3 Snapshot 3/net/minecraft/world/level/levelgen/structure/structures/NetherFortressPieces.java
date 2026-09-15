package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class NetherFortressPieces {
   private static final int MAX_DEPTH = 30;
   private static final int LOWEST_Y_POSITION = 10;
   public static final int MAGIC_START_Y = 64;
   private static final NetherFortressPieces.PieceWeight[] BRIDGE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeStraight.class, 30, 0, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeCrossing.class, 10, 4),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.RoomCrossing.class, 10, 4),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.StairsRoom.class, 10, 3),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.MonsterThrone.class, 5, 2),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleEntrance.class, 5, 1)
   };
   private static final NetherFortressPieces.PieceWeight[] CASTLE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorPiece.class, 25, 0, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorCrossingPiece.class, 15, 5),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class, 5, 10),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class, 5, 10),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorStairsPiece.class, 10, 3, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorTBalconyPiece.class, 7, 2),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleStalkRoom.class, 5, 2)
   };

   private static NetherFortressPieces.@Nullable NetherBridgePiece findAndCreateBridgePieceFactory(
      final NetherFortressPieces.PieceWeight piece,
      final StructurePieceAccessor structurePieceAccessor,
      final RandomSource random,
      final int footX,
      final int footY,
      final int footZ,
      final Direction direction,
      final int depth
   ) {
      Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass = piece.pieceClass;
      NetherFortressPieces.NetherBridgePiece structurePiece = null;
      if (pieceClass == NetherFortressPieces.BridgeStraight.class) {
         structurePiece = NetherFortressPieces.BridgeStraight.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.BridgeCrossing.class) {
         structurePiece = NetherFortressPieces.BridgeCrossing.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.RoomCrossing.class) {
         structurePiece = NetherFortressPieces.RoomCrossing.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.StairsRoom.class) {
         structurePiece = NetherFortressPieces.StairsRoom.createPiece(structurePieceAccessor, footX, footY, footZ, depth, direction);
      } else if (pieceClass == NetherFortressPieces.MonsterThrone.class) {
         structurePiece = NetherFortressPieces.MonsterThrone.createPiece(structurePieceAccessor, footX, footY, footZ, depth, direction);
      } else if (pieceClass == NetherFortressPieces.CastleEntrance.class) {
         structurePiece = NetherFortressPieces.CastleEntrance.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.CastleSmallCorridorPiece.class) {
         structurePiece = NetherFortressPieces.CastleSmallCorridorPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class) {
         structurePiece = NetherFortressPieces.CastleSmallCorridorRightTurnPiece.createPiece(
            structurePieceAccessor, random, footX, footY, footZ, direction, depth
         );
      } else if (pieceClass == NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class) {
         structurePiece = NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.createPiece(
            structurePieceAccessor, random, footX, footY, footZ, direction, depth
         );
      } else if (pieceClass == NetherFortressPieces.CastleCorridorStairsPiece.class) {
         structurePiece = NetherFortressPieces.CastleCorridorStairsPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.CastleCorridorTBalconyPiece.class) {
         structurePiece = NetherFortressPieces.CastleCorridorTBalconyPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.CastleSmallCorridorCrossingPiece.class) {
         structurePiece = NetherFortressPieces.CastleSmallCorridorCrossingPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      } else if (pieceClass == NetherFortressPieces.CastleStalkRoom.class) {
         structurePiece = NetherFortressPieces.CastleStalkRoom.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
      }

      return structurePiece;
   }

   public static class BridgeCrossing extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 19;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 19;

      public BridgeCrossing(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      protected BridgeCrossing(final int west, final int north, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(west, 64, north, direction, 19, 10, 19));
         this.setOrientation(direction);
      }

      protected BridgeCrossing(final StructurePieceType type, final CompoundTag tag) {
         super(type, tag);
      }

      public BridgeCrossing(final CompoundTag tag) {
         this(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 8, 3, false);
         this.generateChildLeft((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 3, 8, false);
         this.generateChildRight((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 3, 8, false);
      }

      public static NetherFortressPieces.@Nullable BridgeCrossing createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -8, -3, 0, 19, 10, 19, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.BridgeCrossing(genDepth, box, direction)
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
         this.generateBox(level, chunkBB, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 5, 0, 10, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 8, 18, 7, 10, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 7; x <= 11; x++) {
            for (int z = 0; z <= 2; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 18 - z, chunkBB);
            }
         }

         this.generateBox(level, chunkBB, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 0; x <= 2; x++) {
            for (int z = 7; z <= 11; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), 18 - x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class BridgeEndFiller extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 8;
      private final int selfSeed;

      public BridgeEndFiller(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, genDepth, boundingBox);
         this.setOrientation(direction);
         this.selfSeed = random.nextInt();
      }

      public BridgeEndFiller(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, tag);
         this.selfSeed = tag.getIntOr("Seed", 0);
      }

      public static NetherFortressPieces.@Nullable BridgeEndFiller createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -3, 0, 5, 10, 8, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.BridgeEndFiller(genDepth, random, box, direction)
            : null;
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putInt("Seed", this.selfSeed);
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
         RandomSource selfRandom = RandomSource.createThreadLocalInstance(this.selfSeed);

         for (int x = 0; x <= 4; x++) {
            for (int y = 3; y <= 4; y++) {
               int z = selfRandom.nextInt(8);
               this.generateBox(level, chunkBB, x, y, 0, x, y, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            }
         }

         int z = selfRandom.nextInt(8);
         this.generateBox(level, chunkBB, 0, 5, 0, 0, 5, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         z = selfRandom.nextInt(8);
         this.generateBox(level, chunkBB, 4, 5, 0, 4, 5, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 0; x <= 4; x++) {
            int zx = selfRandom.nextInt(5);
            this.generateBox(level, chunkBB, x, 2, 0, x, 2, zx, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         }

         for (int x = 0; x <= 4; x++) {
            for (int y = 0; y <= 1; y++) {
               int zx = selfRandom.nextInt(3);
               this.generateBox(level, chunkBB, x, y, 0, x, y, zx, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            }
         }
      }
   }

   public static class BridgeStraight extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 19;

      public BridgeStraight(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public BridgeStraight(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 3, false);
      }

      public static NetherFortressPieces.@Nullable BridgeStraight createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -3, 0, 5, 10, 19, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.BridgeStraight(genDepth, random, box, direction)
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
         this.generateBox(level, chunkBB, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 5, 0, 3, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 2; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 18 - z, chunkBB);
            }
         }

         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         BlockState nseFence = nsFence.setValue(FenceBlock.EAST, true);
         BlockState nswFence = nsFence.setValue(FenceBlock.WEST, true);
         this.generateBox(level, chunkBB, 0, 1, 1, 0, 4, 1, nseFence, nseFence, false);
         this.generateBox(level, chunkBB, 0, 3, 4, 0, 4, 4, nseFence, nseFence, false);
         this.generateBox(level, chunkBB, 0, 3, 14, 0, 4, 14, nseFence, nseFence, false);
         this.generateBox(level, chunkBB, 0, 1, 17, 0, 4, 17, nseFence, nseFence, false);
         this.generateBox(level, chunkBB, 4, 1, 1, 4, 4, 1, nswFence, nswFence, false);
         this.generateBox(level, chunkBB, 4, 3, 4, 4, 4, 4, nswFence, nswFence, false);
         this.generateBox(level, chunkBB, 4, 3, 14, 4, 4, 14, nswFence, nswFence, false);
         this.generateBox(level, chunkBB, 4, 1, 17, 4, 4, 17, nswFence, nswFence, false);
      }
   }

   public static class CastleCorridorStairsPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 10;

      public CastleCorridorStairsPiece(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public CastleCorridorStairsPiece(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 0, true);
      }

      public static NetherFortressPieces.@Nullable CastleCorridorStairsPiece createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -7, 0, 5, 14, 10, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleCorridorStairsPiece(genDepth, box, direction)
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
         BlockState stairs = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);

         for (int step = 0; step <= 9; step++) {
            int floor = Math.max(1, 7 - step);
            int roof = Math.min(Math.max(floor + 5, 14 - step), 13);
            int z = step;
            this.generateBox(level, chunkBB, 0, 0, z, 4, floor, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, floor + 1, z, 3, roof - 1, z, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            if (step <= 6) {
               this.placeBlock(level, stairs, 1, floor + 1, z, chunkBB);
               this.placeBlock(level, stairs, 2, floor + 1, z, chunkBB);
               this.placeBlock(level, stairs, 3, floor + 1, z, chunkBB);
            }

            this.generateBox(level, chunkBB, 0, roof, z, 4, roof, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(
               level, chunkBB, 0, floor + 1, z, 0, roof - 1, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
            this.generateBox(
               level, chunkBB, 4, floor + 1, z, 4, roof - 1, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
            if ((step & 1) == 0) {
               this.generateBox(level, chunkBB, 0, floor + 2, z, 0, floor + 3, z, nsFence, nsFence, false);
               this.generateBox(level, chunkBB, 4, floor + 2, z, 4, floor + 3, z, nsFence, nsFence, false);
            }

            for (int x = 0; x <= 4; x++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class CastleCorridorTBalconyPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 9;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 9;

      public CastleCorridorTBalconyPiece(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public CastleCorridorTBalconyPiece(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         int zOff = 1;
         Direction orientation = this.getOrientation();
         if (orientation == Direction.WEST || orientation == Direction.NORTH) {
            zOff = 5;
         }

         this.generateChildLeft((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, zOff, random.nextInt(8) > 0);
         this.generateChildRight((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, zOff, random.nextInt(8) > 0);
      }

      public static NetherFortressPieces.@Nullable CastleCorridorTBalconyPiece createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -3, 0, 0, 9, 7, 9, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleCorridorTBalconyPiece(genDepth, box, direction)
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
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         this.generateBox(level, chunkBB, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 8, 5, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 3, 0, 1, 4, 0, weFence, weFence, false);
         this.generateBox(level, chunkBB, 7, 3, 0, 7, 4, 0, weFence, weFence, false);
         this.generateBox(level, chunkBB, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 1, 4, 2, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 1, 4, 7, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 3, 8, 7, 3, 8, weFence, weFence, false);
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.SOUTH, true), 0, 3, 8, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.SOUTH, true), 8, 3, 8, chunkBB
         );
         this.generateBox(level, chunkBB, 0, 3, 6, 0, 3, 7, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 8, 3, 6, 8, 3, 7, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 4, 5, 1, 5, 5, weFence, weFence, false);
         this.generateBox(level, chunkBB, 7, 4, 5, 7, 5, 5, weFence, weFence, false);

         for (int z = 0; z <= 5; z++) {
            for (int x = 0; x <= 8; x++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class CastleEntrance extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 13;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 13;

      public CastleEntrance(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public CastleEntrance(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 5, 3, true);
      }

      public static NetherFortressPieces.@Nullable CastleEntrance createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -5, -3, 0, 13, 14, 13, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleEntrance(genDepth, random, box, direction)
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
         this.generateBox(level, chunkBB, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.defaultBlockState(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), false);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);

         for (int i = 1; i <= 11; i += 2) {
            this.generateBox(level, chunkBB, i, 10, 0, i, 11, 0, weFence, weFence, false);
            this.generateBox(level, chunkBB, i, 10, 12, i, 11, 12, weFence, weFence, false);
            this.generateBox(level, chunkBB, 0, 10, i, 0, 11, i, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 12, 10, i, 12, 11, i, nsFence, nsFence, false);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, chunkBB);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, chunkBB);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, chunkBB);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, chunkBB);
            if (i != 11) {
               this.placeBlock(level, weFence, i + 1, 13, 0, chunkBB);
               this.placeBlock(level, weFence, i + 1, 13, 12, chunkBB);
               this.placeBlock(level, nsFence, 0, 13, i + 1, chunkBB);
               this.placeBlock(level, nsFence, 12, 13, i + 1, chunkBB);
            }
         }

         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 0, 13, 0, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 0, 13, 12, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true), 12, 13, 12, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 12, 13, 0, chunkBB
         );

         for (int z = 3; z <= 9; z += 2) {
            this.generateBox(level, chunkBB, 1, 7, z, 1, 8, z, nsFence.setValue(FenceBlock.WEST, true), nsFence.setValue(FenceBlock.WEST, true), false);
            this.generateBox(level, chunkBB, 11, 7, z, 11, 8, z, nsFence.setValue(FenceBlock.EAST, true), nsFence.setValue(FenceBlock.EAST, true), false);
         }

         this.generateBox(level, chunkBB, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 4; x <= 8; x++) {
            for (int z = 0; z <= 2; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 12 - z, chunkBB);
            }
         }

         for (int x = 0; x <= 2; x++) {
            for (int z = 4; z <= 8; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - x, -1, z, chunkBB);
            }
         }

         this.generateBox(level, chunkBB, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 1, 6, 6, 4, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 6, 0, 6, chunkBB);
         this.placeBlock(level, Blocks.LAVA.defaultBlockState(), 6, 5, 6, chunkBB);
         BlockPos pos = this.getWorldPos(6, 5, 6);
         if (chunkBB.isInside(pos)) {
            level.scheduleTick(pos, Fluids.LAVA, 0);
         }
      }
   }

   public static class CastleSmallCorridorCrossingPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;

      public CastleSmallCorridorCrossingPiece(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public CastleSmallCorridorCrossingPiece(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 0, true);
         this.generateChildLeft((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
         this.generateChildRight((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorCrossingPiece createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleSmallCorridorCrossingPiece(genDepth, box, direction)
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class CastleSmallCorridorLeftTurnPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;
      private boolean isNeedingChest;

      public CastleSmallCorridorLeftTurnPiece(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, genDepth, boundingBox);
         this.setOrientation(direction);
         this.isNeedingChest = random.nextInt(3) == 0;
      }

      public CastleSmallCorridorLeftTurnPiece(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, tag);
         this.isNeedingChest = tag.getBooleanOr("Chest", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Chest", this.isNeedingChest);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildLeft((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorLeftTurnPiece createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleSmallCorridorLeftTurnPiece(genDepth, random, box, direction)
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 3, 1, 4, 4, 1, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 4, 3, 3, 4, 4, 3, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 3, 4, 1, 4, 4, weFence, weFence, false);
         this.generateBox(level, chunkBB, 3, 3, 4, 3, 4, 4, weFence, weFence, false);
         if (this.isNeedingChest && chunkBB.isInside(this.getWorldPos(3, 2, 3))) {
            this.isNeedingChest = false;
            this.createChest(level, chunkBB, random, 3, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
         }

         this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class CastleSmallCorridorPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;

      public CastleSmallCorridorPiece(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public CastleSmallCorridorPiece(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 1, 0, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorPiece createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleSmallCorridorPiece(genDepth, box, direction)
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 3, 1, 0, 4, 1, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 0, 3, 3, 0, 4, 3, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 4, 3, 1, 4, 4, 1, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 4, 3, 3, 4, 4, 3, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class CastleSmallCorridorRightTurnPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;
      private boolean isNeedingChest;

      public CastleSmallCorridorRightTurnPiece(final int genDepth, final RandomSource random, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, genDepth, boundingBox);
         this.setOrientation(direction);
         this.isNeedingChest = random.nextInt(3) == 0;
      }

      public CastleSmallCorridorRightTurnPiece(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, tag);
         this.isNeedingChest = tag.getBooleanOr("Chest", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Chest", this.isNeedingChest);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildRight((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorRightTurnPiece createPiece(
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleSmallCorridorRightTurnPiece(genDepth, random, box, direction)
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
         this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 3, 1, 0, 4, 1, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 0, 3, 3, 0, 4, 3, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 3, 4, 1, 4, 4, weFence, weFence, false);
         this.generateBox(level, chunkBB, 3, 3, 4, 3, 4, 4, weFence, weFence, false);
         if (this.isNeedingChest && chunkBB.isInside(this.getWorldPos(1, 2, 3))) {
            this.isNeedingChest = false;
            this.createChest(level, chunkBB, random, 1, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
         }

         this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class CastleStalkRoom extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 13;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 13;

      public CastleStalkRoom(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public CastleStalkRoom(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 5, 3, true);
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 5, 11, true);
      }

      public static NetherFortressPieces.@Nullable CastleStalkRoom createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -5, -3, 0, 13, 14, 13, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.CastleStalkRoom(genDepth, box, direction)
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
         this.generateBox(level, chunkBB, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         BlockState nswFence = nsFence.setValue(FenceBlock.WEST, true);
         BlockState nseFence = nsFence.setValue(FenceBlock.EAST, true);

         for (int i = 1; i <= 11; i += 2) {
            this.generateBox(level, chunkBB, i, 10, 0, i, 11, 0, weFence, weFence, false);
            this.generateBox(level, chunkBB, i, 10, 12, i, 11, 12, weFence, weFence, false);
            this.generateBox(level, chunkBB, 0, 10, i, 0, 11, i, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 12, 10, i, 12, 11, i, nsFence, nsFence, false);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, chunkBB);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, chunkBB);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, chunkBB);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, chunkBB);
            if (i != 11) {
               this.placeBlock(level, weFence, i + 1, 13, 0, chunkBB);
               this.placeBlock(level, weFence, i + 1, 13, 12, chunkBB);
               this.placeBlock(level, nsFence, 0, 13, i + 1, chunkBB);
               this.placeBlock(level, nsFence, 12, 13, i + 1, chunkBB);
            }
         }

         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 0, 13, 0, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 0, 13, 12, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true), 12, 13, 12, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 12, 13, 0, chunkBB
         );

         for (int z = 3; z <= 9; z += 2) {
            this.generateBox(level, chunkBB, 1, 7, z, 1, 8, z, nswFence, nswFence, false);
            this.generateBox(level, chunkBB, 11, 7, z, 11, 8, z, nseFence, nseFence, false);
         }

         BlockState stairs = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);

         for (int i = 0; i <= 6; i++) {
            int z = i + 4;

            for (int x = 5; x <= 7; x++) {
               this.placeBlock(level, stairs, x, 5 + i, z, chunkBB);
            }

            if (z >= 5 && z <= 8) {
               this.generateBox(level, chunkBB, 5, 5, z, 7, i + 4, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            } else if (z >= 9 && z <= 10) {
               this.generateBox(level, chunkBB, 5, 8, z, 7, i + 4, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            }

            if (i >= 1) {
               this.generateBox(level, chunkBB, 5, 6 + i, z, 7, 9 + i, z, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            }
         }

         for (int x = 5; x <= 7; x++) {
            this.placeBlock(level, stairs, x, 12, 11, chunkBB);
         }

         this.generateBox(level, chunkBB, 5, 6, 7, 5, 7, 7, nseFence, nseFence, false);
         this.generateBox(level, chunkBB, 7, 6, 7, 7, 7, 7, nswFence, nswFence, false);
         this.generateBox(level, chunkBB, 5, 13, 12, 7, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState eastStairs = stairs.setValue(StairBlock.FACING, Direction.EAST);
         BlockState westStairs = stairs.setValue(StairBlock.FACING, Direction.WEST);
         this.placeBlock(level, westStairs, 4, 5, 2, chunkBB);
         this.placeBlock(level, westStairs, 4, 5, 3, chunkBB);
         this.placeBlock(level, westStairs, 4, 5, 9, chunkBB);
         this.placeBlock(level, westStairs, 4, 5, 10, chunkBB);
         this.placeBlock(level, eastStairs, 8, 5, 2, chunkBB);
         this.placeBlock(level, eastStairs, 8, 5, 3, chunkBB);
         this.placeBlock(level, eastStairs, 8, 5, 9, chunkBB);
         this.placeBlock(level, eastStairs, 8, 5, 10, chunkBB);
         this.generateBox(level, chunkBB, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int x = 4; x <= 8; x++) {
            for (int z = 0; z <= 2; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 12 - z, chunkBB);
            }
         }

         for (int x = 0; x <= 2; x++) {
            for (int z = 4; z <= 8; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class MonsterThrone extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 8;
      private static final int DEPTH = 9;
      private boolean hasPlacedSpawner;

      public MonsterThrone(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public MonsterThrone(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, tag);
         this.hasPlacedSpawner = tag.getBooleanOr("Mob", false);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.putBoolean("Mob", this.hasPlacedSpawner);
      }

      public static NetherFortressPieces.@Nullable MonsterThrone createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final int genDepth, final Direction direction
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -2, 0, 0, 7, 8, 9, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null
            ? new NetherFortressPieces.MonsterThrone(genDepth, box, direction)
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
         this.generateBox(level, chunkBB, 0, 2, 0, 6, 7, 7, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.placeBlock(level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 1, 6, 3, chunkBB);
         this.placeBlock(level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 5, 6, 3, chunkBB);
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.NORTH, true), 0, 6, 3, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.NORTH, true), 6, 6, 3, chunkBB
         );
         this.generateBox(level, chunkBB, 0, 6, 4, 0, 6, 7, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 6, 6, 4, 6, 6, 7, nsFence, nsFence, false);
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.SOUTH, true), 0, 6, 8, chunkBB
         );
         this.placeBlock(
            level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.SOUTH, true), 6, 6, 8, chunkBB
         );
         this.generateBox(level, chunkBB, 1, 6, 8, 5, 6, 8, weFence, weFence, false);
         this.placeBlock(level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 1, 7, 8, chunkBB);
         this.generateBox(level, chunkBB, 2, 7, 8, 4, 7, 8, weFence, weFence, false);
         this.placeBlock(level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 5, 7, 8, chunkBB);
         this.placeBlock(level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 2, 8, 8, chunkBB);
         this.placeBlock(level, weFence, 3, 8, 8, chunkBB);
         this.placeBlock(level, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 4, 8, 8, chunkBB);
         if (!this.hasPlacedSpawner) {
            BlockPos pos = this.getWorldPos(3, 5, 5);
            if (chunkBB.isInside(pos)) {
               this.hasPlacedSpawner = true;
               level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
               if (level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
                  spawner.setEntityId(EntityTypes.BLAZE, random);
               }
            }
         }

         for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   private abstract static class NetherBridgePiece extends StructurePiece {
      protected NetherBridgePiece(final StructurePieceType type, final int genDepth, final BoundingBox boundingBox) {
         super(type, genDepth, boundingBox);
      }

      public NetherBridgePiece(final StructurePieceType type, final CompoundTag tag) {
         super(type, tag);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
      }

      private int updatePieceWeight(final List<NetherFortressPieces.PieceWeight> currentPieces) {
         boolean hasAnyPieces = false;
         int totalWeight = 0;

         for (NetherFortressPieces.PieceWeight piece : currentPieces) {
            if (piece.maxPlaceCount > 0 && piece.placeCount < piece.maxPlaceCount) {
               hasAnyPieces = true;
            }

            totalWeight += piece.weight;
         }

         return hasAnyPieces ? totalWeight : -1;
      }

      private NetherFortressPieces.@Nullable NetherBridgePiece generatePiece(
         final NetherFortressPieces.StartPiece startPiece,
         final List<NetherFortressPieces.PieceWeight> currentPieces,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int depth
      ) {
         int totalWeight = this.updatePieceWeight(currentPieces);
         boolean doStuff = totalWeight > 0 && depth <= 30;
         int numAttempts = 0;

         while (numAttempts < 5 && doStuff) {
            numAttempts++;
            int weightSelection = random.nextInt(totalWeight);

            for (NetherFortressPieces.PieceWeight piece : currentPieces) {
               weightSelection -= piece.weight;
               if (weightSelection < 0) {
                  if (!piece.doPlace(depth) || piece == startPiece.previousPiece && !piece.allowInRow) {
                     break;
                  }

                  NetherFortressPieces.NetherBridgePiece structurePiece = NetherFortressPieces.findAndCreateBridgePieceFactory(
                     piece, structurePieceAccessor, random, footX, footY, footZ, direction, depth
                  );
                  if (structurePiece != null) {
                     piece.placeCount++;
                     startPiece.previousPiece = piece;
                     if (!piece.isValid()) {
                        currentPieces.remove(piece);
                     }

                     return structurePiece;
                  }
               }
            }
         }

         return NetherFortressPieces.BridgeEndFiller.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
      }

      private @Nullable StructurePiece generateAndAddPiece(
         final NetherFortressPieces.StartPiece startPiece,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int footX,
         final int footY,
         final int footZ,
         final Direction direction,
         final int depth,
         final boolean isCastle
      ) {
         if (Math.abs(footX - startPiece.getBoundingBox().minX()) <= 112 && Math.abs(footZ - startPiece.getBoundingBox().minZ()) <= 112) {
            List<NetherFortressPieces.PieceWeight> availablePieces = startPiece.availableBridgePieces;
            if (isCastle) {
               availablePieces = startPiece.availableCastlePieces;
            }

            StructurePiece newPiece = this.generatePiece(startPiece, availablePieces, structurePieceAccessor, random, footX, footY, footZ, direction, depth + 1);
            if (newPiece != null) {
               structurePieceAccessor.addPiece(newPiece);
               startPiece.pendingChildren.add(newPiece);
            }

            return newPiece;
         } else {
            return NetherFortressPieces.BridgeEndFiller.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
         }
      }

      protected @Nullable StructurePiece generateChildForward(
         final NetherFortressPieces.StartPiece startPiece,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int xOff,
         final int yOff,
         final boolean isCastle
      ) {
         Direction orientation = this.getOrientation();
         if (orientation != null) {
            switch (orientation) {
               case NORTH:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + xOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() - 1,
                     orientation,
                     this.getGenDepth(),
                     isCastle
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + xOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.maxZ() + 1,
                     orientation,
                     this.getGenDepth(),
                     isCastle
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + xOff,
                     orientation,
                     this.getGenDepth(),
                     isCastle
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + xOff,
                     orientation,
                     this.getGenDepth(),
                     isCastle
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateChildLeft(
         final NetherFortressPieces.StartPiece startPiece,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int yOff,
         final int zOff,
         final boolean isCastle
      ) {
         Direction orientation = this.getOrientation();
         if (orientation != null) {
            switch (orientation) {
               case NORTH:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.WEST,
                     this.getGenDepth(),
                     isCastle
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.WEST,
                     this.getGenDepth(),
                     isCastle
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth(),
                     isCastle
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth(),
                     isCastle
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateChildRight(
         final NetherFortressPieces.StartPiece startPiece,
         final StructurePieceAccessor structurePieceAccessor,
         final RandomSource random,
         final int yOff,
         final int zOff,
         final boolean isCastle
      ) {
         Direction orientation = this.getOrientation();
         if (orientation != null) {
            switch (orientation) {
               case NORTH:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.EAST,
                     this.getGenDepth(),
                     isCastle
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.minZ() + zOff,
                     Direction.EAST,
                     this.getGenDepth(),
                     isCastle
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth(),
                     isCastle
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     startPiece,
                     structurePieceAccessor,
                     random,
                     this.boundingBox.minX() + zOff,
                     this.boundingBox.minY() + yOff,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth(),
                     isCastle
                  );
            }
         }

         return null;
      }

      protected static boolean isOkBox(final BoundingBox box) {
         return box.minY() > 10;
      }
   }

   private static class PieceWeight {
      public final Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass;
      public final int weight;
      public int placeCount;
      public final int maxPlaceCount;
      public final boolean allowInRow;

      public PieceWeight(
         final Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass, final int weight, final int maxPlaceCount, final boolean allowInRow
      ) {
         this.pieceClass = pieceClass;
         this.weight = weight;
         this.maxPlaceCount = maxPlaceCount;
         this.allowInRow = allowInRow;
      }

      public PieceWeight(final Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass, final int weight, final int maxPlaceCount) {
         this(pieceClass, weight, maxPlaceCount, false);
      }

      public boolean doPlace(final int depth) {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }

      public boolean isValid() {
         return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
      }
   }

   public static class RoomCrossing extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 9;
      private static final int DEPTH = 7;

      public RoomCrossing(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public RoomCrossing(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildForward((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 2, 0, false);
         this.generateChildLeft((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, 2, false);
         this.generateChildRight((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 0, 2, false);
      }

      public static NetherFortressPieces.@Nullable RoomCrossing createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final Direction direction, final int genDepth
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -2, 0, 0, 7, 9, 7, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null ? new NetherFortressPieces.RoomCrossing(genDepth, box, direction) : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 6, 7, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(level, chunkBB, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 0, 4, 5, 0, weFence, weFence, false);
         this.generateBox(level, chunkBB, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 6, 4, 5, 6, weFence, weFence, false);
         this.generateBox(level, chunkBB, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 5, 2, 0, 5, 4, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 5, 2, 6, 5, 4, nsFence, nsFence, false);

         for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class StairsRoom extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 7;

      public StairsRoom(final int genDepth, final BoundingBox boundingBox, final Direction direction) {
         super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, genDepth, boundingBox);
         this.setOrientation(direction);
      }

      public StairsRoom(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, tag);
      }

      @Override
      public void addChildren(final StructurePiece startPiece, final StructurePieceAccessor structurePieceAccessor, final RandomSource random) {
         this.generateChildRight((NetherFortressPieces.StartPiece)startPiece, structurePieceAccessor, random, 6, 2, false);
      }

      public static NetherFortressPieces.@Nullable StairsRoom createPiece(
         final StructurePieceAccessor structurePieceAccessor, final int footX, final int footY, final int footZ, final int genDepth, final Direction direction
      ) {
         BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -2, 0, 0, 7, 11, 7, direction);
         return isOkBox(box) && structurePieceAccessor.findCollisionPiece(box) == null ? new NetherFortressPieces.StairsRoom(genDepth, box, direction) : null;
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
         this.generateBox(level, chunkBB, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 6, 10, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState weFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState nsFence = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(level, chunkBB, 0, 3, 2, 0, 5, 4, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 6, 3, 2, 6, 5, 2, nsFence, nsFence, false);
         this.generateBox(level, chunkBB, 6, 3, 4, 6, 5, 4, nsFence, nsFence, false);
         this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 5, 2, 5, chunkBB);
         this.generateBox(level, chunkBB, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 6, 8, 2, 6, 8, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(level, chunkBB, 2, 5, 0, 4, 5, 0, weFence, weFence, false);

         for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
               this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
            }
         }
      }
   }

   public static class StartPiece extends NetherFortressPieces.BridgeCrossing {
      private NetherFortressPieces.@Nullable PieceWeight previousPiece;
      private final List<NetherFortressPieces.PieceWeight> availableBridgePieces = new ArrayList<>();
      private final List<NetherFortressPieces.PieceWeight> availableCastlePieces = new ArrayList<>();
      public final List<StructurePiece> pendingChildren = Lists.newArrayList();

      public StartPiece(final RandomSource random, final int west, final int north) {
         super(west, north, getRandomHorizontalDirection(random));

         for (NetherFortressPieces.PieceWeight piece : NetherFortressPieces.BRIDGE_PIECE_WEIGHTS) {
            piece.placeCount = 0;
            this.availableBridgePieces.add(piece);
         }

         for (NetherFortressPieces.PieceWeight piece : NetherFortressPieces.CASTLE_PIECE_WEIGHTS) {
            piece.placeCount = 0;
            this.availableCastlePieces.add(piece);
         }
      }

      public StartPiece(final CompoundTag tag) {
         super(StructurePieceType.NETHER_FORTRESS_START, tag);
      }
   }
}
