package net.minecraft.world.level.block.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class StateDefinition<O, S extends StateHolder<O, S>> {
   static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
   private final O owner;
   private final ImmutableSortedMap<String, Property<?>> propertiesByName;
   private final ImmutableList<S> states;

   protected StateDefinition(Function<O, S> p_61052_, O p_61053_, StateDefinition.Factory<O, S> p_61054_, Map<String, Property<?>> p_61055_) {
      this.owner = p_61053_;
      this.propertiesByName = ImmutableSortedMap.copyOf(p_61055_);
      Supplier<S> supplier = () -> p_61052_.apply(p_61053_);
      MapCodec<S> mapcodec = MapCodec.of(Encoder.empty(), Decoder.unit(supplier));
      UnmodifiableIterator mapcodec1 = this.propertiesByName.entrySet().iterator();

      while (mapcodec1.hasNext()) {
         Entry<String, Property<?>> entry = (Entry<String, Property<?>>)mapcodec1.next();
         mapcodec = appendPropertyCodec(mapcodec, supplier, entry.getKey(), entry.getValue());
      }

      MapCodec<S> mapcodec1x = mapcodec;
      Map<Map<Property<?>, Comparable<?>>, S> map = Maps.newLinkedHashMap();
      List<S> list = Lists.newArrayList();
      Stream<List<Pair<Property<?>, Comparable<?>>>> stream = Stream.of(Collections.emptyList());
      UnmodifiableIterator var11 = this.propertiesByName.values().iterator();

      while (var11.hasNext()) {
         Property<?> property = (Property<?>)var11.next();
         stream = stream.flatMap(p_360551_ -> property.getPossibleValues().stream().map(p_155961_ -> {
            List<Pair<Property<?>, Comparable<?>>> list1 = Lists.newArrayList(p_360551_);
            list1.add(Pair.of(property, p_155961_));
            return list1;
         }));
      }

      stream.forEach(p_327405_ -> {
         Reference2ObjectArrayMap<Property<?>, Comparable<?>> reference2objectarraymap = new Reference2ObjectArrayMap(p_327405_.size());

         for (Pair<Property<?>, Comparable<?>> pair : p_327405_) {
            reference2objectarraymap.put((Property)pair.getFirst(), (Comparable)pair.getSecond());
         }

         S s1 = p_61054_.create(p_61053_, reference2objectarraymap, mapcodec1);
         map.put(reference2objectarraymap, s1);
         list.add(s1);
      });

      for (S s : list) {
         s.populateNeighbours(map);
      }

      this.states = ImmutableList.copyOf(list);
   }

   private static <S extends StateHolder<?, S>, T extends Comparable<T>> MapCodec<S> appendPropertyCodec(
      MapCodec<S> p_61077_, Supplier<S> p_61078_, String p_61079_, Property<T> p_61080_
   ) {
      return Codec.mapPair(p_61077_, p_61080_.valueCodec().fieldOf(p_61079_).orElseGet(p_187541_ -> {}, () -> p_61080_.value(p_61078_.get())))
         .xmap(
            p_187536_ -> (StateHolder)((StateHolder)p_187536_.getFirst()).setValue(p_61080_, ((Property.Value)p_187536_.getSecond()).value()),
            p_187533_ -> Pair.of(p_187533_, p_61080_.value(p_187533_))
         );
   }

   public ImmutableList<S> getPossibleStates() {
      return this.states;
   }

   public S any() {
      return (S)this.states.get(0);
   }

   public O getOwner() {
      return this.owner;
   }

   public Collection<Property<?>> getProperties() {
      return this.propertiesByName.values();
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this)
         .add("block", this.owner)
         .add("properties", this.propertiesByName.values().stream().map(Property::getName).collect(Collectors.toList()))
         .toString();
   }

   public @Nullable Property<?> getProperty(String p_61082_) {
      return (Property<?>)this.propertiesByName.get(p_61082_);
   }

   public static class Builder<O, S extends StateHolder<O, S>> {
      private final O owner;
      private final Map<String, Property<?>> properties = Maps.newHashMap();

      public Builder(O p_61098_) {
         this.owner = p_61098_;
      }

      public StateDefinition.Builder<O, S> add(Property<?>... p_61105_) {
         for (Property<?> property : p_61105_) {
            this.validateProperty(property);
            this.properties.put(property.getName(), property);
         }

         return this;
      }

      private <T extends Comparable<T>> void validateProperty(Property<T> p_61100_) {
         String s = p_61100_.getName();
         if (!StateDefinition.NAME_PATTERN.matcher(s).matches()) {
            throw new IllegalArgumentException(this.owner + " has invalidly named property: " + s);
         }

         Collection<T> collection = p_61100_.getPossibleValues();
         if (collection.size() <= 1) {
            throw new IllegalArgumentException(this.owner + " attempted use property " + s + " with <= 1 possible values");
         }

         for (T t : collection) {
            String s1 = p_61100_.getName(t);
            if (!StateDefinition.NAME_PATTERN.matcher(s1).matches()) {
               throw new IllegalArgumentException(this.owner + " has property: " + s + " with invalidly named value: " + s1);
            }
         }

         if (this.properties.containsKey(s)) {
            throw new IllegalArgumentException(this.owner + " has duplicate property: " + s);
         }
      }

      public StateDefinition<O, S> create(Function<O, S> p_61102_, StateDefinition.Factory<O, S> p_61103_) {
         return new StateDefinition<>(p_61102_, this.owner, p_61103_, this.properties);
      }
   }

   public interface Factory<O, S> {
      S create(O var1, Reference2ObjectArrayMap<Property<?>, Comparable<?>> var2, MapCodec<S> var3);
   }
}
