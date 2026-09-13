package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class EnvironmentScanPlacement extends PlacementModifier {
   private final Direction directionOfSearch;
   private final BlockPredicate targetCondition;
   private final BlockPredicate allowedSearchCondition;
   private final int maxSteps;
   public static final MapCodec<EnvironmentScanPlacement> CODEC = RecordCodecBuilder.mapCodec(
      p_191650_ -> p_191650_.group(
            Direction.VERTICAL_CODEC.fieldOf("direction_of_search").forGetter(p_191672_ -> p_191672_.directionOfSearch),
            BlockPredicate.CODEC.fieldOf("target_condition").forGetter(p_191670_ -> p_191670_.targetCondition),
            BlockPredicate.CODEC
               .optionalFieldOf("allowed_search_condition", BlockPredicate.alwaysTrue())
               .forGetter(p_191668_ -> p_191668_.allowedSearchCondition),
            Codec.intRange(1, 32).fieldOf("max_steps").forGetter(p_191652_ -> p_191652_.maxSteps)
         )
         .apply(p_191650_, EnvironmentScanPlacement::new)
   );

   private EnvironmentScanPlacement(Direction p_191645_, BlockPredicate p_191646_, BlockPredicate p_191647_, int p_191648_) {
      this.directionOfSearch = p_191645_;
      this.targetCondition = p_191646_;
      this.allowedSearchCondition = p_191647_;
      this.maxSteps = p_191648_;
   }

   public static EnvironmentScanPlacement scanningFor(Direction p_191658_, BlockPredicate p_191659_, BlockPredicate p_191660_, int p_191661_) {
      return new EnvironmentScanPlacement(p_191658_, p_191659_, p_191660_, p_191661_);
   }

   public static EnvironmentScanPlacement scanningFor(Direction p_191654_, BlockPredicate p_191655_, int p_191656_) {
      return scanningFor(p_191654_, p_191655_, BlockPredicate.alwaysTrue(), p_191656_);
   }

   @Override
   public Stream<BlockPos> getPositions(PlacementContext p_226336_, RandomSource p_226337_, BlockPos p_226338_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_226338_.mutable();
      WorldGenLevel worldgenlevel = p_226336_.getLevel();
      if (!this.allowedSearchCondition.test(worldgenlevel, blockpos$mutableblockpos)) {
         return Stream.of();
      }

      for (int i = 0; i < this.maxSteps; i++) {
         if (this.targetCondition.test(worldgenlevel, blockpos$mutableblockpos)) {
            return Stream.of(blockpos$mutableblockpos);
         }

         blockpos$mutableblockpos.move(this.directionOfSearch);
         if (worldgenlevel.isOutsideBuildHeight(blockpos$mutableblockpos.getY())) {
            return Stream.of();
         }

         if (!this.allowedSearchCondition.test(worldgenlevel, blockpos$mutableblockpos)) {
            break;
         }
      }

      return this.targetCondition.test(worldgenlevel, blockpos$mutableblockpos) ? Stream.of(blockpos$mutableblockpos) : Stream.of();
   }

   @Override
   public PlacementModifierType<?> type() {
      return PlacementModifierType.ENVIRONMENT_SCAN;
   }
}
