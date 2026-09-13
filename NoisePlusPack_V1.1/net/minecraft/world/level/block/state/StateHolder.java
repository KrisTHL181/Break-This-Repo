package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public abstract class StateHolder<O, S> {
   public static final String NAME_TAG = "Name";
   public static final String PROPERTIES_TAG = "Properties";
   private static final Function<Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Entry<Property<?>, Comparable<?>>, String>() {
      public String apply(@Nullable Entry<Property<?>, Comparable<?>> p_61155_) {
         if (p_61155_ == null) {
            return "<NULL>";
         }

         Property<?> property = p_61155_.getKey();
         return property.getName() + "=" + this.getName(property, p_61155_.getValue());
      }

      private <T extends Comparable<T>> String getName(Property<T> p_61152_, Comparable<?> p_61153_) {
         return p_61152_.getName((T)p_61153_);
      }
   };
   protected final O owner;
   private final Reference2ObjectArrayMap<Property<?>, Comparable<?>> values;
   private Map<Property<?>, S[]> neighbours;
   protected final MapCodec<S> propertiesCodec;

   protected StateHolder(O p_61117_, Reference2ObjectArrayMap<Property<?>, Comparable<?>> p_331170_, MapCodec<S> p_61119_) {
      this.owner = p_61117_;
      this.values = p_331170_;
      this.propertiesCodec = p_61119_;
   }

   public <T extends Comparable<T>> S cycle(Property<T> p_61123_) {
      return this.setValue(p_61123_, findNextInCollection(p_61123_.getPossibleValues(), this.getValue(p_61123_)));
   }

   protected static <T> T findNextInCollection(List<T> p_366325_, T p_61132_) {
      int i = p_366325_.indexOf(p_61132_) + 1;
      return i == p_366325_.size() ? p_366325_.getFirst() : p_366325_.get(i);
   }

   @Override
   public String toString() {
      StringBuilder stringbuilder = new StringBuilder();
      stringbuilder.append(this.owner);
      if (!this.getValues().isEmpty()) {
         stringbuilder.append('[');
         stringbuilder.append(this.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
         stringbuilder.append(']');
      }

      return stringbuilder.toString();
   }

   @Override
   public final boolean equals(Object p_397228_) {
      return super.equals(p_397228_);
   }

   @Override
   public int hashCode() {
      return super.hashCode();
   }

   public Collection<Property<?>> getProperties() {
      return Collections.unmodifiableCollection(this.values.keySet());
   }

   public boolean hasProperty(Property<?> p_61139_) {
      return this.values.containsKey(p_61139_);
   }

   public <T extends Comparable<T>> T getValue(Property<T> p_61144_) {
      Comparable<?> comparable = (Comparable<?>)this.values.get(p_61144_);
      if (comparable == null) {
         throw new IllegalArgumentException("Cannot get property " + p_61144_ + " as it does not exist in " + this.owner);
      } else {
         return p_61144_.getValueClass().cast(comparable);
      }
   }

   public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> p_61146_) {
      return Optional.ofNullable(this.getNullableValue(p_61146_));
   }

   public <T extends Comparable<T>> T getValueOrElse(Property<T> p_364529_, T p_364048_) {
      return Objects.requireNonNullElse(this.getNullableValue(p_364529_), p_364048_);
   }

   private <T extends Comparable<T>> @Nullable T getNullableValue(Property<T> p_361815_) {
      Comparable<?> comparable = (Comparable<?>)this.values.get(p_361815_);
      return comparable == null ? null : p_361815_.getValueClass().cast(comparable);
   }

   public <T extends Comparable<T>, V extends T> S setValue(Property<T> p_61125_, V p_61126_) {
      Comparable<?> comparable = (Comparable<?>)this.values.get(p_61125_);
      if (comparable == null) {
         throw new IllegalArgumentException("Cannot set property " + p_61125_ + " as it does not exist in " + this.owner);
      } else {
         return this.setValueInternal(p_61125_, p_61126_, comparable);
      }
   }

   public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> p_263324_, V p_263334_) {
      Comparable<?> comparable = (Comparable<?>)this.values.get(p_263324_);
      return (S)(comparable == null ? this : this.setValueInternal(p_263324_, p_263334_, comparable));
   }

   private <T extends Comparable<T>, V extends T> S setValueInternal(Property<T> p_361946_, V p_367503_, Comparable<?> p_369806_) {
      if (p_369806_.equals(p_367503_)) {
         return (S)this;
      } else {
         int i = p_361946_.getInternalIndex((T)p_367503_);
         if (i < 0) {
            throw new IllegalArgumentException("Cannot set property " + p_361946_ + " to " + p_367503_ + " on " + this.owner + ", it is not an allowed value");
         } else {
            return (S)this.neighbours.get(p_361946_)[i];
         }
      }
   }

   public void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> p_61134_) {
      if (this.neighbours != null) {
         throw new IllegalStateException();
      }

      Map<Property<?>, S[]> map = new Reference2ObjectArrayMap(this.values.size());
      ObjectIterator var3 = this.values.entrySet().iterator();

      while (var3.hasNext()) {
         Entry<Property<?>, Comparable<?>> entry = (Entry<Property<?>, Comparable<?>>)var3.next();
         Property<?> property = entry.getKey();
         map.put(property, property.getPossibleValues().stream().map(p_360554_ -> p_61134_.get(this.makeNeighbourValues(property, p_360554_))).toArray());
      }

      this.neighbours = map;
   }

   private Map<Property<?>, Comparable<?>> makeNeighbourValues(Property<?> p_61141_, Comparable<?> p_61142_) {
      Map<Property<?>, Comparable<?>> map = new Reference2ObjectArrayMap(this.values);
      map.put(p_61141_, p_61142_);
      return map;
   }

   public Map<Property<?>, Comparable<?>> getValues() {
      return this.values;
   }

   protected static <O, S extends StateHolder<O, S>> Codec<S> codec(Codec<O> p_61128_, Function<O, S> p_61129_) {
      return p_61128_.dispatch(
         "Name",
         p_61121_ -> p_61121_.owner,
         p_327407_ -> {
            S s = p_61129_.apply((O)p_327407_);
            return s.getValues().isEmpty()
               ? MapCodec.unit(s)
               : s.propertiesCodec.codec().lenientOptionalFieldOf("Properties").xmap(p_187544_ -> p_187544_.orElse(s), Optional::of);
         }
      );
   }
}
