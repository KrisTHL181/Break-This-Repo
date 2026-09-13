package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.WorldGenerationContext;

public record HeightRangePredicate(VerticalAnchor minInclusive, VerticalAnchor maxInclusive) implements BlockPredicate {
   public static final MapCodec<HeightRangePredicate> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            VerticalAnchor.CODEC.fieldOf("min_inclusive").forGetter(HeightRangePredicate::minInclusive),
            VerticalAnchor.CODEC.fieldOf("max_inclusive").forGetter(HeightRangePredicate::maxInclusive)
         )
         .apply(i, HeightRangePredicate::new)
   );

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.HEIGHT_RANGE;
   }

   public boolean test(final LevelAccessor level, final BlockPos pos) {
      WorldGenerationContext context = WorldGenerationContext.of(level);
      int min = this.minInclusive.resolveY(context);
      int max = this.maxInclusive.resolveY(context);
      return pos.getY() >= min && pos.getY() <= max;
   }
}
