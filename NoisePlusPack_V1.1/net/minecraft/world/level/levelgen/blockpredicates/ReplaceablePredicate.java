package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

class ReplaceablePredicate extends StateTestingPredicate {
   public static final MapCodec<ReplaceablePredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_190529_ -> stateTestingCodec(p_190529_).apply(p_190529_, ReplaceablePredicate::new)
   );

   public ReplaceablePredicate(Vec3i p_190524_) {
      super(p_190524_);
   }

   @Override
   protected boolean test(BlockState p_190527_) {
      return p_190527_.canBeReplaced();
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.REPLACEABLE;
   }
}
