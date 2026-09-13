package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionCountsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {
   List<CollectionCountsPredicate.Entry<T, P>> unpack();

   static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate<T, P>> codec(Codec<P> p_454965_) {
      return CollectionCountsPredicate.Entry.codec(p_454965_).listOf().xmap(CollectionCountsPredicate::of, CollectionCountsPredicate::unpack);
   }

   @SafeVarargs
   static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(CollectionCountsPredicate.Entry<T, P>... p_457183_) {
      return of(List.of(p_457183_));
   }

   static <T, P extends Predicate<T>> CollectionCountsPredicate<T, P> of(List<CollectionCountsPredicate.Entry<T, P>> p_455799_) {
      return switch (p_455799_.size()) {
         case 0 -> new CollectionCountsPredicate.Zero();
         case 1 -> new CollectionCountsPredicate.Single(p_455799_.getFirst());
         default -> new CollectionCountsPredicate.Multiple(p_455799_);
      };
   }

   record Entry<T, P extends Predicate<T>>(P test, MinMaxBounds.Ints count) {
      public static <T, P extends Predicate<T>> Codec<CollectionCountsPredicate.Entry<T, P>> codec(Codec<P> p_453794_) {
         return RecordCodecBuilder.create(
            p_458113_ -> p_458113_.group(
                  p_453794_.fieldOf("test").forGetter(CollectionCountsPredicate.Entry::test),
                  MinMaxBounds.Ints.CODEC.fieldOf("count").forGetter(CollectionCountsPredicate.Entry::count)
               )
               .apply(p_458113_, CollectionCountsPredicate.Entry::new)
         );
      }

      public boolean test(Iterable<T> p_454670_) {
         int i = 0;

         for (T t : p_454670_) {
            if (this.test.test(t)) {
               i++;
            }
         }

         return this.count.matches(i);
      }
   }

   record Multiple<T, P extends Predicate<T>>(List<CollectionCountsPredicate.Entry<T, P>> entries) implements CollectionCountsPredicate<T, P> {
      public boolean test(Iterable<T> p_455807_) {
         for (CollectionCountsPredicate.Entry<T, P> entry : this.entries) {
            if (!entry.test(p_455807_)) {
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
      public boolean test(Iterable<T> p_452412_) {
         return this.entry.test(p_452412_);
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return List.of(this.entry);
      }
   }

   class Zero<T, P extends Predicate<T>> implements CollectionCountsPredicate<T, P> {
      public boolean test(Iterable<T> p_460289_) {
         return true;
      }

      @Override
      public List<CollectionCountsPredicate.Entry<T, P>> unpack() {
         return List.of();
      }
   }
}
