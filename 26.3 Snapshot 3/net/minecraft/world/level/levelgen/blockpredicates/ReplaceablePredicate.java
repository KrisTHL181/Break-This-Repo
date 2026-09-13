package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

public class ReplaceablePredicate extends StateTestingPredicate {
   public static final MapCodec<ReplaceablePredicate> CODEC = RecordCodecBuilder.mapCodec(i -> stateTestingCodec(i).apply(i, ReplaceablePredicate::new));

   public ReplaceablePredicate(final Vec3i offset) {
      super(offset);
   }

   @Override
   protected boolean test(final BlockState state) {
      return state.canBeReplaced();
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.REPLACEABLE;
   }
}
