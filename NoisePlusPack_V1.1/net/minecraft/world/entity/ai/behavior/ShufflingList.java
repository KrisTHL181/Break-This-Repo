package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.RandomSource;

public class ShufflingList<U> implements Iterable<U> {
   protected final List<ShufflingList.WeightedEntry<U>> entries;
   private final RandomSource random = RandomSource.create();

   public ShufflingList() {
      this.entries = Lists.newArrayList();
   }

   private ShufflingList(List<ShufflingList.WeightedEntry<U>> p_147921_) {
      this.entries = Lists.newArrayList(p_147921_);
   }

   public static <U> Codec<ShufflingList<U>> codec(Codec<U> p_147928_) {
      return ShufflingList.WeightedEntry.codec(p_147928_).listOf().xmap(ShufflingList::new, p_147926_ -> p_147926_.entries);
   }

   public ShufflingList<U> add(U p_147930_, int p_147931_) {
      this.entries.add(new ShufflingList.WeightedEntry<>(p_147930_, p_147931_));
      return this;
   }

   public ShufflingList<U> shuffle() {
      this.entries.forEach(p_147924_ -> p_147924_.setRandom(this.random.nextFloat()));
      this.entries.sort(Comparator.comparingDouble(ShufflingList.WeightedEntry::getRandWeight));
      return this;
   }

   public Stream<U> stream() {
      return this.entries.stream().map(ShufflingList.WeightedEntry::getData);
   }

   @Override
   public Iterator<U> iterator() {
      return Iterators.transform(this.entries.iterator(), ShufflingList.WeightedEntry::getData);
   }

   @Override
   public String toString() {
      return "ShufflingList[" + this.entries + "]";
   }

   public static class WeightedEntry<T> {
      final T data;
      final int weight;
      private double randWeight;

      WeightedEntry(T p_147938_, int p_147939_) {
         this.weight = p_147939_;
         this.data = p_147938_;
      }

      private double getRandWeight() {
         return this.randWeight;
      }

      void setRandom(float p_147942_) {
         this.randWeight = -Math.pow(p_147942_, 1.0F / this.weight);
      }

      public T getData() {
         return this.data;
      }

      public int getWeight() {
         return this.weight;
      }

      @Override
      public String toString() {
         return this.weight + ":" + this.data;
      }

      public static <E> Codec<ShufflingList.WeightedEntry<E>> codec(final Codec<E> p_147944_) {
         return new Codec<ShufflingList.WeightedEntry<E>>() {
            public <T> DataResult<Pair<ShufflingList.WeightedEntry<E>, T>> decode(DynamicOps<T> p_147962_, T p_147963_) {
               Dynamic<T> dynamic = new Dynamic(p_147962_, p_147963_);
               return dynamic.get("data")
                  .flatMap(p_147944_::parse)
                  .map(p_147957_ -> new ShufflingList.WeightedEntry<>(p_147957_, dynamic.get("weight").asInt(1)))
                  .map(p_147960_ -> Pair.of(p_147960_, p_147962_.empty()));
            }

            public <T> DataResult<T> encode(ShufflingList.WeightedEntry<E> p_147952_, DynamicOps<T> p_147953_, T p_147954_) {
               return p_147953_.mapBuilder()
                  .add("weight", p_147953_.createInt(p_147952_.weight))
                  .add("data", p_147944_.encodeStart(p_147953_, p_147952_.data))
                  .build(p_147954_);
            }
         };
      }
   }
}
