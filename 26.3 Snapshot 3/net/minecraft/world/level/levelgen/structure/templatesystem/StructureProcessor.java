package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

public interface StructureProcessor {
   default StructureTemplate.@Nullable StructureBlockInfo processBlock(
      final LevelReader level,
      final BlockPos targetPosition,
      final BlockPos referencePos,
      final BlockPos templateRelativePos,
      final StructureTemplate.StructureBlockInfo processedBlockInfo,
      final StructurePlaceSettings settings
   ) {
      return processedBlockInfo;
   }

   MapCodec<? extends StructureProcessor> codec();

   default List<StructureTemplate.StructureBlockInfo> finalizeProcessing(
      final ServerLevelAccessor level,
      final BlockPos position,
      final BlockPos referencePos,
      final List<StructureTemplate.StructureBlockInfo> originalBlockInfoList,
      final List<StructureTemplate.StructureBlockInfo> processedBlockInfoList,
      final StructurePlaceSettings settings
   ) {
      return processedBlockInfoList;
   }

   default boolean evaluatesEntirePieceState() {
      return false;
   }
}
