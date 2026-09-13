package net.minecraft.advancements.predicates;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;

public record CollectionPredicate<T, P extends Predicate<T>>(
   Optional<CollectionContentsPredicate<T, P>> contains, Optional<CollectionCountsPredicate<T, P>> counts, Optional<MinMaxBounds.Ints> size
) implements Predicate<Iterable<? extends T>> {
   public static <T, P extends Predicate<T>> Codec<CollectionPredicate<T, P>> codec(final Codec<P> elementCodec) {
      return RecordCodecBuilder.create(
         i -> i.group(
               CollectionContentsPredicate.codec(elementCodec).optionalFieldOf("contains").forGetter(CollectionPredicate::contains),
               CollectionCountsPredicate.codec(elementCodec).optionalFieldOf("count").forGetter(CollectionPredicate::counts),
               MinMaxBounds.Ints.CODEC.optionalFieldOf("size").forGetter(CollectionPredicate::size)
            )
            .apply(i, CollectionPredicate::new)
      );
   }

   public boolean test(final Iterable<? extends T> value) {
      if (this.contains.isPresent() && !this.contains.get().test(value)) {
         return false;
      } else {
         return this.counts.isPresent() && !this.counts.get().test(value) ? false : !this.size.isPresent() || this.size.get().matches(Iterables.size(value));
      }
   }
}
