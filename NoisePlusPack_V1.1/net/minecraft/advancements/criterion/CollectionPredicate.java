package net.minecraft.advancements.criterion;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;

public record CollectionPredicate<T, P extends Predicate<T>>(
   Optional<CollectionContentsPredicate<T, P>> contains, Optional<CollectionCountsPredicate<T, P>> counts, Optional<MinMaxBounds.Ints> size
) implements Predicate<Iterable<T>> {
   public static <T, P extends Predicate<T>> Codec<CollectionPredicate<T, P>> codec(Codec<P> p_456411_) {
      return RecordCodecBuilder.create(
         p_451304_ -> p_451304_.group(
               CollectionContentsPredicate.codec(p_456411_).optionalFieldOf("contains").forGetter(CollectionPredicate::contains),
               CollectionCountsPredicate.codec(p_456411_).optionalFieldOf("count").forGetter(CollectionPredicate::counts),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("size").forGetter(CollectionPredicate::size)
            )
            .apply(p_451304_, CollectionPredicate::new)
      );
   }

   public boolean test(Iterable<T> p_456577_) {
      if (this.contains.isPresent() && !this.contains.get().test(p_456577_)) {
         return false;
      } else {
         return this.counts.isPresent() && !this.counts.get().test(p_456577_)
            ? false
            : !this.size.isPresent() || this.size.get().matches(Iterables.size(p_456577_));
      }
   }
}
