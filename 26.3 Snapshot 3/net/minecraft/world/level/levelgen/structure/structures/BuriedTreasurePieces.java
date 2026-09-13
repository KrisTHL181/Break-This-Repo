package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BuriedTreasurePieces {
   public static class BuriedTreasurePiece extends StructurePiece {
      public BuriedTreasurePiece(final BlockPos offset) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, 0, new BoundingBox(offset));
      }

      public BuriedTreasurePiece(final CompoundTag tag) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, tag);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
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
         int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.boundingBox.minX(), this.boundingBox.minZ());
         BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(this.boundingBox.minX(), y, this.boundingBox.minZ());

         while (pos.getY() > level.getMinY()) {
            BlockState currentState = level.getBlockState(pos);
            BlockState belowState = level.getBlockState(pos.below());
            if (belowState.is(Blocks.SANDSTONE)
               || belowState.is(Blocks.STONE)
               || belowState.is(Blocks.ANDESITE)
               || belowState.is(Blocks.GRANITE)
               || belowState.is(Blocks.DIORITE)) {
               BlockState softState = !currentState.isAir() && !isLiquid(currentState) ? currentState : Blocks.SAND.defaultBlockState();

               for (Direction direction : Direction.values()) {
                  BlockPos relativePos = pos.relative(direction);
                  BlockState relativeState = level.getBlockState(relativePos);
                  if (relativeState.isAir() || isLiquid(relativeState)) {
                     BlockPos belowRelativePos = relativePos.below();
                     BlockState belowRelativeState = level.getBlockState(belowRelativePos);
                     if ((belowRelativeState.isAir() || isLiquid(belowRelativeState)) && direction != Direction.UP) {
                        level.setBlockAndUpdate(relativePos, belowState);
                     } else {
                        level.setBlockAndUpdate(relativePos, softState);
                     }
                  }
               }

               this.boundingBox = new BoundingBox(pos);
               this.createChest(level, chunkBB, random, pos, BuiltInLootTables.BURIED_TREASURE, null);
               return;
            }

            pos.move(0, -1, 0);
         }
      }

      private static boolean isLiquid(final BlockState blockState) {
         return blockState.is(Blocks.WATER) || blockState.is(Blocks.LAVA);
      }
   }
}
