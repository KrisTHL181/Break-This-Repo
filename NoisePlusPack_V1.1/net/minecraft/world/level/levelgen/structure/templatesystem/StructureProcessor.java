package net.minecraft.world.level.levelgen.structure.templatesystem;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public abstract class StructureProcessor {
   public StructureTemplate.@Nullable StructureBlockInfo processBlock(
      LevelReader p_74416_,
      BlockPos p_74417_,
      BlockPos p_74418_,
      StructureTemplate.StructureBlockInfo p_74419_,
      StructureTemplate.StructureBlockInfo p_74420_,
      StructurePlaceSettings p_74421_
   ) {
      return p_74420_;
   }

   protected abstract StructureProcessorType<?> getType();

   public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
      ServerLevelAccessor p_278247_,
      BlockPos p_277590_,
      BlockPos p_277935_,
      List<StructureTemplate.StructureBlockInfo> p_278070_,
      List<StructureTemplate.StructureBlockInfo> p_278053_,
      StructurePlaceSettings p_277497_
   ) {
      return p_278053_;
   }
}
