package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockIgnoreProcessor implements StructureProcessor {
   private static final Codec<Block> WEIRD_BLOCK_STATE_CODEC = BlockState.CODEC.xmap(BlockBehaviour.BlockStateBase::getBlock, Block::defaultBlockState);
   public static final MapCodec<BlockIgnoreProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(WEIRD_BLOCK_STATE_CODEC.listOf().fieldOf("blocks").forGetter(o -> o.toIgnore)).apply(i, BlockIgnoreProcessor::new)
   );
   public static final BlockIgnoreProcessor STRUCTURE_BLOCK = new BlockIgnoreProcessor(ImmutableList.of(Blocks.STRUCTURE_BLOCK));
   public static final BlockIgnoreProcessor AIR = new BlockIgnoreProcessor(ImmutableList.of(Blocks.AIR));
   public static final BlockIgnoreProcessor STRUCTURE_AND_AIR = new BlockIgnoreProcessor(ImmutableList.of(Blocks.AIR, Blocks.STRUCTURE_BLOCK));
   private final ImmutableList<Block> toIgnore;

   public BlockIgnoreProcessor(final List<Block> toIgnore) {
      this.toIgnore = ImmutableList.copyOf(toIgnore);
   }

   @Override
   public StructureTemplate.@Nullable StructureBlockInfo processBlock(
      final LevelReader level,
      final BlockPos targetPosition,
      final BlockPos referencePos,
      final BlockPos templateRelativePos,
      final StructureTemplate.StructureBlockInfo processedBlockInfo,
      final StructurePlaceSettings settings
   ) {
      return this.toIgnore.contains(processedBlockInfo.state().getBlock()) ? null : processedBlockInfo;
   }

   @Override
   public MapCodec<BlockIgnoreProcessor> codec() {
      return MAP_CODEC;
   }
}
