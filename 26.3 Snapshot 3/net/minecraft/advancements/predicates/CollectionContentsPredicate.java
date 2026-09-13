package net.minecraft.advancements.predicates;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionContentsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<? extends T>> {
   List<P> unpack();

   static <T, P extends Predicate<T>> Codec<CollectionContentsPredicate<T, P>> codec(final Codec<P> elementCodec) {
      return elementCodec.listOf().xmap(CollectionContentsPredicate::of, CollectionContentsPredicate::unpack);
   }

   @SafeVarargs
   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(final P... predicates) {
      return of(List.of(predicates));
   }

   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(final List<P> predicates) {
      return switch (predicates.size()) {
         case 0 -> new CollectionContentsPredicate.Zero();
         case 1 -> new CollectionContentsPredicate.Single(predicates.getFirst());
         default -> new CollectionContentsPredicate.Multiple(predicates);
      };
   }

   record Multiple<T, P extends Predicate<T>>(List<P> tests) implements CollectionContentsPredicate<T, P> {
      public boolean test(final Iterable<? extends T> values) {
         List<Predicate<T>> testsToMatch = new ArrayList<>(this.tests);

         for (T value : values) {
            testsToMatch.removeIf(p -> p.test(value));
            if (testsToMatch.isEmpty()) {
               return true;
            }
         }

         return false;
      }

      @Override
      public List<P> unpack() {
         return this.tests;
      }
   }

   record Single<T, P extends Predicate<T>>(P test) implements CollectionContentsPredicate<T, P> {
      public boolean test(final Iterable<? extends T> values) {
         for (T value : values) {
            if (this.test.test(value)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public List<P> unpack() {
         return List.of(this.test);
      }
   }

   class Zero<T, P extends Predicate<T>> implements CollectionContentsPredicate<T, P> {
      public boolean test(final Iterable<? extends T> values) {
         return true;
      }

      @Override
      public List<P> unpack() {
         return List.of();
      }
   }
}
