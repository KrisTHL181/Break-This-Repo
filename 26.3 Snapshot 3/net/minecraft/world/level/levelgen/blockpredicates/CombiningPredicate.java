package net.minecraft.world.level.levelgen.blockpredicates;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;

public abstract class CombiningPredicate implements BlockPredicate {
   protected final List<BlockPredicate> predicates;

   protected CombiningPredicate(final List<BlockPredicate> predicates) {
      this.predicates = predicates;
   }

   public static <T extends CombiningPredicate> MapCodec<T> codec(final Function<List<BlockPredicate>, T> constructor) {
      return RecordCodecBuilder.mapCodec(i -> i.group(BlockPredicate.CODEC.listOf().fieldOf("predicates").forGetter(p -> p.predicates)).apply(i, constructor));
   }
}
