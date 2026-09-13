package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ScatteredFeaturePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class JungleTemplePiece extends ScatteredFeaturePiece {
   public static final int WIDTH = 12;
   public static final int DEPTH = 15;
   private boolean placedMainChest;
   private boolean placedHiddenChest;
   private boolean placedTrap1;
   private boolean placedTrap2;
   private static final JungleTemplePiece.MossStoneSelector STONE_SELECTOR = new JungleTemplePiece.MossStoneSelector();

   public JungleTemplePiece(RandomSource p_227668_, int p_227669_, int p_227670_) {
      super(StructurePieceType.JUNGLE_PYRAMID_PIECE, p_227669_, 64, p_227670_, 12, 10, 15, getRandomHorizontalDirection(p_227668_));
   }

   public JungleTemplePiece(CompoundTag p_227672_) {
      super(StructurePieceType.JUNGLE_PYRAMID_PIECE, p_227672_);
      this.placedMainChest = p_227672_.getBooleanOr("placedMainChest", false);
      this.placedHiddenChest = p_227672_.getBooleanOr("placedHiddenChest", false);
      this.placedTrap1 = p_227672_.getBooleanOr("placedTrap1", false);
      this.placedTrap2 = p_227672_.getBooleanOr("placedTrap2", false);
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext p_227682_, CompoundTag p_227683_) {
      super.addAdditionalSaveData(p_227682_, p_227683_);
      p_227683_.putBoolean("placedMainChest", this.placedMainChest);
      p_227683_.putBoolean("placedHiddenChest", this.placedHiddenChest);
      p_227683_.putBoolean("placedTrap1", this.placedTrap1);
      p_227683_.putBoolean("placedTrap2", this.placedTrap2);
   }

   @Override
   public void postProcess(
      WorldGenLevel p_227674_,
      StructureManager p_227675_,
      ChunkGenerator p_227676_,
      RandomSource p_227677_,
      BoundingBox p_227678_,
      ChunkPos p_227679_,
      BlockPos p_227680_
   ) {
      if (this.updateAverageGroundHeight(p_227674_, p_227678_, 0)) {
         this.generateBox(p_227674_, p_227678_, 0, -4, 0, this.width - 1, 0, this.depth - 1, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 2, 1, 2, 9, 2, 2, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 2, 1, 12, 9, 2, 12, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 2, 1, 3, 2, 2, 11, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 9, 1, 3, 9, 2, 11, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 1, 3, 1, 10, 6, 1, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 1, 3, 13, 10, 6, 13, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 1, 3, 2, 1, 6, 12, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 10, 3, 2, 10, 6, 12, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 2, 3, 2, 9, 3, 12, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 2, 6, 2, 9, 6, 12, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 3, 7, 3, 8, 7, 11, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 4, 8, 4, 7, 8, 10, false, p_227677_, STONE_SELECTOR);
         this.generateAirBox(p_227674_, p_227678_, 3, 1, 3, 8, 2, 11);
         this.generateAirBox(p_227674_, p_227678_, 4, 3, 6, 7, 3, 9);
         this.generateAirBox(p_227674_, p_227678_, 2, 4, 2, 9, 5, 12);
         this.generateAirBox(p_227674_, p_227678_, 4, 6, 5, 7, 6, 9);
         this.generateAirBox(p_227674_, p_227678_, 5, 7, 6, 6, 7, 8);
         this.generateAirBox(p_227674_, p_227678_, 5, 1, 2, 6, 2, 2);
         this.generateAirBox(p_227674_, p_227678_, 5, 2, 12, 6, 2, 12);
         this.generateAirBox(p_227674_, p_227678_, 5, 5, 1, 6, 5, 1);
         this.generateAirBox(p_227674_, p_227678_, 5, 5, 13, 6, 5, 13);
         this.placeBlock(p_227674_, Blocks.AIR.defaultBlockState(), 1, 5, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.AIR.defaultBlockState(), 10, 5, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.AIR.defaultBlockState(), 1, 5, 9, p_227678_);
         this.placeBlock(p_227674_, Blocks.AIR.defaultBlockState(), 10, 5, 9, p_227678_);

         for (int i = 0; i <= 14; i += 14) {
            this.generateBox(p_227674_, p_227678_, 2, 4, i, 2, 5, i, false, p_227677_, STONE_SELECTOR);
            this.generateBox(p_227674_, p_227678_, 4, 4, i, 4, 5, i, false, p_227677_, STONE_SELECTOR);
            this.generateBox(p_227674_, p_227678_, 7, 4, i, 7, 5, i, false, p_227677_, STONE_SELECTOR);
            this.generateBox(p_227674_, p_227678_, 9, 4, i, 9, 5, i, false, p_227677_, STONE_SELECTOR);
         }

         this.generateBox(p_227674_, p_227678_, 5, 6, 0, 6, 6, 0, false, p_227677_, STONE_SELECTOR);

         for (int l = 0; l <= 11; l += 11) {
            for (int j = 2; j <= 12; j += 2) {
               this.generateBox(p_227674_, p_227678_, l, 4, j, l, 5, j, false, p_227677_, STONE_SELECTOR);
            }

            this.generateBox(p_227674_, p_227678_, l, 6, 5, l, 6, 5, false, p_227677_, STONE_SELECTOR);
            this.generateBox(p_227674_, p_227678_, l, 6, 9, l, 6, 9, false, p_227677_, STONE_SELECTOR);
         }

         this.generateBox(p_227674_, p_227678_, 2, 7, 2, 2, 9, 2, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 9, 7, 2, 9, 9, 2, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 2, 7, 12, 2, 9, 12, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 9, 7, 12, 9, 9, 12, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 4, 9, 4, 4, 9, 4, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 7, 9, 4, 7, 9, 4, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 4, 9, 10, 4, 9, 10, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 7, 9, 10, 7, 9, 10, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 5, 9, 7, 6, 9, 7, false, p_227677_, STONE_SELECTOR);
         BlockState blockstate3 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState blockstate4 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         BlockState blockstate = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState blockstate1 = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         this.placeBlock(p_227674_, blockstate1, 5, 9, 6, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 6, 9, 6, p_227678_);
         this.placeBlock(p_227674_, blockstate, 5, 9, 8, p_227678_);
         this.placeBlock(p_227674_, blockstate, 6, 9, 8, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 4, 0, 0, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 5, 0, 0, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 6, 0, 0, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 7, 0, 0, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 4, 1, 8, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 4, 2, 9, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 4, 3, 10, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 7, 1, 8, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 7, 2, 9, p_227678_);
         this.placeBlock(p_227674_, blockstate1, 7, 3, 10, p_227678_);
         this.generateBox(p_227674_, p_227678_, 4, 1, 9, 4, 1, 9, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 7, 1, 9, 7, 1, 9, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 4, 1, 10, 7, 2, 10, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 5, 4, 5, 6, 4, 5, false, p_227677_, STONE_SELECTOR);
         this.placeBlock(p_227674_, blockstate3, 4, 4, 5, p_227678_);
         this.placeBlock(p_227674_, blockstate4, 7, 4, 5, p_227678_);

         for (int k = 0; k < 4; k++) {
            this.placeBlock(p_227674_, blockstate, 5, 0 - k, 6 + k, p_227678_);
            this.placeBlock(p_227674_, blockstate, 6, 0 - k, 6 + k, p_227678_);
            this.generateAirBox(p_227674_, p_227678_, 5, 0 - k, 7 + k, 6, 0 - k, 9 + k);
         }

         this.generateAirBox(p_227674_, p_227678_, 1, -3, 12, 10, -1, 13);
         this.generateAirBox(p_227674_, p_227678_, 1, -3, 1, 3, -1, 13);
         this.generateAirBox(p_227674_, p_227678_, 1, -3, 1, 9, -1, 5);

         for (int i1 = 1; i1 <= 13; i1 += 2) {
            this.generateBox(p_227674_, p_227678_, 1, -3, i1, 1, -2, i1, false, p_227677_, STONE_SELECTOR);
         }

         for (int j1 = 2; j1 <= 12; j1 += 2) {
            this.generateBox(p_227674_, p_227678_, 1, -1, j1, 3, -1, j1, false, p_227677_, STONE_SELECTOR);
         }

         this.generateBox(p_227674_, p_227678_, 2, -2, 1, 5, -2, 1, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 7, -2, 1, 9, -2, 1, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 6, -3, 1, 6, -3, 1, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 6, -1, 1, 6, -1, 1, false, p_227677_, STONE_SELECTOR);
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.EAST).setValue(TripWireHookBlock.ATTACHED, true),
            1,
            -3,
            8,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.WEST).setValue(TripWireHookBlock.ATTACHED, true),
            4,
            -3,
            8,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, true).setValue(TripWireBlock.WEST, true).setValue(TripWireBlock.ATTACHED, true),
            2,
            -3,
            8,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, true).setValue(TripWireBlock.WEST, true).setValue(TripWireBlock.ATTACHED, true),
            3,
            -3,
            8,
            p_227678_
         );
         BlockState blockstate5 = Blocks.REDSTONE_WIRE
            .defaultBlockState()
            .setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)
            .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE);
         this.placeBlock(p_227674_, blockstate5, 5, -3, 7, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 5, -3, 6, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 5, -3, 5, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 5, -3, 4, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 5, -3, 3, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 5, -3, 2, p_227678_);
         this.placeBlock(
            p_227674_,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            5,
            -3,
            1,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            4,
            -3,
            1,
            p_227678_
         );
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3, -3, 1, p_227678_);
         if (!this.placedTrap1) {
            this.placedTrap1 = this.createDispenser(p_227674_, p_227678_, p_227677_, 3, -2, 1, Direction.NORTH, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
         }

         this.placeBlock(p_227674_, Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true), 3, -2, 2, p_227678_);
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.NORTH).setValue(TripWireHookBlock.ATTACHED, true),
            7,
            -3,
            1,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.SOUTH).setValue(TripWireHookBlock.ATTACHED, true),
            7,
            -3,
            5,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            2,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            3,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            4,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            8,
            -3,
            6,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE),
            9,
            -3,
            6,
            p_227678_
         );
         this.placeBlock(
            p_227674_,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.UP),
            9,
            -3,
            5,
            p_227678_
         );
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 4, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 9, -2, 4, p_227678_);
         if (!this.placedTrap2) {
            this.placedTrap2 = this.createDispenser(p_227674_, p_227678_, p_227677_, 9, -2, 3, Direction.WEST, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
         }

         this.placeBlock(p_227674_, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true), 8, -1, 3, p_227678_);
         this.placeBlock(p_227674_, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true), 8, -2, 3, p_227678_);
         if (!this.placedMainChest) {
            this.placedMainChest = this.createChest(p_227674_, p_227678_, p_227677_, 8, -3, 3, BuiltInLootTables.JUNGLE_TEMPLE);
         }

         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 2, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 1, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 4, -3, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -2, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -1, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 6, -3, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -2, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -1, 5, p_227678_);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 5, p_227678_);
         this.generateBox(p_227674_, p_227678_, 9, -1, 1, 9, -1, 5, false, p_227677_, STONE_SELECTOR);
         this.generateAirBox(p_227674_, p_227678_, 8, -3, 8, 10, -1, 10);
         this.placeBlock(p_227674_, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 8, -2, 11, p_227678_);
         this.placeBlock(p_227674_, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 9, -2, 11, p_227678_);
         this.placeBlock(p_227674_, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 10, -2, 11, p_227678_);
         BlockState blockstate2 = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACING, Direction.NORTH).setValue(LeverBlock.FACE, AttachFace.WALL);
         this.placeBlock(p_227674_, blockstate2, 8, -2, 12, p_227678_);
         this.placeBlock(p_227674_, blockstate2, 9, -2, 12, p_227678_);
         this.placeBlock(p_227674_, blockstate2, 10, -2, 12, p_227678_);
         this.generateBox(p_227674_, p_227678_, 8, -3, 8, 8, -3, 10, false, p_227677_, STONE_SELECTOR);
         this.generateBox(p_227674_, p_227678_, 10, -3, 8, 10, -3, 10, false, p_227677_, STONE_SELECTOR);
         this.placeBlock(p_227674_, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 10, -2, 9, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 8, -2, 9, p_227678_);
         this.placeBlock(p_227674_, blockstate5, 8, -2, 10, p_227678_);
         this.placeBlock(
            p_227674_,
            Blocks.REDSTONE_WIRE
               .defaultBlockState()
               .setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            10,
            -1,
            9,
            p_227678_
         );
         this.placeBlock(p_227674_, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 9, -2, 8, p_227678_);
         this.placeBlock(p_227674_, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -2, 8, p_227678_);
         this.placeBlock(p_227674_, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -1, 8, p_227678_);
         this.placeBlock(p_227674_, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH), 10, -2, 10, p_227678_);
         if (!this.placedHiddenChest) {
            this.placedHiddenChest = this.createChest(p_227674_, p_227678_, p_227677_, 9, -3, 10, BuiltInLootTables.JUNGLE_TEMPLE);
         }
      }
   }

   static class MossStoneSelector extends StructurePiece.BlockSelector {
      @Override
      public void next(RandomSource p_227686_, int p_227687_, int p_227688_, int p_227689_, boolean p_227690_) {
         if (p_227686_.nextFloat() < 0.4F) {
            this.next = Blocks.COBBLESTONE.defaultBlockState();
         } else {
            this.next = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
         }
      }
   }
}
