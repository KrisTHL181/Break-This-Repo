package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
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
   static final NetherFortressPieces.PieceWeight[] BRIDGE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeStraight.class, 30, 0, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.BridgeCrossing.class, 10, 4),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.RoomCrossing.class, 10, 4),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.StairsRoom.class, 10, 3),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.MonsterThrone.class, 5, 2),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleEntrance.class, 5, 1)
   };
   static final NetherFortressPieces.PieceWeight[] CASTLE_PIECE_WEIGHTS = new NetherFortressPieces.PieceWeight[]{
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorPiece.class, 25, 0, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorCrossingPiece.class, 15, 5),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class, 5, 10),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class, 5, 10),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorStairsPiece.class, 10, 3, true),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleCorridorTBalconyPiece.class, 7, 2),
      new NetherFortressPieces.PieceWeight(NetherFortressPieces.CastleStalkRoom.class, 5, 2)
   };

   static NetherFortressPieces.@Nullable NetherBridgePiece findAndCreateBridgePieceFactory(
      NetherFortressPieces.PieceWeight p_228008_,
      StructurePieceAccessor p_228009_,
      RandomSource p_228010_,
      int p_228011_,
      int p_228012_,
      int p_228013_,
      Direction p_228014_,
      int p_228015_
   ) {
      Class<? extends NetherFortressPieces.NetherBridgePiece> oclass = p_228008_.pieceClass;
      NetherFortressPieces.NetherBridgePiece netherfortresspieces$netherbridgepiece = null;
      if (oclass == NetherFortressPieces.BridgeStraight.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.BridgeStraight.createPiece(
            p_228009_, p_228010_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.BridgeCrossing.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.BridgeCrossing.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.RoomCrossing.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.RoomCrossing.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.StairsRoom.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.StairsRoom.createPiece(p_228009_, p_228011_, p_228012_, p_228013_, p_228015_, p_228014_);
      } else if (oclass == NetherFortressPieces.MonsterThrone.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.MonsterThrone.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228015_, p_228014_
         );
      } else if (oclass == NetherFortressPieces.CastleEntrance.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleEntrance.createPiece(
            p_228009_, p_228010_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.CastleSmallCorridorPiece.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorPiece.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.CastleSmallCorridorRightTurnPiece.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorRightTurnPiece.createPiece(
            p_228009_, p_228010_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorLeftTurnPiece.createPiece(
            p_228009_, p_228010_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.CastleCorridorStairsPiece.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleCorridorStairsPiece.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.CastleCorridorTBalconyPiece.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleCorridorTBalconyPiece.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.CastleSmallCorridorCrossingPiece.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleSmallCorridorCrossingPiece.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      } else if (oclass == NetherFortressPieces.CastleStalkRoom.class) {
         netherfortresspieces$netherbridgepiece = NetherFortressPieces.CastleStalkRoom.createPiece(
            p_228009_, p_228011_, p_228012_, p_228013_, p_228014_, p_228015_
         );
      }

      return netherfortresspieces$netherbridgepiece;
   }

   public static class BridgeCrossing extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 19;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 19;

      public BridgeCrossing(int p_228026_, BoundingBox p_228027_, Direction p_228028_) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, p_228026_, p_228027_);
         this.setOrientation(p_228028_);
      }

      protected BridgeCrossing(int p_228022_, int p_228023_, Direction p_228024_) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(p_228022_, 64, p_228023_, p_228024_, 19, 10, 19));
         this.setOrientation(p_228024_);
      }

      protected BridgeCrossing(StructurePieceType p_228030_, CompoundTag p_228031_) {
         super(p_228030_, p_228031_);
      }

      public BridgeCrossing(CompoundTag p_228033_) {
         this(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, p_228033_);
      }

      @Override
      public void addChildren(StructurePiece p_228043_, StructurePieceAccessor p_228044_, RandomSource p_228045_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228043_, p_228044_, p_228045_, 8, 3, false);
         this.generateChildLeft((NetherFortressPieces.StartPiece)p_228043_, p_228044_, p_228045_, 3, 8, false);
         this.generateChildRight((NetherFortressPieces.StartPiece)p_228043_, p_228044_, p_228045_, 3, 8, false);
      }

      public static NetherFortressPieces.@Nullable BridgeCrossing createPiece(
         StructurePieceAccessor p_228047_, int p_228048_, int p_228049_, int p_228050_, Direction p_228051_, int p_228052_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228048_, p_228049_, p_228050_, -8, -3, 0, 19, 10, 19, p_228051_);
         return isOkBox(boundingbox) && p_228047_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.BridgeCrossing(p_228052_, boundingbox, p_228051_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228035_,
         StructureManager p_228036_,
         ChunkGenerator p_228037_,
         RandomSource p_228038_,
         BoundingBox p_228039_,
         ChunkPos p_228040_,
         BlockPos p_228041_
      ) {
         this.generateBox(p_228035_, p_228039_, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 8, 5, 0, 10, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 0, 5, 8, 18, 7, 10, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int i = 7; i <= 11; i++) {
            for (int j = 0; j <= 2; j++) {
               this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228039_);
               this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, p_228039_);
            }
         }

         this.generateBox(p_228035_, p_228039_, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228035_, p_228039_, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int k = 0; k <= 2; k++) {
            for (int l = 7; l <= 11; l++) {
               this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), k, -1, l, p_228039_);
               this.fillColumnDown(p_228035_, Blocks.NETHER_BRICKS.defaultBlockState(), 18 - k, -1, l, p_228039_);
            }
         }
      }
   }

   public static class BridgeEndFiller extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 8;
      private final int selfSeed;

      public BridgeEndFiller(int p_228058_, RandomSource p_228059_, BoundingBox p_228060_, Direction p_228061_) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, p_228058_, p_228060_);
         this.setOrientation(p_228061_);
         this.selfSeed = p_228059_.nextInt();
      }

      public BridgeEndFiller(CompoundTag p_228063_) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, p_228063_);
         this.selfSeed = p_228063_.getIntOr("Seed", 0);
      }

      public static NetherFortressPieces.@Nullable BridgeEndFiller createPiece(
         StructurePieceAccessor p_228073_, RandomSource p_228074_, int p_228075_, int p_228076_, int p_228077_, Direction p_228078_, int p_228079_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228075_, p_228076_, p_228077_, -1, -3, 0, 5, 10, 8, p_228078_);
         return isOkBox(boundingbox) && p_228073_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.BridgeEndFiller(p_228079_, p_228074_, boundingbox, p_228078_)
            : null;
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228081_, CompoundTag p_228082_) {
         super.addAdditionalSaveData(p_228081_, p_228082_);
         p_228082_.putInt("Seed", this.selfSeed);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228065_,
         StructureManager p_228066_,
         ChunkGenerator p_228067_,
         RandomSource p_228068_,
         BoundingBox p_228069_,
         ChunkPos p_228070_,
         BlockPos p_228071_
      ) {
         RandomSource randomsource = RandomSource.create(this.selfSeed);

         for (int i = 0; i <= 4; i++) {
            for (int j = 3; j <= 4; j++) {
               int k = randomsource.nextInt(8);
               this.generateBox(
                  p_228065_, p_228069_, i, j, 0, i, j, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            }
         }

         int l = randomsource.nextInt(8);
         this.generateBox(p_228065_, p_228069_, 0, 5, 0, 0, 5, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         l = randomsource.nextInt(8);
         this.generateBox(p_228065_, p_228069_, 4, 5, 0, 4, 5, l, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int i1 = 0; i1 <= 4; i1++) {
            int k1 = randomsource.nextInt(5);
            this.generateBox(
               p_228065_, p_228069_, i1, 2, 0, i1, 2, k1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
         }

         for (int j1 = 0; j1 <= 4; j1++) {
            for (int l1 = 0; l1 <= 1; l1++) {
               int i2 = randomsource.nextInt(3);
               this.generateBox(
                  p_228065_, p_228069_, j1, l1, 0, j1, l1, i2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            }
         }
      }
   }

   public static class BridgeStraight extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 10;
      private static final int DEPTH = 19;

      public BridgeStraight(int p_228087_, RandomSource p_228088_, BoundingBox p_228089_, Direction p_228090_) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, p_228087_, p_228089_);
         this.setOrientation(p_228090_);
      }

      public BridgeStraight(CompoundTag p_228092_) {
         super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, p_228092_);
      }

      @Override
      public void addChildren(StructurePiece p_228102_, StructurePieceAccessor p_228103_, RandomSource p_228104_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228102_, p_228103_, p_228104_, 1, 3, false);
      }

      public static NetherFortressPieces.@Nullable BridgeStraight createPiece(
         StructurePieceAccessor p_228106_, RandomSource p_228107_, int p_228108_, int p_228109_, int p_228110_, Direction p_228111_, int p_228112_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228108_, p_228109_, p_228110_, -1, -3, 0, 5, 10, 19, p_228111_);
         return isOkBox(boundingbox) && p_228106_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.BridgeStraight(p_228112_, p_228107_, boundingbox, p_228111_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228094_,
         StructureManager p_228095_,
         ChunkGenerator p_228096_,
         RandomSource p_228097_,
         BoundingBox p_228098_,
         ChunkPos p_228099_,
         BlockPos p_228100_
      ) {
         this.generateBox(p_228094_, p_228098_, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228094_, p_228098_, 1, 5, 0, 3, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228094_, p_228098_, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228094_, p_228098_, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228094_, p_228098_, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228094_, p_228098_, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228094_, p_228098_, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228094_, p_228098_, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int i = 0; i <= 4; i++) {
            for (int j = 0; j <= 2; j++) {
               this.fillColumnDown(p_228094_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228098_);
               this.fillColumnDown(p_228094_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, 18 - j, p_228098_);
            }
         }

         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         BlockState blockstate2 = blockstate1.setValue(FenceBlock.EAST, true);
         BlockState blockstate = blockstate1.setValue(FenceBlock.WEST, true);
         this.generateBox(p_228094_, p_228098_, 0, 1, 1, 0, 4, 1, blockstate2, blockstate2, false);
         this.generateBox(p_228094_, p_228098_, 0, 3, 4, 0, 4, 4, blockstate2, blockstate2, false);
         this.generateBox(p_228094_, p_228098_, 0, 3, 14, 0, 4, 14, blockstate2, blockstate2, false);
         this.generateBox(p_228094_, p_228098_, 0, 1, 17, 0, 4, 17, blockstate2, blockstate2, false);
         this.generateBox(p_228094_, p_228098_, 4, 1, 1, 4, 4, 1, blockstate, blockstate, false);
         this.generateBox(p_228094_, p_228098_, 4, 3, 4, 4, 4, 4, blockstate, blockstate, false);
         this.generateBox(p_228094_, p_228098_, 4, 3, 14, 4, 4, 14, blockstate, blockstate, false);
         this.generateBox(p_228094_, p_228098_, 4, 1, 17, 4, 4, 17, blockstate, blockstate, false);
      }
   }

   public static class CastleCorridorStairsPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 10;

      public CastleCorridorStairsPiece(int p_228117_, BoundingBox p_228118_, Direction p_228119_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, p_228117_, p_228118_);
         this.setOrientation(p_228119_);
      }

      public CastleCorridorStairsPiece(CompoundTag p_228121_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, p_228121_);
      }

      @Override
      public void addChildren(StructurePiece p_228131_, StructurePieceAccessor p_228132_, RandomSource p_228133_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228131_, p_228132_, p_228133_, 1, 0, true);
      }

      public static NetherFortressPieces.@Nullable CastleCorridorStairsPiece createPiece(
         StructurePieceAccessor p_228135_, int p_228136_, int p_228137_, int p_228138_, Direction p_228139_, int p_228140_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228136_, p_228137_, p_228138_, -1, -7, 0, 5, 14, 10, p_228139_);
         return isOkBox(boundingbox) && p_228135_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleCorridorStairsPiece(p_228140_, boundingbox, p_228139_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228123_,
         StructureManager p_228124_,
         ChunkGenerator p_228125_,
         RandomSource p_228126_,
         BoundingBox p_228127_,
         ChunkPos p_228128_,
         BlockPos p_228129_
      ) {
         BlockState blockstate = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);

         for (int i = 0; i <= 9; i++) {
            int j = Math.max(1, 7 - i);
            int k = Math.min(Math.max(j + 5, 14 - i), 13);
            int l = i;
            this.generateBox(p_228123_, p_228127_, 0, 0, i, 4, j, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(p_228123_, p_228127_, 1, j + 1, i, 3, k - 1, i, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            if (i <= 6) {
               this.placeBlock(p_228123_, blockstate, 1, j + 1, i, p_228127_);
               this.placeBlock(p_228123_, blockstate, 2, j + 1, i, p_228127_);
               this.placeBlock(p_228123_, blockstate, 3, j + 1, i, p_228127_);
            }

            this.generateBox(p_228123_, p_228127_, 0, k, i, 4, k, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(
               p_228123_, p_228127_, 0, j + 1, i, 0, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
            this.generateBox(
               p_228123_, p_228127_, 4, j + 1, i, 4, k - 1, i, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
            );
            if ((i & 1) == 0) {
               this.generateBox(p_228123_, p_228127_, 0, j + 2, i, 0, j + 3, i, blockstate1, blockstate1, false);
               this.generateBox(p_228123_, p_228127_, 4, j + 2, i, 4, j + 3, i, blockstate1, blockstate1, false);
            }

            for (int i1 = 0; i1 <= 4; i1++) {
               this.fillColumnDown(p_228123_, Blocks.NETHER_BRICKS.defaultBlockState(), i1, -1, l, p_228127_);
            }
         }
      }
   }

   public static class CastleCorridorTBalconyPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 9;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 9;

      public CastleCorridorTBalconyPiece(int p_228145_, BoundingBox p_228146_, Direction p_228147_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, p_228145_, p_228146_);
         this.setOrientation(p_228147_);
      }

      public CastleCorridorTBalconyPiece(CompoundTag p_228149_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, p_228149_);
      }

      @Override
      public void addChildren(StructurePiece p_228159_, StructurePieceAccessor p_228160_, RandomSource p_228161_) {
         int i = 1;
         Direction direction = this.getOrientation();
         if (direction == Direction.WEST || direction == Direction.NORTH) {
            i = 5;
         }

         this.generateChildLeft((NetherFortressPieces.StartPiece)p_228159_, p_228160_, p_228161_, 0, i, p_228161_.nextInt(8) > 0);
         this.generateChildRight((NetherFortressPieces.StartPiece)p_228159_, p_228160_, p_228161_, 0, i, p_228161_.nextInt(8) > 0);
      }

      public static NetherFortressPieces.@Nullable CastleCorridorTBalconyPiece createPiece(
         StructurePieceAccessor p_228163_, int p_228164_, int p_228165_, int p_228166_, Direction p_228167_, int p_228168_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228164_, p_228165_, p_228166_, -3, 0, 0, 9, 7, 9, p_228167_);
         return isOkBox(boundingbox) && p_228163_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleCorridorTBalconyPiece(p_228168_, boundingbox, p_228167_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228151_,
         StructureManager p_228152_,
         ChunkGenerator p_228153_,
         RandomSource p_228154_,
         BoundingBox p_228155_,
         ChunkPos p_228156_,
         BlockPos p_228157_
      ) {
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         this.generateBox(p_228151_, p_228155_, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 0, 2, 0, 8, 5, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 1, 3, 0, 1, 4, 0, blockstate1, blockstate1, false);
         this.generateBox(p_228151_, p_228155_, 7, 3, 0, 7, 4, 0, blockstate1, blockstate1, false);
         this.generateBox(p_228151_, p_228155_, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 1, 1, 4, 2, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 6, 1, 4, 7, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 1, 3, 8, 7, 3, 8, blockstate1, blockstate1, false);
         this.placeBlock(
            p_228151_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.SOUTH, true), 0, 3, 8, p_228155_
         );
         this.placeBlock(
            p_228151_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.SOUTH, true), 8, 3, 8, p_228155_
         );
         this.generateBox(p_228151_, p_228155_, 0, 3, 6, 0, 3, 7, blockstate, blockstate, false);
         this.generateBox(p_228151_, p_228155_, 8, 3, 6, 8, 3, 7, blockstate, blockstate, false);
         this.generateBox(p_228151_, p_228155_, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228151_, p_228155_, 1, 4, 5, 1, 5, 5, blockstate1, blockstate1, false);
         this.generateBox(p_228151_, p_228155_, 7, 4, 5, 7, 5, 5, blockstate1, blockstate1, false);

         for (int i = 0; i <= 5; i++) {
            for (int j = 0; j <= 8; j++) {
               this.fillColumnDown(p_228151_, Blocks.NETHER_BRICKS.defaultBlockState(), j, -1, i, p_228155_);
            }
         }
      }
   }

   public static class CastleEntrance extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 13;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 13;

      public CastleEntrance(int p_228173_, RandomSource p_228174_, BoundingBox p_228175_, Direction p_228176_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, p_228173_, p_228175_);
         this.setOrientation(p_228176_);
      }

      public CastleEntrance(CompoundTag p_228178_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, p_228178_);
      }

      @Override
      public void addChildren(StructurePiece p_228188_, StructurePieceAccessor p_228189_, RandomSource p_228190_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228188_, p_228189_, p_228190_, 5, 3, true);
      }

      public static NetherFortressPieces.@Nullable CastleEntrance createPiece(
         StructurePieceAccessor p_228192_, RandomSource p_228193_, int p_228194_, int p_228195_, int p_228196_, Direction p_228197_, int p_228198_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228194_, p_228195_, p_228196_, -5, -3, 0, 13, 14, 13, p_228197_);
         return isOkBox(boundingbox) && p_228192_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleEntrance(p_228198_, p_228193_, boundingbox, p_228197_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228180_,
         StructureManager p_228181_,
         ChunkGenerator p_228182_,
         RandomSource p_228183_,
         BoundingBox p_228184_,
         ChunkPos p_228185_,
         BlockPos p_228186_
      ) {
         this.generateBox(p_228180_, p_228184_, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(
            p_228180_, p_228184_, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.defaultBlockState(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), false
         );
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);

         for (int i = 1; i <= 11; i += 2) {
            this.generateBox(p_228180_, p_228184_, i, 10, 0, i, 11, 0, blockstate, blockstate, false);
            this.generateBox(p_228180_, p_228184_, i, 10, 12, i, 11, 12, blockstate, blockstate, false);
            this.generateBox(p_228180_, p_228184_, 0, 10, i, 0, 11, i, blockstate1, blockstate1, false);
            this.generateBox(p_228180_, p_228184_, 12, 10, i, 12, 11, i, blockstate1, blockstate1, false);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, p_228184_);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, p_228184_);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, p_228184_);
            this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, p_228184_);
            if (i != 11) {
               this.placeBlock(p_228180_, blockstate, i + 1, 13, 0, p_228184_);
               this.placeBlock(p_228180_, blockstate, i + 1, 13, 12, p_228184_);
               this.placeBlock(p_228180_, blockstate1, 0, 13, i + 1, p_228184_);
               this.placeBlock(p_228180_, blockstate1, 12, 13, i + 1, p_228184_);
            }
         }

         this.placeBlock(
            p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 0, 13, 0, p_228184_
         );
         this.placeBlock(
            p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 0, 13, 12, p_228184_
         );
         this.placeBlock(
            p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true), 12, 13, 12, p_228184_
         );
         this.placeBlock(
            p_228180_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 12, 13, 0, p_228184_
         );

         for (int k = 3; k <= 9; k += 2) {
            this.generateBox(
               p_228180_, p_228184_, 1, 7, k, 1, 8, k, blockstate1.setValue(FenceBlock.WEST, true), blockstate1.setValue(FenceBlock.WEST, true), false
            );
            this.generateBox(
               p_228180_, p_228184_, 11, 7, k, 11, 8, k, blockstate1.setValue(FenceBlock.EAST, true), blockstate1.setValue(FenceBlock.EAST, true), false
            );
         }

         this.generateBox(p_228180_, p_228184_, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int l = 4; l <= 8; l++) {
            for (int j = 0; j <= 2; j++) {
               this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), l, -1, j, p_228184_);
               this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), l, -1, 12 - j, p_228184_);
            }
         }

         for (int i1 = 0; i1 <= 2; i1++) {
            for (int j1 = 4; j1 <= 8; j1++) {
               this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), i1, -1, j1, p_228184_);
               this.fillColumnDown(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - i1, -1, j1, p_228184_);
            }
         }

         this.generateBox(p_228180_, p_228184_, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228180_, p_228184_, 6, 1, 6, 6, 4, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.placeBlock(p_228180_, Blocks.NETHER_BRICKS.defaultBlockState(), 6, 0, 6, p_228184_);
         this.placeBlock(p_228180_, Blocks.LAVA.defaultBlockState(), 6, 5, 6, p_228184_);
         BlockPos blockpos = this.getWorldPos(6, 5, 6);
         if (p_228184_.isInside(blockpos)) {
            p_228180_.scheduleTick(blockpos, Fluids.LAVA, 0);
         }
      }
   }

   public static class CastleSmallCorridorCrossingPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;

      public CastleSmallCorridorCrossingPiece(int p_228203_, BoundingBox p_228204_, Direction p_228205_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, p_228203_, p_228204_);
         this.setOrientation(p_228205_);
      }

      public CastleSmallCorridorCrossingPiece(CompoundTag p_228207_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, p_228207_);
      }

      @Override
      public void addChildren(StructurePiece p_228217_, StructurePieceAccessor p_228218_, RandomSource p_228219_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228217_, p_228218_, p_228219_, 1, 0, true);
         this.generateChildLeft((NetherFortressPieces.StartPiece)p_228217_, p_228218_, p_228219_, 0, 1, true);
         this.generateChildRight((NetherFortressPieces.StartPiece)p_228217_, p_228218_, p_228219_, 0, 1, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorCrossingPiece createPiece(
         StructurePieceAccessor p_228221_, int p_228222_, int p_228223_, int p_228224_, Direction p_228225_, int p_228226_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228222_, p_228223_, p_228224_, -1, 0, 0, 5, 7, 5, p_228225_);
         return isOkBox(boundingbox) && p_228221_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleSmallCorridorCrossingPiece(p_228226_, boundingbox, p_228225_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228209_,
         StructureManager p_228210_,
         ChunkGenerator p_228211_,
         RandomSource p_228212_,
         BoundingBox p_228213_,
         ChunkPos p_228214_,
         BlockPos p_228215_
      ) {
         this.generateBox(p_228209_, p_228213_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228209_, p_228213_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228209_, p_228213_, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228209_, p_228213_, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228209_, p_228213_, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228209_, p_228213_, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228209_, p_228213_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int i = 0; i <= 4; i++) {
            for (int j = 0; j <= 4; j++) {
               this.fillColumnDown(p_228209_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228213_);
            }
         }
      }
   }

   public static class CastleSmallCorridorLeftTurnPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;
      private boolean isNeedingChest;

      public CastleSmallCorridorLeftTurnPiece(int p_228232_, RandomSource p_228233_, BoundingBox p_228234_, Direction p_228235_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, p_228232_, p_228234_);
         this.setOrientation(p_228235_);
         this.isNeedingChest = p_228233_.nextInt(3) == 0;
      }

      public CastleSmallCorridorLeftTurnPiece(CompoundTag p_228237_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, p_228237_);
         this.isNeedingChest = p_228237_.getBooleanOr("Chest", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228259_, CompoundTag p_228260_) {
         super.addAdditionalSaveData(p_228259_, p_228260_);
         p_228260_.putBoolean("Chest", this.isNeedingChest);
      }

      @Override
      public void addChildren(StructurePiece p_228247_, StructurePieceAccessor p_228248_, RandomSource p_228249_) {
         this.generateChildLeft((NetherFortressPieces.StartPiece)p_228247_, p_228248_, p_228249_, 0, 1, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorLeftTurnPiece createPiece(
         StructurePieceAccessor p_228251_, RandomSource p_228252_, int p_228253_, int p_228254_, int p_228255_, Direction p_228256_, int p_228257_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228253_, p_228254_, p_228255_, -1, 0, 0, 5, 7, 5, p_228256_);
         return isOkBox(boundingbox) && p_228251_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleSmallCorridorLeftTurnPiece(p_228257_, p_228252_, boundingbox, p_228256_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228239_,
         StructureManager p_228240_,
         ChunkGenerator p_228241_,
         RandomSource p_228242_,
         BoundingBox p_228243_,
         ChunkPos p_228244_,
         BlockPos p_228245_
      ) {
         this.generateBox(p_228239_, p_228243_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228239_, p_228243_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(p_228239_, p_228243_, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228239_, p_228243_, 4, 3, 1, 4, 4, 1, blockstate1, blockstate1, false);
         this.generateBox(p_228239_, p_228243_, 4, 3, 3, 4, 4, 3, blockstate1, blockstate1, false);
         this.generateBox(p_228239_, p_228243_, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228239_, p_228243_, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228239_, p_228243_, 1, 3, 4, 1, 4, 4, blockstate, blockstate, false);
         this.generateBox(p_228239_, p_228243_, 3, 3, 4, 3, 4, 4, blockstate, blockstate, false);
         if (this.isNeedingChest && p_228243_.isInside(this.getWorldPos(3, 2, 3))) {
            this.isNeedingChest = false;
            this.createChest(p_228239_, p_228243_, p_228242_, 3, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
         }

         this.generateBox(p_228239_, p_228243_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int i = 0; i <= 4; i++) {
            for (int j = 0; j <= 4; j++) {
               this.fillColumnDown(p_228239_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228243_);
            }
         }
      }
   }

   public static class CastleSmallCorridorPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;

      public CastleSmallCorridorPiece(int p_228265_, BoundingBox p_228266_, Direction p_228267_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, p_228265_, p_228266_);
         this.setOrientation(p_228267_);
      }

      public CastleSmallCorridorPiece(CompoundTag p_228269_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, p_228269_);
      }

      @Override
      public void addChildren(StructurePiece p_228279_, StructurePieceAccessor p_228280_, RandomSource p_228281_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228279_, p_228280_, p_228281_, 1, 0, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorPiece createPiece(
         StructurePieceAccessor p_228283_, int p_228284_, int p_228285_, int p_228286_, Direction p_228287_, int p_228288_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228284_, p_228285_, p_228286_, -1, 0, 0, 5, 7, 5, p_228287_);
         return isOkBox(boundingbox) && p_228283_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleSmallCorridorPiece(p_228288_, boundingbox, p_228287_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228271_,
         StructureManager p_228272_,
         ChunkGenerator p_228273_,
         RandomSource p_228274_,
         BoundingBox p_228275_,
         ChunkPos p_228276_,
         BlockPos p_228277_
      ) {
         this.generateBox(p_228271_, p_228275_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228271_, p_228275_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(p_228271_, p_228275_, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228271_, p_228275_, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228271_, p_228275_, 0, 3, 1, 0, 4, 1, blockstate, blockstate, false);
         this.generateBox(p_228271_, p_228275_, 0, 3, 3, 0, 4, 3, blockstate, blockstate, false);
         this.generateBox(p_228271_, p_228275_, 4, 3, 1, 4, 4, 1, blockstate, blockstate, false);
         this.generateBox(p_228271_, p_228275_, 4, 3, 3, 4, 4, 3, blockstate, blockstate, false);
         this.generateBox(p_228271_, p_228275_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int i = 0; i <= 4; i++) {
            for (int j = 0; j <= 4; j++) {
               this.fillColumnDown(p_228271_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228275_);
            }
         }
      }
   }

   public static class CastleSmallCorridorRightTurnPiece extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 7;
      private static final int DEPTH = 5;
      private boolean isNeedingChest;

      public CastleSmallCorridorRightTurnPiece(int p_228294_, RandomSource p_228295_, BoundingBox p_228296_, Direction p_228297_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, p_228294_, p_228296_);
         this.setOrientation(p_228297_);
         this.isNeedingChest = p_228295_.nextInt(3) == 0;
      }

      public CastleSmallCorridorRightTurnPiece(CompoundTag p_228299_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, p_228299_);
         this.isNeedingChest = p_228299_.getBooleanOr("Chest", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228321_, CompoundTag p_228322_) {
         super.addAdditionalSaveData(p_228321_, p_228322_);
         p_228322_.putBoolean("Chest", this.isNeedingChest);
      }

      @Override
      public void addChildren(StructurePiece p_228309_, StructurePieceAccessor p_228310_, RandomSource p_228311_) {
         this.generateChildRight((NetherFortressPieces.StartPiece)p_228309_, p_228310_, p_228311_, 0, 1, true);
      }

      public static NetherFortressPieces.@Nullable CastleSmallCorridorRightTurnPiece createPiece(
         StructurePieceAccessor p_228313_, RandomSource p_228314_, int p_228315_, int p_228316_, int p_228317_, Direction p_228318_, int p_228319_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228315_, p_228316_, p_228317_, -1, 0, 0, 5, 7, 5, p_228318_);
         return isOkBox(boundingbox) && p_228313_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleSmallCorridorRightTurnPiece(p_228319_, p_228314_, boundingbox, p_228318_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228301_,
         StructureManager p_228302_,
         ChunkGenerator p_228303_,
         RandomSource p_228304_,
         BoundingBox p_228305_,
         ChunkPos p_228306_,
         BlockPos p_228307_
      ) {
         this.generateBox(p_228301_, p_228305_, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228301_, p_228305_, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(p_228301_, p_228305_, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228301_, p_228305_, 0, 3, 1, 0, 4, 1, blockstate1, blockstate1, false);
         this.generateBox(p_228301_, p_228305_, 0, 3, 3, 0, 4, 3, blockstate1, blockstate1, false);
         this.generateBox(p_228301_, p_228305_, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228301_, p_228305_, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228301_, p_228305_, 1, 3, 4, 1, 4, 4, blockstate, blockstate, false);
         this.generateBox(p_228301_, p_228305_, 3, 3, 4, 3, 4, 4, blockstate, blockstate, false);
         if (this.isNeedingChest && p_228305_.isInside(this.getWorldPos(1, 2, 3))) {
            this.isNeedingChest = false;
            this.createChest(p_228301_, p_228305_, p_228304_, 1, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
         }

         this.generateBox(p_228301_, p_228305_, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int i = 0; i <= 4; i++) {
            for (int j = 0; j <= 4; j++) {
               this.fillColumnDown(p_228301_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228305_);
            }
         }
      }
   }

   public static class CastleStalkRoom extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 13;
      private static final int HEIGHT = 14;
      private static final int DEPTH = 13;

      public CastleStalkRoom(int p_228327_, BoundingBox p_228328_, Direction p_228329_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, p_228327_, p_228328_);
         this.setOrientation(p_228329_);
      }

      public CastleStalkRoom(CompoundTag p_228331_) {
         super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, p_228331_);
      }

      @Override
      public void addChildren(StructurePiece p_228341_, StructurePieceAccessor p_228342_, RandomSource p_228343_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228341_, p_228342_, p_228343_, 5, 3, true);
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228341_, p_228342_, p_228343_, 5, 11, true);
      }

      public static NetherFortressPieces.@Nullable CastleStalkRoom createPiece(
         StructurePieceAccessor p_228345_, int p_228346_, int p_228347_, int p_228348_, Direction p_228349_, int p_228350_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228346_, p_228347_, p_228348_, -5, -3, 0, 13, 14, 13, p_228349_);
         return isOkBox(boundingbox) && p_228345_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.CastleStalkRoom(p_228350_, boundingbox, p_228349_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228333_,
         StructureManager p_228334_,
         ChunkGenerator p_228335_,
         RandomSource p_228336_,
         BoundingBox p_228337_,
         ChunkPos p_228338_,
         BlockPos p_228339_
      ) {
         this.generateBox(p_228333_, p_228337_, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         BlockState blockstate2 = blockstate1.setValue(FenceBlock.WEST, true);
         BlockState blockstate3 = blockstate1.setValue(FenceBlock.EAST, true);

         for (int i = 1; i <= 11; i += 2) {
            this.generateBox(p_228333_, p_228337_, i, 10, 0, i, 11, 0, blockstate, blockstate, false);
            this.generateBox(p_228333_, p_228337_, i, 10, 12, i, 11, 12, blockstate, blockstate, false);
            this.generateBox(p_228333_, p_228337_, 0, 10, i, 0, 11, i, blockstate1, blockstate1, false);
            this.generateBox(p_228333_, p_228337_, 12, 10, i, 12, 11, i, blockstate1, blockstate1, false);
            this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, p_228337_);
            this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, p_228337_);
            this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, p_228337_);
            this.placeBlock(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, p_228337_);
            if (i != 11) {
               this.placeBlock(p_228333_, blockstate, i + 1, 13, 0, p_228337_);
               this.placeBlock(p_228333_, blockstate, i + 1, 13, 12, p_228337_);
               this.placeBlock(p_228333_, blockstate1, 0, 13, i + 1, p_228337_);
               this.placeBlock(p_228333_, blockstate1, 12, 13, i + 1, p_228337_);
            }
         }

         this.placeBlock(
            p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 0, 13, 0, p_228337_
         );
         this.placeBlock(
            p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 0, 13, 12, p_228337_
         );
         this.placeBlock(
            p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true), 12, 13, 12, p_228337_
         );
         this.placeBlock(
            p_228333_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 12, 13, 0, p_228337_
         );

         for (int j1 = 3; j1 <= 9; j1 += 2) {
            this.generateBox(p_228333_, p_228337_, 1, 7, j1, 1, 8, j1, blockstate2, blockstate2, false);
            this.generateBox(p_228333_, p_228337_, 11, 7, j1, 11, 8, j1, blockstate3, blockstate3, false);
         }

         BlockState blockstate4 = Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);

         for (int j = 0; j <= 6; j++) {
            int k = j + 4;

            for (int l = 5; l <= 7; l++) {
               this.placeBlock(p_228333_, blockstate4, l, 5 + j, k, p_228337_);
            }

            if (k >= 5 && k <= 8) {
               this.generateBox(
                  p_228333_, p_228337_, 5, 5, k, 7, j + 4, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            } else if (k >= 9 && k <= 10) {
               this.generateBox(
                  p_228333_, p_228337_, 5, 8, k, 7, j + 4, k, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false
               );
            }

            if (j >= 1) {
               this.generateBox(p_228333_, p_228337_, 5, 6 + j, k, 7, 9 + j, k, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            }
         }

         for (int k1 = 5; k1 <= 7; k1++) {
            this.placeBlock(p_228333_, blockstate4, k1, 12, 11, p_228337_);
         }

         this.generateBox(p_228333_, p_228337_, 5, 6, 7, 5, 7, 7, blockstate3, blockstate3, false);
         this.generateBox(p_228333_, p_228337_, 7, 6, 7, 7, 7, 7, blockstate2, blockstate2, false);
         this.generateBox(p_228333_, p_228337_, 5, 13, 12, 7, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState blockstate5 = blockstate4.setValue(StairBlock.FACING, Direction.EAST);
         BlockState blockstate6 = blockstate4.setValue(StairBlock.FACING, Direction.WEST);
         this.placeBlock(p_228333_, blockstate6, 4, 5, 2, p_228337_);
         this.placeBlock(p_228333_, blockstate6, 4, 5, 3, p_228337_);
         this.placeBlock(p_228333_, blockstate6, 4, 5, 9, p_228337_);
         this.placeBlock(p_228333_, blockstate6, 4, 5, 10, p_228337_);
         this.placeBlock(p_228333_, blockstate5, 8, 5, 2, p_228337_);
         this.placeBlock(p_228333_, blockstate5, 8, 5, 3, p_228337_);
         this.placeBlock(p_228333_, blockstate5, 8, 5, 9, p_228337_);
         this.placeBlock(p_228333_, blockstate5, 8, 5, 10, p_228337_);
         this.generateBox(p_228333_, p_228337_, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228333_, p_228337_, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);

         for (int l1 = 4; l1 <= 8; l1++) {
            for (int i1 = 0; i1 <= 2; i1++) {
               this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), l1, -1, i1, p_228337_);
               this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), l1, -1, 12 - i1, p_228337_);
            }
         }

         for (int i2 = 0; i2 <= 2; i2++) {
            for (int j2 = 4; j2 <= 8; j2++) {
               this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), i2, -1, j2, p_228337_);
               this.fillColumnDown(p_228333_, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - i2, -1, j2, p_228337_);
            }
         }
      }
   }

   public static class MonsterThrone extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 8;
      private static final int DEPTH = 9;
      private boolean hasPlacedSpawner;

      public MonsterThrone(int p_228356_, BoundingBox p_228357_, Direction p_228358_) {
         super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, p_228356_, p_228357_);
         this.setOrientation(p_228358_);
      }

      public MonsterThrone(CompoundTag p_228360_) {
         super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, p_228360_);
         this.hasPlacedSpawner = p_228360_.getBooleanOr("Mob", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228377_, CompoundTag p_228378_) {
         super.addAdditionalSaveData(p_228377_, p_228378_);
         p_228378_.putBoolean("Mob", this.hasPlacedSpawner);
      }

      public static NetherFortressPieces.@Nullable MonsterThrone createPiece(
         StructurePieceAccessor p_228370_, int p_228371_, int p_228372_, int p_228373_, int p_228374_, Direction p_228375_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228371_, p_228372_, p_228373_, -2, 0, 0, 7, 8, 9, p_228375_);
         return isOkBox(boundingbox) && p_228370_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.MonsterThrone(p_228374_, boundingbox, p_228375_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228362_,
         StructureManager p_228363_,
         ChunkGenerator p_228364_,
         RandomSource p_228365_,
         BoundingBox p_228366_,
         ChunkPos p_228367_,
         BlockPos p_228368_
      ) {
         this.generateBox(p_228362_, p_228366_, 0, 2, 0, 6, 7, 7, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228362_, p_228366_, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 1, 6, 3, p_228366_);
         this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 5, 6, 3, p_228366_);
         this.placeBlock(
            p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.NORTH, true), 0, 6, 3, p_228366_
         );
         this.placeBlock(
            p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.NORTH, true), 6, 6, 3, p_228366_
         );
         this.generateBox(p_228362_, p_228366_, 0, 6, 4, 0, 6, 7, blockstate1, blockstate1, false);
         this.generateBox(p_228362_, p_228366_, 6, 6, 4, 6, 6, 7, blockstate1, blockstate1, false);
         this.placeBlock(
            p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true).setValue(FenceBlock.SOUTH, true), 0, 6, 8, p_228366_
         );
         this.placeBlock(
            p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.SOUTH, true), 6, 6, 8, p_228366_
         );
         this.generateBox(p_228362_, p_228366_, 1, 6, 8, 5, 6, 8, blockstate, blockstate, false);
         this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 1, 7, 8, p_228366_);
         this.generateBox(p_228362_, p_228366_, 2, 7, 8, 4, 7, 8, blockstate, blockstate, false);
         this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 5, 7, 8, p_228366_);
         this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 2, 8, 8, p_228366_);
         this.placeBlock(p_228362_, blockstate, 3, 8, 8, p_228366_);
         this.placeBlock(p_228362_, Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 4, 8, 8, p_228366_);
         if (!this.hasPlacedSpawner) {
            BlockPos blockpos = this.getWorldPos(3, 5, 5);
            if (p_228366_.isInside(blockpos)) {
               this.hasPlacedSpawner = true;
               p_228362_.setBlock(blockpos, Blocks.SPAWNER.defaultBlockState(), 2);
               if (p_228362_.getBlockEntity(blockpos) instanceof SpawnerBlockEntity spawnerblockentity) {
                  spawnerblockentity.setEntityId(EntityType.BLAZE, p_228365_);
               }
            }
         }

         for (int i = 0; i <= 6; i++) {
            for (int j = 0; j <= 6; j++) {
               this.fillColumnDown(p_228362_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228366_);
            }
         }
      }
   }

   abstract static class NetherBridgePiece extends StructurePiece {
      protected NetherBridgePiece(StructurePieceType p_228380_, int p_228381_, BoundingBox p_228382_) {
         super(p_228380_, p_228381_, p_228382_);
      }

      public NetherBridgePiece(StructurePieceType p_228384_, CompoundTag p_228385_) {
         super(p_228384_, p_228385_);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228389_, CompoundTag p_228390_) {
      }

      private int updatePieceWeight(List<NetherFortressPieces.PieceWeight> p_228419_) {
         boolean flag = false;
         int i = 0;

         for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight : p_228419_) {
            if (netherfortresspieces$pieceweight.maxPlaceCount > 0
               && netherfortresspieces$pieceweight.placeCount < netherfortresspieces$pieceweight.maxPlaceCount) {
               flag = true;
            }

            i += netherfortresspieces$pieceweight.weight;
         }

         return flag ? i : -1;
      }

      private NetherFortressPieces.@Nullable NetherBridgePiece generatePiece(
         NetherFortressPieces.StartPiece p_228409_,
         List<NetherFortressPieces.PieceWeight> p_228410_,
         StructurePieceAccessor p_228411_,
         RandomSource p_228412_,
         int p_228413_,
         int p_228414_,
         int p_228415_,
         Direction p_228416_,
         int p_228417_
      ) {
         int i = this.updatePieceWeight(p_228410_);
         boolean flag = i > 0 && p_228417_ <= 30;
         int j = 0;

         while (j < 5 && flag) {
            j++;
            int k = p_228412_.nextInt(i);

            for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight : p_228410_) {
               k -= netherfortresspieces$pieceweight.weight;
               if (k < 0) {
                  if (!netherfortresspieces$pieceweight.doPlace(p_228417_)
                     || netherfortresspieces$pieceweight == p_228409_.previousPiece && !netherfortresspieces$pieceweight.allowInRow) {
                     break;
                  }

                  NetherFortressPieces.NetherBridgePiece netherfortresspieces$netherbridgepiece = NetherFortressPieces.findAndCreateBridgePieceFactory(
                     netherfortresspieces$pieceweight, p_228411_, p_228412_, p_228413_, p_228414_, p_228415_, p_228416_, p_228417_
                  );
                  if (netherfortresspieces$netherbridgepiece != null) {
                     netherfortresspieces$pieceweight.placeCount++;
                     p_228409_.previousPiece = netherfortresspieces$pieceweight;
                     if (!netherfortresspieces$pieceweight.isValid()) {
                        p_228410_.remove(netherfortresspieces$pieceweight);
                     }

                     return netherfortresspieces$netherbridgepiece;
                  }
               }
            }
         }

         return NetherFortressPieces.BridgeEndFiller.createPiece(p_228411_, p_228412_, p_228413_, p_228414_, p_228415_, p_228416_, p_228417_);
      }

      private @Nullable StructurePiece generateAndAddPiece(
         NetherFortressPieces.StartPiece p_228392_,
         StructurePieceAccessor p_228393_,
         RandomSource p_228394_,
         int p_228395_,
         int p_228396_,
         int p_228397_,
         Direction p_228398_,
         int p_228399_,
         boolean p_228400_
      ) {
         if (Math.abs(p_228395_ - p_228392_.getBoundingBox().minX()) <= 112 && Math.abs(p_228397_ - p_228392_.getBoundingBox().minZ()) <= 112) {
            List<NetherFortressPieces.PieceWeight> list = p_228392_.availableBridgePieces;
            if (p_228400_) {
               list = p_228392_.availableCastlePieces;
            }

            StructurePiece structurepiece = this.generatePiece(p_228392_, list, p_228393_, p_228394_, p_228395_, p_228396_, p_228397_, p_228398_, p_228399_ + 1);
            if (structurepiece != null) {
               p_228393_.addPiece(structurepiece);
               p_228392_.pendingChildren.add(structurepiece);
            }

            return structurepiece;
         } else {
            return NetherFortressPieces.BridgeEndFiller.createPiece(p_228393_, p_228394_, p_228395_, p_228396_, p_228397_, p_228398_, p_228399_);
         }
      }

      protected @Nullable StructurePiece generateChildForward(
         NetherFortressPieces.StartPiece p_228402_, StructurePieceAccessor p_228403_, RandomSource p_228404_, int p_228405_, int p_228406_, boolean p_228407_
      ) {
         Direction direction = this.getOrientation();
         if (direction != null) {
            switch (direction) {
               case NORTH:
                  return this.generateAndAddPiece(
                     p_228402_,
                     p_228403_,
                     p_228404_,
                     this.boundingBox.minX() + p_228405_,
                     this.boundingBox.minY() + p_228406_,
                     this.boundingBox.minZ() - 1,
                     direction,
                     this.getGenDepth(),
                     p_228407_
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     p_228402_,
                     p_228403_,
                     p_228404_,
                     this.boundingBox.minX() + p_228405_,
                     this.boundingBox.minY() + p_228406_,
                     this.boundingBox.maxZ() + 1,
                     direction,
                     this.getGenDepth(),
                     p_228407_
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     p_228402_,
                     p_228403_,
                     p_228404_,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + p_228406_,
                     this.boundingBox.minZ() + p_228405_,
                     direction,
                     this.getGenDepth(),
                     p_228407_
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     p_228402_,
                     p_228403_,
                     p_228404_,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + p_228406_,
                     this.boundingBox.minZ() + p_228405_,
                     direction,
                     this.getGenDepth(),
                     p_228407_
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateChildLeft(
         NetherFortressPieces.StartPiece p_228421_, StructurePieceAccessor p_228422_, RandomSource p_228423_, int p_228424_, int p_228425_, boolean p_228426_
      ) {
         Direction direction = this.getOrientation();
         if (direction != null) {
            switch (direction) {
               case NORTH:
                  return this.generateAndAddPiece(
                     p_228421_,
                     p_228422_,
                     p_228423_,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + p_228424_,
                     this.boundingBox.minZ() + p_228425_,
                     Direction.WEST,
                     this.getGenDepth(),
                     p_228426_
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     p_228421_,
                     p_228422_,
                     p_228423_,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + p_228424_,
                     this.boundingBox.minZ() + p_228425_,
                     Direction.WEST,
                     this.getGenDepth(),
                     p_228426_
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     p_228421_,
                     p_228422_,
                     p_228423_,
                     this.boundingBox.minX() + p_228425_,
                     this.boundingBox.minY() + p_228424_,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth(),
                     p_228426_
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     p_228421_,
                     p_228422_,
                     p_228423_,
                     this.boundingBox.minX() + p_228425_,
                     this.boundingBox.minY() + p_228424_,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth(),
                     p_228426_
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateChildRight(
         NetherFortressPieces.StartPiece p_228428_, StructurePieceAccessor p_228429_, RandomSource p_228430_, int p_228431_, int p_228432_, boolean p_228433_
      ) {
         Direction direction = this.getOrientation();
         if (direction != null) {
            switch (direction) {
               case NORTH:
                  return this.generateAndAddPiece(
                     p_228428_,
                     p_228429_,
                     p_228430_,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + p_228431_,
                     this.boundingBox.minZ() + p_228432_,
                     Direction.EAST,
                     this.getGenDepth(),
                     p_228433_
                  );
               case SOUTH:
                  return this.generateAndAddPiece(
                     p_228428_,
                     p_228429_,
                     p_228430_,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + p_228431_,
                     this.boundingBox.minZ() + p_228432_,
                     Direction.EAST,
                     this.getGenDepth(),
                     p_228433_
                  );
               case WEST:
                  return this.generateAndAddPiece(
                     p_228428_,
                     p_228429_,
                     p_228430_,
                     this.boundingBox.minX() + p_228432_,
                     this.boundingBox.minY() + p_228431_,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth(),
                     p_228433_
                  );
               case EAST:
                  return this.generateAndAddPiece(
                     p_228428_,
                     p_228429_,
                     p_228430_,
                     this.boundingBox.minX() + p_228432_,
                     this.boundingBox.minY() + p_228431_,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth(),
                     p_228433_
                  );
            }
         }

         return null;
      }

      protected static boolean isOkBox(BoundingBox p_228387_) {
         return p_228387_.minY() > 10;
      }
   }

   static class PieceWeight {
      public final Class<? extends NetherFortressPieces.NetherBridgePiece> pieceClass;
      public final int weight;
      public int placeCount;
      public final int maxPlaceCount;
      public final boolean allowInRow;

      public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> p_228444_, int p_228445_, int p_228446_, boolean p_228447_) {
         this.pieceClass = p_228444_;
         this.weight = p_228445_;
         this.maxPlaceCount = p_228446_;
         this.allowInRow = p_228447_;
      }

      public PieceWeight(Class<? extends NetherFortressPieces.NetherBridgePiece> p_228440_, int p_228441_, int p_228442_) {
         this(p_228440_, p_228441_, p_228442_, false);
      }

      public boolean doPlace(int p_228450_) {
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

      public RoomCrossing(int p_228455_, BoundingBox p_228456_, Direction p_228457_) {
         super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, p_228455_, p_228456_);
         this.setOrientation(p_228457_);
      }

      public RoomCrossing(CompoundTag p_228459_) {
         super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, p_228459_);
      }

      @Override
      public void addChildren(StructurePiece p_228469_, StructurePieceAccessor p_228470_, RandomSource p_228471_) {
         this.generateChildForward((NetherFortressPieces.StartPiece)p_228469_, p_228470_, p_228471_, 2, 0, false);
         this.generateChildLeft((NetherFortressPieces.StartPiece)p_228469_, p_228470_, p_228471_, 0, 2, false);
         this.generateChildRight((NetherFortressPieces.StartPiece)p_228469_, p_228470_, p_228471_, 0, 2, false);
      }

      public static NetherFortressPieces.@Nullable RoomCrossing createPiece(
         StructurePieceAccessor p_228473_, int p_228474_, int p_228475_, int p_228476_, Direction p_228477_, int p_228478_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228474_, p_228475_, p_228476_, -2, 0, 0, 7, 9, 7, p_228477_);
         return isOkBox(boundingbox) && p_228473_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.RoomCrossing(p_228478_, boundingbox, p_228477_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228461_,
         StructureManager p_228462_,
         ChunkGenerator p_228463_,
         RandomSource p_228464_,
         BoundingBox p_228465_,
         ChunkPos p_228466_,
         BlockPos p_228467_
      ) {
         this.generateBox(p_228461_, p_228465_, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 0, 2, 0, 6, 7, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(p_228461_, p_228465_, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 2, 5, 0, 4, 5, 0, blockstate, blockstate, false);
         this.generateBox(p_228461_, p_228465_, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 2, 5, 6, 4, 5, 6, blockstate, blockstate, false);
         this.generateBox(p_228461_, p_228465_, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 0, 5, 2, 0, 5, 4, blockstate1, blockstate1, false);
         this.generateBox(p_228461_, p_228465_, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228461_, p_228465_, 6, 5, 2, 6, 5, 4, blockstate1, blockstate1, false);

         for (int i = 0; i <= 6; i++) {
            for (int j = 0; j <= 6; j++) {
               this.fillColumnDown(p_228461_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228465_);
            }
         }
      }
   }

   public static class StairsRoom extends NetherFortressPieces.NetherBridgePiece {
      private static final int WIDTH = 7;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 7;

      public StairsRoom(int p_228483_, BoundingBox p_228484_, Direction p_228485_) {
         super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, p_228483_, p_228484_);
         this.setOrientation(p_228485_);
      }

      public StairsRoom(CompoundTag p_228487_) {
         super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, p_228487_);
      }

      @Override
      public void addChildren(StructurePiece p_228497_, StructurePieceAccessor p_228498_, RandomSource p_228499_) {
         this.generateChildRight((NetherFortressPieces.StartPiece)p_228497_, p_228498_, p_228499_, 6, 2, false);
      }

      public static NetherFortressPieces.@Nullable StairsRoom createPiece(
         StructurePieceAccessor p_228501_, int p_228502_, int p_228503_, int p_228504_, int p_228505_, Direction p_228506_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_228502_, p_228503_, p_228504_, -2, 0, 0, 7, 11, 7, p_228506_);
         return isOkBox(boundingbox) && p_228501_.findCollisionPiece(boundingbox) == null
            ? new NetherFortressPieces.StairsRoom(p_228505_, boundingbox, p_228506_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228489_,
         StructureManager p_228490_,
         ChunkGenerator p_228491_,
         RandomSource p_228492_,
         BoundingBox p_228493_,
         ChunkPos p_228494_,
         BlockPos p_228495_
      ) {
         this.generateBox(p_228489_, p_228493_, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 0, 2, 0, 6, 10, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         BlockState blockstate = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
         BlockState blockstate1 = Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
         this.generateBox(p_228489_, p_228493_, 0, 3, 2, 0, 5, 4, blockstate1, blockstate1, false);
         this.generateBox(p_228489_, p_228493_, 6, 3, 2, 6, 5, 2, blockstate1, blockstate1, false);
         this.generateBox(p_228489_, p_228493_, 6, 3, 4, 6, 5, 4, blockstate1, blockstate1, false);
         this.placeBlock(p_228489_, Blocks.NETHER_BRICKS.defaultBlockState(), 5, 2, 5, p_228493_);
         this.generateBox(p_228489_, p_228493_, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 6, 8, 2, 6, 8, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
         this.generateBox(p_228489_, p_228493_, 2, 5, 0, 4, 5, 0, blockstate, blockstate, false);

         for (int i = 0; i <= 6; i++) {
            for (int j = 0; j <= 6; j++) {
               this.fillColumnDown(p_228489_, Blocks.NETHER_BRICKS.defaultBlockState(), i, -1, j, p_228493_);
            }
         }
      }
   }

   public static class StartPiece extends NetherFortressPieces.BridgeCrossing {
      NetherFortressPieces.@Nullable PieceWeight previousPiece;
      final List<NetherFortressPieces.PieceWeight> availableBridgePieces = new ArrayList<>();
      final List<NetherFortressPieces.PieceWeight> availableCastlePieces = new ArrayList<>();
      public final List<StructurePiece> pendingChildren = Lists.newArrayList();

      public StartPiece(RandomSource p_228512_, int p_228513_, int p_228514_) {
         super(p_228513_, p_228514_, getRandomHorizontalDirection(p_228512_));

         for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight : NetherFortressPieces.BRIDGE_PIECE_WEIGHTS) {
            netherfortresspieces$pieceweight.placeCount = 0;
            this.availableBridgePieces.add(netherfortresspieces$pieceweight);
         }

         for (NetherFortressPieces.PieceWeight netherfortresspieces$pieceweight1 : NetherFortressPieces.CASTLE_PIECE_WEIGHTS) {
            netherfortresspieces$pieceweight1.placeCount = 0;
            this.availableCastlePieces.add(netherfortresspieces$pieceweight1);
         }
      }

      public StartPiece(CompoundTag p_228516_) {
         super(StructurePieceType.NETHER_FORTRESS_START, p_228516_);
      }
   }
}
