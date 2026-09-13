package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.SinglePieceStructure;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class DesertPyramidStructure extends SinglePieceStructure {
   public static final MapCodec<DesertPyramidStructure> CODEC = simpleCodec(DesertPyramidStructure::new);

   public DesertPyramidStructure(Structure.StructureSettings p_227418_) {
      super(DesertPyramidPiece::new, 21, 21, p_227418_);
   }

   @Override
   public void afterPlace(
      WorldGenLevel p_273644_,
      StructureManager p_272615_,
      ChunkGenerator p_273655_,
      RandomSource p_272939_,
      BoundingBox p_273179_,
      ChunkPos p_273334_,
      PiecesContainer p_273575_
   ) {
      Set<BlockPos> set = SortedArraySet.create(Vec3i::compareTo);

      for (StructurePiece structurepiece : p_273575_.pieces()) {
         if (structurepiece instanceof DesertPyramidPiece desertpyramidpiece) {
            set.addAll(desertpyramidpiece.getPotentialSuspiciousSandWorldPositions());
            placeSuspiciousSand(p_273179_, p_273644_, desertpyramidpiece.getRandomCollapsedRoofPos());
         }
      }

      ObjectArrayList<BlockPos> objectarraylist = new ObjectArrayList(set.stream().toList());
      RandomSource randomsource = RandomSource.create(p_273644_.getSeed()).forkPositional().at(p_273575_.calculateBoundingBox().getCenter());
      Util.shuffle(objectarraylist, randomsource);
      int i = Math.min(set.size(), randomsource.nextInt(5, 8));
      ObjectListIterator var12 = objectarraylist.iterator();

      while (var12.hasNext()) {
         BlockPos blockpos = (BlockPos)var12.next();
         if (i > 0) {
            i--;
            placeSuspiciousSand(p_273179_, p_273644_, blockpos);
         } else if (p_273179_.isInside(blockpos)) {
            p_273644_.setBlock(blockpos, Blocks.SAND.defaultBlockState(), 2);
         }
      }
   }

   private static void placeSuspiciousSand(BoundingBox p_279472_, WorldGenLevel p_279193_, BlockPos p_279136_) {
      if (p_279472_.isInside(p_279136_)) {
         p_279193_.setBlock(p_279136_, Blocks.SUSPICIOUS_SAND.defaultBlockState(), 2);
         p_279193_.getBlockEntity(p_279136_, BlockEntityType.BRUSHABLE_BLOCK)
            .ifPresent(p_327492_ -> p_327492_.setLootTable(BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY, p_279136_.asLong()));
      }
   }

   @Override
   public StructureType<?> type() {
      return StructureType.DESERT_PYRAMID;
   }
}
