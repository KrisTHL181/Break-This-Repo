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

   private ShufflingList(final List<ShufflingList.WeightedEntry<U>> entries) {
      this.entries = Lists.newArrayList(entries);
   }

   public static <U> Codec<ShufflingList<U>> codec(final Codec<U> elementCodec) {
      return ShufflingList.WeightedEntry.codec(elementCodec).listOf().xmap(ShufflingList::new, l -> l.entries);
   }

   public ShufflingList<U> add(final U data, final int weight) {
      this.entries.add(new ShufflingList.WeightedEntry<>(data, weight));
      return this;
   }

   public ShufflingList<U> shuffle() {
      this.entries.forEach(k -> k.setRandom(this.random.nextFloat()));
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
      private final T data;
      private final int weight;
      private double randWeight;

      private WeightedEntry(final T data, final int weight) {
         this.weight = weight;
         this.data = data;
      }

      private double getRandWeight() {
         return this.randWeight;
      }

      private void setRandom(final float random) {
         this.randWeight = -Math.pow(random, 1.0F / this.weight);
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

      public static <E> Codec<ShufflingList.WeightedEntry<E>> codec(final Codec<E> elementCodec) {
         return new Codec<ShufflingList.WeightedEntry<E>>() {
            public <T> DataResult<Pair<ShufflingList.WeightedEntry<E>, T>> decode(final DynamicOps<T> ops, final T input) {
               Dynamic<T> map = new Dynamic(ops, input);
               return map.get("data")
                  .flatMap(elementCodec::parse)
                  .map(data -> new ShufflingList.WeightedEntry<>(data, map.get("weight").asInt(1)))
                  .map(r -> Pair.of(r, ops.empty()));
            }

            public <T> DataResult<T> encode(final ShufflingList.WeightedEntry<E> input, final DynamicOps<T> ops, final T prefix) {
               return ops.mapBuilder().add("weight", ops.createInt(input.weight)).add("data", elementCodec.encodeStart(ops, input.data)).build(prefix);
            }
         };
      }
   }
}
