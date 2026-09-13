package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public record EnvironmentScanPlacement(Direction directionOfSearch, BlockPredicate targetCondition, BlockPredicate allowedSearchCondition, int maxSteps)
   implements PlacementModifier {
   public static final MapCodec<EnvironmentScanPlacement> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(
            Direction.VERTICAL_CODEC.fieldOf("direction_of_search").forGetter(EnvironmentScanPlacement::directionOfSearch),
            BlockPredicate.CODEC.fieldOf("target_condition").forGetter(EnvironmentScanPlacement::targetCondition),
            BlockPredicate.CODEC
               .optionalFieldOf("allowed_search_condition", BlockPredicate.alwaysTrue())
               .forGetter(EnvironmentScanPlacement::allowedSearchCondition),
            Codec.intRange(1, 32).fieldOf("max_steps").forGetter(EnvironmentScanPlacement::maxSteps)
         )
         .apply(i, EnvironmentScanPlacement::new)
   );

   public static EnvironmentScanPlacement scanningFor(
      final Direction directionOfSearch, final BlockPredicate targetCondition, final BlockPredicate allowedSearchCondition, final int maxSteps
   ) {
      return new EnvironmentScanPlacement(directionOfSearch, targetCondition, allowedSearchCondition, maxSteps);
   }

   public static EnvironmentScanPlacement scanningFor(final Direction directionOfSearch, final BlockPredicate targetCondition, final int maxSteps) {
      return scanningFor(directionOfSearch, targetCondition, BlockPredicate.alwaysTrue(), maxSteps);
   }

   @Override
   public void modify(final PlacementContext context, final RandomSource random, final BlockPos origin, final Consumer<BlockPos> output) {
      BlockPos.MutableBlockPos pos = origin.mutable();
      WorldGenLevel level = context.getLevel();
      if (this.allowedSearchCondition.test(level, pos)) {
         for (int i = 0; i < this.maxSteps; i++) {
            if (this.targetCondition.test(level, pos)) {
               output.accept(pos);
               return;
            }

            pos.move(this.directionOfSearch);
            if (level.isOutsideBuildHeight(pos.getY())) {
               return;
            }

            if (!this.allowedSearchCondition.test(level, pos)) {
               break;
            }
         }

         if (this.targetCondition.test(level, pos)) {
            output.accept(pos);
         }

         return;
      }
   }

   @Override
   public MapCodec<EnvironmentScanPlacement> codec() {
      return CODEC;
   }
}
