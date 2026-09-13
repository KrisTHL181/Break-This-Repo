package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;

@Deprecated
public class SolidPredicate extends StateTestingPredicate {
   public static final MapCodec<SolidPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_190538_ -> stateTestingCodec(p_190538_).apply(p_190538_, SolidPredicate::new)
   );

   public SolidPredicate(Vec3i p_190533_) {
      super(p_190533_);
   }

   @Override
   protected boolean test(BlockState p_190536_) {
      return p_190536_.isSolid();
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.SOLID;
   }
}
