package net.minecraft.util.random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public final class WeightedList<E> {
   private static final int FLAT_THRESHOLD = 64;
   private final int totalWeight;
   private final List<Weighted<E>> items;
   private final WeightedList.@Nullable Selector<E> selector;

   WeightedList(List<? extends Weighted<E>> p_393632_) {
      this.items = List.copyOf(p_393632_);
      this.totalWeight = WeightedRandom.getTotalWeight(p_393632_, Weighted::weight);
      if (this.totalWeight == 0) {
         this.selector = null;
      } else if (this.totalWeight < 64) {
         this.selector = new WeightedList.Flat<>(this.items, this.totalWeight);
      } else {
         this.selector = new WeightedList.Compact<>(this.items);
      }
   }

   public static <E> WeightedList<E> of() {
      return new WeightedList<>(List.of());
   }

   public static <E> WeightedList<E> of(E p_391442_) {
      return new WeightedList<>(List.of(new Weighted<>(p_391442_, 1)));
   }

   @SafeVarargs
   public static <E> WeightedList<E> of(Weighted<E>... p_395273_) {
      return new WeightedList<>(List.of(p_395273_));
   }

   public static <E> WeightedList<E> of(List<Weighted<E>> p_396025_) {
      return new WeightedList<>(p_396025_);
   }

   public static <E> WeightedList.Builder<E> builder() {
      return new WeightedList.Builder<>();
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   public <T> WeightedList<T> map(Function<E, T> p_391461_) {
      return new WeightedList(Lists.transform(this.items, p_392113_ -> p_392113_.map(p_391461_)));
   }

   public Optional<E> getRandom(RandomSource p_394760_) {
      if (this.selector == null) {
         return Optional.empty();
      }

      int i = p_394760_.nextInt(this.totalWeight);
      return Optional.of(this.selector.get(i));
   }

   public E getRandomOrThrow(RandomSource p_395959_) {
      if (this.selector == null) {
         throw new IllegalStateException("Weighted list has no elements");
      }

      int i = p_395959_.nextInt(this.totalWeight);
      return this.selector.get(i);
   }

   public List<Weighted<E>> unwrap() {
      return this.items;
   }

   public static <E> Codec<WeightedList<E>> codec(Codec<E> p_395633_) {
      return Weighted.codec(p_395633_).listOf().xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> codec(MapCodec<E> p_392171_) {
      return Weighted.codec(p_392171_).listOf().xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> nonEmptyCodec(Codec<E> p_395084_) {
      return ExtraCodecs.nonEmptyList(Weighted.codec(p_395084_).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E> Codec<WeightedList<E>> nonEmptyCodec(MapCodec<E> p_397938_) {
      return ExtraCodecs.nonEmptyList(Weighted.codec(p_397938_).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
   }

   public static <E, B extends ByteBuf> StreamCodec<B, WeightedList<E>> streamCodec(StreamCodec<B, E> p_423484_) {
      return Weighted.streamCodec(p_423484_).apply(ByteBufCodecs.list()).map(WeightedList::of, WeightedList::unwrap);
   }

   public boolean contains(E p_394468_) {
      for (Weighted<E> weighted : this.items) {
         if (weighted.value().equals(p_394468_)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean equals(@Nullable Object p_394129_) {
      if (this == p_394129_) {
         return true;
      } else {
         return p_394129_ instanceof WeightedList<?> weightedlist
            ? this.totalWeight == weightedlist.totalWeight && Objects.equals(this.items, weightedlist.items)
            : false;
      }
   }

   @Override
   public int hashCode() {
      int i = this.totalWeight;
      return 31 * i + this.items.hashCode();
   }

   public static class Builder<E> {
      private final com.google.common.collect.ImmutableList.Builder<Weighted<E>> result = ImmutableList.builder();

      public WeightedList.Builder<E> add(E p_395636_) {
         return this.add(p_395636_, 1);
      }

      public WeightedList.Builder<E> add(E p_391313_, int p_397962_) {
         this.result.add(new Weighted<>(p_391313_, p_397962_));
         return this;
      }

      public WeightedList<E> build() {
         return new WeightedList<>(this.result.build());
      }
   }

   static class Compact<E> implements WeightedList.Selector<E> {
      private final Weighted<?>[] entries;

      Compact(List<Weighted<E>> p_394912_) {
         this.entries = p_394912_.toArray(Weighted[]::new);
      }

      @Override
      public E get(int p_395412_) {
         for (Weighted<?> weighted : this.entries) {
            p_395412_ -= weighted.weight();
            if (p_395412_ < 0) {
               return (E)weighted.value();
            }
         }

         throw new IllegalStateException(p_395412_ + " exceeded total weight");
      }
   }

   static class Flat<E> implements WeightedList.Selector<E> {
      private final Object[] entries;

      Flat(List<Weighted<E>> p_397273_, int p_391580_) {
         this.entries = new Object[p_391580_];
         int i = 0;

         for (Weighted<E> weighted : p_397273_) {
            int j = weighted.weight();
            Arrays.fill(this.entries, i, i + j, weighted.value());
            i += j;
         }
      }

      @Override
      public E get(int p_395440_) {
         return (E)this.entries[p_395440_];
      }
   }

   interface Selector<E> {
      E get(int var1);
   }
}
