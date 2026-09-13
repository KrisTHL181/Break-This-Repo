package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

public record ProtectedBlockProcessor(HolderSet<Block> cannotReplace) implements StructureProcessor {
   public static final MapCodec<ProtectedBlockProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("value").forGetter(ProtectedBlockProcessor::cannotReplace))
         .apply(i, ProtectedBlockProcessor::new)
   );

   @Override
   public StructureTemplate.@Nullable StructureBlockInfo processBlock(
      final LevelReader level,
      final BlockPos targetPosition,
      final BlockPos referencePos,
      final BlockPos templateRelativePos,
      final StructureTemplate.StructureBlockInfo processedBlockInfo,
      final StructurePlaceSettings settings
   ) {
      return !level.getBlockState(processedBlockInfo.pos()).is(this.cannotReplace) ? processedBlockInfo : null;
   }

   @Override
   public MapCodec<ProtectedBlockProcessor> codec() {
      return MAP_CODEC;
   }
}
