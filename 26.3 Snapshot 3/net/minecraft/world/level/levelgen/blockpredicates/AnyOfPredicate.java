package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;

public class AnyOfPredicate extends CombiningPredicate {
   public static final MapCodec<AnyOfPredicate> CODEC = codec(AnyOfPredicate::new);

   public AnyOfPredicate(final List<BlockPredicate> predicates) {
      super(predicates);
   }

   public boolean test(final LevelAccessor level, final BlockPos origin) {
      for (BlockPredicate predicate : this.predicates) {
         if (predicate.test(level, origin)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public BlockPredicateType<?> type() {
      return BlockPredicateType.ANY_OF;
   }
}
