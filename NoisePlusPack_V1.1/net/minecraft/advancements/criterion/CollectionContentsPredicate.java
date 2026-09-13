package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionContentsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {
   List<P> unpack();

   static <T, P extends Predicate<T>> Codec<CollectionContentsPredicate<T, P>> codec(Codec<P> p_460194_) {
      return p_460194_.listOf().xmap(CollectionContentsPredicate::of, CollectionContentsPredicate::unpack);
   }

   @SafeVarargs
   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(P... p_457925_) {
      return of(List.of(p_457925_));
   }

   static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(List<P> p_453046_) {
      return switch (p_453046_.size()) {
         case 0 -> new CollectionContentsPredicate.Zero();
         case 1 -> new CollectionContentsPredicate.Single(p_453046_.getFirst());
         default -> new CollectionContentsPredicate.Multiple(p_453046_);
      };
   }

   record Multiple<T, P extends Predicate<T>>(List<P> tests) implements CollectionContentsPredicate<T, P> {
      public boolean test(Iterable<T> p_458202_) {
         List<Predicate<T>> list = new ArrayList<>(this.tests);

         for (T t : p_458202_) {
            list.removeIf(p_459957_ -> p_459957_.test(t));
            if (list.isEmpty()) {
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
      public boolean test(Iterable<T> p_460928_) {
         for (T t : p_460928_) {
            if (this.test.test(t)) {
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
      public boolean test(Iterable<T> p_451116_) {
         return true;
      }

      @Override
      public List<P> unpack() {
         return List.of();
      }
   }
}
