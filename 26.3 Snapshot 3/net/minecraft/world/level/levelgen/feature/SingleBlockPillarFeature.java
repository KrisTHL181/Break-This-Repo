package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public record SingleBlockPillarFeature(
   BlockStateProvider block, BlockPredicate canReplace, Direction direction, float chanceToContinue, Optional<Holder<PlacedFeature>> capFeature
) implements Feature {
   public static final MapCodec<SingleBlockPillarFeature> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            BlockStateProvider.CODEC.fieldOf("block").forGetter(SingleBlockPillarFeature::block),
            BlockPredicate.CODEC.optionalFieldOf("can_replace", BlockPredicate.alwaysTrue()).forGetter(SingleBlockPillarFeature::canReplace),
            Direction.VERTICAL_CODEC.fieldOf("direction").forGetter(SingleBlockPillarFeature::direction),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("chance_to_continue", 1.0F).forGetter(SingleBlockPillarFeature::chanceToContinue),
            PlacedFeature.CODEC.optionalFieldOf("cap_feature").forGetter(SingleBlockPillarFeature::capFeature)
         )
         .apply(i, SingleBlockPillarFeature::new)
   );

   public SingleBlockPillarFeature(final BlockStateProvider block, final BlockPredicate mayReplace, final Direction direction, final float chanceToContinue) {
      this(block, mayReplace, direction, chanceToContinue, Optional.empty());
   }

   @Override
   public MapCodec<SingleBlockPillarFeature> codec() {
      return CODEC;
   }

   @Override
   public boolean place(final WorldGenLevel level, final ChunkGenerator chunkGenerator, final RandomSource random, final BlockPos origin) {
      BlockPos.MutableBlockPos pos = origin.mutable();

      while (this.canReplace.test(level, pos) && random.nextFloat() < this.chanceToContinue && !level.isOutsideBuildHeight(pos)) {
         level.setBlock(pos, this.block.getState(level, random, pos), 2);
         pos.move(this.direction);
      }

      pos.move(this.direction.getOpposite());
      this.capFeature.ifPresent(feature -> feature.value().place(level, chunkGenerator, random, pos));
      return true;
   }
}
