package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public record BlockPredicateFilter(BlockPredicate predicate) implements PlacementFilter {
   public static final MapCodec<BlockPredicateFilter> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(BlockPredicate.CODEC.fieldOf("predicate").forGetter(BlockPredicateFilter::predicate)).apply(i, BlockPredicateFilter::new)
   );

   public static BlockPredicateFilter forPredicate(final BlockPredicate predicate) {
      return new BlockPredicateFilter(predicate);
   }

   @Override
   public boolean shouldPlace(final PlacementContext context, final RandomSource random, final BlockPos origin) {
      return this.predicate.test(context.getLevel(), origin);
   }

   @Override
   public MapCodec<BlockPredicateFilter> codec() {
      return CODEC;
   }
}
