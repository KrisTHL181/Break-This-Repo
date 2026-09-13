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

   public JungleTemplePiece(final RandomSource random, final int west, final int north) {
      super(StructurePieceType.JUNGLE_PYRAMID_PIECE, west, 64, north, 12, 10, 15, getRandomHorizontalDirection(random));
   }

   public JungleTemplePiece(final CompoundTag tag) {
      super(StructurePieceType.JUNGLE_PYRAMID_PIECE, tag);
      this.placedMainChest = tag.getBooleanOr("placedMainChest", false);
      this.placedHiddenChest = tag.getBooleanOr("placedHiddenChest", false);
      this.placedTrap1 = tag.getBooleanOr("placedTrap1", false);
      this.placedTrap2 = tag.getBooleanOr("placedTrap2", false);
   }

   @Override
   protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
      super.addAdditionalSaveData(context, tag);
      tag.putBoolean("placedMainChest", this.placedMainChest);
      tag.putBoolean("placedHiddenChest", this.placedHiddenChest);
      tag.putBoolean("placedTrap1", this.placedTrap1);
      tag.putBoolean("placedTrap2", this.placedTrap2);
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
         this.generateBox(level, chunkBB, 0, -4, 0, this.width - 1, 0, this.depth - 1, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 1, 2, 9, 2, 2, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 1, 12, 9, 2, 12, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 1, 3, 2, 2, 11, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 9, 1, 3, 9, 2, 11, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 3, 1, 10, 6, 1, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 3, 13, 10, 6, 13, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 1, 3, 2, 1, 6, 12, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 10, 3, 2, 10, 6, 12, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 3, 2, 9, 3, 12, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 6, 2, 9, 6, 12, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 3, 7, 3, 8, 7, 11, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 8, 4, 7, 8, 10, false, random, STONE_SELECTOR);
         this.generateAirBox(level, chunkBB, 3, 1, 3, 8, 2, 11);
         this.generateAirBox(level, chunkBB, 4, 3, 6, 7, 3, 9);
         this.generateAirBox(level, chunkBB, 2, 4, 2, 9, 5, 12);
         this.generateAirBox(level, chunkBB, 4, 6, 5, 7, 6, 9);
         this.generateAirBox(level, chunkBB, 5, 7, 6, 6, 7, 8);
         this.generateAirBox(level, chunkBB, 5, 1, 2, 6, 2, 2);
         this.generateAirBox(level, chunkBB, 5, 2, 12, 6, 2, 12);
         this.generateAirBox(level, chunkBB, 5, 5, 1, 6, 5, 1);
         this.generateAirBox(level, chunkBB, 5, 5, 13, 6, 5, 13);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 1, 5, 5, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 10, 5, 5, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 1, 5, 9, chunkBB);
         this.placeBlock(level, Blocks.AIR.defaultBlockState(), 10, 5, 9, chunkBB);

         for (int z = 0; z <= 14; z += 14) {
            this.generateBox(level, chunkBB, 2, 4, z, 2, 5, z, false, random, STONE_SELECTOR);
            this.generateBox(level, chunkBB, 4, 4, z, 4, 5, z, false, random, STONE_SELECTOR);
            this.generateBox(level, chunkBB, 7, 4, z, 7, 5, z, false, random, STONE_SELECTOR);
            this.generateBox(level, chunkBB, 9, 4, z, 9, 5, z, false, random, STONE_SELECTOR);
         }

         this.generateBox(level, chunkBB, 5, 6, 0, 6, 6, 0, false, random, STONE_SELECTOR);

         for (int x = 0; x <= 11; x += 11) {
            for (int z = 2; z <= 12; z += 2) {
               this.generateBox(level, chunkBB, x, 4, z, x, 5, z, false, random, STONE_SELECTOR);
            }

            this.generateBox(level, chunkBB, x, 6, 5, x, 6, 5, false, random, STONE_SELECTOR);
            this.generateBox(level, chunkBB, x, 6, 9, x, 6, 9, false, random, STONE_SELECTOR);
         }

         this.generateBox(level, chunkBB, 2, 7, 2, 2, 9, 2, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 9, 7, 2, 9, 9, 2, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 2, 7, 12, 2, 9, 12, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 9, 7, 12, 9, 9, 12, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 9, 4, 4, 9, 4, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 7, 9, 4, 7, 9, 4, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 9, 10, 4, 9, 10, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 7, 9, 10, 7, 9, 10, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 5, 9, 7, 6, 9, 7, false, random, STONE_SELECTOR);
         BlockState eastStairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
         BlockState westStairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
         BlockState southStairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
         BlockState northStairs = Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
         this.placeBlock(level, northStairs, 5, 9, 6, chunkBB);
         this.placeBlock(level, northStairs, 6, 9, 6, chunkBB);
         this.placeBlock(level, southStairs, 5, 9, 8, chunkBB);
         this.placeBlock(level, southStairs, 6, 9, 8, chunkBB);
         this.placeBlock(level, northStairs, 4, 0, 0, chunkBB);
         this.placeBlock(level, northStairs, 5, 0, 0, chunkBB);
         this.placeBlock(level, northStairs, 6, 0, 0, chunkBB);
         this.placeBlock(level, northStairs, 7, 0, 0, chunkBB);
         this.placeBlock(level, northStairs, 4, 1, 8, chunkBB);
         this.placeBlock(level, northStairs, 4, 2, 9, chunkBB);
         this.placeBlock(level, northStairs, 4, 3, 10, chunkBB);
         this.placeBlock(level, northStairs, 7, 1, 8, chunkBB);
         this.placeBlock(level, northStairs, 7, 2, 9, chunkBB);
         this.placeBlock(level, northStairs, 7, 3, 10, chunkBB);
         this.generateBox(level, chunkBB, 4, 1, 9, 4, 1, 9, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 7, 1, 9, 7, 1, 9, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 4, 1, 10, 7, 2, 10, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 5, 4, 5, 6, 4, 5, false, random, STONE_SELECTOR);
         this.placeBlock(level, eastStairs, 4, 4, 5, chunkBB);
         this.placeBlock(level, westStairs, 7, 4, 5, chunkBB);

         for (int i = 0; i < 4; i++) {
            this.placeBlock(level, southStairs, 5, 0 - i, 6 + i, chunkBB);
            this.placeBlock(level, southStairs, 6, 0 - i, 6 + i, chunkBB);
            this.generateAirBox(level, chunkBB, 5, 0 - i, 7 + i, 6, 0 - i, 9 + i);
         }

         this.generateAirBox(level, chunkBB, 1, -3, 12, 10, -1, 13);
         this.generateAirBox(level, chunkBB, 1, -3, 1, 3, -1, 13);
         this.generateAirBox(level, chunkBB, 1, -3, 1, 9, -1, 5);

         for (int z = 1; z <= 13; z += 2) {
            this.generateBox(level, chunkBB, 1, -3, z, 1, -2, z, false, random, STONE_SELECTOR);
         }

         for (int z = 2; z <= 12; z += 2) {
            this.generateBox(level, chunkBB, 1, -1, z, 3, -1, z, false, random, STONE_SELECTOR);
         }

         this.generateBox(level, chunkBB, 2, -2, 1, 5, -2, 1, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 7, -2, 1, 9, -2, 1, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 6, -3, 1, 6, -3, 1, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 6, -1, 1, 6, -1, 1, false, random, STONE_SELECTOR);
         this.placeBlock(
            level,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.EAST).setValue(TripWireHookBlock.ATTACHED, true),
            1,
            -3,
            8,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.WEST).setValue(TripWireHookBlock.ATTACHED, true),
            4,
            -3,
            8,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, true).setValue(TripWireBlock.WEST, true).setValue(TripWireBlock.ATTACHED, true),
            2,
            -3,
            8,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.EAST, true).setValue(TripWireBlock.WEST, true).setValue(TripWireBlock.ATTACHED, true),
            3,
            -3,
            8,
            chunkBB
         );
         BlockState redstoneWireNS = Blocks.REDSTONE_WIRE
            .defaultBlockState()
            .setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)
            .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE);
         this.placeBlock(level, redstoneWireNS, 5, -3, 7, chunkBB);
         this.placeBlock(level, redstoneWireNS, 5, -3, 6, chunkBB);
         this.placeBlock(level, redstoneWireNS, 5, -3, 5, chunkBB);
         this.placeBlock(level, redstoneWireNS, 5, -3, 4, chunkBB);
         this.placeBlock(level, redstoneWireNS, 5, -3, 3, chunkBB);
         this.placeBlock(level, redstoneWireNS, 5, -3, 2, chunkBB);
         this.placeBlock(
            level,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            5,
            -3,
            1,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            4,
            -3,
            1,
            chunkBB
         );
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3, -3, 1, chunkBB);
         if (!this.placedTrap1) {
            this.placedTrap1 = this.createDispenser(level, chunkBB, random, 3, -2, 1, Direction.NORTH, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
         }

         this.placeBlock(level, Blocks.VINE.defaultBlockState().setValue(VineBlock.SOUTH, true), 3, -2, 2, chunkBB);
         this.placeBlock(
            level,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.NORTH).setValue(TripWireHookBlock.ATTACHED, true),
            7,
            -3,
            1,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(TripWireHookBlock.FACING, Direction.SOUTH).setValue(TripWireHookBlock.ATTACHED, true),
            7,
            -3,
            5,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            2,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            3,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.TRIPWIRE.defaultBlockState().setValue(TripWireBlock.NORTH, true).setValue(TripWireBlock.SOUTH, true).setValue(TripWireBlock.ATTACHED, true),
            7,
            -3,
            4,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            8,
            -3,
            6,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE),
            9,
            -3,
            6,
            chunkBB
         );
         this.placeBlock(
            level,
            Blocks.REDSTONE_WIRE.defaultBlockState().setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE).setValue(RedStoneWireBlock.SOUTH, RedstoneSide.UP),
            9,
            -3,
            5,
            chunkBB
         );
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 4, chunkBB);
         this.placeBlock(level, redstoneWireNS, 9, -2, 4, chunkBB);
         if (!this.placedTrap2) {
            this.placedTrap2 = this.createDispenser(level, chunkBB, random, 9, -2, 3, Direction.WEST, BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER);
         }

         this.placeBlock(level, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true), 8, -1, 3, chunkBB);
         this.placeBlock(level, Blocks.VINE.defaultBlockState().setValue(VineBlock.EAST, true), 8, -2, 3, chunkBB);
         if (!this.placedMainChest) {
            this.placedMainChest = this.createChest(level, chunkBB, random, 8, -3, 3, BuiltInLootTables.JUNGLE_TEMPLE);
         }

         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 9, -3, 2, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 1, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 4, -3, 5, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -2, 5, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 5, -1, 5, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 6, -3, 5, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -2, 5, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 7, -1, 5, chunkBB);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8, -3, 5, chunkBB);
         this.generateBox(level, chunkBB, 9, -1, 1, 9, -1, 5, false, random, STONE_SELECTOR);
         this.generateAirBox(level, chunkBB, 8, -3, 8, 10, -1, 10);
         this.placeBlock(level, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 8, -2, 11, chunkBB);
         this.placeBlock(level, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 9, -2, 11, chunkBB);
         this.placeBlock(level, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 10, -2, 11, chunkBB);
         BlockState lever = Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACING, Direction.NORTH).setValue(LeverBlock.FACE, AttachFace.WALL);
         this.placeBlock(level, lever, 8, -2, 12, chunkBB);
         this.placeBlock(level, lever, 9, -2, 12, chunkBB);
         this.placeBlock(level, lever, 10, -2, 12, chunkBB);
         this.generateBox(level, chunkBB, 8, -3, 8, 8, -3, 10, false, random, STONE_SELECTOR);
         this.generateBox(level, chunkBB, 10, -3, 8, 10, -3, 10, false, random, STONE_SELECTOR);
         this.placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 10, -2, 9, chunkBB);
         this.placeBlock(level, redstoneWireNS, 8, -2, 9, chunkBB);
         this.placeBlock(level, redstoneWireNS, 8, -2, 10, chunkBB);
         this.placeBlock(
            level,
            Blocks.REDSTONE_WIRE
               .defaultBlockState()
               .setValue(RedStoneWireBlock.NORTH, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.SOUTH, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.EAST, RedstoneSide.SIDE)
               .setValue(RedStoneWireBlock.WEST, RedstoneSide.SIDE),
            10,
            -1,
            9,
            chunkBB
         );
         this.placeBlock(level, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 9, -2, 8, chunkBB);
         this.placeBlock(level, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -2, 8, chunkBB);
         this.placeBlock(level, Blocks.STICKY_PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.WEST), 10, -1, 8, chunkBB);
         this.placeBlock(level, Blocks.REPEATER.defaultBlockState().setValue(RepeaterBlock.FACING, Direction.NORTH), 10, -2, 10, chunkBB);
         if (!this.placedHiddenChest) {
            this.placedHiddenChest = this.createChest(level, chunkBB, random, 9, -3, 10, BuiltInLootTables.JUNGLE_TEMPLE);
         }
      }
   }

   private static class MossStoneSelector extends StructurePiece.BlockSelector {
      @Override
      public void next(final RandomSource random, final int worldX, final int worldY, final int worldZ, final boolean isEdge) {
         if (random.nextFloat() < 0.4F) {
            this.next = Blocks.COBBLESTONE.defaultBlockState();
         } else {
            this.next = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
         }
      }
   }
}
