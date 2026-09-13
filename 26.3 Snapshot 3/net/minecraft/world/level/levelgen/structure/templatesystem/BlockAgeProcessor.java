package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import org.jspecify.annotations.Nullable;

public class BlockAgeProcessor implements StructureProcessor {
   public static final MapCodec<BlockAgeProcessor> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.FLOAT.fieldOf("mossiness").forGetter(p -> p.mossiness)).apply(i, BlockAgeProcessor::new)
   );
   private static final float PROBABILITY_OF_REPLACING_FULL_BLOCK = 0.5F;
   private static final float PROBABILITY_OF_REPLACING_STAIRS = 0.5F;
   private static final float PROBABILITY_OF_REPLACING_OBSIDIAN = 0.15F;
   private static final BlockState[] NON_MOSSY_REPLACEMENTS = new BlockState[]{
      Blocks.STONE_SLAB.defaultBlockState(), Blocks.STONE_BRICK_SLAB.defaultBlockState()
   };
   private final float mossiness;

   public BlockAgeProcessor(final float mossiness) {
      this.mossiness = mossiness;
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
      RandomSource random = settings.getRandom(processedBlockInfo.pos());
      BlockState state = processedBlockInfo.state();
      BlockPos pos = processedBlockInfo.pos();
      BlockState newState = null;
      if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.STONE) || state.is(Blocks.CHISELED_STONE_BRICKS)) {
         newState = this.maybeReplaceFullStoneBlock(random);
      } else if (state.is(BlockTags.STAIRS)) {
         newState = this.maybeReplaceStairs(state, random);
      } else if (state.is(BlockTags.SLABS)) {
         newState = this.maybeReplaceSlab(state, random);
      } else if (state.is(BlockTags.WALLS)) {
         newState = this.maybeReplaceWall(state, random);
      } else if (state.is(Blocks.OBSIDIAN)) {
         newState = this.maybeReplaceObsidian(random);
      }

      return newState != null ? new StructureTemplate.StructureBlockInfo(pos, newState, processedBlockInfo.nbt()) : processedBlockInfo;
   }

   private @Nullable BlockState maybeReplaceFullStoneBlock(final RandomSource random) {
      if (random.nextFloat() >= 0.5F) {
         return null;
      }

      BlockState[] nonMossyReplacements = new BlockState[]{
         Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(random, Blocks.STONE_BRICK_STAIRS)
      };
      BlockState[] mossyReplacements = new BlockState[]{
         Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), getRandomFacingStairs(random, Blocks.MOSSY_STONE_BRICK_STAIRS)
      };
      return this.getRandomBlock(random, nonMossyReplacements, mossyReplacements);
   }

   private @Nullable BlockState maybeReplaceStairs(final BlockState blockState, final RandomSource random) {
      if (random.nextFloat() >= 0.5F) {
         return null;
      }

      BlockState[] mossyReplacements = new BlockState[]{
         Blocks.MOSSY_STONE_BRICK_STAIRS.withPropertiesOf(blockState), Blocks.MOSSY_STONE_BRICK_SLAB.defaultBlockState()
      };
      return this.getRandomBlock(random, NON_MOSSY_REPLACEMENTS, mossyReplacements);
   }

   private @Nullable BlockState maybeReplaceSlab(final BlockState blockState, final RandomSource random) {
      return random.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_SLAB.withPropertiesOf(blockState) : null;
   }

   private @Nullable BlockState maybeReplaceWall(final BlockState blockState, final RandomSource random) {
      return random.nextFloat() < this.mossiness ? Blocks.MOSSY_STONE_BRICK_WALL.withPropertiesOf(blockState) : null;
   }

   private @Nullable BlockState maybeReplaceObsidian(final RandomSource random) {
      return random.nextFloat() < 0.15F ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : null;
   }

   private static BlockState getRandomFacingStairs(final RandomSource random, final Block stairBlock) {
      return stairBlock.defaultBlockState()
         .setValue(StairBlock.FACING, Direction.Plane.HORIZONTAL.getRandomDirection(random))
         .setValue(StairBlock.HALF, Util.getRandom(Half.values(), random));
   }

   private BlockState getRandomBlock(final RandomSource random, final BlockState[] nonMossyBlocks, final BlockState[] mossyBlocks) {
      return random.nextFloat() < this.mossiness ? getRandomBlock(random, mossyBlocks) : getRandomBlock(random, nonMossyBlocks);
   }

   private static BlockState getRandomBlock(final RandomSource random, final BlockState[] blocks) {
      return blocks[random.nextInt(blocks.length)];
   }

   @Override
   public MapCodec<BlockAgeProcessor> codec() {
      return MAP_CODEC;
   }
}
