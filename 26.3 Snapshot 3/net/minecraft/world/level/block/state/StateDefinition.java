package net.minecraft.world.level.block.state;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class StateDefinition<O, S extends StateHolder<O, S>> {
   private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_]+$");
   private static final Comparable<?>[] EMPTY_VALUES = new Comparable[0];
   private static final Property<?>[] EMPTY_KEYS = new Property[0];
   private static final StateHolder<?, ?>[][] EMPTY_NEIGHBORS = new StateHolder[0][];
   private final O owner;
   private final ImmutableSortedMap<String, Property<?>> propertiesByName;
   private final ImmutableList<S> states;
   private final MapCodec<S> propertiesCodec;

   protected StateDefinition(
      final Function<O, S> defaultState, final O owner, final StateDefinition.Factory<O, S> factory, final Map<String, Property<?>> properties
   ) {
      this.owner = owner;
      int propertyCount = properties.size();
      if (propertyCount == 0) {
         this.propertiesByName = ImmutableSortedMap.of();
         this.propertiesCodec = createCodec(owner, defaultState, this.propertiesByName);
         this.states = createSingletonState(owner, factory);
      } else {
         this.propertiesByName = ImmutableSortedMap.copyOf(properties);
         this.propertiesCodec = createCodec(owner, defaultState, this.propertiesByName);
         if (propertyCount == 1) {
            this.states = createSinglePropertyStates(owner, factory, this.propertiesByName);
         } else {
            this.states = createMultiPropertyStates(owner, factory, this.propertiesByName);
         }
      }
   }

   private static <O, S extends StateHolder<O, S>> MapCodec<S> createCodec(
      final O owner, final Function<O, S> defaultState, final Map<String, Property<?>> propertiesByName
   ) {
      Supplier<S> defaultSupplier = () -> defaultState.apply(owner);
      MapCodec<S> codec = MapCodec.unit(defaultSupplier);

      for (Entry<String, Property<?>> entry : propertiesByName.entrySet()) {
         codec = appendPropertyCodec(codec, defaultSupplier, entry.getKey(), entry.getValue());
      }

      return codec;
   }

   private static <O, S extends StateHolder<O, S>> ImmutableList<S> createSingletonState(final O owner, final StateDefinition.Factory<O, S> factory) {
      S singletonState = (S)factory.create(owner, EMPTY_KEYS, EMPTY_VALUES);
      singletonState.initializeNeighbors((S[][])emptyNeighbors());
      return ImmutableList.of(singletonState);
   }

   private static <O, S extends StateHolder<O, S>> ImmutableList<S> createSinglePropertyStates(
      final O owner, final StateDefinition.Factory<O, S> factory, final Map<String, Property<?>> propertiesByName
   ) {
      return createSinglePropertyStates(owner, factory, (Property)Iterables.getOnlyElement(propertiesByName.values()));
   }

   private static <O, S extends StateHolder<O, S>, T extends Comparable<T>> ImmutableList<S> createSinglePropertyStates(
      final O owner, final StateDefinition.Factory<O, S> factory, final Property<T> property
   ) {
      Property<?>[] propertyKeys = new Property[]{property};
      List<T> propertyValues = property.getPossibleValues();
      int valueCount = propertyValues.size();
      com.google.common.collect.ImmutableList.Builder<S> states = ImmutableList.builderWithExpectedSize(valueCount);
      S[] propertyNeighbours = (S[])(new StateHolder[valueCount]);
      S[][] neighbours = (S[][])(new StateHolder[][]{propertyNeighbours});

      for (int i = 0; i < valueCount; i++) {
         T propertyValue = (T)propertyValues.get(i);
         assert property.getInternalIndex(propertyValue) == i;
         S blockState = (S)factory.create(owner, propertyKeys, new Comparable[]{propertyValue});
         states.add(blockState);
         propertyNeighbours[i] = blockState;
         blockState.initializeNeighbors(neighbours);
      }

      return states.build();
   }

   private static <O, S extends StateHolder<O, S>> ImmutableList<S> createMultiPropertyStates(
      final O owner, final StateDefinition.Factory<O, S> factory, final Map<String, Property<?>> propertiesByName
   ) {
      Property<?>[] propertyKeys = propertiesByName.values().toArray(EMPTY_KEYS);
      List<List<? extends Comparable<?>>> allPropertyValues = new ArrayList<>(propertyKeys.length);

      for (Property<?> property : propertyKeys) {
         allPropertyValues.add((List<? extends Comparable<?>>)property.getPossibleValues());
      }

      List<List<Comparable<?>>> stateValues = Lists.cartesianProduct(allPropertyValues);
      Map<List<Comparable<?>>, S> statesByValues = new HashMap<>();
      com.google.common.collect.ImmutableList.Builder<S> states = ImmutableList.builderWithExpectedSize(stateValues.size());

      for (List<Comparable<?>> values : stateValues) {
         List<Comparable<?>> valuesCopy = List.copyOf(values);
         S blockState = (S)factory.create(owner, propertyKeys, valuesCopy.toArray(EMPTY_VALUES));
         statesByValues.put(valuesCopy, blockState);
         states.add(blockState);
      }

      StateDefinition.StateCollection<S> stateCollection = new StateDefinition.StateCollection<>(statesByValues, new HashMap<>());
      statesByValues.forEach((valuesx, state) -> state.initializeNeighbors(stateCollection.fillNeighborsForState(propertyKeys, valuesx)));
      return states.build();
   }

   private static <S extends StateHolder<?, ?>> S[][] emptyNeighbors() {
      return (S[][])EMPTY_NEIGHBORS;
   }

   // ===== 修改：修复 orElseGet 调用，删除无用的 var0 -> {} =====
   private static <S extends StateHolder<?, S>, T extends Comparable<T>> MapCodec<S> appendPropertyCodec(
      final MapCodec<S> codec, final Supplier<S> defaultSupplier, final String name, final Property<T> property
   ) {
      return Codec.mapPair(codec, property.valueCodec().fieldOf(name).orElseGet(() -> property.value(defaultSupplier.get())))
         .xmap(
            pair -> ((StateHolder)pair.getFirst()).setValue(property, ((Property.Value)pair.getSecond()).value()),
            state -> Pair.of(state, property.value(state))
         );
   }

   public ImmutableList<S> getPossibleStates() {
      return this.states;
   }

   public S any() {
      return (S)this.states.getFirst();
   }

   public MapCodec<S> propertiesCodec() {
      return this.propertiesCodec;
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

   public @Nullable Property<?> getProperty(final String name) {
      return (Property<?>)this.propertiesByName.get(name);
   }

   public boolean isSingletonState() {
      return this.propertiesByName.isEmpty();
   }

   public static class Builder<O, S extends StateHolder<O, S>> {
      private final O owner;
      private final Map<String, Property<?>> properties = Maps.newHashMap();

      public Builder(final O owner) {
         this.owner = owner;
      }

      public StateDefinition.Builder<O, S> add(final Property<?>... properties) {
         for (Property<?> property : properties) {
            this.validateProperty(property);
            this.properties.put(property.getName(), property);
         }

         return this;
      }

      private <T extends Comparable<T>> void validateProperty(final Property<T> property) {
         String name = property.getName();
         if (!StateDefinition.NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException(this.owner + " has invalidly named property: " + name);
         }

         Collection<T> values = property.getPossibleValues();
         if (values.size() <= 1) {
            throw new IllegalArgumentException(this.owner + " attempted use property " + name + " with <= 1 possible values");
         }

         for (T comparable : values) {
            String valueName = property.getName(comparable);
            if (!StateDefinition.NAME_PATTERN.matcher(valueName).matches()) {
               throw new IllegalArgumentException(this.owner + " has property: " + name + " with invalidly named value: " + valueName);
            }
         }

         if (this.properties.containsKey(name)) {
            throw new IllegalArgumentException(this.owner + " has duplicate property: " + name);
         }
      }

      public StateDefinition<O, S> create(final Function<O, S> defaultState, final StateDefinition.Factory<O, S> factory) {
         return new StateDefinition<>(defaultState, this.owner, factory, this.properties);
      }
   }

   public interface Factory<O, S> {
      S create(O type, Property<?>[] propertyKeys, Comparable<?>[] propertyValues);
   }

   record StateCollection<S extends StateHolder<?, ?>>(Map<List<Comparable<?>>, S> statesByValues, Map<List<Comparable<?>>, S[]> statesByPivotCache) {
      public S[][] fillNeighborsForState(final Property<?>[] propertyKeys, final List<Comparable<?>> propertyValues) {
         S[][] neighbors = (S[][])(new StateHolder[propertyKeys.length][]);
         List<Comparable<?>> valuesKey = new ArrayList<>(propertyValues);

         for (int i = 0; i < propertyKeys.length; i++) {
            neighbors[i] = this.fillStatesForPivot(valuesKey, propertyKeys[i], i);
         }

         return neighbors;
      }

      private <T extends Comparable<T>> S[] fillStatesForPivot(final List<Comparable<?>> valuesKey, final Property<T> pivot, final int pivotIndex) {
         Comparable<?> ownPivotValue = valuesKey.set(pivotIndex, StateDefinition.StateCollection.Wildcard.INSTANCE);

         try {
            S[] cachedResult = (S[])((StateHolder[])this.statesByPivotCache.get(valuesKey));
            if (cachedResult != null) {
               return cachedResult;
            }

            S[] neighbourStatesForPivot = this.computeStatesForPivot(valuesKey, pivot, pivotIndex);
            valuesKey.set(pivotIndex, StateDefinition.StateCollection.Wildcard.INSTANCE);
            this.statesByPivotCache.put(List.copyOf(valuesKey), neighbourStatesForPivot);
            return neighbourStatesForPivot;
         } finally {
            valuesKey.set(pivotIndex, ownPivotValue);
         }
      }

      private <T extends Comparable<T>> S[] computeStatesForPivot(final List<Comparable<?>> valuesKey, final Property<T> pivot, final int pivotIndex) {
         List<T> possiblePivotValues = pivot.getPossibleValues();
         int pivotValuesCount = possiblePivotValues.size();
         S[] result = (S[])(new StateHolder[pivotValuesCount]);

         for (int pivotValueIndex = 0; pivotValueIndex < pivotValuesCount; pivotValueIndex++) {
            T possiblePivotValue = (T)possiblePivotValues.get(pivotValueIndex);
            assert pivot.getInternalIndex(possiblePivotValue) == pivotValueIndex;
            valuesKey.set(pivotIndex, possiblePivotValue);
            S neighbourState = Objects.requireNonNull(this.statesByValues.get(valuesKey));
            result[pivotValueIndex] = neighbourState;
         }

         return result;
      }

      private enum Wildcard {
         INSTANCE;
      }
   }
}