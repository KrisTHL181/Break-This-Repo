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
      public BuriedTreasurePiece(BlockPos p_227366_) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, 0, new BoundingBox(p_227366_));
      }

      public BuriedTreasurePiece(CompoundTag p_227368_) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, p_227368_);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_227378_, CompoundTag p_227379_) {
      }

      @Override
      public void postProcess(
         WorldGenLevel p_227370_,
         StructureManager p_227371_,
         ChunkGenerator p_227372_,
         RandomSource p_227373_,
         BoundingBox p_227374_,
         ChunkPos p_227375_,
         BlockPos p_227376_
      ) {
         int i = p_227370_.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.boundingBox.minX(), this.boundingBox.minZ());
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(this.boundingBox.minX(), i, this.boundingBox.minZ());

         while (blockpos$mutableblockpos.getY() > p_227370_.getMinY()) {
            BlockState blockstate = p_227370_.getBlockState(blockpos$mutableblockpos);
            BlockState blockstate1 = p_227370_.getBlockState(blockpos$mutableblockpos.below());
            if (blockstate1 == Blocks.SANDSTONE.defaultBlockState()
               || blockstate1 == Blocks.STONE.defaultBlockState()
               || blockstate1 == Blocks.ANDESITE.defaultBlockState()
               || blockstate1 == Blocks.GRANITE.defaultBlockState()
               || blockstate1 == Blocks.DIORITE.defaultBlockState()) {
               BlockState blockstate2 = !blockstate.isAir() && !this.isLiquid(blockstate) ? blockstate : Blocks.SAND.defaultBlockState();

               for (Direction direction : Direction.values()) {
                  BlockPos blockpos = blockpos$mutableblockpos.relative(direction);
                  BlockState blockstate3 = p_227370_.getBlockState(blockpos);
                  if (blockstate3.isAir() || this.isLiquid(blockstate3)) {
                     BlockPos blockpos1 = blockpos.below();
                     BlockState blockstate4 = p_227370_.getBlockState(blockpos1);
                     if ((blockstate4.isAir() || this.isLiquid(blockstate4)) && direction != Direction.UP) {
                        p_227370_.setBlock(blockpos, blockstate1, 3);
                     } else {
                        p_227370_.setBlock(blockpos, blockstate2, 3);
                     }
                  }
               }

               this.boundingBox = new BoundingBox(blockpos$mutableblockpos);
               this.createChest(p_227370_, p_227374_, p_227373_, blockpos$mutableblockpos, BuiltInLootTables.BURIED_TREASURE, null);
               return;
            }

            blockpos$mutableblockpos.move(0, -1, 0);
         }
      }

      private boolean isLiquid(BlockState p_227381_) {
         return p_227381_ == Blocks.WATER.defaultBlockState() || p_227381_ == Blocks.LAVA.defaultBlockState();
      }
   }
}
