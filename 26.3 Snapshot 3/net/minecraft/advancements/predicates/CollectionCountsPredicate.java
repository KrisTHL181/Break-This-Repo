package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionCountsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<? extends T>> {
   List<CollectionCountsPredicate.Entry<T, P>> unpack();

   static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate<T, P>> codec(final Codec<P> elementCodec) {
      return CollectionCountsPredicate.Entry.codec(elementCodec).listOf().xmap(CollectionCountsPredicate::of, CollectionCountsPredicate::unpack);
   }

   @SafeVarargs
   static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(final CollectionCountsPredicate.Entry<T, P>... predicates) {
      return of(List.of(predicates));
   }

   static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(final List<CollectionCountsPredicate.Entry<T, P>> predicates) {
      return switch (predicates.size()) {
         case 0 -> new CollectionCountsPredicate.Zero();
         case 1 -> new CollectionCountsPredicate.Single(predicates.getFirst());
         default -> new CollectionCountsPredicate.Multiple(predicates);
      };
   }

   record Entry<T, P extends Predicate<T>>(P test, MinMaxBounds.Ints count) {
      public static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate.Entry<T, P>> codec(final Codec<P> elementCodec) {
         return RecordCodecBuilder.create(
            i -> i.group(
                  elementCodec.fieldOf("test").forGetter(CollectionCountsPredicate.Entry::test),
                  MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(CollectionCountsPredicate.Entry::count)
               )
               .apply(i, CollectionCountsPredicate.Entry::new)
         );
      }

      public boolean test(final Iterable<? extends T> values) {
         int count = 0;

         for (T value : values) {
            if (this.test.test(value)) {
               count++;
            }
         }

         return this.count.matches(count);
      }
   }

   record Multiple<T, P extends Predicate<T>>(List<CollectionCountsPredicate.Entry<T, P>> entries) implements CollectionCountsPredicate<T, P> {
      public boolean test(final Iterable<? extends T> values) {
         for (CollectionCountsPredicate.Entry<T, P> entry : this.entries) {
            if (!entry.test(values)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return this.entries;
      }
   }

   record Single<T, P extends Predicate<T>>(CollectionCountsPredicate.Entry<T, P> entry) implements CollectionCountsPredicate<T, P> {
      public boolean test(final Iterable<? extends T> values) {
         return this.entry.test(values);
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return List.of(this.entry);
      }
   }

   class Zero<T, P extends Predicate<T>> implements CollectionCountsPredicate<T, P> {
      public boolean test(final Iterable<? extends T> values) {
         return true;
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return List.of();
      }
   }
}
