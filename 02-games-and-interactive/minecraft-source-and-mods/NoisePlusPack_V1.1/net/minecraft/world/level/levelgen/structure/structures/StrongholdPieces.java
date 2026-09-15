package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
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
         public boolean doPlace(int p_229450_) {
            return super.doPlace(p_229450_) && p_229450_ > 4;
         }
      },
      new StrongholdPieces.PieceWeight(StrongholdPieces.PortalRoom.class, 20, 1) {
         @Override
         public boolean doPlace(int p_229456_) {
            return super.doPlace(p_229456_) && p_229456_ > 5;
         }
      }
   };
   private static List<StrongholdPieces.PieceWeight> currentPieces;
   static @Nullable Class<? extends StrongholdPieces.StrongholdPiece> imposedPiece;
   private static int totalWeight;
   static final StrongholdPieces.SmoothStoneSelector SMOOTH_STONE_SELECTOR = new StrongholdPieces.SmoothStoneSelector();

   public static void resetPieces() {
      currentPieces = Lists.newArrayList();

      for (StrongholdPieces.PieceWeight strongholdpieces$pieceweight : STRONGHOLD_PIECE_WEIGHTS) {
         strongholdpieces$pieceweight.placeCount = 0;
         currentPieces.add(strongholdpieces$pieceweight);
      }

      imposedPiece = null;
   }

   private static boolean updatePieceWeight() {
      boolean flag = false;
      totalWeight = 0;

      for (StrongholdPieces.PieceWeight strongholdpieces$pieceweight : currentPieces) {
         if (strongholdpieces$pieceweight.maxPlaceCount > 0 && strongholdpieces$pieceweight.placeCount < strongholdpieces$pieceweight.maxPlaceCount) {
            flag = true;
         }

         totalWeight = totalWeight + strongholdpieces$pieceweight.weight;
      }

      return flag;
   }

   private static StrongholdPieces.@Nullable StrongholdPiece findAndCreatePieceFactory(
      Class<? extends StrongholdPieces.StrongholdPiece> p_229427_,
      StructurePieceAccessor p_229428_,
      RandomSource p_229429_,
      int p_229430_,
      int p_229431_,
      int p_229432_,
      Direction p_229433_,
      int p_229434_
   ) {
      StrongholdPieces.StrongholdPiece strongholdpieces$strongholdpiece = null;
      if (p_229427_ == StrongholdPieces.Straight.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.Straight.createPiece(p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_);
      } else if (p_229427_ == StrongholdPieces.PrisonHall.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.PrisonHall.createPiece(p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_);
      } else if (p_229427_ == StrongholdPieces.LeftTurn.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.LeftTurn.createPiece(p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_);
      } else if (p_229427_ == StrongholdPieces.RightTurn.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.RightTurn.createPiece(p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_);
      } else if (p_229427_ == StrongholdPieces.RoomCrossing.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.RoomCrossing.createPiece(
            p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_
         );
      } else if (p_229427_ == StrongholdPieces.StraightStairsDown.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.StraightStairsDown.createPiece(
            p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_
         );
      } else if (p_229427_ == StrongholdPieces.StairsDown.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.StairsDown.createPiece(p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_);
      } else if (p_229427_ == StrongholdPieces.FiveCrossing.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.FiveCrossing.createPiece(
            p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_
         );
      } else if (p_229427_ == StrongholdPieces.ChestCorridor.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.ChestCorridor.createPiece(
            p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_
         );
      } else if (p_229427_ == StrongholdPieces.Library.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.Library.createPiece(p_229428_, p_229429_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_);
      } else if (p_229427_ == StrongholdPieces.PortalRoom.class) {
         strongholdpieces$strongholdpiece = StrongholdPieces.PortalRoom.createPiece(p_229428_, p_229430_, p_229431_, p_229432_, p_229433_, p_229434_);
      }

      return strongholdpieces$strongholdpiece;
   }

   private static StrongholdPieces.@Nullable StrongholdPiece generatePieceFromSmallDoor(
      StrongholdPieces.StartPiece p_229418_,
      StructurePieceAccessor p_229419_,
      RandomSource p_229420_,
      int p_229421_,
      int p_229422_,
      int p_229423_,
      Direction p_229424_,
      int p_229425_
   ) {
      if (!updatePieceWeight()) {
         return null;
      }

      if (imposedPiece != null) {
         StrongholdPieces.StrongholdPiece strongholdpieces$strongholdpiece = findAndCreatePieceFactory(
            imposedPiece, p_229419_, p_229420_, p_229421_, p_229422_, p_229423_, p_229424_, p_229425_
         );
         imposedPiece = null;
         if (strongholdpieces$strongholdpiece != null) {
            return strongholdpieces$strongholdpiece;
         }
      }

      int j = 0;

      while (j < 5) {
         j++;
         int i = p_229420_.nextInt(totalWeight);

         for (StrongholdPieces.PieceWeight strongholdpieces$pieceweight : currentPieces) {
            i -= strongholdpieces$pieceweight.weight;
            if (i < 0) {
               if (!strongholdpieces$pieceweight.doPlace(p_229425_) || strongholdpieces$pieceweight == p_229418_.previousPiece) {
                  break;
               }

               StrongholdPieces.StrongholdPiece strongholdpieces$strongholdpiece1 = findAndCreatePieceFactory(
                  strongholdpieces$pieceweight.pieceClass, p_229419_, p_229420_, p_229421_, p_229422_, p_229423_, p_229424_, p_229425_
               );
               if (strongholdpieces$strongholdpiece1 != null) {
                  strongholdpieces$pieceweight.placeCount++;
                  p_229418_.previousPiece = strongholdpieces$pieceweight;
                  if (!strongholdpieces$pieceweight.isValid()) {
                     currentPieces.remove(strongholdpieces$pieceweight);
                  }

                  return strongholdpieces$strongholdpiece1;
               }
            }
         }
      }

      BoundingBox boundingbox = StrongholdPieces.FillerCorridor.findPieceBox(p_229419_, p_229420_, p_229421_, p_229422_, p_229423_, p_229424_);
      return boundingbox != null && boundingbox.minY() > 1 ? new StrongholdPieces.FillerCorridor(p_229425_, boundingbox, p_229424_) : null;
   }

   static @Nullable StructurePiece generateAndAddPiece(
      StrongholdPieces.StartPiece p_229437_,
      StructurePieceAccessor p_229438_,
      RandomSource p_229439_,
      int p_229440_,
      int p_229441_,
      int p_229442_,
      Direction p_229443_,
      int p_229444_
   ) {
      if (p_229444_ > 50) {
         return null;
      }

      if (Math.abs(p_229440_ - p_229437_.getBoundingBox().minX()) <= 112 && Math.abs(p_229442_ - p_229437_.getBoundingBox().minZ()) <= 112) {
         StructurePiece structurepiece = generatePieceFromSmallDoor(p_229437_, p_229438_, p_229439_, p_229440_, p_229441_, p_229442_, p_229443_, p_229444_ + 1);
         if (structurepiece != null) {
            p_229438_.addPiece(structurepiece);
            p_229437_.pendingChildren.add(structurepiece);
         }

         return structurepiece;
      } else {
         return null;
      }
   }

   public static class ChestCorridor extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 5;
      private static final int DEPTH = 7;
      private boolean hasPlacedChest;

      public ChestCorridor(int p_229465_, RandomSource p_229466_, BoundingBox p_229467_, Direction p_229468_) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, p_229465_, p_229467_);
         this.setOrientation(p_229468_);
         this.entryDoor = this.randomSmallDoor(p_229466_);
      }

      public ChestCorridor(CompoundTag p_229470_) {
         super(StructurePieceType.STRONGHOLD_CHEST_CORRIDOR, p_229470_);
         this.hasPlacedChest = p_229470_.getBooleanOr("Chest", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229492_, CompoundTag p_229493_) {
         super.addAdditionalSaveData(p_229492_, p_229493_);
         p_229493_.putBoolean("Chest", this.hasPlacedChest);
      }

      @Override
      public void addChildren(StructurePiece p_229480_, StructurePieceAccessor p_229481_, RandomSource p_229482_) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229480_, p_229481_, p_229482_, 1, 1);
      }

      public static StrongholdPieces.@Nullable ChestCorridor createPiece(
         StructurePieceAccessor p_229484_, RandomSource p_229485_, int p_229486_, int p_229487_, int p_229488_, Direction p_229489_, int p_229490_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229486_, p_229487_, p_229488_, -1, -1, 0, 5, 5, 7, p_229489_);
         return isOkBox(boundingbox) && p_229484_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.ChestCorridor(p_229490_, p_229485_, boundingbox, p_229489_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229472_,
         StructureManager p_229473_,
         ChunkGenerator p_229474_,
         RandomSource p_229475_,
         BoundingBox p_229476_,
         ChunkPos p_229477_,
         BlockPos p_229478_
      ) {
         this.generateBox(p_229472_, p_229476_, 0, 0, 0, 4, 4, 6, true, p_229475_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229472_, p_229475_, p_229476_, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(p_229472_, p_229475_, p_229476_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         this.generateBox(p_229472_, p_229476_, 3, 1, 2, 3, 1, 4, Blocks.STONE_BRICKS.defaultBlockState(), Blocks.STONE_BRICKS.defaultBlockState(), false);
         this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 1, p_229476_);
         this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 1, 5, p_229476_);
         this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 2, p_229476_);
         this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3, 2, 4, p_229476_);

         for (int i = 2; i <= 4; i++) {
            this.placeBlock(p_229472_, Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2, 1, i, p_229476_);
         }

         if (!this.hasPlacedChest && p_229476_.isInside(this.getWorldPos(3, 2, 3))) {
            this.hasPlacedChest = true;
            this.createChest(p_229472_, p_229476_, p_229475_, 3, 2, 3, BuiltInLootTables.STRONGHOLD_CORRIDOR);
         }
      }
   }

   public static class FillerCorridor extends StrongholdPieces.StrongholdPiece {
      private final int steps;

      public FillerCorridor(int p_229496_, BoundingBox p_229497_, Direction p_229498_) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, p_229496_, p_229497_);
         this.setOrientation(p_229498_);
         this.steps = p_229498_ != Direction.NORTH && p_229498_ != Direction.SOUTH ? p_229497_.getXSpan() : p_229497_.getZSpan();
      }

      public FillerCorridor(CompoundTag p_229500_) {
         super(StructurePieceType.STRONGHOLD_FILLER_CORRIDOR, p_229500_);
         this.steps = p_229500_.getIntOr("Steps", 0);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229517_, CompoundTag p_229518_) {
         super.addAdditionalSaveData(p_229517_, p_229518_);
         p_229518_.putInt("Steps", this.steps);
      }

      public static @Nullable BoundingBox findPieceBox(
         StructurePieceAccessor p_229510_, RandomSource p_229511_, int p_229512_, int p_229513_, int p_229514_, Direction p_229515_
      ) {
         int i = 3;
         BoundingBox boundingbox = BoundingBox.orientBox(p_229512_, p_229513_, p_229514_, -1, -1, 0, 5, 5, 4, p_229515_);
         StructurePiece structurepiece = p_229510_.findCollisionPiece(boundingbox);
         if (structurepiece == null) {
            return null;
         }

         if (structurepiece.getBoundingBox().minY() == boundingbox.minY()) {
            for (int j = 2; j >= 1; j--) {
               boundingbox = BoundingBox.orientBox(p_229512_, p_229513_, p_229514_, -1, -1, 0, 5, 5, j, p_229515_);
               if (!structurepiece.getBoundingBox().intersects(boundingbox)) {
                  return BoundingBox.orientBox(p_229512_, p_229513_, p_229514_, -1, -1, 0, 5, 5, j + 1, p_229515_);
               }
            }
         }

         return null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229502_,
         StructureManager p_229503_,
         ChunkGenerator p_229504_,
         RandomSource p_229505_,
         BoundingBox p_229506_,
         ChunkPos p_229507_,
         BlockPos p_229508_
      ) {
         for (int i = 0; i < this.steps; i++) {
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 0, 0, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 0, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 0, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 0, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 4, 0, i, p_229506_);

            for (int j = 1; j <= 3; j++) {
               this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 0, j, i, p_229506_);
               this.placeBlock(p_229502_, Blocks.CAVE_AIR.defaultBlockState(), 1, j, i, p_229506_);
               this.placeBlock(p_229502_, Blocks.CAVE_AIR.defaultBlockState(), 2, j, i, p_229506_);
               this.placeBlock(p_229502_, Blocks.CAVE_AIR.defaultBlockState(), 3, j, i, p_229506_);
               this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 4, j, i, p_229506_);
            }

            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 0, 4, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 4, i, p_229506_);
            this.placeBlock(p_229502_, Blocks.STONE_BRICKS.defaultBlockState(), 4, 4, i, p_229506_);
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

      public FiveCrossing(int p_229527_, RandomSource p_229528_, BoundingBox p_229529_, Direction p_229530_) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, p_229527_, p_229529_);
         this.setOrientation(p_229530_);
         this.entryDoor = this.randomSmallDoor(p_229528_);
         this.leftLow = p_229528_.nextBoolean();
         this.leftHigh = p_229528_.nextBoolean();
         this.rightLow = p_229528_.nextBoolean();
         this.rightHigh = p_229528_.nextInt(3) > 0;
      }

      public FiveCrossing(CompoundTag p_229532_) {
         super(StructurePieceType.STRONGHOLD_FIVE_CROSSING, p_229532_);
         this.leftLow = p_229532_.getBooleanOr("leftLow", false);
         this.leftHigh = p_229532_.getBooleanOr("leftHigh", false);
         this.rightLow = p_229532_.getBooleanOr("rightLow", false);
         this.rightHigh = p_229532_.getBooleanOr("rightHigh", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229554_, CompoundTag p_229555_) {
         super.addAdditionalSaveData(p_229554_, p_229555_);
         p_229555_.putBoolean("leftLow", this.leftLow);
         p_229555_.putBoolean("leftHigh", this.leftHigh);
         p_229555_.putBoolean("rightLow", this.rightLow);
         p_229555_.putBoolean("rightHigh", this.rightHigh);
      }

      @Override
      public void addChildren(StructurePiece p_229542_, StructurePieceAccessor p_229543_, RandomSource p_229544_) {
         int i = 3;
         int j = 5;
         Direction direction = this.getOrientation();
         if (direction == Direction.WEST || direction == Direction.NORTH) {
            i = 8 - i;
            j = 8 - j;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, 5, 1);
         if (this.leftLow) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, i, 1);
         }

         if (this.leftHigh) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, j, 7);
         }

         if (this.rightLow) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, i, 1);
         }

         if (this.rightHigh) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229542_, p_229543_, p_229544_, j, 7);
         }
      }

      public static StrongholdPieces.@Nullable FiveCrossing createPiece(
         StructurePieceAccessor p_229546_, RandomSource p_229547_, int p_229548_, int p_229549_, int p_229550_, Direction p_229551_, int p_229552_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229548_, p_229549_, p_229550_, -4, -3, 0, 10, 9, 11, p_229551_);
         return isOkBox(boundingbox) && p_229546_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.FiveCrossing(p_229552_, p_229547_, boundingbox, p_229551_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229534_,
         StructureManager p_229535_,
         ChunkGenerator p_229536_,
         RandomSource p_229537_,
         BoundingBox p_229538_,
         ChunkPos p_229539_,
         BlockPos p_229540_
      ) {
         this.generateBox(p_229534_, p_229538_, 0, 0, 0, 9, 8, 10, true, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229534_, p_229537_, p_229538_, this.entryDoor, 4, 3, 0);
         if (this.leftLow) {
            this.generateBox(p_229534_, p_229538_, 0, 3, 1, 0, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightLow) {
            this.generateBox(p_229534_, p_229538_, 9, 3, 1, 9, 5, 3, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.leftHigh) {
            this.generateBox(p_229534_, p_229538_, 0, 5, 7, 0, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightHigh) {
            this.generateBox(p_229534_, p_229538_, 9, 5, 7, 9, 7, 9, CAVE_AIR, CAVE_AIR, false);
         }

         this.generateBox(p_229534_, p_229538_, 5, 1, 10, 7, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(p_229534_, p_229538_, 1, 2, 1, 8, 2, 6, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229534_, p_229538_, 4, 1, 5, 4, 4, 9, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229534_, p_229538_, 8, 1, 5, 8, 4, 9, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229534_, p_229538_, 1, 4, 7, 3, 4, 9, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229534_, p_229538_, 1, 3, 5, 3, 3, 6, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(
            p_229534_, p_229538_, 1, 3, 4, 3, 3, 4, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false
         );
         this.generateBox(
            p_229534_, p_229538_, 1, 4, 6, 3, 4, 6, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false
         );
         this.generateBox(p_229534_, p_229538_, 5, 1, 7, 7, 1, 8, false, p_229537_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(
            p_229534_, p_229538_, 5, 1, 9, 7, 1, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false
         );
         this.generateBox(
            p_229534_, p_229538_, 5, 2, 7, 7, 2, 7, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false
         );
         this.generateBox(
            p_229534_, p_229538_, 4, 5, 7, 4, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false
         );
         this.generateBox(
            p_229534_, p_229538_, 8, 5, 7, 8, 5, 9, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), false
         );
         this.generateBox(
            p_229534_,
            p_229538_,
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
         this.placeBlock(p_229534_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 6, 5, 6, p_229538_);
      }
   }

   public static class LeftTurn extends StrongholdPieces.Turn {
      public LeftTurn(int p_229557_, RandomSource p_229558_, BoundingBox p_229559_, Direction p_229560_) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, p_229557_, p_229559_);
         this.setOrientation(p_229560_);
         this.entryDoor = this.randomSmallDoor(p_229558_);
      }

      public LeftTurn(CompoundTag p_229562_) {
         super(StructurePieceType.STRONGHOLD_LEFT_TURN, p_229562_);
      }

      @Override
      public void addChildren(StructurePiece p_229572_, StructurePieceAccessor p_229573_, RandomSource p_229574_) {
         Direction direction = this.getOrientation();
         if (direction != Direction.NORTH && direction != Direction.EAST) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229572_, p_229573_, p_229574_, 1, 1);
         } else {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229572_, p_229573_, p_229574_, 1, 1);
         }
      }

      public static StrongholdPieces.@Nullable LeftTurn createPiece(
         StructurePieceAccessor p_229576_, RandomSource p_229577_, int p_229578_, int p_229579_, int p_229580_, Direction p_229581_, int p_229582_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229578_, p_229579_, p_229580_, -1, -1, 0, 5, 5, 5, p_229581_);
         return isOkBox(boundingbox) && p_229576_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.LeftTurn(p_229582_, p_229577_, boundingbox, p_229581_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229564_,
         StructureManager p_229565_,
         ChunkGenerator p_229566_,
         RandomSource p_229567_,
         BoundingBox p_229568_,
         ChunkPos p_229569_,
         BlockPos p_229570_
      ) {
         this.generateBox(p_229564_, p_229568_, 0, 0, 0, 4, 4, 4, true, p_229567_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229564_, p_229567_, p_229568_, this.entryDoor, 1, 1, 0);
         Direction direction = this.getOrientation();
         if (direction != Direction.NORTH && direction != Direction.EAST) {
            this.generateBox(p_229564_, p_229568_, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(p_229564_, p_229568_, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class Library extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 14;
      protected static final int HEIGHT = 6;
      protected static final int TALL_HEIGHT = 11;
      protected static final int DEPTH = 15;
      private final boolean isTall;

      public Library(int p_229589_, RandomSource p_229590_, BoundingBox p_229591_, Direction p_229592_) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, p_229589_, p_229591_);
         this.setOrientation(p_229592_);
         this.entryDoor = this.randomSmallDoor(p_229590_);
         this.isTall = p_229591_.getYSpan() > 6;
      }

      public Library(CompoundTag p_229594_) {
         super(StructurePieceType.STRONGHOLD_LIBRARY, p_229594_);
         this.isTall = p_229594_.getBooleanOr("Tall", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229612_, CompoundTag p_229613_) {
         super.addAdditionalSaveData(p_229612_, p_229613_);
         p_229613_.putBoolean("Tall", this.isTall);
      }

      public static StrongholdPieces.@Nullable Library createPiece(
         StructurePieceAccessor p_229604_, RandomSource p_229605_, int p_229606_, int p_229607_, int p_229608_, Direction p_229609_, int p_229610_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229606_, p_229607_, p_229608_, -4, -1, 0, 14, 11, 15, p_229609_);
         if (!isOkBox(boundingbox) || p_229604_.findCollisionPiece(boundingbox) != null) {
            boundingbox = BoundingBox.orientBox(p_229606_, p_229607_, p_229608_, -4, -1, 0, 14, 6, 15, p_229609_);
            if (!isOkBox(boundingbox) || p_229604_.findCollisionPiece(boundingbox) != null) {
               return null;
            }
         }

         return new StrongholdPieces.Library(p_229610_, p_229605_, boundingbox, p_229609_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229596_,
         StructureManager p_229597_,
         ChunkGenerator p_229598_,
         RandomSource p_229599_,
         BoundingBox p_229600_,
         ChunkPos p_229601_,
         BlockPos p_229602_
      ) {
         int i = 11;
         if (!this.isTall) {
            i = 6;
         }

         this.generateBox(p_229596_, p_229600_, 0, 0, 0, 13, i - 1, 14, true, p_229599_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229596_, p_229599_, p_229600_, this.entryDoor, 4, 1, 0);
         this.generateMaybeBox(
            p_229596_, p_229600_, p_229599_, 0.07F, 2, 1, 1, 11, 4, 13, Blocks.COBWEB.defaultBlockState(), Blocks.COBWEB.defaultBlockState(), false, false
         );
         int j = 1;
         int k = 12;

         for (int l = 1; l <= 13; l++) {
            if ((l - 1) % 4 == 0) {
               this.generateBox(p_229596_, p_229600_, 1, 1, l, 1, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.generateBox(p_229596_, p_229600_, 12, 1, l, 12, 4, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
               this.placeBlock(p_229596_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 2, 3, l, p_229600_);
               this.placeBlock(p_229596_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 11, 3, l, p_229600_);
               if (this.isTall) {
                  this.generateBox(p_229596_, p_229600_, 1, 6, l, 1, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
                  this.generateBox(
                     p_229596_, p_229600_, 12, 6, l, 12, 9, l, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false
                  );
               }
            } else {
               this.generateBox(p_229596_, p_229600_, 1, 1, l, 1, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               this.generateBox(p_229596_, p_229600_, 12, 1, l, 12, 4, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               if (this.isTall) {
                  this.generateBox(p_229596_, p_229600_, 1, 6, l, 1, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
                  this.generateBox(p_229596_, p_229600_, 12, 6, l, 12, 9, l, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
               }
            }
         }

         for (int l1 = 3; l1 < 12; l1 += 2) {
            this.generateBox(p_229596_, p_229600_, 3, 1, l1, 4, 3, l1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(p_229596_, p_229600_, 6, 1, l1, 7, 3, l1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
            this.generateBox(p_229596_, p_229600_, 9, 1, l1, 10, 3, l1, Blocks.BOOKSHELF.defaultBlockState(), Blocks.BOOKSHELF.defaultBlockState(), false);
         }

         if (this.isTall) {
            this.generateBox(p_229596_, p_229600_, 1, 5, 1, 3, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(p_229596_, p_229600_, 10, 5, 1, 12, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(p_229596_, p_229600_, 4, 5, 1, 9, 5, 2, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.generateBox(p_229596_, p_229600_, 4, 5, 12, 9, 5, 13, Blocks.OAK_PLANKS.defaultBlockState(), Blocks.OAK_PLANKS.defaultBlockState(), false);
            this.placeBlock(p_229596_, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 11, p_229600_);
            this.placeBlock(p_229596_, Blocks.OAK_PLANKS.defaultBlockState(), 8, 5, 11, p_229600_);
            this.placeBlock(p_229596_, Blocks.OAK_PLANKS.defaultBlockState(), 9, 5, 10, p_229600_);
            BlockState blockstate5 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            BlockState blockstate = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true);
            this.generateBox(p_229596_, p_229600_, 3, 6, 3, 3, 6, 11, blockstate, blockstate, false);
            this.generateBox(p_229596_, p_229600_, 10, 6, 3, 10, 6, 9, blockstate, blockstate, false);
            this.generateBox(p_229596_, p_229600_, 4, 6, 2, 9, 6, 2, blockstate5, blockstate5, false);
            this.generateBox(p_229596_, p_229600_, 4, 6, 12, 7, 6, 12, blockstate5, blockstate5, false);
            this.placeBlock(
               p_229596_, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true), 3, 6, 2, p_229600_
            );
            this.placeBlock(
               p_229596_, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.EAST, true), 3, 6, 12, p_229600_
            );
            this.placeBlock(
               p_229596_, Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.WEST, true), 10, 6, 2, p_229600_
            );

            for (int i1 = 0; i1 <= 2; i1++) {
               this.placeBlock(
                  p_229596_,
                  Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true).setValue(FenceBlock.WEST, true),
                  8 + i1,
                  6,
                  12 - i1,
                  p_229600_
               );
               if (i1 != 2) {
                  this.placeBlock(
                     p_229596_,
                     Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.EAST, true),
                     8 + i1,
                     6,
                     11 - i1,
                     p_229600_
                  );
               }
            }

            BlockState blockstate6 = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            this.placeBlock(p_229596_, blockstate6, 10, 1, 13, p_229600_);
            this.placeBlock(p_229596_, blockstate6, 10, 2, 13, p_229600_);
            this.placeBlock(p_229596_, blockstate6, 10, 3, 13, p_229600_);
            this.placeBlock(p_229596_, blockstate6, 10, 4, 13, p_229600_);
            this.placeBlock(p_229596_, blockstate6, 10, 5, 13, p_229600_);
            this.placeBlock(p_229596_, blockstate6, 10, 6, 13, p_229600_);
            this.placeBlock(p_229596_, blockstate6, 10, 7, 13, p_229600_);
            int j1 = 7;
            int k1 = 7;
            BlockState blockstate1 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true);
            this.placeBlock(p_229596_, blockstate1, 6, 9, 7, p_229600_);
            BlockState blockstate2 = Blocks.OAK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true);
            this.placeBlock(p_229596_, blockstate2, 7, 9, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate1, 6, 8, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate2, 7, 8, 7, p_229600_);
            BlockState blockstate3 = blockstate.setValue(FenceBlock.WEST, true).setValue(FenceBlock.EAST, true);
            this.placeBlock(p_229596_, blockstate3, 6, 7, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate3, 7, 7, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate1, 5, 7, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate2, 8, 7, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate1.setValue(FenceBlock.NORTH, true), 6, 7, 6, p_229600_);
            this.placeBlock(p_229596_, blockstate1.setValue(FenceBlock.SOUTH, true), 6, 7, 8, p_229600_);
            this.placeBlock(p_229596_, blockstate2.setValue(FenceBlock.NORTH, true), 7, 7, 6, p_229600_);
            this.placeBlock(p_229596_, blockstate2.setValue(FenceBlock.SOUTH, true), 7, 7, 8, p_229600_);
            BlockState blockstate4 = Blocks.TORCH.defaultBlockState();
            this.placeBlock(p_229596_, blockstate4, 5, 8, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate4, 8, 8, 7, p_229600_);
            this.placeBlock(p_229596_, blockstate4, 6, 8, 6, p_229600_);
            this.placeBlock(p_229596_, blockstate4, 6, 8, 8, p_229600_);
            this.placeBlock(p_229596_, blockstate4, 7, 8, 6, p_229600_);
            this.placeBlock(p_229596_, blockstate4, 7, 8, 8, p_229600_);
         }

         this.createChest(p_229596_, p_229600_, p_229599_, 3, 3, 5, BuiltInLootTables.STRONGHOLD_LIBRARY);
         if (this.isTall) {
            this.placeBlock(p_229596_, CAVE_AIR, 12, 9, 1, p_229600_);
            this.createChest(p_229596_, p_229600_, p_229599_, 12, 8, 1, BuiltInLootTables.STRONGHOLD_LIBRARY);
         }
      }
   }

   static class PieceWeight {
      public final Class<? extends StrongholdPieces.StrongholdPiece> pieceClass;
      public final int weight;
      public int placeCount;
      public final int maxPlaceCount;

      public PieceWeight(Class<? extends StrongholdPieces.StrongholdPiece> p_229619_, int p_229620_, int p_229621_) {
         this.pieceClass = p_229619_;
         this.weight = p_229620_;
         this.maxPlaceCount = p_229621_;
      }

      public boolean doPlace(int p_229623_) {
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

      public PortalRoom(int p_229629_, BoundingBox p_229630_, Direction p_229631_) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, p_229629_, p_229630_);
         this.setOrientation(p_229631_);
      }

      public PortalRoom(CompoundTag p_229633_) {
         super(StructurePieceType.STRONGHOLD_PORTAL_ROOM, p_229633_);
         this.hasPlacedSpawner = p_229633_.getBooleanOr("Mob", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229654_, CompoundTag p_229655_) {
         super.addAdditionalSaveData(p_229654_, p_229655_);
         p_229655_.putBoolean("Mob", this.hasPlacedSpawner);
      }

      @Override
      public void addChildren(StructurePiece p_229643_, StructurePieceAccessor p_229644_, RandomSource p_229645_) {
         if (p_229643_ != null) {
            ((StrongholdPieces.StartPiece)p_229643_).portalRoomPiece = this;
         }
      }

      public static StrongholdPieces.@Nullable PortalRoom createPiece(
         StructurePieceAccessor p_229647_, int p_229648_, int p_229649_, int p_229650_, Direction p_229651_, int p_229652_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229648_, p_229649_, p_229650_, -4, -1, 0, 11, 8, 16, p_229651_);
         return isOkBox(boundingbox) && p_229647_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.PortalRoom(p_229652_, boundingbox, p_229651_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229635_,
         StructureManager p_229636_,
         ChunkGenerator p_229637_,
         RandomSource p_229638_,
         BoundingBox p_229639_,
         ChunkPos p_229640_,
         BlockPos p_229641_
      ) {
         this.generateBox(p_229635_, p_229639_, 0, 0, 0, 10, 7, 15, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229635_, p_229638_, p_229639_, StrongholdPieces.StrongholdPiece.SmallDoorType.GRATES, 4, 1, 0);
         int i = 6;
         this.generateBox(p_229635_, p_229639_, 1, 6, 1, 1, 6, 14, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 9, 6, 1, 9, 6, 14, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 2, 6, 1, 8, 6, 2, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 2, 6, 14, 8, 6, 14, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 1, 1, 1, 2, 1, 4, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 8, 1, 1, 9, 1, 4, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 1, 1, 1, 1, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(p_229635_, p_229639_, 9, 1, 1, 9, 1, 3, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         this.generateBox(p_229635_, p_229639_, 3, 1, 8, 7, 1, 12, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 4, 1, 9, 6, 1, 11, Blocks.LAVA.defaultBlockState(), Blocks.LAVA.defaultBlockState(), false);
         BlockState blockstate = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
         BlockState blockstate1 = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true);

         for (int j = 3; j < 14; j += 2) {
            this.generateBox(p_229635_, p_229639_, 0, 3, j, 0, 4, j, blockstate, blockstate, false);
            this.generateBox(p_229635_, p_229639_, 10, 3, j, 10, 4, j, blockstate, blockstate, false);
         }

         for (int i1 = 2; i1 < 9; i1 += 2) {
            this.generateBox(p_229635_, p_229639_, i1, 3, 15, i1, 4, 15, blockstate1, blockstate1, false);
         }

         BlockState blockstate5 = Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         this.generateBox(p_229635_, p_229639_, 4, 1, 5, 6, 1, 7, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 4, 2, 6, 6, 2, 7, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229635_, p_229639_, 4, 3, 7, 6, 3, 7, false, p_229638_, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int k = 4; k <= 6; k++) {
            this.placeBlock(p_229635_, blockstate5, k, 1, 4, p_229639_);
            this.placeBlock(p_229635_, blockstate5, k, 2, 5, p_229639_);
            this.placeBlock(p_229635_, blockstate5, k, 3, 6, p_229639_);
         }

         BlockState blockstate6 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.NORTH);
         BlockState blockstate2 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.SOUTH);
         BlockState blockstate3 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.EAST);
         BlockState blockstate4 = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, Direction.WEST);
         boolean flag = true;
         boolean[] aboolean = new boolean[12];

         for (int l = 0; l < aboolean.length; l++) {
            aboolean[l] = p_229638_.nextFloat() > 0.9F;
            flag &= aboolean[l];
         }

         this.placeBlock(p_229635_, blockstate6.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[0]), 4, 3, 8, p_229639_);
         this.placeBlock(p_229635_, blockstate6.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[1]), 5, 3, 8, p_229639_);
         this.placeBlock(p_229635_, blockstate6.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[2]), 6, 3, 8, p_229639_);
         this.placeBlock(p_229635_, blockstate2.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[3]), 4, 3, 12, p_229639_);
         this.placeBlock(p_229635_, blockstate2.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[4]), 5, 3, 12, p_229639_);
         this.placeBlock(p_229635_, blockstate2.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[5]), 6, 3, 12, p_229639_);
         this.placeBlock(p_229635_, blockstate3.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[6]), 3, 3, 9, p_229639_);
         this.placeBlock(p_229635_, blockstate3.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[7]), 3, 3, 10, p_229639_);
         this.placeBlock(p_229635_, blockstate3.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[8]), 3, 3, 11, p_229639_);
         this.placeBlock(p_229635_, blockstate4.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[9]), 7, 3, 9, p_229639_);
         this.placeBlock(p_229635_, blockstate4.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[10]), 7, 3, 10, p_229639_);
         this.placeBlock(p_229635_, blockstate4.setValue(EndPortalFrameBlock.HAS_EYE, aboolean[11]), 7, 3, 11, p_229639_);
         if (flag) {
            BlockState blockstate7 = Blocks.END_PORTAL.defaultBlockState();
            this.placeBlock(p_229635_, blockstate7, 4, 3, 9, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 5, 3, 9, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 6, 3, 9, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 4, 3, 10, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 5, 3, 10, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 6, 3, 10, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 4, 3, 11, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 5, 3, 11, p_229639_);
            this.placeBlock(p_229635_, blockstate7, 6, 3, 11, p_229639_);
         }

         if (!this.hasPlacedSpawner) {
            BlockPos blockpos = this.getWorldPos(5, 3, 6);
            if (p_229639_.isInside(blockpos)) {
               this.hasPlacedSpawner = true;
               p_229635_.setBlock(blockpos, Blocks.SPAWNER.defaultBlockState(), 2);
               if (p_229635_.getBlockEntity(blockpos) instanceof SpawnerBlockEntity spawnerblockentity) {
                  spawnerblockentity.setEntityId(EntityType.SILVERFISH, p_229638_);
               }
            }
         }
      }
   }

   public static class PrisonHall extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 9;
      protected static final int HEIGHT = 5;
      protected static final int DEPTH = 11;

      public PrisonHall(int p_229660_, RandomSource p_229661_, BoundingBox p_229662_, Direction p_229663_) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, p_229660_, p_229662_);
         this.setOrientation(p_229663_);
         this.entryDoor = this.randomSmallDoor(p_229661_);
      }

      public PrisonHall(CompoundTag p_229665_) {
         super(StructurePieceType.STRONGHOLD_PRISON_HALL, p_229665_);
      }

      @Override
      public void addChildren(StructurePiece p_229675_, StructurePieceAccessor p_229676_, RandomSource p_229677_) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229675_, p_229676_, p_229677_, 1, 1);
      }

      public static StrongholdPieces.@Nullable PrisonHall createPiece(
         StructurePieceAccessor p_229679_, RandomSource p_229680_, int p_229681_, int p_229682_, int p_229683_, Direction p_229684_, int p_229685_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229681_, p_229682_, p_229683_, -1, -1, 0, 9, 5, 11, p_229684_);
         return isOkBox(boundingbox) && p_229679_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.PrisonHall(p_229685_, p_229680_, boundingbox, p_229684_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229667_,
         StructureManager p_229668_,
         ChunkGenerator p_229669_,
         RandomSource p_229670_,
         BoundingBox p_229671_,
         ChunkPos p_229672_,
         BlockPos p_229673_
      ) {
         this.generateBox(p_229667_, p_229671_, 0, 0, 0, 8, 4, 10, true, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229667_, p_229670_, p_229671_, this.entryDoor, 1, 1, 0);
         this.generateBox(p_229667_, p_229671_, 1, 1, 10, 3, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(p_229667_, p_229671_, 4, 1, 1, 4, 3, 1, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229667_, p_229671_, 4, 1, 3, 4, 3, 3, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229667_, p_229671_, 4, 1, 7, 4, 3, 7, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateBox(p_229667_, p_229671_, 4, 1, 9, 4, 3, 9, false, p_229670_, StrongholdPieces.SMOOTH_STONE_SELECTOR);

         for (int i = 1; i <= 3; i++) {
            this.placeBlock(
               p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, i, 4, p_229671_
            );
            this.placeBlock(
               p_229667_,
               Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true).setValue(IronBarsBlock.EAST, true),
               4,
               i,
               5,
               p_229671_
            );
            this.placeBlock(
               p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, i, 6, p_229671_
            );
            this.placeBlock(
               p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 5, i, 5, p_229671_
            );
            this.placeBlock(
               p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 6, i, 5, p_229671_
            );
            this.placeBlock(
               p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true).setValue(IronBarsBlock.EAST, true), 7, i, 5, p_229671_
            );
         }

         this.placeBlock(
            p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 2, p_229671_
         );
         this.placeBlock(
            p_229667_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true), 4, 3, 8, p_229671_
         );
         BlockState blockstate1 = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.WEST);
         BlockState blockstate = Blocks.IRON_DOOR
            .defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.WEST)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
         this.placeBlock(p_229667_, blockstate1, 4, 1, 2, p_229671_);
         this.placeBlock(p_229667_, blockstate, 4, 2, 2, p_229671_);
         this.placeBlock(p_229667_, blockstate1, 4, 1, 8, p_229671_);
         this.placeBlock(p_229667_, blockstate, 4, 2, 8, p_229671_);
      }
   }

   public static class RightTurn extends StrongholdPieces.Turn {
      public RightTurn(int p_229687_, RandomSource p_229688_, BoundingBox p_229689_, Direction p_229690_) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, p_229687_, p_229689_);
         this.setOrientation(p_229690_);
         this.entryDoor = this.randomSmallDoor(p_229688_);
      }

      public RightTurn(CompoundTag p_229692_) {
         super(StructurePieceType.STRONGHOLD_RIGHT_TURN, p_229692_);
      }

      @Override
      public void addChildren(StructurePiece p_229702_, StructurePieceAccessor p_229703_, RandomSource p_229704_) {
         Direction direction = this.getOrientation();
         if (direction != Direction.NORTH && direction != Direction.EAST) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229702_, p_229703_, p_229704_, 1, 1);
         } else {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229702_, p_229703_, p_229704_, 1, 1);
         }
      }

      public static StrongholdPieces.@Nullable RightTurn createPiece(
         StructurePieceAccessor p_229706_, RandomSource p_229707_, int p_229708_, int p_229709_, int p_229710_, Direction p_229711_, int p_229712_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229708_, p_229709_, p_229710_, -1, -1, 0, 5, 5, 5, p_229711_);
         return isOkBox(boundingbox) && p_229706_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.RightTurn(p_229712_, p_229707_, boundingbox, p_229711_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229694_,
         StructureManager p_229695_,
         ChunkGenerator p_229696_,
         RandomSource p_229697_,
         BoundingBox p_229698_,
         ChunkPos p_229699_,
         BlockPos p_229700_
      ) {
         this.generateBox(p_229694_, p_229698_, 0, 0, 0, 4, 4, 4, true, p_229697_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229694_, p_229697_, p_229698_, this.entryDoor, 1, 1, 0);
         Direction direction = this.getOrientation();
         if (direction != Direction.NORTH && direction != Direction.EAST) {
            this.generateBox(p_229694_, p_229698_, 0, 1, 1, 0, 3, 3, CAVE_AIR, CAVE_AIR, false);
         } else {
            this.generateBox(p_229694_, p_229698_, 4, 1, 1, 4, 3, 3, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class RoomCrossing extends StrongholdPieces.StrongholdPiece {
      protected static final int WIDTH = 11;
      protected static final int HEIGHT = 7;
      protected static final int DEPTH = 11;
      protected final int type;

      public RoomCrossing(int p_229718_, RandomSource p_229719_, BoundingBox p_229720_, Direction p_229721_) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, p_229718_, p_229720_);
         this.setOrientation(p_229721_);
         this.entryDoor = this.randomSmallDoor(p_229719_);
         this.type = p_229719_.nextInt(5);
      }

      public RoomCrossing(CompoundTag p_229723_) {
         super(StructurePieceType.STRONGHOLD_ROOM_CROSSING, p_229723_);
         this.type = p_229723_.getIntOr("Type", 0);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229745_, CompoundTag p_229746_) {
         super.addAdditionalSaveData(p_229745_, p_229746_);
         p_229746_.putInt("Type", this.type);
      }

      @Override
      public void addChildren(StructurePiece p_229733_, StructurePieceAccessor p_229734_, RandomSource p_229735_) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229733_, p_229734_, p_229735_, 4, 1);
         this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229733_, p_229734_, p_229735_, 1, 4);
         this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229733_, p_229734_, p_229735_, 1, 4);
      }

      public static StrongholdPieces.@Nullable RoomCrossing createPiece(
         StructurePieceAccessor p_229737_, RandomSource p_229738_, int p_229739_, int p_229740_, int p_229741_, Direction p_229742_, int p_229743_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229739_, p_229740_, p_229741_, -4, -1, 0, 11, 7, 11, p_229742_);
         return isOkBox(boundingbox) && p_229737_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.RoomCrossing(p_229743_, p_229738_, boundingbox, p_229742_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229725_,
         StructureManager p_229726_,
         ChunkGenerator p_229727_,
         RandomSource p_229728_,
         BoundingBox p_229729_,
         ChunkPos p_229730_,
         BlockPos p_229731_
      ) {
         this.generateBox(p_229725_, p_229729_, 0, 0, 0, 10, 6, 10, true, p_229728_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229725_, p_229728_, p_229729_, this.entryDoor, 4, 1, 0);
         this.generateBox(p_229725_, p_229729_, 4, 1, 10, 6, 3, 10, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(p_229725_, p_229729_, 0, 1, 4, 0, 3, 6, CAVE_AIR, CAVE_AIR, false);
         this.generateBox(p_229725_, p_229729_, 10, 1, 4, 10, 3, 6, CAVE_AIR, CAVE_AIR, false);
         switch (this.type) {
            case 0:
               this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST), 4, 3, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST), 6, 3, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH), 5, 3, 4, p_229729_);
               this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH), 5, 3, 6, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 4, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 4, 1, 6, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 4, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 6, 1, 6, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 4, p_229729_);
               this.placeBlock(p_229725_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 5, 1, 6, p_229729_);
               break;
            case 1:
               for (int i1 = 0; i1 < 5; i1++) {
                  this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 1, 3 + i1, p_229729_);
                  this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 7, 1, 3 + i1, p_229729_);
                  this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i1, 1, 3, p_229729_);
                  this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 3 + i1, 1, 7, p_229729_);
               }

               this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 1, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 2, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.STONE_BRICKS.defaultBlockState(), 5, 3, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.WATER.defaultBlockState(), 5, 4, 5, p_229729_);
               break;
            case 2:
               for (int i = 1; i <= 9; i++) {
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 1, 3, i, p_229729_);
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 9, 3, i, p_229729_);
               }

               for (int j = 1; j <= 9; j++) {
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), j, 3, 1, p_229729_);
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), j, 3, 9, p_229729_);
               }

               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 4, p_229729_);
               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 1, 6, p_229729_);
               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 4, p_229729_);
               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 5, 3, 6, p_229729_);
               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, 1, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, 1, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, 3, 5, p_229729_);
               this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, 3, 5, p_229729_);

               for (int k = 1; k <= 3; k++) {
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, k, 4, p_229729_);
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, k, 4, p_229729_);
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 4, k, 6, p_229729_);
                  this.placeBlock(p_229725_, Blocks.COBBLESTONE.defaultBlockState(), 6, k, 6, p_229729_);
               }

               this.placeBlock(p_229725_, Blocks.WALL_TORCH.defaultBlockState(), 5, 3, 5, p_229729_);

               for (int l = 2; l <= 8; l++) {
                  this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 2, 3, l, p_229729_);
                  this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 3, 3, l, p_229729_);
                  if (l <= 3 || l >= 7) {
                     this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 4, 3, l, p_229729_);
                     this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 5, 3, l, p_229729_);
                     this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 6, 3, l, p_229729_);
                  }

                  this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 7, 3, l, p_229729_);
                  this.placeBlock(p_229725_, Blocks.OAK_PLANKS.defaultBlockState(), 8, 3, l, p_229729_);
               }

               BlockState blockstate = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
               this.placeBlock(p_229725_, blockstate, 9, 1, 3, p_229729_);
               this.placeBlock(p_229725_, blockstate, 9, 2, 3, p_229729_);
               this.placeBlock(p_229725_, blockstate, 9, 3, 3, p_229729_);
               this.createChest(p_229725_, p_229729_, p_229728_, 3, 4, 8, BuiltInLootTables.STRONGHOLD_CROSSING);
         }
      }
   }

   static class SmoothStoneSelector extends StructurePiece.BlockSelector {
      @Override
      public void next(RandomSource p_229749_, int p_229750_, int p_229751_, int p_229752_, boolean p_229753_) {
         if (p_229753_) {
            float f = p_229749_.nextFloat();
            if (f < 0.2F) {
               this.next = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            } else if (f < 0.5F) {
               this.next = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
            } else if (f < 0.55F) {
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

      public StairsDown(StructurePieceType p_229764_, int p_229765_, int p_229766_, int p_229767_, Direction p_229768_) {
         super(p_229764_, p_229765_, makeBoundingBox(p_229766_, 64, p_229767_, p_229768_, 5, 11, 5));
         this.isSource = true;
         this.setOrientation(p_229768_);
         this.entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;
      }

      public StairsDown(int p_229759_, RandomSource p_229760_, BoundingBox p_229761_, Direction p_229762_) {
         super(StructurePieceType.STRONGHOLD_STAIRS_DOWN, p_229759_, p_229761_);
         this.isSource = false;
         this.setOrientation(p_229762_);
         this.entryDoor = this.randomSmallDoor(p_229760_);
      }

      public StairsDown(StructurePieceType p_229770_, CompoundTag p_229771_) {
         super(p_229770_, p_229771_);
         this.isSource = p_229771_.getBooleanOr("Source", false);
      }

      public StairsDown(CompoundTag p_229773_) {
         this(StructurePieceType.STRONGHOLD_STAIRS_DOWN, p_229773_);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229795_, CompoundTag p_229796_) {
         super.addAdditionalSaveData(p_229795_, p_229796_);
         p_229796_.putBoolean("Source", this.isSource);
      }

      @Override
      public void addChildren(StructurePiece p_229783_, StructurePieceAccessor p_229784_, RandomSource p_229785_) {
         if (this.isSource) {
            StrongholdPieces.imposedPiece = StrongholdPieces.FiveCrossing.class;
         }

         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229783_, p_229784_, p_229785_, 1, 1);
      }

      public static StrongholdPieces.@Nullable StairsDown createPiece(
         StructurePieceAccessor p_229787_, RandomSource p_229788_, int p_229789_, int p_229790_, int p_229791_, Direction p_229792_, int p_229793_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229789_, p_229790_, p_229791_, -1, -7, 0, 5, 11, 5, p_229792_);
         return isOkBox(boundingbox) && p_229787_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.StairsDown(p_229793_, p_229788_, boundingbox, p_229792_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229775_,
         StructureManager p_229776_,
         ChunkGenerator p_229777_,
         RandomSource p_229778_,
         BoundingBox p_229779_,
         ChunkPos p_229780_,
         BlockPos p_229781_
      ) {
         this.generateBox(p_229775_, p_229779_, 0, 0, 0, 4, 10, 4, true, p_229778_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229775_, p_229778_, p_229779_, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(p_229775_, p_229778_, p_229779_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 4);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 6, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 6, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5, 2, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 4, 3, p_229779_);
         this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 5, 3, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 4, 3, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 3, p_229779_);
         this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 4, 3, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 3, 2, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 2, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 3, 3, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 2, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 2, 1, p_229779_);
         this.placeBlock(p_229775_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 1, 2, p_229779_);
         this.placeBlock(p_229775_, Blocks.SMOOTH_STONE_SLAB.defaultBlockState(), 1, 1, 3, p_229779_);
      }
   }

   public static class StartPiece extends StrongholdPieces.StairsDown {
      public StrongholdPieces.@Nullable PieceWeight previousPiece;
      public StrongholdPieces.@Nullable PortalRoom portalRoomPiece;
      public final List<StructurePiece> pendingChildren = Lists.newArrayList();

      public StartPiece(RandomSource p_229801_, int p_229802_, int p_229803_) {
         super(StructurePieceType.STRONGHOLD_START, 0, p_229802_, p_229803_, getRandomHorizontalDirection(p_229801_));
      }

      public StartPiece(CompoundTag p_229805_) {
         super(StructurePieceType.STRONGHOLD_START, p_229805_);
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

      public Straight(int p_229813_, RandomSource p_229814_, BoundingBox p_229815_, Direction p_229816_) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, p_229813_, p_229815_);
         this.setOrientation(p_229816_);
         this.entryDoor = this.randomSmallDoor(p_229814_);
         this.leftChild = p_229814_.nextInt(2) == 0;
         this.rightChild = p_229814_.nextInt(2) == 0;
      }

      public Straight(CompoundTag p_229818_) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT, p_229818_);
         this.leftChild = p_229818_.getBooleanOr("Left", false);
         this.rightChild = p_229818_.getBooleanOr("Right", false);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229840_, CompoundTag p_229841_) {
         super.addAdditionalSaveData(p_229840_, p_229841_);
         p_229841_.putBoolean("Left", this.leftChild);
         p_229841_.putBoolean("Right", this.rightChild);
      }

      @Override
      public void addChildren(StructurePiece p_229828_, StructurePieceAccessor p_229829_, RandomSource p_229830_) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229828_, p_229829_, p_229830_, 1, 1);
         if (this.leftChild) {
            this.generateSmallDoorChildLeft((StrongholdPieces.StartPiece)p_229828_, p_229829_, p_229830_, 1, 2);
         }

         if (this.rightChild) {
            this.generateSmallDoorChildRight((StrongholdPieces.StartPiece)p_229828_, p_229829_, p_229830_, 1, 2);
         }
      }

      public static StrongholdPieces.@Nullable Straight createPiece(
         StructurePieceAccessor p_229832_, RandomSource p_229833_, int p_229834_, int p_229835_, int p_229836_, Direction p_229837_, int p_229838_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229834_, p_229835_, p_229836_, -1, -1, 0, 5, 5, 7, p_229837_);
         return isOkBox(boundingbox) && p_229832_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.Straight(p_229838_, p_229833_, boundingbox, p_229837_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229820_,
         StructureManager p_229821_,
         ChunkGenerator p_229822_,
         RandomSource p_229823_,
         BoundingBox p_229824_,
         ChunkPos p_229825_,
         BlockPos p_229826_
      ) {
         this.generateBox(p_229820_, p_229824_, 0, 0, 0, 4, 4, 6, true, p_229823_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229820_, p_229823_, p_229824_, this.entryDoor, 1, 1, 0);
         this.generateSmallDoor(p_229820_, p_229823_, p_229824_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 6);
         BlockState blockstate = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.EAST);
         BlockState blockstate1 = Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.WEST);
         this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 1, 2, 1, blockstate);
         this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 3, 2, 1, blockstate1);
         this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 1, 2, 5, blockstate);
         this.maybeGenerateBlock(p_229820_, p_229824_, p_229823_, 0.1F, 3, 2, 5, blockstate1);
         if (this.leftChild) {
            this.generateBox(p_229820_, p_229824_, 0, 1, 2, 0, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }

         if (this.rightChild) {
            this.generateBox(p_229820_, p_229824_, 4, 1, 2, 4, 3, 4, CAVE_AIR, CAVE_AIR, false);
         }
      }
   }

   public static class StraightStairsDown extends StrongholdPieces.StrongholdPiece {
      private static final int WIDTH = 5;
      private static final int HEIGHT = 11;
      private static final int DEPTH = 8;

      public StraightStairsDown(int p_229846_, RandomSource p_229847_, BoundingBox p_229848_, Direction p_229849_) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, p_229846_, p_229848_);
         this.setOrientation(p_229849_);
         this.entryDoor = this.randomSmallDoor(p_229847_);
      }

      public StraightStairsDown(CompoundTag p_229851_) {
         super(StructurePieceType.STRONGHOLD_STRAIGHT_STAIRS_DOWN, p_229851_);
      }

      @Override
      public void addChildren(StructurePiece p_229861_, StructurePieceAccessor p_229862_, RandomSource p_229863_) {
         this.generateSmallDoorChildForward((StrongholdPieces.StartPiece)p_229861_, p_229862_, p_229863_, 1, 1);
      }

      public static StrongholdPieces.@Nullable StraightStairsDown createPiece(
         StructurePieceAccessor p_229865_, RandomSource p_229866_, int p_229867_, int p_229868_, int p_229869_, Direction p_229870_, int p_229871_
      ) {
         BoundingBox boundingbox = BoundingBox.orientBox(p_229867_, p_229868_, p_229869_, -1, -7, 0, 5, 11, 8, p_229870_);
         return isOkBox(boundingbox) && p_229865_.findCollisionPiece(boundingbox) == null
            ? new StrongholdPieces.StraightStairsDown(p_229871_, p_229866_, boundingbox, p_229870_)
            : null;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_229853_,
         StructureManager p_229854_,
         ChunkGenerator p_229855_,
         RandomSource p_229856_,
         BoundingBox p_229857_,
         ChunkPos p_229858_,
         BlockPos p_229859_
      ) {
         this.generateBox(p_229853_, p_229857_, 0, 0, 0, 4, 10, 7, true, p_229856_, StrongholdPieces.SMOOTH_STONE_SELECTOR);
         this.generateSmallDoor(p_229853_, p_229856_, p_229857_, this.entryDoor, 1, 7, 0);
         this.generateSmallDoor(p_229853_, p_229856_, p_229857_, StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING, 1, 1, 7);
         BlockState blockstate = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);

         for (int i = 0; i < 6; i++) {
            this.placeBlock(p_229853_, blockstate, 1, 6 - i, 1 + i, p_229857_);
            this.placeBlock(p_229853_, blockstate, 2, 6 - i, 1 + i, p_229857_);
            this.placeBlock(p_229853_, blockstate, 3, 6 - i, 1 + i, p_229857_);
            if (i < 5) {
               this.placeBlock(p_229853_, Blocks.STONE_BRICKS.defaultBlockState(), 1, 5 - i, 1 + i, p_229857_);
               this.placeBlock(p_229853_, Blocks.STONE_BRICKS.defaultBlockState(), 2, 5 - i, 1 + i, p_229857_);
               this.placeBlock(p_229853_, Blocks.STONE_BRICKS.defaultBlockState(), 3, 5 - i, 1 + i, p_229857_);
            }
         }
      }
   }

   abstract static class StrongholdPiece extends StructurePiece {
      protected StrongholdPieces.StrongholdPiece.SmallDoorType entryDoor = StrongholdPieces.StrongholdPiece.SmallDoorType.OPENING;

      protected StrongholdPiece(StructurePieceType p_229874_, int p_229875_, BoundingBox p_229876_) {
         super(p_229874_, p_229875_, p_229876_);
      }

      public StrongholdPiece(StructurePieceType p_229878_, CompoundTag p_229879_) {
         super(p_229878_, p_229879_);
         this.entryDoor = p_229879_.<StrongholdPieces.StrongholdPiece.SmallDoorType>read(
               "EntryDoor", StrongholdPieces.StrongholdPiece.SmallDoorType.LEGACY_CODEC
            )
            .orElseThrow();
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_229891_, CompoundTag p_229892_) {
         p_229892_.store("EntryDoor", StrongholdPieces.StrongholdPiece.SmallDoorType.LEGACY_CODEC, this.entryDoor);
      }

      protected void generateSmallDoor(
         WorldGenLevel p_229881_,
         RandomSource p_229882_,
         BoundingBox p_229883_,
         StrongholdPieces.StrongholdPiece.SmallDoorType p_229884_,
         int p_229885_,
         int p_229886_,
         int p_229887_
      ) {
         switch (p_229884_) {
            case OPENING:
               this.generateBox(
                  p_229881_, p_229883_, p_229885_, p_229886_, p_229887_, p_229885_ + 3 - 1, p_229886_ + 3 - 1, p_229887_, CAVE_AIR, CAVE_AIR, false
               );
               break;
            case WOOD_DOOR:
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_, p_229886_, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_, p_229886_ + 1, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_, p_229886_ + 2, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 1, p_229886_ + 2, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 2, p_229886_ + 2, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 2, p_229886_ + 1, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 2, p_229886_, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.OAK_DOOR.defaultBlockState(), p_229885_ + 1, p_229886_, p_229887_, p_229883_);
               this.placeBlock(
                  p_229881_,
                  Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                  p_229885_ + 1,
                  p_229886_ + 1,
                  p_229887_,
                  p_229883_
               );
               break;
            case GRATES:
               this.placeBlock(p_229881_, Blocks.CAVE_AIR.defaultBlockState(), p_229885_ + 1, p_229886_, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.CAVE_AIR.defaultBlockState(), p_229885_ + 1, p_229886_ + 1, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), p_229885_, p_229886_, p_229887_, p_229883_);
               this.placeBlock(
                  p_229881_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.WEST, true), p_229885_, p_229886_ + 1, p_229887_, p_229883_
               );
               this.placeBlock(
                  p_229881_,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  p_229885_,
                  p_229886_ + 2,
                  p_229887_,
                  p_229883_
               );
               this.placeBlock(
                  p_229881_,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  p_229885_ + 1,
                  p_229886_ + 2,
                  p_229887_,
                  p_229883_
               );
               this.placeBlock(
                  p_229881_,
                  Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true),
                  p_229885_ + 2,
                  p_229886_ + 2,
                  p_229887_,
                  p_229883_
               );
               this.placeBlock(
                  p_229881_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), p_229885_ + 2, p_229886_ + 1, p_229887_, p_229883_
               );
               this.placeBlock(
                  p_229881_, Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.EAST, true), p_229885_ + 2, p_229886_, p_229887_, p_229883_
               );
               break;
            case IRON_DOOR:
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_, p_229886_, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_, p_229886_ + 1, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_, p_229886_ + 2, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 1, p_229886_ + 2, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 2, p_229886_ + 2, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 2, p_229886_ + 1, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.STONE_BRICKS.defaultBlockState(), p_229885_ + 2, p_229886_, p_229887_, p_229883_);
               this.placeBlock(p_229881_, Blocks.IRON_DOOR.defaultBlockState(), p_229885_ + 1, p_229886_, p_229887_, p_229883_);
               this.placeBlock(
                  p_229881_,
                  Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER),
                  p_229885_ + 1,
                  p_229886_ + 1,
                  p_229887_,
                  p_229883_
               );
               this.placeBlock(
                  p_229881_,
                  Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.NORTH),
                  p_229885_ + 2,
                  p_229886_ + 1,
                  p_229887_ + 1,
                  p_229883_
               );
               this.placeBlock(
                  p_229881_,
                  Blocks.STONE_BUTTON.defaultBlockState().setValue(ButtonBlock.FACING, Direction.SOUTH),
                  p_229885_ + 2,
                  p_229886_ + 1,
                  p_229887_ - 1,
                  p_229883_
               );
         }
      }

      protected StrongholdPieces.StrongholdPiece.SmallDoorType randomSmallDoor(RandomSource p_229900_) {
         int i = p_229900_.nextInt(5);
         switch (i) {
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
         StrongholdPieces.StartPiece p_229894_, StructurePieceAccessor p_229895_, RandomSource p_229896_, int p_229897_, int p_229898_
      ) {
         Direction direction = this.getOrientation();
         if (direction != null) {
            switch (direction) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229894_,
                     p_229895_,
                     p_229896_,
                     this.boundingBox.minX() + p_229897_,
                     this.boundingBox.minY() + p_229898_,
                     this.boundingBox.minZ() - 1,
                     direction,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229894_,
                     p_229895_,
                     p_229896_,
                     this.boundingBox.minX() + p_229897_,
                     this.boundingBox.minY() + p_229898_,
                     this.boundingBox.maxZ() + 1,
                     direction,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229894_,
                     p_229895_,
                     p_229896_,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + p_229898_,
                     this.boundingBox.minZ() + p_229897_,
                     direction,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229894_,
                     p_229895_,
                     p_229896_,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + p_229898_,
                     this.boundingBox.minZ() + p_229897_,
                     direction,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateSmallDoorChildLeft(
         StrongholdPieces.StartPiece p_229902_, StructurePieceAccessor p_229903_, RandomSource p_229904_, int p_229905_, int p_229906_
      ) {
         Direction direction = this.getOrientation();
         if (direction != null) {
            switch (direction) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229902_,
                     p_229903_,
                     p_229904_,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + p_229905_,
                     this.boundingBox.minZ() + p_229906_,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229902_,
                     p_229903_,
                     p_229904_,
                     this.boundingBox.minX() - 1,
                     this.boundingBox.minY() + p_229905_,
                     this.boundingBox.minZ() + p_229906_,
                     Direction.WEST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229902_,
                     p_229903_,
                     p_229904_,
                     this.boundingBox.minX() + p_229906_,
                     this.boundingBox.minY() + p_229905_,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229902_,
                     p_229903_,
                     p_229904_,
                     this.boundingBox.minX() + p_229906_,
                     this.boundingBox.minY() + p_229905_,
                     this.boundingBox.minZ() - 1,
                     Direction.NORTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected @Nullable StructurePiece generateSmallDoorChildRight(
         StrongholdPieces.StartPiece p_229908_, StructurePieceAccessor p_229909_, RandomSource p_229910_, int p_229911_, int p_229912_
      ) {
         Direction direction = this.getOrientation();
         if (direction != null) {
            switch (direction) {
               case NORTH:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229908_,
                     p_229909_,
                     p_229910_,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + p_229911_,
                     this.boundingBox.minZ() + p_229912_,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case SOUTH:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229908_,
                     p_229909_,
                     p_229910_,
                     this.boundingBox.maxX() + 1,
                     this.boundingBox.minY() + p_229911_,
                     this.boundingBox.minZ() + p_229912_,
                     Direction.EAST,
                     this.getGenDepth()
                  );
               case WEST:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229908_,
                     p_229909_,
                     p_229910_,
                     this.boundingBox.minX() + p_229912_,
                     this.boundingBox.minY() + p_229911_,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
               case EAST:
                  return StrongholdPieces.generateAndAddPiece(
                     p_229908_,
                     p_229909_,
                     p_229910_,
                     this.boundingBox.minX() + p_229912_,
                     this.boundingBox.minY() + p_229911_,
                     this.boundingBox.maxZ() + 1,
                     Direction.SOUTH,
                     this.getGenDepth()
                  );
            }
         }

         return null;
      }

      protected static boolean isOkBox(BoundingBox p_229889_) {
         return p_229889_.minY() > 10;
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

      protected Turn(StructurePieceType p_229930_, int p_229931_, BoundingBox p_229932_) {
         super(p_229930_, p_229931_, p_229932_);
      }

      public Turn(StructurePieceType p_229934_, CompoundTag p_229935_) {
         super(p_229934_, p_229935_);
      }
   }
}
